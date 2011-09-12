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

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.admin.rest.ResourceUtil;
import org.glassfish.api.admin.config.ApplicationName;
import org.glassfish.api.admin.config.Named;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.*;

import javax.ws.rs.Path;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.Util;
import org.glassfish.admin.rest.generator.CommandResourceMetaData;
import org.glassfish.admin.rest.generator.ResourcesGeneratorBase;
import org.glassfish.admin.rest.resources.GeneratorResource;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.api.admin.CommandRunner;

/**
 *
 * @author jasonlee
 */
@Path("/client/")
public class ClientGeneratorBaseResource {
    private static final String OUTPUT_PATH = "target/generated";
    private static final String CLIENT_PKG = "org.glassfish.admin.rest.client";
    private static final String BASE_CLASS = "org.glassfish.admin.rest.client.RestClientBase";
    private static final String DEFAULT_OUTPUT_DIR = System.getProperty("user.home") +
        "/src/glassfish/3.2/admin/rest/src/generated/java/org/glassfish/admin/rest/client/";

    private Set<String> alreadyGenerated = new HashSet<String>();
    private File baseDirectory;
    private DomDocument document;

    @Context
    private Habitat habitat;

    public ClientGeneratorBaseResource() {
    }

    @GET
    public String get(@QueryParam("outputDir")String outputDir) {
        String retVal = "Code Generation done at : " + outputDir + "\n";
        if(outputDir == null) {
            outputDir = DEFAULT_OUTPUT_DIR;
        }
        baseDirectory = new File(outputDir);
        boolean success = baseDirectory.mkdirs();
        if (success) {
            try {
                generateClasses();
            } catch (Exception ex) {
                Logger.getLogger(GeneratorResource.class.getName()).log(Level.SEVERE, null, ex);
                retVal = "Exception encountered during generation process: " + ex.toString() + "\nPlease look at server.log for more information."; //i18n
            }
        } else {
            retVal = "Unable to create outout directory"; // i18n
        }
        return retVal;
    }

    public void generateClasses()  {
        Domain entity = habitat.getComponent(Domain.class);
        Dom dom = Dom.unwrap(entity);
        document = dom.document;
        ConfigModel rootModel = dom.document.getRoot().model;
        alreadyGenerated.clear();

        generateSingle(rootModel);
    }

    protected void generateGetSegment(StringBuilder sb, String tagName) {
        // getSegment()
        sb.append("\n")
            .append("    @Override protected String getSegment() {\n")
            .append("        return \"/")
            .append(tagName)
            .append("\";\n    }\n");
    }

    protected void writeClassFile(String className, StringBuilder source) {
        File classFile = new File(baseDirectory, className + ".java");
        BufferedWriter bw = null;
                
        try {
            bw = new BufferedWriter(new FileWriter(classFile));
            bw.write(source.toString());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ex) {
                    Logger.getLogger(ClientGeneratorBaseResource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    protected void generateSingle (ConfigModel model)  {
        String className = getBeanName(model.getTagName());

        if (alreadyGenerated(className)) {
            return;
        }

        StringBuilder source = startClass (model, className);

        generateCommandMethods(source, className);
        Set<String> processed = processElements(source, model);
        processAttributes(source, model, processed);
        finishClass (source);

        writeClassFile(className, source);
    }

    protected void generateList (StringBuilder sb, ConfigModel model) {
        String serverConfigName = ResourceUtil.getUnqualifiedTypeName(model.targetTypeName);
        String beanName = getBeanName(serverConfigName);

        generateCommandMethods(sb, "List" + beanName);

        generateGetPostCommandMethod(sb, beanName);

        generateSingle(model);
    }

    protected void processAttributes(StringBuilder sb, ConfigModel model, Set<String> processed) {
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
                // getter
                sb.append("    public ")
                        .append(type)
                        .append(" get")
                        .append(methodName)
                        .append("() {\n        return getValue(\"")
                        .append(fieldName)
                        .append("\",")
                        .append(type)
                        .append(".class);\n    }\n\n");

                // setter
                sb.append("    public void set")
                        .append(methodName)
                        .append("(")
                        .append(type)
                        .append(" value) {\n        setValue(\"")
                        .append(fieldName)
                        .append("\", value);\n    }\n\n");
            }
        }
    }

    void generateGetPostCommandMethod(StringBuilder sb, String resourceName) {
        String commandName = ResourcesGeneratorBase.configBeanToPOSTCommand.get("List"+resourceName);
        if (commandName != null) {
            final CommandModel cm = getCommandModel(commandName);
            if (cm != null ) {//and the command exits
                generateCommandMethod(sb, Util.methodNameFromDtdName(commandName, null), "POST", ResourceUtil.convertToXMLName(resourceName), cm);
            }
        }
    }

    //    private void generateCommandMethods(String parentBeanName, ClassWriter parentWriter) {
    private void generateCommandMethods(StringBuilder source, String className) {
        List<CommandResourceMetaData> commandMetaData = CommandResourceMetaData.getMetaData(className);
        if (commandMetaData.size() > 0) {
            for (CommandResourceMetaData metaData : commandMetaData) {
                CommandModel cm = getCommandModel(metaData.command);
                if (cm != null) {
                    String methodName = Util.methodNameFromDtdName(metaData.command, null);
                    if (!methodName.startsWith("_")) {
                        generateCommandMethod(source, methodName, metaData.httpMethod, metaData.resourcePath, cm);
                    }
                }

            }
        }
    }

    protected void generateCommandMethod(StringBuilder sb, String methodName, String httpMethod, String resourcePath, CommandModel cm) {
        String parameters = getMethodParameterList(cm, false);
        String withOptional = getMethodParameterList(cm, true);
        sb.append("\n    public RestResponse ")
                .append(methodName)
                .append("(")
                .append(parameters)
                .append(") {\n")
                .append(generateMethodBody(cm, httpMethod, resourcePath, false))
                .append("    }\n");
        if (withOptional.length() > parameters.length()) {
            sb.append("\n    public RestResponse ")
                    .append(methodName)
                    .append("(")
                    .append(withOptional)
                    .append(") {\n")
                    .append(generateMethodBody(cm, httpMethod, resourcePath, true))
                    .append("    }\n");

        }
    }

    protected String generateMethodBody(CommandModel cm, String httpMethod, String resourcePath, boolean includeOptional) {
        StringBuilder sb = new StringBuilder();
        sb.append("        Map<String, Object> payload = new HashMap<String, Object>();\n");
        Map<String, String> paramNames = new HashMap<String, String>();
        Collection<ParamModel> params = cm.getParameters();
        if ((params != null) && (!params.isEmpty())) {
            for (ParamModel model : params) {
                Param param = model.getParam();
                boolean include = true;
                if (param.optional() && !includeOptional) {
                        include = false;
                }
                if (!include) {
                    continue;
                }
                paramNames.put((!param.alias().isEmpty()) ? param.alias() : model.getName(), model.getName());
            }
        }
        for (Map.Entry<String, String> paramName : paramNames.entrySet()) {
            sb.append("        payload.put(\"")
                    .append(paramName.getKey())
                    .append("\", _")
                    .append(Util.eleminateHypen(paramName.getValue()))
                    .append(");\n");
        }
        sb.append("        return execute(Method.")
                    .append(httpMethod.toUpperCase(Locale.US))
                    .append(", \"/")
                    .append(resourcePath)
                    .append("\"")
                    .append(", payload);\n");


        return sb.toString();
    }

    protected String getMethodParameterList(CommandModel cm, boolean includeOptional) {
        StringBuilder sb = new StringBuilder();
        Collection<ParamModel> params = cm.getParameters();
        if ((params != null) && (!params.isEmpty())) {
            String sep = "";
            for (ParamModel model : params) {
                Param param = model.getParam();
                boolean include = true;
                if (param.optional() && !includeOptional) {
                        include = false;
                }
                if (!include) {
                    continue;
                }

                String type = model.getType().getName();
                if (type.startsWith("java.lang")) {
                    type = model.getType().getSimpleName();
                }
                sb.append(sep)
                        .append(type)
                        .append(" _")
                        .append(Util.eleminateHypen(model.getName()));
                sep = ", ";
            }
        }

        return sb.toString();
    }

    protected String generateParameterName(ParamModel model) {
        Param param = model.getParam();
        final String paramName = (!param.alias().isEmpty()) ? param.alias() : model.getName();

        return  paramName;
    }

    private CommandModel getCommandModel(String commandName) {
        CommandRunner cr = habitat.getComponent(CommandRunner.class);
        return cr.getModel(commandName, RestService.logger);
    }

    protected StringBuilder startClass(ConfigModel model, String className) {
        StringBuilder sb = new StringBuilder();
        final String keyName = getKeyAttributeName(model);
        boolean hasKey = (keyName != null);
        boolean isDomain = className.equals("Domain");

        sb.append("package org.glassfish.admin.rest.client;\n")
                .append("import java.util.HashMap;\n")
                .append("import java.util.Map;\n")
                .append("import com.sun.jersey.api.client.Client;\n")
                .append("\npublic class ")
                .append(className)
                .append(" extends RestClientBase {\n");

        // ctor
        if (isDomain) {
            sb.append("    private RestClient parent;\n")

                .append("    public ").append(className)
                .append("(RestClient parent) {\n")
                .append("        super(parent.client, null);\n")
                .append("        this.parent = parent;\n    }\n");

        } else {
            if (hasKey) {
                sb.append("    private String name;\n")
                        .append("    protected ")
                        .append(className)
                        .append("(Client c, RestClientBase p, String name) {\n")
                        .append("        super(c, p);\n")
                        .append("        this.name = name;\n")
                        .append("    }\n");


            } else {
                sb.append("    protected ")
                        .append(className)
                        .append("(Client c, RestClientBase p) {\n")
                        .append("        super(c,p);\n")
                        .append("    }\n");

            }
        }

        // getRestUrl()
        if (hasKey || isDomain) {
            sb.append("    @Override protected String getRestUrl() {\n");
            if (hasKey) {
                sb.append("        return super.getRestUrl() + (isNew() ? \"\" :  \"/\" + name);\n");
            } else if (isDomain) {
                sb.append("        return parent.getRestUrl() + getSegment();\n");
            }
            sb.append("    }\n");
        }
        generateGetSegment(sb, model.getTagName());


        return sb;
    }

    protected Set<String> processElements(StringBuilder sb, ConfigModel model) {
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
                        this.createGetChildResource(sb, subChildConfigModel, childResourceClassName);
                        generateSingle(subChildConfigModel);
                    } else {
                        processNonLeafChildConfigModel(sb, subChildConfigModel, childElement);
                    }
                }
            } else  if (childElement.isLeaf()) {
                if (processed.contains(childElement.xmlName)) {
                    continue;
                }
                processed.add(childElement.xmlName);

                if (childElement.isCollection()) {
                    //generateCollectionLeafResource
                    generateCollectionLeafResource(sb, childElement.xmlName);
                    System.out.println("generateCollectionLeafResource for " + elementName + " off of " + model.getTagName());
                } else {
                    System.out.println("generateLeafResource for " + elementName + " off of " + model.getTagName());
//                    generateSingle(document.getModelByElementName(elementName));
                    generateLeafResource(sb, childElement.xmlName);
                }
            } else {
                processNonLeafChildElement(sb, elementName, childElement);
            }
        }

        return processed;
    }

    private void generateCollectionLeafResource(StringBuilder parent, String xmlName) {
        String className = getBeanName(xmlName);

        parent.append("    public ")
                .append(className)
                .append(" get")
                .append(className)
                .append("() {\n")
                .append("        return new ")
                .append(className)
                .append("(client, this);\n")
                .append("    }\n");

//        StringBuilder childClass = startClass(model, className);
        StringBuilder childClass = new StringBuilder();

        childClass.append("package org.glassfish.admin.rest.client;\n")
                .append("import com.sun.jersey.api.client.Client;\n")
                .append("\npublic class ")
                .append(className)
                .append(" extends RestLeafCollection {\n")
                .append("    protected ")
                .append(className)
                .append("(Client c, RestClientBase p) {\n")
                .append("        super(c,p);\n")
                .append("    }\n");

        generateGetSegment(childClass, xmlName);
        childClass.append("}\n");

        writeClassFile(className, childClass);
    }

    private void generateLeafResource(StringBuilder parent, String xmlName) {
        String className = getBeanName(xmlName);

        parent.append("    public ")
                .append(className)
                .append(" get")
                .append(className)
                .append("() {\n")
                .append("        return new ")
                .append(className)
                .append("(client, this);\n")
                .append("    }\n");

//        StringBuilder childClass = startClass(model, className);
        StringBuilder childClass = new StringBuilder();

        childClass.append("package org.glassfish.admin.rest.client;\n")
                .append("import com.sun.jersey.api.client.Client;\n")
                .append("\npublic class ")
                .append(className)
                .append(" extends RestLeaf {\n")
                .append("    protected ")
                .append(className)
                .append("(Client c, RestClientBase p) {\n")
                .append("        super(c,p);\n")
                .append("    }\n");

        generateGetSegment(childClass, xmlName);
        childClass.append("}\n");

        writeClassFile(className, childClass);
    }

    private void processNonLeafChildConfigModel(StringBuilder sb, ConfigModel childConfigModel, ConfigModel.Property childElement) {
        String childResourceClassName = ResourceUtil.getUnqualifiedTypeName(childConfigModel.targetTypeName);
        createGetChildResource(sb, childConfigModel, childResourceClassName);
        if (childElement.isCollection()) {
            generateList(sb, childConfigModel);
        } else {
            throw new RuntimeException("The code flow should never reach here. Non-leaf ChildElements are assumed to be collection typed.");
        }
    }

    protected void finishClass(StringBuilder sb) {
        sb.append("}");
    }

    private String getBeanName(String elementName) {
        StringBuilder ret = new StringBuilder();
        boolean nextisUpper = true;
        for (int i = 0; i < elementName.length(); i++) {
            if (nextisUpper == true) {
                ret.append(elementName.substring(i, i + 1).toUpperCase(Locale.US));
                nextisUpper = false;
            } else {
                if (elementName.charAt(i) == '-') {
                    nextisUpper = true;
                } else {
                    nextisUpper = false;
                    ret.append(elementName.substring(i, i + 1));
                }
            }
        }
        return ret.toString();
    }

    private boolean alreadyGenerated(String className) {
        boolean retVal = true;
        if (!alreadyGenerated.contains(className)) {
            alreadyGenerated.add(className);
            retVal = false;
        }
        return retVal;
    }

    private void processNonLeafChildElement(StringBuilder sb, String elementName, ConfigModel.Property childElement) {
        ConfigModel.Node node = (ConfigModel.Node) childElement;
        ConfigModel childModel = node.getModel();
        String beanName = ResourceUtil.getUnqualifiedTypeName(childModel.targetTypeName);

        createGetChildResource(sb, childModel, beanName);

        if (childElement.isCollection()) {
            generateList(sb, childModel);
        } else {
            generateSingle(childModel);
        }
    }

    protected void createGetChildResource(StringBuilder sb, ConfigModel model, String childResourceClassName) {
        final boolean hasKey = getKeyAttributeName(model) != null;
        sb.append("    public ")
                .append(childResourceClassName)
                .append(" get")
                .append(childResourceClassName)
                .append("(");
        if (hasKey) {
            sb.append("String name");
        }
        sb.append(") {\n")
                .append("        return new ")
                .append(childResourceClassName)
                .append("(client, this");
        if (hasKey) {
            sb.append(", name");
        }
        sb.append(");\n    }\n");
    }

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

    //**************************************************************************
    // Copied from ResourcesGeneratorBase
    // TODO: Refactor these methods from both classes to somewhere shared
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
            /*
            if (keyAttributeName == null)//nothing, so pick the first one
            {
                Set<String> attributeNames = model.getAttributeNames();
                if (!attributeNames.isEmpty()) {
                    keyAttributeName = getBeanName(attributeNames.iterator().next());
                } else {
                    //TODO carried forward from old generator. Should never reach here. But we do for ConfigExtension and WebModuleConfig
//                    keyAttributeName = "ThisIsAModelBug:NoKeyAttr"; //no attr choice fo a key!!! Error!!!
                }

            }
            */
        } else {
            keyAttributeName = getBeanName(model.key.substring(1, model.key.length()));
        }
        return keyAttributeName;
    }
}