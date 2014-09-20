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

//import com.sun.enterprise.util.i18n.StringManager;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.TargetModuleID;
import org.glassfish.deployment.client.DFDeploymentStatus;
import org.glassfish.deployment.client.DFProgressObject;

/**
 * Implementation of the Progress Object 
 * @author  dochez
 * @author  tjquinn
 */
public class ProgressObjectImpl extends DFProgressObject {

    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ProgressObjectImpl.class);
    
    protected CommandType commandType;
    protected Object[] args;
    private Vector listeners = new Vector(); // <-- needs to be synchronized
    protected TargetImpl target;
    protected TargetImpl[] targetsList;
    protected String moduleID;
    protected ModuleType moduleType;
    protected DeploymentStatusImpl deploymentStatus =null;
    protected TargetModuleID[] targetModuleIDs = null;
    protected Vector deliveredEvents = new Vector();
    protected DFDeploymentStatus finalDeploymentStatus = null;
    protected boolean deployActionCompleted;
    protected String warningMessages;
    
    private final static String MODULE_ID = 
        DFDeploymentStatus.MODULE_ID;
    private final static String MODULE_TYPE = 
        DFDeploymentStatus.MODULE_TYPE;
    private final static String KEY_SEPARATOR = 
        DFDeploymentStatus.KEY_SEPARATOR;
    private final static String SUBMODULE_COUNT = 
        DFDeploymentStatus.SUBMODULE_COUNT;
    private final static String CONTEXT_ROOT = 
        DFDeploymentStatus.CONTEXT_ROOT;
    private final static String WARNING_PREFIX = "WARNING: ";

    /** Creates a new instance of ProgressObjectImpl */
    public ProgressObjectImpl(TargetImpl target) {
        this.target = target;
        deploymentStatus = new DeploymentStatusImpl(this);
        deploymentStatus.setState(StateType.RELEASED);
        finalDeploymentStatus = new DFDeploymentStatus();
        deployActionCompleted = false;
    }

    public ProgressObjectImpl(TargetImpl[] targets) {
        this.targetsList = targets;
        this.target = targets[0];
        deploymentStatus = new DeploymentStatusImpl(this);
        deploymentStatus.setState(StateType.RELEASED);
        finalDeploymentStatus = new DFDeploymentStatus();
        deployActionCompleted = false;
    }
    
    public ProgressObjectImpl(Target[] targets) {
        this(toTargetImpl(targets));
    }
    
    public ProgressObjectImpl(Target target) {
        this(toTargetImpl(target));
    }
    
    // XXX should be stricter than public
    public static TargetImpl toTargetImpl(Target target) {
        if (target instanceof TargetImpl) {
            return (TargetImpl) target;
        } else {
                throw new IllegalArgumentException(localStrings.getLocalString(
                        "enterprise.deployapi.spi.wrongImpl",
                        "Expected Target implementation class of {0} but found instance of {1} instead",
                        TargetImpl.class.getName(),
                        target.getClass().getName()));
            }
    }
    
    // XXX should be stricter than public
    public static TargetImpl[] toTargetImpl(Target[] targets) {
        TargetImpl[] result = new TargetImpl[targets.length];
        int i = 0;
        for (Target t : targets) {
            result[i++] = toTargetImpl(t);
        }
        return result;
    }
    
    /** Add a listener to receive Progress events on deployment
     * actions.
     *
     * @param The listener to receive events
     * @see ProgressEvent
     */
    public void addProgressListener(ProgressListener pol) {
	synchronized (listeners) {
            listeners.add(pol);
	    if (deliveredEvents.size() > 0) {
	        for (Iterator i = deliveredEvents.iterator(); i.hasNext();) {
		        pol.handleProgressEvent((ProgressEvent)i.next());
	        }
	    }
	}
    }
    
    /** (optional)
     * A cancel request on an in-process operation
     * stops all further processing of the operation and returns
     * the environment to it original state before the operation
     * was executed.  An operation that has run to completion
     * cannot be cancelled.
     *
     * @throws OperationUnsupportedException this optional command
     *         is not supported by this implementation.
     */
    public void cancel() throws OperationUnsupportedException {
        throw new OperationUnsupportedException("cancel not supported");
    }
    
    /** Return the ClientConfiguration object associated with the
     * TargetModuleID.
     *
     * @return ClientConfiguration for a given TargetModuleID or
     *         null if none exists.
     */
    public ClientConfiguration getClientConfiguration(TargetModuleID id) {
        return null;
    }
    
    /** Retrieve the status of this activity.
     *
     * @return An object containing the status
     *          information.
     */
    public DeploymentStatus getDeploymentStatus() {
        DeploymentStatusImpl result = new DeploymentStatusImpl(this);
        result.setState(deploymentStatus.getState());
        result.setMessage(deploymentStatus.getMessage());
        
        return result;
    }

    /**
     * Retrieve the final deployment status which has complete details for each stage
     */
    public DFDeploymentStatus getCompletedStatus() {
        if(deployActionCompleted) {
            return finalDeploymentStatus;
        }
        return null;
    }
    
    /** Retrieve the list of TargetModuleIDs successfully
     * processed or created by the associated DeploymentManager
     * operation.
     *
     * @return a list of TargetModuleIDs.
     */
    public TargetModuleID[] getResultTargetModuleIDs() {

        /**
         * this should go once CTS has fixed their bugs...
         */
        if (targetModuleIDs==null) {
            if(target != null) {
                initializeTargetModuleIDs(moduleID);
//            } else if(targetsList != null) {
//                initializeTargetModuleIDForAllServers(null, null);
            }
        }
        // will return null until the operation is completed
        return targetModuleIDs;
    }
    
    public void setModuleID(String id) {
        moduleID = id;
    }

    /**
     * initialize the target module IDs with the passed application moduleID
     * and the descriptors
     */
    protected void initializeTargetModuleIDs(String moduleID) {
        TargetModuleIDImpl parentTargetModuleID = new TargetModuleIDImpl(target, moduleID);        
        
        targetModuleIDs = new TargetModuleIDImpl[1];        
        targetModuleIDs[0] = parentTargetModuleID;
    }

//    /**
//     * Initialize the target module IDs with the application information stored
//     * in the DeploymentStatus for all the server in the target list.
//     */
//    protected void initializeTargetModuleIDForAllServers(
//        DFDeploymentStatus status) {
//
//        if(targetsList == null) {
//            return;
//        }
//
//        targetModuleIDs = new TargetModuleIDImpl[targetsList.length];
//        String tmpModuleID = status == null 
//                        ? this.moduleID : status.getProperty(MODULE_ID);
//        String key = tmpModuleID + KEY_SEPARATOR + MODULE_TYPE;
//        ModuleType type = status == null
//                        ? getModuleType()
//                        : ModuleType.getModuleType((new Integer(status.getProperty(key))).intValue());
//
//        for(int i=0; i<targetsList.length; i++) {
//            TargetModuleIDImpl parentTargetModuleID = new TargetModuleIDImpl(tmpModuleID, targetsList[i]);
//            targetModuleIDs[i] = parentTargetModuleID;
//
//            if (status != null) {
//                // let's get the host name and port where the application was deployed 
//                HostAndPort webHost=null;
//                try {
//                    Object[] params = new Object[]{ tmpModuleID, Boolean.FALSE };
//                    String[] signature = new String[]{ "java.lang.String", "boolean"};
//                    ObjectName applicationsMBean = new ObjectName(APPS_CONFIGMBEAN_OBJNAME);                
//                    webHost = (HostAndPort) mbsc.invoke(applicationsMBean, "getHostAndPort", params, signature);                        
//                } catch(Exception e) {
//                    Logger.getAnonymousLogger().log(Level.WARNING, e.getLocalizedMessage(), e);
//                }
//
//                key = tmpModuleID + KEY_SEPARATOR + SUBMODULE_COUNT;
//                if (status.getProperty(key) == null) { //standalone module
//                    if (ModuleType.WAR.equals(type)) {
//                        key = tmpModuleID + KEY_SEPARATOR + CONTEXT_ROOT;
//                        String contextRoot = status.getProperty(key);
//                        initTargetModuleIDWebURL(parentTargetModuleID, webHost, contextRoot);
//                     }
//                } else {
//                    int counter = (Integer.valueOf(status.getProperty(key))).intValue();
//                    // now for each sub module            
//                    for (int j = 0; j < counter; j++) {
//                        //subModuleID
//                        key = tmpModuleID + KEY_SEPARATOR + MODULE_ID + KEY_SEPARATOR + String.valueOf(j);
//                        String subModuleID = status.getProperty(key);
//                        TargetModuleIDImpl subModule = new TargetModuleIDImpl(subModuleID, targetsList[i]);
//
//                        //subModuleType 
//                        key = subModuleID + KEY_SEPARATOR + MODULE_TYPE;
//                        type = ModuleType.getModuleType((new Integer(status.getProperty(key))).intValue());
//                        subModule.setModuleType(type);
//                        if (ModuleType.WAR.equals(type) && webHost!=null) {
//                            key = subModuleID + KEY_SEPARATOR + CONTEXT_ROOT;
//                            String contextRoot = status.getProperty(key);
//                            initTargetModuleIDWebURL(subModule, webHost, contextRoot);
//                        }
//                        parentTargetModuleID.addChildTargetModuleID(subModule);
//                    }
//                } 
//            }
//        }
//    }

//    /**
//     * private method to initialize the web url for the associated deployed web
//     * module
//     */
//    private void initTargetModuleIDWebURL(
//        TargetModuleIDImpl tm, HostAndPort webHost, String contextRoot) {
//        
//        if (webHost==null)
//            return;
//        
//        try {
//            // Patchup code for fixing netbeans issue 6221411; Need to find a 
//            // good solution for this and WSDL publishing
//            String host;
//            SunDeploymentManager sdm = new SunDeploymentManager(tm.getConnectionInfo());
//            if(sdm.isPE()) {
//                host = tm.getConnectionInfo().getHostName();
//            } else {
//                host = webHost.getHost();
//            }
//            
//            URL webURL = new URL("http", host, webHost.getPort(), contextRoot);
//            tm.setWebURL(webURL.toExternalForm());
//        } catch(Exception e) {
//            Logger.getAnonymousLogger().log(Level.WARNING, e.getLocalizedMessage(), e);
//        }
//    }
//    
    /** Tests whether the vendor supports a cancel
     * opertation for deployment activities.
     *
     * @return <code>true</code> if canceling an
     *         activity is supported by this platform.
     */
    public boolean isCancelSupported() {
        return false;
    }
    
    /** Tests whether the vendor supports a stop
     * opertation for deployment activities.
     *
     * @return <code>true</code> if canceling an
     *         activity is supported by this platform.
     */
    public boolean isStopSupported() {
        return false;
    }
    
    /** Remove a ProgressObject listener.
     *
     * @param The listener being removed
     * @see ProgressEvent
     */
    public void removeProgressListener(ProgressListener pol) {
	synchronized (listeners) {
            listeners.remove(pol);
	}
    }
    
    /** (optional)
     * A stop request on an in-process operation allows the
     * operation on the current TargetModuleID to run to completion but
     * does not process any of the remaining unprocessed TargetModuleID
     * objects.  The processed TargetModuleIDs must be returned by the
     * method getResultTargetModuleIDs.
     *
     * @throws OperationUnsupportedException this optional command
     *         is not supported by this implementation.
     */
    public void stop() throws OperationUnsupportedException {
        throw new OperationUnsupportedException("stop not supported");
    }

    
    public void setCommand(CommandType commandType, Object[] args) {
        this.commandType = commandType;
        this.args = args;
    }
    
    /**
     * Notifies all listeners that have registered interest for ProgressEvent notification. 
     */
    protected void fireProgressEvent(ProgressEvent progressEvent) {
        /*
         *Bug 4977764
         *Iteration failed due to concurrent modification of the vector.  Even though the add, remove, and fire 
         *methods synchronize on the listeners vector, a listener could conceivably invoke add or remove 
         *recursively, thereby triggering the concurrent modification exception.
         *
         *Fix: clone the listeners vector and iterate through the clone.  
         */
	Vector currentListeners = null;
        synchronized (listeners) {
            currentListeners = (Vector) listeners.clone();
            /*
             *The following add must remain inside the synchronized block.  Otherwise, there will be a small window
             *in which a new listener's registration could interleave with fireProgressEvent, registering itself 
             *after the listeners vector had been cloned (thus excluding the new listener from the iteration a
             *few lines below) but before the list of previously-delivered events had been updated.  
             *This would cause the new listener to miss the event that was firing.  
             *Keeping the following add inside the synchronized block ensures that updates to the listeners 
             *vector by addProgressListener and to deliveredEvents by fireProgressEvent do not interleave and therefore
             *all listeners will receive all events.
             */
            
            deliveredEvents.add(progressEvent);
        }

        for (Iterator listenersItr = currentListeners.iterator(); listenersItr.hasNext();) {
            ((ProgressListener)listenersItr.next()).handleProgressEvent(progressEvent);
        }
    }


    /**
     * Notifies all listeners that have registered interest for ProgressEvent notification. 
     */
    protected void fireProgressEvent(StateType state, String message) {
        fireProgressEvent(state, message, target);
    }

    /**
     * Notifies all listeners that have registered interest for ProgressEvent notification. 
     */
    protected void fireProgressEvent(StateType state, String message, TargetImpl aTarget) {
        
        StateType stateToBroadcast = (state != null) ? state : deploymentStatus.getState();

        /* new copy of DeploymentStatus */
	DeploymentStatusImpl depStatus = new DeploymentStatusImpl(this);
	depStatus.setState(stateToBroadcast);
        depStatus.setMessage(message);

        /*
         *Update this progress object's status before notifying listeners.
         */
        if (state != null) {
            deploymentStatus.setMessage(message);
            deploymentStatus.setState(state); // retain current state
	}
        
        /* send notification */
	TargetModuleIDImpl tmi = new TargetModuleIDImpl(aTarget, moduleID);
	fireProgressEvent(new ProgressEvent(this, tmi, depStatus));
    }

    CommandType getCommandType() {
        return commandType;
    }
    
    /**
     * Sets the module type for this deployed module
     * @param the module type
     */
    public void setModuleType(ModuleType moduleType) {
        this.moduleType = moduleType;
    }
    
    /**
     * @return the module type of this deployed module
     */ 
    public ModuleType getModuleType() {
        return moduleType;
    }    

    /**
     * Since DeploymentStatus only provides the ability to pass a String to the
     * ProgressListener, the following is a convenience method for allowing the
     * stack-trace from a Throwable to be converted to a String to send to the
     * ProgressListeners.
     */
    protected String getThrowableString(Throwable t) {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	PrintStream ps = new PrintStream(bos);
	t.printStackTrace(ps);
	ps.close(); // may not be necessary
	return bos.toString();
    }
    
    /**
     * Parse the DeploymentStatus to get the status message within
     */
    private String getDeploymentStatusMessage(DFDeploymentStatus status) {
        if(status == null) {
            return null;
        }
        // if stage status is success, return as it is
        if (DFDeploymentStatus.Status.SUCCESS.isWorseThanOrEqual(status.getStatus())) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        DFDeploymentStatus.parseDeploymentStatus(status, pw);
        byte[] statusBytes = bos.toByteArray();
        String statusString = new String(statusBytes);
        // if stage status is WARNING, collect the warning messages
        if(status.getStatus() == DFDeploymentStatus.Status.WARNING) {
            if(warningMessages==null) {
                warningMessages = WARNING_PREFIX + statusString;
            } else {
                warningMessages += statusString;
            }
            return null;
        }
        // Failed stage; return the failure message
        return statusString;
    }

    public void setupForNormalExit(
            String message, 
            TargetImpl aTarget, 
            TargetModuleIDImpl[] tmids) {
        String i18nmsg;
        // If we ever got some warning during any of the stages, the the final status is warning; else status=success
        if(warningMessages == null) {
            i18nmsg = localStrings.getLocalString(
                    "enterprise.deployment.client.action_completed",
                    "{0} completed successfully",
                    message);
            finalDeploymentStatus.setStageStatus(DFDeploymentStatus.Status.SUCCESS);
        } else {
            i18nmsg = localStrings.getLocalString(
                    "enterprise.deployment.client.action_completed_with_warning", 
                    "Action completed with warning message: {0}",
                    warningMessages);
            finalDeploymentStatus.setStageStatus(DFDeploymentStatus.Status.WARNING);            
        }
        finalDeploymentStatus.setStageStatusMessage(i18nmsg);
        deployActionCompleted = true;
        targetModuleIDs = tmids;
        fireProgressEvent(StateType.COMPLETED, i18nmsg, aTarget);
        for (TargetModuleIDImpl tmid : tmids) {
            // initialize moduleID so the event can be populated with 
            // the proper moduleID.
            moduleID = tmid.getModuleID();
            fireProgressEvent(StateType.COMPLETED, message, tmid.getTargetImpl());
        }
        return;
    }
    
    public void setupForAbnormalExit(String errorMsg, TargetImpl aTarget) {
        String i18nmsg = localStrings.getLocalString(
                "enterprise.deployment.client.action_failed", 
                "Action failed {0}",
                 errorMsg);
        finalDeploymentStatus.setStageStatus(DFDeploymentStatus.Status.FAILURE);
        finalDeploymentStatus.setStageStatusMessage(i18nmsg);
        
        deployActionCompleted = true;
        fireProgressEvent(StateType.FAILED, i18nmsg, aTarget);
        return;
    }

    /**
     * Given a Deployment status, this checks if the status is success
     */
    public boolean checkStatusAndAddStage(TargetImpl aTarget, String action, DFDeploymentStatus currentStatus) {
        String statusMsg = getDeploymentStatusMessage(currentStatus);
        finalDeploymentStatus.addSubStage(currentStatus);
        if(statusMsg == null) {
            fireProgressEvent(StateType.RUNNING, localStrings.getLocalString("enterprise.deployment.client.action_completed", "Action {0} completed", action), aTarget);
            return true;
        }
        setupForAbnormalExit(localStrings.getLocalString("enterprise.deployment.client.action_failed_with_message", "Action {0} failed - {1}", action, statusMsg),  aTarget);
        return false;
    }
}
