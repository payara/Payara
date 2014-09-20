/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.generator.client;

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.client.RestClientBase;
import org.glassfish.admin.rest.client.RestLeaf;
import org.glassfish.admin.rest.client.RestLeafCollection;
import org.glassfish.admin.rest.generator.CommandResourceMetaData;
import org.glassfish.admin.rest.generator.ResourcesGeneratorBase;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.glassfish.admin.rest.RestLogging;

/**
 *
 * @author jasonlee
 */
public abstract class ClientGenerator {
    private static final String OUTPUT_PATH = "target/generated";
    private static final String CLIENT_PKG = "org.glassfish.admin.rest.client";
    private static final String BASE_CLASS = "org.glassfish.admin.rest.client.RestClientBase";

    protected Set<String> alreadyGenerated = new HashSet<String>();
    protected ServiceLocator habitat;
    protected List<String> messages = new ArrayList<String>();
    protected String versionString;
    protected static final String ARTIFACT_NAME = "rest-client-wrapper";

    private DomDocument document;

    public ClientGenerator(ServiceLocator habitat) {
        this.habitat = habitat;
        versionString = Version.getVersionNumber();
    }

    public abstract ClientClassWriter getClassWriter(ConfigModel model, String className, Class parent);
    public abstract Map<String, URI> getArtifact();

    public List<String> getMessages() {
        return messages;
    }

    public void generateClasses() {
        Domain entity = getBaseServiceLocator().getService(Domain.class);
        Dom dom = Dom.unwrap(entity);
        document = dom.document;
        ConfigModel rootModel = dom.document.getRoot().model;
        alreadyGenerated.clear();

        generateSingle(rootModel);
    }

    public ServiceLocator getBaseServiceLocator() {
        return habitat;
    }

    public void generateSingle (ConfigModel model)  {
        String className = Util.getBeanName(model.getTagName());

        if (alreadyGenerated(className)) {
            return;
        }

        ClientClassWriter writer = getClassWriter(model, className, RestClientBase.class);

        writer.generateGetSegment(model.getTagName());

        generateCommandMethods(writer, className);
        Set<String> processed = processElements(writer, model);
        processAttributes(writer, model, processed);

        writer.done();
    }

    public void generateList (ClientClassWriter writer, ConfigModel model) {
        String serverConfigName = ResourceUtil.getUnqualifiedTypeName(model.targetTypeName);
        String beanName = Util.getBeanName(serverConfigName);

        generateCommandMethods(writer, "List" + beanName);

//        generateGetPostCommandMethod(writer, beanName);

        generateSingle(model);
    }

    protected void generateGetPostCommandMethod(ClientClassWriter writer, String resourceName) {
        String commandName = ResourcesGeneratorBase.configBeanToPOSTCommand.get("List"+resourceName);
        if (commandName != null) {
            final CommandModel cm = getCommandModel(commandName);
            if (cm != null ) {//and the command exits
                writer.generateCommandMethod(Util.methodNameFromDtdName(commandName, null), "POST", ResourceUtil.convertToXMLName(resourceName), cm);
            }
        }
    }

    //    private void generateCommandMethods(String parentBeanName, ClassWriter parentWriter) {
    protected void generateCommandMethods(ClientClassWriter writer, String className) {
        List<CommandResourceMetaData> commandMetaData = CommandResourceMetaData.getMetaData(className);
        if (commandMetaData.size() > 0) {
            for (CommandResourceMetaData metaData : commandMetaData) {
                CommandModel cm = getCommandModel(metaData.command);
                if (cm != null) {
                    String methodName = Util.methodNameFromDtdName(metaData.command, null);
                    if (!methodName.startsWith("_")) {
                        writer.generateCommandMethod(methodName, metaData.httpMethod, metaData.resourcePath, cm);
                    }
                }

            }
        }
    }

    protected void processAttributes(ClientClassWriter writer, ConfigModel model, Set<String> processed) {
        Class clazz = model.getProxyType();
        for (Method method : clazz.getMethods()) {
            String methodName = method.getName();
            Attribute a = method.getAnnotation(Attribute.class);
            Param p = method.getAnnotation(Param.class);
            if ((a != null) || (p != null)) {
                String type = "String";
                if (a != null) {
                    type = a.dataType().getName();
                }
                if (methodName.startsWith("get") || methodName.startsWith("set")) {
                    methodName = methodName.substring(3);
                }
                String fieldName = Util.lowerCaseFirstLetter(methodName);
                if (processed.contains(fieldName)) {
                    continue;
                }
                processed.add(fieldName);

                writer.generateGettersAndSetters(type, methodName, fieldName);
            }
        }
    }

    protected String generateParameterName(ParamModel model) {
        Param param = model.getParam();
        final String paramName = (!param.alias().isEmpty()) ? param.alias() : model.getName();

        return  paramName;
    }

    protected CommandModel getCommandModel(String commandName) {
        CommandRunner cr = getBaseServiceLocator().getService(CommandRunner.class);
        return cr.getModel(commandName, RestLogging.restLogger);
    }

    protected Set<String> processElements(ClientClassWriter writer, ConfigModel model) {
        Set<String> processed = new HashSet<String>();
        for (String elementName : model.getElementNames()) {
            if (processed.contains(elementName)) {
                continue;
            }
            processed.add(elementName);

            ConfigModel.Property childElement = model.getElement(elementName);

            if (elementName.equals("*")) {
                ConfigModel.Node node = (ConfigModel.Node) childElement;
                ConfigModel childModel = node.getModel();
                List<ConfigModel> subChildConfigModels = ResourceUtil.getRealChildConfigModels(childModel, document);
                for (ConfigModel subChildConfigModel : subChildConfigModels) {
                    if (ResourceUtil.isOnlyATag(childModel)) {
                        String childResourceClassName = ResourceUtil.getUnqualifiedTypeName(subChildConfigModel.targetTypeName);
                        writer.createGetChildResource(subChildConfigModel, childResourceClassName, childResourceClassName);
                        generateSingle(subChildConfigModel);
                    } else {
                        processNonLeafChildConfigModel(writer, subChildConfigModel, childElement);
                    }
                }
            } else  if (childElement.isLeaf()) {
                if (processed.contains(childElement.xmlName)) {
                    continue;
                }
                processed.add(childElement.xmlName);

                if (childElement.isCollection()) {
                    //generateCollectionLeafResource
                    System.out.println("generateCollectionLeafResource for " + elementName + " off of " + model.getTagName());
                    generateCollectionLeafResource(writer, childElement.xmlName);
                } else {
                    System.out.println("generateLeafResource for " + elementName + " off of " + model.getTagName());
//                    generateSingle(document.getModelByElementName(elementName));
                    generateLeafResource(writer, childElement.xmlName);
                }
            } else {
                processNonLeafChildElement(writer, elementName, childElement);
            }
        }

        return processed;
    }

    protected void generateCollectionLeafResource(ClientClassWriter writer, String xmlName) {
        String className = Util.getBeanName(xmlName);
        writer.generateCollectionLeafResourceGetter(className);
        ClientClassWriter childClass = getClassWriter(null, className, RestLeafCollection.class);
        childClass.generateGetSegment(xmlName);
        childClass.done();
    }

    protected void generateLeafResource(ClientClassWriter writer, String xmlName) {
        String className = Util.getBeanName(xmlName);

        writer.generateRestLeafGetter(className);
        ClientClassWriter childClass = getClassWriter(null, className, RestLeaf.class);
        childClass.generateGetSegment(xmlName);
        childClass.done();
    }

    protected void processNonLeafChildConfigModel(ClientClassWriter writer, ConfigModel childConfigModel, ConfigModel.Property childElement) {
        String childResourceClassName = ResourceUtil.getUnqualifiedTypeName(childConfigModel.targetTypeName);
        writer.createGetChildResource(childConfigModel, childResourceClassName, childResourceClassName);
        if (childElement.isCollection()) {
            generateList(writer, childConfigModel);
        } else {
            throw new RuntimeException("The code flow should never reach here. Non-leaf ChildElements are assumed to be collection typed.");
        }
    }

    protected boolean alreadyGenerated(String className) {
        boolean retVal = true;
        if (!alreadyGenerated.contains(className)) {
            alreadyGenerated.add(className);
            retVal = false;
        }
        return retVal;
    }

    private void processNonLeafChildElement(ClientClassWriter writer, String elementName, ConfigModel.Property childElement) {
        ConfigModel.Node node = (ConfigModel.Node) childElement;
        ConfigModel childModel = node.getModel();
        String beanName = ResourceUtil.getUnqualifiedTypeName(childModel.targetTypeName);

        writer.createGetChildResource(childModel, Util.upperCaseFirstLetter(Util.eleminateHypen(elementName)), beanName);

        if (childElement.isCollection()) {
            generateList(writer, childModel);
        } else {
            generateSingle(childModel);
        }
    }
}
