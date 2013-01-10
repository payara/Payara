package com.sun.enterprise.v3.admin;

import org.glassfish.api.admin.Job;
import org.glassfish.api.admin.JobCreator;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.security.auth.Subject;
import java.io.File;

/**
 * This service implements the <code>JobCreator</code> and is
 * used for creating Jobs
 * @author Bhakti Mehta
 */
@Service (name="job-creator")
public class JobCreatorService  implements JobCreator {

    @Inject
    private ServerEnvironment serverEnvironment;

    private static final String JOBS_FILE = "jobs.xml";
    /**
     * This will create a new job with the name of command and a new unused id for the job
     *
     *
     * @param scope The scope of the command or null if there is no scope
     * @param name  The name of the command
     * @return   a newly created job
     */
    @Override
    public Job createJob(String id,String scope, String name, Subject subject,boolean isManagedJob) {
        AdminCommandInstanceImpl job = null;
        if (isManagedJob) {
            job =  new AdminCommandInstanceImpl(id,name,scope,subject,true);
            job.setJobsFile(getJobsFile());
        } else {
            job =  new AdminCommandInstanceImpl(name,scope,subject,false);
        }
        return job;
    }

    /**
     *  This returns the jobs file for commands
     * @return the location of the jobs.xml file
     */
    public File getJobsFile() {
        return
        new File(serverEnvironment.getConfigDirPath(),JOBS_FILE);
    }
}
