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
package org.glassfish.api.admin;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.api.admin.progress.JobInfos;
import org.jvnet.hk2.annotations.Contract;

import javax.security.auth.Subject;
import javax.xml.bind.JAXBContext;

/**
 * This is the contract for the JobManagerService
 * The JobManager will be responsible for
 *  1. generating unique ids for jobs
 *  2. serving as a registry for jobs
 *  3. creating thread pools for jobs
 *  4.removing expired jobs
 *
 * @author Martin Mares
 * @author Bhakti Mehta
 */

@Contract
public interface JobManager {
    
    /** Container for checkpoint related objects
     */
    public class Checkpoint implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private Job job;
        private AdminCommand command;
        private AdminCommandContext context;

        public Checkpoint(Job job, AdminCommand command, AdminCommandContext context) {
            this.job = job;
            this.command = command;
            this.context = context;
        }

        public Job getJob() {
            return job;
        }

        public AdminCommand getCommand() {
            return command;
        }

        public AdminCommandContext getContext() {
            return context;
        }
        
    }
            

    /**
     * This method is used to generate a unique id for a managed job
     * @return returns a new id for the job
     */
    public String getNewId() ;

    /**
     * This method will register the job in the job registry
     * @param instance job to be registered
     * @throws IllegalArgumentException
     */
    public void registerJob(Job instance) throws IllegalArgumentException;

    /**
     * This method will return the list of jobs in the job registry
     * @return list of jobs
     */
    public Iterator<Job> getJobs();

    /**
     * This method is used to get a job by its id
     * @param id  The id to look up the job in the job registry
     * @return  the Job
     */
    public Job get(String id);

    /**
     * This will purge the job associated with the id from the registry
     * @param id  the id of the Job which needs to be purged
     */
    public void purgeJob(String id);


    /**
     * This will get the list of jobs from the job registry which have completed
     * @return the details of all completed jobs using JobInfos
     */
    public JobInfos getCompletedJobs(File jobs);

    /**
     * This is a convenience method to get a completed job with an id
     * @param id  the completed Job whose id needs to be looked up
     * @return the completed Job
     */
    public Object getCompletedJobForId(String id);


    /**
     * This is used to purge a completed job whose id is provided
     * @param id  the id of the Job which needs to be purged
     * @return the new list of completed jobs
     */
    public Object purgeCompletedJobForId(String id);

    /**
     * This is used to get the jobs file for a job
     * @return the location of the job file
     */
    public File getJobsFile();
    
    /** Stores current command state.
     */
    public void checkpoint(AdminCommand command, AdminCommandContext context) throws IOException;
    
    /** Stores current command state.
     */
    public void checkpoint(AdminCommandContext context, Serializable data) throws IOException;
    
    /** Load checkpoint related data.
     */
    public <T extends Serializable> T loadCheckpointData(String jobId) throws IOException, ClassNotFoundException;

}
