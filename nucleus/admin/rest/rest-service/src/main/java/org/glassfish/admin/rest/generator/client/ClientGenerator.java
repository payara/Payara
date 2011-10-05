/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.admin.rest.ResourceUtil;
import org.glassfish.api.admin.config.ApplicationName;
import org.glassfish.api.admin.config.Named;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.Util;
import org.glassfish.admin.rest.client.RestClientBase;
import org.glassfish.admin.rest.client.RestLeaf;
import org.glassfish.admin.rest.client.RestLeafCollection;
import org.glassfish.admin.rest.generator.CommandResourceMetaData;
import org.glassfish.admin.rest.generator.ResourcesGeneratorBase;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.api.admin.CommandRunner;

/**
 *
 * @author jasonlee
 */
public abstract class ClientGenerator {
    private static final String OUTPUT_PATH = "target/generated";
    private static final String CLIENT_PKG = "org.glassfish.admin.rest.client";
    private static final String BASE_CLASS = "org.glassfish.admin.rest.client.RestClientBase";

    protected Set<String> alreadyGenerated = new HashSet<String>();
    protected Habitat habitat;
    protected Version version;
    
    private DomDocument document;

    public ClientGenerator(Habitat habitat) {
        this.habitat = habitat;
        version = habitat.getByType(Version.class);
    }
    
    public void generateClasses() {
        Domain entity = getHabitat().getComponent(Domain.class);
        Dom dom = Dom.unwrap(entity);
        document = dom.document;
        ConfigModel rootModel = dom.document.getRoot().model;
        alreadyGenerated.clear();

        generateSingle(rootModel);
    }
    
    public abstract Map<String, URI> getArtifact();

    public abstract ClientClassWriter getClassWriter(ConfigModel model, String className, Class parent);

    public Habitat getHabitat() {
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

        generateGetPostCommandMethod(writer, beanName);

        generateSingle(model);
    }

    void generateGetPostCommandMethod(ClientClassWriter writer, String resourceName) {
        String commandName = ResourcesGeneratorBase.configBeanToPOSTCommand.get("List"+resourceName);
        if (commandName != null) {
            final CommandModel cm = getCommandModel(commandName);
            if (cm != null ) {//and the command exits
                writer.generateCommandMethod(Util.methodNameFromDtdName(commandName, null), "POST", ResourceUtil.convertToXMLName(resourceName), cm);
            }
        }
    }

    //    private void generateCommandMethods(String parentBeanName, ClassWriter parentWriter) {
    private void generateCommandMethods(ClientClassWriter writer, String className) {
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

    private CommandModel getCommandModel(String commandName) {
        CommandRunner cr = getHabitat().getComponent(CommandRunner.class);
        return cr.getModel(commandName, RestService.logger);
    }

    protected Set<String> processElements(ClientClassWriter writer, ConfigModel model) {
        Set<String> processed = new HashSet<String>();
        for (String elementName : model.getElementNames()) {
            if (processed.contains(elementName)) {
                continue;
            }
            processed.add(elementName);

            ConfigModel.Property childElement = model.getElement(elementName);
            
            System.out.println("Processing element " + elementName + 
                    " child element = " + childElement.xmlName);

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

    private void generateCollectionLeafResource(ClientClassWriter writer, String xmlName) {
        String className = Util.getBeanName(xmlName);
        writer.generateCollectionLeafResourceGetter(className);
        ClientClassWriter childClass = getClassWriter(null, className, RestLeafCollection.class);
        childClass.generateGetSegment(xmlName);
        childClass.done();
    }

    private void generateLeafResource(ClientClassWriter writer, String xmlName) {
        String className = Util.getBeanName(xmlName);

        writer.generateRestLeafGetter(className);
        ClientClassWriter childClass = getClassWriter(null, className, RestLeaf.class);
        childClass.generateGetSegment(xmlName);
        childClass.done();
    }

    private void processNonLeafChildConfigModel(ClientClassWriter writer, ConfigModel childConfigModel, ConfigModel.Property childElement) {
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
        
        System.out.println("Process non-lead child element. childModel = " +
                childModel.getTagName() + ", beanName = " + beanName);

        writer.createGetChildResource(childModel, Util.upperCaseFirstLetter(Util.eleminateHypen(elementName)), beanName);

        if (childElement.isCollection()) {
            generateList(writer, childModel);
        } else {
            generateSingle(childModel);
        }
    }

    /**
    protected void createDuckTypedMethod(StringBuilder sb, Method m) {
        String returnType = m.getReturnType().getSimpleName();

        try {
            if (implementsInterface(m.getReturnType(), Named.class) || implementsInterface(m.getReturnType(), ApplicationName.class)) {
                // ugly hack?
                if (m.getParameterTypes().length > 1) {
                    return;
                }

                List<ConfigModel> list = document.getAllModelsImplementing(m.getReturnType());

                if (list != null) {
                    for (ConfigModel el : list) {
                        generateSingle(el);
                    }
                }

                sb.append("    public ")
                        .append(returnType)
                        .append(" ")
                        .append(m.getName())
                        .append("(");
                int count = 0;
                for (Class<?> c : m.getParameterTypes()) {
                    sb.append(c.getName())
                            .append(" in")
                            .append(++count);
                }

                sb.append(") {\n")
                        .append("        return new ")
                        .append(returnType)
                        .append("(client, this, in1);\n    };\n");
            }
        } catch (Exception e) {

        }
    }
    
    protected Boolean implementsInterface(Class<?> clazz, Class interf) {
        for (Class c : clazz.getInterfaces()) {
            if (c.equals(interf)) {
                return true;
            }
        }
        return false;
    }
    */
}