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

package com.sun.enterprise.config.modularity;

import static com.sun.enterprise.config.util.ConfigApiLoggerInfo.*;

import com.sun.enterprise.config.modularity.annotation.CustomConfiguration;
import com.sun.enterprise.config.modularity.annotation.HasCustomizationTokens;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;
import com.sun.enterprise.config.modularity.parser.ConfigurationPopulator;
import com.sun.enterprise.config.modularity.parser.ModuleXMLConfigurationFileParser;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Module;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.config.serverbeans.SystemPropertyBag;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.config.support.Singleton;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigInjector;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.beans.PropertyVetoException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains utility methods for zero-config
 *
 * @author Masoud Kalali
 */
@Service
@Singleton
public final class ConfigModularityUtils {
    
    private static final Logger LOG = getLogger();

    @Inject
    private ServiceLocator serviceLocator;
    @Inject
    private StartupContext context;

    private boolean ignorePersisting = false;
    private boolean isCommandInvocation = false;

    public <U extends ConfigBeanProxy> URL getConfigurationFileUrl(Class<U> configBeanClass, String baseFileName, String runtimeType) {
        //TODO can be optimized a little by checking the default file...
        String fileName = runtimeType + "-" + baseFileName;
        URL fileUrl = configBeanClass.getClassLoader().getResource("META-INF/configuration/" + fileName);
        if (fileUrl == null) {
            fileUrl = configBeanClass.getClassLoader().getResource("META-INF/configuration/" + baseFileName);
        }
        return fileUrl;
    }

    /**
     * If exists, locate and return a URL to the configuration snippet for the given config bean class.
     *
     * @param configBeanClass the config bean type we want to check for its configuration snippet
     * @return A url to the file or null of not exists
     */
    public List<ConfigBeanDefaultValue> getDefaultConfigurations(Class configBeanClass, String runtimeType) {

        //Determine if it is DAS or instance
        CustomConfiguration c = (CustomConfiguration) configBeanClass.getAnnotation(CustomConfiguration.class);
        List<ConfigBeanDefaultValue> defaults = Collections.emptyList();
        if (c.usesOnTheFlyConfigGeneration()) {
            Method m = getGetDefaultValuesMethod(configBeanClass);
            if (m != null) {
                try {
                    defaults = (List<ConfigBeanDefaultValue>) m.invoke(null, runtimeType);
                } catch (Exception e) {
                    LogHelper.log(LOG, Level.INFO, cannotGetDefaultConfig, e,configBeanClass.getName());
                }
            }
        } else {
            //TODO properly handle the exceptions
            LocalStringManager localStrings =
                    new LocalStringManagerImpl(configBeanClass);
            ModuleXMLConfigurationFileParser parser = new ModuleXMLConfigurationFileParser(localStrings);
            try {
                defaults = parser.parseServiceConfiguration(getConfigurationFileUrl(configBeanClass, c.baseConfigurationFileName(), runtimeType).openStream());
            } catch (XMLStreamException e) {
                LOG.log(Level.SEVERE, cannotParseDefaultDefaultConfig, e);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, cannotParseDefaultDefaultConfig, e);
            }
        }
        return defaults;
    }

    public boolean hasCustomConfig(Class configBeanClass) {
        return configBeanClass.getAnnotation(CustomConfiguration.class) != null;
    }

    /**
     * Find a getter method that returns a collection of the type we want to put set.
     *
     * @param owner     The class we want to search to find a method that returns a Collection typed with toSetType
     * @param typeToSet The type we want to find a matching collection fo
     * @return The Method that
     */
    public Method findSuitableCollectionGetter(Class owner, Class typeToSet) {
        Method[] methods = owner.getMethods();
        Method tm = returnException(owner, typeToSet);
        if (tm != null) return tm;
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
                                    if ((m.getAnnotation(DuckTyped.class) != null)) {
                                        return m;
                                    } else {
                                        Method deepM = findDeeperSuitableCollectionGetter(owner, typeToSet);
                                        return deepM != null ? deepM : m;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return findDeeperSuitableCollectionGetter(owner, typeToSet);
    }

    private Method returnException(Class owner, Class typeToSet) {
        if (owner.isAssignableFrom(Applications.class) && (typeToSet.isAssignableFrom(Application.class)) || typeToSet.isAssignableFrom(Module.class)) {
            try {
                Method m = owner.getMethod("getModules");
                return m;
            } catch (NoSuchMethodException e) {
                LogHelper.log(LOG, Level.INFO, noMethodInReturnException, e, 
                        owner.getName(), typeToSet.getName());
            }
        }
        return null;
    }

    public Method findDeeperSuitableCollectionGetter(Class owner, Class typeToSet) {
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

    public boolean checkInterfaces(Class[] ifs, Type actualGenericParameter) {
        for (Class clz : ifs) {
            if (clz.getSimpleName().equals("ConfigBeanProxy") || clz.getSimpleName().equals("Injectable")
                    || clz.getSimpleName().equals("PropertyBag")) {
                continue;
            }
            if (actualGenericParameter instanceof Class && clz.isAssignableFrom((Class) actualGenericParameter)) {
                return true;
            }
        }
        return false;
    }

    public Class getOwningClassForLocation(String location) {
        StringTokenizer tokenizer = new StringTokenizer(location, "/", false);
        if (!tokenizer.hasMoreElements()) return null;
        if (!tokenizer.nextToken().equalsIgnoreCase("domain")) return null;
        String level = "domain";
        if (location.equalsIgnoreCase("domain/configs")) return getClassFor("domain");
        //It is a named config so we shall just return the config class
        if ((tokenizer.countTokens() == 2) && location.startsWith("domain/configs")) {
            return Config.class;
        }
        while (tokenizer.hasMoreElements()) {
            level = tokenizer.nextToken();
        }
        return getClassFor(level);
    }


    public ConfigBeanProxy getOwningObject(String location) {
        if (!location.startsWith("domain/configs")) {
            if (!location.startsWith("domain")) {
                //Sorry only know domain and below :D
                return null;
            }
            StringTokenizer tokenizer = new StringTokenizer(location, "/", false);
            //something directly inside the domain itself as we know one token is domain for sure
            if (tokenizer.countTokens() == 1) {
                return serviceLocator.getService(Domain.class);
            }
            location = location.substring(location.indexOf("/", "domain".length()) + 1);
            tokenizer = new StringTokenizer(location, "/", false);
            ConfigBeanProxy parent = serviceLocator.getService(Domain.class);

            //skipping the domain itself as a token, we know it and took it away.
            String parentElement = "domain";
            String childElement = null;
            while (tokenizer.hasMoreTokens()) {
                try {
                    childElement = tokenizer.nextToken();
                    parent = getOwner(parent, parentElement, childElement);
                    parentElement = childElement;
                } catch (Exception e) {
                    LogHelper.log(LOG, Level.INFO, cannotGetParentConfigBean, e, childElement);
                }
            }
            return parent;
        } else {
            Class typeToFindGetter = getOwningClassForLocation(location);
            if (typeToFindGetter == null) {
                return null;
            }

            //Check if config object is where the location or it goes deeper in the config layers.
            StringTokenizer tokenizer = new StringTokenizer(location, "/", false);
            //something directly inside the config itself
            if (tokenizer.countTokens() == 3) {
                String expression = location.substring(location.lastIndexOf("[") + 1, location.length() - 1);
                String configName = resolveExpression(expression);
                return serviceLocator.<Domain>getService(Domain.class).getConfigNamed(configName);
            }

            location = location.substring(location.indexOf("/", "domain/configs".length()) + 1);
            tokenizer = new StringTokenizer(location, "/", false);
            String curLevel = tokenizer.nextToken();
            String expression;
            if (curLevel.contains("[")) {
                expression = curLevel.substring(curLevel.lastIndexOf("[") + 1, curLevel.length() - 1);
            } else {
                expression = curLevel;
            }

            String configName = resolveExpression(expression);
            ConfigBeanProxy parent = serviceLocator.<Domain>getService(Domain.class).getConfigNamed(configName);

            String childElement;
            String parentElement = "Config";
            while (tokenizer.hasMoreTokens()) {
                try {
                    childElement = tokenizer.nextToken();
                    parent = getOwner(parent, parentElement, childElement);
                    parentElement = childElement;
                } catch (Exception e) {
                    LogHelper.log(LOG, Level.INFO, cannotGetParentConfigBean, e, configName);
                }
            }
            return parent;
        }
    }

    public ConfigBeanProxy getOwner(ConfigBeanProxy parent, String parentElement, String childElement) throws InvocationTargetException, IllegalAccessException {

        if (childElement.contains("CURRENT_INSTANCE_CONFIG_NAME")) {
            return serviceLocator.<Config>getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
        }
        if (childElement.contains("CURRENT_INSTANCE_SERVER_NAME")) {
            return serviceLocator.<Server>getService(Server.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
        }
        if (childElement.endsWith("]")) {
            String componentName;
            String elementName;
            elementName = childElement.substring(childElement.lastIndexOf("/") + 1, childElement.indexOf("["));
            componentName = childElement.substring(childElement.lastIndexOf("[") + 1, childElement.indexOf("]"));
            Class childClass = getClassFor(elementName);
            Class parentClass = getClassFor(parentElement);
            Method m = findSuitableCollectionGetter(parentClass, childClass);
            if (m != null) {
                try {
                    Collection col = (Collection) m.invoke(parent);
                    componentName = resolveExpression(componentName);
                    return getNamedConfigBeanFromCollection(col, componentName, childClass);
                } catch (Exception e) {
                    LogHelper.log(LOG, Level.INFO, invalidPath, e, childElement,componentName);
                }
            }
            return null;
        } else {
            Class clz = getClassFor(childElement);
            if (parent == null) return null;
            Method m = getMatchingGetterMethod(parent.getClass(), clz);
            if (m != null) {
                return (ConfigBeanProxy) m.invoke(parent);
            } else {

                try {
                    m = parent.getClass().getMethod("getExtensionByType", java.lang.Class.class);
                } catch (NoSuchMethodException e) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Cannot find getExtensionByType", e);
                    }
                }
                if (m != null) {
                    return (ConfigBeanProxy) m.invoke(parent, clz);
                }
                return null;
            }
        }
    }

    public <U extends ConfigBeanProxy> List<U> getExtensions(ConfigBeanProxy parent) {

        Method m = null;
        try {
            if (parent != null) {
                m = parent.getClass().getMethod("getExtensions");
            }
        } catch (NoSuchMethodException e) {
        }
        if (m != null) {
            try {
                return (List<U>) m.invoke(parent);
            } catch (Exception e) {

            }
        }
        return Collections.emptyList();
    }

    public <T extends ConfigBeanProxy> T setConfigBean(T finalConfigBean, ConfigBeanDefaultValue configBeanDefaultValue, ConfigBeanProxy parent) {
        Class owningClassForLocation = getOwningClassForLocation(configBeanDefaultValue.getLocation());
        Class configBeanClass = getClassForFullName(configBeanDefaultValue.getConfigBeanClassName());

        try {
            ConfigBeanProxy configBeanInstance = null;
            if (getNameForConfigBean(finalConfigBean, configBeanClass) == null) {
                List<ConfigBeanProxy> extensions = getExtensions(parent);
                for (ConfigBeanProxy extension : extensions) {
                    try {
                        configBeanInstance = (ConfigBeanProxy) configBeanClass.cast(extension);
                        break;
                    } catch (Exception e) {
                        // ignore, not the right type.
                    }
                }
                if (!configBeanDefaultValue.replaceCurrentIfExists() || !stackPositionHigher(finalConfigBean, configBeanInstance)) {
                    if (configBeanInstance != null) return (T) configBeanInstance;
                }
                if (configBeanInstance != null) {
                    extensions.remove(configBeanInstance);
                }
            }

        } catch (InvocationTargetException e) {
            LOG.log(Level.INFO, cannotSetConfigBean, e);
        } catch (IllegalAccessException e) {
            LOG.log(Level.INFO, cannotSetConfigBean, e);
        }

        Method m = getMatchingSetterMethod(owningClassForLocation, configBeanClass);
        if (m != null) {
            try {
                if (configBeanClass.getAnnotation(HasCustomizationTokens.class) != null) {
                    applyCustomTokens(configBeanDefaultValue, finalConfigBean, parent);
                }
                m.invoke(parent, finalConfigBean);
            } catch (Exception e) {
                LogHelper.log(LOG, Level.INFO, cannotSetConfigBeanFor, e, finalConfigBean.getClass().getName());
            }
            return finalConfigBean;
        }

        m = findSuitableCollectionGetter(owningClassForLocation, configBeanClass);
        if (m != null) {
            try {
                Collection col = (Collection) m.invoke(parent);
                String name = getNameForConfigBean(finalConfigBean, configBeanClass);
                ConfigBeanProxy itemToRemove = getNamedConfigBeanFromCollection(col, name, configBeanClass);
                if (configBeanDefaultValue.replaceCurrentIfExists()) {
                    try {
                        if (itemToRemove != null) {
                            if (stackPositionHigher(finalConfigBean, itemToRemove)) {
                                col.remove(itemToRemove);
                            }
                        }
                    } catch (Exception ex) {
                        LogHelper.log(LOG, Level.INFO, cannotRemoveConfigBean, ex, finalConfigBean.getClass().getName());
                    }
                }
                if (configBeanClass.getAnnotation(HasCustomizationTokens.class) != null) {
                    applyCustomTokens(configBeanDefaultValue, finalConfigBean, parent);
                }
                if (itemToRemove != null && !configBeanDefaultValue.replaceCurrentIfExists()) {
                    //Check for duplication here.
                    if (((ConfigView) Proxy.getInvocationHandler(itemToRemove)).getProxyType().isAssignableFrom(configBeanClass)) {
                        return finalConfigBean;
                    }
                }
                col.add(finalConfigBean);
                return finalConfigBean;
            } catch (Exception e) {
                LogHelper.log(LOG, Level.INFO, cannotSetConfigBeanFor, e, finalConfigBean.getClass().getName());
            }
        }
        return null;
    }

    public <T extends ConfigBeanProxy> boolean stackPositionHigher(T finalConfigBean, ConfigBeanProxy itemToRemove) {
        if (itemToRemove == null || finalConfigBean == null) return true;
        if (RankedConfigBeanProxy.class.isAssignableFrom(finalConfigBean.getClass()) && RankedConfigBeanProxy.class.isAssignableFrom(itemToRemove.getClass())) {
            int itemToRemoveRank = Integer.parseInt(((RankedConfigBeanProxy) itemToRemove).getRank());
            int finalConfigBeanRank = Integer.parseInt(((RankedConfigBeanProxy) finalConfigBean).getRank());
            return finalConfigBeanRank > itemToRemoveRank;
        } else {
            return true;
        }

    }

    public synchronized <T extends ConfigBeanProxy> void applyCustomTokens(final ConfigBeanDefaultValue configBeanDefaultValue,
                                                                           T finalConfigBean, ConfigBeanProxy parent)
            throws TransactionFailure, PropertyVetoException {
        //go up in the parents tree till meet someone ImplementingSystemProperty
        //then that is the freaking parent, get it and set the SystemProperty :D
        if (parent instanceof SystemPropertyBag) {
            addSystemPropertyForToken(configBeanDefaultValue.getCustomizationTokens(), (SystemPropertyBag) parent);
        } else {
            ConfigBeanProxy curParent = finalConfigBean;
            while (!(curParent instanceof SystemPropertyBag)) {
                curParent = curParent.getParent();
            }
            if (configBeanDefaultValue.getCustomizationTokens().size() != 0) {
                final boolean oldIP = isIgnorePersisting();
                try {
                    setIgnorePersisting(true);
                    final SystemPropertyBag bag = (SystemPropertyBag) curParent;
                    final List<ConfigCustomizationToken> tokens = configBeanDefaultValue.getCustomizationTokens();
                    ConfigSupport.apply(new SingleConfigCode<SystemPropertyBag>() {
                        public Object run(SystemPropertyBag param) throws PropertyVetoException, TransactionFailure {
                            addSystemPropertyForToken(tokens, bag);
                            return param;
                        }
                    }, bag);
                } finally {
                    setIgnorePersisting(oldIP);
                }
            }
        }
    }

    public void addSystemPropertyForToken(List<ConfigCustomizationToken> tokens, SystemPropertyBag bag)
            throws TransactionFailure, PropertyVetoException {
        for (ConfigCustomizationToken token : tokens) {
            if (!bag.containsProperty(token.getName())) {
                SystemProperty prop = bag.createChild(SystemProperty.class);
                prop.setName(token.getName());
                prop.setDescription(token.getDescription());
                prop.setValue(token.getValue());
                bag.getSystemProperty().add(prop);
            }
        }
    }

    public <T extends ConfigBeanProxy> T getCurrentConfigBeanForDefaultValue(ConfigBeanDefaultValue defaultValue)
            throws InvocationTargetException, IllegalAccessException {
        //TODO make this method target aware!
        Class parentClass = getOwningClassForLocation(defaultValue.getLocation());
        Class configBeanClass = getClassForFullName(defaultValue.getConfigBeanClassName());
        Method m = findSuitableCollectionGetter(parentClass, configBeanClass);
        if (m != null) {
            ConfigParser configParser = new ConfigParser(serviceLocator);
            // I don't use the GlassFish document here as I don't need persistence
            final DomDocument doc = new DomDocument<GlassFishConfigBean>(serviceLocator) {
                @Override
                public Dom make(final ServiceLocator serviceLocator, XMLStreamReader xmlStreamReader, GlassFishConfigBean dom,
                                ConfigModel configModel) {
                    // by default, people get the translated view.
                    return new GlassFishConfigBean(serviceLocator, this, dom, configModel, xmlStreamReader);
                }
            };

            ConfigBeanProxy parent = getOwningObject(defaultValue.getLocation());
            ConfigurationPopulator populator = new ConfigurationPopulator(defaultValue.getXmlConfiguration(), doc, parent);
            populator.run(configParser);
            ConfigBeanProxy configBean = doc.getRoot().createProxy(configBeanClass);
            Collection col = (Collection) m.invoke(parent);
            return (T) getConfigBeanFromCollection(col, configBean, configBeanClass);

        }
        return null;
    }

    public <T extends ConfigBeanProxy> T getConfigBeanFromCollection(Collection<T> col, T configBeanObject,
                                                                     Class typeOfObjects)
            throws InvocationTargetException, IllegalAccessException {
        String nameToLookFor = getNameForConfigBean(configBeanObject, typeOfObjects);
        if (nameToLookFor != null) {
            T returnee = getNamedConfigBeanFromCollection(col, nameToLookFor, typeOfObjects);
            if (returnee != null) return returnee;
        } else {
            for (T configBean : col) {
                try {
                    typeOfObjects.cast(configBean);
                    return configBean;
                } catch (Exception ex) {
                    //ignore it
                }
            }
        }
        return null;
    }

    public <T extends ConfigBeanProxy> T getNamedConfigBeanFromCollection(Collection<T> col,
                                                                          String nameToLookFor,
                                                                          Class typeOfObjects)
            throws InvocationTargetException, IllegalAccessException {
        if (nameToLookFor == null) return null;
        for (Object item : col) {
            if (!((ConfigView) Proxy.getInvocationHandler(item)).getProxyType().isAssignableFrom(typeOfObjects)) {
                continue;
            }
            String name = getNameForConfigBean(item, typeOfObjects);
            if (nameToLookFor.equalsIgnoreCase(name)) {
                return (T) item;
            }
        }
        return null;
    }

    public String getNameForConfigBean(Object configBean, Class configBeanType) throws InvocationTargetException, IllegalAccessException {
        if (configBean instanceof Named) {
            Named nme = (Named) configBean;
            return nme.getName();
        }
        if (configBean instanceof Resource) {
            Resource res = (Resource) configBean;
            return res.getIdentity();
        }
        Method[] methods = configBeanType.getMethods();
        for (Method method : methods) {
            Attribute attributeAnnotation = method.getAnnotation(Attribute.class);
            if ((attributeAnnotation != null) && attributeAnnotation.key()) {
                return (String) method.invoke(configBean);
            }
        }
        return null;
    }

    /**
     * convert a configuration element name to representing class name
     *
     * @param name the configuration element name we want to convert to class name
     * @return the class name which the configuration element represent.
     */
    public String convertConfigElementNameToClassName(String name) {
        // first, trim off the prefix
        StringTokenizer tokenizer = new StringTokenizer(name, "-", false);
        StringBuilder className = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String part = tokenizer.nextToken();
            Locale loc = Locale.getDefault();
            part = part.replaceFirst(part.substring(0, 1), part.substring(0, 1).toUpperCase(loc));
            className.append(part);

        }
        return className.toString();
    }


    public Class getClassFor(String serviceName) {
        serviceName = getServiceTypeNameIfNamedComponent(serviceName);
        ConfigInjector injector = serviceLocator.getService(ConfigInjector.class, serviceName.toLowerCase(Locale.getDefault()));
        return getClassFromInjector(injector);
    }

    public Class getClassFromInjector(ConfigInjector injector) {
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

    public String getServiceTypeNameIfNamedComponent(String serviceName) {
        if (serviceName.endsWith("]")) {
            serviceName = serviceName.substring(0, serviceName.indexOf("["));
        }
        return serviceName;
    }

    public String resolveExpression(String expression) {
        if (expression.startsWith("$")) {
            String name = expression.substring(1, expression.length());
            if (name.equalsIgnoreCase("CURRENT_INSTANCE_CONFIG_NAME")) {
                expression = serviceLocator.<Config>getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME).getName();
            }
            if (name.equalsIgnoreCase("CURRENT_INSTANCE_SERVER_NAME")) {
                expression = serviceLocator.<Server>getService(Server.class, ServerEnvironment.DEFAULT_INSTANCE_NAME).getName();
            }
        }
        return expression;
    }

    public String serializeConfigBeanByType(Class configBeanType) {
        ConfigBeanProxy configBeanProxy = getConfigBeanInstanceFor(configBeanType);
        return serializeConfigBean(configBeanProxy);
    }

    public ConfigBeanProxy getConfigBeanInstanceFor(Class configBeanType) {
        return (ConfigBeanProxy) serviceLocator.getService(configBeanType);
    }

    public String serializeConfigBean(ConfigBeanProxy configBean) {
        if (configBean == null) return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = null;
        IndentingXMLStreamWriter indentingXMLStreamWriter = null;
        String s = null;
        try {
            writer = xmlFactory.createXMLStreamWriter(new BufferedOutputStream(bos));
            indentingXMLStreamWriter = new IndentingXMLStreamWriter(writer);
            Dom configBeanDom = Dom.unwrap(configBean);
            configBeanDom.writeTo(configBeanDom.model.getTagName(), indentingXMLStreamWriter);
            indentingXMLStreamWriter.flush();
            s = bos.toString();
        } catch (XMLStreamException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Cannot serialize the configbean: " + configBean.toString(), e);
            }
            return null;
        } finally {
            try {
                if (bos != null)
                    bos.close();
                if (writer != null)
                    writer.close();
                if (indentingXMLStreamWriter != null)
                    indentingXMLStreamWriter.close();
            } catch (IOException e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Cannot serialize the configbean: " + configBean.toString(), e);
                }
            } catch (XMLStreamException e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Cannot serialize the configbean: " + configBean.toString(), e);
                }
            }

        }
        return s;
    }

    /**
     * Find a suitable getter method in the given class. the returned method represent a method that will return back a type of  methodReturnType.
     *
     * @param classToQuery     The class we want to find the getter in
     * @param methodReturnType the type we want to find the getter for
     * @return A Method object for a getter method in the classToQuery  which returns the    methodReturnType
     */
    public Method getMatchingGetterMethod(Class classToQuery, Class methodReturnType) {
        Method[] methods = classToQuery.getMethods();
        for (Method method : methods) {
            Class<?> rt = method.getReturnType();
            if (rt != null && methodReturnType != null) {
                if (rt.getSimpleName().equals(methodReturnType.getSimpleName())) {
                    return method;
                }
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
    public Method getMatchingSetterMethod(Class classToQuery, Class typeToSet) {
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


    public Class getDuckClass(Class configBeanType) {
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

    public Method getGetDefaultValuesMethod(Class configBeanType) {
        Class duck = getDuckClass(configBeanType);
        if (duck == null) {
            return null;
        }
        Method m;
        try {
            m = duck.getMethod("getDefaultValues", String.class);
        } catch (Exception ex) {
            return null;
        }
        return m;
    }

    public boolean deleteConfigurationForConfigBean(ConfigBeanProxy configBean, Collection col, ConfigBeanDefaultValue defaultValue) {
        String name;
        ConfigBeanProxy itemToRemove;
        try {
            Class configBeanClass = getClassForFullName(defaultValue.getConfigBeanClassName());
            name = getNameForConfigBean(configBean, configBeanClass);
            itemToRemove = getNamedConfigBeanFromCollection(col, name, configBeanClass);
            if (itemToRemove != null) {
                col.remove(itemToRemove);
                return true;
            }
            if (name == null) {
                col.remove(configBean);
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    public Class getClassForFullName(String configBeanClassName) {
        ActiveDescriptor<?> descriptor = serviceLocator.getBestDescriptor(BuilderHelper.createContractFilter(configBeanClassName));
        if (descriptor != null) {
            if (!descriptor.isReified()) {
                descriptor = serviceLocator.reifyDescriptor(descriptor);
            }
            return getClassFromDescriptor(descriptor);
        } else {
            descriptor = serviceLocator.getBestDescriptor(BuilderHelper.createContractFilter(configBeanClassName + "Injector"));
            if (!descriptor.isReified()) {
                descriptor = serviceLocator.reifyDescriptor(descriptor);
            }
            ConfigInjector injector = (ConfigInjector) serviceLocator.getServiceHandle(descriptor).getService();
            return getClassFromInjector(injector);
        }
    }

    public Class getClassFromDescriptor(ActiveDescriptor<?> descriptor) {

        Class<?> defaultReturnValue = descriptor.getImplementationClass();

        String name = descriptor.getName();
        if (name == null) return defaultReturnValue;

        Class<?> foundContract = null;
        for (Type contract : descriptor.getContractTypes()) {
            if (!(contract instanceof Class)) continue;

            Class<?> cc = (Class<?>) contract;
            if (cc.getName().equals(name)) {
                foundContract = cc;
                break;
            }
        }

        if (foundContract == null) return defaultReturnValue;
        return foundContract;
    }

    public String replacePropertiesWithCurrentValue(String xmlConfiguration, ConfigBeanDefaultValue value) throws InvocationTargetException, IllegalAccessException {
        for (ConfigCustomizationToken token : value.getCustomizationTokens()) {
            String toReplace = "${" + token.getName() + "}";
            ConfigBeanProxy current = getCurrentConfigBeanForDefaultValue(value);
            String propertyValue = getPropertyValue(token, current);
            if (propertyValue != null) {
                xmlConfiguration = xmlConfiguration.replace(toReplace, propertyValue);
            }
        }
        return xmlConfiguration;
    }


    public String getPropertyValue(ConfigCustomizationToken token, ConfigBeanProxy finalConfigBean) {
        if (finalConfigBean != null) {
            ConfigBeanProxy parent = finalConfigBean.getParent();
            while (!(parent instanceof SystemPropertyBag)) {
                parent = parent.getParent();
                if (parent == null) return null;
            }
            if (((SystemPropertyBag) parent).getSystemProperty(token.getName()) != null) {
                return ((SystemPropertyBag) parent).getSystemProperty(token.getName()).getValue();
            }
            return null;
        } else return token.getValue();
    }


    public String getRuntimeTypePrefix(StartupContext startupContext) {
        Properties args = startupContext.getArguments();
        RuntimeType serverType = RuntimeType.getDefault();
        String typeString = args.getProperty("-type");
        if (typeString != null)
            serverType = RuntimeType.valueOf(typeString);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("server type is: " + serverType.name());
        }
        if (serverType.isEmbedded()) return "embedded";
        if (serverType.isSingleInstance() || serverType.isDas()) return "admin";
        if (serverType.isInstance()) return "instance";
        return "";
    }


    public List<Class> getAnnotatedConfigBeans(Class annotationType) {
        List<Class> prox = new ArrayList<Class>();
        List<ActiveDescriptor<?>> descriptor = serviceLocator.getDescriptors(BuilderHelper.createContractFilter(ConfigInjector.class.getName()));
        Class<?> clz = null;
        for (ActiveDescriptor desc : descriptor) {
            if (desc.getName() == null) {
                continue;
            }
            ConfigInjector injector = serviceLocator.getService(ConfigInjector.class, desc.getName());
            if (injector != null) {
                String clzName = injector.getClass().getName().substring(0, injector.getClass().getName().length() - 8);
                if (clzName == null) {
                    continue;
                }
                try {
                    clz = injector.getClass().getClassLoader().loadClass(clzName);
                    if (clz == null) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE, "Cannot find the class mapping to:  " + clzName);
                        }
                        continue;
                    }
                } catch (Throwable e) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Cannot load the class", e);
                    }
                    continue;
                }
            }
            if (clz != null) {
                if (clz.isAnnotationPresent(annotationType)) {
                    prox.add(clz);
                }
            }
        }
        return prox;
    }

    public boolean isIgnorePersisting() {
        return ignorePersisting;
    }

    public void setIgnorePersisting(boolean ignorePersisting) {
        this.ignorePersisting = ignorePersisting;
    }

    public boolean isCommandInvocation() {
        return isCommandInvocation;
    }

    public void setCommandInvocation(boolean commandInvocation) {
        isCommandInvocation = commandInvocation;
    }

    public List<Class> getInstalledExtensions(Class extensionType) {
        List<Class> extensions = new ArrayList();
        List<Class> cbeans = getAnnotatedConfigBeans(Configured.class);

        for (Class c : cbeans) {
            try {
                if (c.asSubclass(extensionType) != null && c != extensionType) {
                    extensions.add(c);
                }
            } catch (ClassCastException e) {
                continue;
            }
        }
        return extensions;
    }
}
