/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;

/**
 * This command manages configured jobs
 * Managed jobs are commands which are annotated with @ManagedJob
 * or running with --detach
 * You can configure the job retention period, job inactivity period
 * persisting options for those jobs which will be used by the Job Manager
 * to purge the jobs according to the criteria specified
 *
 * @author Bhakti Mehta
 */
@Service(name = "configure-managed-jobs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
public class ConfigureManagedJobs implements AdminCommand {

    @Inject
    Domain domain;

    @Param(name="job-inactivity-limit", optional=true)
    String jobInactivityLimit;

    @Param(name="job-retention-period", optional=true)
    String jobRetentionPeriod;

    @Param(name="persist", optional=true, defaultValue="false")
    boolean persist;


    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.getLogger();

        ManagedJobConfig managedJobConfig = domain.getExtensionByType(ManagedJobConfig.class);
        if (managedJobConfig == null ) {
            String msg = "unable.to.get.ManagedJobConfig";
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<ManagedJobConfig>() {

                @Override
                public Object run(ManagedJobConfig param) throws PropertyVetoException, TransactionFailure {

                    if (jobInactivityLimit != null)
                        param.setJobInactivityLimitInHours(jobInactivityLimit);
                    if (jobRetentionPeriod != null)
                        param.setJobRetentionPeriodInHours(jobRetentionPeriod);
                    if (persist)
                        param.setPersistingEnabled(persist);

                    return param;
                }
            }, managedJobConfig);

        } catch(TransactionFailure e) {
            logger.warning("failed.to.configure.ManagedJobConfig" );
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }

    }

}
