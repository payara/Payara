/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.hk2.component.InhabitantsFile;
import com.sun.hk2.component.InjectionResolver;
import com.sun.hk2.component.LazyInhabitant;
import com.sun.logging.LogDomains;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModelProvider;
import org.glassfish.common.util.admin.ParamTokenizer;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Multiple;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.InjectionManager;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.config.*;
import org.jvnet.tiger_types.Types;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jvnet.hk2.annotations.InhabitantAnnotation;

/**
 * services pertinent to generic CRUD command implementations
 *
 * @author Jerome Dochez
 *
 */
public abstract class GenericCrudCommand implements CommandModelProvider, PostConstruct {
    
    private InjectionResolver<Param> injector;

    @Inject
    LazyInhabitant<?> myself;

    final protected static Logger logger = LogDomains.getLogger(GenericCrudCommand.class, LogDomains.ADMIN_LOGGER);
    final protected static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(GenericCrudCommand.class);

    String commandName;
    Class parentType=null;
    Class targetType=null;
    Method targetMethod;
    // default level of noise, useful for just swithching these classes in debugging.
    protected final Level level = Level.FINE;

    public void postConstruct() {
        List<String> indexes = myself.metadata().get(InhabitantsFile.INDEX_KEY);
        if (indexes.size()!=1) {
            StringBuffer sb = new StringBuffer();
            for (String index : indexes) {
                sb.append(index).append(" ");
            }
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.too_many_indexes",
                    "The metadata for this generic implementation has more than one index {0}",
                    sb.toString());
            Object[] params = new Object[] { sb.toString()};
            logger.log(Level.SEVERE, "GenericCrudCommand.too_many_indexes", params);
            throw new ComponentException(msg);
        }
        String index = indexes.get(0);
        if (index.indexOf(":")==-1) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.unamed_service",
                    "The service {0} is un-named, for generic command, the service name is the command name and must be provided",
                    index);
            Object[] params = new Object[] { index};
            logger.log(Level.SEVERE, "GenericCrudCommand.unamed_service", params);
            throw new ComponentException(msg);            
        }
        commandName = index.substring(index.indexOf(":")+1);
        String parentTypeName = myself.metadata().getOne(InhabitantsFile.TARGET_TYPE);
        String decoratedTypeName = myself.metadata().getOne(InhabitantsFile.DECORATED_TYPE);
        if (logger.isLoggable(level)) {
            logger.log(level,"Generic method parent targeted type is " + parentTypeName);
        }

        try {
            if (decoratedTypeName==null) {
                parentType = loadClass(parentTypeName);
            } else {
                parentType = loadClass(decoratedTypeName);
                targetType = loadClass(parentTypeName);
            }
        } catch(ClassNotFoundException e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.configbean_not_found",
                    "The Config Bean {0} cannot be loaded by the generic command implementation : {1}",
                    parentTypeName, e.getMessage());
            Object[] params = new Object[] { parentTypeName, e.getMessage()};
            logger.log(Level.SEVERE, "GenericCrudCommand.configbean_not_found",params);
            throw new ComponentException(msg, e);
        }

        // find now the accessor method.
        String methodName = myself.metadata().get("method-name").get(0);
        targetMethod=null;
        methodlookup:
        for (Method m : parentType.getMethods()) {
            if (m.getName().equals(methodName)) {
                // Make sure that this method is annotated with an annotation 
                // that is annotated with InhabitantAnnotation (such as @Create). 
                // This makes sure that we have found the method we are looking for
                // in case there is a like-named method that is not annotated.
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType().getAnnotation(InhabitantAnnotation.class) != null) {
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
            Object[] params = new Object[] { parentTypeName, methodName};
            logger.log(Level.SEVERE,"GenericCrudCommand.configbean_not_found", params);
            throw new ComponentException(msg);
        }

        if (targetType==null) {
            if (targetMethod.getParameterTypes().length==0) {
                if (targetMethod.getGenericReturnType() instanceof ParameterizedType) {
                    targetType = Types.erasure(Types.getTypeArgument(
                                targetMethod.getGenericReturnType(),0));
                } else {
                    targetType =targetMethod.getReturnType();
                }

            } else {
                targetType = targetMethod.getParameterTypes()[0];
            }
        }
    }

    protected <T extends Annotation> T getAnnotation(Method target, Class<T> type) {


        T annotation = targetMethod.getAnnotation(type);
        if (annotation==null) {
            // we need to check for any annotation that has the @Multiple annotation
            for (Annotation a : targetMethod.getAnnotations()) {
                Multiple multiple = a.annotationType().getAnnotation(Multiple.class);
                if (multiple!=null) {
                    try {
                        Method m = a.getClass().getMethod("value");
                        Annotation[] potentials = (Annotation[]) m.invoke(a);
                        if (potentials!=null) {
                            for (Annotation potential : potentials) {
                                if (potential.annotationType().equals(type)) {
                                    m = potential.getClass().getMethod("value");
                                    String value = (String) m.invoke(potential);
                                    if (value.equals(commandName)) {
                                        return type.cast(potential);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            // still not found, it may have been placed on the target type using a @Decorate.
            String decoratedTypeName = myself.metadata().getOne(InhabitantsFile.TARGET_TYPE);
            try {
                if (decoratedTypeName!=null) {
                    Class decoratedType = myself.getClassLoader().loadClass(decoratedTypeName);
                    annotation = (T) decoratedType.getAnnotation(type);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            if (annotation!=null) {
                return annotation;
            }

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
            public <V> V getValue(Object component, Inhabitant<?> onBehalfOf, AnnotatedElement annotated, Type genericType, Class<V> type) throws ComponentException {
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
                            Object[] params = new Object[] { annotated.getClass().toString()};
                            logger.log(Level.SEVERE, "GenericCrudCommand.invalid_type", params);
                            throw new ComponentException(msg);
                        }
                    } catch (IllegalAccessException e) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.invocation_failure",
                                "Failure {0} while getting List<?> values from component",
                                e.getMessage());
                        Object[] params = new Object[] { e.getMessage()};
                        logger.log(Level.SEVERE, "GenericCrudCommand.invocation_failure", params);
                        throw new ComponentException(msg, e);
                    } catch (InvocationTargetException e) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.invocation_failure",
                                "Failure {0} while getting List<?> values from component",
                                e.getMessage());
                        Object[] params = new Object[] { e.getMessage()};
                        logger.log(Level.SEVERE, "GenericCrudCommand.invocation_failure", params);
                        throw new ComponentException(msg, e);
                    }
                    Object value = delegate.getValue(component, null, annotated, genericType, type);
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
                        throw new ComponentException("Cannot determine parametized type from " + annotated.toString());
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
                            Object[] params = new Object[] {annotated.toString()};
                            logger.log(Level.SEVERE, "GenericCrudCommand.nongeneric_type", params);
                            throw new ComponentException(msg);
                    }
                    if (!ConfigBeanProxy.class.isAssignableFrom(itemType)) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.wrong_type",
                                "The generic type {0} is not supported, only List<? extends ConfigBeanProxy> is",
                                annotated.toString());
                        Object[] params = new Object[] { annotated.toString()};
                        logger.log(Level.SEVERE, "GenericCrudCommand.wrong_type", params);
                        throw new ComponentException(msg);
                        
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
                        Object[] params = new Object[] { e.getMessage(), itemType.getName()};
                        logger.log(Level.SEVERE, "GenericCrudCommand.introspection_failure", params);
                        throw new ComponentException(msg, e);
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
                                public <V> V getValue(Object component, Inhabitant<?> onBehalfOf, AnnotatedElement annotated, Type genericType, Class<V> type) throws ComponentException {
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
                            Object[] params = new Object[] { transactionFailure.getMessage(), itemType};
                            logger.log(Level.SEVERE, "GenericCrudCommand.transactionException", params);
                            throw new ComponentException(msg, transactionFailure);
                        }

                    }
                    return null;
                }
                return delegate.getValue(component, null, annotated, genericType, type);
            }

            @Override
            public boolean isOptional(AnnotatedElement annotated, Param annotation) {
                return annotation.optional();
            }
        };
        
    }

    protected Class loadClass(String type) throws ClassNotFoundException {
        // by default I use the inhabitant class loader
        return myself.getClassLoader().loadClass(type);
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
                        childCM.classLoaderHolder.get().loadClass(childTypeName));
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
 
}
