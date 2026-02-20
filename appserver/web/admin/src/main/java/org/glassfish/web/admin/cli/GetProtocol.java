/* 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2017-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.glassfish.web.admin.LogFacade;
import org.jvnet.hk2.annotations.Service;

/**
 * CLI command for getting the properties of a protocol.
 * <p>
 * {@literal
 * Usage: asadmin> get-protocol
 * [--target <target(default:server)>]
 * [-v|--verbose <verbose(default:false)>]
 * protocolname
 * }</p>
 */
@Service(name = "get-protocol")
@PerLookup
@I18n("get.protocol")
@ExecuteOn(RuntimeType.DAS)
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean = HttpService.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-protocol",
            description = "get-protocol")
})
public class GetProtocol implements AdminCommand {

    private static final Logger logger = LogFacade.getLogger();

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(primary = true)
    private String protocolName;

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
        List<Protocol> protocols = config.getNetworkConfig().getProtocols().getProtocol();
        Optional<Protocol> optionalProtocol = protocols.stream()
                .filter(protocol -> protocol.getName().equals(protocolName))
                .findFirst();
        if (!optionalProtocol.isPresent()) {
            report.failure(logger, MessageFormat.format(logger.getResourceBundle().getString(LogFacade.UNKNOWN_PROTOCOL), protocolName, target));
            return;
        }
        Protocol protocol = optionalProtocol.get();

        // Write message body
        report.appendMessage(String.format("Name: %s\n", protocol.getName()));

        // Write HTTP config options
        report.appendMessage("\nHTTP:\n");
        report.appendMessage(String.format("Server Name: %s\n", protocol.getHttp().getServerName()));
        report.appendMessage(String.format("Max Connections: %s seconds\n", protocol.getHttp().getMaxConnections()));
        report.appendMessage(String.format("Default Virtual Server: %s\n", protocol.getHttp().getDefaultVirtualServer()));
        report.appendMessage(String.format("Server Header: %s\n", protocol.getHttp().getServerHeader()));
        if (verbose) {
            report.appendMessage(String.format("Request Timeout: %s seconds\n", protocol.getHttp().getRequestTimeoutSeconds()));
            report.appendMessage(String.format("Timeout: %s seconds\n", protocol.getHttp().getTimeoutSeconds()));
            report.appendMessage(String.format("DNS Lookup Enabled: %s\n", protocol.getHttp().getDnsLookupEnabled()));
            report.appendMessage(String.format("X Frame Options: %s\n", protocol.getHttp().getXframeOptions()));
        }

        // Write HTTP/2 config options
        report.appendMessage("\nHTTP/2:\n");
        report.appendMessage(String.format("Enabled: %s\n", protocol.getHttp().getHttp2Enabled()));
        if (Boolean.parseBoolean(protocol.getHttp().getHttp2Enabled())) {
            report.appendMessage(String.format("Push Enabled: %s\n", protocol.getHttp().getHttp2PushEnabled()));
            report.appendMessage(String.format("Cipher Check: %s\n", !Boolean.parseBoolean(protocol.getHttp().getHttp2DisableCipherCheck())));
            if (verbose) {
                report.appendMessage(String.format("Max Concurrent Streams: %s\n", protocol.getHttp().getHttp2MaxConcurrentStreams()));
                report.appendMessage(String.format("Initial Window Size: %s bytes\n", protocol.getHttp().getHttp2InitialWindowSizeInBytes()));
                report.appendMessage(String.format("Max Frame Payload Size: %s bytes\n", protocol.getHttp().getHttp2MaxFramePayloadSizeInBytes()));
                report.appendMessage(String.format("Max Header List Size: %s bytes\n", protocol.getHttp().getHttp2MaxHeaderListSizeInBytes()));
                report.appendMessage(String.format("Streams High Water Mark: %s\n", protocol.getHttp().getHttp2StreamsHighWaterMark()));
                report.appendMessage(String.format("Clean Percentage: %s\n", protocol.getHttp().getHttp2CleanPercentage()));
                report.appendMessage(String.format("Clean Frequency Check: %s\n", protocol.getHttp().getHttp2CleanFrequencyCheck()));
            }
        }

        // Write the variables as properties
        Properties properties = new Properties();
        properties.put("name", protocol.getName());
        properties.put("serverName", protocol.getHttp().getServerName() == null ? "null" : protocol.getHttp().getServerName());
        properties.put("maxConnections", protocol.getHttp().getMaxConnections());
        properties.put("defaultVirtualServer", protocol.getHttp().getDefaultVirtualServer());
        properties.put("serverHeader", protocol.getHttp().getServerHeader());
        properties.put("requestTimeoutSeconds", protocol.getHttp().getRequestTimeoutSeconds());
        properties.put("timeoutSeconds", protocol.getHttp().getTimeoutSeconds());
        properties.put("dnsLookupEnabled", protocol.getHttp().getDnsLookupEnabled());
        properties.put("xFrameOptions", protocol.getHttp().getXframeOptions());
        properties.put("http2Enabled", protocol.getHttp().getHttp2Enabled());
        properties.put("http2MaxConcurrentStreams", protocol.getHttp().getHttp2MaxConcurrentStreams());
        properties.put("http2InitialWindowSizeInBytes", protocol.getHttp().getHttp2InitialWindowSizeInBytes());
        properties.put("http2MaxFramePayloadSizeInBytes", protocol.getHttp().getHttp2MaxFramePayloadSizeInBytes());
        properties.put("http2MaxHeaderListSizeInBytes", protocol.getHttp().getHttp2MaxHeaderListSizeInBytes());
        properties.put("http2StreamsHighWaterMark", protocol.getHttp().getHttp2StreamsHighWaterMark());
        properties.put("http2CleanPercentage", protocol.getHttp().getHttp2CleanPercentage());
        properties.put("http2CleanFrequencyCheck", protocol.getHttp().getHttp2CleanFrequencyCheck());
        properties.put("http2DisableCipherCheck", protocol.getHttp().getHttp2DisableCipherCheck());
        properties.put("http2PushEnabled", protocol.getHttp().getHttp2PushEnabled());
        report.setExtraProperties(properties);
    }

}
