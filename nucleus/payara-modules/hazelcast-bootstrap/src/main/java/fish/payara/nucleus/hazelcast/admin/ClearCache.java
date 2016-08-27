/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.hazelcast.admin;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
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

/**
 * ASAdmin command to clear the cache of all entries
 *
 * @author steve
 */
@Service(name = "clear-cache")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("clear-cache")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "lclear-cache",
            description = "Clears a JCache or Hazalcast IMap")
})
public class ClearCache implements AdminCommand {

    @Inject
    HazelcastCore hazelcast;

    @Inject
    protected Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Param(name = "name", defaultValue = "")
    protected String cacheName;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        if (hazelcast.isEnabled()) {
            HazelcastInstance instance = hazelcast.getInstance();
            // first look for a HZ IMap
            if (instance != null) {
                for (DistributedObject dobject : instance.getDistributedObjects()) {
                    if (cacheName.equals(dobject.getName())) {
                        if (dobject instanceof IMap) {
                            ((IMap<Object, Object>) dobject).clear();
                            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                            actionReport.setMessage("Cleared Cache " + cacheName);
                            return;
                        }
                    }
                }

                CacheManager cm = hazelcast.getCachingProvider().getCacheManager();
                Cache cache = cm.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                }

            } else {
                actionReport.setMessage("Hazelcast is not enabled");
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }

        } else {
            actionReport.setMessage("Hazelcast is not enabled");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

}
