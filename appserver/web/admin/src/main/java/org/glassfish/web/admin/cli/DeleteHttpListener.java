/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
import java.util.regex.Pattern;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Delete http listener command
 */
@Service(name = "delete-http-listener")
@PerLookup
@I18n("delete.http.listener")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteHttpListener implements AdminCommand {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    @Param(name = "listener_id", primary = true)
    String listenerId;
    @Param(name = "secure", optional = true)
    String secure;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    Domain domain;
    @Inject
    ServiceLocator services;
    private NetworkConfig networkConfig;

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and
     * the values the parameter values
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
        networkConfig = config.getNetworkConfig();
        if (!exists()) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.DELETE_HTTP_LISTENER_NOT_EXISTS), listenerId));
            report.setActionExitCode(ExitCode.FAILURE);
            return;
        }
        try {
            NetworkListener ls = networkConfig.getNetworkListener(listenerId);
            final String name = ls.getProtocol();
            VirtualServer vs = config.getHttpService()
                .getVirtualServerByName(ls.findHttpProtocol().getHttp().getDefaultVirtualServer());
            ConfigSupport.apply(new DeleteNetworkListener(), networkConfig.getNetworkListeners());
            ConfigSupport.apply(new UpdateVirtualServer(), vs);
            cleanUp(name);
            report.setActionExitCode(ExitCode.SUCCESS);
        } catch (TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.DELETE_HTTP_LISTENER_FAIL), listenerId));
            report.setActionExitCode(ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private boolean exists() {
        if (networkConfig != null) {
            return networkConfig.getNetworkListener(listenerId) != null;
        } else {
            return false;
        }
    }

    private void cleanUp(String name) throws TransactionFailure {
        boolean found = false;
        if (networkConfig != null) {
            for (NetworkListener candidate : networkConfig.getNetworkListeners().getNetworkListener()) {
                found |= candidate.getProtocol().equals(name);
            }
            if (!found) {
                ConfigSupport.apply(new DeleteProtocol(name), networkConfig.getProtocols());
            }
        }
    }

    private class DeleteNetworkListener implements SingleConfigCode<NetworkListeners> {
        public Object run(NetworkListeners param) throws PropertyVetoException, TransactionFailure {
            final List<NetworkListener> list = param.getNetworkListener();
            for (NetworkListener listener : list) {
                if (listener.getName().equals(listenerId)) {
                    list.remove(listener);
                    break;
                }
            }
            return list;
        }
    }

    private class UpdateVirtualServer implements SingleConfigCode<VirtualServer> {
        public Object run(VirtualServer avs) throws PropertyVetoException {
            String lss = avs.getNetworkListeners();
            if (lss != null && lss.contains(listenerId)) { //change only if needed
                Pattern p = Pattern.compile(",");
                String[] names = p.split(lss);
                List<String> nl = new ArrayList<String>();
                for (String rawName : names) {
                    final String name = rawName.trim();
                    if (!listenerId.equals(name)) {
                        nl.add(name);
                    }
                }
                //we removed the listenerId from lss and is captured in nl by now
                lss = nl.toString();
                lss = lss.substring(1, lss.length() - 1);
                avs.setNetworkListeners(lss);
            }
            return avs;
        }
    }

    private static class DeleteProtocol implements SingleConfigCode<Protocols> {
        private final String name;

        public DeleteProtocol(String name) {
            this.name = name;
        }

        public Object run(Protocols param) throws PropertyVetoException, TransactionFailure {
            List<Protocol> list = new ArrayList<Protocol>(param.getProtocol());
            for (Protocol old : list) {
                if (name.equals(old.getName())) {
                    param.getProtocol().remove(old);
                    break;
                }
            }
            return param;
        }
    }
}
