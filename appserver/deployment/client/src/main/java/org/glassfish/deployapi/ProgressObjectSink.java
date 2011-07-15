/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployapi;

import java.util.Collection;
import java.util.Vector;
import java.util.Iterator;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import org.glassfish.deployment.client.DFProgressObject;
import org.glassfish.deployment.client.DFDeploymentStatus;
import org.glassfish.deployment.client.DFDeploymentStatus.Status;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * This class acts as a sink for ProgressObject. It registers itself
 * as ProgressObject listener for multiple deployment actions and 
 * tunnel all events to registered ProgressObject listener.
 *<p>
 *Whenever this class receives a progress event from one of its sources (one of the deploymentFacility
 *actions) it forwards that event on to the sink's listeners, changing the state of the event to 
 *"running."  Then, after the sink receives the completion or failure event from the last source,
 *it forwards that event as running (as it had all earlier events) and then sends one final
 *aggregate completion or failure event.
 *<p>
 *The sink always follows this pattern, even if it encapsulates only a single source.  JSR88 clients should
 *be aware of this behavior.  
 *
 * @author Jerome Dochez
 */
public class ProgressObjectSink extends DFProgressObject implements ProgressListener {
    
    private static String LINE_SEPARATOR = System.getProperty("line.separator");
    private Vector registeredPL = new Vector();
    private Vector deliveredEvents = new Vector();
    
    /* aggregate state starts as successful and is changed only if at least one source operation fails */
    private StateType finalStateType = StateType.COMPLETED;
    private String finalMessage;
    
    private Vector targetModuleIDs = new Vector();
    private Vector sources = new Vector();
        
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(ProgressObjectSink.class);
    
    DFDeploymentStatus completedStatus = new DFDeploymentStatus();
    private boolean completedStatusReady = false;
    
    /**
     * register to a new ProgressObject for ProgressEvent notifications
     */
    public void sinkProgressObject(ProgressObject source) {
        /*
         *The following two statements must appear in the order shown.  Otherwise, a race condition can exist. 
         */
        sources.add(source);
        source.addProgressListener(this);
    }
    
    /** 
     * receives notification of a progress event from one of our 
     * registered interface.
     */
    public void handleProgressEvent(ProgressEvent progressEvent) {
        
        ProgressEvent forwardedEvent;
        DeploymentStatus forwardedDS = progressEvent.getDeploymentStatus();
        
        // we intercept all events...
        if (!forwardedDS.isRunning()) {
            // this mean we are either completed or failed...
            if (forwardedDS.isFailed()) {
                /*
                 *Once at least one operation fails, we know that the aggregate state will have
                 *to be failed.
                 */
                finalStateType = StateType.FAILED;
            }
            
            // since this is the completion event 
            // we are done with that progress listener;
            Object source = progressEvent.getSource();
            if (source instanceof ProgressObject) {
                ProgressObject po = (ProgressObject) source;
                po.removeProgressListener(this);
                
                sources.remove(source);
                
                if (forwardedDS.isCompleted()) {
                
                    TargetModuleID[] ids = po.getResultTargetModuleIDs();
                    for (int i=0;i<ids.length;i++) {
                        targetModuleIDs.add(ids[i]);
                    }
                }
            } else {
                throw new RuntimeException(localStrings.getLocalString(
                    "enterprise.deployment.client.noprogressobject",
                    "Progress event does not contain a ProgressObject source"
                    ));
            }
            
            /*
             *Update the completionStatus by adding a stage to it and recording the completion
             *of this event as the newest stage.
             */
            updateCompletedStatus(forwardedDS);
            
            // now we change our event state to running.  We always forward every event from a
            // source to the listeners with "running" status because the sink is not yet completely
            // finished.  We will also send a final aggregate completion event
            // if this is a completion event from our last source (see below).
            DeploymentStatusImpl forwardedStatus = new DeploymentStatusImpl();
            forwardedStatus.setState(StateType.RUNNING);
            forwardedStatus.setMessage(forwardedDS.getMessage());
            forwardedStatus.setCommand(forwardedDS.getCommand());
            forwardedEvent = new ProgressEvent(this, progressEvent.getTargetModuleID(), forwardedStatus);
        } else {
            // This is a "running" event from one of our sources, so we just need to swap the source...
            forwardedEvent = new ProgressEvent(this, progressEvent.getTargetModuleID(), 
                forwardedDS);
        }
        
        // we need to fire the received event to our listeners
        Collection clone;
        ProgressEvent finalEvent = null;
        
        synchronized(registeredPL) {
            clone = (Collection) registeredPL.clone();
            deliveredEvents.add(forwardedEvent);
            /*
             *If we are done with all of our sources, let's wrap up by creating a final event that will
             *be broadcast to the listeners along with the forwarded event.  Also create the completed status
             *that meets the requirements of the JESProgressObject interface.
             */
            if (sources.isEmpty()) {
                prepareCompletedStatus();
                DeploymentStatusImpl status = new DeploymentStatusImpl();
                status.setState(finalStateType);
                if (finalStateType.equals(StateType.FAILED)) {
                    status.setMessage(localStrings.getLocalString(
                        "enterprise.deployment.client.aggregatefailure",
                        "At least one operation failed"
                        ));
                } else {
                    status.setMessage(localStrings.getLocalString(
                        "enterprise.deployment.client.aggregatesuccess",
                        "All operations completed successfully"
                        ));
                }
                finalEvent = new ProgressEvent(this, progressEvent.getTargetModuleID(), status);
                deliveredEvents.add(finalEvent);
            }
        }        
        
        for (Iterator itr=clone.iterator();itr.hasNext();) {
            ProgressListener pl = (ProgressListener) itr.next();
            pl.handleProgressEvent(forwardedEvent);
        }

        /*
         *Send the final event if there is one.
         */
        if (finalEvent != null) {
            for (Iterator itr=clone.iterator();itr.hasNext();) {
                ProgressListener pl = (ProgressListener) itr.next();
                pl.handleProgressEvent(finalEvent);
            }
        }
    }    
    
    
    /**
     * Register a new ProgressListener
     * @param the new listener instance
     */
    public void addProgressListener(ProgressListener progressListener) {
        
	Collection clone;
        synchronized(registeredPL) {
            registeredPL.add(progressListener);
        
            // now let's deliver all the events we already received.
            clone = (Collection) deliveredEvents.clone();
        }
        
        for (Iterator itr=clone.iterator();itr.hasNext();) {
            ProgressEvent pe = (ProgressEvent) itr.next();
            progressListener.handleProgressEvent(pe);
        }
    }
    
    /**
     * removes a ProgressListener from our list of listeners
     * @param the ProgressListener to remove
     */
    public void removeProgressListener(ProgressListener progressListener) {
        registeredPL.remove(progressListener);
    }
    
    
    public javax.enterprise.deploy.spi.status.ClientConfiguration getClientConfiguration(TargetModuleID targetModuleID) {
        // since we are never called upon deploying, I don't 
        // have to deal with this at this time.
        return null;
    }
    
    public DeploymentStatus getDeploymentStatus() {
        DeploymentStatusImpl status = new DeploymentStatusImpl();
        if (sources.isEmpty()) {
            status.setState(finalStateType);
            status.setMessage(finalMessage);
        } else {
            status.setState(StateType.RUNNING);
        }
        return status;        
    }
    
    public TargetModuleID[] getResultTargetModuleIDs() {
        
        TargetModuleID[] ids = new TargetModuleID[targetModuleIDs.size()];
        targetModuleIDs.copyInto(ids);
        return ids;
    }
    
    public boolean isCancelSupported() {
        
        // if only one of our sources does not support cancel, we don't
        for (Iterator itr=getSources().iterator();itr.hasNext();) {
            ProgressObject source = (ProgressObject) itr.next();
            if (!source.isCancelSupported()) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isStopSupported() {
        
        // if only one of our sources does not support stop, we don't
        for (Iterator itr=getSources().iterator();itr.hasNext();) {
            ProgressObject source = (ProgressObject) itr.next();
            if (!source.isStopSupported()) {
                return false;
            }
        }
        return true;
    }
    
    public void cancel() throws OperationUnsupportedException {
        if (!isCancelSupported()) {
            throw new OperationUnsupportedException("cancel");
        }
        for (Iterator itr=getSources().iterator();itr.hasNext();) {
            ProgressObject source = (ProgressObject) itr.next();
            source.cancel();
        }        
    }
    
    public void stop() throws OperationUnsupportedException {
        if (!isStopSupported()) {
            throw new OperationUnsupportedException("stop");
        }
        for (Iterator itr=getSources().iterator();itr.hasNext();) {
            ProgressObject source = (ProgressObject) itr.next();
            source.stop();
        }
        
    }
    
    private Collection getSources() {
        return (Collection) sources.clone();
    }
    
    private void prepareCompletedStatus() {
        /*
         *The substages may have status values of success when in fact a warning is present
         *in a substage.  Traverse all the substages, composing the true aggregate state and
         *message based on the most severe state that is present in the entire stage tree.
         */
        Status worstStatus = Status.NOTINITIALIZED;
        StringBuffer msgs = new StringBuffer();

        Status newWorstStatus = aggregateStages(worstStatus, msgs, completedStatus);
        completedStatus.setStageStatus(newWorstStatus);
        completedStatus.setStageStatusMessage(msgs.toString());
        
        completedStatusReady = true;
    }
    
    private Status aggregateStages(Status worstStatusSoFar, StringBuffer msgs, DFDeploymentStatus stage) {
        /*
         *Starting with the stage passed in, see if its severity is more urgent than that seen so far.
         *If so, then discard the messages accumulated so far for the less urgent severity and save
         *this stage's message and severity as the worst seen so far.
         */
        Status stageStatus = stage.getStageStatus();
        if (stageStatus.isWorseThan(worstStatusSoFar)) {
            worstStatusSoFar = stageStatus;
            msgs.delete(0,msgs.length());
        }
        
        /*
         *If the stage's severity is the same as the currently worst seen, then add this stage's message
         *to the aggregate message.
         */
        if (stageStatus == worstStatusSoFar) {
            msgs.append(stage.getStageStatusMessage()).append(LINE_SEPARATOR);
        }
        
        /*
         *Now, do the same for each substage.
         */
        for (Iterator it = stage.getSubStages(); it.hasNext(); ) {
            DFDeploymentStatus substage = (DFDeploymentStatus) it.next();
            worstStatusSoFar = aggregateStages(worstStatusSoFar, msgs, substage);
        }
        
        return worstStatusSoFar;
    }
    
    /**
     *Report completed status for deploytool.
     *@return null if not completed, or the DFDeploymentStatus set to reflect the completion
     */
    public DFDeploymentStatus getCompletedStatus() {
	DFDeploymentStatus answer = null;
        if (completedStatusReady) {
            answer = completedStatus;
        }
        return answer;
    }

    private void updateCompletedStatus(DeploymentStatus ds) {
        /*
         *If the status passed in is already a backend.DeploymentStatus then add it as a new stage to the 
         *completed status.  Otherwise, create a new backend.DeploymentStatus, fill it in as much as 
         *possible, and add it as the next stage.
         */
        
        DFDeploymentStatus newStageStatus = null;
        if (ds instanceof DeploymentStatusImpl) {
            DeploymentStatusImpl dsi = (DeploymentStatusImpl) ds;
            newStageStatus = dsi.progressObject.getCompletedStatus();
        } else {
            /*
             *Create a new status stage and add it to the completed status.
             */
            newStageStatus = new DFDeploymentStatus(completedStatus);
            /*
             *The new state status depends on the DeploymentStatus outcome.
             */
            int stageStatus = -1;
            Throwable exc;

            reviseStatusAndMessage(ds, newStageStatus);
        }
        if (newStageStatus != null) {
            /*
             *Update the final status state if this new stage's state is worse than the final status's
             *current state.
             */
            completedStatus.addSubStage(newStageStatus);
            /*
             *The status being reported may say it is successful but there could be warnings in substages 
             *(or substages of substages...).  So the truly final state and message for the completed 
             *status is determined once, after the last source of events is removed.
             */
        } else {
            System.err.println("A newStageStatus was null");
        }
    }
    
    private void reviseStatusAndMessage(DeploymentStatus ds, DFDeploymentStatus newStageStatus) {
        String msgKey = null;
        Status stageStatus;
        
        if (ds.isCompleted()) {
            /*
             *The deployment status for this source was successful. 
             */
            msgKey = "enterprise.deployment.client.action_completed";
            stageStatus = Status.SUCCESS;
        } else {
            /*
             *The deployment status for this source failed.
             */
            msgKey = "enterprise.deployment.client.action_failed";
            stageStatus = Status.FAILURE;
        }

        String i18msg = localStrings.getLocalString(msgKey, ds.getMessage());
        newStageStatus.setStageStatus(stageStatus);
        newStageStatus.setStageStatusMessage(i18msg);
    }
}
