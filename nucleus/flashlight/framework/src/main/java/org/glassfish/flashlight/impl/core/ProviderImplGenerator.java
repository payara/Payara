/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.flashlight.impl.core;

/**
 * @author Mahesh Kannan
 *         Date: Jul 20, 2008
 */
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.provider.ProbeRegistry;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.glassfish.flashlight.FlashlightLoggerInfo;

public class ProviderImplGenerator {
    private static final Logger logger = FlashlightLoggerInfo.getLogger();

    public String defineClass(FlashlightProbeProvider provider, Class providerClazz) {

        String generatedClassName = provider.getModuleProviderName() + "_Flashlight_" + provider.getModuleName() + "_"
                + "Probe_" + ((provider.getProbeProviderName() == null) ? providerClazz.getName() : provider.getProbeProviderName());
        generatedClassName = providerClazz.getName() + "_" + generatedClassName;

        byte[] classData = generateClassData(provider, providerClazz, generatedClassName);

        ProtectionDomain pd = providerClazz.getProtectionDomain();

        java.lang.reflect.Method jm = null;
        for (java.lang.reflect.Method jm2 : ClassLoader.class.getDeclaredMethods()) {
            if (jm2.getName().equals("defineClass") && jm2.getParameterTypes().length == 5) {
                jm = jm2;
                break;
            }
        }

        if (jm == null)
            throw new RuntimeException();

        final java.lang.reflect.Method clM = jm;
        try {
            java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            if (!clM.isAccessible()) {
                                clM.setAccessible(true);
                            }
                            return null;
                        }
                    });

            clM.invoke(providerClazz.getClassLoader(), generatedClassName, classData, 0,
                    classData.length, pd);

            return generatedClassName;
        }
        catch (PrivilegedActionException pEx) {
            throw new RuntimeException(pEx);
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new RuntimeException(illegalAccessException);
        }
        catch (InvocationTargetException invtEx) {
            throw new RuntimeException(invtEx);
        }

    }

    public byte[] generateClassData(FlashlightProbeProvider provider, Class providerClazz, String generatedClassName) {


        Type classType = Type.getType(providerClazz);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("** classType: " + classType);
            logger.fine("** classDesc: " + Type.getDescriptor(providerClazz));
            logger.fine("Generating for: " + generatedClassName);
        }

        generatedClassName = generatedClassName.replace('.', '/');

        int cwFlags = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;
        ClassWriter cw = new ClassWriter(cwFlags);

        int access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL;
        String[] interfaces = new String[]{providerClazz.getName().replace('.', '/')};
        cw.visit(Opcodes.V1_5, access, generatedClassName, null, "java/lang/Object", interfaces);


        for (FlashlightProbe probe : provider.getProbes()) {
            Type probeType = Type.getType(FlashlightProbe.class);
            int fieldAccess = Opcodes.ACC_PUBLIC;
            String fieldName = "_flashlight_" + probe.getProbeName();
            cw.visitField(fieldAccess, fieldName,
                    probeType.getDescriptor(), null, null);
        }

        Type probeType = Type.getType(FlashlightProbe.class);
        for (FlashlightProbe probe : provider.getProbes()) {
            StringBuilder methodDesc = new StringBuilder();
            methodDesc.append("void ").append(probe.getProviderJavaMethodName());
            methodDesc.append("(");
            String delim = "";
            for (Class paramType : probe.getParamTypes()) {
                methodDesc.append(delim).append(paramType.getName());
                delim = ", ";
            }
            methodDesc.append(")");
            Method m = Method.getMethod(methodDesc.toString());
            GeneratorAdapter gen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null, null, cw);

            String fieldName = "_flashlight_" + probe.getProbeName();
            gen.loadThis();
            gen.visitFieldInsn(Opcodes.GETFIELD,
                    generatedClassName,
                    fieldName, probeType.getDescriptor());
            int index = gen.newLocal(probeType);
            gen.storeLocal(index);
            gen.loadLocal(index);
            gen.invokeVirtual(probeType, Method.getMethod("boolean isEnabled()"));
            gen.push(true);
            Label enabledLabel = new Label();
            Label notEnabledLabel = new Label();

            gen.ifCmp(Type.getType(boolean.class), GeneratorAdapter.EQ, enabledLabel);
            gen.goTo(notEnabledLabel);
            gen.visitLabel(enabledLabel);
            gen.loadLocal(index);
            gen.loadArgArray();
            gen.invokeVirtual(probeType, Method.getMethod("void fireProbe(Object[])"));
            gen.visitLabel(notEnabledLabel);
            gen.returnValue();
            gen.endMethod();
        }


        generateConstructor(cw, generatedClassName, provider);

        cw.visitEnd();

        byte[] classData = cw.toByteArray();

        int index = generatedClassName.lastIndexOf('.');
        String clsName = generatedClassName.substring(index + 1);


        if (Boolean.parseBoolean(System.getenv("AS_DEBUG"))) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Generated ClassDATA " + clsName);

            // the path is horribly long.  Let's just write t directly into the
            // lib dir.  It is not for loading as a class but just for us humans
            // to decompile to figure out what is going on.  No need to make it even harder!
            clsName = clsName.replace('.', '/');
            clsName = clsName.replace('\\', '/'); // just in case Windows?  unlikely...
            index = clsName.lastIndexOf("/");

            if (index >= 0)
                clsName = clsName.substring(index + 1);
            FileOutputStream fos = null;
            try {
                String rootPath = System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY)
                        + File.separator + "lib" + File.separator;

                String fileName = rootPath + clsName + ".class";

                if (logger.isLoggable(Level.FINE))
                    logger.fine("ClassFile: " + fileName);

                File file = new File(fileName);

                if (FileUtils.mkdirsMaybe(file.getParentFile())) {
                    fos = new FileOutputStream(file);
                    fos.write(classData);
                    fos.flush();
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            finally {
                try {
                    if (fos != null)
                        fos.close();
                }
                catch (Exception e) {
                    // nothing can be done...
                }
            }
        }
        return classData;
    }

    private void generateConstructor(ClassWriter cw, String generatedClassName, FlashlightProbeProvider provider) {
        Method m = Method.getMethod("void <init> ()");
        GeneratorAdapter gen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null, null, cw);
        gen.loadThis();
        gen.invokeConstructor(Type.getType(Object.class), m);

        Type probeRegType = Type.getType(ProbeRegistry.class);
        Type probeType = Type.getType(FlashlightProbe.class);

        gen.loadThis();
        for (FlashlightProbe probe : provider.getProbes()) {

            gen.dup();

            String fieldName = "_flashlight_" + probe.getProbeName();
            gen.push(probe.getId());
            gen.invokeStatic(probeRegType,
                    Method.getMethod("org.glassfish.flashlight.provider.FlashlightProbe getProbeById(int)"));

            gen.visitFieldInsn(Opcodes.PUTFIELD,
                    generatedClassName,
                    fieldName, probeType.getDescriptor());
        }

        gen.pop();
        //return the value from constructor
        gen.returnValue();
        gen.endMethod();
    }
}
/*************
 *
 * Example of generated file
 *
 * package org.glassfish.kernel.admin.monitor.ThreadPoolProbeProvider_core_Flashlight_threadpool_Probe_org.glassfish.kernel.admin.monitor;

import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.provider.ProbeRegistry;

public final class ThreadPoolProbeProvider
implements org.glassfish.kernel.admin.monitor.ThreadPoolProbeProvider
{
public FlashlightProbe _flashlight_threadReturnedToPoolEvent;
public FlashlightProbe _flashlight_threadDispatchedFromPoolEvent;
public FlashlightProbe _flashlight_newThreadsAllocatedEvent;
public FlashlightProbe _flashlight_maxNumberOfThreadsReachedEvent;

public void threadReturnedToPoolEvent(String paramString1, String paramString2)
{
FlashlightProbe localFlashlightProbe = this._flashlight_threadReturnedToPoolEvent;
if (localFlashlightProbe.isEnabled() != true)
return;
localFlashlightProbe.fireProbe(new Object[] { paramString1, paramString2 });
}

public void threadDispatchedFromPoolEvent(String paramString1, String paramString2)
{
FlashlightProbe localFlashlightProbe = this._flashlight_threadDispatchedFromPoolEvent;
if (localFlashlightProbe.isEnabled() != true)
return;
localFlashlightProbe.fireProbe(new Object[] { paramString1, paramString2 });
}

public void newThreadsAllocatedEvent(String paramString, int paramInt, boolean paramBoolean)
{
FlashlightProbe localFlashlightProbe = this._flashlight_newThreadsAllocatedEvent;
if (localFlashlightProbe.isEnabled() != true)
return;
localFlashlightProbe.fireProbe(new Object[] { paramString, new Integer(paramInt), new Boolean(paramBoolean) });
}

public void maxNumberOfThreadsReachedEvent(String paramString, int paramInt)
{
FlashlightProbe localFlashlightProbe = this._flashlight_maxNumberOfThreadsReachedEvent;
if (localFlashlightProbe.isEnabled() != true)
return;
localFlashlightProbe.fireProbe(new Object[] { paramString, new Integer(paramInt) });
}

public ThreadPoolProbeProvider()
{
ThreadPoolProbeProvider tmp5_4 = this;
tmp5_4._flashlight_threadReturnedToPoolEvent = ProbeRegistry.getProbeById(4);
ThreadPoolProbeProvider tmp13_5 = tmp5_4;
tmp13_5._flashlight_threadDispatchedFromPoolEvent = ProbeRegistry.getProbeById(3);
ThreadPoolProbeProvider tmp21_13 = tmp13_5;
tmp21_13._flashlight_newThreadsAllocatedEvent = ProbeRegistry.getProbeById(1);
ThreadPoolProbeProvider tmp29_21 = tmp21_13;
tmp29_21._flashlight_maxNumberOfThreadsReachedEvent = ProbeRegistry.getProbeById(2);
tmp29_21;
}
}
 ***********************************************************************
 * Another example
 * package com.sun.enterprise.v3.admin.ListContractsProbeProvider_admin-commands_Flashlight_glassfish_Probe_com.sun.enterprise.v3.admin;

import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.provider.ProbeRegistry;

public final class ListContractsProbeProvider
implements com.sun.enterprise.v3.admin.ListContractsProbeProvider
{
public FlashlightProbe _flashlight_listContractsEvent;

public void listContractsEvent(String paramString, boolean paramBoolean)
{
FlashlightProbe localFlashlightProbe = this._flashlight_listContractsEvent;
if (localFlashlightProbe.isEnabled() != true)
return;
localFlashlightProbe.fireProbe(new Object[] { paramString, new Boolean(paramBoolean) });
}

public ListContractsProbeProvider()
{
ListContractsProbeProvider tmp5_4 = this;
tmp5_4._flashlight_listContractsEvent = ProbeRegistry.getProbeById(113);
tmp5_4;
}
}

 */
