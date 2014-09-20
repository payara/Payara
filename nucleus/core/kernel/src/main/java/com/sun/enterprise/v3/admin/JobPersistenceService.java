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

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.api.admin.progress.JobInfos;
import org.glassfish.api.admin.progress.JobPersistence;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;


/**
 * This service persists information for managed jobs to the file
 * @author Bhakti Mehta
 */
@Service(name="job-persistence")
public class JobPersistenceService implements JobPersistence {

    protected Marshaller jaxbMarshaller ;

    protected Unmarshaller jaxbUnmarshaller;

    protected JobInfos jobInfos;

    @Inject
    private JobManagerService jobManager;

    protected JAXBContext jaxbContext;

    protected final static Logger logger = KernelLoggerInfo.getLogger();


    protected static final LocalStringManagerImpl adminStrings =
            new LocalStringManagerImpl(JobPersistenceService.class);
    @Override
    public  void persist(Object obj) {
        JobInfo jobInfo = (JobInfo)obj;

        jobInfos = jobManager.getCompletedJobs(jobManager.getJobsFile());

        doPersist(jobInfos,jobInfo);

    }

    public void doPersist(JobInfos jobInfos, JobInfo jobInfo) {
        File file = jobInfo.getJobsFile();
        synchronized (file) {

            if (jobInfos == null) {
                jobInfos = new JobInfos();
            }

            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(JobInfos.class);
                jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                CopyOnWriteArrayList<JobInfo> jobList = new CopyOnWriteArrayList<JobInfo>(jobInfos.getJobInfoList());
                jobInfos.setJobInfoList(jobList);
                jobList.add(jobInfo);
                jaxbMarshaller.marshal(jobInfos, file);
                jobManager.addToCompletedJobs(new CompletedJob(jobInfo.jobId,jobInfo.commandCompletionDate,jobInfo.getJobsFile()));
                jobManager.purgeJob(jobInfo.jobId);

            } catch (JAXBException e) {
                throw new RuntimeException(adminStrings.getLocalString("error.persisting.jobs","Error while persisting jobs",jobInfo.jobId,e.getLocalizedMessage()),e);

            }
        }
    }




}
