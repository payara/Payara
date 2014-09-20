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
import java.util.ResourceBundle;

import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.admin.monitor.HttpServiceStatsProviderBootstrap;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Delete Network Listener command
 */
@Service(name = "delete-network-listener")
@PerLookup
@I18n("delete.network.listener")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteNetworkListener implements AdminCommand {

    private static final ResourceBundle rb = HttpServiceStatsProviderBootstrap.rb;

    @LogMessageInfo(
            message = "{0} Network Listener doesn't exist.",
            level = "INFO")
    protected static final String DELETE_NETWORK_LISTENER_NOT_EXISTS = "AS-WEB-ADMIN-00030";

    @LogMessageInfo(
            message = "Deletion of NetworkListener {0} failed.",
            level = "INFO")
    protected static final String DELETE_NETWORK_LISTENER_FAIL = "AS-WEB-ADMIN-00031";

    @Param(name = "networkListenerName", primary = true)
    String networkListenerName;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    NetworkListener listenerToBeRemoved = null;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    ServiceLocator services;
    @Inject
    Domain domain;

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
        NetworkListeners networkListeners = config.getNetworkConfig().getNetworkListeners();
        try {
            if (findListener(networkListeners, report)) {
                final Protocol httpProtocol = listenerToBeRemoved.findHttpProtocol();
                final VirtualServer virtualServer = config.getHttpService().getVirtualServerByName(
                        httpProtocol.getHttp().getDefaultVirtualServer());

                ConfigSupport.apply(new ConfigCode() {
                    public Object run(ConfigBeanProxy... params) throws PropertyVetoException {
                        final NetworkListeners listeners = (NetworkListeners) params[0];
                        final VirtualServer server = (VirtualServer) params[1];
                        listeners.getNetworkListener().remove(listenerToBeRemoved);
                        server.removeNetworkListener(listenerToBeRemoved.getName());
                        return listenerToBeRemoved;
                    }
                }, networkListeners, virtualServer);

            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(DELETE_NETWORK_LISTENER_FAIL), networkListenerName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private boolean findListener(final NetworkListeners networkListeners, ActionReport report) {
        for (NetworkListener listener : networkListeners.getNetworkListener()) {
            if(listener.getName().equals(networkListenerName)) {
                listenerToBeRemoved = listener;
            }
        }
        if (listenerToBeRemoved == null) {
            report.setMessage(MessageFormat.format(rb.getString(DELETE_NETWORK_LISTENER_NOT_EXISTS), networkListenerName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        return true;
    }

}
