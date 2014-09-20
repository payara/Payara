/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.common.ActionReporter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import javax.security.auth.Subject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.RestExtension;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.composite.metadata.AttributeReference;
import org.glassfish.admin.rest.composite.metadata.DefaultBeanReference;
import org.glassfish.admin.rest.composite.metadata.HelpText;
import org.glassfish.admin.rest.utils.JsonUtil;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.SseCommandHelper;
import org.glassfish.admin.rest.utils.StringUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.media.sse.EventOutput;

import static org.glassfish.pfl.objectweb.asm.Opcodes.*;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.MessageInterpolatorImpl;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * @author jdlee
 */
public class CompositeUtil {
    private static final Map<String, Class<?>> generatedClasses = new HashMap<String, Class<?>>();
    private static final Map<String, List<String>> modelExtensions = new HashMap<String, List<String>>();
    private boolean extensionsLoaded = false;
    private static volatile Validator beanValidator = null;
    private static final LocalStringManagerImpl adminStrings = new LocalStringManagerImpl(CompositeUtil.class);

    private CompositeUtil() {
    }

    private static class LazyHolder {
        public static final CompositeUtil INSTANCE = new CompositeUtil();
    }

    public static CompositeUtil instance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * This method will return a generated concrete class that implements the interface requested, as well as any
     * interfaces intended to extend the base model interface.  Model extensions must be annotated with
     *
     * @param modelIface   The base interface for the desired data model
     * @return An instance of a concrete class implementing the requested interfaces
     * @throws Exception
     * @RestModelExtension, and must be compiled with rest-annotation-processor library on the compile classpath
     * for metadata generation.
     */
    public synchronized <T> T getModel(Class<T> modelIface) {
        String className = modelIface.getName() + "Impl";
        if (!generatedClasses.containsKey(className)) {
            Map<String, Map<String, Object>> properties = new HashMap<String, Map<String, Object>>();

            Set<Class<?>> interfaces = getModelExtensions(modelIface);
            interfaces.add(modelIface);

            for (Class<?> iface : interfaces) {
                analyzeInterface(iface, properties);
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            visitClass(classWriter, className, interfaces, properties);

            for (Map.Entry<String, Map<String, Object>> entry : properties.entrySet()) {
                String name = entry.getKey();
                final Map<String, Object> property = entry.getValue();
                Class<?> type = (Class<?>) property.get("type");
                createField(classWriter, name, type);
                createGettersAndSetters(classWriter, modelIface, className, name, property);

            }

            createConstructor(classWriter, className, properties);
            classWriter.visitEnd();
            Class<?> newClass;
            try {
                newClass = defineClass(modelIface, className, classWriter.toByteArray());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            generatedClasses.put(className, newClass);
        }
        try {
            return (T) generatedClasses.get(className).newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

        public Set<Class<?>> getRestModels() {
        Set<Class<?>>classes = new HashSet<Class<?>>();
        for (ActiveDescriptor ad : Globals.getDefaultBaseServiceLocator()
                .getDescriptors(BuilderHelper.createContractFilter(RestModel.class.getName()))) {
            try {
                classes.add(CompositeUtil.instance().getModel(Class.forName(ad.getImplementation())).getClass());
            } catch (ClassNotFoundException ex) {
                RestLogging.restLogger.log(Level.SEVERE, null, ex);
            }
        }

        return classes;
    }

    /**
     * Find and execute all resource extensions for the specified base resource and HTTP method
     * TODO: method enum?
     *
     * @param baseClass
     * @param data
     * @param method
     */
    public Object getResourceExtensions(Class<?> baseClass, Object data, String method) {
        List<RestExtension> extensions = new ArrayList<RestExtension>();

        for (RestExtension extension : Globals.getDefaultHabitat().<RestExtension>getAllServices(RestExtension.class)) {
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

    public ParameterMap addToParameterMap(ParameterMap parameters, String basePath,
            Class<?> configBean, Object source, Subject subject) {
        String name;
        Map<String, String> currentValues =
                Util.getCurrentValues(basePath, Globals.getDefaultHabitat(), subject);
        for (Method cbMethod : configBean.getMethods()) {
            name = cbMethod.getName();
            if (name.startsWith("set")/* && (cbMethod.getAnnotation(Attribute.class) !=null)*/) {
                String getterName = "get" + name.substring(3, 4).toUpperCase(Locale.getDefault()) + name.substring(4);
                try {
                    Method getter = source.getClass().getMethod(getterName);
                    final String key = ResourceUtil.convertToXMLName(name.substring(3));
                    Object value = null;
                    try {
                        value = getter.invoke(source);
                    } catch (Exception ex) {
                        RestLogging.restLogger.log(Level.SEVERE, null, ex);
                    }
                    if (value != null) {
                        String currentValue = currentValues.get(basePath + key);

                        if ((currentValue == null) || "".equals(value) || (!currentValue.equals(value))) {
                            parameters.add("DEFAULT", basePath + "." + key + "=" + value);
                        }
                    }
                } catch (NoSuchMethodException ex) {
                    RestLogging.restLogger.log(Level.FINE, null, ex);
                }
            }
        }

        return parameters;
    }

    /**
     * Convert the given <code>RestModel</code> encoded as JSON to a live Java Object.
     *
     * @param modelClass The target <code>RestModel</code> type
     * @param json       The json encoding of the object
     * @return
     */
    public <T> T unmarshallClass(Locale locale, Class<T> modelClass, JSONObject json) throws JSONException {
        T model = getModel(modelClass);
        for (Method setter : getSetters(modelClass)) {
            String name = setter.getName();
            String attribute = name.substring(3, 4).toLowerCase(Locale.getDefault()) + name.substring(4);
            Type param0 = setter.getGenericParameterTypes()[0];
            Class class0 = setter.getParameterTypes()[0];
            if (json.has(attribute)) {
                java.lang.Object o = json.get(attribute);
                if (JSONArray.class.isAssignableFrom(o.getClass())) {
                    Object values = processJsonArray(locale, param0, (JSONArray) o);
                    invoke(locale, setter, attribute, model, values);
                } else if (JSONObject.class.isAssignableFrom(o.getClass())) {
                    invoke(locale, setter, attribute, model, unmarshallClass(locale, class0, (JSONObject) o));
                } else {
                    if ("null".equals(o.toString())) {
                        o = null;
                    }
                    if (!isUnmodifiedConfidentialProperty(modelClass, name, o)) {
                        invoke(locale, setter, attribute, model, o);
                    }
                }
            }
        }
        return model;
    }

    private boolean isUnmodifiedConfidentialProperty(Class modelClass, String setterMethodName, Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        String s = (String)value;
        if (!JsonUtil.CONFIDENTIAL_PROPERTY_SET.equals(s)) {
            return false;
        }
        String getterMethodName = "g" + setterMethodName.substring(1);
        return JsonUtil.isConfidentialProperty(modelClass, getterMethodName);
    }

    private Object processJsonArray(Locale locale, Type param0, JSONArray array) throws JSONException {
        Type type;
        boolean isArray = false;
        if (ParameterizedType.class.isAssignableFrom(param0.getClass())) {
            type = ((ParameterizedType) param0).getActualTypeArguments()[0];
        } else {
            isArray = ((Class<?>)param0).isArray();
            type = ((Class<?>)param0).getComponentType();
        }
        // TODO: We either have a List<T> or T[]. While this works, perhaps we should only support List<T>. It's cleaner.
        Object values = isArray ?
                Array.newInstance((Class<?>) type, array.length()) :
                new ArrayList();

        for (int i = 0; i < array.length(); i++) {
            Object element = array.get(i);
            if (JSONObject.class.isAssignableFrom(element.getClass())) {
                if (isArray) {
                    Array.set(values, i, unmarshallClass(locale, (Class) type, (JSONObject) element));
                } else {
                    ((List)values).add(unmarshallClass(locale, (Class) type, (JSONObject) element));
                }
            } else {
                if (isArray) {
                    Array.set(values, i, element);
                } else {
                    ((List)values).add(element);
                }
            }
        }
        return values;
    }

    private void invoke(Locale locale, Method m, String attribute, Object o, Object... args) {
        try {
            m.invoke(o, args);
        } catch (IllegalArgumentException iae) {
            // TODO: i18n
            String message = "An exception occured while trying to set the value for the property '" + 
                    attribute + "': " + iae.getLocalizedMessage();
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(message).build());
        } catch (Exception e) {
            String message = "An exception occured while trying to set the value for the property '" + 
                    attribute + "': " + e.getLocalizedMessage();
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build());
        }
    }

    /**
     * If the <code>HelpText</code> annotation is in the list of <code>Annotation</code>s, return the value from the
     * specified bundle for the given key.
     *
     * @param annos
     * @return
     */
    public String getHelpText(Annotation[] annos) {
        String helpText = null;
        if (annos != null) {
            for (Annotation annotation : annos) {
                if (HelpText.class.isAssignableFrom(annotation.getClass())) {
                    HelpText ht = (HelpText) annotation;
                    ResourceBundle bundle = ResourceBundle.getBundle(ht.bundle(), Locale.getDefault());
                    helpText = bundle.getString(ht.key());
                }
            }
        }

        return helpText;
    }

    public <T> Set<ConstraintViolation<T>> validateRestModel(Locale locale, T model) {
        initBeanValidator();

        Set<ConstraintViolation<T>> constraintViolations = beanValidator.validate(model);
        if (constraintViolations == null || constraintViolations.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        return constraintViolations;
    }

    public <T> String getValidationFailureMessages(Locale locale, Set<ConstraintViolation<T>> constraintViolations, T model) {
        StringBuilder msg = new StringBuilder(adminStrings.getLocalString("rest.model.validationFailure",
                "Properties for model {0} violate the following constraints: ",
                model.getClass().getSimpleName()));
        String sep = "";
        String violationMsg = adminStrings.getLocalString("rest.model.validationFailure.reason",
                "on property [ {1} ] violation reason [ {0} ]");
        for (ConstraintViolation cv : constraintViolations) {
            msg.append(sep)
                    .append(MessageFormat.format(violationMsg, cv.getMessage(), cv.getPropertyPath()));

            sep = "\n";
        }
        return msg.toString();
    }

    /**
     * Apply changes to domain.xml
     *
     * @param changes
     * @param basePath
     */
    public void applyChanges(Map<String, String> changes, String basePath, Subject subject) {
        RestActionReporter ar = Util.applyChanges(changes, basePath, subject);
        if (!ar.getActionExitCode().equals(ExitCode.SUCCESS)) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).
                    entity(ar.getCombinedMessage()).build());
        }
    }

    /**
     * Execute a delete <code>AdminCommand</code> with no parameters.
     *
     * @param subject
     * @param command
     * @return
     */
    public ActionReporter executeDeleteCommand(Subject subject, String command) {
        return executeDeleteCommand(subject, command, new ParameterMap());
    }

    /**
     * Execute a delete <code>AdminCommand</code> with the specified parameters.
     *
     * @param subject
     * @param command
     * @param parameters
     * @return
     */
    public ActionReporter executeDeleteCommand(Subject subject, String command, ParameterMap parameters) {
        return executeCommand(subject, command, parameters, Status.BAD_REQUEST, true, true, false);
    }

    /**
     * Execute a delete <code>AdminCommand</code> with the specified parameters.
     *
     * @param subject
     * @param command
     * @param parameters
     * @return
     */
    public ActionReporter executeDeleteCommandManaged(Subject subject, String command, ParameterMap parameters) {
        return executeCommand(subject, command, parameters, Status.BAD_REQUEST, true, true, true);
    }


    /**
     * Execute a writing <code>AdminCommand</code> with no parameters.
     *
     * @param subject
     * @param command
     * @return
     */
    public ActionReporter executeWriteCommand(Subject subject, String command) {
        return executeWriteCommand(subject, command, new ParameterMap());
    }

    /**
     * Execute a writing <code>AdminCommand</code> with the specified parameters.
     *
     * @param subject
     * @param command
     * @param parameters
     * @return
     */
    public ActionReporter executeWriteCommand(Subject subject, String command, ParameterMap parameters) {
        return executeCommand(subject, command, parameters, Status.BAD_REQUEST, true, true, false);
    }

    /**
     * Execute a writing <code>AdminCommand</code> with the specified parameters as managed job.
     *
     * @param subject
     * @param command
     * @param parameters
     * @return
     */
    public ActionReporter executeWriteCommandManaged(Subject subject, String command, ParameterMap parameters) {
        return executeCommand(subject, command, parameters, Status.BAD_REQUEST, true, true, true);
    }

    /**
     * Execute a read-only <code>AdminCommand</code> with the specified parameters.
     *
     * @param subject
     * @param command
     * @return
     */
    public ActionReporter executeReadCommand(Subject subject, String command) {
        return executeReadCommand(subject, command, new ParameterMap());
    }

    /**
     * Execute a read-only <code>AdminCommand</code> with no parameters.
     *
     * @param subject
     * @param command
     * @param parameters
     * @return
     */
    public ActionReporter executeReadCommand(Subject subject, String command, ParameterMap parameters) {
        return executeCommand(subject, command, parameters, Status.NOT_FOUND, true, true, false);
    }

    /**
     * Execute an <code>AdminCommand</code> with the specified parameters.
     *
     * @param command
     * @param parameters
     * @param throwBadRequest (vs. NOT_FOUND)
     * @param throwOnWarning  (vs.ignore warning)
     * @return
     */
    public ActionReporter executeCommand(Subject subject, String command, ParameterMap parameters, Status status, boolean includeFailureMessage, boolean throwOnWarning, boolean managed) {
        RestActionReporter ar = ResourceUtil.runCommand(command, parameters, subject, managed);
        ExitCode code = ar.getActionExitCode();
        if (code.equals(ExitCode.FAILURE) || (code.equals(ExitCode.WARNING) && throwOnWarning)) {
            Throwable t = ar.getFailureCause();
            if (t instanceof SecurityException) {
              throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
            }
            if (includeFailureMessage) {
                throw new WebApplicationException(Response.status(status).entity(ar.getCombinedMessage()).build());
            } else {
                throw new WebApplicationException(status);
            }
        }
        return ar;
    }

    /** Execute an <code>AdminCommand</code> with the specified parameters and
     * return EventOutput suitable for SSE.
     */
    public EventOutput executeSseCommand(Subject subject, String command, ParameterMap parameters) {
        return executeSseCommand(subject, command, parameters, null);
    }

    /** Execute an <code>AdminCommand</code> with the specified parameters and
     * return EventOutput suitable for SSE.
     */
    public EventOutput executeSseCommand(Subject subject, String command, ParameterMap parameters, SseCommandHelper.ActionReportProcessor processor) {
        return ResourceUtil.runCommandWithSse(command, parameters, subject, processor);
    }

    public Locale getLocale(HttpHeaders requestHeaders) {
        return getLocale(requestHeaders.getRequestHeaders());
    }

    public Locale getLocale(MultivaluedMap<String,String> requestHeaders) {
        String hdr = requestHeaders.getFirst("Accept-Language");
        return (hdr != null) ? new Locale(hdr) : null;
    }

    /*******************************************************************************************************************
     * Private implementation methods
     ******************************************************************************************************************/
    /**
     * Find and return all <code>interface</code>s that extend <code>baseModel</code>
     *
     * @param baseModel
     * @return
     */
    private Set<Class<?>> getModelExtensions(Class<?> baseModel) {
        Set<Class<?>> exts = new HashSet<Class<?>>();

        if (!extensionsLoaded) {
            synchronized (modelExtensions) {
                if (!extensionsLoaded) {
                    loadModelExtensionMetadata(baseModel);
                }
            }
        }

        List<String> list = modelExtensions.get(baseModel.getName());
        if (list != null) {
            for (String className : list) {
                try {
                    Class<?> c = Class.forName(className, true, baseModel.getClassLoader());
                    exts.add(c);
                } catch (ClassNotFoundException ex) {
                    RestLogging.restLogger.log(Level.SEVERE, null, ex);
                }
            }
        }

        return exts;
    }

    /**
     * Locate and process all <code>RestModelExtension</code> metadata files
     *
     * @param similarClass
     */
    private void loadModelExtensionMetadata(Class<?> similarClass) {
        BufferedReader reader = null;
        try {
            Enumeration<URL> urls = similarClass.getClassLoader().getResources("META-INF/restmodelextensions");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                while (reader.ready()) {
                    final String line = reader.readLine();
                    if ((line == null) || line.isEmpty()) {
                        continue;
                    }
                    if (line.charAt(0) != '#') {
                        if (!line.contains(":")) {
                            RestLogging.restLogger.log(Level.INFO,
                                    RestLogging.INCORRECTLY_FORMATTED_ENTRY,
                                    new String[]{
                                        "META-INF/restmodelextensions",
                                        line
                                    });
                        }
                        String[] entry = line.split(":");
                        String base = entry[0];
                        String ext = entry[1];
                        List<String> list = modelExtensions.get(base);
                        if (list == null) {
                            list = new ArrayList<String>();
                            modelExtensions.put(base, list);
                        }
                        list.add(ext);
                    }
                }

            }
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    RestLogging.restLogger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private List<Method> getSetters(Class<?> clazz) {
        List<Method> methods = new ArrayList<Method>();

        for (Method method : clazz.getMethods()) {
            if (method.getName().startsWith("set")) {
                methods.add(method);
            }
        }

        return methods;
    }

    private void analyzeInterface(Class<?> iface, Map<String, Map<String, Object>> properties) throws SecurityException {
        // find class level bean reference
        String defaultBean = null;
        if (iface.isAnnotationPresent(DefaultBeanReference.class)) {
            DefaultBeanReference beanRef = iface.getAnnotation(DefaultBeanReference.class);
            defaultBean = beanRef.bean();   
        }

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

                String bean = null;
                String attribute = null;
                AttributeReference ar = method.getAnnotation(AttributeReference.class);
                if (ar != null) {
                    bean = ar.bean();
                    attribute = ar.attribute();
                }
                if (!StringUtil.notEmpty(bean)) {
                    bean = defaultBean;
                }
                if (!StringUtil.notEmpty(attribute)) {
                    attribute = name;
                }
                if (StringUtil.notEmpty(bean) && StringUtil.notEmpty(attribute)) {
                    property.put("annotations", gatherReferencedAttributes(bean, attribute));
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

    private Map<String, Map<String, Object>> gatherReferencedAttributes(String bean, String attribute) {
        Map<String, Map<String, Object>> annos = new HashMap<String, Map<String, Object>>();
        try {
            Class<?> configBeanClass = Class.forName(bean);
            Method m = configBeanClass.getMethod("get" + attribute);
            for (Annotation a : m.getAnnotations()) {
                Map<String, Object> anno = new HashMap<String, Object>();
                for (Method am : a.annotationType().getDeclaredMethods()) {
                    String methodName = am.getName();
                    Object value = am.invoke(a);
                    anno.put(methodName, value);
                }
                annos.put(a.annotationType().getName(), anno);
            }
        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, ex.getLocalizedMessage());
        }

        return annos;
    }

    private void handleGetExtensions(List<RestExtension> extensions, Object data) {
        for (RestExtension re : extensions) {
            re.get(data);
        }
    }

    private ParameterMap handlePostExtensions(List<RestExtension> extensions, Object data) {
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
    private String getInternalTypeString(Class<?> type) {
        return type.isPrimitive()
                ? Primitive.getPrimitive(type.getName()).getInternalType()
                : (type.isArray() ? getInternalName(type.getName()) : ("L" + getInternalName(type.getName() + ";")));
    }

    private String getPropertyName(String name) {
        return name.substring(0, 1).toLowerCase(Locale.getDefault()) + name.substring(1);
    }

    /**
     * This method starts the class definition, adding the JAX-B annotations to allow for marshalling via JAX-RS
     */
    private void visitClass(ClassWriter classWriter, String className, Set<Class<?>> ifaces, Map<String, Map<String, Object>> properties) {
        String[] ifaceNames = new String[ifaces.size() + 1];
        int i = 1;
        ifaceNames[0] = getInternalName(RestModel.class.getName());
        for (Class<?> iface : ifaces) {
            ifaceNames[i++] = iface.getName().replace(".", "/");
        }
        className = getInternalName(className);
        classWriter.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className,
                null,
                "org/glassfish/admin/rest/composite/RestModelImpl",
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
     *
     */
    private void createConstructor(ClassWriter cw, String className, Map<String, Map<String, Object>> properties) {
        MethodVisitor method = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        method.visitCode();
        method.visitVarInsn(ALOAD, 0);
        method.visitMethodInsn(INVOKESPECIAL, "org/glassfish/admin/rest/composite/RestModelImpl", "<init>", "()V");

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

    /**
     * This method generates the byte code to set the default value for a given field. Efforts are made to determine the
     * best way to create the correct value. If the field is a primitive, the one-arg, String constructor of the
     * appropriate wrapper class is called to generate the value. If the field is not a primitive, a one-arg, String
     * constructor is requested to build the value. If both of these attempts fail, the default value is set using the
     * String representation as given via the @Attribute annotation.
     * <p/>
     * TODO: it may make sense to treat primitives here as non-String types.
     */
    private void setDefaultValue(MethodVisitor method, String className, String fieldName, Class<?> fieldClass, String defaultValue) {
        final String type = getInternalTypeString(fieldClass);
        Object value = defaultValue;
        fieldName = getPropertyName(fieldName);

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
    private void createField(ClassWriter cw, String name, Class<?> type) {
        String internalType = getInternalTypeString(type);
        FieldVisitor field = cw.visitField(ACC_PRIVATE, getPropertyName(name), internalType, null, null);
        field.visitAnnotation("Ljavax/xml/bind/annotation/XmlAttribute;", true).visitEnd();
        field.visitEnd();
    }

    /**
     * Create getters and setters for the given field
     */
    private void createGettersAndSetters(ClassWriter cw, Class c, String className, String name, Map<String, Object> props) {
        Class<?> type = (Class<?>) props.get("type");
        String internalType = getInternalTypeString(type);
        className = getInternalName(className);

        // Create the getter
        MethodVisitor getter = cw.visitMethod(ACC_PUBLIC, "get" + name, "()" + internalType, null, null);
        getter.visitCode();
        getter.visitVarInsn(ALOAD, 0);
        getter.visitFieldInsn(GETFIELD, className, getPropertyName(name), internalType);
        getter.visitInsn(type.isPrimitive()
                ? Primitive.getPrimitive(internalType).getReturnOpcode()
                : ARETURN);
        getter.visitMaxs(0, 0);
        getter.visitEnd();
        Map<String, Map<String, Object>> annotations = (Map<String, Map<String, Object>>) props.get("annotations");
        if (annotations != null) {
            for (Map.Entry<String, Map<String, Object>> entry : annotations.entrySet()) {
                String annotationClass = entry.getKey();
                Map<String, Object> annotationValues = entry.getValue();
                AnnotationVisitor av = getter.visitAnnotation("L" + getInternalName(annotationClass) + ";", true);
                for (Map.Entry<String, Object> values : annotationValues.entrySet()) {
                    final String paramName = values.getKey();
                    Object paramValue = values.getValue();
                    if (Class.class.isAssignableFrom(paramValue.getClass())) {
                        paramValue = org.objectweb.asm.Type.getType("L" + getInternalName(paramValue.getClass().getName()) + ";");
                    }
                    if (paramValue.getClass().isArray() && (Array.getLength(paramValue) == 0)) {
                        continue;
                    }
                    av.visit(paramName, paramValue);
                }
                av.visitEnd();
            }
        }

        // Create the setter
        MethodVisitor setter = cw.visitMethod(ACC_PUBLIC, "set" + name, "(" + internalType + ")V", null, null);
        setter.visitCode();
        setter.visitVarInsn(ALOAD, 0);
        setter.visitVarInsn(type.isPrimitive()
                ? Primitive.getPrimitive(internalType).getSetOpCode()
                : ALOAD, 1);
        setter.visitFieldInsn(PUTFIELD, className, getPropertyName(name), internalType);
        setter.visitVarInsn(ALOAD, 0);
        setter.visitLdcInsn(name);
        setter.visitMethodInsn(INVOKEVIRTUAL, className, "fieldSet", "(Ljava/lang/String;)V");
        setter.visitInsn(RETURN);
        setter.visitMaxs(0, 0);
        setter.visitEnd();
    }

    /**
     * Convert the dotted class name to the "internal" (bytecode) representation
     */
    private String getInternalName(String className) {
        return className.replace(".", "/");
    }

    // TODO: This is duplicated from the generator class.
    private Class<?> defineClass(Class<?> similarClass, String className, byte[] classBytes) throws Exception {
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
                        @Override
                        public java.lang.Object run() throws Exception {
                            if (!clM.isAccessible()) {
                                clM.setAccessible(true);
                            }
                            return null;
                        }
                    });

            RestLogging.restLogger.log(Level.FINEST, "Loading bytecode for {0}", className);
            final ClassLoader classLoader =
                    similarClass.getClassLoader();
            //Thread.currentThread().getContextClassLoader();
//                    Thread.currentThread().getContextClassLoader();
            try {
                clM.invoke(classLoader, className, byteContent, 0, byteContent.length, pd);
            } catch (Exception e) {
                throw new RuntimeException(e);
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

    private static synchronized void initBeanValidator() {
        if (beanValidator != null) {
            return;
        }
        ClassLoader cl = System.getSecurityManager() == null
                ? Thread.currentThread().getContextClassLoader()
                : AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
        try {
            Thread.currentThread().setContextClassLoader(Validation.class.getClassLoader());
            ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
            ValidatorContext validatorContext = validatorFactory.usingContext();
            validatorContext.messageInterpolator(new MessageInterpolatorImpl());
            beanValidator = validatorContext.getValidator();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
