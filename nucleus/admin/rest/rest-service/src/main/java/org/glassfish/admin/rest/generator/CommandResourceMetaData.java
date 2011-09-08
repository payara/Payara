/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Inhabitant;

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

    public static List<CommandResourceMetaData> getRestRedirectPointToBean(String beanName) {
        synchronized (restRedirects) {
            if (restRedirects.isEmpty()) {
                Iterator<Inhabitant<?>> iter = Globals.getDefaultHabitat().getInhabitantsByContract(AdminCommand.class.getName()).iterator();
                while (iter.hasNext()) {
                    Inhabitant<?> inhab = iter.next();
                    final Class<? extends AdminCommand> clazz = (Class<? extends AdminCommand>)inhab.type();
                    RestEndpoints endpoints = clazz.getAnnotation(RestEndpoints.class);
                    if (endpoints != null) {
                        //Logger.getLogger("CommandResourceMetaData").log(Level.FINE, "Found annotations on {0}", clazz.getName());
                        RestEndpoint[] list = endpoints.value();
                        if ((list != null) && (list.length > 0)) {
                            for (RestEndpoint endpoint : list) {
                                Service service = clazz.getAnnotation(Service.class);
                                String configBean = endpoint.configBean().getSimpleName();

                                CommandResourceMetaData metaData = new CommandResourceMetaData();
                                metaData.command = service.name();
                                metaData.httpMethod = endpoint.opType().name();
                                metaData.resourcePath = endpoint.path();
                                metaData.displayName = endpoint.description();

                                metaData.commandParams = new ParameterMetaData[endpoint.params().length];
                                int index = 0;
                                for (RestParam param : endpoint.params()) {
                                    ParameterMetaData currentParam = new ParameterMetaData();
                                    metaData.commandParams[index] = currentParam;
                                    currentParam.name = param.name();
                                    currentParam.value = param.value();
                                }

                                List<CommandResourceMetaData> commandList = restRedirects.get(configBean);
                                if (commandList == null) {
                                    commandList = new ArrayList<CommandResourceMetaData>();
                                    restRedirects.put(configBean, commandList);
                                }
                                commandList.add(metaData);
                            }
                        }
                    }
                }
            }
        }

        return restRedirects.get(beanName);
    }
    private static String configBeansToCommandResourcesMap[][] = {
        //{config-bean, command, method, resource-path, command-action, command-params...}
        {"Application", "_get-deployment-configurations", "GET", "_get-deployment-configurations", "Get Deployment Configurations", "appname=$parent"},
        {"Application", "disable", "POST", "disable", "Disable", "id=$parent"},
        {"Application", "disable-http-lb-application", "POST", "disable-http-lb-application", "disable-http-lb-application", "name=$parent"},
        {"Application", "enable", "POST", "enable", "Enable", "id=$parent"},
        {"Application", "enable-http-lb-application", "POST", "application", "disable-http-lb-application", "name=$parent"},
        {"Application", "get-client-stubs", "GET", "get-client-stubs", "Get Client Stubs", "appname=$parent"},
        {"Application", "list-web-context-param", "GET", "list-web-context-param", "application", "name=$parent"},
        {"Application", "list-web-env-entry", "GET", "list-web-env-entry", "list-web-env-entry", "id=$parent"},
        {"Application", "set-web-context-param", "POST", "set-web-context-param", "set-web-context-param", "id=$parent"},
        {"Application", "set-web-env-entry", "POST", "set-web-env-entry", "set-web-env-entry", "id=$parent"},
        {"Application", "show-component-status", "GET", "show-component-status", "Show Component Status", "id=$parent"},
        {"Application", "unset-web-context-param", "POST", "unset-web-context-param", "unset-web-context-param", "id=$parent"},
        {"Application", "unset-web-env-entry", "POST", "unset-web-env-entry", "unset-web-env-entry", "id=$parent"},
        {"Applications", "list-applications", "GET", "list-applications", "list-applications"},
        {"Applications", "list-application-refs", "GET", "list-application-refs", "list-application-refs"},
        {"AuthRealm", "__list-group-names", "GET", "list-group-names", "List Group Names", "realmName=$parent"},
        {"AuthRealm", "__supports-user-management", "GET", "supports-user-management", "Check Support", "realmName=$parent"},
        {"AuthRealm", "create-file-user", "POST", "create-user", "Create", "authrealmname=$parent"},
        {"AuthRealm", "delete-file-user", "DELETE", "delete-user", "Delete", "authrealmname=$parent"},
        {"AuthRealm", "list-file-users", "GET", "list-users", "List Users", "authrealmname=$parent"},
        {"AuthRealm", "update-file-user", "POST", "update-user", "Update User", "authrealmname=$parent"},
        {"Cluster", "__get-jmsdest", "GET", "__get-jmsdest", "Get JMS Destination", "target=$parent"},
        {"Cluster", "__resolve-tokens", "GET", "resolve-tokens", "Resolve Tokens", "target=$parent"},
        {"Cluster", "__update-jmsdest", "POST", "__update-jmsdest", "Get JMS Destination", "target=$parent"},
        {"Cluster", "change-master-broker", "POST", "change-master-broker", "change-master-broker"},
        {"Cluster", "configure-jms-cluster", "POST", "configure-jms-cluster", "configure-jms-cluster", "id=$parent"},
        {"Cluster", "configure-lb-weight", "POST", "configure-lb-weight", "Configure LB Weight", "target=$parent"},
        {"Cluster", "create-http-redirect", "POST", "create-http-redirect", "create-http-redirect"},
        {"Cluster", "create-jmsdest", "POST", "create-jmsdest", "Create JMS Destination", "target=$parent"},
        {"Cluster", "create-lifecycle-module", "POST", "create-lifecycle-module", "Create Lifecycle Module", "target=$parent"},
        {"Cluster", "delete-cluster", "POST", "delete-cluster", "Delete Cluster", "id=$parent"}, // FIXME
        {"Cluster", "delete-http-redirect", "DELETE", "delete-http-redirect", "delete-http-redirect"},
        {"Cluster", "delete-jmsdest", "DELETE", "delete-jmsdest", "Delete JMS Destination", "target=$parent"},
        {"Cluster", "delete-lifecycle-module", "DELETE", "delete-lifecycle-module", "Delete Lifecycle Module", "target=$parent"},
        {"Cluster", "disable-http-lb-server", "POST", "disable-http-lb-server", "disable-http-lb-server", "id=$parent"},
        {"Cluster", "enable-http-lb-server", "POST", "enable-http-lb-server", "enable-http-lb-server", "id=$parent"},
        {"Cluster", "flush-jmsdest", "POST", "flush-jmsdest", "Flush", "target=$parent"},
        {"Cluster", "generate-jvm-report", "GET", "generate-jvm-report", "Generate Report", "target=$parent"},
        {"Cluster", "get-health", "GET", "get-health", "Get Health", "id=$parent"},
        {"Cluster", "jms-ping", "GET", "jms-ping", "Ping JMS", "id=$parent"},
        {"Clusters", "list-clusters", "GET", "list-clusters", "List Clusters"},
        {"Cluster", "list-instances", "GET", "list-instances", "List Cluster Instances", "id=$parent"},
        {"Cluster", "list-jmsdest", "GET", "list-jmsdest", "List JMS Destinations", "id=$parent"},
        {"Cluster", "list-lifecycle-modules", "GET", "list-lifecycle-modules", "List Lifecycle Modules", "id=$parent"},
        {"Cluster", "migrate-timers", "POST", "migrate-timers", "Migrate Timers"},
        {"Cluster", "start-cluster", "POST", "start-cluster", "Start Cluster", "id=$parent"},
        {"Cluster", "stop-cluster", "POST", "stop-cluster", "Stop Cluster", "id=$parent"},
        {"Config", "__resolve-tokens", "GET", "resolve-tokens", "Resolve Tokens", "target=$parent"},
        {"Config", "delete-config", "POST", "delete-config", "Delete Config", "id=$parent"},
//        {"Config", "__synchronize-realm-from-config", "POST", "synchronize-realm-from-config", "Synchronize-realm-from-config", "target=$parent"},
//        {"Configs", "copy-config", "POST", "copy-config", "Copy Config"},
        {"Configs", "list-configs", "GET", "list-configs", "list-configs"},
        {"ConnectionPool", "ping-connection-pool", "GET", "ping", "Ping"},
        {"Domain", "__anonymous-user-enabled", "GET", "anonymous-user-enabled", "Get"},
        {"Domain", "__locations", "GET", "location", "Location"},
        {"Domain", "_get-host-and-port", "GET", "host-port", "HostPort"},
        {"Domain", "_get-restart-required", "GET", "_get-restart-required", "Restart Reasons"},
        {"Domain", "_get-runtime-info", "GET", "get-runtime-info", "Get Runtime Info"},
        {"Domain", "_bootstrap-secure-admin", "POST", "_bootstrap-secure-admin", "_bootstrap-secure-admin"},
        {"Domain", "_create-node", "POST", "_create-node", "_create-node"},
        {"Domain", "_create-node-implicit", "POST", "_create-node-implicit", "_create-node-implicit"},
        {"Domain", "_deploy", "POST", "_deploy", "_deploy"},
        {"Domain", "_dump-hk2", "POST", "_dump-hk2", "_dump-hk2"},
        {"Domain", "_get-habitat-info", "GET", "_get-habitat-info", "_get-habitat-info"},
        {"Domain", "_get-log-file", "GET", "_get-log-file", "_get-log-file"},
        {"Domain", "_get-rest-admin-config", "GET", "_get-rest-admin-config", "_get-rest-admin-config"},
        {"Domain", "_get-targets", "GET", "_get-targets", "_get-targets"},
        {"Domain", "_gms-announce-after-start-cluster-command", "POST", "_gms-announce-after-start-cluster-command", "_gms-announce-after-start-cluster-command"},
        {"Domain", "_gms-announce-after-stop-cluster-command", "POST", "_gms-announce-after-stop-cluster-command", "_gms-announce-after-stop-cluster-command"},
        {"Domain", "_gms-announce-before-start-cluster-command", "POST", "_gms-announce-before-start-cluster-command", "_gms-announce-before-start-cluster-command"},
        {"Domain", "_gms-announce-before-stop-cluster-command", "POST", "_gms-announce-before-stop-cluster-command", "_gms-announce-before-stop-cluster-command"},
        {"Domain", "_instanceValidateRemoteDirDeployment", "POST", "_instanceValidateRemoteDirDeployment", "_instanceValidateRemoteDirDeployment"},
        {"Domain", "_lifecycle", "POST", "_lifecycle", "_lifecycle"},
        {"Domain", "_list-app-refs", "GET", "_list-app-refs", "_list-app-refs"},
        {"Domain", "_list-resources", "GET", "_list-resources", "_list-resources"},
        {"Domain", "_post-register-instance", "POST", "_post-register-instance", "_post-register-instance"},
        {"Domain", "_post-unregister-instance", "POST", "_post-unregister-instance", "_post-unregister-instance"},
        {"Domain", "_postdeploy", "POST", "_postdeploy", "_postdeploy"},
        {"Domain", "_recover-transactions-internal", "POST", "_recover-transactions-internal", "_recover-transactions-internal"},
        {"Domain", "_register-instance", "POST", "_register-instance", "_register-instance"},
        {"Domain", "_register-instance-at-instance", "POST", "_register-instance-at-instance", "_register-instance-at-instance"},
        {"Domain", "_restart-instance", "POST", "_restart-instance", "_restart-instance"},
        {"Domain", "_set-rest-admin-config", "POST", "_set-rest-admin-config", "_set-rest-admin-config"},
        {"Domain", "_stop-instance", "POST", "_stop-instance", "_stop-instance"},
        {"Domain", "_synchronize-files", "POST", "_synchronize-files", "_synchronize-files"},
        {"Domain", "_unregister-instance", "POST", "_unregister-instance", "_unregister-instance"},
        {"Domain", "_validate-node", "POST", "_validate-node", "_validate-node"},
        {"Domain", "_validateRemoteDirDeployment", "POST", "_validateRemoteDirDeployment", "_validateRemoteDirDeployment"},
        {"Domain", "change-admin-password", "POST", "change-admin-password", "change-admin-password"},
        {"Domain", "collect-log-files", "POST", "collect-log-files", "collect-log-files"},
        {"Domain", "configure-ldap-for-admin", "POST", "configure-ldap-for-admin", "configure-ldap-for-admin"},
        {"Domain", "create-instance", "POST", "create-instance", "Create Instance"},
        {"Domain", "create-password-alias", "POST", "create-password-alias", "create-password-alias"},
        {"Domain", "delete-password-alias", "POST", "delete-password-alias", "delete-password-alias"},
        {"Domain", "list-password-aliases", "GET", "list-password-aliases", "list-password-aliases"},
        {"Domain", "update-password-alias", "POST", "update-password-alias", "update-password-alias"},
        {"Domain", "disable-monitoring", "POST", "disable-monitoring", "Disable Monitoring"},
        {"Domain", "disable-secure-admin", "POST", "disable-secure-admin", "disable-secure-admin"},
        {"Domain", "enable-monitoring", "POST", "enable-monitoring", "Enable Monitoring"},
        {"Domain", "enable-secure-admin", "POST", "enable-secure-admin", "enable-secure-admin"},
        {"Domain", "export-sync-bundle", "POST", "export-sync-bundle", "export-sync-bundle"},
        {"Domain", "generate-domain-schema", "POST", "generate-domain-schema", "Generate Domain Schema"},
        {"Domain", "get", "POST", "get", "Get"},
        {"Domain", "list-commands", "GET", "list-commands", "list-commands"},
        {"Domain", "list-containers", "GET", "list-containers", "list-containers"},
        {"Domain", "list-instances", "GET", "list-instances", "List Instances"},
        {"Domain", "list-log-attributes", "GET", "list-log-attributes", "list-log-attributes"},
        {"Domain", "list-log-levels", "GET", "list-log-levels", "LogLevels"},
        {"Domain", "delete-log-levels", "DELETE", "delete-log-levels", "delete-log-levels"},
        {"Domain", "list-modules", "GET", "list-modules", "list-modules"},
        {"Domain", "list-nodes", "GET", "list-nodes", "list-nodes"},
        {"Domain", "list-nodes-config", "GET", "list-nodes-config", "list-nodes-config"},
        {"Domain", "list-jvm-options", "GET", "list-jvm-options", "list-jvm-options"},
        {"Domain", "list-persistence-types", "GET", "list-persistence-types", "List Persistence Types"},
        {"Domain", "list-system-properties", "GET", "list-system-properties", "list-system-properties"},
        {"Domain", "list-timers", "GET", "list-timers", "list-timers"},
        {"Domain", "list-transports", "GET", "list-transports", "list-transports"},
        {"Domain", "restart-domain", "POST", "restart", "Restart"},
        {"Domain", "rotate-log", "POST", "rotate-log", "RotateLog"},
        {"Domain", "set-log-attributes", "POST", "set-log-attributes", "set-log-attributes"},
        {"Domain", "set-log-levels", "POST", "set-log-levels", "LogLevel"},
        {"Domain", "stop-domain", "POST", "stop", "Stop"},
        {"Domain", "uptime", "GET", "uptime", "Uptime"},
        {"Domain", "version", "GET", "version", "Version"},
        {"HttpService", "list-http-listeners", "GET", "list-http-listeners", "list-http-listeners"},
        {"HttpService", "list-network-listeners", "GET", "list-network-listeners", "list-network-listeners"},
        {"HttpService", "list-protocol-filters", "GET", "list-protocol-filters", "list-protocol-filters"},
        {"HttpService", "list-protocol-finders", "GET", "list-protocol-finders", "list-protocol-finders"},
        {"HttpService", "list-protocols", "GET", "list-protocols", "list-protocols"},
        {"HttpService", "list-virtual-servers", "GET", "list-virtual-servers", "list-virtual-servers"},
        {"IiopListener", "create-ssl", "POST", "create-ssl", "Create", "id=$parent", "type=iiop-listener"},
        {"IiopService", "create-ssl", "POST", "create-ssl", "Create", "type=iiop-service"},
        {"IiopService", "list-iiop-listeners", "GET", "list-iiop-listeners", "list-iiop-listeners"},
        {"JmxConnector", "create-ssl", "POST", "create-ssl", "Create", "id=$parent", "type=jmx-connector"},
        {"JavaConfig", "create-profiler", "POST", "create-profiler", "Create Profiler"},
        {"JavaConfig", "generate-jvm-report", "GET", "generate-jvm-report", "Generate Report", "target=$grandparent"},
        {"JmsService", "list-jms-hosts", "GET", "list-jms-hosts", "list-jms-hosts"},
        {"JmsHost", "delete-jms-host", "DELETE", "delete-jms-host", "Delete JMS Host", "id=$parent"},
        {"LbConfig", "create-http-lb-ref", "POST", "create-http-lb-ref", "create-http-lb-ref", "config=$parent"},
        {"LbConfig", "create-http-listener", "POST", "create-http-listener", "create-http-listener"},
        {"LbConfig", "delete-http-lb-ref", "POST", "delete-http-lb-ref", "delete-http-lb-ref"},
        {"LbConfig", "delete-http-listener", "POST", "delete-http-listener", "delete-http-listener"},
        {"LbConfig", "export-http-lb-config", "POST", "export-http-lb-config", "export-http-lb-config", "config=$parent"},
        {"LbConfigs", "list-http-lb-configs", "GET", "list-http-lb-configs", "list-http-lb-configs"},
        {"LbConfigs", "list-http-lbs", "GET", "list-http-lbs", "list-http-lbs"},
        {"ListApplication", "__list-webservices", "GET", "list-webservices", "List Webservices"},
        {"ListApplication", "_get-context-root", "GET", "get-context-root", "Get Context Root"},
        {"ListApplication", "_get-relative-jws-uri", "GET", "_get-relative-jws-uri", "Get Relative JWS URI"},
        {"ListApplication", "_is-sniffer-user-visible", "GET", "is-sniffer-user-visible", "Is Sniffer User Visible"},
        {"ListApplication", "create-lifecycle-module", "POST", "create-lifecycle-module", "Create Lifecycle Module"},
        {"ListApplication", "delete-lifecycle-module", "DELETE", "delete-lifecycle-module", "Delete Lifecycle Module"},
        {"ListApplication", "list-components", "GET", "list-components", "List Components"},
        {"ListApplication", "list-lifecycle-modules", "GET", "list-lifecycle-modules", "List Lifecycle Modules"},
        {"ListApplication", "list-sub-components", "GET", "list-sub-components", "List Subcomponents"},
        {"ListAuthRealm", "__list-predefined-authrealm-classnames", "GET", "list-predefined-authrealm-classnames", "List Auth Realms"},
        {"LoadBalancer", "apply-http-lb-changes", "POST", "apply-http-lb-changes", "apply-http-lb-changes", "id=$parent"},
        {"LoadBalancer", "export-http-lb-config", "POST", "export-http-lb-config", "export-http-lb-config", "lbname=$parent"},
        {"NetworkListener", "create-ssl", "POST", "create-ssl", "Create", "id=$parent", "type=http-listener"},
        {"Node", "_delete-node", "DELETE", "delete-node", "Delete Node", "id=$parent"},
        {"Node", "_update-node", "POST", "_update-node", "Update Node", "name=$parent"},
        {"Node", "ping-node-ssh", "GET", "ping-node-ssh", "Ping Node", "id=$parent"},
        {"Node", "update-node-ssh", "POST", "update-node-ssh", "Update Node", "id=$parent"},
        {"Node", "update-node-config", "POST", "update-node-config", "Update Node Config", "id=$parent"},
        {"Nodes", "create-node-config", "POST", "create-node-config", "Create Node Config"},
        {"Nodes", "delete-node-config", "POST", "delete-node-config", "Delete Node Config"},
        {"Nodes", "delete-node-ssh", "POST", "delete-node-ssh", "Delete Node SSH"},
        {"Nodes", "create-node-ssh", "POST", "create-node", "Create Node SSH"},
        {"Nodes", "list-nodes-ssh", "GET", "list-nodes-ssh", "list-nodes-ssh"},
        {"Profiler", "delete-profiler", "DELETE", "delete-profiler", "Delete Profiler"},
        {"Protocol", "create-http", "POST", "create-http", "Create", "id=$parent"},
        {"Protocol", "create-protocol-filter", "POST", "create-protocol-filter", "Create", "protocol=$parent"},
        {"Protocol", "create-protocol-finder", "POST", "create-protocol-finder", "Create", "protocol=$parent"},
        {"Protocol", "delete-http", "DELETE", "delete-http", "Delete", "id=$parent"},
        {"Protocol", "delete-protocol-filter", "DELETE", "delete-protocol-filter", "Delete", "protocol=$parent"},
        {"Protocol", "delete-protocol-finder", "DELETE", "delete-protocol-finder", "Delete", "protocol=$parent"},
        {"Protocol", "create-ssl", "POST", "create-ssl", "Create", "id=$parent", "type=http-listener"},
        {"Resources", "_get-activation-spec-class", "GET", "get-activation-spec-class", "Get Activation Spec Class"},
        {"Resources", "_get-admin-object-class-names", "GET", "get-admin-object-class-names", "Get Admin Object Class Names"},
        {"Resources", "_get-admin-object-config-properties", "GET", "get-admin-object-config-properties", "Get Admin Object Config Properties"},
        {"Resources", "_get-admin-object-interface-names", "GET", "get-admin-object-interface-names", "Get Admin Object Interface Names"},
        {"Resources", "_get-built-in-custom-resources", "GET", "get-built-in-custom-resources", "Get Built In Custom Resources"},
        {"Resources", "_get-connection-definition-names", "GET", "get-connection-definition-names", "Get Connection Definition Names"},
        {"Resources", "_get-connection-definition-properties-and-defaults", "GET", "get-connection-definition-properties-and-defaults", "Get Connection Definition Properties And Defaults"},
        {"Resources", "_get-connector-config-java-beans", "GET", "get-connector-config-java-beans", "Get Connector Config Java Beans"},
        {"Resources", "_get-database-vendor-names", "GET", "get-database-vendor-names", "Get Database Vendor Names"},
        {"Resources", "_get-jdbc-driver-class-names", "GET", "get-jdbc-driver-class-names", "Get Jdbc Driver Class Names"},
        {"Resources", "_get-mcf-config-properties", "GET", "get-mcf-config-properties", "Get Mcf Config Properties"},
        {"Resources", "_get-message-listener-config-properties", "GET", "get-message-listener-config-properties", "Get Message Listener Config Properties"},
        {"Resources", "_get-message-listener-config-property-types", "GET", "get-message-listener-config-property-types", "Get Message Listener Config Property Types"},
        {"Resources", "_get-message-listener-types", "GET", "get-message-listener-types", "Get Message Listener Types"},
        {"Resources", "_get-resource-adapter-config-properties", "GET", "get-resource-adapter-config-properties", "Get Resource Adapter Config Properties"},
        {"Resources", "_get-system-rars-allowing-pool-creation", "GET", "get-system-rars-allowing-pool-creation", "Get System Rars Allowing Pool Creation"},
        {"Resources", "_get-validation-class-names", "GET", "get-validation-class-names", "Get Validation Class Names"},
        {"Resources", "_get-validation-table-names", "GET", "get-validation-table-names", "Get Validation Table Names"},
        {"Resources", "add-resources", "POST", "add-resources", "add-resources"},
        {"Resources", "create-jms-resource", "POST", "create-jms-resource", "create-jms-resource"},
        {"Resources", "delete-jms-resource", "POST", "delete-jms-resource", "delete-jms-resource"},
        {"Resources", "flush-connection-pool", "POST", "flush-connection-pool", "Flush Connection Pool"},
        {"Resources", "ping-connection-pool", "GET", "ping-connection-pool", "Ping Connection Pool"},
        {"Resources", "update-connector-security-map", "POST", "update-connector-security-map", "update-connector-security-map"},
        {"Resources", "list-admin-objects", "GET", "list-admin-objects", "List Admin Objects"},
        {"Resources", "list-connector-connection-pools", "GET", "list-connector-connection-pools", "List Connector Connection Pools"},
        {"Resources", "list-connector-resources", "GET", "list-connector-resources", "List Connector Resources"},
        {"Resources", "list-custom-resources", "GET", "list-custom-resources", "List Custom Resources"},
        {"Resources", "list-javamail-resources", "GET", "list-javamail-resources", "List JavaMail Resources"},
        {"Resources", "list-jdbc-connection-pools", "GET", "list-jdbc-connection-pools", "List JDBC Connection Pools"},
        {"Resources", "list-jdbc-resources", "GET", "list-jdbc-resources", "List JDBC Resources"},
        {"Resources", "list-jndi-entries", "GET", "list-jndi-entries", "list-jndi-entries"},
        {"Resources", "list-jndi-resources", "GET", "list-jndi-resources", "List JNDI Resources"},
        {"Resources", "list-resource-adapter-configs", "GET", "list-resource-adapter-configs", "list-resource-adapter-configs"},
        {"Resources", "list-resource-refs", "GET", "list-resource-refs", "list-resource-refs"},
        {"Resources", "list-jms-resources", "GET", "list-jms-resources", "list-jms-resources"},
        {"SecurityService", "list-supported-cipher-suites", "GET", "list-supported-cipher-suites", "List Supported Cipher Suites"},
        {"SecurityService", "list-audit-modules", "GET", "list-audit-modules", "list-audit-modules"},
        {"SecurityService", "list-auth-realms", "GET", "list-auth-realms", "list-auth-realms"},
        {"SecurityService", "list-connector-security-maps", "GET", "list-connector-security-maps", "list-connector-security-maps"},
        {"SecurityService", "list-connector-work-security-maps", "GET", "list-connector-work-security-maps", "list-connector-work-security-maps"},
        {"SecurityService", "list-file-groups", "GET", "list-file-groups", "list-file-groups"},
        {"SecurityService", "list-jacc-providers", "GET", "list-jacc-providers", "list-jacc-providers"},
        {"SecurityService", "list-message-security-providers", "GET", "list-message-security-providers", "list-message-security-providers"},
        {"Server", "__get-jmsdest", "GET", "__get-jmsdest", "Get JMS Destination", "target=$parent"},
        {"Server", "__resolve-tokens", "GET", "resolve-tokens", "Resolve Tokens", "target=$parent"},
        {"Server", "__update-jmsdest", "POST", "__update-jmsdest", "Get JMS Destination", "target=$parent"},
        {"Server", "configure-lb-weight", "POST", "configure-lb-weight", "Configure LB Weight", "target=$parent"},
        {"Server", "create-http-health-checker", "POST", "create-http-health-checker", "create-http-health-checker", "target=$parent"},
        {"Server", "create-http-redirect", "POST", "create-http-redirect", "create-http-redirect"},
        {"Server", "create-jmsdest", "POST", "create-jmsdest", "Create JMS Destination", "target=$parent"},
        {"Server", "create-lifecycle-module", "POST", "create-lifecycle-module", "Create Lifecycle Module", "target=$parent"},
        {"Server", "delete-http-health-checker", "DELETE", "delete-http-health-checker", "delete-http-health-checker", "target=$parent"},
        {"Server", "delete-http-redirect", "DELETE", "delete-http-redirect", "delete-http-redirect"}, {"Server", "enable-http-lb-server", "POST", "enable-http-lb-server", "enable-http-lb-server", "id=$parent"},
        {"Server", "delete-instance", "DELETE", "delete-instance", "Delete Instance", "id=$parent"},
        {"Server", "delete-jmsdest", "DELETE", "delete-jmsdest", "Delete JMS Destination", "target=$parent"},
        {"Server", "delete-lifecycle-module", "DELETE", "delete-lifecycle-module", "Delete Lifecycle Module", "target=$parent"},
        {"Server", "disable-http-lb-server", "POST", "disable-http-lb-server", "disable-http-lb-server", "id=$parent"},
        {"Server", "flush-jmsdest", "POST", "flush-jmsdest", "Flush", "target=$parent"},
        {"Server", "generate-jvm-report", "GET", "generate-jvm-report", "Generate Report", "target=$parent"},
        {"Server", "jms-ping", "GET", "jms-ping", "Ping JMS", "id=$parent"},
        {"Server", "list-jmsdest", "GET", "list-jmsdest", "List JMS Destinations", "id=$parent"},
        {"Server", "list-lifecycle-modules", "GET", "list-lifecycle-modules", "List Lifecycle Modules", "id=$parent"},
        {"Server", "recover-transactions", "POST", "recover-transactions", "Recover", "id=$parent"},
        {"Server", "restart-instance", "POST", "restart-instance", "Restart Instance", "id=$parent"},
        {"Server", "start-instance", "POST", "start-instance", "Start Instance", "id=$parent"},
        {"Server", "stop-instance", "POST", "stop-instance", "Stop Instance", "id=$parent"},
        {"ThreadPools", "list-threadpools", "GET", "list-threadpools", "list-threadpools"},
        {"TransactionService", "freeze-transaction-service", "POST", "freeze-transaction-service", "freeze-transaction-service"},
        {"TransactionService", "unfreeze-transaction-service", "POST", "unfreeze-transaction-service", "unfreeze-transaction-service"},
        {"WorkSecurityMap", "update-connector-work-security-map", "POST", "update-connector-work-security-map", "Update", "id=$parent"}
    };
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
