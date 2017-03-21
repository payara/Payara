/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.ProtocolFinder;
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
import org.glassfish.internal.api.Target;
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
 * Command to create Network Listener
 */
@Service(name = "create-network-listener")
@PerLookup
@I18n("create.network.listener")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})  
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class CreateNetworkListener implements AdminCommand {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    @Param(name = "address", optional = true)
    String address;
    @Param(name = "listenerport", optional = false, alias="Port")
    String port;
    @Param(name = "threadpool", optional = true, defaultValue = "http-thread-pool", alias="threadPool")
    String threadPool;
    @Param(name = "protocol", optional = false)
    String protocol;
    @Param(name = "name", primary = true)
    String listenerName;
    @Param(name = "transport", optional = true, defaultValue = "tcp")
    String transport;
    @Param(name = "enabled", optional = true, defaultValue = "true")
    Boolean enabled;
    @Param(name = "jkenabled", optional = true, defaultValue = "false", alias = "jkEnabled")
    Boolean jkEnabled;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;
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
        final ActionReport report = context.getActionReport();
        NetworkConfig networkConfig = config.getNetworkConfig();
        NetworkListeners nls = networkConfig.getNetworkListeners();
        // ensure we don't have one of this name already
        for (NetworkListener networkListener : nls.getNetworkListener()) {
            if (networkListener.getName().equals(listenerName)) {
                report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_NETWORK_LISTENER_FAIL_DUPLICATE), listenerName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        if (!verifyUniquePort(networkConfig)) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.PORT_IN_USE), port, address));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        Protocol prot = networkConfig.findProtocol(protocol);
        if (prot == null) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_HTTP_FAIL_PROTOCOL_NOT_FOUND), protocol));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        if (prot.getHttp() == null && prot.getPortUnification() == null) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_NETWORK_LISTENER_FAIL_BAD_PROTOCOL), protocol));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        try {
            ConfigSupport.apply(new ConfigCode() {
                public Object run(ConfigBeanProxy... params) throws TransactionFailure, PropertyVetoException {
                    NetworkListeners listeners = (NetworkListeners) params[0];
                    NetworkListener newNetworkListener = listeners.createChild(NetworkListener.class);
                    newNetworkListener.setProtocol(protocol);
                    newNetworkListener.setTransport(transport);
                    newNetworkListener.setEnabled(enabled.toString());
                    newNetworkListener.setJkEnabled(jkEnabled.toString());
                    newNetworkListener.setPort(port);
                    newNetworkListener.setThreadPool(threadPool);
                    newNetworkListener.setName(listenerName);
                    newNetworkListener.setAddress(address);
                    listeners.getNetworkListener().add(newNetworkListener);
                    ((VirtualServer) params[1]).addNetworkListener(listenerName);
                    return newNetworkListener;
                }
            }, nls, findVirtualServer(prot));
        } catch (TransactionFailure e) {
            e.printStackTrace();
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_NETWORK_LISTENER_FAIL), listenerName) +
                    (e.getMessage() == null ? "No reason given" : e.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private VirtualServer findVirtualServer(Protocol protocol) {
        String name = null;
        final Http http = protocol.getHttp();
        if (http != null) {
            name = http.getDefaultVirtualServer();
        } else {
            final List<ProtocolFinder> finders = protocol.getPortUnification().getProtocolFinder();
            for (ProtocolFinder finder : finders) {
                if (name == null) {
                    final Protocol p = finder.findProtocol();
                    if (p.getHttp() != null) {
                        name = p.getHttp().getDefaultVirtualServer();
                    }
                }
            }
        }

        return config.getHttpService().getVirtualServerByName(name);
    }

    private boolean verifyUniquePort(NetworkConfig networkConfig) {
        //check port uniqueness, only for same address
        for (NetworkListener listener : networkConfig.getNetworkListeners()
            .getNetworkListener()) {
            if (listener.getPort().trim().equals(port) &&
                listener.getAddress().trim().equals(address)) {
                return false;
            }
        }
        return true;
    }
}
