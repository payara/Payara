/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.util.HostAndPort;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.StringUtils;
import java.util.Collection;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

@Service(name="_get-host-and-port")
@org.glassfish.api.admin.ExecuteOn(value={RuntimeType.DAS})
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="host-port", 
        description="HostPort")
})
public class GetHostAndPortCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    @Param(optional=true)
    public String target = "server";

    @Param(optional=true)
    public String virtualServer = null;

    @Param(optional=true, defaultValue="false")
    public Boolean securityEnabled = false;

    @Param(optional=true)
    public String moduleId = null;

    @Inject
    Configs configs;

    @Inject 
    Domain domain;
    
    private Config config;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(GetHostAndPortCommand.class);    

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        String configName = null;
        Server server = domain.getServerNamed(target);
        if (server != null) {
            configName = server.getConfigRef();
        } else {
            Cluster cluster = domain.getClusterNamed(target);
            if (cluster != null) {
                configName = cluster.getConfigRef();
            }
        }
        config = configs.getConfigByName(configName);
        if (config != null) {
            accessChecks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(config), "read"));
        }
        return accessChecks;
    }

    
    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();

        ActionReport.MessagePart part = report.getTopMessagePart();

        HttpService httpService = null;
        HostAndPort hostAndPort = null;

        try {
            if (config == null) {
                throw new Exception("No such target:" + target);   
            }
            httpService = config.getHttpService();

            if (httpService != null) {
                hostAndPort = getHostAndPortForRequest(httpService);
            }
        } catch (Exception e) {
            report.setMessage(e.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (hostAndPort != null) {
            part.setMessage(hostAndPort.getHost() + ":" + 
                hostAndPort.getPort());
            part.addProperty("host", hostAndPort.getHost()); //property for REST Access
            part.addProperty("port", ""+hostAndPort.getPort()); //property for REST Access

        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private HostAndPort getHostAndPortForRequest(HttpService httpService) 
        throws Exception {
        if (moduleId == null) {
            if (virtualServer == null) {
                return getHostAndPort(httpService, securityEnabled);
            } else {
                VirtualServer vs = httpService.getVirtualServerByName(
                    virtualServer);
                if (vs == null) {
                    throw new Exception("Virtual server: " + 
                        virtualServer + " does not exist!");
                }
                return getHostAndPort(httpService, vs, securityEnabled);
            }
        }

        ApplicationRef appRef = domain.getApplicationRefInTarget(
            moduleId, target); 

        List<String> vsList = null;
        if (appRef != null) {
            vsList = StringUtils.parseStringList(appRef.getVirtualServers(), " ,");
        }

        if (vsList==null) {
            return getHostAndPort(httpService, securityEnabled);
        }

        for (String virtualServer : vsList) {
            HostAndPort hp = getHostAndPort(httpService,
                httpService.getVirtualServerByName(virtualServer), 
                securityEnabled);
            if (hp!=null) {
                return hp;
            }
        }
        return null;
    }

    private HostAndPort getHostAndPort(HttpService httpService, VirtualServer vs, boolean securityEnabled) {
        List<VirtualServer> virtualServerList =
            httpService.getVirtualServer();
        List<NetworkListener> httpListenerList =
            httpService.getParent(Config.class).getNetworkConfig().getNetworkListeners().getNetworkListener();

        for (VirtualServer virtualServer : virtualServerList) {
            if (!virtualServer.getId().equals(vs.getId())) {
                continue;
            }
            String vsHttpListeners = virtualServer.getNetworkListeners();
            if (vsHttpListeners == null) {
                continue;
            }
            List<String> vsHttpListenerList =
                StringUtils.parseStringList(vsHttpListeners, " ,");

            for (String vsHttpListener : vsHttpListenerList) {
                for (NetworkListener httpListener : httpListenerList) {
                    if (!httpListener.getName().equals(vsHttpListener)) {
                        continue;
                    }
                    if (!Boolean.valueOf(httpListener.getEnabled())) {
                        continue;
                    }
                    final Protocol protocol = httpListener.findHttpProtocol();
                    if (Boolean.valueOf(protocol.getSecurityEnabled())
                        == securityEnabled) {
                        String serverName = protocol.getHttp().getServerName();
                        if (serverName == null ||
                            serverName.trim().equals("")) {
                            serverName = DeploymentCommandUtils.getLocalHostName();
                        }
                        String portStr = httpListener.getPort();
                        String redirPort = protocol.getHttp().getRedirectPort();
                        if (redirPort != null &&
                            !redirPort.trim().equals("")) {
                            portStr = redirPort;
                        }
                        int port = Integer.parseInt(portStr);
                        return new HostAndPort(
                            serverName, port, securityEnabled);
                    }
                }
            }
        }
        return null;
    }

    private HostAndPort getHostAndPort(HttpService httpService, boolean securityEnabled) {
        List<NetworkListener> httpListenerList =
            httpService.getParent(Config.class).getNetworkConfig().getNetworkListeners().getNetworkListener();

        for (NetworkListener httpListener : httpListenerList) {
            if (!Boolean.valueOf(httpListener.getEnabled())) {
                continue;
            }
            final Protocol protocol = httpListener.findHttpProtocol();
            final Http http = protocol.getHttp();
            if (http.getDefaultVirtualServer().equals("__asadmin")){
                continue;
            }
            if (Boolean.valueOf(protocol.getSecurityEnabled()) ==
                securityEnabled) {

                String serverName = http.getServerName();
                if (serverName == null ||
                    serverName.trim().equals("")) {
                    serverName = DeploymentCommandUtils.getLocalHostName();
                }
                String portStr = httpListener.getPort();
                String redirPort = http.getRedirectPort();
                if (redirPort != null &&
                    !redirPort.trim().equals("")) {
                    portStr = redirPort;
                }
                int port = Integer.parseInt(portStr);
                return new HostAndPort(
                    serverName, port, securityEnabled);
            }
        }
        return null;
    }
}
