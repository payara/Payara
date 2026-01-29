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

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

@Service(name = "list-caches")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list-caches")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DOMAIN, CommandTarget.DEPLOYMENT_GROUP, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "list-caches",
            description = "Lists Hazelcast caches and Distributed Objects")
})
/**
 * Lists the Hazelcast Distributed Objects in the cluster
 *
 * @author steve
 */
public class ListCaches implements AdminCommand {

    @Inject
    HazelcastCore hazelcast;

    @Inject
    protected Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Override
    @SuppressWarnings("unchecked")
    public void execute(AdminCommandContext context) {

        final ActionReport actionReport = context.getActionReport();

        if (hazelcast.isEnabled()) {
            HazelcastInstance instance = hazelcast.getInstance();
            if (instance != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("{ \n");
                for (DistributedObject dobject : instance.getDistributedObjects()) {
                    if (dobject instanceof IMap) {
                        String memberName = dobject.getName();
                        if (memberName != null) {
                            builder.append("HZMap ").append(memberName).append(" ");
                        }
                        builder.append(dobject.getServiceName()).append(' ');
                        builder.append(((IMap<Object, Object>) dobject).size()).append(" entries");
                        builder.append("\n");
                    }
                }

                for (String cacheName : hazelcast.getCachingProvider().getCacheManager().getCacheNames()) {
                    if (cacheName != null) {
                        builder.append("JCache ").append(cacheName).append(" \n");
                    }
                }

                builder.append("}\n");
                actionReport.setMessage(builder.toString());

                // build extra message
                Properties extraProps = new Properties();
                StringBuilder extraBuilder = new StringBuilder(actionReport.getMessage());
                extraBuilder.append("<br/>");
                for (ActionReport subReport : actionReport.getSubActionsReport()) {
                    extraBuilder.append(subReport.getMessage()).append("<br/>");
                }
                extraProps.put("Cache", extraBuilder.toString());
                actionReport.setExtraProperties(extraProps);
            } else {
                Properties extraProps = new Properties();
                extraProps.put("Caches", "Hazelcast is not enabled");
                actionReport.setExtraProperties(extraProps);
                actionReport.setMessage("Hazelcast is not enabled");
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
        } else {
            Properties extraProps = new Properties();
            extraProps.put("Caches", "Hazelcast is not enabled");
            actionReport.setExtraProperties(extraProps);
            actionReport.setMessage("Hazelcast is not enabled");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
