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
package org.glassfish.admin.rest.resources;

import com.sun.enterprise.config.serverbeans.Domain;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.generator.ClassWriter;
import org.glassfish.admin.rest.generator.CommandResourceMetaData;
import org.glassfish.admin.rest.generator.CommandResourceMetaData.ParameterMetaData;


import org.glassfish.admin.rest.generator.ResourcesGenerator;
import org.glassfish.admin.rest.generator.ResourcesGeneratorBase;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

/**
 * @author Ludovic Champenois ludo@java.net
 */
@Path("/status/")
public class StatusGenerator extends AbstractResource {

    protected StringBuilder status = new StringBuilder();
    private Set<String> commandsUsed = new TreeSet<String>();
    private Set<String> allCommands = new TreeSet<String>();
    private Set<String> restRedirectCommands = new TreeSet<String>();
    private Map<String, String> commandsToResources = new TreeMap<String, String>();
    private Map<String, String> resourcesToDeleteCommands = new TreeMap<String, String>();
    private Properties propsI18N = new SortedProperties();

    static private class SortedProperties extends Properties {

        public Enumeration keys() {
            Enumeration keysEnum = super.keys();
            Vector<String> keyList = new Vector<String>();
            while (keysEnum.hasMoreElements()) {
                keyList.add((String) keysEnum.nextElement());
            }
            Collections.sort(keyList);
            return keyList.elements();
        }
    }

    @GET
    @Produces({"text/plain"})
    public String getPlain() {
//        status.append("\n------------------------");
//        status.append("Status of Command usage\n");
        try {
            Domain entity = serviceLocator.getService(Domain.class);
            Dom dom = Dom.unwrap(entity);
            DomDocument document = dom.document;
            ConfigModel rootModel = dom.document.getRoot().model;

            ResourcesGenerator resourcesGenerator = new NOOPResourcesGenerator(serviceLocator);
            resourcesGenerator.generateSingle(rootModel, document);
            resourcesGenerator.endGeneration();
        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
            //retVal = "Exception encountered during generation process: " + ex.toString() + "\nPlease look at server.log for more information.";
        }

        status.append("\n------------------------");
        status.append("All Commands used in REST Admin:\n");
        for (String ss : commandsUsed) {
            status.append(ss + "\n");
        }

        listOfCommands();
        for (String ss : commandsUsed) {
            allCommands.remove(ss);
        }

        status.append("\n------------------------");
        status.append("Missing Commands not used in REST Admin:\n");

        for (String ss : allCommands) {
            if (hasTargetParam(ss)) {
                status.append(ss + "          has a target param " + "\n");
            } else {
                status.append(ss + "\n");
            }
        }
        status.append("\n------------------------");
        status.append("REST-REDIRECT Commands defined on ConfigBeans:\n");

        for (String ss : restRedirectCommands) {
            status.append(ss + "\n");
        }


        status.append("\n------------------------");
        status.append("Commands to Resources Mapping Usage in REST Admin:\n");

        for (String ss : commandsToResources.keySet()) {
            if (hasTargetParam(ss)) {
                status.append(ss + "   :::target:::   " + commandsToResources.get(ss) + "\n");
            } else {
                status.append(ss + "      :::      " + commandsToResources.get(ss) + "\n");
            }

        }
        status.append("\n------------------------");
        status.append("Resources with Delete Commands in REST Admin (not counting RESTREDIRECT:\n");
        for (String ss : resourcesToDeleteCommands.keySet()) {
            status.append(ss + "      :::      " + resourcesToDeleteCommands.get(ss) + "\n");
        }

        FileOutputStream f = null;
        try {
            f = new FileOutputStream(System.getProperty("user.home") + "/GlassFishI18NData.properties");
            propsI18N.store(f, "");

        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ex) {
                    RestLogging.restLogger.log(Level.SEVERE, null, ex);
                }
            }
        }
        return status.toString();
    }

    @GET
    @Produces({MediaType.TEXT_HTML})
    public String getHtml() {
        try {
            Domain entity = serviceLocator.getService(Domain.class);
            Dom dom = Dom.unwrap(entity);
            DomDocument document = dom.document;
            ConfigModel rootModel = dom.document.getRoot().model;

            ResourcesGenerator resourcesGenerator = new NOOPResourcesGenerator(serviceLocator);
            resourcesGenerator.generateSingle(rootModel, document);
            resourcesGenerator.endGeneration();
        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }

        status.append("<h4>All Commands used in REST Admin</h4>\n<ul>\n");
        for (String ss : commandsUsed) {
            status.append("<li>").append(ss).append("</li>\n");
        }

        listOfCommands();
        for (String ss : commandsUsed) {
            allCommands.remove(ss);
        }

        status.append("</ul>\n<hr/>\n")
                .append("<h4>Missing Commands not used in REST Admin</h4>\n<ul>\n");

        for (String ss : allCommands) {
            if (hasTargetParam(ss)) {
                status.append("<li>").append(ss).append("          has a target param.</li>\n");
            } else {
                status.append("<li>").append(ss).append("</li>\n");
            }
        }

        status.append("</ul>\n<hr/>\n")
                .append("<h4>REST-REDIRECT Commands defined on ConfigBeans</h4>\n<ul>\n");

        for (String ss : restRedirectCommands) {
            status.append("<li>").append(ss).append("</li>\n");
        }


        status.append("</ul>\n<hr/>\n")
                .append("<h4>Commands to Resources Mapping Usage in REST Admin</h4>\n")
                .append("<table border=\"1\" style=\"border-collapse: collapse\">\n")
                .append("<tr><th>Command</th><th>Target</th><th>Resource</th></tr>\n");

        for (String ss : commandsToResources.keySet()) {
            status.append("<tr><td>").append(ss).append("</td><td>")
                    .append(hasTargetParam(ss) ? "target" : "").append("</td><td>")
                    .append(commandsToResources.get(ss)).append("</td></tr>\n");
        }
        status.append("</table>\n<hr/>\n")
                .append("<h4>Resources with Delete Commands in REST Admin (not counting RESTREDIRECT)</h4>\n")
                .append("<table border=\"1\" style=\"border-collapse: collapse\">\n")
                .append("<tr><th>Resource</th><th>Delete Command</th></tr>\n");
        for (String ss : resourcesToDeleteCommands.keySet()) {
            status.append("<tr><td>").append(ss)
                    .append("</td><td>").append(resourcesToDeleteCommands.get(ss)).append("</td></tr>\n");
        }
        status.append("</table>");

        FileOutputStream f = null;
        try {
            f = new FileOutputStream(System.getProperty("user.home") + "/GlassFishI18NData.properties");
            propsI18N.store(f, "");

        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ex) {
                    RestLogging.restLogger.log(Level.SEVERE, null, ex);
                }
            }
        }
        return status.toString();
    }

    public void listOfCommands() {
        CommandRunner cr = serviceLocator.getService(CommandRunner.class);
        RestActionReporter ar = new RestActionReporter();
        ParameterMap parameters = new ParameterMap();
        cr.getCommandInvocation("list-commands", ar, getSubject())
                .parameters(parameters).execute();
        List<ActionReport.MessagePart> children = ar.getTopMessagePart().getChildren();
        for (ActionReport.MessagePart part : children) {
            allCommands.add(part.getMessage());

        }
        ar = new RestActionReporter();
        parameters = new ParameterMap();
        parameters.add("DEFAULT", "_");
        cr.getCommandInvocation("list-commands", ar, getSubject()).parameters(parameters).execute();
        children = ar.getTopMessagePart().getChildren();
        for (ActionReport.MessagePart part : children) {
            allCommands.add(part.getMessage());

        }

    }

    class NOOPClassWriter implements ClassWriter {

        private String className;

        public NOOPClassWriter(String className, String baseClassName, String resourcePath) {
            this.className = className;
            if (baseClassName.equals("TemplateRestResource")) {
                resourcesToDeleteCommands.put(className, ""); //init the map with empty values
            }
        }

        @Override
        public void createGetCommandResourcePaths(List<CommandResourceMetaData> commandMetaData) {
            for (CommandResourceMetaData metaData : commandMetaData) {
//                StatusGenerator.this.status.append("   ");
//                StatusGenerator.this.status.append(metaData.command);
                commandsUsed.add(metaData.command);

                if (commandsToResources.containsKey(metaData.command)) {
                    String val = commandsToResources.get(metaData.command) + ", " + className;
                    commandsToResources.put(metaData.command, val);

                } else {
                    commandsToResources.put(metaData.command, className);
                }

//                StatusGenerator.this.status.append("\n");
            }

//            StatusGenerator.this.status.append("\n");

        }

        @Override
        public void createGetCommandResource(String commandResourceClassName, String resourcePath) {
        }

        @Override
        public void createCommandResourceConstructor(String commandResourceClassName, String commandName, String httpMethod, boolean linkedToParent, ParameterMetaData[] commandParams, String commandDisplayName, String commandAction) {
        }

        @Override
        public void createCustomResourceMapping(String resourceClassName, String mappingPath) {
        }

        @Override
        public void done() {
        }

        @Override
        public void createGetDeleteCommand(String commandName) {
//            StatusGenerator.this.status.append("   ");
//            StatusGenerator.this.status.append(commandName);
//            StatusGenerator.this.status.append("\n");
            commandsUsed.add(commandName);
            if (commandsToResources.containsKey(commandName)) {
                String val = commandsToResources.get(commandName) + ", " + className;
                commandsToResources.put(commandName, val);

            } else {
                commandsToResources.put(commandName, className);
            }
            resourcesToDeleteCommands.put(className, commandName);
        }

        @Override
        public void createGetPostCommand(String commandName) {
//            StatusGenerator.this.status.append("   ");
//            StatusGenerator.this.status.append(commandName);
//            StatusGenerator.this.status.append("\n");
            commandsUsed.add(commandName);
            if (commandsToResources.containsKey(commandName)) {
                String val = commandsToResources.get(commandName) + ", " + className;
                commandsToResources.put(commandName, val);

            } else {
                commandsToResources.put(commandName, className);
            }

        }

        @Override
        public void createGetChildResource(String path, String childResourceClassName) {
        }

        @Override
        public void createGetChildResourceForListResources(String keyAttributeName, String childResourceClassName) {
        }

        @Override
        public void createGetPostCommandForCollectionLeafResource(String commandName) {
//            StatusGenerator.this.status.append("   ");
//            StatusGenerator.this.status.append(commandName);
//            StatusGenerator.this.status.append("\n");
            commandsUsed.add(commandName);
            if (commandsToResources.containsKey(commandName)) {
                String val = commandsToResources.get(commandName) + ", " + className;
                commandsToResources.put(commandName, val);

            } else {
                commandsToResources.put(commandName, className);
            }
        }

        @Override
        public void createGetDeleteCommandForCollectionLeafResource(String commandName) {
//            StatusGenerator.this.status.append("   ");
//            StatusGenerator.this.status.append(commandName);
//            StatusGenerator.this.status.append("\n");
            commandsUsed.add(commandName);
            if (commandsToResources.containsKey(commandName)) {
                String val = commandsToResources.get(commandName) + ", " + className;
                commandsToResources.put(commandName, val);

            } else {
                commandsToResources.put(commandName, className);
            }
        }

        @Override
        public void createGetDisplayNameForCollectionLeafResource(String displayName) {
        }
    }

    class NOOPResourcesGenerator extends ResourcesGeneratorBase {

        public NOOPResourcesGenerator(ServiceLocator h) {
            super(h);

        }

        @Override
        public ClassWriter getClassWriter(String className, String baseClassName, String resourcePath) {
            return new NOOPClassWriter(className, baseClassName, resourcePath);
        }

        @Override
        public String endGeneration() {
            return "done";
        }

        @Override
        public void configModelVisited(ConfigModel model) {
            //I18n Calculation
            for (String a : model.getAttributeNames()) {
                String key = model.targetTypeName + "." + Util.eleminateHypen(a);
                propsI18N.setProperty(key + ".label", a);
                propsI18N.setProperty(key + ".help", a);
            }


            Class<? extends ConfigBeanProxy> cbp = null;
            try {
                cbp = (Class<? extends ConfigBeanProxy>) model.classLoaderHolder.loadClass(model.targetTypeName);
            } catch (MultiException e) {
                e.printStackTrace();
                return;
            }
            RestRedirects restRedirects = cbp.getAnnotation(RestRedirects.class);
            if (restRedirects != null) {

                RestRedirect[] values = restRedirects.value();
                for (RestRedirect r : values) {
                    commandsUsed.add(r.commandName());
                    restRedirectCommands.add(r.commandName());

                }
            }
        }
    }

    public Boolean hasTargetParam(String command) {
        try {
            if (command != null) {
                Collection<CommandModel.ParamModel> params;
                params = getParamMetaData(command);


                Iterator<CommandModel.ParamModel> iterator = params.iterator();
                CommandModel.ParamModel paramModel;
                while (iterator.hasNext()) {
                    paramModel = iterator.next();
                    //   Param param = paramModel.getParam();
                    if (paramModel.getName().equals("target")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            RestLogging.restLogger.log(Level.FINE, e.getMessage());
        }

        return false;
    }

    public Collection<CommandModel.ParamModel> getParamMetaData(String commandName) {
        CommandRunner cr = serviceLocator.getService(CommandRunner.class);
        CommandModel cm = cr.getModel(commandName, RestLogging.restLogger);
        Collection<CommandModel.ParamModel> params = cm.getParameters();
        return params;
    }
}
