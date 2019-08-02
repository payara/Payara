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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.kernel.embedded;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.glassfish.internal.embedded.LifecycleException;
import org.glassfish.internal.embedded.Port;
import org.glassfish.internal.embedded.admin.CommandExecution;
import org.glassfish.internal.embedded.admin.CommandParameters;
import org.glassfish.internal.embedded.admin.EmbeddedAdminContainer;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.ActionReport;

import java.util.List;
import java.util.ArrayList;

import com.sun.enterprise.admin.report.PlainTextActionReporter;
import org.glassfish.internal.api.InternalSystemAdministrator;

/**
 * Implementation of the embedded command execution
 *
 * @author Jerome Dochez
 */
@Service
public class EmbeddedAdminCtrImpl implements EmbeddedAdminContainer {

    @Inject
    CommandRunner runner;
    
    @Inject
    private InternalSystemAdministrator kernelIdentity;

    private final static List<Sniffer> empty = new ArrayList<Sniffer>();

    public List<Sniffer> getSniffers() {
        return empty;
    }

    public void bind(Port port, String protocol) {

    }

    public void start() throws LifecycleException {

    }

    public void stop() throws LifecycleException {

    }

    public CommandExecution execute(String commandName, CommandParameters params) {
        ParameterMap props = params.getOptions();
        if (params.getOperands().size() > 0) {
            for (String op : params.getOperands())
                props.add("DEFAULT", op);
        }
        final ActionReport report = new PlainTextActionReporter();
        CommandExecution ce = new CommandExecution() {

            public ActionReport getActionReport() {
                return report;
            }

            public ActionReport.ExitCode getExitCode() {
                return report.getActionExitCode();
            }

            public String getMessage() {
                return report.getMessage();
            }
        };
        runner.getCommandInvocation(commandName, report, kernelIdentity.getSubject()).parameters(props).execute();
        return ce;
    }

    public void bind(Port port) {

    }

}
