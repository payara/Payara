/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli.internal;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import javax.inject.Inject;

/**
 * @author Jagadish Ramu
 */
@Service(name = "_get-activation-spec-class")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Resources.class,
        opType=RestEndpoint.OpType.GET,
        path="get-activation-spec-class",
        description="Get Activation Spec Class")
})
public class GetActivationSpecClass implements AdminCommand {
    @Inject
    private ConnectorRuntime connectorRuntime;

    @Param
    private String rarName;

    @Param
    private String messageListenerType;

    /**
     * @inheritDoc
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        try {
            String activationSpecClass =
                    connectorRuntime.getActivationSpecClass(rarName, messageListenerType);
            final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
            part.setMessage(activationSpecClass);
        } catch (Exception e) {
            report.setMessage("_get-activation-spec-class failed");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }

        ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
        report.setActionExitCode(ec);
    }
}
