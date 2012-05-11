/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.composite;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.admin.rest.RestExtension;
import org.glassfish.pfl.objectweb.asm.Type;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.Attribute;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

/**
 *
 * @author jdlee
 */
public class CompositeUtil {
//    private static final String PACKAGE_NAME = "org.glassfish.admin.rest.model.proxy";
    private static final Map<String, Class<?>> generatedClasses = new HashMap<String, Class<?>>();

    public synchronized static <T> T getModel(Class<T> clazz, Class similarClass,
            Class<?>[] interfaces) throws Exception {
        String className = //PACKAGE_NAME + "." +
                clazz.getName() + "Impl";
        if (!alreadyGenerated(className)) {
            // TODO: This will be replace by HK2 code, once the HK2 integration is completed
//            Class<?>[] interfaces = new Class<?>[]{
//                clazz,
//                ClusterExtension.class
//            };
            Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();

            for (Class<?> iface : interfaces) {
                for (Method method : iface.getMethods()) {
                    String name = method.getName();
                    final boolean isGetter = name.startsWith("get");
                    if (isGetter || name.startsWith("set")) {
                        name = name.substring(3);
                        Map<String, String> property = properties.get(name);
                        if (property == null) {
                            property = new HashMap<String, String>();
                            properties.put(name, property);
                        }

                        String type = "String";
                        Attribute attr = method.getAnnotation(Attribute.class);
                        if (attr != null) {
                            property.put("type", attr.dataType().getCanonicalName());
                            property.put("defaultValue", attr.defaultValue());
                        } else {
                            property.put("type", isGetter ?
                                method.getReturnType().getCanonicalName() :
                                method.getParameterTypes()[0].getCanonicalName());
                        }
                    }
                }
            }
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            visitClass(cw, className, interfaces, properties);

//            for (Map.Entry<String, String> entry : properties.entrySet()) {
             for (Map.Entry<String, Map<String, String>> entry : properties.entrySet()) {
                String name = entry.getKey();
                String type = entry.getValue().get("type");
                createField(cw, name, type);
                createGettersAndSetters(cw, clazz, className, name, type);

            }

            createConstructor(cw, className, properties);
            cw.visitEnd();
            Class<?> newClass = defineClass(similarClass, className, cw.toByteArray());
            generatedClasses.put(className, newClass);
        }

        return (T)generatedClasses.get(className).newInstance();
                //similarClass.forName(className).newInstance();
    }

    // TODO: method enum?
    public static void getResourceExtensions(Habitat habitat, Class<?> baseClass,
            Object data, String method) {
        Collection<RestExtension> extensions = habitat.getAllByContract(RestExtension.class);

        for (RestExtension extension : extensions) {
            if (baseClass.getName().equals(extension.getParent())) {
                if ("get".equalsIgnoreCase(method)) {
                    extension.get(data);
                }
            }
        }
        //.get(foo);
    }

    protected static void visitClass(ClassWriter cw, String className, Class<?>[] ifaces, Map<String, Map<String, String>> properties) {
        String[] ifaceNames = new String[ifaces.length];
        int i = 0;
        for (Class<?> iface : ifaces) {
            ifaceNames[i++] = iface.getName().replace(".", "/");
        }
        className = getInternalName(className);
        cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className,
                null,
                "java/lang/Object",
                ifaceNames);

        // Add @XmlRootElement
        cw.visitAnnotation("Ljavax/xml/bind/annotation/XmlRootElement;", true).visitEnd();

        // Add @XmlAccessType
        AnnotationVisitor av0 = cw.visitAnnotation("Ljavax/xml/bind/annotation/XmlAccessorType;", true);
        av0.visitEnum("value", "Ljavax/xml/bind/annotation/XmlAccessType;", "FIELD");
        av0.visitEnd();
    }

    protected static void createConstructor(ClassWriter cw, String className, Map<String, Map<String, String>> properties) {
        // Create the ctor
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        for (Map.Entry<String, Map<String, String>> property : properties.entrySet()) {
            String name = property.getKey();
            String defaultValue = property.getValue().get("defaultValue");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                final String type = getInternalName(property.getValue().get("type"));
                mv.visitVarInsn(ALOAD, 0);
                mv.visitTypeInsn(NEW, type);
                mv.visitInsn(DUP);
                mv.visitLdcInsn(defaultValue);
                mv.visitMethodInsn(INVOKESPECIAL, type, "<init>", "(Ljava/lang/String;)V");
                mv.visitFieldInsn(PUTFIELD, getInternalName(className), name, "L" + type + ";");

            }
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Add the field to the class
     */
    protected static void createField(ClassWriter cw, String name, String type) {
        // TODO: Add support for primitives
        FieldVisitor fv = cw.visitField(ACC_PUBLIC, name, "L" + getInternalName(type) + ";", null, null);
        fv.visitAnnotation("Ljavax/xml/bind/annotation/XmlAttribute;", true).visitEnd();
        fv.visitEnd();
    }

    protected static void createGettersAndSetters(ClassWriter cw, Class c, String className, String name, String type) {
        // TODO: Add support for primitives
        type = getInternalName(type);
        className = getInternalName(className);

        // Create the getter
        MethodVisitor getterVistor = cw.visitMethod(ACC_PUBLIC, "get" + name, "()L" + type + ";", null, null);
        getterVistor.visitCode();
        getterVistor.visitVarInsn(ALOAD, 0);
        getterVistor.visitFieldInsn(GETFIELD, className, name, "L" + type + ";");
        getterVistor.visitInsn(Type.getType(c).getOpcode(IRETURN));
        getterVistor.visitMaxs(0, 0);
        getterVistor.visitEnd();

        // Create the setter
        MethodVisitor setterVisitor = cw.visitMethod(ACC_PUBLIC, "set" + name, "(L" + type + ";)V", null, null);
        setterVisitor.visitCode();
        setterVisitor.visitVarInsn(ALOAD, 0);
        setterVisitor.visitVarInsn(Type.getType(c).getOpcode(ILOAD), 1);
        setterVisitor.visitFieldInsn(PUTFIELD, className, name, "L" + type + ";");
        setterVisitor.visitInsn(RETURN);
        setterVisitor.visitMaxs(0, 0);
        setterVisitor.visitEnd();
    }

    protected static boolean alreadyGenerated(String className) {
        return generatedClasses.containsKey(className);
    }

    protected static String getInternalName(String className) {
        return className.replace(".", "/");
    }

    // TODO: This is duplicated from the generator class.  
    protected static Class<?> defineClass(Class similarClass, String className, byte[] classBytes) throws Exception {
        byte[] byteContent = classBytes;
        ProtectionDomain pd = similarClass.getProtectionDomain();

        java.lang.reflect.Method jm = null;
        for (java.lang.reflect.Method jm2 : ClassLoader.class.getDeclaredMethods()) {
            if (jm2.getName().equals("defineClass") && jm2.getParameterTypes().length == 5) {
                jm = jm2;
                break;
            }
        }
        if (jm == null) {//should never happen, makes findbug happy
            throw new RuntimeException("cannot find method called defineclass...");
        }
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

            Logger.getLogger(CompositeUtil.class.getName()).log(Level.FINE, "Loading bytecode for {0}", className);
            final ClassLoader classLoader =
                    similarClass.getClassLoader();
//                    Thread.currentThread().getContextClassLoader();
            try {
                Class<?> newClass = (Class<?>)clM.invoke(
                        classLoader,
                        //                    Thread.currentThread().getContextClassLoader(),
                        className, byteContent, 0,
                        byteContent.length, pd);
                System.out.println(newClass.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException cnfEx) {
                throw new RuntimeException(cnfEx);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
}
