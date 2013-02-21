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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
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
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.admin.monitor.HttpServiceStatsProviderBootstrap;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Command to create protocol element within network-config
 *
 * Sample Usage : create-protocol [--securityenabled true|false] protocol_name
 *
 * domain.xml element example
 *
 * <protocol name="http-listener-1"> <http max-connections="250" default-virtual-server="server" server-name="">
 * <file-cache enabled="false" /> </http> <ssl ssl3-enabled="false" cert-nickname="s1as" /> </protocol>
 *
 * @author Nandini Ektare
 */
@Service(name = "create-protocol")
@PerLookup
@I18n("create.protocol")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class CreateProtocol implements AdminCommand {
    @Param(name = "protocolname", primary = true)
    String protocolName;
    // TODO:
    // After v3 release, incorporate changes to CRUD <http/>, <port-unification/>
    // and <protocol-chain-instance-handler/>. As each has considerable number
    // of config options and no specific ids to co-relate, we may need to choose
    // the way create-ssl has been done. Grizzly team concurs on this proposal
    @Param(name = "securityenabled", alias="securityEnabled", optional = true, defaultValue = "false")
    Boolean securityEnabled = false;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    Domain domain;
    @Inject
    ServiceLocator services;

    private static final ResourceBundle rb = HttpServiceStatsProviderBootstrap.rb;

    @LogMessageInfo(
            message = "{0} protocol already exists. Cannot add duplicate protocol.",
            level = "INFO")
    private static final String CREATE_PROTOCOL_FAIL_DUPLICATE = "AS-WEB-ADMIN-00017";

    @LogMessageInfo(
            message = "Failed to create protocol {0}.",
            level = "INFO")
    private static final String CREATE_PROTOCOL_FAIL = "AS-WEB-ADMIN-00018";

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
        // check for duplicates
        NetworkConfig networkConfig = config.getNetworkConfig();
        Protocols protocols = networkConfig.getProtocols();
        for (Protocol protocol : protocols.getProtocol()) {
            if (protocolName != null &&
                protocolName.equalsIgnoreCase(protocol.getName())) {
                report.setMessage(MessageFormat.format(rb.getString(CREATE_PROTOCOL_FAIL_DUPLICATE), protocolName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        // Add to the <network-config>
        try {
            create(protocols, protocolName, securityEnabled);
        } catch (TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(CREATE_PROTOCOL_FAIL), protocolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        } catch (Exception e) {
            report.setMessage(MessageFormat.format(rb.getString(CREATE_PROTOCOL_FAIL), protocolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

    public static void create(final Protocols protocols, final String name, final Boolean securityEnabled)
        throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<Protocols>() {
            public Object run(Protocols param) throws TransactionFailure {
                Protocol newProtocol = param.createChild(Protocol.class);
                newProtocol.setName(name);
                newProtocol.setSecurityEnabled(securityEnabled == null ? null : securityEnabled.toString());
                param.getProtocol().add(newProtocol);
                return newProtocol;
            }
        }, protocols);
    }
}
