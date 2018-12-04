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

package org.glassfish.ejb.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.List;

import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.EjbContainerUtil;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.internal.api.Target;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;


@Service(name="list-timers")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.timers")
@ExecuteOn(value={RuntimeType.DAS})
@TargetType(value={CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-timers", 
        description="List Timers")
})
public class ListTimers implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ListTimers.class);

    @Param(primary=true, optional=true,
    defaultValue=SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;

    @Inject
    private EjbContainerUtil ejbContainerUtil;

    @Inject
    Target targetUtil;

    /**
     * Executes the command
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        String[] serverIds = null;
        if(targetUtil.isCluster(target)) {
            List<Server> serversInCluster = targetUtil.getInstances(target);
            serverIds = new String[serversInCluster.size()];
            for(int i = 0; i < serverIds.length; i++) {
                serverIds[i] = serversInCluster.get(i).getName();
            }
        } else {
            serverIds = new String[] {target};
        }
        try {
            String[] timerCounts = listTimers(serverIds);
            for (int i = 0; i < serverIds.length; i++) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(serverIds[i] + ": " + timerCounts[i]);
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("list.timers.failed",
                    "List Timers command failed"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private String[] listTimers(String[] serverIds) {
        String[] result = new String[serverIds.length];

        EJBTimerService ejbTimerService = null;
        if (EJBTimerService.isPersistentTimerServiceLoaded()) {
            ejbTimerService = EJBTimerService.getPersistentTimerService();
        } else if (EJBTimerService.isNonPersistentTimerServiceLoaded()) {
            ejbTimerService = EJBTimerService.getNonPersistentTimerService();
        }
        if (ejbTimerService != null) {
            result = ejbTimerService.listTimers(serverIds);
        }

        return result;
    }

}
