/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.config;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import com.sun.enterprise.admin.commands.CreateSsl;
import com.sun.enterprise.admin.commands.DeleteSsl;
import com.sun.enterprise.admin.commands.SslConfigHandler;
import org.glassfish.api.ActionReport;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.web.LogFacade;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * SSL configuration handler for http-listener protocol
 * @author Jerome Dochez
 */
@Service(name="http-listener")
public class WebSslConfigHandler implements SslConfigHandler {

    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    @Override
    public void create(final CreateSsl command, ActionReport report) {

        NetworkConfig netConfig = command.config.getNetworkConfig();
        // ensure we have the specified listener
        NetworkListener listener = netConfig.getNetworkListener(command.listenerId);
        Protocol httpProtocol;
        try {
            if (listener == null) {
                report.setMessage(
                        MessageFormat.format(
                                rb.getString(LogFacade.CREATE_SSL_HTTP_NOT_FOUND),
                                        command.listenerId));
                httpProtocol = command.findOrCreateProtocol(command.listenerId);
            } else {
                httpProtocol = listener.findHttpProtocol();
                Ssl ssl = httpProtocol.getSsl();
                if (ssl != null) {
                    report.setMessage(
                            MessageFormat.format(
                                    rb.getString(LogFacade.CREATE_SSL_HTTP_ALREADY_EXISTS),
                                            command.listenerId));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
            ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                        public Object run(Protocol param) throws TransactionFailure {
                            Ssl newSsl = param.createChild(Ssl.class);
                            command.populateSslElement(newSsl);
                            param.setSsl(newSsl);
                            return newSsl;
                        }
                    }, httpProtocol);

        } catch (TransactionFailure e) {
            command.reportError(report, e);
        }
        command.reportSuccess(report);
    }

    @Override
    public void delete(DeleteSsl command, ActionReport report) {

        NetworkConfig netConfig = command.config.getNetworkConfig();
        NetworkListener networkListener =
            netConfig.getNetworkListener(command.listenerId);

        if (networkListener == null) {
            report.setMessage(
                    MessageFormat.format(
                            rb.getString(LogFacade.DELETE_SSL_HTTP_LISTENER_NOT_FOUND), command.listenerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        Protocol protocol = networkListener.findHttpProtocol();
        if (protocol.getSsl() == null) {
            report.setMessage(
                    MessageFormat.format(
                            rb.getString(LogFacade.DELETE_SSL_ELEMENT_DOES_NOT_EXIST), command.listenerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                public Object run(Protocol param) {
                    param.setSsl(null);
                    return null;
                }
            }, networkListener.findHttpProtocol());
        } catch(TransactionFailure e) {
            command.reportError(report, e);
        }
    }
}
