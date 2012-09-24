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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ManagedJobConfig;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.Job;
import org.glassfish.api.admin.JobManager;
import org.glassfish.api.admin.AdminCommandState;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

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
public class JobManagerService implements JobManager {
    @Inject
    Domain domain;

    private ManagedJobConfig managedJobConfig;
    
    private static final int MAX_SIZE = 65535;
    
    private HashMap<String, Job> jobRegistry = new HashMap<String, Job>();

    private AtomicInteger lastId = new AtomicInteger(0);

    private static final LocalStringManagerImpl adminStrings =
            new LocalStringManagerImpl(JobManagerService.class);

    private final static Logger logger = LogDomains.getLogger(JobManagerService.class, LogDomains.ADMIN_LOGGER);


    /**
     * This will return a new id which is unused
     * @return
     */
    protected synchronized String getNewId() {
        int nextId = lastId.incrementAndGet();
        if (nextId > MAX_SIZE) {
            reset();
        }
        String nextIdToUse = String.valueOf(nextId);
        return !idInUse(nextIdToUse) ? String.valueOf(nextId): getNewId();
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
        return jobRegistry.containsKey(id) ;
    }

    /**
     * This will create a new job with the name of command and a new unused id for the job
     * @param name  The name of the command
     * @return   a newly created job
     */
    @Override
    public Job createJob(String name,boolean isManagedJob) {
        if (isManagedJob) {
            return new AdminCommandInstanceImpl(getNewId(),name);
        } else {
            return new AdminCommandInstanceImpl(name);
        }
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
    public ArrayList<Job> getExpiredJobs() {
        ArrayList expiredJobs = new ArrayList();
        Iterator<Job> jobs = getJobs();
        while ( jobs.hasNext()) {
            Job job = jobs.next();
            long executedTime = job.getCommandExecutionDate();
            long currentTime = System.currentTimeMillis();

            long jobsRetentionPeriod = 86400000;
            boolean enableJobManager = Boolean.parseBoolean(System.getProperty("enableJobManager"));
            if (enableJobManager)  {
                managedJobConfig = domain.getExtensionByType(ManagedJobConfig.class);
                jobsRetentionPeriod = convert(managedJobConfig.getJobRetentionPeriod());
            }
            if (currentTime - executedTime > jobsRetentionPeriod) {
                 expiredJobs.add(job);
            }

        }
        return expiredJobs;
    }

    public long convert(String input ) {
        String period = input.substring(0,input.length()-1);
        Long timeInterval = new Long(period);
        String s = input.toLowerCase();
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
}
