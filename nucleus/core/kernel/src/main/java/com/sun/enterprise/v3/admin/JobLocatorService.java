package com.sun.enterprise.v3.admin;

import org.glassfish.api.StartupRunLevel;

import org.glassfish.api.admin.JobLocator;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This service will look for completed jobs from the jobs.xml
 * files and load the information
 *
 * @author Bhakti Mehta
 */
@Service(name = "job-locator")
public class JobLocatorService implements JobLocator {

    protected List<File> jobFiles = (List<File>) Collections.synchronizedList(new ArrayList<File>()) ;

    @Override
    public List<File> locateJobXmlFiles() {
        return jobFiles;

    }


    public void addFile(File file) {
        jobFiles.add(file);
    }
}
