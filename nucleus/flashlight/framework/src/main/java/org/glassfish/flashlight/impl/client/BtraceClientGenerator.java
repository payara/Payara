/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.flashlight.impl.client;

/**
 * @author Mahesh Kannan
 * Started: Jul 20, 2008
 * @author Byron Nevins, August 2009
 */
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.provider.ProbeRegistry;
import org.objectweb.asm.AnnotationVisitor;
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
import java.util.Collection;

public class BtraceClientGenerator {
    private BtraceClientGenerator() {
        // all static class -- no instances allowed
    }

    public static byte[] generateBtraceClientClassData(int clientID, Collection<FlashlightProbe> probes) {
        // create a unique name.  It does not matter what the name is.
        String generatedClassName = "com/sun/btrace/flashlight/BTrace_Flashlight_" + clientID;
        //Start of writing a class using ASM, which will be our BTrace Client
        int cwFlags = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;
        ClassWriter cw = new ClassWriter(cwFlags);

        //Define the access identifiers for the BTrace Client class
        int access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL;
        cw.visit(Opcodes.V1_5, access, generatedClassName, null,
                "java/lang/Object", null);
        //Need a @OnMethod annotation, so prepare your Annotation Visitor for that
        cw.visitAnnotation("Lcom/sun/btrace/annotations/BTrace;", true);

        //Iterate through the probes, so you will create one method for each probe
        int methodCounter = 0;
        for (FlashlightProbe probe : probes) {
            //Preparing the class method header and params (type) for @OnMethod annotation
            StringBuilder typeDesc = new StringBuilder("void ");
            StringBuilder methodDesc = new StringBuilder("void __");
            methodDesc.append(probe.getProviderJavaMethodName()).append("__");
            methodDesc.append(clientID).append("_").append(methodCounter).append("_");
            methodDesc.append("(");
            typeDesc.append("(");
            String delim = "";
            String typeDelim = "";
            Class[] paramTypes = probe.getParamTypes();
            for (int index = 0; index < paramTypes.length; index++) {
                Class paramType = paramTypes[index];
                methodDesc.append(delim).append(paramType.getName());
                // Dont add the param type for type desc, if self is the first index
                if (!(probe.hasSelf() && (index == 0))) {
                    typeDesc.append(typeDelim).append(paramType.getName());
                    typeDelim = ",";
                }
                delim = ", ";
            }
            methodDesc.append(")");
            typeDesc.append(")");
            //Creating the class method
            Method m = Method.getMethod(methodDesc.toString());
            GeneratorAdapter gen = new GeneratorAdapter(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, m, null, null, cw);
            // Add the @Self annotation
            if (probe.hasSelf()) {
                String[] paramNames = probe.getProbeParamNames();
                for (int index = 0; index < paramNames.length; index++) {
                    if (paramNames[index].equalsIgnoreCase(FlashlightProbe.SELF)) {
                        AnnotationVisitor paramVisitor = gen.visitParameterAnnotation(index, "Lcom/sun/btrace/annotations/Self;", true);
                        paramVisitor.visitEnd();
                    }
                }
            }
            //Add the @OnMethod annotation to this method
            AnnotationVisitor av = gen.visitAnnotation("Lcom/sun/btrace/annotations/OnMethod;", true);
            av.visit("clazz", "" + probe.getProviderClazz().getName());
            av.visit("method", probe.getProviderJavaMethodName());
            av.visit("type", typeDesc.toString());
            av.visitEnd();
            //Add the body
            gen.push(probe.getId());
            gen.loadArgArray();
            gen.invokeStatic(Type.getType(
                    ProbeRegistry.class), Method.getMethod("void invokeProbe(int, Object[])"));
            gen.returnValue();
            gen.endMethod();
            methodCounter++;
        }
        BtraceClientGenerator.generateConstructor(cw);
        cw.visitEnd();
        byte[] classData = cw.toByteArray();
        writeClass(classData, generatedClassName);
        return classData;
    }

    private static void writeClass(byte[] classData, String generatedClassName) {
        // only do this if we are in "debug" mode
//        if(Boolean.parseBoolean(System.getenv("AS_DEBUG")) == false)
//            return;

        System.out.println("**** Generated BTRACE Client " + generatedClassName);
        FileOutputStream fos = null;

        try {
            int index = generatedClassName.lastIndexOf('/');
            String rootPath = System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY) +
                    File.separator + "lib" + File.separator;

            String fileName = rootPath + generatedClassName.substring(index + 1) + ".class";
            //System.out.println("***ClassFile: " + fileName);
            File file = new File(fileName);

            fos = new FileOutputStream(file);
            fos.write(classData);
            fos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                }
                catch(Exception e) {
                // can't do anything...
                }
            }
        }
    }

    private static void generateConstructor(ClassWriter cw) {
        Method m = Method.getMethod("void <init> ()");
        GeneratorAdapter gen = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, null, null, cw);
        gen.loadThis();
        gen.invokeConstructor(Type.getType(Object.class), m);
        //return the value from constructor
        gen.returnValue();
        gen.endMethod();
    }
}


/****  Example generated class (bnevins, August 2009)
 *
 * package com.sun.btrace.flashlight.org.glassfish.web.admin.monitor;

import com.sun.btrace.annotations.BTrace;
import com.sun.btrace.annotations.OnMethod;
import javax.servlet.Servlet;
import org.glassfish.flashlight.provider.ProbeRegistry;

@BTrace
public final class ServletStatsProvider_BTrace_7_
{
  @OnMethod(clazz="org.glassfish.web.admin.monitor.ServletProbeProvider", method="servletInitializedEvent", type="void (javax.servlet.Servlet, java.lang.String, java.lang.String)")
  public static void __servletInitializedEvent__7_0_(Servlet paramServlet, String paramString1, String paramString2)
  {
    ProbeRegistry.invokeProbe(78, new Object[] { paramServlet, paramString1, paramString2 });
  }

  @OnMethod(clazz="org.glassfish.web.admin.monitor.ServletProbeProvider", method="servletDestroyedEvent", type="void (javax.servlet.Servlet, java.lang.String, java.lang.String)")
  public static void __servletDestroyedEvent__7_1_(Servlet paramServlet, String paramString1, String paramString2)
  {
    ProbeRegistry.invokeProbe(79, new Object[] { paramServlet, paramString1, paramString2 });
  }
}
 */
