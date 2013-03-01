package org.glassfish.api.admin;

import org.jvnet.hk2.annotations.Contract;

import java.io.File;
import java.util.List;

/**
 * This is a contract which is used to locate all the jobs.xml files
 * on server startup
 * @author Bhakti Mehta
 */
@Contract
public interface JobLocator {

    /**
     * This method checks if there any any persisted and completed jobs
     * @return A list of the job files located in the system
     */
    public List<File> locateJobXmlFiles();
}
