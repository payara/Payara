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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 * 
 * Portions Copyright [2017-2025] [Payara Foundation and/or its affiliates] 
 */
package org.glassfish.web.admin.cli;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.FileCache;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.web.admin.LogFacade;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Command to create http element within a protocol element
 *
 * Sample Usage : create-http protocol_name
 *
 * domain.xml element example
 *
 * &lt;http max-connections=&quot;250&quot; default-virtual-server=&quot;server&quot; server-name=&quot;&quot;&gt; &lt;file-cache enabled=&quot;false&quot; /&gt; &lt;/http&gt;
 *
 * @author Justin Lee
 */
@Service(name = "create-http")
@PerLookup
@I18n("create.http")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Protocol.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-http", 
        description="Create",
        params={
            @RestParam(name="id", value="$parent")
        })
})
public class CreateHttp implements AdminCommand {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    @Param(name = "protocolname", primary = true)
    String protocolName;
    @Param(name = "request-timeout-seconds", optional = true, alias="requestTimeoutSeconds")
    String requestTimeoutSeconds;
    @Param(name = "timeout-seconds", defaultValue = "30", optional = true, alias="timeoutSeconds")
    String timeoutSeconds;
    @Param(name = "max-connection", defaultValue = "256", optional = true, alias="maxConnections")
    String maxConnections;
    @Param(name = "default-virtual-server", alias="defaultVirtualServer")
    String defaultVirtualServer;
    @Param(name = "dns-lookup-enabled", defaultValue = "false", optional = true, alias="dnsLookupEnabled")
    Boolean dnsLookupEnabled = false;
    @Param(name = "servername", optional = true, alias="serverName")
    String serverName;
    @Param(name = "serverHeader", optional = true, defaultValue = "true", alias="serverHeader")
    Boolean serverHeader = false;
    @Param(name = "xframe", optional = true, defaultValue = "true", alias="xframeOptions")
    Boolean xFrameOptions = false;
    
    @Param(name = "http2Enabled", alias = "http2enabled", optional = true, defaultValue = "true")
    Boolean http2Enabled;
    @Param(name = "http2MaxConcurrentStreams", alias = "http2maxconcurrentstreams", optional = true)
    Integer http2MaxConcurrentStreams;
    @Param(name = "http2InitialWindowSizeInBytes", alias = "http2initialwindowsizeinbytes", optional = true)
    Integer http2InitialWindowSizeInBytes;
    @Param(name = "http2MaxFramePayloadSizeInBytes", alias = "http2maxframepayloadsizeinbytes", optional = true)
    Integer http2MaxFramePayloadSizeInBytes;
    @Param(name = "http2MaxHeaderListSizeInBytes", alias = "http2maxheaderlistsizeinbytes", optional = true)
    Integer http2MaxHeaderListSizeInBytes;
    @Param(name = "http2StreamsHighWaterMark", alias = "http2streamshighwatermark", optional = true)
    Float http2StreamsHighWaterMark;
    @Param(name = "http2CleanPercentage", alias = "http2cleanpercentage", optional = true)
    Float http2CleanPercentage;
    @Param(name = "http2CleanFrequencyCheck", alias = "http2cleanfrequencycheck", optional = true)
    Integer http2CleanFrequencyCheck;
    @Param(name = "http2DisableCipherCheck", alias = "http2disableciphercheck", optional = true)
    Boolean http2DisableCipherCheck;
    @Param(name = "http2PushEnabled", alias = "http2pushenabled", optional = true)
    Boolean http2PushEnabled;
    
    
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    ServiceLocator services;
    @Inject
    Domain domain;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter
     * values.
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
        Protocols protocols = config.getNetworkConfig().getProtocols();
        Protocol protocol = null;
        for (Protocol p : protocols.getProtocol()) {
            if(protocolName.equals(p.getName())) {
                protocol = p;
            }
        }
        if (protocol == null) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_HTTP_FAIL_PROTOCOL_NOT_FOUND), protocolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        if (protocol.getHttp() != null) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_HTTP_FAIL_DUPLICATE), protocolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;

        }

        // Add to the <network-config>
        try {
            ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                public Object run(Protocol param) throws TransactionFailure {
                    Http http = param.createChild(Http.class);
                    final FileCache cache = http.createChild(FileCache.class);
                    cache.setEnabled("false");
                    http.setFileCache(cache);
                    http.setDefaultVirtualServer(defaultVirtualServer);
                    http.setDnsLookupEnabled(dnsLookupEnabled == null ? null : dnsLookupEnabled.toString());
                    http.setMaxConnections(maxConnections);
                    http.setRequestTimeoutSeconds(requestTimeoutSeconds);
                    http.setTimeoutSeconds(timeoutSeconds);
                    http.setServerHeader(serverHeader == null ? null : serverHeader.toString());
                    http.setXframeOptions(xFrameOptions == null ? null : xFrameOptions.toString());
                    http.setServerName(serverName);
                    
                    // HTTP2 options
                    http.setHttp2Enabled(http2Enabled.toString());
                    if (http2MaxConcurrentStreams != null) {
                        http.setHttp2MaxConcurrentStreams(http2MaxConcurrentStreams.toString());
                    }
                    if (http2InitialWindowSizeInBytes != null) {
                        http.setHttp2InitialWindowSizeInBytes(http2InitialWindowSizeInBytes.toString());
                    }
                    if (http2MaxFramePayloadSizeInBytes != null) {
                        http.setHttp2MaxFramePayloadSizeInBytes(http2MaxFramePayloadSizeInBytes.toString());
                    }
                    if (http2MaxHeaderListSizeInBytes != null) {
                        http.setHttp2MaxHeaderListSizeInBytes(http2MaxHeaderListSizeInBytes.toString());
                    }
                    if (http2StreamsHighWaterMark != null) {
                        http.setHttp2StreamsHighWaterMark(http2StreamsHighWaterMark.toString());
                    }
                    if (http2CleanPercentage != null) {
                        http.setHttp2CleanPercentage(http2CleanPercentage.toString());
                    }
                    if (http2CleanFrequencyCheck != null) {
                        http.setHttp2CleanFrequencyCheck(http2CleanFrequencyCheck.toString());
                    }
                    if (http2DisableCipherCheck != null) {
                        http.setHttp2DisableCipherCheck(http2DisableCipherCheck.toString());
                    }
                    if (http2PushEnabled != null) {
                        http.setHttp2PushEnabled(http2PushEnabled.toString());
                    }
                    param.setHttp(http);
                    return http;
                }
            }, protocol);
        } catch (TransactionFailure e) {
            report.setMessage(
                    MessageFormat.format(
                            rb.getString(LogFacade.CREATE_HTTP_REDIRECT_FAIL),
                            protocolName,
                            e.getMessage() == null ? "No reason given." : e.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
