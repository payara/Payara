/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

/** Implements the front end for generating the JVM report. Sends back a String
 * to the asadmin console based on server's locale.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @since GlassFish V3
 */
@Service(name="generate-jvm-report")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("generate.jvm.report")
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE})
@ExecuteOn(value = {RuntimeType.INSTANCE}, ifNeverStarted=FailurePolicy.Error)
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.GET, 
        path="generate-jvm-report", 
        description="Generate Report",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.GET, 
        path="generate-jvm-report", 
        description="Generate Report",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=JavaConfig.class,
        opType=RestEndpoint.OpType.GET, 
        path="generate-jvm-report", 
        description="Generate Report",
        params={
            @RestParam(name="target", value="$grandparent")
        })
})
@AccessRequired(resource="domain/jvm", action="read")
public class GenerateJvmReportCommand implements AdminCommand {
    
    @Param(name="target", optional=true) 
    String target;
    
    @Param(name="type", optional=true, defaultValue="summary",
           acceptableValues = "summary, thread, class, memory, log")
    String type;
    
    private MBeanServer mbs = null;  //needs to be injected, I guess

    public void execute(AdminCommandContext ctx) {
        prepare();
        String result = getResult();
        ActionReport report = ctx.getActionReport();
        report.setMessage(result);
        report.setActionExitCode(ExitCode.SUCCESS);
    }
    
    private synchronized void prepare() {
        mbs = ManagementFactory.getPlatformMBeanServer();
    }
    private String getResult() {
        if (type.equals("summary"))
            return new SummaryReporter(mbs).getSummaryReport();
        else if (type.equals("thread"))
            return new ThreadMonitor(mbs).getThreadDump();
        else if (type.equals("class"))
            return new ClassReporter(mbs).getClassReport();
        else if (type.equals("memory"))
            return new MemoryReporter(mbs).getMemoryReport();
        else if (type.equals("log"))
            return new LogReporter().getLoggingReport();
        else
            throw new IllegalArgumentException("Unsupported Option: " + type);   //this should not happen
    }
}
