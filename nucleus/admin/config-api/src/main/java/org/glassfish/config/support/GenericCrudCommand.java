/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandModelProvider;
import org.glassfish.common.util.admin.ParamTokenizer;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.GenerateServiceFromMethod;
import org.jvnet.hk2.config.InjectionManager;
import org.jvnet.hk2.config.InjectionResolver;
import org.jvnet.hk2.config.TransactionFailure;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.Self;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.tiger_types.Types;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.logging.LogHelper;


/**
 * services pertinent to generic CRUD command implementations
 *
 * @author Jerome Dochez
 *
 */
public abstract class GenericCrudCommand implements CommandModelProvider, PostConstruct, AdminCommandSecurity.Preauthorization {
    
    private InjectionResolver<Param> injector;

    @Inject @Self
    private ActiveDescriptor<?> myself;

    final protected static Logger logger = ConfigApiLoggerInfo.getLogger();
    final protected static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(GenericCrudCommand.class);

    protected String commandName;
    protected Class parentType=null;
    protected Class targetType=null;
    protected Method targetMethod;
    
    // default level of noise, useful for just swithching these classes in debugging.
    protected final Level level = Level.FINE;
    
    @Inject
    ServiceLocator habitat;
   
    InjectionManager manager;
    CrudResolver resolver;
    InjectionResolver paramResolver;
    Class<? extends CrudResolver> resolverType;
    
    void prepareInjection(final AdminCommandContext ctx) {
        // inject resolver with command parameters...
        manager = new InjectionManager();

        resolver = habitat.getService(resolverType);

        paramResolver = getInjectionResolver();

        manager.inject(resolver, paramResolver);
    }

    @Override
    public boolean preAuthorization(AdminCommandContext adminCommandContext) {
        prepareInjection(adminCommandContext);
        return true;
    }
    
    private static String getOne(String key, Map<String, List<String>> metadata) {
    	if (key == null || metadata == null) return null;
    	
    	List<String> findInMe = metadata.get(key);
    	if (findInMe == null) return null;
    	
    	return findInMe.get(0);
    }

    public void postConstruct() {
        commandName = myself.getName();
        
        String parentTypeName = getOne(GenerateServiceFromMethod.PARENT_CONFIGURED, myself.getMetadata());
        if (logger.isLoggable(level)) {
            logger.log(level,"Generic method parent targeted type is " + parentTypeName);
        }

        try {
            parentType = loadClass(parentTypeName);
        }
        catch(ClassNotFoundException e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.configbean_not_found",
                    "The Config Bean {0} cannot be loaded by the generic command implementation : {1}",
                    parentTypeName, e.getMessage());
            LogHelper.log(logger, Level.SEVERE, ConfigApiLoggerInfo.CFG_BEAN_CL_FAILED, e, parentTypeName);
            throw new RuntimeException(msg, e);
        }

        // find now the accessor method.
        String methodName = getOne(GenerateServiceFromMethod.METHOD_NAME, myself.getMetadata());
        targetMethod=null;
        methodlookup:
        for (Method m : parentType.getMethods()) {
            if (m.getName().equals(methodName)) {
                // Make sure that this method is annotated with an annotation 
                // that is annotated with InhabitantAnnotation (such as @Create). 
                // This makes sure that we have found the method we are looking for
                // in case there is a like-named method that is not annotated.
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType().getAnnotation(GenerateServiceFromMethod.class) != null) {
                        targetMethod=m;
                        break methodlookup;
                    }
                }
            }
        }

        if (targetMethod==null) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.configbean_not_found",
                    "The Config Bean {0} cannot be loaded by the generic command implementation : {1}",
                    parentTypeName, methodName);
            logger.log(Level.SEVERE, ConfigApiLoggerInfo.CFG_BEAN_CL_FAILED, parentTypeName);
            throw new RuntimeException(msg);
        }
        
        String targetTypeName = getOne(GenerateServiceFromMethod.METHOD_ACTUAL, myself.getMetadata());
        try {
            targetType = loadClass(targetTypeName);
        }
        catch(ClassNotFoundException e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.configbean_not_found",
                    "The Config Bean {0} cannot be loaded by the generic command implementation : {1}",
                    targetTypeName, e.getMessage());
            LogHelper.log(logger, Level.SEVERE, ConfigApiLoggerInfo.CFG_BEAN_CL_FAILED, e, targetTypeName);
            throw new RuntimeException(msg, e);
        }
    }

    protected <T extends Annotation> T getAnnotation(Method target, Class<T> type) {
        T annotation = targetMethod.getAnnotation(type);
        if (annotation == null) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.annotation_not_found",
                    "Cannot find annotation {0} with value {1} on method {2}",
                    type.getName(), commandName, targetMethod.toString());
            throw new RuntimeException(msg);
            
        }
            
        return annotation;
    }

    /**
     * we need to have access to the injector instance that has all the parameters context 
     * @param injector the original command injector
     */
    // todo : would be lovely to replace this with some smart injection...
    public void setInjectionResolver(InjectionResolver<Param> injector) {
        this.injector = injector;
    }

    public InjectionResolver<Param> getInjectionResolver() {
        final InjectionResolver<Param> delegate = injector;
        return new InjectionResolver<Param>(Param.class) {
            @Override
            public <V> V getValue(Object component, AnnotatedElement annotated, Type genericType, Class<V> type) throws MultiException {
                if (type.isAssignableFrom(List.class)) {
                    final List<ConfigBeanProxy> values;
                    try {
                        if (annotated instanceof Method) {
                            values = (List<ConfigBeanProxy>) ((Method) annotated).invoke(component);
                        } else if (annotated instanceof Field) {
                            values = (List<ConfigBeanProxy>) ((Field) annotated).get(component);
                        } else {
                            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                    "GenericCrudCommand.invalid_type",
                                    "Invalid annotated type {0} passed to InjectionResolver:getValue()",
                                    annotated.getClass().toString());
                            logger.log(Level.SEVERE, ConfigApiLoggerInfo.INVALID_ANNO_TYPE, 
                                    annotated.getClass().toString());
                            throw new MultiException(new IllegalArgumentException(msg));
                        }
                    } catch (IllegalAccessException e) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.invocation_failure",
                                "Failure {0} while getting List<?> values from component",
                                e.getMessage());
                        logger.log(Level.SEVERE,ConfigApiLoggerInfo.INVOKE_FAILURE);
                        throw new MultiException(new IllegalStateException(msg, e));
                    } catch (InvocationTargetException e) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.invocation_failure",
                                "Failure {0} while getting List<?> values from component",
                                e.getMessage());
                        logger.log(Level.SEVERE, ConfigApiLoggerInfo.INVOKE_FAILURE);
                        throw new MultiException(new IllegalStateException(msg, e));
                    }
                    Object value = delegate.getValue(component, annotated, genericType, type);
                    if (value==null) {
                        if (logger.isLoggable(level)) {
                            logger.log(level, "Value of " + annotated.toString() + " is null");
                        }
                        return null;
                    }
                    Type genericReturnType=null;
                    if (annotated instanceof Method) {
                        genericReturnType = ((Method) annotated).getGenericReturnType();
                    } else if (annotated instanceof Field) {
                        genericReturnType = ((Field) annotated).getGenericType();
                    }
                    if (genericReturnType==null) {
                        throw new MultiException(new IllegalArgumentException(
                                "Cannot determine parametized type from " + annotated.toString()));
                    }

                    final Class<? extends ConfigBeanProxy> itemType = Types.erasure(Types.getTypeArgument(genericReturnType, 0));
                    if (logger.isLoggable(level)) {
                        logger.log(level, "Found that List<?> really is a List<" + itemType.toString() + ">");
                    }
                    if (itemType==null) {
                            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                    "GenericCrudCommand.nongeneric_type",
                                    "The List type returned by {0} must be a generic type",
                                    annotated.toString());
                            logger.log(Level.SEVERE, ConfigApiLoggerInfo.LIST_NOT_GENERIC_TYPE, annotated.toString());
                            throw new MultiException(new IllegalArgumentException(msg));
                    }
                    if (!ConfigBeanProxy.class.isAssignableFrom(itemType)) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.wrong_type",
                                "The generic type {0} is not supported, only List<? extends ConfigBeanProxy> is",
                                annotated.toString());
                        logger.log(Level.SEVERE, ConfigApiLoggerInfo.GENERIC_TYPE_NOT_SUPPORTED, annotated.toString());
                        throw new MultiException(new IllegalArgumentException(msg));
                        
                    }
                    Properties props = convertStringToProperties(value.toString(), ':');
                    if (logger.isLoggable(level)) {
                        for (Map.Entry<Object, Object> entry : props.entrySet()) {
                            logger.log(level, "Subtype " + itemType + " key:" + entry.getKey() + " value:" + entry.getValue());
                        }
                    }
                    final BeanInfo beanInfo;
                    try {
                        beanInfo = Introspector.getBeanInfo(itemType);
                    } catch (IntrospectionException e) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.introspection_failure",
                                "Failure {0} while instrospecting {1} to find all getters and setters",
                                e.getMessage(), itemType.getName());
                        LogHelper.log(logger, Level.SEVERE, ConfigApiLoggerInfo.INTROSPECTION_FAILED, e, itemType.getName());
                        throw new MultiException(new IllegalStateException(msg, e));
                    }
                    for (final Map.Entry<Object, Object> entry : props.entrySet()) {
                        ConfigBeanProxy child = (ConfigBeanProxy) component;
                        try {
                            ConfigBeanProxy cc = child.createChild(itemType);
                            new InjectionManager().inject(cc, itemType, new InjectionResolver<Attribute>(Attribute.class) {

                                @Override
                                public boolean isOptional(AnnotatedElement annotated, Attribute annotation) {
                                    return true;    
                                }

                                @Override
                                public Method getSetterMethod(Method annotated, Attribute annotation) {
                                    // Attribute annotation are always annotated on the getter, we need to find the setter
                                    // variant.
                                    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                                        if (pd.getReadMethod().equals(annotated)) {
                                            return pd.getWriteMethod();
                                        }
                                    }
                                    return annotated;
                                }

                                @Override
                                public <V> V getValue(Object component, AnnotatedElement annotated, Type genericType, Class<V> type) throws MultiException {
                                    String name = annotated.getAnnotation(Attribute.class).value();
                                    if ((name==null || name.length()==0) && annotated instanceof Method) {

                                        // maybe there is a better way to do this...
                                        name = ((Method) annotated).getName().substring(3);

                                        if (name.equalsIgnoreCase("name") || name.equalsIgnoreCase("key")) {
                                            return type.cast(entry.getKey());
                                        }
                                        if (name.equalsIgnoreCase("value")) {
                                            return type.cast(entry.getValue());
                                        }
                                    }
                                    return null;
                                }
                            });
                            values.add(cc);
                        } catch (TransactionFailure transactionFailure) {
                            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.transactionException",
                                "Transaction exception {0} while injecting {1}",
                                transactionFailure.getMessage(), itemType);
                            LogHelper.log(logger, Level.SEVERE, ConfigApiLoggerInfo.TX_FAILED,
                                    transactionFailure, itemType);
                            throw new MultiException(new IllegalStateException(msg, transactionFailure));
                        }

                    }
                    return null;
                }
                return delegate.getValue(component, annotated, genericType, type);
            }

            @Override
            public boolean isOptional(AnnotatedElement annotated, Param annotation) {
                return annotation.optional();
            }
        };
        
    }

    protected Class<?> loadClass(String type) throws ClassNotFoundException {
        HK2Loader loader = myself.getLoader();
        
        if (loader == null) {
            return getClass().getClassLoader().loadClass(type);
        }
        
        try {
            return loader.loadClass(type);
        }
        catch (MultiException me) {
            for (Throwable th : me.getErrors()) {
                if (th instanceof ClassNotFoundException) {
                    throw (ClassNotFoundException) th;
                }
            }
            
            throw new ClassNotFoundException(me.getMessage());
        }
        
    }    

    /**
     * Convert a String with the following format to Properties:
     * name1=value1:name2=value2:name3=value3:...
     * The Properties object contains elements:
     * {name1=value1, name2=value2, name3=value3, ...}
     *
     * @param propsString the String to convert
     * @param sep the separator character
     * @return Properties containing the elements in String
     */
    public static Properties convertStringToProperties(String propsString, char sep) {
        final Properties properties = new Properties();
        if ((propsString != null)&&(!propsString.equals("[]"))) {

            //This is because when there are multiple values in the arraylist
            //they appear like [foo=bar:baz=baz1] so need to remove the braces
            String unbracedString = propsString.substring(propsString.indexOf('[')+1);
            
            ParamTokenizer stoken = new ParamTokenizer(unbracedString, sep);
            while (stoken.hasMoreTokens()) {
                String token = stoken.nextTokenKeepEscapes();
                final ParamTokenizer nameTok = new ParamTokenizer(token, '=');
                String name = null, value = null;
                if (nameTok.hasMoreTokens())
                    name = nameTok.nextToken();
                if (nameTok.hasMoreTokens())
                    value = nameTok.nextToken();
                if (nameTok.hasMoreTokens() || name == null || value == null)
                    throw new IllegalArgumentException("TODO : i18n : Invalid property syntax." + propsString);
                        //strings.getLocalString("InvalidPropertySyntax",
                        //    "Invalid property syntax.", propsString));
                int index = value.indexOf(']');
               
                String unbracedValue =index > 0 ? value.substring(0,index) : value;

                properties.setProperty(name, unbracedValue);
            }
        }
        return properties;
    }

    /**
     * Returns the element name used by the parent to store instances of the child
     *
     * @param document the dom document this configuration element lives in.
     * @param parent type of the parent
     * @param child type of the child
     * @return the element name holding child's instances in the parent
     * @throws ClassNotFoundException when subclasses cannot be loaded
     */
    public static String elementName(DomDocument document, Class<?> parent, Class<?> child)
        throws ClassNotFoundException {

        ConfigModel cm = document.buildModel(parent);
        for (String elementName : cm.getElementNames()) {
            ConfigModel.Property prop = cm.getElement(elementName);
            if (prop instanceof ConfigModel.Node) {
                ConfigModel childCM = ((ConfigModel.Node) prop).getModel();
                String childTypeName = childCM.targetTypeName;
                if (childTypeName.equals(child.getName())) {
                    return childCM.getTagName();
                }
                // check the inheritance hierarchy
                List<ConfigModel> subChildrenModels = document.getAllModelsImplementing(
                        childCM.classLoaderHolder.loadClass(childTypeName));
                if (subChildrenModels!=null) {
                    for (ConfigModel subChildModel : subChildrenModels) {
                        if (subChildModel.targetTypeName.equals(child.getName())) {
                            return subChildModel.getTagName();
                        }
                    }
                }

            }
        }
        return null;
    }
    
    /** Decorator class if particular command
     */
    public abstract Class getDecoratorClass();
 
}
