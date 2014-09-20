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

package org.glassfish.deployment.client;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.io.PrintWriter;

/**
 * This class encapsulates all status related to backend deployment
 * Backend deployement can consist of several stages. Those stages 
 * are organized in a parent/children relationship where the children
 * are the sub stages of a parent stage. For instance, deployment stage
 * consist of j2eec stage and application loading stage. For a stage to 
 * be sucessful, its status and all its children's status  must be warning 
 * or success  
 * 
 * @author Jerome Dochez
 *
 */
public class DFDeploymentStatus {
	/**
	 * Possible status for a stage or overall deployment status
         * <p>
         * Note that ordering from worst to best simplifies comparisons among
         * Status values.  
	 */
        public enum Status {
            FAILURE,
            WARNING,
            SUCCESS,
            NOTINITIALIZED;
                    
            public boolean isWorseThan(Status other) {
                return (compareTo(other) < 0);
            }

            public boolean isWorseThanOrEqual(Status other) {
                return (compareTo(other) <= 0);
            }
        }
        private static final String NEWLINE = System.getProperty("line.separator");

    /*
     * Possible properties for the additional status
     */
    public static final String CONTEXT_ROOT       = "ContextRoot";
    public static final String KEY_SEPARATOR      = "_";
    public static final String MODULE_ID          = "moduleid";
    public static final String MODULE_TYPE        = "ModuleType";
    public static final String SUBMODULE_COUNT    = "NumberOfSubModules";
    public static final String WSDL_PUBLISH_URL   = "ClientPublishURL";
    public static final String WSDL_LOCATION      = "WsdlFileLocation";
    public static final String WSDL_DIRECTORY     = "WsdlDirectory";
    public static final String WSDL_FILE_ENTRIES  = "WsdlFileEntries";
    public static final String COUNT              = "NumberOfEntries";

	/**
	 * instance information : 
	 * 	- information about this stage
	 *      - information about sub stages
	 */	
	private String stageDescription;
	private Status stageStatus = Status.NOTINITIALIZED;
	private String stageStatusMessage = "";
	private Throwable stageException;
	
	private List<DFDeploymentStatus> subStages = new ArrayList<DFDeploymentStatus>();
        private DFDeploymentStatus parent = null;
        
        /**
         * backend deployment can transfer some information back to the 
         * client process through these properties.
         */
        private Map additionalStatus = new Properties();

        // this field is still kept for backward compatibility reason
        private Properties props=null;
        
	
	/**
	 *  Creates a new uninitialized DFDeploymentStatus instance
	 */
	public DFDeploymentStatus() {
	}
	
	/**
	 * Creates a new uninitialized DFDeploymentStatus instance as a
	 * sub stage deployment status of the passed DFDeploymentStatus
	 * @param parent DFDeploymentStatus
	 */
	public DFDeploymentStatus(DFDeploymentStatus parent) {
		if (parent==null) {
			throw new IllegalArgumentException("parent deployment status cannot be null");
		}
		parent.addSubStage(this);
	}
	
	/**
	 * @return the combined status for the stage and its children, 
	 * it will return the lowest status as defined in the constants
	 */
	public Status getStatus() {
		
		Status currentStatus = stageStatus;
		
		// iterate over all sub stages to get their status
		for (Iterator stageItr = subStages.iterator();stageItr.hasNext();) {
			DFDeploymentStatus subStage = (DFDeploymentStatus) stageItr.next();
			Status subStageStatus= subStage.getStatus();
			// if the sub stage status is a lower number than our current, something
			// went horribly wrong in the substage, update ours
			if (subStageStatus.isWorseThan(currentStatus)) {
				currentStatus = subStageStatus;
			}
		}
		return currentStatus;
	}
	
	/**
	 * Add a sub stage to this deployment status 
	 * @param the sub stage deployment status
	 */
	public void addSubStage(DFDeploymentStatus subStage) {
		subStages.add(subStage);
                subStage.setParent(this);
	}
	
	/**
	 * Get the list of sub stages for this deployment status
	 * @return an Iterator for the sub stages
	 */
	public Iterator getSubStages() {
		return subStages.iterator();
	}
		
	/**
	 *  Set the status for this stage
	 * @param the status as defined in the constants
	 */
	public void setStageStatus(Status status) {
		stageStatus = status;
	}
	
	/** 
	 * @return the status for this stage (ignoring sub stages status)
	 */
	public Status getStageStatus() {
		return stageStatus;
	}
	
	/**
	 * @return the exception if an exception was thrown during 
	 *  the execution of the stage
	 */
	public Throwable getStageException() {
		return stageException;
	}

	/**
	 * @return a meaningful i18ned stage description
	 */
	public String getStageIdentifier() {
		return stageDescription;
	}

        /**
         * @return a meaningful i18ned stage description
         */
        public String getStageDescription() {
                return stageDescription;
        }


	/**
	 * @return a meaningful i18ned reason for failure or warning
	 */
	public String getStageStatusMessage() {
		return stageStatusMessage;
	}

	/**
	 * When the stage throws an exception, it should store it here in 
	 * the assiciated deployment status
	 * @param throwable
	 */
	public void setStageException(Throwable throwable) {
        if (throwable != null) {
            stageException = new Throwable(throwable.toString());
            stageException.setStackTrace(throwable.getStackTrace());
        } else {
            stageException = null;
        }
	}

	/**
	 * @param string for a meaningful i18ned stage description
	 */
	public void setStageDescription(String string) {
		stageDescription = string;
	}

	/**
	 * @param string for a meaningful i18ned reason for stage
	 *  warning or failure
	 */
	public void setStageStatusMessage(String string) {
		stageStatusMessage = string;
	}
        
        /** 
         * @return the current deployment status or one of the sub stage
         * deployment status with a status equal to the provided level.
         */
        public DFDeploymentStatus getStageStatusForLevel(Status level) {
            if (stageStatus == level) {
                return this;
            } else {
                for (Iterator itr = subStages.iterator();itr.hasNext();) {
                    DFDeploymentStatus subStage = (DFDeploymentStatus) itr.next();
                    if (subStage.getStatus()==level) {
                        return subStage;
                    }
                }
            }
            return null;
        }

        /**
         * @return the parent status for this status if any
         */
        public DFDeploymentStatus getParent() {
            return parent;
        }
        
        /**
         * Setthe parent status for this status
         */
        public void setParent(DFDeploymentStatus parent) {
            this.parent = parent;
        }
        
        /**
         * @return the main status for this deployment operation, which is 
         * the parent of all status in this hierarchy
         */
        public DFDeploymentStatus getMainStatus() {
            if (parent!=null) {
                return parent.getMainStatus();
            }
            // if this is the top level DF, strip the outmost level 
            // which contains no information
            Iterator subIter = getSubStages();
            if (subIter.hasNext()) {
                DFDeploymentStatus subStage =
                    (DFDeploymentStatus) subIter.next();
                if (this.getStageStatus().isWorseThan(
                    subStage.getStageStatus())) {                
                    subStage.setStageStatus(this.getStageStatus()); 
                }
                return subStage;
                
            } 
            return this;
        }
        
        /**
         * Add a new property to this status object
         */
        public void addProperty(String propertyName, String propertyValue) {
            additionalStatus.put(propertyName, propertyValue);

            // the name-value pair is also stored in props 
            // for backward compatibility
            if (props==null) {
                props = new Properties();
            }
            props.put(propertyName, propertyValue);
        }
        
        /**
         * @return the value of for property name
         */
        public String getProperty(String propertyName) {
            if (additionalStatus.get(propertyName) != null) {
                return (String)additionalStatus.get(propertyName);
            } else {
            // we also try to retrieve from props 
            // for backward compatibility
                if (props==null) {
                    return null;
                }
                return props.getProperty(propertyName);
            }
        }

        /**
         * @return the additional status map
         */
        public Map getAdditionalStatus() {
            return additionalStatus;
        }

        public String getAllStageMessages() {
            StringBuilder sb = new StringBuilder();
            getAllStageMessages(this, sb);
            return sb.toString();
        }
        
        public static void getAllStageMessages(DFDeploymentStatus status, StringBuilder sb) {
            sb.append(status.getStageStatusMessage());
            for (DFDeploymentStatus child : status.subStages) {
                sb.append(NEWLINE);
                getAllStageMessages(child, sb);
            }
        }
        
        
        /**
         * Set the additional status for this status
         */
        public void setAdditionalStatus(Map additionalStatus) {
            this.additionalStatus = additionalStatus;
        }
        
        /**
         * @return a meaningful string about mysefl
         */
        @Override
        public String toString() {
            return "Status " + stageStatus + " message " + stageStatusMessage 
                + " \nException " + stageException;
        }

        /**
         * Get all stages with this deployment status level
         * @param parent status it needs to iterate through
         * @param status level it's looking for
         * @return an iterator for the stages with the level
         */
        public static Iterator getAllStageStatusForLevel(
            DFDeploymentStatus status,
            Status level) {
            List stages = new ArrayList();
            if (status.getStageStatus() == level) {
                stages.add(status);
            }
            // need to iterate through the rest of status hierarchy
            for (Iterator itr = status.getSubStages();itr.hasNext();) {
                DFDeploymentStatus subStage = (DFDeploymentStatus) itr.next();
                if (subStage.getStageStatus() == level) {
                    stages.add(subStage);
                }
                for (Iterator itr2 = subStage.getSubStages();itr2.hasNext();) {
                    DFDeploymentStatus subStage2 = (DFDeploymentStatus) itr2.next();
                    if (subStage2.getStageStatus() == level) {
                        stages.add(subStage2);
                    }

                    for (Iterator itr3 = subStage2.getSubStages();
                        itr3.hasNext();) {
                        DFDeploymentStatus subStage3 = 
                            (DFDeploymentStatus) itr3.next();
                        if (subStage3.getStageStatus() == level) {
                            stages.add(subStage3);
                        }
                    }
                }
            }
            return stages.iterator();
        }

//        /**
//         * Implement the MapCapable interface so DFDeploymentStatus 
//         * can be converted back and forth with CompositeData
//         */
//        public Map asMap() {
//            HashMap m = new HashMap();
//            m.put(MapCapable.MAP_CAPABLE_CLASS_NAME_KEY, com.sun.appserv.management.deploy.DFDeploymentStatus.DEPLOYMENT_STATUS_CLASS_NAME );
//            m.put(com.sun.appserv.management.deploy.DFDeploymentStatus.STAGE_STATUS_KEY, new Integer(stageStatus));
//            m.put(com.sun.appserv.management.deploy.DFDeploymentStatus.STAGE_STATUS_MESSAGE_KEY, stageStatusMessage);
//            m.put(com.sun.appserv.management.deploy.DFDeploymentStatus.STAGE_DESCRIPTION_KEY, stageDescription);
//            m.put(com.sun.appserv.management.deploy.DFDeploymentStatus.SUB_STAGES_KEY, subStagesToMapList());
//            m.put(com.sun.appserv.management.deploy.DFDeploymentStatus.STAGE_THROWABLE_KEY, stageException);
//            m.put(com.sun.appserv.management.deploy.DFDeploymentStatus.ADDITIONAL_STATUS_KEY, additionalStatus);
//            return( m );
//
//        }
//
//        /**
//         * DFDeploymentStatus class may not be available on the client side.
//         *
//         * Converts the list of DFDeploymentStatus objects to list of Maps.
//         * Iterates through the substages list and calls asMap().
//         */
//        List subStagesToMapList() {
//            final List l = new ArrayList(subStages.size());
//            final Iterator it = subStages.iterator();
//            while (it.hasNext()) {
//                final MapCapable mc = (MapCapable)it.next();
//                l.add(mc.asMap());
//            }
//            return l;
//        }
//
//        /**
//                Return the interface that this Map represents
//                (the Java classname).
//         */
//        public String   getMapClassName() {
//	    return com.sun.appserv.management.deploy.DFDeploymentStatus.DEPLOYMENT_STATUS_CLASS_NAME;
//	}


        /**
         * Traverse through the DeploymenStatus hierarchy and 
         * write failure/warning msgs to the print writer
         */
        public static void parseDeploymentStatus(DFDeploymentStatus status, 
            PrintWriter pw) {
            if (status != null) {
                // if it's falure case, print all exceptions
                if (status.getStatus() == DFDeploymentStatus.Status.FAILURE) {
                    for (Iterator itr = getAllStageStatusForLevel(status, DFDeploymentStatus.Status.FAILURE); itr.hasNext();) {
                        DFDeploymentStatus stage = (DFDeploymentStatus) itr.next();
                        if (stage.getParent() == null) {
                            // don't print the message from the outmost level
                            continue;
                        }
                        printFailure(pw, stage);
                    }
                } 

                // if it's warning case, print all warnings
                else if (status.getStatus() == DFDeploymentStatus.Status.WARNING) {
                    for (Iterator itr = getAllStageStatusForLevel(status, DFDeploymentStatus.Status.WARNING); itr.hasNext();) {
                        DFDeploymentStatus stage = (DFDeploymentStatus) itr.next();
                        if (stage.getParent() == null) {
                            // don't print the message from the outmost level
                            continue;
                        }
                        String msg = stage.getStageStatusMessage();
                        if (msg != null) {
                            pw.println(msg);
                        }
                    }
                }
                pw.flush();
            }
        }

        /**
        * Prints the status string and/or status exception
        * @param pw PrintWriter to which info is printed.
        * @param status DFDeploymentStatus
        * @param t Throwable to print
        */
        private static void printFailure(PrintWriter pw, 
            DFDeploymentStatus status) {
            String msg = status.getStageStatusMessage();
            Throwable t = status.getStageException();
            if (msg != null && msg.trim().length() > 0) {
                pw.println(msg);
                // only print the exception if it's not the same as the 
                // the status message 
                if (t != null && t.getMessage() != null && 
                    !t.getMessage().equals(msg)) {
                    pw.println(t.toString());
                }
            } else {
                if (t != null) {
                    pw.println(t.toString());
                }
            }
        }

}
