/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.Param;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.deployment.common.DeploymentUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.config.support.PropertyResolver;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestEndpoint;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.util.Collection;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;

@Service(name="_get-application-launch-urls")
@ExecuteOn(value={RuntimeType.DAS})
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Applications.class, opType= RestEndpoint.OpType.GET, path="_get-application-launch-urls", description="Get Urls for launch the application")
})
public class GetApplicationLaunchURLsCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    @Param(primary=true)
    private String appname = null;

    @Inject
    Domain domain;

    @Inject
    CommandRunner commandRunner;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(GetApplicationLaunchURLsCommand.class);    

    private List<Server> servers;
    
    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        List<String> targets = domain.getAllReferencedTargetsForApplication(appname);
        for (String target : targets) {
            if (domain.isAppEnabledInTarget(appname, target)) {
                servers = new ArrayList<Server>();
                Cluster cluster = domain.getClusterNamed(target);
                if (cluster != null) {
                    servers = cluster.getInstances();
                }
                Server server = domain.getServerNamed(target);
                if (server != null) {
                    servers.add(server);
                }
                for (Server svr : servers) {
                    accessChecks.add(new AccessCheck(DeploymentCommandUtils.getTargetResourceNameForExistingAppRef(domain, svr.getName(), appname), "read"));
                }
            }
        }
        return accessChecks;
    }

    
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger = context.getLogger();
        getLBLaunchURLInformation(appname, report, context.getSubject());
        List<URL> launchURLs = getLaunchURLInformation(appname, logger);
        if(launchURLs == null || launchURLs.isEmpty()){
            return;
        }
        ActionReport.MessagePart part = report.getTopMessagePart();
        //Adding a new part for adding instance(s) urls
        part = part.addChild();
        part.setMessage("Instances");
        int j = 0;
        for (URL url : launchURLs) {
            ActionReport.MessagePart childPart = part.addChild();
            childPart.setMessage(Integer.toString(j++));
            childPart.addProperty(DeploymentProperties.PROTOCOL,
                url.getProtocol());
            childPart.addProperty(DeploymentProperties.HOST,
                url.getHost());
            childPart.addProperty(DeploymentProperties.PORT,
                String.valueOf(url.getPort()));
            childPart.addProperty(DeploymentProperties.CONTEXT_PATH,
                url.getPath());
        }
    }

    private void getLBLaunchURLInformation(String appName, ActionReport report, final Subject subject){
        CommandRunner.CommandInvocation invocation =
                commandRunner.getCommandInvocation("_get-lb-launch-urls", report, subject);
        if(invocation != null){
            ParameterMap map = new ParameterMap();
            map.add("appname", appName);
            invocation.parameters(map).execute();
        }
    }

    private List<URL> getLaunchURLInformation(String appName, Logger logger) {
        List<URL> launchURLs = new ArrayList<URL>();
        String contextRoot = getContextRoot(appName);

        for (Server svr : servers) {
            launchURLs.addAll(getURLsForServer(svr, appName, contextRoot, logger));
        }
        return launchURLs;
    }

    private String getContextRoot(String appName) {
        Application application = domain.getApplications().getApplication(appName);
        if(application == null){
            return "";
        }
        String contextRoot = application.getContextRoot();
        // non standalone war cases
        if (contextRoot == null) {
            contextRoot = "";
        }
        return contextRoot;
    }

    private List<URL> getURLsForServer(Server server, String appName, String contextRoot, Logger logger) {
        List<URL> serverURLs = new ArrayList<URL>();

        String virtualServers = server.getApplicationRef(appName).getVirtualServers();
        if (virtualServers == null || virtualServers.trim().equals("")) {
            return serverURLs;
        }

        String nodeName = server.getNodeRef();
        String host = null;
        if (nodeName != null) {
            Node node = domain.getNodeNamed(nodeName);
            host = node.getNodeHost();
        }
        if (host == null || host.trim().equals("") || host.trim().equalsIgnoreCase("localhost")) {
            host = DeploymentCommandUtils.getLocalHostName();
        }

        List<String> vsList = StringUtils.parseStringList(virtualServers, " ,");
        Config config =  domain.getConfigNamed(server.getConfigRef());
        HttpService httpService = config.getHttpService();
        for (String vsName : vsList) {
            VirtualServer vs = httpService.getVirtualServerByName(vsName);
            String vsHttpListeners = vs.getNetworkListeners();
            if (vsHttpListeners == null || vsHttpListeners.trim().equals("")) {
                continue;
            }
            List<String> vsHttpListenerList =
                StringUtils.parseStringList(vsHttpListeners, " ,");
            List<NetworkListener> httpListenerList = config.getNetworkConfig().getNetworkListeners().getNetworkListener();

            for (String vsHttpListener : vsHttpListenerList) {
                for (NetworkListener httpListener : httpListenerList) {
                    if (!httpListener.getName().equals(vsHttpListener)) {
                        continue;
                    }
                    if (!Boolean.valueOf(httpListener.getEnabled())) {
                        continue;
                    }
                    Protocol protocol = httpListener.findHttpProtocol();
                    //Do not include jk enabled listeners
                    if(Boolean.valueOf(protocol.getHttp().getJkEnabled())){
                        continue;
                    }
                    boolean securityEnabled = Boolean.valueOf(protocol.getSecurityEnabled());
                    String proto = (securityEnabled ? "https" : "http");
                    String portStr = httpListener.getPort();
                    String redirPort = protocol.getHttp().getRedirectPort();
                    if (redirPort != null && !redirPort.trim().equals("")) {
                        portStr = redirPort;
                    }
                    // we need to resolve port for non-DAS instances
                    if (!DeploymentUtils.isDASTarget(server.getName())) {
                        PropertyResolver resolver = new PropertyResolver(domain, server.getName());
                        portStr = resolver.getPropertyValue(portStr);
                    }
                    try {
                        int port = Integer.parseInt(portStr);
                        URL url = new URL(proto, host, port, contextRoot);
                        serverURLs.add(url);
                    } catch (Exception ee) {
                        logger.log(Level.WARNING, ee.getMessage(), ee);
                    }
                }
            }
        }
        return serverURLs;
    }
}
