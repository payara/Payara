/*
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.admin.remote.AdminCommandStateImpl;
import com.sun.enterprise.util.LocalStringManagerImpl;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandEventBroker.AdminCommandListener;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.progress.JobInfo;
import org.glassfish.hk2.api.PerLookup;

import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.glassfish.api.admin.AdminCommandState.State.PREPARED;
import static org.glassfish.api.admin.AdminCommandState.State.RUNNING;
import static org.glassfish.api.admin.AdminCommandState.State.COMPLETED;


/**
 * Gets CommandInstance from registry based on given id and forwards all events.
 *
 * @author Martin Mares
 * @author Bhakti Mehta
 */
@Service(name = AttachCommand.COMMAND_NAME)
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n(AttachCommand.COMMAND_NAME)
@ManagedJob
public class AttachCommand implements AdminCommand, AdminCommandListener,AdminCommandSecurity.AccessCheckProvider {

    
    public static final String COMMAND_NAME = "attach";
    private final static LocalStringManagerImpl strings = new LocalStringManagerImpl(AttachCommand.class);

    private AdminCommandEventBroker eventBroker;
    private Job attached;

    @Inject
    JobManager registry;

    @Param(primary=true, optional=false, multiple=false)
    String jobID;

    protected final List<AccessRequired.AccessCheck> accessChecks = new ArrayList<AccessRequired.AccessCheck>();

    @Override
    public void execute(AdminCommandContext context) {
        eventBroker = context.getEventBroker();
        ActionReport ar = context.getActionReport();
        attached = registry.get(jobID);
        JobInfo jobInfo = null;
        String jobName = null;
        if (attached == null) {
            //try for completed jobs
            if (registry.getCompletedJobs() != null)
                jobInfo = (JobInfo) registry.getCompletedJobForId(jobID);
             if (jobInfo != null) {
                 jobName = jobInfo.jobName;
             }

        }

        if ((attached == null && jobInfo == null) || (attached != null && attached.getName().startsWith("_"))
                || (attached != null && AttachCommand.COMMAND_NAME.equals(attached.getName()))) {
            ar.setActionExitCode(ActionReport.ExitCode.FAILURE);
            ar.setMessage(strings.getLocalString("attach.wrong.commandinstance.id", "Command instance {0} does not exist.", jobID));
            return;
        }
        if (attached != null) {
            AdminCommandEventBroker attachedBroker = attached.getEventBroker();
            synchronized (attachedBroker) {
                onAdminCommandEvent(AdminCommandStateImpl.EVENT_STATE_CHANGED, attached);
                if (attached.getCommandProgress() != null) {
                    onAdminCommandEvent(CommandProgress.EVENT_PROGRESSSTATUS_STATE, attached.getCommandProgress());
                }
                attachedBroker.registerListener(".*", this);
            }
            synchronized (attached) {
                while(attached.getState().equals(PREPARED) ||
                        attached.getState().equals( RUNNING)) {
                    try {
                        attached.wait(1000*60*5); //5000L just to be sure
                    } catch (InterruptedException ex) {}
                }
                if (attached.getState().equals(COMPLETED)) {
                    ar.setActionExitCode(attached.getActionReport().getActionExitCode());
                    ar.appendMessage(strings.getLocalString("attach.finished", "Command {0} executed {1}",attached.getName(),attached.getActionReport().getActionExitCode()));
                }
            }
        } else {

            if (jobInfo != null && jobInfo.exitCode.equals(COMPLETED.toString())) {
                //TODO fix this
                ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                ar.appendMessage(strings.getLocalString("attach.finished", "Command {0} executed{1}",jobName,ActionReport.ExitCode.SUCCESS));
            }

        }

    }

    @Override
    public void onAdminCommandEvent(String name, Object event) {
        if (name == null || name.startsWith("client.")) { //Skip nonsence or own events
            return;
        }
        if (AdminCommandStateImpl.EVENT_STATE_CHANGED.equals(name) && 
                ((Job) event).getState().equals(COMPLETED)) {
            synchronized (attached) {
                attached.notifyAll();
            }
        } else {
            eventBroker.fireEvent(name, event); //Forward
        }
    }


    @Override
    public Collection<? extends AccessRequired.AccessCheck> getAccessChecks() {
        accessChecks.add(new AccessRequired.AccessCheck("jobs/system/$user/$attached","READ"));
        return accessChecks;
    }


}
