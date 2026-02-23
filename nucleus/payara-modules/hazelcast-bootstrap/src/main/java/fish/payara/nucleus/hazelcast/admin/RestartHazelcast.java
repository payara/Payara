/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.nucleus.hazelcast.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.Properties;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.TargetBasedExecutor;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author steve
 */
@Service(name = "restart-hazelcast")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("restart-hazelcast")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DOMAIN, CommandTarget.DEPLOYMENT_GROUP, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG, CommandTarget.DOMAIN})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "restart-hazelcast",
            description = "Restart Hazelcast")
})
public class RestartHazelcast implements AdminCommand {
    
    @Inject
    HazelcastCore hazelcast;    
    
    @Inject
    TargetBasedExecutor executor;
    
    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;
    
    @Inject
    private ServerEnvironment serverEnv;

    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        // what is wrong is that this does not work for the local server as it will never match
        boolean forMe = isThisForMe();
        if (forMe && hazelcast.isEnabled()) {
            hazelcast.setEnabled(false);
            hazelcast.setEnabled(true);
            actionReport.setMessage("Hazelcast Restarted");
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } else if (forMe && !hazelcast.isEnabled()) {
            actionReport.setMessage("Hazelcast not enabled. Restart ignored");
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } else {
            actionReport.setMessage("Restart Ignored");
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }

    }
    
    private boolean isThisForMe() {
        boolean result = false;
        String instanceName = serverEnv.getInstanceName();
        
        if (instanceName.equals(target)) {
            result = true;
        }
        
        if (target.equals("domain")) {
            result = true;
        }
        return result;
    }
    
}
