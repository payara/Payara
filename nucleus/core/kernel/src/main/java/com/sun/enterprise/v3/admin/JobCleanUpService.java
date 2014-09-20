/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ManagedJobConfig;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import javax.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This is an hk2 service which will clear all expired and inactive jobs
 * @author Bhakti Mehta
 */
@Service(name="job-cleanup")
@RunLevel(value= StartupRunLevel.VAL)
public class JobCleanUpService implements PostConstruct,ConfigListener {

    @Inject
    JobManagerService jobManagerService;

    @Inject
    Domain domain;

    private ManagedJobConfig managedJobConfig;

    private final static Logger logger = KernelLoggerInfo.getLogger();

    private ScheduledExecutorService scheduler;


    private static final LocalStringManagerImpl adminStrings =
            new LocalStringManagerImpl(JobCleanUpService.class);



    @Override
    public void postConstruct() {
        logger.log(Level.FINE,KernelLoggerInfo.initializingJobCleanup);

        managedJobConfig = domain.getExtensionByType(ManagedJobConfig.class);
        ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(managedJobConfig);
        logger.fine(KernelLoggerInfo.initializingManagedConfigBean);
        bean.addListener(this);


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


        logger.fine(KernelLoggerInfo.schedulingCleanup);
        //default values to 20 minutes for delayBetweenRuns and initialDelay
        long delayBetweenRuns = 1200000;
        long initialDelay = 1200000;

        delayBetweenRuns = jobManagerService.convert(managedJobConfig.getPollInterval());
        initialDelay = jobManagerService.convert(managedJobConfig.getInitialDelay());


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
                //This can have data  when server starts up  initially or as jobs complete
                ConcurrentHashMap<String,CompletedJob> completedJobsMap = jobManagerService.getCompletedJobsInfo();

                Iterator<CompletedJob> completedJobs = new HashSet<CompletedJob>(completedJobsMap.values()).iterator();
                while (completedJobs.hasNext()   ) {
                    CompletedJob completedJob = completedJobs.next();

                    logger.log(Level.FINE,KernelLoggerInfo.cleaningJob, new Object[]{completedJob.getId()});

                    cleanUpExpiredJobs(completedJob.getJobsFile());
                }
            } catch (Exception e ) {
                throw new RuntimeException(KernelLoggerInfo.exceptionCleaningJobs,e);
            }

        }


    }

    /**
     * This will periodically purge expired jobs
     */
    private void cleanUpExpiredJobs(File file) {
        ArrayList<JobInfo> expiredJobs = jobManagerService.getExpiredJobs(file);
        if (expiredJobs.size() > 0 ) {
            for (JobInfo job: expiredJobs) {
                //remove from Job registy
                jobManagerService.purgeJob(job.jobId);
                //remove from jobs.xml file
                jobManagerService.purgeCompletedJobForId(job.jobId,file);
                //remove from local cache for completed jobs
                jobManagerService.removeFromCompletedJobs(job.jobId);
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,KernelLoggerInfo.cleaningJob, job.jobId);
                }
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

                       logger.log(Level.FINE, KernelLoggerInfo.changeManagedJobConfig, new Object[]{
                               changedType.getName()
                       ,changedInstance.toString()});
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



