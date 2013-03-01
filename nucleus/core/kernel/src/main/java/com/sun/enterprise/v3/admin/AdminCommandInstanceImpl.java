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

import com.sun.enterprise.admin.event.AdminCommandEventBrokerImpl;
import com.sun.enterprise.admin.remote.AdminCommandStateImpl;
import com.sun.enterprise.v3.admin.JobPersistenceService;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.AdminCommandEventBroker;
import org.glassfish.api.admin.Job;
import org.glassfish.api.admin.CommandProgress;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.api.admin.progress.JobPersistence;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.services.common.SubjectUtil;

import javax.security.auth.Subject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Represents running (or finished) command instance.
 *
 *
 * @author Martin Mares
 * @author Bhakti Mehta
 */

public class AdminCommandInstanceImpl extends AdminCommandStateImpl implements Job {
    
    private CommandProgress commandProgress;
    private Payload.Outbound payload;
    private final AdminCommandEventBroker broker;

    private final long executionDate;

    private final String commandName;

    private final Subject subject;

    private final String scope;

    private boolean isManagedJob;

    private File jobsFile;

    private long completionDate;



    protected AdminCommandInstanceImpl(String id, String name,String commandScope,Subject sub,boolean managedJob) {
        super(id);
        this.broker = new AdminCommandEventBrokerImpl();
        this.executionDate = new Date().getTime();
        this.commandName = name;
        this.scope= commandScope;
        isManagedJob = managedJob;
        this.subject = sub;

    }

    protected AdminCommandInstanceImpl( String name,String scope,Subject sub,boolean managedJob) {
        this(null,name,scope,sub,managedJob);
    }
    
    @Override
    public CommandProgress getCommandProgress() {
        return commandProgress;
    }

    @Override
    public void setCommandProgress(CommandProgress commandProgress) {
        this.commandProgress = commandProgress;
        commandProgress.setEventBroker(broker);
    }

    @Override
    public AdminCommandEventBroker getEventBroker() {
        return this.broker;
    }

    @Override
    public File getJobsFile() {
        return jobsFile;
    }

    @Override
    public void setJobsFile(File jobsFile) {
        this.jobsFile = jobsFile;
    }
    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public String getName() {
        return commandName;
    }



    @Override
    protected void setState(State state) {
        if (state != null && state != getState()) {
            super.setState(state);
            getEventBroker().fireEvent(EVENT_STATE_CHANGED, this);
        }
    }
    
    @Override
    public boolean isOutboundPayloadEmpty() {
        return payload == null || payload.size() == 0;
    }
    
    @Override
    public void complete(ActionReport report, Payload.Outbound outbound) {
        if (commandProgress != null && report != null && report.getActionExitCode() == ExitCode.SUCCESS) {
            commandProgress.complete();
        }
        this.payload = outbound;
        this.completionDate = System.currentTimeMillis();
        JobPersistence jobPersistenceService = null;
        if (isManagedJob) {
            if (scope != null)   {
                jobPersistenceService = Globals.getDefaultHabitat().getService(JobPersistence.class,scope+"job-persistence");
            }  else  {
                jobPersistenceService = Globals.getDefaultHabitat().getService(JobPersistenceService.class);
            }

            List<String> userList =  SubjectUtil.getUsernamesFromSubject(subject);
            jobPersistenceService.persist(new JobInfo(id,commandName,executionDate,report.getActionExitCode().name(),userList.get(0),report.getMessage(),getJobsFile(),State.COMPLETED.name(),completionDate));
        }
        complete(report);
    }

    @Override
    public long getCommandExecutionDate ()  {
        return executionDate;

    }

    @Override
    public Payload.Outbound getPayload() {
        return payload;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public long getCommandCompletionDate() {
         return completionDate;
    }

}
