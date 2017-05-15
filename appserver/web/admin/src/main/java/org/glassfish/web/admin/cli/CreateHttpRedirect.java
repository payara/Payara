/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.grizzly.config.dom.HttpRedirect;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.internal.api.Target;
import org.glassfish.web.admin.LogFacade;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * <p>
 * Command to create <code>http-redirect</code> element as a child of the
 * <code>protocol</code> element.
 * </p>
 *
 * <p>
 * domain.xml example:
 * </p>
 * <pre>
 *     &lt;http-redirect port=&quot;8181&quot; secure=&quot;true&quot; /&gt;
 * </pre>
 *
 * @since 3.1
 */
@Service(name="create-http-redirect")
@PerLookup
@I18n("create.http.redirect")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-http-redirect", 
        description="create-http-redirect"),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-http-redirect", 
        description="create-http-redirect")
})
public class CreateHttpRedirect implements AdminCommand {

    @Param(name = "protocolname", primary = true)
    String protocolName;

    @Param(name="redirect-port", optional=true)
    String port;

    @Param(name="secure-redirect", optional=true)
    String secure;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    ServiceLocator services;

    @Inject
    Domain domain;

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    // ----------------------------------------------- Methods from AdminCommand


    @Override
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
        if (protocol.getHttpRedirect() != null) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_HTTP_REDIRECT_FAIL_DUPLICATE), protocolName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                public Object run(Protocol param) throws TransactionFailure {
                    HttpRedirect httpRedirect = param.createChild(HttpRedirect.class);
                    httpRedirect.setPort(port);
                    httpRedirect.setSecure(secure);
                    param.setHttpRedirect(httpRedirect);
                    return httpRedirect;
                }
            }, protocol);
        } catch (TransactionFailure e) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_HTTP_REDIRECT_FAIL), protocolName) +
                    (e.getMessage() == null ? "No reason given." : e.getMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
