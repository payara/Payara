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
package org.glassfish.admin.rest.generator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigInjector;

/**
 * @author Mitesh Meswani
 */
public class CommandResourceMetaData {

    public String command;
    public String httpMethod;
    public String resourcePath;
    public String displayName;
    public ParameterMetaData[] commandParams;
    public String customClassName; // used by the custom resource mapping
    private static final Map<String, List<CommandResourceMetaData>> restRedirects = new HashMap<String, List<CommandResourceMetaData>>();

    public static class ParameterMetaData {
        String name;
        String value;
    }

    public static List<CommandResourceMetaData> getMetaData(String beanName) {
        //TODO need to read this from a file instead of from memory and then initialize data structure Map<String, List<CommandResourceMetaData> >
        List<CommandResourceMetaData> retVal = new LinkedList<CommandResourceMetaData>();
        for (String[] currentRow : configBeansToCommandResourcesMap) {
            if (beanName.equals(currentRow[0])) {
                CommandResourceMetaData metaData = new CommandResourceMetaData();
                metaData.command = currentRow[1];
                metaData.httpMethod = currentRow[2];
                metaData.resourcePath = currentRow[3];
                metaData.displayName = currentRow[4];
                // Each row has variable no of commandParams. If commandParams are present, extract them from current row and stuff into a String[]
                int PARAMETER_START_INDEX = 5;
                if (currentRow.length > PARAMETER_START_INDEX) {
                    metaData.commandParams = new ParameterMetaData[currentRow.length - PARAMETER_START_INDEX];
                    for (int i = PARAMETER_START_INDEX; i < currentRow.length; i++) {
                        String[] nameValue = currentRow[i].split("=", 2); // The params are written as 'name=value', split them around "="
                        ParameterMetaData currentParam = new ParameterMetaData();
                        metaData.commandParams[i - PARAMETER_START_INDEX] = currentParam;
                        currentParam.name = nameValue[0];
                        currentParam.value = nameValue[1];
                    }
                }
                retVal.add(metaData);
            }
        }
        final List<CommandResourceMetaData> restRedirectPointToBean = getRestRedirectPointToBean(beanName);
        if (restRedirectPointToBean != null) {
            retVal.addAll(restRedirectPointToBean);
        }
        return retVal;
    }

    public static List<CommandResourceMetaData> getCustomResourceMapping(String beanName) {
        List<CommandResourceMetaData> customResources = new LinkedList<CommandResourceMetaData>();
        for (String[] row : configBeanCustomResources) {
            if (row[0].equals(beanName)) {
                CommandResourceMetaData metaData = new CommandResourceMetaData();
                metaData.customClassName = row[1];
                metaData.resourcePath = row[2];

                customResources.add(metaData);
            }
        }

        return customResources;
    }

    @SuppressWarnings("unchecked")
    public static List<CommandResourceMetaData> getRestRedirectPointToBean(String beanName) {
        synchronized (restRedirects) {
            if (restRedirects.isEmpty()) {
                final ServiceLocator habitat = Globals.getDefaultHabitat();

                processConfigBeans(habitat);

                List<ActiveDescriptor<?>> iter = habitat.getDescriptors(
                        BuilderHelper.createContractFilter(AdminCommand.class.getName()));
                for (ActiveDescriptor<?> ad : iter) {
                    if (!(ad.getQualifiers().contains(RestEndpoints.class.getName()))) {
                        continue;
                    }
                    
                    if (!ad.isReified()) {
                        try {
                            habitat.reifyDescriptor(ad);
                        }
                        catch (MultiException me) {
                            // If we can't see the command, forget it
                            continue;
                        }
                    }
                    
                    final Class<? extends AdminCommand> clazz = (Class<? extends AdminCommand>) ad.getImplementationClass();
                    RestEndpoints endpoints = clazz.getAnnotation(RestEndpoints.class);
                    if (endpoints != null) {
                        RestEndpoint[] list = endpoints.value();
                        if ((list != null) && (list.length > 0)) {
                            for (RestEndpoint endpoint : list) {
                                Service service = clazz.getAnnotation(Service.class);
                                String configBean = endpoint.configBean().getSimpleName();

                                CommandResourceMetaData metaData = new CommandResourceMetaData();
                                metaData.command = service.name();
                                metaData.httpMethod = endpoint.opType().name();
                                metaData.resourcePath = endpoint.path().isEmpty() ? service.name() : endpoint.path();
                                metaData.displayName = endpoint.description().isEmpty() ? metaData.resourcePath : endpoint.description();

                                metaData.commandParams = new ParameterMetaData[endpoint.params().length];
                                int index = 0;
                                for (RestParam param : endpoint.params()) {
                                    ParameterMetaData currentParam = new ParameterMetaData();
                                    metaData.commandParams[index] = currentParam;
                                    currentParam.name = param.name();
                                    currentParam.value = param.value();
                                    index++;
                                }
                                addCommandMetaData(configBean, metaData);
                            }
                        }
                    }
                }
            }
        }

        return restRedirects.get(beanName);
    }

    private static void processConfigBeans(ServiceLocator serviceLocator) {

		List<ActiveDescriptor<?>> allDescriptors = serviceLocator.getDescriptors(
		        BuilderHelper.createContractFilter(ConfigInjector.class.getName()));
		
		HashSet<String> alreadyChecked = new HashSet<String>();

        for (ActiveDescriptor<?> ad : allDescriptors){
            List<String> targets = ad.getMetadata().get("target");
            if (targets == null) continue;
            
            for (String t : targets) {
                if (alreadyChecked.contains(t)) continue;
                alreadyChecked.add(t);
                
                try {
                    Class<?> tclass = Class.forName(t);
                    if (tclass != null && ConfigBeanProxy.class.isAssignableFrom(tclass)) {
                        String beanName = tclass.getSimpleName();
                        for (Method m : tclass.getMethods()) {
                            if (m.isAnnotationPresent(Create.class)) {
                                addCreateMethod(beanName, m, m.getAnnotation(Create.class));
                            } else if (m.isAnnotationPresent(Delete.class)) {
                                addDeleteMethod(beanName, m, m.getAnnotation(Delete.class));
                            }
                        }
                    }
                } catch (ClassNotFoundException cnfe) {
                    continue;
                }
            }
        }
    }

    private static void addCreateMethod(String beanName, Method m, Create create) {
        CommandResourceMetaData metaData = new CommandResourceMetaData();
        metaData.command = create.value();
        metaData.httpMethod = "POST";
        metaData.resourcePath = create.value();
        metaData.displayName = create.value();
        addCommandMetaData(beanName, metaData);
    }

    private static void addDeleteMethod(String beanName, Method m, Delete delete) {
        CommandResourceMetaData metaData = new CommandResourceMetaData();
        metaData.command = delete.value();
        metaData.httpMethod = "DELETE";
        metaData.resourcePath = delete.value();
        metaData.displayName = delete.value();
        addCommandMetaData(beanName, metaData);
    }

    private static void addCommandMetaData(String beanName, CommandResourceMetaData cmd) {
        List<CommandResourceMetaData> commandList = restRedirects.get(beanName);
        if (commandList == null) {
            commandList = new ArrayList<CommandResourceMetaData>();
            restRedirects.put(beanName, commandList);
        }
        commandList.add(cmd);
    }

    // This data structure is for exceptional cases only. The preferred mapping approach is to
    // use @RestEndpoints/@RestEndpoint
    private static String configBeansToCommandResourcesMap[][] = {
        //{config-bean, command, method, resource-path, command-action, command-params...}
        {"Domain", "change-admin-password", "POST", "change-admin-password", "change-admin-password"},
        {"Domain", "stop-domain", "POST", "stop", "Stop"},
        {"IiopListener", "create-ssl", "POST", "create-ssl", "Create", "id=$parent", "type=iiop-listener"}, // Not used? IiopListener not in nucleus
        {"IiopService", "create-ssl", "POST", "create-ssl", "Create", "type=iiop-service"}, // Not used? IiopListener not in nucleus
        {"LbConfig", "create-http-listener", "POST", "create-http-listener", "create-http-listener"},
        {"LbConfig", "delete-http-listener", "POST", "delete-http-listener", "delete-http-listener"},
        {"ListApplication", "create-lifecycle-module", "POST", "create-lifecycle-module", "Create Lifecycle Module"}, // TODO: ListApplication not found
        {"ListApplication", "delete-lifecycle-module", "DELETE", "delete-lifecycle-module", "Delete Lifecycle Module"},};
    private static final String[][] configBeanCustomResources = {
        // ConfigBean, Custom Resource Class, path
        {"Cluster", "SystemPropertiesCliResource", "system-properties"},
        {"Config", "SystemPropertiesCliResource", "system-properties"},
        {"Domain", "JmxServiceUrlsResource", "jmx-urls"},
        {"Domain", "LogViewerResource", "view-log"},
        {"Domain", "SetDomainConfigResource", "set"},
        {"Domain", "SystemPropertiesCliResource", "system-properties"},
        {"NetworkListener", "FindHttpProtocolResource", "find-http-protocol"},
        {"Server", "SystemPropertiesCliResource", "system-properties"}
    };
}
