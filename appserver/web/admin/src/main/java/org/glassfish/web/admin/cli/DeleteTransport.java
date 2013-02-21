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

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Transport;
import org.glassfish.grizzly.config.dom.Transports;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Delete Transport command
 * 
 */
@Service(name="delete-transport")
@PerLookup
@I18n("delete.transport")
@org.glassfish.api.admin.ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteTransport implements AdminCommand {

    private static final ResourceBundle rb = HttpServiceStatsProviderBootstrap.rb;

    @LogMessageInfo(
            message = "{0} transport is being used in the network listener {1}.",
            level = "INFO")
    protected static final String DELETE_TRANSPORT_BEINGUSED = "AS-WEB-ADMIN-00040";

    @LogMessageInfo(
            message = "Deletion of Transport {0} failed.",
            level = "INFO")
    protected static final String DELETE_TRANSPORT_FAIL = "AS-WEB-ADMIN-00041";

    @LogMessageInfo(
            message = "{0} transport doesn''t exist.",
            level = "INFO")
    protected static final String DELETE_TRANSPORT_NOT_EXISTS = "AS-WEB-ADMIN-00042";

    @Param(name="transportname", primary=true)
    String transportName;

    Transport transportToBeRemoved = null;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    
    @Inject
    Domain domain;
    
    @Inject
    ServiceLocator services;
    
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
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

        NetworkConfig networkConfig = config.getNetworkConfig();
        Transports transports = networkConfig.getTransports();

        try {
            for (Transport transport : transports.getTransport()) {
                if (transportName.equalsIgnoreCase(transport.getName())) {
                    transportToBeRemoved = transport;
                }
            }

            if (transportToBeRemoved == null) {
                report.setMessage(MessageFormat.format(rb.getString(DELETE_TRANSPORT_NOT_EXISTS), transportName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            // check if the transport to be deleted is being used by
            // any network listener

            List<NetworkListener> nwlsnrList =
                transportToBeRemoved.findNetworkListeners();
            for (NetworkListener nwlsnr : nwlsnrList) {
              if (transportToBeRemoved.getName().equals(nwlsnr.getTransport())) {
                  report.setMessage(
                          MessageFormat.format(
                                  rb.getString(DELETE_TRANSPORT_BEINGUSED),
                                  transportName,
                                  nwlsnr.getName()));
                  report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                  return;
              }
            }

            ConfigSupport.apply(new SingleConfigCode<Transports>() {
                public Object run(Transports param) {
                    param.getTransport().remove(transportToBeRemoved);
                    return transportToBeRemoved;
                }
            }, transports);
            
        } catch(TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(DELETE_TRANSPORT_FAIL), transportName) +
                    "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
