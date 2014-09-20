/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.orb.admin.config.handler;

import com.sun.enterprise.admin.commands.CreateSsl;
import com.sun.enterprise.admin.commands.DeleteSsl;
import com.sun.enterprise.admin.commands.SslConfigHandler;
import org.glassfish.orb.admin.config.IiopService;
import com.sun.enterprise.config.serverbeans.SslClientConfig;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.grizzly.config.dom.Ssl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * SSL configuration handler for iiop-service.
 * @author Jerome Dochez
 */
@Service(name="iiop-service")
public class IiopServiceSslConfigHandler implements SslConfigHandler {

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreateSsl.class);

    @Override
    public void create(final CreateSsl command, ActionReport report) {
        IiopService iiopSvc = command.config.getExtensionByType(IiopService.class);
        if (iiopSvc.getSslClientConfig() != null) {
            report.setMessage(
                localStrings.getLocalString(
                    "create.ssl.iiopsvc.alreadyExists", "IIOP Service " +
                        "already has been configured with SSL configuration."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<IiopService>() {
                        public Object run(IiopService param)
                                throws PropertyVetoException, TransactionFailure {
                            SslClientConfig newSslClientCfg =
                                    param.createChild(SslClientConfig.class);
                            Ssl newSsl = newSslClientCfg.createChild(Ssl.class);
                            command.populateSslElement(newSsl);
                            newSslClientCfg.setSsl(newSsl);
                            param.setSslClientConfig(newSslClientCfg);
                            return newSsl;
                        }
                    }, iiopSvc);

        } catch (TransactionFailure e) {
            command.reportError(report, e);
        }
        command.reportSuccess(report);
    }

    @Override
    public void delete(DeleteSsl command, ActionReport report) {
        if (command.config.getExtensionByType(IiopService.class).getSslClientConfig() == null) {
            report.setMessage(localStrings.getLocalString(
                    "delete.ssl.element.doesnotexistforiiop",
                    "Ssl element does not exist for IIOP service"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<IiopService>() {
                    public Object run(IiopService param)
                            throws PropertyVetoException {
                        param.setSslClientConfig(null);
                        return null;
                    }
                }, command.config.getExtensionByType(IiopService.class));
        } catch (TransactionFailure e) {
            command.reportError(report, e);
        }
    }
}
