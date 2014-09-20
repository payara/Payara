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
 *         Date: Nov 8, 2009
 * Fixed a bunch of FindBugs problem, 2/2012, Byron Nevins
 */
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.flashlight.FlashlightLoggerInfo;
import java.security.*;
import org.objectweb.asm.*;

import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicInteger;

public class ProviderSubClassImplGenerator {
    private static final Logger logger = FlashlightLoggerInfo.getLogger();
    public final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ProviderSubClassImplGenerator.class);
    private String invokerId;
    private Class providerClazz;
    private static AtomicInteger counter = new AtomicInteger();

    public ProviderSubClassImplGenerator(Class providerClazz, String invokerId) {
        this.providerClazz = providerClazz;
        this.invokerId = invokerId;
    }

    public <T> Class<T> generateAndDefineClass(final Class<T> providerClazz, String invokerId) {

        int id = counter.incrementAndGet();
        String providerClassName = providerClazz.getName().replace('.', '/');
        String generatedClassName = providerClassName + invokerId + "_" + id;
        byte[] provClassData = null;
        try {
            InputStream is = providerClazz.getClassLoader().getResourceAsStream(providerClassName + ".class");
            int sz = is.available();
            provClassData = new byte[sz];
            int index = 0;
            while (index < sz) {
                int r = is.read(provClassData, index, sz - index);
                if (r > 0) {
                    index += r;
                }
            }
        }
        catch (Exception ex) {
            return null;
        }

        ClassReader cr = new ClassReader(provClassData);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
        byte[] classData = null;
        ProbeProviderSubClassGenerator sgen = new ProbeProviderSubClassGenerator(cw,
                invokerId, "_" + id);
        cr.accept(sgen, 0);
        classData = cw.toByteArray();

        ProtectionDomain pd = providerClazz.getProtectionDomain();

        SubClassLoader scl = createSubClassLoader(providerClazz);

        if(scl == null)
            return null;

        try {
            String gcName = scl.defineClass(generatedClassName, classData, pd);
            if (logger.isLoggable(Level.FINE))
                logger.fine("**** DEFINE CLASS SUCCEEDED for " + gcName + "," + generatedClassName);
            return (Class<T>) scl.loadClass(gcName);
        }
        catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Byron Nevins Feb 2012 FindBugs fix
     * Hide this ugly access control code in this method
     * @return the SubClassLoader or null if error(s)
     */
    private SubClassLoader createSubClassLoader(final Class theClass) {
        try {
            return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<SubClassLoader>() {
                        @Override
                        public SubClassLoader run() throws Exception {
                            return new SubClassLoader(theClass.getClassLoader());
                        }
                    });
        }
        catch (Exception e) {
            return null;
        }
    }

    static class SubClassLoader
            extends ClassLoader {
        SubClassLoader(ClassLoader cl) {
            super(cl);
        }

        String defineClass(String className, byte[] data, ProtectionDomain pd)
                throws Exception {

            className = className.replace('/', '.');
            super.defineClass(className, data, 0, data.length, pd);
            return className;
        }
    }

    private static class ProbeProviderSubClassGenerator
            extends ClassAdapter {
        String superClassName;
        String token;
        String id;

        ProbeProviderSubClassGenerator(ClassVisitor cv, String token, String id) {
            super(cv);
            this.id = id;
            this.token = token;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.superClassName = name;
            super.visit(version, access, name + token + id, signature, name, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor delegate = super.visitAnnotation(desc, visible);
            if ("Lorg/glassfish/external/probe/provider/annotations/ProbeProvider;".equals(desc)) {
                return new ProbeProviderAnnotationVisitor(delegate, token);
            }
            else {
                return delegate;
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] strings) {

            if ("<init>".equals(name) && desc.equals("()V")) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, strings);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, "<init>", desc);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();

                return null;
            }
            else {
                return super.visitMethod(access, name, desc, signature, strings);
            }
        }
    }

    private static class ProbeProviderAnnotationVisitor
            implements AnnotationVisitor {
        private AnnotationVisitor delegate;
        private String token;

        ProbeProviderAnnotationVisitor(AnnotationVisitor delegate, String token) {
            this.delegate = delegate;
            this.token = token;
        }

        @Override
        public void visit(String attrName, Object value) {
            delegate.visit(attrName, ("probeProviderName".equals(attrName) ? value + token : value));
        }

        @Override
        public void visitEnum(String s, String s1, String s2) {
            delegate.visitEnum(s, s1, s2);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String s, String s1) {
            return delegate.visitAnnotation(s, s1);
        }

        @Override
        public AnnotationVisitor visitArray(String s) {
            return delegate.visitArray(s);
        }

        @Override
        public void visitEnd() {
            delegate.visitEnd();
        }
    }
}
