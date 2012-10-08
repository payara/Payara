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
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.config.serverbeans.Domain ;
import com.sun.enterprise.config.serverbeans.ManagedJobConfig;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.Job;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.PostStartupRunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import javax.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This is an hk2 service which will clear all expired and inactive jobs
 * @author Bhakti Mehta
 */
@Service
@RunLevel(value= StartupRunLevel.VAL)
public class JobCleanUpService implements PostConstruct,ConfigListener {

    @Inject
    JobManagerService jobManagerService;

    @Inject
    Domain domain;

    private ManagedJobConfig managedJobConfig;

    private final static Logger logger = LogDomains.getLogger(JobCleanUpService.class, LogDomains.ADMIN_LOGGER);

    boolean enableJobManager = false;

    private ScheduledExecutorService scheduler;


    private static final LocalStringManagerImpl adminStrings =
            new LocalStringManagerImpl(JobCleanUpService.class);
    @Override
    public void postConstruct() {
        logger.fine(adminStrings.getLocalString("jobcleanup.service.init", "Initializing Job Cleanup service"));
        enableJobManager = Boolean.parseBoolean(System.getProperty("enableJobManager","false"));
        if (enableJobManager)  {
            managedJobConfig = domain.getExtensionByType(ManagedJobConfig.class);
            ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(managedJobConfig);
            logger.fine(adminStrings.getLocalString("init.managed.config.bean", "Initializing ManagedJobConfig bean"));
            bean.addListener(this);
        }

        scheduler = Executors.newScheduledThreadPool(10, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread result = new Thread(r);
                result.setDaemon(true);
                return result;
            }
        });
        scheduleCleanUp();
    }


    /**
     * This will schedule a cleanup of expired jobs based on configurable values
     */
    private void scheduleCleanUp() {
        enableJobManager = Boolean.parseBoolean(System.getProperty("enableJobManager","false"));
        logger.fine(adminStrings.getLocalString("scheduling.cleanup", "Scheduling cleanup"));
        //default values to 20 minutes for delayBetweenRuns and initialDelay
        long delayBetweenRuns = 1200000;
        long initialDelay = 1200000;
        if (enableJobManager) {
            delayBetweenRuns = jobManagerService.convert(managedJobConfig.getPollInterval());
            initialDelay = jobManagerService.convert(managedJobConfig.getInitialDelay());
        }

        ScheduledFuture<?> cleanupFuture = scheduler.scheduleAtFixedRate(new JobCleanUpTask(),initialDelay,delayBetweenRuns,TimeUnit.MILLISECONDS);

    }

    /**
     * This method is notified for any changes in job-inactivity-limit or
     * job-retention-period or persist, initial-delay or poll-interval option in
     * ManagedJobConfig. Any change results
     * in the job cleanup service to change the behaviour
     * being updated.
     * @param events the configuration change events.
     * @return the unprocessed change events.
     */
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(events, new PropertyChangeHandler(), logger);
    }


    private  final class JobCleanUpTask implements Runnable {
        public void run() {
            try {
                logger.fine(adminStrings.getLocalString("cleaning.jobs","Cleaning jobs"));
                cleanUpExpiredJobs();
            } catch (Exception e ) {
                throw new RuntimeException(adminStrings.getLocalString("error.cleaning.jobs","Error while cleaning jobs" +e));
            }

        }

    }

    /**
     * This will periodically purge expired jobs
     */
    private void cleanUpExpiredJobs() {
        ArrayList<Job> expiredJobs = jobManagerService.getExpiredJobs();
        if (expiredJobs.size() > 0 ) {
            for (Job job: expiredJobs) {
                jobManagerService.purgeJob(job.getId());
                logger.fine("Cleaning job" + job.getId());
            }
        }

    }


    class PropertyChangeHandler implements Changed {

        @Override
        public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {
            NotProcessed np = null;
            switch (type) {
                case CHANGE:
                    if(logger.isLoggable(Level.FINE)) {
                        logger.fine("ManagedJobConfig " + changedType.getName() + " was changed : " + changedInstance);
                    }
                    np = handleChangeEvent(changedInstance);
                    break;
                default:
            }
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleChangeEvent(T instance) {
            scheduleCleanUp();
            return null;
        }
    }
}



