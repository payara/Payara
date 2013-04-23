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
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.grizzly.config.dom.Ssl;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.admin.commands.SslConfigHandler;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

/**
 * SSL configuration handler for iiop-listener configuration
 * @author Jerome Dochez
 *
 */
@Service(name="iiop-listener")
public class IiopSslConfigHandler implements SslConfigHandler {

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreateSsl.class);


    @Override
    public void create(final CreateSsl command, ActionReport report) {
       IiopService iiopService = command.config.getExtensionByType(IiopService.class);
        // ensure we have the specified listener
        IiopListener iiopListener = null;
        for (IiopListener listener : iiopService.getIiopListener()) {
            if (listener.getId().equals(command.listenerId)) {
                iiopListener = listener;
            }
        }
        if (iiopListener == null) {
            report.setMessage(
                localStrings.getLocalString("create.ssl.iiop.notfound",
                    "IIOP Listener named {0} to which this ssl element is " +
                        "being added does not exist.", command.listenerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        if (iiopListener.getSsl() != null) {
            report.setMessage(
                localStrings.getLocalString("create.ssl.iiop.alreadyExists",
                    "IIOP Listener named {0} to which this ssl element is " +
                        "being added already has an ssl element.", command.listenerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<IiopListener>() {
                        public Object run(IiopListener param)
                                throws PropertyVetoException, TransactionFailure {
                            Ssl newSsl = param.createChild(Ssl.class);
                            command.populateSslElement(newSsl);
                            param.setSsl(newSsl);
                            return newSsl;
                        }
                    }, iiopListener);

        } catch (TransactionFailure e) {
            command.reportError(report, e);
        }
        command.reportSuccess(report);
    }

    @Override
    public void delete(DeleteSsl command, ActionReport report) {

        IiopService iiopService = command.config.getExtensionByType(IiopService.class);
        IiopListener iiopListener = null;
        for (IiopListener listener : iiopService.getIiopListener()) {
            if (listener.getId().equals(command.listenerId)) {
                iiopListener = listener;
            }
        }

        if (iiopListener == null) {
            report.setMessage(localStrings.getLocalString(
                    "delete.ssl.iiop.listener.notfound",
                    "Iiop Listener named {0} not found", command.listenerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (iiopListener.getSsl() == null) {
            report.setMessage(localStrings.getLocalString(
                    "delete.ssl.element.doesnotexist", "Ssl element does " +
                    "not exist for Listener named {0}", command.listenerId));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        try {
            ConfigSupport.apply(new SingleConfigCode<IiopListener>() {
                public Object run(IiopListener param)
                throws PropertyVetoException {
                    param.setSsl(null);
                    return null;
                }
            }, iiopListener);
        } catch(TransactionFailure e) {
            command.reportError(report, e);
        }
    }
}
