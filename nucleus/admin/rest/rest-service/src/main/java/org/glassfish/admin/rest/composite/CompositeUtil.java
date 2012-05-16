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
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.Attribute;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;

/**
 *
 * @author jdlee
 */
public class CompositeUtil {
    private static final Map<String, Class<?>> generatedClasses = new HashMap<String, Class<?>>();

    public synchronized static <T> T getModel(Class<T> clazz, Class similarClass,
            Class<?>[] interfaces) throws Exception {
        String className = clazz.getName() + "Impl";
        if (!alreadyGenerated(className)) {
            // TODO: This will be replace by HK2 code, once the HK2 integration is completed
//            Class<?>[] interfaces = new Class<?>[]{
//                clazz,
//                ClusterExtension.class
//            };
            Map<String, Map<String, Object>> properties = new HashMap<String, Map<String, Object>>();

            for (Class<?> iface : interfaces) {
                for (Method method : iface.getMethods()) {
                    String name = method.getName();
                    final boolean isGetter = name.startsWith("get");
                    if (isGetter || name.startsWith("set")) {
                        name = name.substring(3);
                        Map<String, Object> property = properties.get(name);
                        if (property == null) {
                            property = new HashMap<String, Object>();
                            properties.put(name, property);
                        }

                        Attribute attr = method.getAnnotation(Attribute.class);
                        if (attr != null) {
                            property.put("defaultValue", attr.defaultValue());
                        }
                        Class<?> type = isGetter
                                ? method.getReturnType()
                                : method.getParameterTypes()[0];
                        property.put("type", type);
                    }
                }
            }
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            visitClass(cw, className, interfaces, properties);

             for (Map.Entry<String, Map<String, Object>> entry : properties.entrySet()) {
                String name = entry.getKey();
                Class<?> type = (Class<?>)entry.getValue().get("type");
                createField(cw, name, type);
                createGettersAndSetters(cw, clazz, className, name, type);

            }

            createConstructor(cw, className, properties);
            cw.visitEnd();
            Class<?> newClass = defineClass(similarClass, className, cw.toByteArray());
            generatedClasses.put(className, newClass);
        }

        return (T)generatedClasses.get(className).newInstance();
    }

    // TODO: method enum?
    public static void getResourceExtensions(Habitat habitat, Class<?> baseClass, Object data, String method) {
        Collection<RestExtension> extensions = habitat.getAllByContract(RestExtension.class);

        for (RestExtension extension : extensions) {
            if (baseClass.getName().equals(extension.getParent())) {
                if ("get".equalsIgnoreCase(method)) {
                    extension.get(data);
                }
            }
        }
    }

    protected static String getInternalTypeString(Class<?> type) {
        return type.isPrimitive()
                ? getPrimitiveInternalType(type.getName())//Primitive.getPrimitive(type.getName()).getInternalType()
                : ("L" + getInternalName(type.getName()) + ";");
    }

    protected static void visitClass(ClassWriter cw, String className, Class<?>[] ifaces, Map<String, Map<String, Object>> properties) {
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

    protected static void createConstructor(ClassWriter cw, String className, Map<String, Map<String, Object>> properties) {
        // Create the ctor
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        for (Map.Entry<String, Map<String, Object>> property : properties.entrySet()) {
            String fieldName = property.getKey();
            String defaultValue = (String)property.getValue().get("defaultValue");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                setDefaultValue(mv, className, fieldName, (Class<?>)property.getValue().get("type"), defaultValue);
            }
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    static enum Primitive {
        DOUBLE ("D", DRETURN, DLOAD),
        FLOAT  ("F", FRETURN, FLOAD),
        LONG   ("J", LRETURN, LLOAD),
        SHORT  ("S", IRETURN, ILOAD),
        INT    ("I", IRETURN, ILOAD),
//        CHAR   ("C", IRETURN, ILOAD),
        BYTE   ("B", IRETURN, ILOAD),
        BOOLEAN("Z", IRETURN, ILOAD);
        
        private final int returnOpcode;
        private final int setOpcode;
        private final String internalType;
        
        Primitive(String type, int returnOpcode, int setOpcode) {
            this.internalType = type;
            this.returnOpcode = returnOpcode;
            this.setOpcode = setOpcode;
        }
        public int getReturnOpcode() {
            return returnOpcode;
        }
        public int getSetOpCode() {
            return setOpcode;
        }
        public String getInternalType() {
            return internalType;
        }
        static Primitive getPrimitive (String type) {
            if ("S".equals(type) || "short".equals(type)) {
                return SHORT;
            } else if ("J".equals(type) || "long".equals(type)) {
                return LONG;
            } else if ("I".equals(type) || "int".equals(type)) {
                return INT;
            } else if ("F".equals(type) || "float".equals(type)) {
                return FLOAT;
            } else if ("D".equals(type) || "double".equals(type)) {
                return DOUBLE;
//            } else if ("C".equals(type) || "char".equals(type)) {
//                return CHAR;
            } else if ("B".equals(type) || "byte".equals(type)) {
                return BYTE;
            } else if ("Z".equals(type) || "boolean".equals(type)) {
                return BOOLEAN;
            } else {
                throw new RuntimeException ("Unknown primitive type: " + type);
            }
        }
    };

    protected static void setDefaultValue(MethodVisitor mv, String className, String fieldName, Class<?> fieldClass, String defaultValue) {
        final String type = getInternalTypeString(fieldClass);
        Object value = defaultValue;

        if (fieldClass.isPrimitive()) {
            switch (Primitive.getPrimitive(type)) {
                case SHORT: value = Short.valueOf(defaultValue); break;
                case LONG: value = Long.valueOf(defaultValue); break;
                case INT: value = Integer.valueOf(defaultValue); break;
                case FLOAT: value = Float.valueOf(defaultValue); break;
                case DOUBLE: value = Double.valueOf(defaultValue); break;
//                case CHAR: value = Character.valueOf(defaultValue.charAt(0)); break;
                case BYTE: value = Byte.valueOf(defaultValue); break;
                case BOOLEAN: value = Boolean.valueOf(defaultValue); break;
            }
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(value);
            System.out.println("CTOR: Using " + type + " for PUTFIELD " + fieldName);
            mv.visitFieldInsn(PUTFIELD, getInternalName(className), fieldName, type);
        } else {
            if (!fieldClass.equals(String.class)) {
                mv.visitVarInsn(ALOAD, 0);
                final String internalName = getInternalName(fieldClass.getName());
                mv.visitTypeInsn(NEW, internalName);
                mv.visitInsn(DUP);
                mv.visitLdcInsn(defaultValue);
                mv.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "(Ljava/lang/String;)V");
                mv.visitFieldInsn(PUTFIELD, getInternalName(className), fieldName, type);
            } else {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(value);
                mv.visitFieldInsn(PUTFIELD, getInternalName(className), fieldName, type);
            }
        }


    }

    /**
     * Add the field to the class
     */
    protected static void createField(ClassWriter cw, String name, Class<?> type) {
        // TODO: Add support for primitives
        String internalType = getInternalTypeString(type);
        FieldVisitor fv = cw.visitField(ACC_PROTECTED, name, internalType, null, null);
        fv.visitAnnotation("Ljavax/xml/bind/annotation/XmlAttribute;", true).visitEnd();
        fv.visitEnd();
    }

    protected static void createGettersAndSetters(ClassWriter cw, Class c, String className, String name, Class<?> type) {
        String internalType = getInternalTypeString(type);
        className = getInternalName(className);

        // Create the getter
        MethodVisitor getterVistor = cw.visitMethod(ACC_PUBLIC, "get" + name, "()" + internalType, null, null);
        getterVistor.visitCode();
        getterVistor.visitVarInsn(ALOAD, 0);
        getterVistor.visitFieldInsn(GETFIELD, className, name, internalType);
        int opcode = //type.isPrimitive() ?
                //Primitive.getPrimitive(internalType).getReturnOpcode() :
                ARETURN;
        if (type.isPrimitive()) {
            switch (Primitive.getPrimitive(internalType)) {
                case DOUBLE : opcode = DRETURN; break;
                case FLOAT : opcode = FRETURN; break;
                case LONG : opcode = LRETURN; break;
                case BOOLEAN:
                case BYTE:
//                case CHAR:
                case SHORT:
                case INT: opcode = IRETURN; break;
            }
        }
        getterVistor.visitInsn(opcode);
        getterVistor.visitMaxs(0, 0);
        getterVistor.visitEnd();

        // Create the setter
        MethodVisitor setterVisitor = cw.visitMethod(ACC_PUBLIC, "set" + name, "(" + internalType + ")V", null, null);
        opcode = //type.isPrimitive() ?
                //Primitive.getPrimitive(internalType).getSetOpCode() :
                ALOAD;
        if (type.isPrimitive()) {
            switch (Primitive.getPrimitive(internalType)) {
                case DOUBLE : opcode = DLOAD; break;
                case FLOAT : opcode = FLOAD; break;
                case LONG : opcode = LLOAD; break;
                case BOOLEAN:
                case BYTE:
//                case CHAR:
                case SHORT:
                case INT: opcode = ILOAD; break;
            }
        }
        setterVisitor.visitCode();
        setterVisitor.visitVarInsn(ALOAD, 0);
        setterVisitor.visitVarInsn(opcode, 1);
        System.out.println("SETTER: Using " + internalType + " for PUTFIELD " + name);
        setterVisitor.visitFieldInsn(PUTFIELD, className, name, internalType);
        setterVisitor.visitInsn(RETURN);
        setterVisitor.visitMaxs(0,0);
        setterVisitor.visitEnd();
    }

    protected static boolean alreadyGenerated(String className) {
        return generatedClasses.containsKey(className);
    }

    protected static String getInternalName(String className) {
        return className.replace(".", "/");
    }

    protected static String getPrimitiveInternalType(String type) {
        if (type.equals("int")) {
            return "I";
        } else if (type.equals("float")) {
            return "F";
        } else if (type.equals("double")) {
            return "D";
        } else if (type.equals("short")) {
            return "S";
        } else if (type.equals("long")) {
            return "J";
        } else if (type.equals("byte")) {
            return "B";
        } else if (type.equals("char")) {
            return "C";
        } else if (type.equals("boolean")) {
            return "Z";
        } else {
            throw new RuntimeException("Unexpected primitve type: " + type);
        }
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
