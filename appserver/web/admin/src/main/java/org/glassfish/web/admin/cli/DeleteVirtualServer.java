/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.admin.cli;

import java.beans.PropertyVetoException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.web.admin.LogFacade;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Delete virtual server command
 * 
 */
@Service(name="delete-virtual-server")
@PerLookup
@I18n("delete.virtual.server")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})  
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteVirtualServer implements AdminCommand {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    
    @Param(name="virtual_server_id", primary=true)
    String vsid;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    Domain domain;

    @Inject
    ServiceLocator services;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Server server;

    private HttpService httpService;
    private NetworkConfig networkConfig;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        Target targetUtil = services.getService(Target.class);
        Config newConfig = targetUtil.getConfig(target);
        if (newConfig!=null) {
            config = newConfig;
        }
        ActionReport report = context.getActionReport();
        httpService = config.getHttpService();
        networkConfig = config.getNetworkConfig();

        if(!exists()) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.DELETE_VIRTUAL_SERVER_NOT_EXISTS), vsid));
            report.setActionExitCode(ExitCode.FAILURE);
            return;
        }

        // reference check
        String referencedBy = getReferencingListener();
        if(referencedBy != null && referencedBy.length() != 0) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.DELETE_VIRTUAL_SERVER_REFERENCED), vsid, referencedBy));
            report.setActionExitCode(ExitCode.FAILURE);
            return;
        }

        try {

            // we need to determine which deployed applications reference this virtual-server
            List<ApplicationRef> appRefs = new ArrayList<ApplicationRef>();
            for (ApplicationRef appRef : server.getApplicationRef()) {
                if (appRef.getVirtualServers()!=null && appRef.getVirtualServers().contains(vsid)) {
                    appRefs.add(appRef);
                }
            }
            // transfer into the array of arguments
            ConfigBeanProxy[] proxies = new ConfigBeanProxy[appRefs.size()+1];
            proxies[0] = httpService;
            for (int i=0;i<appRefs.size();i++) {
                proxies[i+1] = appRefs.get(i);
            }

            ConfigSupport.apply(new ConfigUpdate(vsid), proxies);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        } catch(TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.DELETE_VIRTUAL_SERVER_FAIL), vsid));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
    
    private boolean exists() {
        if ((vsid == null) || (httpService == null))
            return false;
        
        List<VirtualServer> list = httpService.getVirtualServer();
        
        for(VirtualServer vs : list) {
            String currId = vs.getId();
         
            if(currId != null && currId.equals(vsid))
                return true;
        }
        return false;
    }

    private String getReferencingListener() {
        if (networkConfig != null) {
            List<NetworkListener> list = networkConfig.getNetworkListeners().getNetworkListener();
        
            for(NetworkListener listener: list) {
                String virtualServer = listener.findHttpProtocol().getHttp().getDefaultVirtualServer();
         
                if(virtualServer != null && virtualServer.equals(vsid)) {
                    return listener.getName();
                }
            }
        }
        return null;
    }

    private static class ConfigUpdate implements ConfigCode {
        private ConfigUpdate(String vsid) {
            this.vsid = vsid;
        }
        public Object run(ConfigBeanProxy... proxies) throws PropertyVetoException, TransactionFailure {
            List<VirtualServer> list = ((HttpService) proxies[0]).getVirtualServer();
            for(VirtualServer item : list) {
                String currId = item.getId();
                if (currId != null && currId.equals(vsid)) {
                    list.remove(item);
                    break;
                }
            }
            // we now need to remove the virtual server id from all application-ref passed.
            if (proxies.length>1) {
                // we have some appRefs to clean.
                for (int i=1;i<proxies.length;i++) {
                    ApplicationRef appRef = (ApplicationRef) proxies[i];
                    StringBuilder newList = new StringBuilder();
                    StringTokenizer st = new StringTokenizer(appRef.getVirtualServers(), ",");
                    while (st.hasMoreTokens()) {
                        final String id = st.nextToken();
                        if (!id.equals(vsid)) {
                            if (newList.length()>0) {
                                newList.append(",");
                            }
                            newList.append(id);
                        }
                    }
                    appRef.setVirtualServers(newList.toString());
                }
            }
            return list;
        }
        private String vsid;
    }
}
