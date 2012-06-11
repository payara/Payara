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
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigInjector;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.StringTokenizer;
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
        URL fileUrl = null;
        String defaultConfigurationFileName = configBeanClass.getSimpleName() + ".xml";
        //TODO get the config directory of the domain possibly trough ServerEnvironment class
        String externalSnippetDirectory = System.getProperty("com.sun.aas.instanceRoot") + "/config";
        String snippetDirectory = externalSnippetDirectory + "/snippets";
        File configFile = new File(snippetDirectory + "/" + defaultConfigurationFileName);
        if (configFile.isFile()) {
            // TODO: i18n, better error handling, use of new logging framework
            LOG.info("Using external configuration file for: " + configBeanClass.getSimpleName());
            try {
                fileUrl = configFile.toURI().toURL();
            } catch (MalformedURLException e) {
                // TODO catch adn handle
            }
        } else {
            // TODO: i18n, better error handling, use of new logging framework
            LOG.info("Using built-in configuration file for: " + configBeanClass.getSimpleName());
            fileUrl = configBeanClass.getClassLoader().getResource("META-INF/" + defaultConfigurationFileName);
        }
        return fileUrl;
    }

    /**
     * @param ins the InputStream to read and turn it into String
     * @return String equivalent of the stream
     * @throws IOException
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
        String className = convertConfigElementNameToClassName(serviceName);
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

    public static String serializeConfigBeanByType(Class configBeanType, Habitat habitat) {
        ConfigBeanProxy configBeanProxy = getConfigBeanInstanceFor(configBeanType, habitat);
        return serializeConfigBean(configBeanProxy);
    }

    public static ConfigBeanProxy getConfigBeanInstanceFor(Class configBeanType, Habitat habitat) {
        return (ConfigBeanProxy) habitat.getComponent(configBeanType);

    }

    public static String serializeConfigBean(ConfigBeanProxy configBean) {
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
    public static Method getMatchingGetterMethod(Class classToQuery, Class methodReturnType) {
        Method[] methods = classToQuery.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getReturnType().getSimpleName().equals(methodReturnType.getSimpleName())) {
                return methods[i];
            }
        }
        return null;
    }


    /**
     * Finds and return the setter method matching the class named fqcn in the configLoader
     *
     * @param classToQuery The ConfigLoader we want to inspect for presence of a setter method accepting class of type fqcn.
     * @param fqcn         the fully qualified class name to find its setter in the configLoader
     * @return the matching Method object or null if not present.
     */
    public static Method getMatchingSetterMethod(Class classToQuery, String fqcn) {
        String className = fqcn.substring(fqcn.lastIndexOf(".") + 1, fqcn.length());
        String setterName = "set" + className;
        Method[] methods = classToQuery.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equalsIgnoreCase(setterName)) {
                return methods[i];
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
    public static boolean checkAttributePresence(Class classToQuery, String attributeName) {
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

        // first, trim off the prefix
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


    private boolean hasMatchingConfigBean(String serviceName, Habitat habitat) {
        Class clz = ZeroConfigUtils.getClassFor(serviceName, (Habitat) habitat);
        return clz != null;
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
        Class configBeanType = ZeroConfigUtils.getClassFor(serviceName, (Habitat) habitat);
        Domain domain = habitat.getComponent(Domain.class);
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            Config c = domain.getConfigNamed(target);
            c.getExtensionByType(configBeanType);
        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            //TODO to be developed during ms4
            domain.getExtensionByType(configBeanType);

        }
    }


    public static Method getGetDefaultValuesMethod(Class configBeanType) {
        Method m = null;
        boolean customConfigFactoryPresent = false;
        Class duck = null;
        final Class[] clz = configBeanType.getClasses();
        for (int i = 0; i < clz.length; i++) {
            duck = clz[i];
            if (duck.getSimpleName().equals("Duck")) {
                customConfigFactoryPresent = true;
                break;
            }
        }
        if (customConfigFactoryPresent) {
            try {
                m = duck.getMethod("getDefaultValues", Config.class);
            } catch (NoSuchMethodException ex) {
                //TODO deal with ti
            } catch (SecurityException ex) {
                //TODO deal with ti
            }
        }
        return m;
    }

}


