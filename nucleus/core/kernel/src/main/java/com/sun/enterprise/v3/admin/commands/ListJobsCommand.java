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
package com.sun.enterprise.v3.admin.commands;
import com.sun.enterprise.admin.progress.ProgressStatusClient;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.v3.admin.JobAuthorizationAttributeProcessor;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Collection;
import java.util.Date;
import javax.inject.Inject;

import com.sun.enterprise.v3.admin.JobManagerService;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.api.admin.progress.JobInfos;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.security.services.common.SubjectUtil;
import org.jvnet.hk2.annotations.Service;


/**
 * This command will list the jobs related information
 * Currently it prints the jobId, name, time of execution,user and the state
 *
 * @author Bhakti Mehta
 */
@Service(name="list-jobs")
@PerLookup
@I18n("list-jobs")
public class ListJobsCommand implements AdminCommand,AdminCommandSecurity.AccessCheckProvider {
    
    private ActionReport report;
    private static final String DEFAULT_USER_STRING = "-";

    @Inject
    private JobManagerService jobManagerService;

    /**
     * Associates an access check with each candidate JobInfo we might report on.
     */
    private final Collection<AccessRequired.AccessCheck<JobInfo>> jobAccessChecks = new ArrayList<AccessRequired.AccessCheck<JobInfo>>();
    
    private final String JOBS_FILE = "jobs.xml";

    @Param(optional = true, primary = true)
    String jobID;

    @Inject
    private ServerEnvironment serverEnvironment;

    protected static final String TITLE_NAME = "NAME";
    protected static final String TITLE_JOBID = "JOB ID";
    protected static final String TITLE_TIME = "TIME";
    protected static final String TITLE_STATE = "STATE";
    protected static final String TITLE_EXITCODE = "EXIT CODE";
    protected static final String TITLE_USER = "USER";
    protected static final String TITLE_NONE = "Nothing to list.";
    public static final String NAME = "jobName";
    public static final String ID = "jobId";
    public static final String DATE = "executionDate";
    public static final String CODE = "exitCode";
    public static final String USER = "user";
    public static final String STATE = "jobState";
    public static final String MESSAGE = "message";
    public static final String COMPLETION_DATE = "completionDate";


    final private static StringManager localStrings =
            StringManager.getManager(ListJobsCommand.class);

    protected JobInfos getCompletedJobs() {
        return jobManagerService.getCompletedJobs(jobManagerService.getJobsFile());
    }
    
    protected JobInfo getCompletedJobForId(final String jobID) {
        return (JobInfo) jobManagerService.getCompletedJobForId(jobID);
    }
    
    protected boolean isSingleJobOK(final Job singleJob) {
        return (singleJob != null);
    }
    
    protected boolean isJobEligible(final Job job) {
        return !skipJob(job.getName()) && checkScope(job);
    }

    protected boolean checkScope(Job job) {
        return job.getScope()==null;
    }
    
    private List<JobInfo> chooseJobs() {
        List<JobInfo> jobsToReport = new ArrayList<JobInfo>();

        if (jobID != null) {
            Job oneJob = jobManagerService.get(jobID);
            JobInfo info = null;

            if (isSingleJobOK(oneJob)) {
                List<String> userList =  oneJob.getSubjectUsernames();
                ActionReport actionReport = oneJob.getActionReport();
                String message = actionReport == null ? "" : actionReport.getMessage();

                if (!StringUtils.ok(message)) {
                    message = ProgressStatusClient.composeMessageForPrint(oneJob.getCommandProgress());
                }
                String exitCode =  actionReport == null ? "" : actionReport.getActionExitCode().name();
                info = new JobInfo(oneJob.getId(),oneJob.getName(),oneJob.getCommandExecutionDate(),exitCode,userList.get(0),message,oneJob.getJobsFile(),oneJob.getState().name(),0);

            }  else {
                if (getCompletedJobs() != null) {
                    info = getCompletedJobForId(jobID);
                }
            }

          if (info != null && !skipJob(info.jobName)) {
              jobsToReport.add(info);
          }

        }  else {

            for (Iterator<Job> iterator = jobManagerService.getJobs(); iterator.hasNext(); ) {
                Job job = iterator.next();
                if (isJobEligible(job)) {
                    List<String> userList =  job.getSubjectUsernames();
                    ActionReport actionReport = job.getActionReport();

                    String message = actionReport == null ? "" : actionReport.getMessage();
                    if (!StringUtils.ok(message)) {
                        message = ProgressStatusClient.composeMessageForPrint(job.getCommandProgress());
                    }
                    String exitCode = actionReport == null ? "" : actionReport.getActionExitCode().name();

                    String user = DEFAULT_USER_STRING;
                    if(userList.size() > 0){
                        user = userList.get(0);
                    }
                    jobsToReport.add(new JobInfo(job.getId(),job.getName(),job.getCommandExecutionDate(),exitCode,user,message,job.getJobsFile(),job.getState().name(),0));
                }
            }

            JobInfos completedJobs = getCompletedJobs();
            if (completedJobs != null ) {
                for (JobInfo info : completedJobs.getJobInfoList()) {
                    if (!skipJob(info.jobName)) {
                        jobsToReport.add(info);
                    }
                }
            }
        }
        return jobsToReport;
    }
        
    @Override
    public void execute(AdminCommandContext context) {
        display(AccessRequired.AccessCheck.relatedObjects(jobAccessChecks),context);
    }
    
    public static boolean skipJob(String name) {
        return name == null || "attach".equals(name) || name.startsWith("_");
    }


    @Override
    public Collection<? extends AccessRequired.AccessCheck> getAccessChecks() {
        final List<JobInfo> jobInfoList = chooseJobs();
        for (JobInfo jobInfo : jobInfoList) {
            jobAccessChecks.add(new AccessRequired.AccessCheck<JobInfo>(jobInfo,
                    JobAuthorizationAttributeProcessor.JOB_RESOURCE_NAME_PREFIX + jobInfo.jobId,"read", false));
        }
        return jobAccessChecks;
    }

    public void display(Collection<JobInfo> jobInfoList, AdminCommandContext context) {
        report = context.getActionReport();


        int longestName = TITLE_NAME.length();
        int longestJobId = TITLE_JOBID.length();
        int longestTime = TITLE_TIME.length();
        int longestState = TITLE_STATE.length();
        int longestUser = TITLE_USER.length();
        int longestExitCode = TITLE_EXITCODE.length();

        for (JobInfo job :jobInfoList) {
                   int  jobId = job.jobId.length();
                   int time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(job.commandExecutionDate).length();
                   int name = job.jobName.length();
                   int state = job.state.length();
                   int user ;
                    if(job.user != null){
                        user = job.user.length();
                    }else{
                        user = DEFAULT_USER_STRING.length();
                    }
                   int exitCode = job.exitCode.length();

                   if (name > longestName)
                       longestName = name;
                   if (time > longestTime)
                       longestTime = time;
                   if (jobId > longestJobId)
                       longestJobId = jobId;
                   if (state> longestState)
                       longestState = state;
                   if (user > longestUser)
                       longestUser = user;
                   if (exitCode> longestExitCode)
                       longestExitCode = exitCode;

               }

               if (jobInfoList.size() < 1) {
                   report.setMessage(TITLE_NONE);

               }
               longestName += 2;
               longestJobId += 2;
               longestState += 2;
               longestTime += 2;
               longestUser += 2;
               longestExitCode +=2;


               String formattedLine =
                       "%-" + longestName
                               + "s %-" + longestJobId
                               + "s %-" + longestTime
                               + "s %-" + longestState
                               + "s %-" + longestExitCode
                               + "s %-" + longestUser
                               + "s";


               // no linefeed at the end!!!
               boolean first = true;
               MessagePart topMsg = report.getTopMessagePart();
               Properties properties = report.getExtraProperties();
               if (properties == null) {
                   properties = new Properties();
                   report.setExtraProperties(properties);
               }
               Collection<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
               properties.put("jobs", details);
               for (JobInfo info : jobInfoList) {
                   if (first)    {
                       topMsg.setMessage(String.format(formattedLine, TITLE_NAME, TITLE_JOBID, TITLE_TIME, TITLE_STATE,TITLE_EXITCODE,TITLE_USER ));
                       first = false;
                   }

                   MessagePart msg = topMsg.addChild();
                   msg.setMessage(String.format(formattedLine, info.jobName, info.jobId,  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(info.commandExecutionDate), info.state,info.exitCode,info.user));
                   Map<String, Object> detail = new HashMap<String, Object>();
                   details.add(detail);
                   detail.put(NAME, info.jobName);
                   detail.put(ID, info.jobId);
                   detail.put(DATE, new Date(info.commandExecutionDate));
                   if (info.commandCompletionDate == 0)
                       //for a running job
                       detail.put(COMPLETION_DATE, " ");
                   else
                       // for a completed job
                       detail.put(COMPLETION_DATE, new Date(info.commandCompletionDate));
                   detail.put(STATE,info.state);
                   detail.put(CODE, info.exitCode);
                   detail.put(MESSAGE, info.message);
                   detail.put(USER, info.user);
               }

               report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}


