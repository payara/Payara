package org.glassfish.api.admin;

import org.jvnet.hk2.annotations.Contract;

import javax.security.auth.Subject;

/**
 * This is the contract responsible for creating Job
 * @author Bhakti Mehta
 */
@Contract
public interface JobCreator {

    public Job createJob(String id,String scope , String name, Subject subject, boolean isManagedJob);

}

