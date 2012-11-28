/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.universal.process.ProcessUtils;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;

import javax.inject.Singleton;
import org.glassfish.server.ServerEnvironmentImpl;
import com.sun.enterprise.glassfish.bootstrap.StartupContextUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import org.glassfish.api.admin.*;
import org.glassfish.internal.config.UnprocessedConfigListener;

/**
 * Locations command to indicate where this server is installed.
 * @author Jerome Dochez
 */
@Service(name="__locations")
@Singleton
@CommandLock(CommandLock.LockType.NONE)
@I18n("locations.command")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="locations", 
        description="Location",
        useForAuthorization=true)
})
public class LocationsCommand implements AdminCommand {
    
    @Inject
    ServerEnvironmentImpl env;

    @Inject
    private UnprocessedConfigListener ucl;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage(env.getInstanceRoot().getAbsolutePath().replace('\\', '/'));
        MessagePart mp = report.getTopMessagePart();
        mp.addProperty("Base-Root", StartupContextUtil.getInstallRoot(env.getStartupContext()).getAbsolutePath());
        mp.addProperty("Domain-Root", env.getDomainRoot().getAbsolutePath());
        mp.addProperty("Instance-Root", env.getInstanceRoot().getAbsolutePath());
        mp.addProperty("Config-Dir", env.getConfigDirPath().getAbsolutePath());
        mp.addProperty("Uptime", ""+getUptime());
        mp.addProperty("Pid", ""+ProcessUtils.getPid());
        mp.addProperty("Restart-Required", ""+ucl.serverRequiresRestart());
    }

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
