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

package com.sun.enterprise.config.util.zeroconfig;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigInjector;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains utility methods for zero-config
 *
 * @author Masoud Kalali
 */
public final class ZeroConfigUtils {
    private static final Logger LOG = Logger.getLogger(ZeroConfigUtils.class.getName());

    /**
     * If exists, locate and return a URL to the configuration snippet for the given config bean class.
     *
     * @param configBeanClass the config bean type we want to check for its configuration snippet
     * @param <U>             the type of the config bean we want to check
     * @return A url to the file or null of not exists
     */
    public static <U extends ConfigBeanProxy> URL getConfigurationFileUrl(Class<U> configBeanClass) {
        String defaultConfigurationFileName = configBeanClass.getSimpleName() + ".xml";
        return getConfigurationFileUrl(configBeanClass, defaultConfigurationFileName);
    }

    public static <U extends ConfigBeanProxy> URL getConfigurationFileUrl(Class<U> configBeanClass, String fileName) {
        return configBeanClass.getClassLoader().getResource("META-INF/" + fileName);
    }

    /**
     * If exists, locate and return a URL to the configuration snippet for the given config bean class.
     *
     * @param configBeanClass the config bean type we want to check for its configuration snippet
     * @return A url to the file or null of not exists
     */
    public static List<ConfigBeanDefaultValue> getDefaultConfigurations(Class configBeanClass) {
        Method m = getGetDefaultValuesMethod(configBeanClass);
        List<ConfigBeanDefaultValue> defaults = Collections.emptyList();
        if (m != null) {
            try {
                defaults = (List<ConfigBeanDefaultValue>) m.invoke(null);
            } catch (Exception e) {
                LOG.log(Level.INFO, "cannot get default configuration for: " + configBeanClass.getName(), e);
            }
        }
        return defaults;
    }

    public static boolean hasCustomConfig(Class configBeanClass) {
        return getGetDefaultValuesMethod(configBeanClass) != null;
    }

    /**
     * Find a getter method that returns a collection of the type we want to put set.
     *
     * @param owner     The class we want to search to find a method that returns a Collection typed with toSetType
     * @param typeToSet The type we want to find a matching collection fo
     * @return The Method that
     */
    private static Method findSuitableCollectionGetter(Class owner, Class typeToSet) {
        Method[] methods = owner.getMethods();
        for (Method m : methods) {
            if (m.getName().startsWith("get")) {
                Type t = m.getGenericReturnType();
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    Type actualGenericParameter = pt.getActualTypeArguments()[0];
                    if (pt.getActualTypeArguments().length == 1) {
                        if (Collection.class.isAssignableFrom(m.getReturnType())) {
                            if (actualGenericParameter instanceof Class) {
                                if (typeToSet.isAssignableFrom((Class) actualGenericParameter)) {
                                    return m;
                                }
                            }
                        }
                    }
                }
            }
        }
        return findDeeperSuitableCollectionGetter(owner, typeToSet);
    }

    private static Method findDeeperSuitableCollectionGetter(Class owner, Class typeToSet) {

        Class[] ifs = typeToSet.getInterfaces();
        Method[] methods = owner.getMethods();
        for (Method m : methods) {
            if (m.getName().startsWith("get")) {
                Type t = m.getGenericReturnType();
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    Type actualGenericParameter = pt.getActualTypeArguments()[0];
                    if (pt.getActualTypeArguments().length == 1) {
                        if (Collection.class.isAssignableFrom(m.getReturnType())) {
                            if (actualGenericParameter instanceof Class) {
                                if (checkInterfaces(ifs, actualGenericParameter)) {
                                    return m;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean checkInterfaces(Class[] ifs, Type actualGenericParameter) {
        for (Class clz : ifs) {
            if (clz.getSimpleName().equals("ConfigBeanProxy") || clz.getSimpleName().equals("Injectable")
                    || clz.getSimpleName().equals("PropertyBag")) {
                continue;
            }
            if (clz.isAssignableFrom((Class) actualGenericParameter)) {
                return true;
            }
        }
        return false;
    }

    private static Class getOwningClassForLocation(String location, Habitat habitat) {
        StringTokenizer tokenizer = new StringTokenizer(location, "/", false);
        if (!tokenizer.hasMoreElements()) return null;
        if (!tokenizer.nextToken().equalsIgnoreCase("domain")) return null;
        String level = "domain";
        if (location.equalsIgnoreCase("domain/configs")) return getClassFor("domain", habitat);
        //It is a named config so we shall just return the config class
        if ((tokenizer.countTokens() == 2) && location.startsWith("domain/configs/")) {
            return Config.class;
        }
        while (tokenizer.hasMoreElements()) {
            level = tokenizer.nextToken();
        }
        return getClassFor(level, habitat);
    }


    public static ConfigBeanProxy getOwningObject(String location, Habitat habitat) {

        //TODO MS4 We ignore anything at domain level for ms3 just config level
        if (!location.startsWith("domain/configs/")) return null;

        Class typeToFindGetter = getOwningClassForLocation(location, habitat);
        if (typeToFindGetter == null) {
            return null;
        }
        //if we want to set something at domain level, we already know it even if it is a whole new Config object
        if (typeToFindGetter.equals(Domain.class)) return habitat.getComponent(Domain.class);
        //Check if config object is where the location or it goes deeper in the config layers.
        StringTokenizer tokenizer = new StringTokenizer(location, "/", false);
        //something directly inside the config itself
        if (tokenizer.countTokens() == 3) {
            return habitat.getComponent(Domain.class).getConfigNamed(location.substring(location.lastIndexOf("/") + 1, location.length()));
        }

        location = location.substring(location.indexOf("/", "domain/configs".length()) + 1);
        tokenizer = new StringTokenizer(location, "/", false);
        String configName = tokenizer.nextToken();
        ConfigBeanProxy parent = habitat.getComponent(Domain.class).getConfigNamed(configName);

        String childElement;
        String parentElement = "Config";
        while (tokenizer.hasMoreTokens()) {
            try {
                childElement = tokenizer.nextToken();
                parent = getOwner(parent, parentElement, childElement, habitat);
                parentElement = childElement;
            } catch (Exception e) {
                LOG.log(Level.INFO, "cannot get parent config bean for: " + configName, e);
            }
        }
        return parent;
    }

    private static ConfigBeanProxy getOwner(ConfigBeanProxy parent, String parentElement, String childElement,
                                            Habitat habitat) throws InvocationTargetException, IllegalAccessException {
        if (childElement.endsWith("]")) {
            String componentName;
            String elementName;
            elementName = childElement.substring(childElement.lastIndexOf("/") + 1, childElement.indexOf("["));
            componentName = childElement.substring(childElement.lastIndexOf("[") + 1, childElement.indexOf("]"));
            Class childClass = getClassFor(elementName, habitat);
            Class parentClass = getClassFor(parentElement, habitat);
            Method m = ZeroConfigUtils.findSuitableCollectionGetter(parentClass, childClass);
            if (m != null) {
                try {
                    Collection col = (Collection) m.invoke(parent);
                    return getNamedConfigBeanFromCollection(col, componentName, childClass);
                } catch (Exception e) {
                    LOG.log(Level.INFO, "The provided path is not valid: " + childElement, e);
                }
            }
            return null;
        } else {
            Class clz = getClassFor(childElement, habitat);
            Method m = getMatchingGetterMethod(parent.getClass(), clz);
            if (m != null) {
                return (ConfigBeanProxy) m.invoke(parent);
            } else {

                try {
                    m = parent.getClass().getMethod("getExtensionByType", java.lang.Class.class);
                } catch (NoSuchMethodException e) {
                    LOG.log(Level.INFO, "Cannot find getExtensionByType", e);
                }
                if (m != null) {
                    return (ConfigBeanProxy) m.invoke(parent, clz);
                }
                return null;
            }
        }
    }

    public static <T extends ConfigBeanProxy> void setConfigBean(T finalConfigBean, ConfigBeanDefaultValue configBeanDefaultValue, Habitat habitat, ConfigBeanProxy parent) {

        Class clz = ZeroConfigUtils.getOwningClassForLocation(configBeanDefaultValue.getLocation(), habitat);
        Method m;
        m = getMatchingSetterMethod(clz, configBeanDefaultValue.getConfigBeanClass());
        if (m != null) {
            try {
                m.invoke(parent, finalConfigBean);
            } catch (Exception e) {
                LOG.log(Level.INFO, "cannot set ConfigBean for: " + finalConfigBean.getClass().getName(), e);
            }
            return;
        }

        m = ZeroConfigUtils.findSuitableCollectionGetter(clz, configBeanDefaultValue.getConfigBeanClass());
        if (m != null) {
            try {
                Collection col = (Collection) m.invoke(parent);
                if (configBeanDefaultValue.isReplaceCurrentIfExists()) {
                    String name = getNameForConfigBean(finalConfigBean, configBeanDefaultValue.getConfigBeanClass());
                    ConfigBeanProxy itemToRemove = getNamedConfigBeanFromCollection(col, name, configBeanDefaultValue.getConfigBeanClass());
                    col.remove(itemToRemove);
                }
                ((Collection) m.invoke(parent)).add(finalConfigBean);

            } catch (Exception e) {
                LOG.log(Level.INFO, "cannot set ConfigBean for: " + finalConfigBean.getClass().getName(), e);
            }
        }
    }

    private static <T extends ConfigBeanProxy> T getNamedConfigBeanFromCollection(Collection<T> col, String nameToLookFor, Class typeOfObjects) throws InvocationTargetException, IllegalAccessException {
        for (Object item : col) {
            String name = getNameForConfigBean(item, typeOfObjects);
            if (name.equalsIgnoreCase(nameToLookFor)) {
                return (T) item;
            }
        }
        return null;
    }

    private static String getNameForConfigBean(Object configBean, Class ConfigBeanType) throws InvocationTargetException, IllegalAccessException {
        Method[] methods = ConfigBeanType.getDeclaredMethods();
        for (Method method : methods) {
            Attribute attributeAnnotation = method.getAnnotation(Attribute.class);
            if ((attributeAnnotation != null) && attributeAnnotation.key()) {
                String name;
                return (String) method.invoke(configBean);
            }
        }
        return null;
    }

    /**
     * @param ins the InputStream to read and turn it into String
     * @return String equivalent of the stream
     */
    public static String streamToString(InputStream ins, String encoding) {
        return new Scanner(ins, encoding).useDelimiter("\\A").next();
    }

    /**
     * convert a configuration element name to representing class name
     *
     * @param name the configuration element name we want to convert to class name
     * @return the class name which the configuration element represent.
     */
    public static String convertConfigElementNameToClassName(String name) {
        // first, trim off the prefix
        StringTokenizer tokenizer = new StringTokenizer(name, "-", false);
        StringBuilder className = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String part = tokenizer.nextToken();
            part = part.replaceFirst(part.substring(0, 1), part.substring(0, 1).toUpperCase());
            className.append(part);

        }
        return className.toString();
    }

    public static <P extends ConfigBeanProxy> URL getDefaultSnippetUrl(Class<P> configBean) {
        String xmlSnippetFileLocation = "META-INF/" + configBean.getSimpleName() + ".xml";
        return configBean.getClassLoader().getResource(xmlSnippetFileLocation);
    }

    public static Class getClassFor(String serviceName, Habitat habitat) {
        serviceName = getServiceNameIfNamedComponent(serviceName);
        ConfigInjector injector = habitat.getComponent(ConfigInjector.class, serviceName);

        if (injector != null) {
            String clzName = injector.getClass().getName().substring(0, injector.getClass().getName().length() - 8);
            try {
                return injector.getClass().getClassLoader().loadClass(clzName);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private static String getServiceNameIfNamedComponent(String serviceName) {
        if (serviceName.endsWith("]")) {
            serviceName = serviceName.substring(0, serviceName.indexOf("["));
        }
        return serviceName;
    }

    public static String serializeConfigBeanByType(Class configBeanType, Habitat habitat) {
        ConfigBeanProxy configBeanProxy = getConfigBeanInstanceFor(configBeanType, habitat);
        return serializeConfigBean(configBeanProxy);
    }

    private static ConfigBeanProxy getConfigBeanInstanceFor(Class configBeanType, Habitat habitat) {
        return (ConfigBeanProxy) habitat.getComponent(configBeanType);

    }

    private static String serializeConfigBean(ConfigBeanProxy configBean) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = xmlFactory.createXMLStreamWriter(new BufferedOutputStream(bos));
            IndentingXMLStreamWriter indentingXMLStreamWriter = new IndentingXMLStreamWriter(writer);
            Dom configBeanDom = Dom.unwrap(configBean);
            configBeanDom.writeTo(configBeanDom.model.getTagName(), indentingXMLStreamWriter);
            indentingXMLStreamWriter.close();
        } catch (XMLStreamException e) {
            return null;
        }
        return bos.toString();
    }

    /**
     * Find a suitable getter method in the given class. the returned method represent a method that will return back a type of  methodReturnType.
     *
     * @param classToQuery     The class we want to find the getter in
     * @param methodReturnType the type we want to find the getter for
     * @return A Method object for a getter method in the classToQuery  which returns the    methodReturnType
     */
    private static Method getMatchingGetterMethod(Class classToQuery, Class methodReturnType) {
        Method[] methods = classToQuery.getMethods();
        for (Method method : methods) {
            if (method.getReturnType().getSimpleName().equals(methodReturnType.getSimpleName())) {
                return method;
            }
        }
        return null;
    }

    /**
     * Finds and return the setter method matching the class identified by   typeToSet
     *
     * @param classToQuery The ConfigLoader we want to inspect for presence of a setter method accepting class of type fqcn.
     * @param typeToSet    the type we want to find a setter for
     * @return the matching Method object or null if not present.
     */
    private static Method getMatchingSetterMethod(Class classToQuery, Class typeToSet) {
        String className = typeToSet.getName().substring(typeToSet.getName().lastIndexOf(".") + 1, typeToSet.getName().length());
        String setterName = "set" + className;
        Method[] methods = classToQuery.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(setterName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * checks and see if a class has an attribute with he specified name or not.
     *
     * @param classToQuery  the class toc heck the attribute presence
     * @param attributeName the attribute to check its presence in the class.
     * @return true if present and false if not.
     */
    private static boolean checkAttributePresence(Class classToQuery, String attributeName) {
        String fieldName = convertAttributeToPropertyName(attributeName);
        String methodName = "set" + fieldName.replaceFirst(fieldName.substring(0, 1), String.valueOf(Character.toUpperCase(fieldName.charAt(0))));
        Method[] methods = classToQuery.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * convert an xml attribute name to variable name representing it.
     *
     * @param attributeName the attribute name in "-" separated form as appears in the domain.xml
     * @return the class instance variable which represent that attributeName
     */
    private static String convertAttributeToPropertyName(String attributeName) {
        StringTokenizer tokenizer = new StringTokenizer(attributeName, "-", false);
        StringBuilder propertyName = new StringBuilder();
        boolean isFirst = true;
        while (tokenizer.hasMoreTokens()) {
            String part = tokenizer.nextToken();
            if (!isFirst) {
                part = part.replaceFirst(part.substring(0, 1), part.substring(0, 1).toUpperCase());
            }
            isFirst = false;
            propertyName.append(part);
        }
        return propertyName.toString();
    }

    public static boolean isConfigElementPresent(String serviceName, Habitat habitat, String target) {
        Class configBeanType = getClassFor(serviceName, habitat);
        Domain domain = habitat.getComponent(Domain.class);
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            Config c = domain.getConfigNamed(target);
            if (c.checkIfConfigExists(configBeanType)) {
                return true;
            }
        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            if (domain.checkIfConfigExists(configBeanType)) {
                return true;
            }
        }
        return false;
    }


    public static void addBeanToDomainXml(String serviceName, String target, Habitat habitat) {
        Class configBeanType = ZeroConfigUtils.getClassFor(serviceName, habitat);
        Domain domain = habitat.getComponent(Domain.class);
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            Config c = domain.getConfigNamed(target);
            c.getExtensionByType(configBeanType);
        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            //TODO to be developed during ms4
            domain.getExtensionByType(configBeanType);

        }
    }

    private static Class getDuckClass(Class configBeanType) {
        Class duck;
        final Class[] clz = configBeanType.getDeclaredClasses();
        for (Class aClz : clz) {
            duck = aClz;
            if (duck.getSimpleName().equals("Duck")) {
                return duck;
            }
        }
        return null;
    }

    private static Method getGetDefaultValuesMethod(Class configBeanType) {
        Class duck = getDuckClass(configBeanType);
        if (duck == null) {
            return null;
        }
        Method m;
        try {
            m = duck.getMethod("getDefaultValues");
        } catch (Exception ex) {
            return null;
        }
        return m;
    }

}


