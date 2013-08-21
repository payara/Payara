/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AuthorizationPreprocessor;
import org.glassfish.api.admin.Job;
import org.glassfish.api.admin.progress.JobInfo;
import org.jvnet.hk2.annotations.Service;

/**
 * Attaches a user attribute to job resources for authorization.
 * 
 * @author Tim Quinn
 * @author Bhakti Mehta
 */
@Service
@Singleton
public class JobAuthorizationAttributeProcessor implements AuthorizationPreprocessor {

    private final static String USER_ATTRIBUTE_NAME = "user";
    
    public final static String JOB_RESOURCE_NAME_PREFIX_NO_SLASH = "jobs/job";
    public final static String JOB_RESOURCE_NAME_PREFIX = JOB_RESOURCE_NAME_PREFIX_NO_SLASH + '/';
    
    public final static Pattern JOB_PATTERN = Pattern.compile("(?:" + JOB_RESOURCE_NAME_PREFIX_NO_SLASH + "(?:/(\\d*))?)");
    
    @Inject
    private JobManagerService jobManager;
    
    @Override
    public void describeAuthorization(Subject subject, String resourceName, String action, AdminCommand command, Map<String, Object> context, Map<String, String> subjectAttributes, Map<String, String> resourceAttributes, Map<String, String> actionAttributes) {
        final Matcher m = JOB_PATTERN.matcher(resourceName);
        if ( ! m.matches()) {
            return;
        }
        if (m.groupCount() == 0) {
            /*
             * The resource name pattern did not match for including a job ID, 
             * so we will not be able to attach a user attribute to the resource.
             */
            return;
        }
        final String jobID = m.group(1);
        final Job job = jobManager.get(jobID);
        String userID = null;
        
        /*
         * This logic might run before any validation in the command has run,
         * in which case the job ID would be invalid and the job manager and/or
         * the completed jobs store might not know about the job.
         */
        if (job != null && job.getSubjectUsernames().size() > 0) {
            userID = job.getSubjectUsernames().get(0);
        } else {
            if (jobManager.getCompletedJobs(jobManager.getJobsFile()) != null) {
                    final JobInfo jobInfo = (JobInfo) jobManager.getCompletedJobForId(jobID);
                    if (jobInfo != null) {
                        userID = jobInfo.user;
                    }
                }
        } 
            
        if (userID != null) {
            resourceAttributes.put(USER_ATTRIBUTE_NAME, userID);
        }
    }
}
