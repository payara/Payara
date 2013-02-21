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
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transport;
import org.glassfish.internal.api.Target;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.admin.monitor.HttpServiceStatsProviderBootstrap;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transactions;

/**
 * Create Http Listener Command
 */
@Service(name = "create-http-listener")
@PerLookup
@I18n("create.http.listener")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})  
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class CreateHttpListener implements AdminCommand {

    @Param(name = "listeneraddress")
    String listenerAddress;
    @Param(name = "listenerport")
    String listenerPort;
    @Param(name = "defaultvs", optional = true)
    String defaultVS;
    @Param(name = "default-virtual-server", optional = true)
    String defaultVirtualServer;
    @Param(name = "servername", optional = true)
    String serverName;
    @Param(name = "xpowered", optional = true, defaultValue = "true")
    Boolean xPoweredBy;
    @Param(name = "acceptorthreads", optional = true)
    String acceptorThreads;
    @Param(name = "redirectport", optional = true)
    String redirectPort;
    @Param(name = "securityenabled", optional = true, defaultValue = "false")
    Boolean securityEnabled;
    @Param(optional = true, defaultValue = "true")
    Boolean enabled;
    @Param(optional = true, defaultValue = "false")
    Boolean secure; //FIXME
    @Param(name = "listener_id", primary = true)
    String listenerId;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    ServiceLocator services;
    @Inject
    CommandRunner runner;
    @Inject
    Domain domain;
    private static final String DEFAULT_TRANSPORT = "tcp";
    private NetworkConfig networkConfig = null;

    private static final Logger logger = HttpServiceStatsProviderBootstrap.logger;

    private static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "The acceptor threads must be at least 1",
            level = "INFO")
    private static final String ACCEPTOR_THREADS_TOO_LOW = "AS-WEB-ADMIN-00003";

    @LogMessageInfo(
            message = "Listener {0} could not be created, actual reason: {1}",
            level = "INFO")
    private static final String CREATE_HTTP_LISTENER_FAIL = "AS-WEB-ADMIN-00004";

    @LogMessageInfo(
            message = "A default virtual server is required.  Please use --default-virtual-server to specify this value.",
            level = "INFO")
    private static final String CREATE_HTTP_LISTENER_VS_BLANK = "AS-WEB-ADMIN-00005";

    @LogMessageInfo(
            message = "--defaultVS and --default-virtual-server conflict.  Please use only --default-virtual-server to specify this value.",
            level = "INFO")
    private static final String CREATE_HTTP_LISTENER_VS_BOTH_PARAMS = "AS-WEB-ADMIN-00006";

    @LogMessageInfo(
            message = "Attribute value (default-virtual-server = {0}) is not found in list of virtual servers defined in config.",
            level = "INFO")
    private static final String CREATE_HTTP_LISTENER_VS_NOTEXISTS = "AS-WEB-ADMIN-00007";

    @LogMessageInfo(
            message = "Http Listener named {0} already exists.",
            level = "INFO")
    private static final String CREATE_HTTP_LISTENER_DUPLICATE = "AS-WEB-ADMIN-00008";

    @LogMessageInfo(
            message = "Port [{0}] is already taken for address [{1}], please choose another port.",
            level = "INFO")
    protected static final String PORT_IN_USE = "AS-WEB-ADMIN-00009";

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and
     * the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        if(!validateInputs(report)) {
            return;
        }
        Target targetUtil = services.getService(Target.class);
        Config newConfig = targetUtil.getConfig(target);
        if (newConfig!=null) {
            config = newConfig;
        }
        networkConfig = config.getNetworkConfig();
        HttpService httpService = config.getHttpService();
        if (!(verifyUniqueName(report, networkConfig) && verifyUniquePort(report, networkConfig)
            && verifyDefaultVirtualServer(report))) {
            return;
        }
        VirtualServer vs = httpService.getVirtualServerByName(defaultVirtualServer);
        boolean listener = false;
        boolean protocol = false;
        boolean transport = false;
        try {
            transport = createOrGetTransport(null);
            protocol = createProtocol(context);
            createHttp(context);
            final ThreadPool threadPool = getThreadPool(networkConfig);
            listener = createNetworkListener(networkConfig, transport, threadPool);
            updateVirtualServer(vs);
        } catch (TransactionFailure e) {
            try {
                if (listener) {
                    deleteListener(context);
                }
                if (protocol) {
                    deleteProtocol(context);
                }
                if (transport) {
                    deleteTransport(context);
                }
            } catch (Exception e1) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, e.getMessage(), e);
                }
                throw new RuntimeException(e.getMessage());
            }
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, e.getMessage(), e);
            }
            report.setMessage(MessageFormat.format(rb.getString(CREATE_HTTP_LISTENER_FAIL), listenerId, e.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private void updateVirtualServer(VirtualServer vs) throws TransactionFailure {
        //now change the associated virtual server
        ConfigSupport.apply(new SingleConfigCode<VirtualServer>() {
            public Object run(VirtualServer avs) throws PropertyVetoException {
                String DELIM = ",";
                String lss = avs.getNetworkListeners();
                boolean listenerShouldBeAdded = true;
                if (lss == null || lss.length() == 0) {
                    lss = listenerId; //the only listener in the list
                } else if (!lss.contains(listenerId)) { //listener does not already exist
                    if (!lss.endsWith(DELIM)) {
                        lss += DELIM;
                    }
                    lss += listenerId;
                } else { //listener already exists in the list, do nothing
                    listenerShouldBeAdded = false;
                }
                if (listenerShouldBeAdded) {
                    avs.setNetworkListeners(lss);
                }
                return avs;
            }
        }, vs);
    }

    private boolean createNetworkListener(NetworkConfig networkConfig, final boolean newTransport,
        final ThreadPool threadPool) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<NetworkListeners>() {
            public Object run(NetworkListeners listenersParam)
                throws TransactionFailure {
                final NetworkListener newListener = listenersParam.createChild(NetworkListener.class);
                newListener.setName(listenerId);
                newListener.setAddress(listenerAddress);
                newListener.setPort(listenerPort);
                newListener.setTransport(newTransport ? listenerId : DEFAULT_TRANSPORT);
                newListener.setProtocol(listenerId);
                newListener.setThreadPool(threadPool.getName());
                newListener.setEnabled(enabled.toString());
                listenersParam.getNetworkListener().add(newListener);
                return newListener;
            }
        }, networkConfig.getNetworkListeners());
        services.<Transactions>getService(Transactions.class).waitForDrain();
        return true;
    }

    private boolean verifyDefaultVirtualServer(ActionReport report) {
        if (defaultVS == null && defaultVirtualServer == null) {
            report.setMessage(rb.getString(CREATE_HTTP_LISTENER_VS_BLANK));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        if (defaultVS != null && defaultVirtualServer != null && !defaultVS.equals(defaultVirtualServer)) {
            report.setMessage(rb.getString(CREATE_HTTP_LISTENER_VS_BOTH_PARAMS));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        } else if (defaultVirtualServer == null && defaultVS != null) {
            defaultVirtualServer = defaultVS;
        }
        //no need to check the other things (e.g. id) for uniqueness
        // ensure that the specified default virtual server exists
        if (!defaultVirtualServerExists()) {
            report.setMessage(MessageFormat.format(rb.getString(CREATE_HTTP_LISTENER_VS_NOTEXISTS), defaultVirtualServer));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        return true;
    }

    private boolean verifyUniquePort(ActionReport report, NetworkConfig networkConfig) {
        //check port uniqueness, only for same address
        for (NetworkListener listener : networkConfig.getNetworkListeners()
            .getNetworkListener()) {
            if (listener.getPort().trim().equals(listenerPort) &&
                listener.getAddress().trim().equals(listenerAddress)) {
                String def = "Port is already taken by another listener, choose another port.";
                String msg = MessageFormat.format(rb.getString(PORT_IN_USE), listenerPort, listenerAddress);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return false;
            }
        }
        return true;
    }

    private boolean validateInputs(final ActionReport report) {
        if(acceptorThreads != null && Integer.parseInt(acceptorThreads) < 1) {
            report.setMessage(MessageFormat.format(rb.getString(ACCEPTOR_THREADS_TOO_LOW), listenerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        return true;
    }
    private boolean verifyUniqueName(ActionReport report, NetworkConfig networkConfig) {
        // ensure we don't already have one of this name
        for (NetworkListener listener : networkConfig.getNetworkListeners().getNetworkListener()) {
            if (listener.getName().equals(listenerId)) {
                report.setMessage(MessageFormat.format(rb.getString(CREATE_HTTP_LISTENER_DUPLICATE), listenerId));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return false;
            }
        }
        return true;
    }

    private ThreadPool getThreadPool(NetworkConfig config) {
        final List<ThreadPool> pools = config.getParent(Config.class).getThreadPools().getThreadPool();
        ThreadPool target = null;
        for (ThreadPool pool : pools) {
            if ("http-thread-pool".equals(pool.getName())) {
                target = pool;
            }
        }
        if (target == null && !pools.isEmpty()) {
            target = pools.get(0);
        }
        return target;
    }

    private boolean createOrGetTransport(final AdminCommandContext context) throws TransactionFailure {
        boolean newTransport = false;
        for (Transport t : networkConfig.getTransports().getTransport()) {
            if (!t.getName().equals(DEFAULT_TRANSPORT)) {
                 newTransport = true;
            }
        }
        if (newTransport) {
            final CreateTransport command = (CreateTransport) runner
                .getCommand("create-transport", context.getActionReport(), context.getLogger());
            command.transportName = listenerId;
            command.acceptorThreads = acceptorThreads;
            command.target = target;
            command.execute(context);
            checkProgress(context);
            newTransport = true;
        }
        return newTransport;
    }

    private boolean createProtocol(final AdminCommandContext context) throws TransactionFailure {
        final CreateProtocol command = (CreateProtocol) runner
            .getCommand("create-protocol", context.getActionReport(), context.getLogger());
        command.protocolName = listenerId;
        command.securityEnabled = securityEnabled;
        command.target = target;
        command.execute(context);
        checkProgress(context);
        return true;
    }

    private boolean createHttp(final AdminCommandContext context) throws TransactionFailure {
        final CreateHttp command = (CreateHttp) runner
            .getCommand("create-http", context.getActionReport(), context.getLogger());
        command.protocolName = listenerId;
        command.defaultVirtualServer = defaultVirtualServer;
        command.xPoweredBy = xPoweredBy;
        command.serverName = serverName;
        command.target = target;
        command.execute(context);
        checkProgress(context);
        return true;
    }

    private void checkProgress(final AdminCommandContext context) throws TransactionFailure {
        if(context.getActionReport().getActionExitCode() != ExitCode.SUCCESS) {
            throw new TransactionFailure(context.getActionReport().getMessage());
        }
    }

    private boolean deleteProtocol(final AdminCommandContext context) {
        final DeleteProtocol command = (DeleteProtocol) runner
            .getCommand("delete-protocol", context.getActionReport(), context.getLogger());
        command.protocolName = listenerId;
        command.target = target;
        command.execute(context);
        return true;
    }

    private boolean deleteTransport(final AdminCommandContext context) {
        final DeleteTransport command = (DeleteTransport) runner
            .getCommand("delete-transport", context.getActionReport(), context.getLogger());
        command.transportName = listenerId;
        command.target = target;
        command.execute(context);
        return true;
    }

    private boolean deleteListener(final AdminCommandContext context) {
        final DeleteNetworkListener command = (DeleteNetworkListener) runner
            .getCommand("delete-network-listener", context.getActionReport(), context.getLogger());
        command.networkListenerName = listenerId;
        command.target = target;
        command.execute(context);
        return true;
    }

    private boolean defaultVirtualServerExists() {
        return (defaultVirtualServer != null) && (
                config.getHttpService().getVirtualServerByName(defaultVirtualServer) != null);
    }
}
