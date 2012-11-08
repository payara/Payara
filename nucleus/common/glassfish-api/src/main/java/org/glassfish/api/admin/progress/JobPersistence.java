package org.glassfish.api.admin.progress;


import org.jvnet.hk2.annotations.Contract;

/**
 *  A contract to persist jobs related information to files
 *
 **/

@Contract
public interface JobPersistence {

    public void persist(JobInfo jobInfo);

}
