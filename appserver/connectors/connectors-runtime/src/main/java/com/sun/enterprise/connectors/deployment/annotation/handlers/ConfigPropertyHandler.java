/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2026] Payara Foundation and/or its affiliates

package com.sun.enterprise.connectors.deployment.annotation.handlers;

import com.sun.enterprise.deployment.annotation.context.RarBundleContext;
import com.sun.enterprise.deployment.annotation.handlers.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import jakarta.resource.spi.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.IllegalStateException;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.apf.impl.HandlerProcessingResultImpl;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.apf.*;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jvnet.hk2.annotations.Service;

/**
 * @author Jagadish Ramu
 */
@Service
@AnnotationHandlerFor(ConfigProperty.class)
public class ConfigPropertyHandler extends AbstractHandler {

    protected final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ConfigPropertyHandler.class);

    protected final static Logger logger = AnnotationUtils.getLogger();

    private static final String SUCCESS = "success";
    private static final String OBJECT = Object.class.getName();

    public HandlerProcessingResult processAnnotation(AnnotationInfo element) throws AnnotationProcessorException {
        AnnotatedElementHandler aeHandler = element.getProcessingContext().getHandler();
        AnnotationInstance configProperty = element.getAnnotation();

        if (aeHandler instanceof RarBundleContext) {

            RarBundleContext rbc = (RarBundleContext) aeHandler;
            ConnectorDescriptor desc = rbc.getDescriptor();
            handleConfigPropertyAnnotation(configProperty, element, desc);
        } else {
            return getFailureResult(element, "not a rar bundle context", true);
        }
        return getDefaultProcessedResult();
    }

    public HandlerProcessingResult handleConfigPropertyAnnotation(
            AnnotationInstance configProperty, AnnotationInfo element, ConnectorDescriptor desc){
            IndexView index = element.getIndex();
            String defaultValue = AnnotationValues.string(index, configProperty, "defaultValue");
            String[] description = AnnotationValues.strings(index, configProperty, "description");
            boolean ignore = AnnotationValues.bool(index, configProperty, "ignore");
            boolean supportsDynamicUpdates = AnnotationValues.bool(index, configProperty, "supportsDynamicUpdates");
            boolean confidential = AnnotationValues.bool(index, configProperty, "confidential");

            String type ;

            if (element.getElementType().equals(ElementType.METHOD)) {
                MethodInfo m = element.getAnnotatedElement().asMethod();
                String result = validateMethod(index, m, configProperty);

                if(!result.equals(SUCCESS)){
                    return getFailureResult(element, result, true);
                }

                type = getType(index, configProperty, ClassHierarchy.javaName(m.parameterTypes().get(0)));
                //XXX: Siva: For now use the first provided description
                String firstDesc = "";
                if (description.length > 0) firstDesc = description[0];
                ConnectorConfigProperty ep = getConfigProperty(defaultValue, firstDesc, ignore,
                        supportsDynamicUpdates, confidential, type, m.name().substring(3));

                handleConfigPropertyAnnotation(element, desc, ep, m.declaringClass());

            } else if (element.getElementType().equals(ElementType.FIELD)) {
                FieldInfo f = element.getAnnotatedElement().asField();
                String result = validateField(index, f, configProperty);


                if(!result.equals(SUCCESS)){
                    return getFailureResult(element, result, true);
                }

                type = getType(index, configProperty, ClassHierarchy.javaName(f.type()));

                if(defaultValue.equals("")){
                    defaultValue = deriveDefaultValueOfField(f, desc);
                }
                //XXX: Siva: For now use the first provided description
                String firstDesc = "";
                if (description.length > 0) firstDesc = description[0];
                ConnectorConfigProperty ep = getConfigProperty(defaultValue,firstDesc, ignore,
                        supportsDynamicUpdates, confidential,  type, f.name());

                handleConfigPropertyAnnotation(element, desc, ep, f.declaringClass());
            }
        return getDefaultProcessedResult();
    }

    /**
     * @return the name of the wrapper class of the primitive type
     */
    private static String getWrapperClass(String primitive){
        if(primitive.equalsIgnoreCase("int")){
            return Integer.class.getName();
        }else if(primitive.equalsIgnoreCase("long")){
            return Long.class.getName();
        }else if(primitive.equalsIgnoreCase("short")){
            return Short.class.getName();
        }else if(primitive.equalsIgnoreCase("char")){
            return Character.class.getName();
        }else if(primitive.equalsIgnoreCase("byte")){
            return Byte.class.getName();
        }else if(primitive.equalsIgnoreCase("boolean")){
            return Boolean.class.getName();
        }else if(primitive.equalsIgnoreCase("float")){
            return Float.class.getName();
        }else if(primitive.equalsIgnoreCase("double")){
            return Double.class.getName();
        }else{
            throw new IllegalArgumentException("Could not determine Wrapper class for primitive type ["+primitive+"]");
        }
    }

    /**
     * The default of a field without an explicit {@code defaultValue} is what its getter returns on a
     * new instance of the JavaBean. The index cannot tell, so this is the one place where a resource
     * adapter class is loaded and instantiated, through the class loader of the descriptor.
     */
    private static String deriveDefaultValueOfField(FieldInfo f, ConnectorDescriptor desc){
        String declaringClassName = f.declaringClass().name().toString();
        String fieldName = f.name();
        String value = null;
        try {
            Class<?> declaringClass = Class.forName(declaringClassName, false, classLoader(desc));
            Object o = declaringClass.newInstance();
            String getterMethod = "get" + getCamelCasedPropertyName(fieldName);

            if(Boolean.class.getName().equals(ClassHierarchy.javaName(f.type()))){
                getterMethod = "is" + getCamelCasedPropertyName(fieldName);
            }
            Method m = declaringClass.getDeclaredMethod(getterMethod);
            m.setAccessible(true);
            Object result = m.invoke(o);
            if(result != null) {
                value = result.toString();
            }
        } catch (Exception e) {
            Object[] args = {fieldName, declaringClassName, e.getMessage()};
            String localString = localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.configpropertyfieldreadfailure",
                    "failed to read the value of field [{0}] on class [{1}], reason : {2}", args);
            logger.log(Level.WARNING, localString, e);
        }
        return value;
    }

    /**
     * @return the class loader of the resource adapter: the descriptor's, as the context class loader
     * of the DAS does not see a resource adapter that is not targeted to it, or the context class
     * loader when the descriptor has none
     */
    public static ClassLoader classLoader(ConnectorDescriptor desc) {
        ClassLoader classLoader = desc.getClassLoader();
        return classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

    /**
     * Returns camel-cased version of a propertyName. Used to construct
     * correct accessor and mutator method names for a give property.
     */
    private static String getCamelCasedPropertyName(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase(Locale.getDefault()) +
                propertyName.substring(1);
    }


    private static ConnectorConfigProperty getConfigProperty(String defaultValue, String description, boolean ignore,
                                                      boolean supportsDynamicUpdates, boolean confidential,
                                                      String type, String propertyName) {
        ConnectorConfigProperty ep = new ConnectorConfigProperty();
        //use description if specified
        if (!description.equals("")) {
            ep.setDescription(description);
        }
        //use default value if specified
        if ( defaultValue!= null && !defaultValue.equals("")) {
            ep.setValue(defaultValue);
        }
        ep.setType(type);

        ep.setName(propertyName);

        if (!ep.isSetIgnoreCalled()) {
            ep.setIgnore(ignore);
        }
        if (!ep.isSetConfidentialCalled()) {
            ep.setConfidential(confidential);
        }
        if (!ep.isSupportsDynamicUpdates()) {
            ep.setSupportsDynamicUpdates(supportsDynamicUpdates);
        }
        return ep;
    }

    private void handleConfigPropertyAnnotation(AnnotationInfo element, ConnectorDescriptor desc,
                                                ConnectorConfigProperty ep, ClassInfo declaringClass) {
        IndexView index = element.getIndex();

        if ((ClassHierarchy.isSubtype(index, declaringClass, ResourceAdapter.class)
                && (!ClassHierarchy.isAbstract(declaringClass))) ||
                ( ClassHierarchy.annotation(index, declaringClass, Connector.class) != null)){
            if (!processConnector(index, desc, ep, declaringClass)) {
                //need to book-keep the annotation for post-processing
                desc.addConfigPropertyAnnotation(declaringClass.name().toString(), element);
            }
        } else if (ClassHierarchy.isSubtype(index, declaringClass, ManagedConnectionFactory.class) &&
                (!ClassHierarchy.isAbstract(declaringClass))) {
                //@ConnectionDefintion, @ConnectionDefinitions must be of type ManagedConnectionFactory and hence
                //the above check is sufficient to take care of JavaBean as well annotation.
                processConnectionDefinition(element, desc, ep, declaringClass);
        } else if ((ClassHierarchy.isSubtype(index, declaringClass, ActivationSpec.class) &&
                (!ClassHierarchy.isAbstract(declaringClass))) ||
                (ClassHierarchy.annotation(index, declaringClass, Activation.class) != null) ) {
            processActivation(element, desc, ep, declaringClass);
        } else if (ClassHierarchy.annotation(index, declaringClass, AdministeredObject.class) != null ||
                isAdminObjectJavaBean(declaringClass, desc)){
            processAdministeredObject(element, desc, ep, declaringClass);
        }
    }

    private boolean isAdminObjectJavaBean(ClassInfo adminObjectClass, ConnectorDescriptor desc) {
        boolean isAdminObject = false;
        Set adminObjects = desc.getAdminObjects();
        Iterator adminObjectsItr = adminObjects.iterator();
        while(adminObjectsItr.hasNext()){
            AdminObject adminObject = (AdminObject)adminObjectsItr.next();
            if(adminObject.getAdminObjectClass().equals(adminObjectClass.name().toString())){
                isAdminObject = true;
                break;
            }
        }
        return isAdminObject;
    }

    private void processAdministeredObject(AnnotationInfo element, ConnectorDescriptor desc,
                                                           ConnectorConfigProperty ep, ClassInfo declaringClass) {
        IndexView index = element.getIndex();
        String declaringClassName = declaringClass.name().toString();

        AnnotationInstance ao = ClassHierarchy.annotation(index, declaringClass, AdministeredObject.class);
        if (ao != null) {
            String[] adminObjectInterfaces = AnnotationValues.classNames(index, ao, "adminObjectInterfaces");
            if(adminObjectInterfaces.length > 0){
                for (String adminObjectInterface : adminObjectInterfaces) {
                    handleAdministeredObject(element, desc, ep, declaringClass, adminObjectInterface);
                }
            }else{
                //handle the case where admin object interfaces are not specified via annotaiton
                List<String> interfacesList = AdministeredObjectHandler.
                        deriveAdminObjectInterfacesFromHierarchy(declaringClass);

                //We assume that there will be only one interface (if there had been many, admin-object annotation
                //handler would have rejected it.)
                if(interfacesList.size() == 1){
                    handleAdministeredObject(element, desc, ep, declaringClass, interfacesList.get(0));
                }
            }
        } else {
            Set adminObjects = desc.getAdminObjects();
            Iterator adminObjectItr = adminObjects.iterator();
            while(adminObjectItr.hasNext()){
                AdminObject adminObject = (AdminObject)adminObjectItr.next();
                if(adminObject.getAdminObjectClass().equals(declaringClassName)){
                    if (!(isConfigDefined(adminObject.getConfigProperties(), ep))) {
                        adminObject.addConfigProperty(ep);
                    }
                    String uniqueName = adminObject.getAdminObjectInterface() + "_" + adminObject.getAdminObjectClass();
                    if(!desc.getConfigPropertyProcessedClasses().contains(uniqueName)){
                        processParent(index, ClassHierarchy.superClass(index, declaringClass),
                                adminObject.getConfigProperties(), desc);
                        desc.addConfigPropertyProcessedClass(declaringClassName);
                    }
                }
            }
        }
    }

    private void handleAdministeredObject(AnnotationInfo element, ConnectorDescriptor desc,
                                          ConnectorConfigProperty ep, ClassInfo adminObjectClass, String adminObjectIntf) {
        IndexView index = element.getIndex();
        String adminObjectClassName = adminObjectClass.name().toString();
        AdminObject adminObject = desc.getAdminObject(adminObjectIntf, adminObjectClassName);
        if (adminObject != null) {
            if (!(isConfigDefined(adminObject.getConfigProperties(), ep))) {
                adminObject.addConfigProperty(ep);
            }
            if(!desc.getConfigPropertyProcessedClasses().contains(adminObjectClassName)){
                processParent(index, ClassHierarchy.superClass(index, adminObjectClass),
                        adminObject.getConfigProperties(), desc);
                desc.addConfigPropertyProcessedClass(adminObjectClassName);
            }
        } else {
            // ideally adminObject should not be null as "@AdministeredObject" 
            // should have been handled before @ConfigProperty
            getFailureResult(element, "could not get adminobject of interface " +
                    "[ " + adminObjectIntf + " ]" +
                    " and class [ " + adminObjectClassName + " ]", true);
        }
    }

    private void processActivation(AnnotationInfo element, ConnectorDescriptor desc,
                                   ConnectorConfigProperty ep, ClassInfo declaringClass) {
        IndexView index = element.getIndex();
        String declaringClassName = declaringClass.name().toString();

           InboundResourceAdapter ira = desc.getInboundResourceAdapter();
        AnnotationInstance activation = ClassHierarchy.annotation(index, declaringClass, Activation.class);
        if (activation != null) {
            // Inbound Resource Adapter should have been defined if @Activation annotation
            // was processed successfully, before.
            if (desc.getInBoundDefined()) {
                String[] messageListeners = AnnotationValues.classNames(index, activation, "messageListeners");

                //messageListeners cant be 0 as we ask "@Activation" to be handled before "@ConfigProperty"
                for (String clz : messageListeners) {
                    if (ira.hasMessageListenerType(clz)) {
                        MessageListener ml = ira.getMessageListener(clz);

                        //check whether the activation-spec class in the descriptor
                        //for a particular message-listener is the same as this class as it is possible
                        //that this activation-spec class may have been ignored if ra.xml is already defined with
                        //this particular message-listener-type. If so, we should not add config-property as they
                        // belong to a particular activation-spec class.
                        if (ml.getActivationSpecClass().equals(declaringClassName)) {
                            if (!(isConfigDefined(ml.getConfigProperties(), ep))) {
                                ml.addConfigProperty(ep);
                            }
                            if (!desc.getConfigPropertyProcessedClasses().contains(declaringClassName)) {
                                processParent(index, ClassHierarchy.superClass(index, declaringClass),
                                        ml.getConfigProperties(), desc);
                                desc.addConfigPropertyProcessedClass(declaringClassName);
                            }
                        }
                    }
                }
            }
        } else {
                if(desc.getInBoundDefined()){
                    Set messageListeners = desc.getInboundResourceAdapter().getMessageListeners();
                    Iterator mlItr = messageListeners.iterator();
                    while(mlItr.hasNext()){
                        MessageListener ml = (MessageListener)mlItr.next();
                        if(ml.getActivationSpecClass().equals(declaringClassName)){
                            if (!(isConfigDefined(ml.getConfigProperties(), ep))) {
                                ml.addConfigProperty(ep);
                            }
                            if(!desc.getConfigPropertyProcessedClasses().contains(declaringClassName)){
                                processParent(index, ClassHierarchy.superClass(index, declaringClass),
                                        ml.getConfigProperties(), desc);
                                desc.addConfigPropertyProcessedClass(declaringClassName);
                            }
                        }
                    }
                }
            }
        }

    private void processConnectionDefinition(AnnotationInfo element, ConnectorDescriptor desc,
                                             ConnectorConfigProperty ep, ClassInfo declaringClass) {
        IndexView index = element.getIndex();
        String declaringClassName = declaringClass.name().toString();
        if (desc.getOutBoundDefined()) {

            OutboundResourceAdapter ora = desc.getOutboundResourceAdapter();
            Set connectionDefinitions = ora.getConnectionDefs();

            for (Object o : connectionDefinitions) {
                ConnectionDefDescriptor cd = (ConnectionDefDescriptor) o;

                if (cd.getManagedConnectionFactoryImpl().equals(declaringClassName)) {

                    if (!(isConfigDefined(cd.getConfigProperties(), ep))) {
                        cd.addConfigProperty(ep);
                    }
                    //As same MCF class can be used for multiple connection-definitions
                    //store it based on connection-factory-interface class which is the unique
                    //identifier for a connection-definition
                    if(!desc.getConfigPropertyProcessedClasses().contains(cd.getConnectionFactoryIntf())){
                        processParent(index, ClassHierarchy.superClass(index, declaringClass),
                                cd.getConfigProperties(), desc);
                        desc.addConfigPropertyProcessedClass(cd.getConnectionFactoryIntf());
                    }
                }
                //ignore if connection-definition entry is not found as it is possible that
                //ra.xml has a connection-definition with the same connection-factory class
                //as this annotation.

                //it is possible that multiple ConnectionDefinitions with same MCF class, but different
                //connection-factory-interface can be defined.Hence process all connection definitions
            }

        } else {
            // if there is a @ConfigProperty annotation on any of the connection-definition (MCF), either it is
            // defined via ra.xml and hence actual @ConnectionDefinition(s) annotation is ignored
            // or
            // no clash between ra.xml and the annotation, actual annotation is considered
            // So, outbound-ra must have been defined either way.
            getFailureResult(element, "Outbound RA is not defined", true);
        }
    }

    public static boolean processConnector(IndexView index, ConnectorDescriptor desc, ConnectorConfigProperty ep,
                                           ClassInfo declaringClass) {
        // make sure that the RA Class considered here is the one specified in descriptor
        // If not, it will be processed once the @Connector is selected during post-processing 

        // handle the annotation specified on a ResourceAdapter JavaBean
        // make sure that the property is not already specified in DD
        String declaringClassName = declaringClass.name().toString();
        if(desc.getResourceAdapterClass().equals(declaringClassName)){
            if (!(isConfigDefined(desc.getConfigProperties(), ep))) {
                desc.addConfigProperty(ep);
            }
            if(!desc.getConfigPropertyProcessedClasses().contains(declaringClassName)){
                processParent(index, ClassHierarchy.superClass(index, declaringClass), desc.getConfigProperties(), desc);
                desc.addConfigPropertyProcessedClass(declaringClassName);
            }
            //indicate that the config-property is processed
            return true;
        }else{
            //indicate that the config-property is not processed and need to be processed during post-processing
            return false;
        }
    }

    private static String validateMethod(IndexView index, MethodInfo m, AnnotationInstance property){

        if(!m.name().startsWith("set")){
            return "not a standard JavaBean setter method : [" + m.name() + " ] ";
        }

        int modifier = m.flags();

        //we are not restricting protected, default methods as potentially
        //any of the sub-classes may broaden the accessibility.
        if(Modifier.isPrivate(modifier)){
            return "@ConfigProperty annotation on a " +
                    "private setter method [ "+m.name()+" ] " +
                    "of class [ "+m.declaringClass().name()+" ]";
        }

        Type type = AnnotationValues.value(index, property, "type").asClass();
        List<Type> parameters = m.parameterTypes();
        Type propertyType;
        if (parameters.size() != 0) {
            if (parameters.size() == 1) {
                propertyType = parameters.get(0);
            } else {
                return "more than one parameter for JavaBean setter method : [" + m.name() + " ] ";
            }
        } else {
            return "no parameters for JavaBean setter method :  [" + m.name() + " ] ";
        }
        //check compatibility between annotation type and property-type

        if (!isObject(type) && !isAssignableFrom(index, propertyType, type)) {
            String typeName = ClassHierarchy.javaName(type);
            String propertyTypeName = ClassHierarchy.javaName(propertyType);
            if(type.kind() == Type.Kind.PRIMITIVE){
                typeName = getWrapperClass(typeName);
            }else if(propertyType.kind() == Type.Kind.PRIMITIVE){
                propertyTypeName = getWrapperClass(propertyTypeName);
            }

            if(!isAssignableFrom(index, propertyTypeName, typeName)){
                return "annotation type [" + describe(index, typeName) + "] and property-type" +
                        " [" + describe(index, propertyTypeName) + "] " +
                        "are not assignment compatible";
            }
        }
        return SUCCESS;
    }

    /**
     * @param declaredType the type of the annotated field or setter parameter, as {@code Class.getName()} gives it
     * @return the type of the config property: the annotation's unless it is left at {@code Object}
     */
    private static String getType(IndexView index, AnnotationInstance property, String declaredType){
        String configPropertyType = AnnotationValues.className(index, property, "type");

        if (configPropertyType.equals(OBJECT)) {
            configPropertyType = declaredType;
        }
        return configPropertyType;
    }

    private static String validateField(IndexView index, FieldInfo f, AnnotationInstance property){

        Type returnType = f.type();
        Type type = AnnotationValues.value(index, property, "type").asClass();
        if (!isObject(type)) {
            //check compatibility between annotation type and return-type
            if (!isAssignableFrom(index, returnType, type)) {
                return "annotation type [" + describe(index, ClassHierarchy.javaName(type)) + "] " +
                        "and return-type [" + describe(index, ClassHierarchy.javaName(returnType)) + "] " +
                        "are not assignment compatible for @ConfigProperty in " +
                        "field [ " + f.name() + " ] of class [ " + describe(index, f.declaringClass().name().toString()) + " ]";
            }
        }
        return SUCCESS;
    }

    private static boolean isObject(Type type) {
        return OBJECT.equals(ClassHierarchy.javaName(type));
    }

    /**
     * {@code target.isAssignableFrom(source)}: a primitive is assignable to itself only
     */
    private static boolean isAssignableFrom(IndexView index, Type target, Type source) {
        if (target.kind() == Type.Kind.PRIMITIVE || source.kind() == Type.Kind.PRIMITIVE) {
            return ClassHierarchy.javaName(target).equals(ClassHierarchy.javaName(source));
        }
        return isAssignableFrom(index, ClassHierarchy.javaName(target), ClassHierarchy.javaName(source));
    }

    private static boolean isAssignableFrom(IndexView index, String target, String source) {
        return ClassHierarchy.isSubtype(index, DotName.createSimple(source), DotName.createSimple(target));
    }

    /**
     * @return what {@code Class.toString()} says of the type: {@code int}, {@code class X} or {@code interface X}
     */
    private static String describe(IndexView index, String javaName) {
        if (javaName.startsWith("[")) {
            return "class " + javaName;
        }
        ClassInfo type = index.getClassByName(DotName.createSimple(javaName));
        if (type == null) {
            // a primitive
            return javaName;
        }
        return (ClassHierarchy.isInterface(type) ? "interface " : "class ") + javaName;
    }

    /**
     * Collects the {@code @ConfigProperty} declarations of the class and of its superclasses.
     *
     * @param claz the class to start from, null for the superclass of a class extending {@code Object}
     * @param desc the resource adapter, whose class loader the JavaBeans load with to derive field defaults
     */
    public static void processParent(IndexView index, ClassInfo claz, Set configProperties, ConnectorDescriptor desc) {

        if(claz == null){
            return;
        }
        //process the methods
        for (MethodInfo m : claz.methods()) {
            AnnotationInstance property = ClassHierarchy.annotation(index, m, ConfigProperty.class);
            if (property != null) {
                String result = validateMethod(index, m, property);
                if (!result.equals(SUCCESS)) {
                    throw new IllegalStateException(result);
                }
                String defaultValue = AnnotationValues.string(index, property, "defaultValue");
                processConfigProperty(index, configProperties, m.name().substring(3), property, defaultValue,
                        ClassHierarchy.javaName(m.parameterTypes().get(0)));
            }
        }

        //process the fields
        for (FieldInfo f : claz.fields()) {
            AnnotationInstance property = ClassHierarchy.annotation(index, f, ConfigProperty.class);

            if (property != null) {
                String status = validateField(index, f, property);
                if(!status.equals(SUCCESS)){
                    throw new IllegalStateException(status);
                }
                String defaultValue = AnnotationValues.string(index, property, "defaultValue");

                if (defaultValue.equals("")) {
                    defaultValue = deriveDefaultValueOfField(f, desc);
                }
                processConfigProperty(index, configProperties, f.name(), property, defaultValue,
                        ClassHierarchy.javaName(f.type()));
            }
        }

        //process its super-class
        processParent(index, ClassHierarchy.superClass(index, claz), configProperties, desc);
    }

    private static void processConfigProperty(IndexView index, Set configProperties, String propertyName,
                                              AnnotationInstance property, String defaultValue, String declaredEntityType) {
        String description = "";
        String[] descriptions = AnnotationValues.strings(index, property, "description");
        if(descriptions.length > 0){
            description = descriptions[0];
        }
        String type = getType(index, property, declaredEntityType);
        ConnectorConfigProperty ccp = getConfigProperty(defaultValue, description,
                AnnotationValues.bool(index, property, "ignore"),
                AnnotationValues.bool(index, property, "supportsDynamicUpdates"),
                AnnotationValues.bool(index, property, "confidential"),
                type, propertyName);
        if(!isConfigDefined(configProperties, ccp)){
            configProperties.add(ccp);
        }
    }

    private static boolean isConfigDefined(Set configProperties, ConnectorConfigProperty ep) {
        boolean result = false;
        for (Object o : configProperties) {
            ConnectorConfigProperty ddEnvProperty = (ConnectorConfigProperty) o;
            if (ddEnvProperty.getName().equals(ep.getName())) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * @return a default processed result
     */
    protected HandlerProcessingResult getDefaultProcessedResult() {
        return HandlerProcessingResultImpl.getDefaultResult(
                getAnnotationType(), ResultType.PROCESSED);
    }


    public Class<? extends Annotation>[] getTypeDependencies() {
        return new Class[]{Connector.class, ConnectionDefinition.class, ConnectionDefinitions.class,
                Activation.class, AdministeredObject.class};
    }

    private void debug(String s) {
        logger.log(Level.INFO, "[ConfigPropertyHandler] " + s);
    }

    private HandlerProcessingResultImpl getFailureResult(AnnotationInfo element, String message, boolean doLog) {
        HandlerProcessingResultImpl result = new HandlerProcessingResultImpl();
        result.addResult(getAnnotationType(), ResultType.FAILED);
        if (doLog) {
            String className = element.getDeclaringClass().name().toString();
            Object args[] = new Object[]{
                element.getAnnotation(),
                className,
                message,
            };
            String localString = localStrings.getLocalString(
                    "enterprise.deployment.annotation.handlers.connectorannotationfailure",
                    "failed to handle annotation [ {0} ] on class [ {1} ], reason : {2}", args);
            logger.log(Level.WARNING, localString);
        }
        return result;
    }
}
