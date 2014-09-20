/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.codegen;

import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;
import com.sun.ejb.spi.container.OptionalLocalInterfaceProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.Permission;

import com.sun.enterprise.deployment.util.TypeUtil;

public class EjbOptionalIntfGenerator
    implements Opcodes {

    private static final int INTF_FLAGS = ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES;

    private static final String DELEGATE_FIELD_NAME = "__ejb31_delegate";

    private static final Class[] emptyClassArray = new Class[] {};

    private Map<String, byte[]> classMap = new HashMap<String, byte[]>();

    private ClassLoader loader;

    private ProtectionDomain protectionDomain;

    public EjbOptionalIntfGenerator(ClassLoader loader) {
        this.loader = loader;
    }

    public Class loadClass(final String name)
        throws ClassNotFoundException
    {
        Class clz = null;

        try {
            clz = loader.loadClass(name);
        } catch(ClassNotFoundException cnfe) {

            final byte[] classData = (byte []) classMap.get(name);

            if (classData != null) {

                clz = (Class) java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                            public java.lang.Object run() {
                                return makeClass(name, classData, protectionDomain, loader);
                            }
                        }
                );
            }
        }

        if( clz == null ) {

            throw new ClassNotFoundException(name);
        }

        return clz;       
    }

    public void generateOptionalLocalInterface(Class ejbClass, String intfClassName)
        throws Exception {

        generateInterface(ejbClass, intfClassName, Serializable.class);
    }

    public void generateInterface(Class ejbClass, String intfClassName, final Class... interfaces) throws Exception {
        String[] interfaceNames = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = Type.getType(interfaces[i]).getInternalName();
        }

        if( protectionDomain == null ) {
            protectionDomain = ejbClass.getProtectionDomain();
        }

        ClassWriter cw = new ClassWriter(INTF_FLAGS);

//        ClassVisitor tv = (_debug)
//                ? new TraceClassVisitor(cw, new PrintWriter(System.out)) : cw;
        ClassVisitor tv = cw;
        String intfInternalName = intfClassName.replace('.', '/');
        tv.visit(V1_1, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
                intfInternalName, null,
                Type.getType(Object.class).getInternalName(), 
                interfaceNames );

        for (java.lang.reflect.Method m : ejbClass.getMethods()) {
            if (qualifiedAsBeanMethod(m)) {
                generateInterfaceMethod(tv, m);
            }
        }

        tv.visitEnd();

        byte[] classData = cw.toByteArray();
        classMap.put(intfClassName, classData);
    }
    
    /**
     * Determines if a method from a bean class can be considered as a business
     * method for EJB of no-interface view.
     * @param m a public method
     * @return true if m can be included as a bean business method.
     */
    private boolean qualifiedAsBeanMethod(java.lang.reflect.Method m) {
        if (m.getDeclaringClass() == Object.class) {
            return false;
        }
        int mod = m.getModifiers();
        return !Modifier.isStatic(mod) && !Modifier.isFinal(mod);
    }

    private boolean hasSameSignatureAsExisting(java.lang.reflect.Method toMatch,
                                               Set<java.lang.reflect.Method> methods) {
        boolean sameSignature = false;
        for(java.lang.reflect.Method m : methods) {
            if( TypeUtil.sameMethodSignature(m, toMatch) ) {
                sameSignature = true;
                break;
            }
        }
        return sameSignature;
    }

    public void generateOptionalLocalInterfaceSubClass(Class superClass, String subClassName,
                                                       Class delegateClass)
        throws Exception {

        generateSubclass(superClass, subClassName, delegateClass, IndirectlySerializable.class);
    }

    public void generateSubclass(Class superClass, String subClassName, Class delegateClass, Class... interfaces)
            throws Exception {


        if( protectionDomain == null ) {
            protectionDomain = superClass.getProtectionDomain();
        }

        ClassWriter cw = new ClassWriter(INTF_FLAGS);

       ClassVisitor tv = cw;
//        ClassVisitor tv = (_debug)
//                ? new TraceClassVisitor(cw, new PrintWriter(System.out)) : cw;
        
        String[] interfaceNames = new String[interfaces.length + 1];
        interfaceNames[0] = OptionalLocalInterfaceProvider.class.getName().replace('.', '/');
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i+1] = interfaces[i].getName().replace('.', '/');
        }

        tv.visit(V1_1, ACC_PUBLIC, subClassName.replace('.', '/'), null,
                Type.getType(superClass).getInternalName(), interfaceNames);

        String fldDesc = Type.getDescriptor(delegateClass);
        FieldVisitor fv = tv.visitField(ACC_PRIVATE, DELEGATE_FIELD_NAME,
                fldDesc, null, null);
        fv.visitEnd();

	// Generate constructor. The EJB spec only allows no-arg constructors, but
	// JSR 299 added requirements that allow a single constructor to define
	// parameters injected by CDI.
	{

	    Constructor[] ctors = superClass.getConstructors();
	    Constructor ctorWithParams = null;
	    for(Constructor ctor : ctors) {
		if(ctor.getParameterTypes().length == 0) {
                    ctorWithParams = null;    //exists the no-arg ctor, use it
                    break;
                } else if(ctorWithParams == null) {
		    ctorWithParams = ctor;
		}
	    }

	    MethodVisitor cv = tv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
	    cv.visitVarInsn(ALOAD, 0);
	    String paramTypeString = "()V";
	    // if void, only one param (implicit 'this' param)
	    int maxValue = 1;
	    if( ctorWithParams != null ) {
		Class[] paramTypes = ctorWithParams.getParameterTypes();
		for(int i = 0; i < paramTypes.length; i++) {
		    cv.visitInsn(ACONST_NULL);
		}
		paramTypeString = Type.getConstructorDescriptor(ctorWithParams);
		// num params + one for 'this' pointer
		maxValue = paramTypes.length + 1;
	    }
	    cv.visitMethodInsn(INVOKESPECIAL,  Type.getType(superClass).getInternalName(), "<init>",
			   paramTypeString);
	    cv.visitInsn(RETURN);
	    cv.visitMaxs(maxValue, 1);
	}

        generateSetDelegateMethod(tv, delegateClass, subClassName);

        for (Class anInterface : interfaces) {

            // dblevins: Don't think we need this special case.
            // Should be covered by letting generateBeanMethod
            // handle the methods on IndirectlySerializable.
            //
            // Not sure where the related tests are to verify.
            if (anInterface.equals(IndirectlySerializable.class)) {
                generateGetSerializableObjectFactoryMethod(tv, fldDesc, subClassName.replace('.', '/'));
                continue;
            }

            for (java.lang.reflect.Method method : anInterface.getMethods()) {
                generateBeanMethod(tv, subClassName, method, delegateClass);
            }
        }


        Set<java.lang.reflect.Method> allMethods = new HashSet<java.lang.reflect.Method>();
        
        for (java.lang.reflect.Method m : superClass.getMethods()) {
            if (qualifiedAsBeanMethod(m)) {
                generateBeanMethod(tv, subClassName, m, delegateClass);
            }
        }
        
        for (Class clz = superClass; clz != Object.class; clz = clz.getSuperclass()) {
            java.lang.reflect.Method[] beanMethods = clz.getDeclaredMethods();
            for (java.lang.reflect.Method mth : beanMethods) {
                if( !hasSameSignatureAsExisting(mth, allMethods)) {
                    int modifiers = mth.getModifiers();
                    boolean isPublic = Modifier.isPublic(modifiers);
                    boolean isPrivate = Modifier.isPrivate(modifiers);
                    boolean isProtected = Modifier.isProtected(modifiers);
                    boolean isPackage = !isPublic && !isPrivate && !isProtected;

                    boolean isStatic = Modifier.isStatic(modifiers);

                    if( (isPackage || isProtected) && !isStatic ) {
                        generateNonAccessibleMethod(tv, mth);
                    }                    
                    allMethods.add(mth);
                }
            }
        }

        // add toString() method if it was not overridden
        java.lang.reflect.Method mth = Object.class.getDeclaredMethod("toString");
        if( !hasSameSignatureAsExisting(mth, allMethods)) {
                        //generateBeanMethod(tv, subClassName, mth, delegateClass);
            generateToStringBeanMethod(tv, superClass);
        }

        tv.visitEnd();

        byte[] classData = cw.toByteArray();
        classMap.put(subClassName, classData);
    }


    private static void generateInterfaceMethod(ClassVisitor cv, java.lang.reflect.Method m)
        throws Exception {

        String methodName = m.getName();
        Type returnType = Type.getReturnType(m);
        Type[] argTypes = Type.getArgumentTypes(m);

        Method asmMethod = new Method(methodName, returnType, argTypes);
        GeneratorAdapter cg = new GeneratorAdapter(ACC_PUBLIC + ACC_ABSTRACT,
                asmMethod, null, getExceptionTypes(m), cv);
        cg.endMethod();

    }

    private static void generateBeanMethod(ClassVisitor cv, String subClassName,
                                           java.lang.reflect.Method m, Class delegateClass)
        throws Exception {

        String methodName = m.getName();
        Type returnType = Type.getReturnType(m);
        Type[] argTypes = Type.getArgumentTypes(m);
        Method asmMethod = new Method(methodName, returnType, argTypes);

        GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, asmMethod, null,
                getExceptionTypes(m), cv);
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, subClassName.replace('.', '/'),
                DELEGATE_FIELD_NAME, Type.getType(delegateClass).getDescriptor());
        mg.loadArgs();
        mg.invokeInterface(Type.getType(delegateClass), asmMethod);
        mg.returnValue();
        mg.endMethod();

    }

    private static void generateToStringBeanMethod(ClassVisitor cv, Class superClass)
        throws Exception {

        String toStringMethodName = "toString";
        String toStringMethodDescriptor = "()Ljava/lang/String;";
        String stringBuilder = "java/lang/StringBuilder";

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, toStringMethodName, toStringMethodDescriptor, null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, stringBuilder);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, stringBuilder, "<init>", "()V");
        mv.visitLdcInsn(superClass.getName() + "@");
        mv.visitMethodInsn(INVOKEVIRTUAL, stringBuilder, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toHexString", "(I)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, stringBuilder, "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        mv.visitMethodInsn(INVOKEVIRTUAL, stringBuilder, toStringMethodName, toStringMethodDescriptor);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 1);

    }

    private static void generateNonAccessibleMethod(ClassVisitor cv,
                                           java.lang.reflect.Method m)
        throws Exception {

        String methodName = m.getName();
        Type returnType = Type.getReturnType(m);
        Type[] argTypes = Type.getArgumentTypes(m);
        Method asmMethod = new Method(methodName, returnType, argTypes);

        // Only called for non-static Protected or Package access
        int access =  ACC_PUBLIC;

        GeneratorAdapter mg = new GeneratorAdapter(access, asmMethod, null,
                getExceptionTypes(m), cv);

        mg.throwException(Type.getType(javax.ejb.EJBException.class),
                "Illegal non-business method access on no-interface view");
        
        mg.returnValue();
        
        mg.endMethod();

    }

    private static void generateGetSerializableObjectFactoryMethod(ClassVisitor classVisitor,
                                                                   String fieldDesc,
                                                                   String classDesc) {

        MethodVisitor cv = classVisitor.visitMethod(ACC_PUBLIC, "getSerializableObjectFactory",
                "()L" + SerializableObjectFactory.class.getName().replace('.', '/') + ";", null, new String[] { "java/io/IOException" });
        cv.visitVarInsn(ALOAD, 0);
        cv.visitFieldInsn(GETFIELD, classDesc, DELEGATE_FIELD_NAME, fieldDesc);
        cv.visitTypeInsn(CHECKCAST, IndirectlySerializable.class.getName().replace('.', '/'));
        cv.visitMethodInsn(INVOKEINTERFACE,
                IndirectlySerializable.class.getName().replace('.', '/'), "getSerializableObjectFactory",
                "()L" + SerializableObjectFactory.class.getName().replace('.', '/') + ";");
        cv.visitInsn(ARETURN);
        cv.visitMaxs(1, 1);

        
    }


    private static Type[] getExceptionTypes(java.lang.reflect.Method m) {
        Class[] exceptions = m.getExceptionTypes();
        Type[] eTypes = new Type[exceptions.length];
        for (int i=0; i<exceptions.length; i++) {
            eTypes[i] = Type.getType(exceptions[i]);
        }

        return eTypes;
    }

    private static void generateSetDelegateMethod(ClassVisitor cv, Class delegateClass,
                                                  String subClassName)
        throws Exception {

        Class optProxyClass = OptionalLocalInterfaceProvider.class;
        java.lang.reflect.Method proxyMethod = optProxyClass.getMethod(
                "setOptionalLocalIntfProxy", java.lang.reflect.Proxy.class);

        String methodName = proxyMethod.getName();
        Type returnType = Type.getReturnType(proxyMethod);
        Type[] argTypes = Type.getArgumentTypes(proxyMethod);
        Type[] eTypes = getExceptionTypes(proxyMethod);

        Method asmMethod = new Method(methodName, returnType, argTypes);
        GeneratorAdapter mg2 = new GeneratorAdapter(ACC_PUBLIC, asmMethod, null, eTypes, cv);
        mg2.visitVarInsn(ALOAD, 0);
        mg2.visitVarInsn(ALOAD, 1);
        mg2.visitTypeInsn(CHECKCAST, delegateClass.getName().replace('.', '/'));
        String delIntClassDesc = Type.getType(delegateClass).getDescriptor();
        mg2.visitFieldInsn(PUTFIELD, subClassName.replace('.', '/'),
                DELEGATE_FIELD_NAME, delIntClassDesc);
        mg2.returnValue();
        mg2.endMethod();
    }

     // A Method for the protected ClassLoader.defineClass method, which we access
    // using reflection.  This requires the supressAccessChecks permission.
    private static final java.lang.reflect.Method defineClassMethod = AccessController.doPrivileged(
	new PrivilegedAction<java.lang.reflect.Method>() {
	    public java.lang.reflect.Method run() {
		try {
		    java.lang.reflect.Method meth = ClassLoader.class.getDeclaredMethod(
			"defineClass", String.class,
			byte[].class, int.class, int.class,
			ProtectionDomain.class ) ;
		    meth.setAccessible( true ) ;
		    return meth ;
		} catch (Exception exc) {
		    throw new RuntimeException(
			"Could not find defineClass method!", exc ) ;
		}
	    }
	}
    ) ;

    private static final Permission accessControlPermission =
	    new ReflectPermission( "suppressAccessChecks" ) ;

    // This requires a permission check
    private Class<?> makeClass( String name, byte[] def, ProtectionDomain pd,
	    ClassLoader loader ) {

	SecurityManager sman = System.getSecurityManager() ;
	if (sman != null)
	    sman.checkPermission( accessControlPermission ) ;

	try {
	    return (Class)defineClassMethod.invoke( loader,
		name, def, 0, def.length, pd ) ;
	} catch (Exception exc) {
	    throw new RuntimeException( "Could not invoke defineClass!",
		exc ) ;
	}
    }
}
