/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.executorservice.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.executorservice.PayaraExecutorServiceConfiguration;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "set-payara-executor-service-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean = PayaraExecutorServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-payara-executor-service-configuration",
            description = "Sets the Payara Executor Service Configuration")
})
public class SetPayaraExecutorServiceConfigurationCommand implements AdminCommand {

    @Inject
    private Target targetUtil;
    
    @Inject
    ServerEnvironment serverEnvironment;
    
    @Inject
    PayaraExecutorService payaraExecutorService;
    
    @Param(name = "threadPoolExecutorCorePoolSize", optional = true, alias = "threadpoolexecutorcorepoolsize")
    private Integer threadPoolExecutorCorePoolSize;
    
    @Param(name = "threadPoolExecutorMaxPoolSize", optional = true, alias = "threadpoolexecutormaxpoolsize")
    private Integer threadPoolExecutorMaxPoolSize;
    
    @Param(name = "threadPoolExecutorKeepAliveTime", optional = true, alias = "threadpoolexecutorkeepalivetime")
    @Min(value = 1, message = "Keep alive time must be greater than 1")
    private String threadPoolExecutorKeepAliveTime;
    
    @Param(name = "threadPoolExecutorKeepAliveTimeUnit", optional = true, alias = "threadpoolexecutorkeepalivetimeunit",
            acceptableValues = "days,DAYS,hours,HOURS,microseconds,MICROSECONDS,milliseconds,MILLISECONDS,minutes,"
                    + "MINUTES,nanoseconds,NANOSECONDS,seconds,SECONDS")
    private String threadPoolExecutorKeepAliveTimeUnit;
    
    @Param(name = "threadPoolExecutorQueueSize", optional = true, alias = "threadpoolexecutorqueuesize")
    private Integer threadPoolExecutorQueueSize;
    
    @Param(name = "scheduledThreadPoolExecutorCorePoolSize", optional = true, alias = "scheduledthreadpoolexecutorcorepoolsize")
    private Integer scheduledThreadPoolExecutorCorePoolSize;
    
    @Param(name = "target", optional = true, defaultValue = "server-config")
    private String target;
    
    @Override
    public void execute(AdminCommandContext acc) {
        Config configVal = targetUtil.getConfig(target);
        PayaraExecutorServiceConfiguration payaraExecutorServiceConfiguration = configVal.getExtensionByType(
                PayaraExecutorServiceConfiguration.class);
        if (payaraExecutorServiceConfiguration != null) {
            try {
                ConfigSupport.apply((PayaraExecutorServiceConfiguration config) -> {
                    if (threadPoolExecutorCorePoolSize != null) {
                        config.setThreadPoolExecutorCorePoolSize(threadPoolExecutorCorePoolSize);
                    }
                    
                    if (threadPoolExecutorMaxPoolSize != null) {
                        config.setThreadPoolExecutorMaxPoolSize(threadPoolExecutorMaxPoolSize);
                    }
                    
                    if (threadPoolExecutorKeepAliveTime != null) {
                        config.setThreadPoolExecutorKeepAliveTime(threadPoolExecutorKeepAliveTime);
                    }
                    
                    if (threadPoolExecutorKeepAliveTimeUnit != null) {
                        config.setThreadPoolExecutorKeepAliveTimeUnit(
                                threadPoolExecutorKeepAliveTimeUnit.toUpperCase());
                    }
                    
                    if (threadPoolExecutorQueueSize != null) {
                        config.setThreadPoolExecutorQueueSize(threadPoolExecutorQueueSize);
                    }
                    
                    if (scheduledThreadPoolExecutorCorePoolSize != null) {
                        config.setScheduledThreadPoolExecutorCorePoolSize(scheduledThreadPoolExecutorCorePoolSize);
                    }
                    
                    return null;
                }, payaraExecutorServiceConfiguration);
            } catch (TransactionFailure ex) {
                acc.getActionReport().failure(Logger.getLogger(
                        SetPayaraExecutorServiceConfigurationCommand.class.getName()), 
                        "Failed to set executor service configuration", ex);
            }
        }
    }   
}
