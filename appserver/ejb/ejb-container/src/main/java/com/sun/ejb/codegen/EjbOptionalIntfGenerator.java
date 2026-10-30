/*
 * Copyright (c) 2022, 2025 Contributors to the Eclipse Foundation.
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.ejb.spi.container.OptionalLocalInterfaceProvider;
import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;
import com.sun.enterprise.deployment.util.TypeUtil;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static com.sun.ejb.codegen.ClassGenerator.defineClass;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

public class EjbOptionalIntfGenerator extends BeanGeneratorBase {

    private static final String DELEGATE_FIELD_NAME = "__ejb31_delegate";

    private final Map<String, byte[]> classMap = new HashMap<>();
    private final ClassLoader loader;
    private ProtectionDomain protectionDomain;

    public EjbOptionalIntfGenerator(ClassLoader loader) {
        this.loader = loader;
    }

    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        Class<?> clz = null;
        try {
            clz = loader.loadClass(name);
        } catch (ClassNotFoundException cnfe) {
            final byte[] classData = classMap.get(name);
            if (classData != null) {
                clz = defineClass(loader, name, classData, protectionDomain);
            }
        }

        if (clz == null) {
            throw new ClassNotFoundException(name);
        }

        return clz;
    }

    public void generateOptionalLocalInterface(Class<?> ejbClass, String intfClassName) {
        generateInterface(ejbClass, intfClassName, Serializable.class);
    }

    public void generateInterface(Class<?> ejbClass, String intfClassName, final Class<?>... interfaces) {
        String[] interfaceNames = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = Type.getInternalName(interfaces[i]);
        }

        if (protectionDomain == null) {
            protectionDomain = ejbClass.getProtectionDomain();
        }

        ClassWriter cw = new ClassWriter(0);

        cw.visit(V17,
                ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                intfClassName.replace('.', '/'),
                null,
                "java/lang/Object",
                interfaceNames);

        for (Method method : ejbClass.getMethods()) {
            if (qualifiedAsBeanMethod(method)) {
                generateInterfaceMethod(cw, method);
            }
        }

        cw.visitEnd();

        classMap.put(intfClassName, cw.toByteArray());
    }

    /**
     * Determines if a method from a bean class can be considered as a business
     * method for EJB of no-interface view.
     * @param method a public method
     * @return true if m can be included as a bean business method.
     */
    private boolean qualifiedAsBeanMethod(Method method) {
        if (method.getDeclaringClass() == Object.class) {
            return false;
        }
        int modifiers = method.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers);
    }

    private boolean hasSameSignatureAsExisting(Method methodToMatch, Set<Method> methods) {
        boolean sameSignature = false;
        for(Method method : methods) {
            if( TypeUtil.sameMethodSignature(method, methodToMatch) ) {
                sameSignature = true;
                break;
            }
        }
        return sameSignature;
    }

    public void generateOptionalLocalInterfaceSubClass(Class<?> superClass, String subClassName, Class<?> delegateClass) {
        generateSubclass(superClass, subClassName, delegateClass, IndirectlySerializable.class);
    }

    public void generateSubclass(Class<?> superClass, String subClassName, Class<?> delegateClass, Class<?>... interfaces) {
        String subClassInternalName = subClassName.replace('.', '/');
        String fieldDesc = Type.getDescriptor(delegateClass);

        if (protectionDomain == null) {
            protectionDomain = superClass.getProtectionDomain();
        }

        ClassWriter cw = new ClassWriter(0);

        String[] interfaceNames = new String[interfaces.length + 1];
        interfaceNames[0] = Type.getInternalName(OptionalLocalInterfaceProvider.class);
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i + 1] = Type.getInternalName(interfaces[i]);
        }

        cw.visit(V17, ACC_PUBLIC, subClassInternalName, null, Type.getInternalName(superClass), interfaceNames);

        generateDelegateField(cw, fieldDesc);

        generateConstructor(cw, superClass, true);

        generateSetDelegateMethod(cw, delegateClass, subClassInternalName);

        for (Class<?> intf : interfaces) {
            // dblevins: Don't think we need this special case.
            // Should be covered by letting generateBeanMethod
            // handle the methods on IndirectlySerializable.
            //
            // Not sure where the related tests are to verify.
            if (intf.equals(IndirectlySerializable.class)) {
                generateGetSerializableObjectFactoryMethod(cw, fieldDesc, subClassInternalName);
                continue;
            }

            for (Method method : intf.getMethods()) {
                generateBeanMethod(cw, subClassInternalName, method, delegateClass);
            }
        }

        Set<Method> allMethods = new HashSet<>();

        for (Method method : superClass.getMethods()) {
            if (qualifiedAsBeanMethod(method)) {
                generateBeanMethod(cw, subClassInternalName, method, delegateClass);
            }
        }

        for (Class<?> clz = superClass; clz != Object.class; clz = clz.getSuperclass()) {
            Method[] beanMethods = clz.getDeclaredMethods();
            for (Method method : beanMethods) {
                if(!hasSameSignatureAsExisting(method, allMethods)) {
                    int modifiers = method.getModifiers();
                    boolean isPublic = Modifier.isPublic(modifiers);
                    boolean isPrivate = Modifier.isPrivate(modifiers);
                    boolean isProtected = Modifier.isProtected(modifiers);
                    boolean isPackage = !isPublic && !isPrivate && !isProtected;

                    boolean isStatic = Modifier.isStatic(modifiers);

                    if( (isPackage || isProtected) && !isStatic ) {
                        generateNonAccessibleMethod(cw, method);
                    }
                    allMethods.add(method);
                }
            }
        }

        // add toString() method if it was not overridden
        try {
            Method method = Object.class.getDeclaredMethod("toString");
            if (!hasSameSignatureAsExisting(method, allMethods)) {
                generateToStringBeanMethod(cw, superClass);
            }
        } catch (NoSuchMethodException e) {
            // Should never be thrown
            throw new IllegalStateException(e);
        }

        cw.visitEnd();

        classMap.put(subClassName, cw.toByteArray());
    }

    private static void generateDelegateField(ClassVisitor cv, String fieldDesc) {
        FieldVisitor fv = cv.visitField(ACC_PRIVATE, DELEGATE_FIELD_NAME, fieldDesc, null, null);
        fv.visitEnd();
    }

    private static void generateInterfaceMethod(ClassVisitor cv, Method method) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_ABSTRACT,
                method.getName(),
                Type.getMethodDescriptor(method),
                null,
                getExceptions(method));
        mv.visitEnd();
    }

    private static void generateBeanMethod(ClassVisitor cv, String subClassInternalName, Method method, Class<?> delegateClass) {
        String methodName = method.getName();
        String methodDesc = Type.getMethodDescriptor(method);

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, getExceptions(method));
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, subClassInternalName, DELEGATE_FIELD_NAME, Type.getDescriptor(delegateClass));

        int varIndex = 1;
        for (Type argumentType : Type.getArgumentTypes(methodDesc)) {
            mv.visitVarInsn(argumentType.getOpcode(ILOAD), varIndex);
            varIndex += argumentType.getSize();
        }

        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(delegateClass), methodName, methodDesc, true);

        Type returnType = Type.getReturnType(methodDesc);

        mv.visitInsn(returnType.getOpcode(IRETURN));
        mv.visitMaxs(Math.max(varIndex, returnType.getSize()), varIndex);
        mv.visitEnd();
    }

    private static void generateToStringBeanMethod(ClassVisitor cv, Class<?> superClass) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(superClass.getName() + "@");
        mv.visitMethodInsn(INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/Object",
                "hashCode",
                "()I",
                false);
        mv.visitMethodInsn(INVOKESTATIC,
                "java/lang/Integer",
                "toHexString",
                "(I)Ljava/lang/String;",
                false);
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false);
        mv.visitMethodInsn(INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "toString",
                "()Ljava/lang/String;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

    // Only called for non-static Protected or Package access
    private static void generateNonAccessibleMethod(ClassVisitor cv, Method method) {
        String methodDesc = Type.getMethodDescriptor(method);

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, method.getName(), methodDesc, null, getExceptions(method));
        mv.visitCode();
        mv.visitTypeInsn(NEW, "jakarta/ejb/EJBException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Illegal non-business method access on no-interface view");
        mv.visitMethodInsn(INVOKESPECIAL,
                "jakarta/ejb/EJBException",
                "<init>",
                "(Ljava/lang/String;)V",
                false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(3, Type.getArgumentsAndReturnSizes(methodDesc) >> 2);
        mv.visitEnd();
    }

    private static void generateGetSerializableObjectFactoryMethod(ClassVisitor cv, String fieldDesc, String classDesc) {
        String methodName = "getSerializableObjectFactory";
        String methodDesc = Type.getMethodDescriptor(Type.getType(SerializableObjectFactory.class));
        String methodOwner = Type.getInternalName(IndirectlySerializable.class);

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, methodDesc, null, new String[] {"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, classDesc, DELEGATE_FIELD_NAME, fieldDesc);
        mv.visitTypeInsn(CHECKCAST, methodOwner);
        mv.visitMethodInsn(INVOKEINTERFACE, methodOwner, methodName, methodDesc, true);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static String[] getExceptions(Method method) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        String[] exceptions = new String[exceptionTypes.length];
        for (int i = 0; i < exceptionTypes.length; i++) {
            exceptions[i] = Type.getInternalName(exceptionTypes[i]);
        }

        return exceptions;
    }

    private static void generateSetDelegateMethod(ClassVisitor cv, Class<?> delegateClass, String subClassInternalName) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "setOptionalLocalIntfProxy", "(Ljava/lang/reflect/Proxy;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(delegateClass));
        mv.visitFieldInsn(PUTFIELD, subClassInternalName, DELEGATE_FIELD_NAME, Type.getDescriptor(delegateClass));
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }
}
