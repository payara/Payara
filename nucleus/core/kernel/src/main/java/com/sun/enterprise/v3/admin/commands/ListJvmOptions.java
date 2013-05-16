/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * Lists the JVM options configured in server's configuration.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @author Kin-man Chung
 * @since GlassFish V3
 */
@Service(name="list-jvm-options")   //implements the cli command by this "name"
@PerLookup            //should be provided "per lookup of this class", not singleton
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.jvm.options")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-jvm-options", 
        description="list-jvm-options")
})
public final class ListJvmOptions implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Param(name="target", optional=true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Param(name="profiler", optional=true)
    Boolean profiler=false;
    
    @Inject
    Target targetService;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    private static final StringManager lsm = StringManager.getManager(ListJvmOptions.class); 
    
    @AccessRequired.To("read")
    private JavaConfig jc;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.updateConfigIfNeeded(config, targetService, target);
        jc = config.getJavaConfig();
        return true;
    }
    
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        List<String> opts;
        if (profiler) {
                if (jc.getProfiler() == null) {
                    report.setMessage(lsm.getString("create.profiler.first"));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            opts = jc.getProfiler().getJvmOptions();
        } else
            opts = jc.getJvmOptions();
        //Collections.sort(opts); //sorting is garbled by Reporter anyway, so let's move sorting to the client side
        try {
            for (String option : opts) {
                ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(option);
            }
        } catch (Exception e) {
            report.setMessage(lsm.getStringWithDefault("list.jvm.options.failed",
                    "Command: list-jvm-options failed"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);        
    }
}
