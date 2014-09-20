/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import org.glassfish.api.Param;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.universal.Duration;
import org.glassfish.api.admin.*;

import javax.inject.Inject;

/**
 * uptime command
 * Reports on how long the server has been running.
 * 
 */
@Service(name = "uptime")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("uptime")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="uptime", 
        description="Uptime",
        useForAuthorization=true)
})
public class UptimeCommand implements AdminCommand {

    @Inject
    ServerEnvironmentImpl env;
    @Param(name = "milliseconds", optional = true, defaultValue = "false")
    Boolean milliseconds;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        long totalTime_ms = getUptime();
        String totalTime_mss = "" + totalTime_ms;	
        Duration duration = new Duration(totalTime_ms);
        duration.setTerse();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        String message;

        if (milliseconds)
            message = totalTime_mss;
        else
            message = localStrings.getLocalString("uptime.output.terse", "Uptime: {0}", duration);

        report.setMessage(message);
        report.getTopMessagePart().addProperty("milliseconds", totalTime_mss);
    }
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(UptimeCommand.class);

    private long getUptime() {
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        long totalTime_ms = -1;

        if (mxbean != null)
            totalTime_ms = mxbean.getUptime();

        if (totalTime_ms <= 0) {
            long start = env.getStartupContext().getCreationTime();
            totalTime_ms = System.currentTimeMillis() - start;
        }
        return totalTime_ms;
    }
}
