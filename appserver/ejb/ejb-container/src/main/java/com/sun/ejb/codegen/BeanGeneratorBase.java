/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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

import jakarta.inject.Inject;
import java.lang.reflect.Constructor;
import org.glassfish.deployment.common.DeploymentException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * Common methods related to ASM bytecode generation.
 *
 * @author Alexander Pinčuk
 */
class BeanGeneratorBase {

    /**
     * Generate constructor.
     *
     * <p>The EJB spec only allows no-arg constructors, but CDI added requirements
     * that allow a single constructor to define parameters injected by CDI.
     *
     * @param cv the ASM {@code ClassVisitor}.
     * @param superClass a superclass.
     * @param withNoArguments indicates if generated constructor takes no arguments.
     */
    protected static void generateConstructor(ClassVisitor cv, Class<?> superClass, boolean withNoArguments) {
        Constructor<?> parentCtor = findParentConstructor(superClass);
        String parentCtorDesc = Type.getConstructorDescriptor(parentCtor);
        String ctorDesc = withNoArguments ? "()V" : parentCtorDesc;
        String superClassInternalName = Type.getInternalName(superClass);

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", ctorDesc, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);  // load 'this'

        int argCount = parentCtor.getParameterCount();
        for (int i = 0; i < argCount; i++) {
            if (withNoArguments) {
                mv.visitInsn(ACONST_NULL);
            } else {
                mv.visitVarInsn(ALOAD, i + 1);
            }
        }

        mv.visitMethodInsn(INVOKESPECIAL, superClassInternalName, "<init>", parentCtorDesc, false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(argCount + 1, withNoArguments ? 1 : argCount + 1);
        mv.visitEnd();
    }

    private static Constructor<?> findParentConstructor(Class<?> superClass) {
        Constructor<?>[] ctors = superClass.getConstructors();

        Constructor<?> parentCtor = null;
        for (Constructor<?> ctor : ctors) {
            if (ctor.getParameterCount() == 0) {
                // exists the no-arg constructor, use it
                parentCtor = ctor;
                break;
            } else if (ctor.isAnnotationPresent(Inject.class)) {
                // exists a CDI bean constructor, use it
                parentCtor = ctor;
            }
        }

        if (parentCtor == null) {
            // Should never be thrown.
            throw new DeploymentException("A class " + superClass.getName() + " doesn't have any appropriate constructor");
        }

        return parentCtor;
    }
}
