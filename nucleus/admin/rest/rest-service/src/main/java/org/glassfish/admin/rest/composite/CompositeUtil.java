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

import com.sun.enterprise.v3.common.ActionReporter;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.admin.rest.RestExtension;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.Attribute;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
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

    /**
     * This method will return a generated concrete class that implements the interface requested, as well as any
     * interfaces intended to extend the base model interface. The intent to extend the model is shown via annotations
     * yet to be developed. Currently, this API requires the caller to specify all the desired interfaces, though this
     * requirement will be removed with the full integration of HK2 2.x into GlassFish.
     *
     * @param modelIface The base interface for the desired data model
     * @param similarClass the Class for the calling code, used to load the generated class into the Classloader
     * @param extensions An array of the interfaces, excluding the base interface, to implement
     * @return An instance of a concrete class implementing the requested interfaces
     * @throws Exception
     */
    public synchronized static <T> T getModel(Class<T> modelIface, Class similarClass,
                                              Class<?>[] extensions) throws Exception {
        String className = modelIface.getName() + "Impl";
        if (!generatedClasses.containsKey(className)) {
            // TODO: This will be replace by HK2 code, once the HK2 integration is completed
//            Class<?>[] interfaces = new Class<?>[]{
//                clazz,
//                ClusterExtension.class
//            };
            Map<String, Map<String, Object>> properties = new HashMap<String, Map<String, Object>>();

            Set<Class<?>> interfaces = new HashSet<Class<?>>(Arrays.asList(extensions));
            interfaces.add(modelIface);

            for (Class<?> iface : interfaces) {
                analyzeInterface(iface, properties);
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            visitClass(classWriter, className, interfaces, properties);

            for (Map.Entry<String, Map<String, Object>> entry : properties.entrySet()) {
                String name = entry.getKey();
                Class<?> type = (Class<?>) entry.getValue().get("type");
                createField(classWriter, name, type);
                createGettersAndSetters(classWriter, modelIface, className, name, type);

            }

            createConstructor(classWriter, className, properties);
            classWriter.visitEnd();
            Class<?> newClass = defineClass(similarClass, className, classWriter.toByteArray());
            generatedClasses.put(className, newClass);
        }

        return (T) generatedClasses.get(className).newInstance();
    }

    private static void analyzeInterface(Class<?> iface, Map<String, Map<String, Object>> properties) throws SecurityException {
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

    // TODO: method enum?
    /**
     * Find and execute all resource extensions for the specified base resource and HTTP method
     *
     * @param habitat
     * @param baseClass
     * @param data
     * @param method
     */
    public static Object getResourceExtensions(Class<?> baseClass, Object data, String method) {
        List<RestExtension> extensions = new ArrayList<RestExtension>();

        for (RestExtension extension : Globals.getDefaultBaseServiceLocator().getAllByContract(RestExtension.class)) {
            if (baseClass.getName().equals(extension.getParent())) {
                extensions.add(extension);
            }
        }
        
        if ("get".equalsIgnoreCase(method)) {
            handleGetExtensions(extensions, data);
        } else if ("post".equalsIgnoreCase(method)) {
             return handlePostExtensions(extensions, data);
        }

        return void.class;
    }

    public static ParameterMap addToParameterMap(ParameterMap parameters, String basePath,
                                   Class<?> configBean, Object source) {
        String name;
        Map<String, String> currentValues = Util.getCurrentValues(basePath, Globals.getDefaultBaseServiceLocator());
        for (Method cbMethod : configBean.getMethods()) {
            name = cbMethod.getName();
            if (name.startsWith("set")/* && (cbMethod.getAnnotation(Attribute.class) !=null)*/) {
                String getterName = "get" + name.substring(3, 4).toUpperCase() + name.substring(4);
                try {
                    Method getter = source.getClass().getMethod(getterName);
                    final String key = ResourceUtil.convertToXMLName(name.substring(3));
                    final Object value = getter.invoke(source);
                    if (value != null) {
                        String currentValue = currentValues.get(basePath + key);

                        if ((currentValue == null) || "".equals(value) || (!currentValue.equals(value))) {
                            parameters.add("DEFAULT", basePath + "." + key + "=" + value);
                        }
                    }
                } catch (Exception ex) {
//                    Logger.getLogger(CompositeUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return parameters;
    }

    public static ActionReporter executeCommand(String command, ParameterMap parameters) {
        RestActionReporter ar;
        if (!parameters.entrySet().isEmpty()) {
            ar = ResourceUtil.runCommand(command, parameters, Globals.getDefaultBaseServiceLocator(), ""); //TODO The last parameter is resultType and is not used. Refactor the called method to remove it
            if (ar.getActionExitCode().equals(ExitCode.FAILURE)) {
                throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(ar.getCombinedMessage()).
                        build());
            }
        } else {
            ar = new RestActionReporter();
            ar.setActionExitCode(ExitCode.SUCCESS);
        }
        return ar;
    }

    private static void handleGetExtensions(List<RestExtension> extensions, Object data) {
        for (RestExtension re : extensions) {
            re.get(data);
        }
    }

    private static ParameterMap handlePostExtensions(List<RestExtension> extensions, Object data) {
        ParameterMap parameters = new ParameterMap();
        for (RestExtension re : extensions) {
            parameters.mergeAll(re.post(data));
        }
        return parameters;
    }

    /**
     * This builds the representation of a type suitable for use in bytecode. For example, the internal type for String
     * would be "L;java/lang/String;", and a double would be "D".
     *
     * @param type The desired class
     * @return
     */
    protected static String getInternalTypeString(Class<?> type) {
        return type.isPrimitive()
               ? Primitive.getPrimitive(type.getName()).getInternalType()
               : ("L" + getInternalName(type.getName()) + ";");
    }

    private String getPropertyName(String name) {
        return name.substring(0,1).toLowerCase() + name.substring(1);
    }

    /**
     * This method starts the class definition, adding the JAX-B annotations to allow for marshalling via JAX-RS
     */
    protected static void visitClass(ClassWriter classWriter, String className, Set<Class<?>> ifaces, Map<String, Map<String, Object>> properties) {
        String[] ifaceNames = new String[ifaces.size()+1];
        int i = 1;
        ifaceNames[0] = getInternalName(RestModel.class.getName());
        for (Class<?> iface : ifaces) {
            ifaceNames[i++] = iface.getName().replace(".", "/");
        }
        className = getInternalName(className);
        classWriter.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className,
                          null,
                          "java/lang/Object",
                          ifaceNames);

        // Add @XmlRootElement
        classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlRootElement;", true).visitEnd();

        // Add @XmlAccessType
        AnnotationVisitor annotation = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlAccessorType;", true);
        annotation.visitEnum("value", "Ljavax/xml/bind/annotation/XmlAccessType;", "FIELD");
        annotation.visitEnd();
    }

    /**
     * This method creates the default constructor for the class. Default values are set for any @Attribute defined with
     * a defaultValue.
     */
    protected static void createConstructor(ClassWriter cw, String className, Map<String, Map<String, Object>> properties) {
        // Create the ctor
        MethodVisitor method = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        for (Map.Entry<String, Map<String, Object>> property : properties.entrySet()) {
            String fieldName = property.getKey();
            String defaultValue = (String) property.getValue().get("defaultValue");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                setDefaultValue(method, className, fieldName, (Class<?>) property.getValue().get("type"), defaultValue);
            }
        }

        method.visitInsn(RETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
    }

    /*
     * This method generates the byte code to set the default value for a given field. Efforts are made to determine the
     * best way to create the correct value. If the field is a primitive, the one-arg, String constructor of the
     * appropriate wrapper class is called to generate the value. If the field is not a primitive, a one-arg, String
     * constructor is requested to build the value. If both of these attempts fail, the default value is set using the
     * String representation as given via the @Attribute annotation.
     *
     * TODO: it may make sense to treat primitives here as non-String types.
     */
    protected static void setDefaultValue(MethodVisitor method, String className, String fieldName, Class<?> fieldClass, String defaultValue) {
        final String type = getInternalTypeString(fieldClass);
        Object value = defaultValue;

        if (fieldClass.isPrimitive()) {
            switch (Primitive.getPrimitive(type)) {
                case SHORT:
                    value = Short.valueOf(defaultValue);
                    break;
                case LONG:
                    value = Long.valueOf(defaultValue);
                    break;
                case INT:
                    value = Integer.valueOf(defaultValue);
                    break;
                case FLOAT:
                    value = Float.valueOf(defaultValue);
                    break;
                case DOUBLE:
                    value = Double.valueOf(defaultValue);
                    break;
//                case CHAR: value = Character.valueOf(defaultValue.charAt(0)); break;
                case BYTE:
                    value = Byte.valueOf(defaultValue);
                    break;
                case BOOLEAN:
                    value = Boolean.valueOf(defaultValue);
                    break;
            }
            method.visitVarInsn(ALOAD, 0);
            method.visitLdcInsn(value);
            method.visitFieldInsn(PUTFIELD, getInternalName(className), fieldName, type);
        } else {
            if (!fieldClass.equals(String.class)) {
                method.visitVarInsn(ALOAD, 0);
                final String internalName = getInternalName(fieldClass.getName());
                method.visitTypeInsn(NEW, internalName);
                method.visitInsn(DUP);
                method.visitLdcInsn(defaultValue);
                method.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "(Ljava/lang/String;)V");
                method.visitFieldInsn(PUTFIELD, getInternalName(className), fieldName, type);
            } else {
                method.visitVarInsn(ALOAD, 0);
                method.visitLdcInsn(value);
                method.visitFieldInsn(PUTFIELD, getInternalName(className), fieldName, type);
            }
        }
    }

    /**
     * Add the field to the class, adding the @XmlAttribute annotation for marshalling purposes.
     */
    protected static void createField(ClassWriter cw, String name, Class<?> type) {
        String internalType = getInternalTypeString(type);
        FieldVisitor field = cw.visitField(ACC_PRIVATE, name, internalType, null, null);
        field.visitAnnotation("Ljavax/xml/bind/annotation/XmlAttribute;", true).visitEnd();
        field.visitEnd();
    }

    /**
     * Create getters and setters for the given field
     */
    protected static void createGettersAndSetters(ClassWriter cw, Class c, String className, String name, Class<?> type) {
        String internalType = getInternalTypeString(type);
        className = getInternalName(className);

        // Create the getter
        MethodVisitor getter = cw.visitMethod(ACC_PUBLIC, "get" + name, "()" + internalType, null, null);
        getter.visitCode();
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className, name, internalType);
        getter.visitInsn(type.isPrimitive()
                         ? Primitive.getPrimitive(internalType).getReturnOpcode()
                         : ARETURN);
        getter.visitMaxs(0, 0);
        getter.visitEnd();

        // Create the setter
        MethodVisitor setter = cw.visitMethod(ACC_PUBLIC, "set" + name, "(" + internalType + ")V", null, null);
        setter.visitCode();
        setter.visitVarInsn(ALOAD, 0);
        setter.visitVarInsn(type.isPrimitive()
                            ? Primitive.getPrimitive(internalType).getSetOpCode()
                            : ALOAD, 1);
        setter.visitFieldInsn(PUTFIELD, className, name, internalType);
        setter.visitInsn(RETURN);
        setter.visitMaxs(0, 0);
        setter.visitEnd();
    }

    /**
     * Convert the dotted class name to the "internal" (bytecode) representation
     */
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
            final ClassLoader classLoader = similarClass.getClassLoader();
//                    Thread.currentThread().getContextClassLoader();
            try {
                Class<?> newClass = (Class<?>) clM.invoke(classLoader, className, byteContent, 0, byteContent.length, pd);
            } catch (Exception e) {
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
