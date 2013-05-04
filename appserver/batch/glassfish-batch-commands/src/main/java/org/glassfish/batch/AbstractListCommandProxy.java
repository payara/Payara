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
package org.glassfish.batch;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to list batch jobs info
 *
 *         1      *             1      *
 * jobName --------> instanceId --------> executionId
 *
 * @author Mahesh Kannan
 */
public abstract class AbstractListCommandProxy
    implements AdminCommand {

    @Inject
    protected ServiceLocator serviceLocator;

    @Inject
    protected Target targetUtil;

    @Inject
    protected Logger logger;

    @Param(name = "terse", optional=true, defaultValue="false", shortName="t")
    protected boolean isTerse = false;

    @Param(name = "output", shortName = "o", optional = true)
    protected String outputHeaderList;

    @Param(name = "header", shortName = "h", optional = true)
    protected boolean header;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    protected String target;

    @Param(name = "long", shortName = "l", optional = true)
    protected boolean useLongFormat;

    protected ActionReport.ExitCode commandsExitCode = ActionReport.ExitCode.SUCCESS;

    @Override
    public final void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        ActionReport subReport = null;
        if (! preInvoke(context, actionReport)) {
            commandsExitCode = ActionReport.ExitCode.FAILURE;
            return;
        }

        if (targetUtil.isCluster(target)) {
            for (Server serverInst : targetUtil.getInstances(target)) {
                try {
                    subReport = executeInternalCommand(context, serverInst.getName());
                    break;
                } catch (Throwable ex) {
                    logger.log(Level.INFO, "Got exception: " + ex.toString());
                }
            }
        } else if (target.equals("server")) {
            subReport = executeInternalCommand(context, target);
        } else {
            subReport = executeInternalCommand(context, target);
        }

        if (subReport != null) {
            if (subReport.getExtraProperties() != null && subReport.getExtraProperties().size() > 0)
                postInvoke(context, subReport);
            else {
                if (subReport.getSubActionsReport() != null && subReport.getSubActionsReport().size() > 0) {
                    postInvoke(context, subReport.getSubActionsReport().get(0));
                } else {
                    actionReport.setMessage(subReport.getMessage());
                    commandsExitCode = subReport.getActionExitCode();
                }
            }
        }
        actionReport.setActionExitCode(commandsExitCode);
    }

    protected boolean preInvoke(AdminCommandContext ctx, ActionReport subReport) {
        return true;
    }

    protected abstract String getCommandName();

    protected abstract void postInvoke(AdminCommandContext context, ActionReport subReport);

    private ActionReport executeInternalCommand(AdminCommandContext context, String targetInstanceName) {
        String commandName = getCommandName();
        ParameterMap params = new ParameterMap();
        params.add("target", targetInstanceName);
        fillParameterMap(params);
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv = runner.getCommandInvocation(commandName, subReport, context.getSubject());
        inv.parameters(params);
        inv.execute();

        return subReport;
    }

    protected void fillParameterMap(ParameterMap parameterMap) {
        if (isTerse)
            parameterMap.add("terse", ""+isTerse);
        if (outputHeaderList != null)
            parameterMap.add("output", outputHeaderList);
        if (header)
            parameterMap.add("header", ""+header);
        if (useLongFormat)
            parameterMap.add("long", ""+useLongFormat);
    }

    protected boolean isLongNumber(String str) {
        try {
            long val = Long.parseLong(str);
        } catch (NumberFormatException nEx) {
            return false;
        }

        return true;
    }
}
