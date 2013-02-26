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
import com.sun.enterprise.v3.server.ExecutorServiceFactory;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.glassfish.api.admin.AdminCommandState;
import org.glassfish.api.admin.Job;
import org.glassfish.api.admin.JobManager;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.api.admin.progress.JobInfos;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;
import javax.xml.bind.Marshaller;
import org.glassfish.api.logging.LogLevel;

/**
 *  This is the implementation for the JobManagerService
 *  The JobManager is responsible
 *  1. generating unique ids for jobs
 *  2. serving as a registry for jobs
 *  3. creating threadpools for jobs
 *  4.removing expired jobs
 *
 * @author Martin Mares
 * @author Bhakti Mehta
 */

@Service
@Singleton
public class JobManagerService implements JobManager,PostConstruct {


    @Inject
    private Domain domain;

    private ManagedJobConfig managedJobConfig;
    
    private static final int MAX_SIZE = 65535;
    
    private ConcurrentHashMap<String, Job> jobRegistry = new ConcurrentHashMap<String, Job>();

    private AtomicInteger lastId = new AtomicInteger(0);

    protected static final LocalStringManagerImpl adminStrings =
            new LocalStringManagerImpl(JobManagerService.class);

    private final static Logger logger = KernelLoggerInfo.getLogger();

     private ExecutorService pool;

     @Inject
     private ExecutorServiceFactory executorFactory;
     
    @Inject
    private ServerEnvironment serverEnvironment;

    private final String JOBS_FILE = "jobs.xml";

    protected JAXBContext jaxbContext;

    protected File jobsFile;


    /**
     * This will return a new id which is unused
     * @return
     */
    public synchronized String getNewId() {
        int nextId = lastId.incrementAndGet();
        if (nextId > MAX_SIZE) {
            reset();
        }
        String nextIdToUse = String.valueOf(nextId);
        return !idInUse(nextIdToUse) ? String.valueOf(nextId): getNewId();
    }

    public JobInfo getCompletedJobForId(String id) {
        for (JobInfo jobInfo: getCompletedJobs().getJobInfoList()) {
            if (jobInfo.jobId.equals(id)) {
                return jobInfo;
            }

        }
        return null;
    }


    /**
     * This resets the id to 0
     */
    private void reset() {
        lastId.set(0);
    }

    /**
     * This method will return if the id is in use
     * @param id
     * @return true if id is in use
     */
    private boolean idInUse(String id) {

        if (jobRegistry.containsKey(id)) {
            return true;
        }
        if (getCompletedJobs() != null) {
            for (JobInfo job : getCompletedJobs().getJobInfoList())  {
                if ((job.jobId).equals(id)) {
                    return true;
                }

            }
        }
        return false;
    }



    /**
     * This adds the jobs
     * @param instance
     * @throws IllegalArgumentException
     */
    @Override
    public synchronized void registerJob(Job instance) throws IllegalArgumentException {
        if (instance == null) {
            throw new IllegalArgumentException(adminStrings.getLocalString("job.cannot.be.null","Job cannot be null"));
        }
        if (jobRegistry.containsKey(instance.getId())) {
            throw new IllegalArgumentException(adminStrings.getLocalString("job.id.in.use","Job id is already in use."));
        }

        jobRegistry.put(instance.getId(), instance);

        if (instance instanceof AdminCommandInstanceImpl) {
            ((AdminCommandInstanceImpl) instance).setState(AdminCommandState.State.RUNNING);
        }
    }

    /**
     * This returns all the jobs in the registry
     * @return   The iterator of jobs
     */
    @Override
    public Iterator<Job> getJobs() {
        return jobRegistry.values().iterator();
    }

    /**
     * This will return a job associated with the id
     * @param id  The job whose id matches
     * @return
     */
    @Override
    public Job get(String id) {
        return jobRegistry.get(id);
    }

    /**
     * This will return a list of jobs which have crossed the JOBS_RETENTION_PERIOD
     * and need to be purged
     * @return  list of jobs to be purged
     */
    public synchronized ArrayList<JobInfo> getExpiredJobs() {
        ArrayList<JobInfo> expiredJobs = new ArrayList<JobInfo>();
        JobInfos jobInfos = getCompletedJobs();
        for(JobInfo job:jobInfos.getJobInfoList()) {

            long executedTime = job.commandExecutionDate;
            long currentTime = System.currentTimeMillis();

            long jobsRetentionPeriod = 86400000;


            managedJobConfig = domain.getExtensionByType(ManagedJobConfig.class);
            jobsRetentionPeriod = convert(managedJobConfig.getJobRetentionPeriod());

            if (currentTime - executedTime > jobsRetentionPeriod &&
                    job.state.equals(AdminCommandState.State.COMPLETED.name())) {
                 expiredJobs.add(job);
            }

        }
        return expiredJobs;
    }

    public long convert(String input ) {
        String period = input.substring(0,input.length()-1);
        Long timeInterval = new Long(period);
        String s = input.toLowerCase(Locale.US);
        long milliseconds = 86400000;
        if (s.indexOf("s") > 0 ) {
            milliseconds = timeInterval*1000;
        }
        else if (s.indexOf("h") > 0 ) {
            milliseconds = timeInterval*3600*1000;

        }
        else if (s.indexOf("m") > 0 ) {
            milliseconds = timeInterval*60*1000;
        }
        return milliseconds;
    }


    /**
     * This will remove the job from the registry
     * @param id  The job id of the job to be removed
     */
    @Override
    public synchronized void purgeJob(String id) {
        Job obj = jobRegistry.remove(id);

        logger.fine(adminStrings.getLocalString("removed.expired.job","Removed expired job ",  obj));

    }

    public ExecutorService getThreadPool() {
        return pool ;
    }


    /**
     * This will load the jobs which have already completed
     * and persisted in the jobs.xml
     * @return JobsInfos which contains information about completed jobs
     */
    @Override
    public  JobInfos getCompletedJobs() {
        synchronized (jobsFile) {
            try {
                if (jaxbContext == null)
                    jaxbContext = JAXBContext.newInstance(JobInfos.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                if (jobsFile != null && jobsFile.exists())  {
                    JobInfos jobInfos = (JobInfos)unmarshaller.unmarshal(jobsFile);
                    return jobInfos;
                }
            } catch (JAXBException e) {
                throw new RuntimeException(adminStrings.getLocalString("error.reading.completed.jobs","Error reading completed jobs ",  e.getLocalizedMessage()), e);
            }
            return null;
        }
    }

    /**
     * This method looks for the completed jobs
     * and purges a job which is marked with the jobId
     * @param jobId the job to purge
     * @return  the new list of completed jobs
     */
    @Override
    public  JobInfos purgeCompletedJobForId(String jobId) {
        JobInfos completedJobInfos = getCompletedJobs();
        synchronized (jobsFile) {
            CopyOnWriteArrayList<JobInfo> jobList = new CopyOnWriteArrayList<JobInfo>();

            if (completedJobInfos != null)   {
                jobList.addAll(completedJobInfos.getJobInfoList());

                for (JobInfo jobInfo: jobList ) {
                    if (jobInfo.jobId.equals(jobId)) {
                        jobList.remove(jobInfo);
                    }

                }
            }

            JobInfos jobInfos = new JobInfos();
            try {
                if (jaxbContext == null)
                    jaxbContext = JAXBContext.newInstance(JobInfos.class);

                jobInfos.setJobInfoList(jobList);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.marshal(jobInfos, jobsFile);
            } catch (JAXBException e) {
                throw new RuntimeException(adminStrings.getLocalString("error.purging.completed.job","Error purging completed job ", jobId,e.getLocalizedMessage()), e);
            }
            return jobInfos;
        }

    }


    @Override
    public void postConstruct() {
        jobsFile =
                new File(serverEnvironment.getConfigDirPath(),JOBS_FILE);
        pool = executorFactory.provide();
    }

    @Override
    public File getJobsFile() {
        return jobsFile;
    }
}
