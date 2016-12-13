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
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.glassfish.api.admin.config.ModelBinding;
import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.web.admin.LogFacade;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

/**
 * Command to create virtual server
 */
@Service(name = "create-virtual-server")
@PerLookup
@I18n("create.virtual.server")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class CreateVirtualServer implements AdminCommand {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    @Param(name = "hosts", defaultValue = "${com.sun.aas.hostName}")
    String hosts;
    @Param(name = "httplisteners", optional = true)
    String httpListeners;
    @Param(name = "networklisteners", optional = true)
    String networkListeners;
    @Param(name = "defaultwebmodule", optional = true)
    String defaultWebModule;
    @Param(name = "state", defaultValue = "on", acceptableValues = "on, off, disabled", optional = true)
    String state;
    @Param(name = "logfile", optional = true)
    @ModelBinding(type=VirtualServer.class, getterMethodName ="getLogFile")
    String logFile;
    @Param(name = "property", optional = true, separator = ':')
    Properties properties;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    @Param(name = "virtual_server_id", primary = true)
    String virtualServerId;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    Domain domain;
    @Inject
    ServiceLocator services;

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
        if (networkListeners != null && httpListeners != null) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_VIRTUAL_SERVER_BOTH_HTTP_NETWORK), virtualServerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        //use the listener parameter provided by the user.
        if (networkListeners == null) {
            networkListeners = httpListeners;
        }
        HttpService httpService = config.getHttpService();
        // ensure we don't already have one of this name
        for (VirtualServer virtualServer : httpService.getVirtualServer()) {
            if (virtualServer.getId().equals(virtualServerId)) {
                report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_VIRTUAL_SERVER_DUPLICATE), virtualServerId));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<HttpService>() {
                public Object run(HttpService param) throws PropertyVetoException, TransactionFailure {
                    String docroot = "${com.sun.aas.instanceRoot}/docroot";    // default
                    String accessLog = "${com.sun.aas.instanceRoot}/logs/access";    // default

                    VirtualServer newVirtualServer = param.createChild(VirtualServer.class);
                    newVirtualServer.setId(virtualServerId);
                    newVirtualServer.setHosts(hosts);
                    newVirtualServer.setNetworkListeners(networkListeners);
                    newVirtualServer.setDefaultWebModule(defaultWebModule);
                    newVirtualServer.setState(state);
                    newVirtualServer.setLogFile(logFile);
                    // 1. add properties
                    // 2. check if the access-log and docroot properties have
                    //    been specified. We will use with default 
                    //    values if the properties have not been specified.
                    if (properties != null) {
                        for (Map.Entry entry : properties.entrySet()) {
                            String pn = (String) entry.getKey();
                            String pv = (String)entry.getValue();

                            if ("docroot".equals(pn)) {
                                docroot = pv;
                            } else if ("accesslog".equals(pn)) {
                                accessLog = pv;
                            } else {
                                Property property = newVirtualServer.createChild(Property.class);
                                property.setName(pn);
                                property.setValue(pv);
                                newVirtualServer.getProperty().add(property);
                            }
                        }
                    }

                    newVirtualServer.setDocroot(docroot);
                    newVirtualServer.setAccessLog(accessLog);

                    param.getVirtualServer().add(newVirtualServer);
                    return newVirtualServer;
                }
            }, httpService);

        } catch (TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_VIRTUAL_SERVER_FAIL), virtualServerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
