/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ManagedJobConfig;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import org.glassfish.kernel.KernelLoggerInfo;



/**
 * This command manages configured jobs
 * Managed jobs are commands which are annotated with @ManagedJob ,@Progress
 * or running with --detach
 * You can configure the job retention period, job inactivity period,initial-delay,poll-interval
 * persisting options for those jobs which will be used by the Job Manager
 * to purge the jobs according to the criteria specified.
 * Definition of parameters:
 * job-retention-period - Time period to store the jobs. Defaults 24 hours.
 *
 * job-inactivity-period  -Time period after which we expire an inactive, non responsive command
 *
 * initial-delay - Initial delay after which the cleanup service should start purging
 * This is useful when the server restarts will provide some time for the Job Manager to
 * bootstrap
 *
 * poll-interval - The time interval after which the JobCleanupService should poll for expired jobs
 *

 *
 * @author Bhakti Mehta
 */
@Service(name = "configure-managed-jobs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@AccessRequired(resource="domain/managed-job-config", action="update")
public class ConfigureManagedJobs implements AdminCommand {

    @Inject
    Domain domain;

    @Param(name="in-memory-retention-period", optional=true)
    String inMemoryRetentionPeriod;

    @Param(name="job-retention-period", optional=true)
    String jobRetentionPeriod;

    @Param(name="cleanup-initial-delay", optional=true)
    String initialDelay;

    @Param(name="cleanup-poll-interval", optional=true)
    String pollInterval;



    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.getLogger();

        ManagedJobConfig managedJobConfig = domain.getExtensionByType(ManagedJobConfig.class);
        if (managedJobConfig == null ) {
           logger.warning(KernelLoggerInfo.getFailManagedJobConfig);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(KernelLoggerInfo.getLogger().getResourceBundle().getString(KernelLoggerInfo.getFailManagedJobConfig));
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<ManagedJobConfig>() {

                @Override
                public Object run(ManagedJobConfig param) throws PropertyVetoException, TransactionFailure {

                    if (inMemoryRetentionPeriod != null)
                        param.setInMemoryRetentionPeriod(inMemoryRetentionPeriod);
                    if (jobRetentionPeriod != null)
                        param.setJobRetentionPeriod(jobRetentionPeriod);
                    if (pollInterval != null)
                        param.setPollInterval(pollInterval);
                    if (initialDelay != null)
                        param.setInitialDelay(initialDelay);

                    return param;
                }
            }, managedJobConfig);

        } catch(TransactionFailure e) {
            logger.warning(KernelLoggerInfo.configFailManagedJobConfig);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }

    }

}



