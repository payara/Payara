/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.commands;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Ssl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

@Service(name="protocol")
public class ProtocolSslConfigHandler implements SslConfigHandler {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ProtocolSslConfigHandler.class);


    // ------------------------------------------- Methods from SslConfigHandler


    @Override
    public void create(final CreateSsl command, final ActionReport report) {
        try {
            final Protocol protocol =
                    command.findOrCreateProtocol(command.listenerId, false);
            if (protocol == null) {
                report.setMessage(
                        localStrings.getLocalString(
                                "create.ssl.protocol.notfound.fail",
                                "Unable to find protocol {0}.",
                                command.listenerId));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            } else {
                ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                                    public Object run(Protocol param) throws TransactionFailure {
                                        Ssl newSsl = param.createChild(Ssl.class);
                                        param.setSecurityEnabled("true");
                                        command.populateSslElement(newSsl);
                                        param.setSsl(newSsl);
                                        return newSsl;
                                    }
                                }, protocol);
            }
        } catch (TransactionFailure transactionFailure) {
            command.reportError(report, transactionFailure);
            return;
        }
        command.reportSuccess(report);
    }

    @Override
    public void delete(final DeleteSsl command, final ActionReport report) {
        try {
            NetworkConfig networkConfig = command.config.getNetworkConfig();
            final Protocol protocol = networkConfig.findProtocol(command.listenerId);
            if (protocol != null) {

                ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                    public Object run(Protocol param) {
                        param.setSecurityEnabled("false");
                        param.setSsl(null);
                        return null;
                    }
                }, protocol);
            }
        } catch (TransactionFailure transactionFailure) {
            command.reportError(report, transactionFailure);
        }
    }
}
