/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.DomDocument;

/**
 * @author Mitesh Meswani
 * @author Ludovic Champenois
 */
public abstract class ResourcesGeneratorBase implements ResourcesGenerator {

    private static Set<String> alreadyGenerated = new HashSet<String>();
    ServiceLocator habitat;

    public ResourcesGeneratorBase(ServiceLocator habitat) {
        this.habitat = habitat;
    }

    /**
     * Generate REST resource for a single config model.
     */
    @Override
    public void generateSingle(ConfigModel model, DomDocument domDocument) {
        configModelVisited(model);
        //processRedirectsAnnotation(model); // TODO need to extract info from RestRedirect Annotations

        String serverConfigName = ResourceUtil.getUnqualifiedTypeName(model.targetTypeName);
        String beanName = getBeanName(serverConfigName);
        String className = getClassName(beanName);

        if (alreadyGenerated(className)) {
            return;
        }

        String baseClassName = "TemplateRestResource";
        String resourcePath = null;

        if (beanName.equals("Domain")) {
            baseClassName = "org.glassfish.admin.rest.resources.GlassFishDomainResource";
            resourcePath = "domain";
        }

        ClassWriter classWriter = getClassWriter(className, baseClassName, resourcePath);

        if (classWriter != null) {
            generateCommandResources(beanName, classWriter);

            generateGetDeleteCommandMethod(beanName, classWriter);

            generateCustomResourceMapping(beanName, classWriter);

            for (String elementName : model.getElementNames()) {
                ConfigModel.Property childElement = model.getElement(elementName);
                if (elementName.equals("*")) {
                    ConfigModel.Node node = (ConfigModel.Node) childElement;
                    ConfigModel childModel = node.getModel();
                    List<ConfigModel> subChildConfigModels = ResourceUtil.getRealChildConfigModels(childModel, domDocument);
                    for (ConfigModel subChildConfigModel : subChildConfigModels) {
                        if (ResourceUtil.isOnlyATag(childModel) || ResourceUtil.isOnlyATag(subChildConfigModel) || subChildConfigModel.getAttributeNames().isEmpty() || hasSingletonAnnotation(subChildConfigModel)) {
                            String childResourceClassName = getClassName(ResourceUtil.getUnqualifiedTypeName(subChildConfigModel.targetTypeName));
                            String childPath = subChildConfigModel.getTagName();
                            classWriter.createGetChildResource(childPath, childResourceClassName);
                            generateSingle(subChildConfigModel, domDocument);
                        } else {
                            processNonLeafChildConfigModel(subChildConfigModel, childElement, domDocument, classWriter);

                        }
                    }
                } else if (childElement.isLeaf()) {
                    if (childElement.isCollection()) {
                        //handle the CollectionLeaf config objects.
                        //JVM Options is an example of CollectionLeaf object.
                        String childResourceBeanName = getBeanName(elementName);
                        String childResourceClassName = getClassName(childResourceBeanName);
                        classWriter.createGetChildResource(elementName, childResourceClassName);

                        //create resource class
                        generateCollectionLeafResource(childResourceBeanName);
                    } else {
                        String childResourceBeanName = getBeanName(elementName);
                        String childResourceClassName = getClassName(childResourceBeanName);
                        classWriter.createGetChildResource(elementName, childResourceClassName);

                        //create resource class
                        generateLeafResource(childResourceBeanName);
                    }
                } else {  // => !childElement.isLeaf()
                    processNonLeafChildElement(elementName, childElement, domDocument, classWriter);
                }
            }

            classWriter.done();
        }
    }

    public void generateList(ConfigModel model, DomDocument domDocument) {
        configModelVisited(model);

        String serverConfigName = ResourceUtil.getUnqualifiedTypeName(model.targetTypeName);
        String beanName = getBeanName(serverConfigName);
        String className = "List" + getClassName(beanName);

        if (alreadyGenerated(className)) {
            return;
        }

        ClassWriter classWriter = getClassWriter(className, "TemplateListOfResource", null);

        if (classWriter != null) {
            String keyAttributeName = getKeyAttributeName(model);
            String childResourceClassName = getClassName(beanName);
            classWriter.createGetChildResourceForListResources(keyAttributeName, childResourceClassName);
            generateCommandResources("List" + beanName, classWriter);

            generateGetPostCommandMethod("List" + beanName, classWriter);

            classWriter.done();

            generateSingle(model, domDocument);
        }
    }

    /*
     * empty method to be overwritten to get a callback when a model is visited.
     */
    public void configModelVisited(ConfigModel model) {
    }

    private void generateCollectionLeafResource(String beanName) {
        String className = getClassName(beanName);

        if (alreadyGenerated(className)) {
            return;
        }

        ClassWriter classWriter = getClassWriter(className, "CollectionLeafResource", null);

        if (classWriter != null) {
            CollectionLeafMetaData metaData = configBeanToCollectionLeafMetaData.get(beanName);

            if (metaData != null) {
                if (metaData.postCommandName != null) {
                    if (ResourceUtil.commandIsPresent(habitat, metaData.postCommandName)) {//and the command exits
                        classWriter.createGetPostCommandForCollectionLeafResource(metaData.postCommandName);
                    }
                }

                if (metaData.deleteCommandName != null) {
                    if (ResourceUtil.commandIsPresent(habitat, metaData.deleteCommandName)) {//and the command exits
                        classWriter.createGetDeleteCommandForCollectionLeafResource(metaData.deleteCommandName);
                    }
                }

                //display name method
                classWriter.createGetDisplayNameForCollectionLeafResource(metaData.displayName);
            }

            classWriter.done();
        }
    }

    private void generateLeafResource(String beanName) {
        String className = getClassName(beanName);

        if (alreadyGenerated(className)) {
            return;
        }

        ClassWriter classWriter = getClassWriter(className, "LeafResource", null);

        if (classWriter != null) {
            classWriter.done();
        }
    }

    private void processNonLeafChildElement(String elementName, ConfigModel.Property childElement, DomDocument domDocument, ClassWriter classWriter) {
        ConfigModel.Node node = (ConfigModel.Node) childElement;
        ConfigModel childModel = node.getModel();
        String beanName = ResourceUtil.getUnqualifiedTypeName(childModel.targetTypeName);

        if (beanName.equals("Property")) {
            classWriter.createGetChildResource("property", "PropertiesBagResource");
        } else {
            String childResourceClassName = getClassName(beanName);
            if (childElement.isCollection()) {
                childResourceClassName = "List" + childResourceClassName;
            }
            classWriter.createGetChildResource(/*
                     * childModel.getTagName()
                     */elementName, childResourceClassName);
        }

        if (childElement.isCollection()) {
            generateList(childModel, domDocument);
        } else {
            generateSingle(childModel, domDocument);
        }
    }

    /**
     * process given childConfigModel.
     *
     * @param childConfigModel
     * @param childElement
     * @param domDocument
     * @param classWriter
     */
    private void processNonLeafChildConfigModel(ConfigModel childConfigModel, ConfigModel.Property childElement, DomDocument domDocument, ClassWriter classWriter) {
        String childResourceClassName = getClassName("List" + ResourceUtil.getUnqualifiedTypeName(childConfigModel.targetTypeName));
        String childPath = childConfigModel.getTagName();
        classWriter.createGetChildResource(childPath, childResourceClassName);
        if (childElement.isCollection()) {
            generateList(childConfigModel, domDocument);
        } else {
            //The code flow should never reach here. NonLeaf ChildElements are assumed to be collection typed that is why we generate childResource as
            //generateSingle(childConfigModel, domDocument);
        }
    }

    private void generateGetDeleteCommandMethod(String beanName, ClassWriter classWriter) {
        String commandName = configBeanToDELETECommand.get(beanName);
        if (commandName != null) {
            if (ResourceUtil.commandIsPresent(habitat, commandName)) {//and the command exits
                classWriter.createGetDeleteCommand(commandName);
            }
        }
    }

    private void generateCustomResourceMapping(String beanName, ClassWriter classWriter) {
        for (CommandResourceMetaData cmd : CommandResourceMetaData.getCustomResourceMapping(beanName)) {
            classWriter.createCustomResourceMapping(cmd.customClassName, cmd.resourcePath);
        }
    }

    void generateGetPostCommandMethod(String resourceName, ClassWriter classWriter) {
        String commandName = configBeanToPOSTCommand.get(resourceName);
        if (commandName != null) {
            if (ResourceUtil.commandIsPresent(habitat, commandName)) {//and the command exits
                classWriter.createGetPostCommand(commandName);
            }
        }
    }

    /**
     * Generate resources for commands mapped under given parentBeanName
     *
     * @param parentBeanName
     * @param parentWriter
     */
    private void generateCommandResources(String parentBeanName, ClassWriter parentWriter) {
        List<CommandResourceMetaData> commandMetaData = CommandResourceMetaData.getMetaData(parentBeanName);
        if (commandMetaData.size() > 0) {
            for (CommandResourceMetaData metaData : commandMetaData) {
                if (ResourceUtil.commandIsPresent(habitat, metaData.command)) { //only if the command really exists
                    String commandResourceName = parentBeanName + getBeanName(metaData.resourcePath);
                    String commandResourceClassName = getClassName(commandResourceName);

                    //Generate command resource class
                    generateCommandResourceClass(parentBeanName, metaData);

                    //Generate getCommandResource() method in parent
                    parentWriter.createGetCommandResource(commandResourceClassName, metaData.resourcePath);
                }

            }
            //Generate GetCommandResourcePaths() method in parent
            parentWriter.createGetCommandResourcePaths(commandMetaData);
        }
    }

    /**
     * Generate code for Resource class corresponding to given parentBeanName and command
     *
     * @param parentBeanName
     * @param metaData
     */
    private void generateCommandResourceClass(String parentBeanName, CommandResourceMetaData metaData) {

        String commandResourceClassName = getClassName(parentBeanName + getBeanName(metaData.resourcePath));

        if (alreadyGenerated(commandResourceClassName)) {
            return;
        }

        String commandName = metaData.command;
        String commandDisplayName = metaData.resourcePath;
        String httpMethod = metaData.httpMethod;
        String commandAction = metaData.displayName;
        String baseClassName;

        if ("GET".equals(httpMethod)) {
            baseClassName = "org.glassfish.admin.rest.resources.TemplateCommandGetResource";
        } else if ("DELETE".equals(httpMethod)) {
            baseClassName = "org.glassfish.admin.rest.resources.TemplateCommandDeleteResource";
        } else if ("POST".equals(httpMethod)) {
            baseClassName = "org.glassfish.admin.rest.resources.TemplateCommandPostResource";
        } else {
            throw new GeneratorException("Invalid httpMethod specified: " + httpMethod);
        }

        ClassWriter classWriter = getClassWriter(commandResourceClassName, baseClassName, null);

        if (classWriter != null) {
            boolean isLinkedToParent = false;
            if (metaData.commandParams != null) {
                for (CommandResourceMetaData.ParameterMetaData parameterMetaData : metaData.commandParams) {
                    if (Constants.VAR_PARENT.equals(parameterMetaData.value)) {
                        isLinkedToParent = true;
                    }
                }
            }

            classWriter.createCommandResourceConstructor(commandResourceClassName, commandName, httpMethod,
                    isLinkedToParent, metaData.commandParams, commandDisplayName, commandAction);
            classWriter.done();
        }
    }

    /**
     * @param className
     * @return true if the given className is already generated. false otherwise.
     */
    protected boolean alreadyGenerated(String className) {
        boolean retVal = true;
        if (!alreadyGenerated.contains(className)) {
            alreadyGenerated.add(className);
            retVal = false;
        }
        return retVal;
    }

    /**
     * @param beanName
     * @return generated class name for given beanName
     */
    private String getClassName(String beanName) {
        return beanName + "Resource";
    }

    /**
     * @param elementName
     * @return bean name for the given element name. The name is derived by uppercasing first letter of elementName, eliminating
     * hyphens from elementName and uppercasing letter followed by hyphen
     */
    public static String getBeanName(String elementName) {
        StringBuilder ret = new StringBuilder();
        boolean nextisUpper = true;
        for (int i = 0; i < elementName.length(); i++) {
            if (nextisUpper == true) {
                ret.append(elementName.substring(i, i + 1).toUpperCase(Locale.US));
                nextisUpper = false;
            } else {
                if (elementName.charAt(i) == '-') {
                    nextisUpper = true;
                } else  if (elementName.charAt(i) == '/') {
                    nextisUpper = true;
                } else {
                    nextisUpper = false;
                    ret.append(elementName.substring(i, i + 1));
                }
            }
        }
        return ret.toString();
    }

    /**
     * @param model
     * @return name of the key attribute for the given model.
     */
    private String getKeyAttributeName(ConfigModel model) {
        String keyAttributeName = null;
        if (model.key == null) {
            for (String s : model.getAttributeNames()) {//no key, by default use the name attr
                if (s.equals("name")) {
                    keyAttributeName = getBeanName(s);
                }
            }
            if (keyAttributeName == null)//nothing, so pick the first one
            {
                Set<String> attributeNames = model.getAttributeNames();
                if (!attributeNames.isEmpty()) {
                    keyAttributeName = getBeanName(attributeNames.iterator().next());
                } else {
                    //TODO carried forward from old generator. Should never reach here. But we do for ConfigExtension and WebModuleConfig
                    keyAttributeName = "ThisIsAModelBug:NoKeyAttr"; //no attr choice fo a key!!! Error!!!
                }

            }
        } else {
            final int keyLength = model.key.length();
            String key = model.key.substring(1,
                    model.key.endsWith(">") ? keyLength -1 : keyLength );
            keyAttributeName = getBeanName(key);
        }
        return keyAttributeName;
    }

    private boolean hasSingletonAnnotation(ConfigModel model) {

        Class<? extends ConfigBeanProxy> cbp = null;
        try {
            cbp = (Class<? extends ConfigBeanProxy>) model.classLoaderHolder.loadClass(model.targetTypeName);
            if (cbp != null) {
                org.glassfish.config.support.Singleton sing = cbp.getAnnotation(org.glassfish.config.support.Singleton.class);
                return (sing != null);
            }
        } catch (MultiException e) {
            e.printStackTrace();
        }
        return false;
    }
    //TODO - fetch command name from config bean(RestRedirect annotation).
    //RESTREdirect currently only support automatically these deletes:
    /*
     * delete-admin-object delete-audit-module delete-auth-realm delete-connector-connection-pool delete-connector-resource
     * delete-custom-resource delete-http-listener delete-iiop-listener delete-javamail-resource delete-jdbc-connection-pool
     * delete-jdbc-resource delete-jms-host delete-message-security-provider delete-profiler delete-resource-adapter-config
     * delete-resource-ref delete-system-property delete-virtual-server What is missing is: delete-jms-resource delete-jmsdest
     * delete-jndi-resource delete-lifecycle-module delete-message-security-provider delete-connector-security-map
     * delete-connector-work-security-map delete-node-config delete-node-ssh delete-file-user delete-password-alias
     * delete-http-health-checker delete-http-lb-ref delete-http-redirect delete-instance
     */
    private static final Map<String, String> configBeanToDELETECommand = Collections.unmodifiableMap(new HashMap<String, String>() {

        {
            put("AdminObjectResource", "delete-admin-object");
            put("AuditModule", "delete-audit-module");
            put("AuthRealm", "delete-auth-realm");
            put("ApplicationRef", "delete-application-ref");
            put("Cluster", "delete-cluster");
            put("ConnectorConnectionPool", "delete-connector-connection-pool");
            put("Config", "delete-config");
            put("ConnectorConnectionPool", "delete-connector-connection-pool");
            put("ConnectorResource", "delete-connector-resource");
            put("CustomResource", "delete-custom-resource");
            put("ExternalJndiResource", "delete-jndi-resource");
            put("HttpListener", "delete-http-listener");
            put("Http", "delete-http");
            put("IiopListener", "delete-iiop-listener");
            put("JdbcResource", "delete-jdbc-resource");
            put("JaccProvider", "delete-jacc-provider");
//            put("JmsHost", "delete-jms-host");
            put("LbConfig", "delete-http-lb-config");
            put("LoadBalancer", "delete-http-lb");
            put("NetworkListener", "delete-network-listener");
            put("Profiler", "delete-profiler");
            put("Protocol", "delete-protocol");
            put("ProtocolFilter", "delete-protocol-filter");
            put("ProtocolFinder", "delete-protocol-finder");
            put("ProviderConfig", "delete-message-security-provider");
            put("ResourceAdapterConfig", "delete-resource-adapter-config");
            put("SecurityMap", "delete-connector-security-map");
            put("Ssl", "delete-ssl");
            put("Transport", "delete-transport");
            put("ThreadPool", "delete-threadpool");
            put("VirtualServer", "delete-virtual-server");
            put("WorkSecurityMap", "delete-connector-work-security-map");
        }
    }) ;
    //TODO - fetch command name from config bean(RestRedirect annotation).
    public static final Map<String, String> configBeanToPOSTCommand =Collections.unmodifiableMap( new HashMap<String, String>() {

        {
            put("Application", "redeploy"); //TODO check : This row is not used
            put("JavaConfig", "create-profiler"); // TODO check: This row is not used
            put("ListAdminObjectResource", "create-admin-object");
            put("ListApplication", "deploy");
            put("ListApplicationRef", "create-application-ref");
            put("ListAuditModule", "create-audit-module");
            put("ListAuthRealm", "create-auth-realm");
            put("ListCluster", "create-cluster");
            put("ListConfig", "_create-config");
            put("ListConnectorConnectionPool", "create-connector-connection-pool");
            put("ListConnectorResource", "create-connector-resource");
            put("ListCustomResource", "create-custom-resource");
            put("ListExternalJndiResource", "create-jndi-resource");
            put("ListHttpListener", "create-http-listener");
            put("ListIiopListener", "create-iiop-listener");
            put("ListJaccProvider", "create-jacc-provider");
            put("ListJdbcConnectionPool", "create-jdbc-connection-pool");
            put("ListJdbcResource", "create-jdbc-resource");
            put("ListJmsHost", "create-jms-host");
            put("ListLbConfig", "create-http-lb-config");
            put("ListLoadBalancer", "create-http-lb");
            put("ListMailResource", "create-javamail-resource");
            put("ListMessageSecurityConfig", "create-message-security-provider");
            put("ListNetworkListener", "create-network-listener");
            put("ListProtocol", "create-protocol");
            put("ListResourceAdapterConfig", "create-resource-adapter-config");
            put("ListResourceRef", "create-resource-ref");
            put("ListSystemProperty", "create-system-properties");
            put("ListThreadPool", "create-threadpool");
            put("ListTransport", "create-transport");
            put("ListVirtualServer", "create-virtual-server");
            put("ListWorkSecurityMap", "create-connector-work-security-map");
            put("ProtocolFilter", "create-protocol-filter");
            put("ProtocolFinder", "create-protocol-finder");
            put("ListSecurityMap", "create-connector-security-map");
        }
    });
    //This map is used to generate CollectionLeaf resources.
    //Example: JVM Options. This information will eventually move to config bean-
    //JavaConfig or JvmOptionBag
    public static final Map<String, CollectionLeafMetaData> configBeanToCollectionLeafMetaData =
            new HashMap<String, CollectionLeafMetaData>() {

                {
                    put("JvmOptions", new CollectionLeafMetaData("create-jvm-options", "delete-jvm-options", "JvmOption"));
                    //          put("Principal", new CollectionLeafMetaData("__create-principal", "__delete-principal", "Principal"));
                    //          put("UserGroup", new CollectionLeafMetaData("__create-user-group", "__delete-user-group", "User Group"));
                }
            };
}
