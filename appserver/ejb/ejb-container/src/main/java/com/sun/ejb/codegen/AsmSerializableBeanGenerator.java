/*
 * Copyright (c) 2021, 2025 Contributors to the Eclipse Foundation.
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
// Portions Copyright [2025] [Payara Foundation and/or its affiliates]
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license


package com.sun.ejb.codegen;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static com.sun.ejb.codegen.ClassGenerator.defineClass;
import static com.sun.ejb.codegen.Generator.getBaseName;
import static com.sun.ejb.codegen.Generator.getFullClassName;
import static com.sun.ejb.codegen.Generator.getPackageName;
import static org.objectweb.asm.Opcodes.*;

public class AsmSerializableBeanGenerator extends BeanGeneratorBase {

    private final ClassLoader loader;
    private final Class<?> superClass;
    private final String subClassName;

    /**
     * Adds _Serializable to the original name.
     *
     * @param beanClass full class name
     */
    public static String getGeneratedSerializableClassName(String beanClass) {
        String packageName = getPackageName(beanClass);
        String simpleName = getBaseName(beanClass);
        String generatedSimpleName = "_" + simpleName + "_Serializable";
        return getFullClassName(packageName, generatedSimpleName);
    }

    public AsmSerializableBeanGenerator(ClassLoader loader, Class<?> superClass, String serializableSubclassName) {
        this.loader = loader;
        this.superClass = superClass;
        this.subClassName = serializableSubclassName;
    }

    @SuppressWarnings("unused")
    public String getSerializableSubclassName() {
        return subClassName;
    }

    public Class<?> generateSerializableSubclass() {
        String subClassInternalName = subClassName.replace('.', '/');

        ClassWriter cw = new ClassWriter(0);

        cw.visit(V17, ACC_PUBLIC, subClassInternalName, null, Type.getInternalName(superClass), new String[] {"java/io/Serializable"});

        generateConstructor(cw, superClass, false);

        generateWriteObjectMethod(cw);

        generateReadObjectMethod(cw);

        cw.visitEnd();

        return defineClass(loader, subClassName, cw.toByteArray(), superClass.getProtectionDomain());
    }

    private static void generateWriteObjectMethod(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE,
                "writeObject",
                "(Ljava/io/ObjectOutputStream;)V",
                null,
                new String[] {"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC,
                "com/sun/ejb/EJBUtils",
                "serializeObjectFields",
                "(Ljava/lang/Object;Ljava/io/ObjectOutputStream;)V",
                false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static void generateReadObjectMethod(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE,
                "readObject",
                "(Ljava/io/ObjectInputStream;)V",
                null,
                new String[] {"java/io/IOException", "java/lang/ClassNotFoundException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC,
                "com/sun/ejb/EJBUtils",
                "deserializeObjectFields",
                "(Ljava/lang/Object;Ljava/io/ObjectInputStream;)V",
                false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }
}
