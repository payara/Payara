/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.glassfish.web.admin.LogFacade;
import org.jvnet.hk2.annotations.Service;

/**
 * CLI command for getting the properties of a HTTP listener.
 * <p>
 * {@literal
 * Usage: asadmin> get-http-listener
 * [--target <target(default:server)>]
 * [-v|--verbose <verbose(default:false)>]
 * listenername
 * }</p>
 */
@Service(name = "get-http-listener")
@PerLookup
@I18n("get.http.listener")
@ExecuteOn(RuntimeType.DAS)
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean = HttpService.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-http-listener",
            description = "get-http-listener")
})
public class GetHttpListener implements AdminCommand {

    private static final Logger logger = LogFacade.getLogger();

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(primary = true)
    private String listenerName;

    @Param(optional = true, shortName = "v", defaultValue = "false")
    private Boolean verbose;

    @Inject
    Target targetUtil;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        // Check that a configuration can be found
        if (targetUtil.getConfig(target) == null) {
            report.failure(logger, MessageFormat.format(logger.getResourceBundle().getString(LogFacade.UNKNOWN_CONFIG), target));
            return;
        }
        Config config = targetUtil.getConfig(target);

        // Check that a matching listener can be found
        List<NetworkListener> listeners = config.getNetworkConfig().getNetworkListeners().getNetworkListener();
        Optional<NetworkListener> optionalListener = listeners.stream()
                .filter(listener -> listener.getName().equals(listenerName))
                .findFirst();
        if (!optionalListener.isPresent()) {
            report.failure(logger, MessageFormat.format(logger.getResourceBundle().getString(LogFacade.UNKNOWN_NETWORK_LISTENER), listenerName, target));
            return;
        }
        NetworkListener listener = optionalListener.get();

        // Write message body
        report.appendMessage(String.format("Name: %s\n", listener.getName()));
        report.appendMessage(String.format("Enabled: %s\n", listener.getEnabled()));
        report.appendMessage(String.format("Port: %s\n", listener.getPort()));
        if (listener.getPortRange() != null) {
            report.appendMessage(String.format("Port Range: %s\n", listener.getPortRange()));
        }
        report.appendMessage(String.format("Address: %s\n", listener.getAddress()));
        report.appendMessage(String.format("Protocol: %s\n", listener.getProtocol()));
        if (verbose) {
            report.appendMessage(String.format("Transport: %s\n", listener.getTransport()));
            report.appendMessage(String.format("Type: %s\n", listener.getType()));
            report.appendMessage(String.format("Thread Pool: %s\n", listener.getThreadPool()));
            report.appendMessage(String.format("JK Enabled: %s\n", listener.getJkEnabled()));
            report.appendMessage(String.format("JK Configuration File: %s\n", listener.getJkConfigurationFile()));
        }

        // Write the variables as properties
        Properties properties = new Properties();
        properties.put("name", listener.getName());
        properties.put("enabled", listener.getEnabled());
        properties.put("port", listener.getPort());
        if (listener.getPortRange() != null) {
            properties.put("portRange", listener.getPortRange());
        }
        properties.put("address", listener.getAddress());
        properties.put("protocol", listener.getProtocol());
        properties.put("transport", listener.getTransport());
        properties.put("type", listener.getType());
        properties.put("threadPool", listener.getThreadPool());
        properties.put("jkEnabled", listener.getJkEnabled());
        properties.put("jkConfigurationFile", listener.getJkConfigurationFile());
        report.setExtraProperties(properties);
    }

}
