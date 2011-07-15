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

//----------------------------------------------------------------------------
//
// Module:      RegisteredResources.java
//
// Description: Transaction participant management.
//
// Product:     com.sun.jts.CosTransactions
//
// Author:      Simon Holdsworth
//
// Date:        March, 1997
//
// Copyright (c):   1995-1997 IBM Corp.
//
//   The source code for this program is not published or otherwise divested
//   of its trade secrets, irrespective of what has been deposited with the
//   U.S. Copyright Office.
//
//   This software contains confidential and proprietary information of
//   IBM Corp.
//----------------------------------------------------------------------------

package com.sun.jts.CosTransactions;

import java.util.*;

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;

import com.sun.jts.codegen.otsidl.*;
import com.sun.jts.trace.*;
import com.sun.jts.jtsxa.OTSResourceImpl;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

import javax.transaction.xa.*;
/**
 * The RegisteredResources class provides operations that manage a list
 * of Resource objects involved in a transaction, and their states relative
 * to the transaction. Resource references are stored in lists as there is no
 * way to perform Resource reference comparisons. As an instance of this
 * class may be accessed from multiple threads within a process,
 * serialisation for thread-safety is necessary in the implementation.
 * The information recorded in an instance of this class needs to be
 * reconstructible in the case of a system failure.
 *
 * @version 0.02
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */

//----------------------------------------------------------------------------
// CHANGE HISTORY
//
// Version By     Change Description
//   0.01  SAJH   Initial implementation.
//   0.02  GDH    Gordon Hutchison April 1998
//                Added OBLECT_NOT_EXIST as an allowable exception in
//                distribute rollback.
//----------------------------------------------------------------------------

class RegisteredResources {
    private ArrayList           resourceObjects = null;
    private ArrayList           resourceStates = null;
    private CoordinatorLog   logRecord = null;
    private java.lang.Object logSection = null;
    private java.lang.Object heuristicLogSection = null;
    private Resource laoResource = null;
    private CoordinatorImpl coord = null;
    private static boolean lastXAResCommit = Boolean.getBoolean("com.sun.jts.lastagentcommit");
    // START IASRI 4662745
    //private int commitRetries = -1;
    // private static int commitRetries = -1;
    // END IASRI 4662745

    //introduced for performance - remembers ths number of registered resources
    private int nRes=0;

    // private static String commitRetryVar =
    //     Configuration.getPropertyValue(Configuration.COMMIT_RETRY);

    // private final static long COMMIT_RETRY_WAIT = 60000; // moved to Configuration.java
    private final static String LOG_SECTION_NAME = "RR"/*#Frozen*/;
    private final static String HEURISTIC_LOG_SECTION_NAME = "RRH"/*#Frozen*/;

	/*
		Logger to log transaction messages
	*/  
	    static Logger _logger = LogDomains.getLogger(RegisteredResources.class, LogDomains.TRANSACTION_LOGGER);
    /**
     * Defines the CoordinatorLog which is to be used for recovery.
     * <p>
     * The CoordinatorLog for the transaction is updated with empty sections
     * for RegisteredResources and RegisteredResourcesHeuristic created.
     * <p>
     * Initialises the list of RegisteredResources to be empty.
     * <p>
     * The CoordinatorLog is used as the basis for recovering the
     * RegisteredResources and RegisteredResourcesHeuristic information at
     * restart.
     *
     * @param log  The CoordinatorLog object for the transaction.
     *
     * @return
     *
     * @see
     */
    RegisteredResources(CoordinatorLog log, CoordinatorImpl coord) {

        resourceObjects = new ArrayList();
        resourceStates  = new ArrayList();

        // Create two sections in the CoordinatorLog object for Resources.

        logRecord = log;

        //if (log != null) {

            // Create section for Resources registered as part of transaction

          // logSection = log.createSection(LOG_SECTION_NAME);

            // Create section for Resources with heuristic actions taken

          // heuristicLogSection = log.createSection(HEURISTIC_LOG_SECTION_NAME);
        //}
        this.coord = coord;
    }

    /**
     * Default RegisteredResources constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    RegisteredResources(CoordinatorImpl coord) { this.coord = coord;}

    /**
     * Directs the RegisteredResources to recover its state after a failure.
     * <p>
     * This is based on the given CoordinatorLog object. The participant list
     * and heuristic information is reconstructed.
     *
     * @param log  The CoordinatorLog holding the RegisteredResources state.
     *
     * @return
     *
     * @see
     */

    void reconstruct(CoordinatorLog log) {


        // Set up the instance variables.

        resourceObjects = new ArrayList();
        resourceStates  = new ArrayList();
        boolean infiniteRetry = true;

        // First, get the retry count.

        /**
        if (commitRetries == -1 && commitRetryVar != null) {
            try {
                commitRetries = Integer.parseInt(commitRetryVar);
            } catch (Throwable e) {}

            infiniteRetry = false;
        }
        **/
        int commitRetries = Configuration.getRetries();
        if (commitRetries >= 0)
            infiniteRetry = false;

        // Reconstruct our state from CoordinatorLog object.

        // Get the Registered Resources and the Registered Resources that had
        // Heuristic results.  Return the list of Registered Resources that did
        // not have any Heuristic results.

        heuristicLogSection = log.createSection(HEURISTIC_LOG_SECTION_NAME);
        java.lang.Object[] resources = log.getObjects(heuristicLogSection);
        for(int i = 0; i < resources.length; i++) {
            boolean exceptionThrown=true;
            int commitRetriesLeft = commitRetries;
            while(exceptionThrown){
                try {
                    Resource res =
                        ResourceHelper.narrow((org.omg.CORBA.Object)resources[i]);
                    if (res != null) {
                        resourceObjects.add(res);
			nRes++;
                        resourceStates.add(ResourceStatus.Heuristic);
                    }
		    exceptionThrown=false;
                } catch (Throwable exc) {
                     if(exc instanceof TRANSIENT || exc instanceof COMM_FAILURE) {
                         // If the exception is either TRANSIENT or
                         // COMM_FAILURE, keep retrying

                         //$ CHECK WITH DSOM FOLKS FOR OTHER EXCEPTIONS
						 _logger.log(Level.WARNING,"jts.exception_on_resource_operation",
                                       new java.lang.Object[]{exc.toString(),
									   "reconstruct"});
                         if (commitRetriesLeft > 0 || infiniteRetry) {

                             // For TRANSIENT or COMM_FAILURE, wait
                             // for a while, then retry the commit.
                             if (!infiniteRetry) {
                                 commitRetriesLeft--;
                             }

                             try {
                                 Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                             } catch( Throwable e ) {}

                         } else {

                             // If the retry limit has been exceeded,
                             // end the process with a fatal error.
                             _logger.log(Level.SEVERE,"jts.retry_limit_exceeded",
	                                 new java.lang.Object[] {commitRetries, "commit"});
                             String msg = LogFormatter.getLocalizedMessage(_logger,
					"jts.retry_limit_exceeded",
			                 new java.lang.Object[] {commitRetries, "commit"});
			    throw  new org.omg.CORBA.INTERNAL(msg);
                            //exceptionThrown=false;
								//Commented out code as this statement is not
								//reachable
                        }
                    }
                    else{
                        exceptionThrown=false;
                   }
               }
            }
        }

        // Resources that did not have Heuristic outcomes are added to the list
        // Get section id for Resources registered as part of transaction

        logSection = log.createSection(LOG_SECTION_NAME);
        resources = log.getObjects(logSection);
        for (int i = 0; i < resources.length; i++) {
            boolean exceptionThrown=true;
            int commitRetriesLeft = commitRetries;
            while(exceptionThrown){
                try {
                    Resource res =
                        ResourceHelper.narrow((org.omg.CORBA.Object)resources[i]);
                    if (res != null) {
                        resourceObjects.add(res);
			nRes++;
                        resourceStates.add(ResourceStatus.Registered);
                    }
		    exceptionThrown=false;
                } catch (Throwable exc) {
                     if(exc instanceof TRANSIENT || exc instanceof COMM_FAILURE) {
                         // If the exception is either TRANSIENT or
                         // COMM_FAILURE, keep retrying

                         //$ CHECK WITH DSOM FOLKS FOR OTHER EXCEPTIONS
						_logger.log(Level.WARNING,"jts.exception_on_resource_operation",
                                new java.lang.Object[] {exc.toString(),"reconstruct"});
                         if (commitRetriesLeft > 0 || infiniteRetry) {

                             // For TRANSIENT or COMM_FAILURE, wait
                             // for a while, then retry the commit.
                             if (!infiniteRetry) {
                                 commitRetriesLeft--;
                             }

                             try {
                                 Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                             } catch( Throwable e ) {}

                         } else {

                             // If the retry limit has been exceeded,
                             // end the process with a fatal error.
			     _logger.log(Level.SEVERE,"jts.retry_limit_exceeded",
	                                 new java.lang.Object[] {commitRetries, "commit"});
			     String msg = LogFormatter.getLocalizedMessage(_logger,
					"jts.retry_limit_exceeded",
			                 new java.lang.Object[] {commitRetries, "commit"});
							 		
			    throw  new org.omg.CORBA.INTERNAL(msg);
                           // exceptionThrown=false;
						   //Commented out as this will not be executed 
                        }
                    }
                    else{
                        exceptionThrown=false;
                   }
               }

		   }
	   }

        logRecord = log;
    }
    /**
     * Adds a reference to a Resource object to the list in the
     * registered state.
     *
     * @param obj  The reference of the Resource object to be stored.
     *
     * @return  The number of registered Resource objects.
     *
     * @see
     */
    int addRes(Resource obj) {

        //int result;

        // Add the reference to the list (which was created when this object
        // was created), with the "registered" status.

        resourceObjects.add(obj);
	nRes++;
        resourceStates.add(ResourceStatus.Registered);
        //result =nRes;

        // Dont add the reference to the log record at this point
        // as it may vote read-only.

        return nRes;
    }

    /**
     * Empties the list of registered Resources.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    /*
    void empty() {

        // Empty the list of all Resource references.

        resourceObjects.clear();
	nRes=0;
        resourceStates.clear();
    }
    */

    /**
     * Checks whether there are any Resource objects registered.
     * <p>
     * If there are, the operation returns true, otherwise false.
     *
     * @param
     *
     * @return  Indicates whether any Resources registered.
     *
     * @see
     */
    boolean involved() {

        boolean result = (nRes != 0 );

        return result;
    }

    /**
     * Returns the number of Resources currently in the list.
     *
     * @param
     *
     * @return  The number of registered Resources.
     *
     * @see
     */
    int numRegistered() {

        return nRes;
    }

    /**
     * Distributes prepare messages to all Resources in the registered state.
     * <p>
     * Resource objects that vote to commit the transaction are added to the
     * RegisteredResources section in the CoordinatorLog.
     * <p>
     * All Resources that return VoteReadOnly have their state set to
     * completed. The consolidated result is returned.
     *
     * @param
     *
     * @return  The vote for the transaction.
     *
     * @exception HeuristicMixed  Indicates that a participant voted
     *   to roll the transaction back, but one or more others have
     *   already heuristically committed.
     * @exception HeuristicHazard  Indicates that a participant voted to
     *   roll the transaction back, but one or more others may have
     *   already heuristically committed.
     *
     * @see
     */
    Vote distributePrepare() throws HeuristicMixed, HeuristicHazard {
        Vote result = Vote.VoteReadOnly;
        int laoIndex = -1;
        boolean rmErr = false;

        // Browse through the participants, preparing them, and obtain
        // a consolidated result.  The following is intended to be done
        // asynchronously as a group of perations, however if done
        // sequentially, it should stop after the first rollback vote.
        // If there are no Resource references, return the a read-only vote.

        for (int i = 0;
                i < nRes && result != Vote.VoteRollback;
                i++) {
            boolean isProxy = false;
            Resource currResource = (Resource) resourceObjects.get(i);
            

            if ((i == nRes - 1) && lastXAResCommit && (laoResource == null) && 
                result == Vote.VoteCommit) {
                try {
                    if(_logger.isLoggable(Level.FINER))
                    {
                        _logger.logp(Level.FINER,"RegisteredResources",
                            "distributePrepare()",
                            "Before invoking commit on LA resource = " +
                            currResource);
                    }
                    currResource.commit_one_phase();
                    resourceStates.set(i, ResourceStatus.Completed);
                    if(_logger.isLoggable(Level.FINER))
                    {
                        _logger.logp(Level.FINER,"RegisteredResources",
                            "distributePrepare()",
                            "After invoking commit on LA resource = "+
                            currResource);
                    }
                } catch (Throwable exc) {
                    result =  Vote.VoteRollback;
                    resourceStates.set(i,ResourceStatus.Completed);
                }
                return result;
            }

            // We determine here whether the object is subordinate or proxy
            // because the object may not exist when the prepare returns.

            //String crid = CoordinatorResourceHelper.id();
            //boolean isSubordinate = currResource._is_a( crid );

            // COMMENT(Ram J) the instanceof operation should be replaced
            // by a is_local() call, once the local object contract is
            // implemented.
            if(!(currResource instanceof OTSResourceImpl)) {
              ProxyChecker checkProxy = Configuration.getProxyChecker();
              isProxy = checkProxy.isProxy(currResource);
            }

            Vote currResult = Vote.VoteRollback;

            try {
		 		if(_logger.isLoggable(Level.FINER))
                {
					_logger.logp(Level.FINER,"RegisteredResources","prepare()",
							"Before invoking prepare() on resource:" +
							currResource);
                }
                currResult = currResource.prepare();
                //Mark this resource as LA if vote is null
                if(currResult == null) {
                    if(_logger.isLoggable(Level.FINER))
                    {
		        _logger.logp(Level.FINER,"RegisteredResources","prepare()",
				    "Marking the current resource as LAO:" +
				     currResource);
                    }
                    laoResource =  currResource;
                    laoIndex = i;
                    continue;
                }
		if(_logger.isLoggable(Level.FINER))
                {
		    _logger.logp(Level.FINER,"RegisteredResources","prepare()",
			        "After invoking prepare() on resource:" +
				 currResource + ";This resource voted : "+
				 currResult);
                }
            } catch (Throwable exc) {

                // If a heuristic exception is thrown, this is because the
                // Resource represents a subordinate which is voting to
                // rollback; one of its Resources voted to roll back after
                // others had voted to commit, and one of the commit voters
                // then raised a heuristic exception when told to roll back.
                // In this situation, we need to go back through all Resources
                // and try to roll them back, not including the one
                // which raised the exception.

                boolean hazard = exc instanceof HeuristicHazard;
                boolean internal = exc instanceof INTERNAL;
                if (exc instanceof HeuristicMixed || hazard || internal) {

                    if (!internal) {
                        // Mark the Resource which threw the exception as
                        // heuristic so that we do not
                        // try to roll it back, but we do send it a forget.

                        resourceStates.set(i,ResourceStatus.Heuristic);
                    }

                    try {
                        distributeRollback(true);
                    } catch (Throwable ex2) {

                        // If the rollback threw an exception, change
                        // HeuristicHazard to HeuristicMixed if necessary.

                        if (ex2 instanceof HeuristicMixed && hazard) {
                            hazard = false;
                        }
                    }

                    // Now throw the appropriate exception.

                    if (hazard) {
                        throw (HeuristicHazard) exc;
                    } else if (internal) {
                        throw (INTERNAL) exc;
                    } else if (exc instanceof HeuristicMixed) {
                        throw (HeuristicMixed) exc;
                    } else {
                         throw new HeuristicMixed();
                    }
                } else if (exc instanceof RuntimeException) {
                        rmErr = true;
                }

                // If any other exception is raised, assume the vote
                // is rollback.
                //$Check for specific exceptions ?
				_logger.log(Level.WARNING,"jts.exception_on_resource_operation",
                        new java.lang.Object[] {exc.toString(),"prepare"});
            }

            // Record the outcome from the participant

            // Take an action depending on the participant's vote.

            if (currResult == Vote.VoteCommit) {
                if (logRecord != null) {
                    if (!(currResource instanceof OTSResourceImpl)) {
                        if (logSection == null)
                            logSection = logRecord.createSection(LOG_SECTION_NAME);
                        logRecord.addObject(logSection, currResource);
                    }
                }

                if (result == Vote.VoteReadOnly) {
                    result = Vote.VoteCommit;
                }
            } else {

                // If a participant votes readonly, don't change the overall
                // vote. If a participant votes to rollback the transaction,
                // change the consolidated result to rollback.
                // Dont bother writing the reference to the log.
                // The state of the Resource object is set
                // to completed as we must not call it after it has voted
                // to rollback the transaction. Set the state of a participant
                // that votes read-only to completed as it
                // replies.  The consolidated vote does not change.

                if (!rmErr)
                    resourceStates.set(i,ResourceStatus.Completed);
                if (isProxy) {
                    currResource._release();
                }

                if (currResult == Vote.VoteRollback) {
                    result = currResult;
                }
            }
        }

        if (result == Vote.VoteCommit && laoResource != null) {
            try {
		if(_logger.isLoggable(Level.FINER))
               	{
		    _logger.logp(Level.FINER,"RegisteredResources",
					 "distributePrepare()",
					 "Before invoking commit on LA resource = " +
					 laoResource);
                }
                // laoResource.commit();
                resourceStates.set(laoIndex, ResourceStatus.Completed);
		if(_logger.isLoggable(Level.FINER))
                {
		    _logger.logp(Level.FINER,"RegisteredResources",
					 "distributePrepare()",
					 "After invoking commit on LA resource = "+
					 laoResource);
                }
            } catch (Throwable exc) {
                result =  Vote.VoteRollback;
                resourceStates.set(laoIndex,ResourceStatus.Completed);
            }
        }
        return result;
    }
    
    Resource getLAOResource() {
        return laoResource;
    }
    
    

    /**
     * Distributes commit messages to all Resources in the registered state
     * <p>
     * (i.e. not including those that voted VoteReadOnly). All Resources that
     * return successfully have their state set to completed; those that
     * raise heuristic exceptions are added to the
     * RegisteredResourcesHeuristic section of the CoordinatorLog and have
     * their state set to heuristic.
     * <p>
     * All Resources in the heuristic state are then told to forget,
     * their state is set to completed, and the CoordinatorLog object
     * is forced to the physical log.
     *
     * @param
     *
     * @return
     *
     * @exception HeuristicMixed  Indicates that heuristic decisions have been
     *   taken which have resulted in part of the transaction
     *   being rolled back.
     * @exception HeuristicHazard  Indicates that heuristic decisions may have
     *   been taken which have resulted in part of the transaction
     *   being rolled back.
     *
     * @see
     */
    void distributeCommit() throws HeuristicMixed, HeuristicHazard, NotPrepared {
        boolean infiniteRetry = true;

        boolean heuristicException = false;
        boolean heuristicMixed = false;
        int heuristicRollback = 0;
        int heuristicCommit = 0;
        int success = 0;

        // First, get the retry count.

        /**
        if (commitRetries == -1 && commitRetryVar != null) {
            try {
                commitRetries = Integer.parseInt(commitRetryVar);
            } catch (Throwable e) {}

            infiniteRetry = false;
        }
        **/
        int commitRetries = Configuration.getRetries();
        if (commitRetries >= 0)
            infiniteRetry = false;


        // Browse through the participants, committing them. The following is
        // intended to be done asynchronously as a group of operations.

        boolean transactionCompleted = true;
        String msg = null;
        for (int i = 0; i < nRes; i++) {
            boolean isProxy = false;
            Resource currResource = (Resource) resourceObjects.get(i);

            // If the current Resource in the browse is not in the registered
            // state, skip over it.

            if ((ResourceStatus) resourceStates.get(i) ==
                    ResourceStatus.Registered) {

                boolean heuristicRaised = false;

                // We determine here whether the object is a proxy because the
                // object may not exist when the commit returns.

                // COMMENT(Ram J) the instanceof operation should be replaced
                // by a is_local() call, once the local object contract is
                // implemented.
                if(!(currResource instanceof com.sun.jts.jtsxa.OTSResourceImpl)) {
                    ProxyChecker checkProxy = Configuration.getProxyChecker();
                    isProxy = checkProxy.isProxy(currResource);
                }

                // Change the current Resource's state to completing.

                resourceStates.set(i,ResourceStatus.Completing);

                // Tell the resource to commit.
                // Catch any exceptions here; keep going until
                // no exception is left.

                int commitRetriesLeft = commitRetries;
                boolean exceptionThrown = true;
                while (exceptionThrown) {
                    try {
						if(_logger.isLoggable(Level.FINER))
                    	{
							_logger.logp(Level.FINER,"RegisteredResources",
									"distributeCommit()",
									"Before invoking commit on resource = " +
									currResource);
                    	}
                        currResource.commit();
						if(_logger.isLoggable(Level.FINER))
                    	{
							_logger.logp(Level.FINER,"RegisteredResources",
									"distributeCommit()",
									"After invoking commit on resource = "+
									currResource);
                    	}
                        exceptionThrown = false;
                    } catch (Throwable exc) {

                        if (exc instanceof HeuristicCommit || 
                            // Work around the fact that org.omg.CosTransactions.ResourceOperations#commit
                            // does not declare HeuristicCommit exception
                            (exc instanceof HeuristicHazard && exc.getCause() instanceof XAException && 
                                ((XAException)exc.getCause()).errorCode == XAException.XA_HEURCOM)) {

                            // If the exception is Heuristic Commit, remember
                            // that a heuristic exception has been raised.
                            heuristicException = true;
                            heuristicRaised = true;
                            heuristicMixed = true;
                            exceptionThrown = false;
                            heuristicCommit++;

                        } else if (exc instanceof HeuristicRollback ||
                                   exc instanceof HeuristicHazard ||
                                   exc instanceof HeuristicMixed) {
                            // If the exception is Heuristic Rollback,
                            // Mixed or Hazard, remember that a heuristic
                            // exception has been raised, and also that
                            // damage has occurred.

                            heuristicException = true;
                            if (exc instanceof HeuristicRollback) {
                                heuristicRollback++;
                            }
                            heuristicMixed = !(exc instanceof HeuristicHazard);
                            heuristicRaised = true;
                            exceptionThrown = false;

                        } else if (exc instanceof INV_OBJREF ||
                                   exc instanceof OBJECT_NOT_EXIST) {

                            // If the exception is INV_OBJREF, then the target
                            // Resource object must have already committed.
                            exceptionThrown = false;

                        } else if (exc instanceof NotPrepared) {

                            // If the exception is NotPrepared, then the target
                            // Resource has not recorded the fact that it has
                            // been called for prepare, or some internal glitch
                            // has happened inside the RegisteredResources /
                            // TopCoordinator.  In this case the only sensible
                            // action is to end the process with a fatal error
                            // message.
							_logger.log(Level.SEVERE,
									"jts.exception_on_resource_operation",
	                                 new java.lang.Object[] {exc.toString(),"commit"});
							
                             throw (NotPrepared)exc;
                             /**
							 msg = LogFormatter.getLocalizedMessage(_logger,
					 							"jts.exception_on_resource_operation",
	                                 			new java.lang.Object[] {exc.toString(),
												"commit"});
							 throw  new org.omg.CORBA.INTERNAL(msg);
                             **/
                        } else if (!(exc instanceof TRANSIENT) &&
                                   !(exc instanceof COMM_FAILURE)) {
                            // If the exception is neither TRANSIENT or
                            // COMM_FAILURE, it is unexpected, so display a
                            // message and give up with this Resource.

                            //$ CHECK WITH DSOM FOLKS FOR OTHER EXCEPTIONS
							_logger.log(Level.SEVERE,
									"jts.exception_on_resource_operation",
	                                 new java.lang.Object[] {exc.toString(),"commit"});
							
                             exceptionThrown = false;
                             transactionCompleted = false;
							 msg = LogFormatter.getLocalizedMessage(_logger,
							 					"jts.exception_on_resource_operation",
			                           			new java.lang.Object[] {exc.toString(),
												"commit"});

                        } else if (commitRetriesLeft > 0 || infiniteRetry) {

                            // For TRANSIENT or COMM_FAILURE, wait
                            // for a while, then retry the commit.
                            if (!infiniteRetry) {
                                commitRetriesLeft--;
                            }

                            try {
                                Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                            } catch( Throwable e ) {}

                        } else {

                            // If the retry limit has been exceeded,
                            // end the process with a fatal error.
			    _logger.log(Level.SEVERE,"jts.retry_limit_exceeded",
	                               new java.lang.Object[] {commitRetries, "commit"});

                             exceptionThrown = false;
                             transactionCompleted = false;
			     msg = LogFormatter.getLocalizedMessage(_logger,
					"jts.retry_limit_exceeded",
	                       		new java.lang.Object[] {commitRetries, "commit"});
                        }
                    }
                }


                if (heuristicRaised) {

                    // Either mark the participant as having raised a heuristic
                    // exception, or as completed.

                    resourceStates.set(i,ResourceStatus.Heuristic);

                    if (logRecord != null) {
                        if (!(currResource instanceof OTSResourceImpl)) {
                            if (heuristicLogSection == null)
                                heuristicLogSection = 
                                        logRecord.createSection(HEURISTIC_LOG_SECTION_NAME);
                            logRecord.addObject(heuristicLogSection, currResource);
                        }
                    }
                } else {

                    // If completed, and the object is a proxy,
                    // release the proxy now.

                    resourceStates.set(i,ResourceStatus.Completed);
                    success++;
                    if (isProxy) {
                        currResource._release();
                    }
                }
            }
        }

        // The browse is complete.
        // If a heuristic exception was raised, perform forget processing. This
        // will then throw the appropriate heuristic exception to the caller.
        // Note that HeuristicHazard exception with be converted to the HeuristicRolledbackException
        // by the caller

        if (heuristicException) {
          boolean heuristicHazard = true;
          if ((heuristicCommit + success) == nRes) {
              heuristicMixed = false;
              heuristicHazard = false;
          } else if (heuristicRollback == nRes) {
              heuristicMixed = false;
          }
          distributeForget(commitRetries, infiniteRetry, heuristicHazard, heuristicMixed);
        }

        if (!transactionCompleted) {
            if (coord != null)
                RecoveryManager.addToIncompleTx(coord, true);
            if (msg !=  null)
                throw  new org.omg.CORBA.INTERNAL(msg);
            else
                throw  new org.omg.CORBA.INTERNAL();
        }

        // Otherwise just return normally.
    }

    /**
     * Distributes rollback messages to all Resources in the registered state.
     * <p>
     * (i.e. not including those that voted VoteReadOnly). All Resources that
     * return successfully have their state set to completed; those that raise
     * heuristic exceptions are added to the RegisteredResourcesHeuristic
     * section of the CoordinatorLog and have their state set to heuristic.
     * <p>
     * All Resources in the heuristic state are then told to forget,
     * their state is set to completed, and the CoordinatorLog object
     * is forced to the physical log.
     *
     * @param heuristicException  Indicates that a heuristic exception has
     *   already been thrown for this transaction, and that forget
     *   processing should proceed.
     *
     * @return
     *
     * @exception HeuristicMixed  Indicates that heuristic decisions have been
     *   taken which have resulted in part of the transaction
     *   being rolled back.
     * @exception HeuristicHazard  Indicates that heuristic decisions
     *   may have been taken which have resulted in part of the transaction
     *   being rolled back.
     *
     * @see
     */
    void distributeRollback(boolean heuristicException)
            throws HeuristicMixed, HeuristicHazard {

        boolean infiniteRetry = true;
        boolean heuristicMixed = false;
        int heuristicRollback = 0;
        int success = 0;
        int processed = 0;

        // First, get the retry count.

        /**
        if (commitRetries == -1 && commitRetryVar != null) {

            try {
                commitRetries = Integer.parseInt(commitRetryVar);
            } catch (Throwable e) {}

            infiniteRetry = false;
        }
        **/
        int commitRetries = Configuration.getRetries();
        if (commitRetries >= 0)
            infiniteRetry = false;



        // Browse through the participants, committing them. The following is
        // intended to be done asynchronously as a group of operations.

        boolean transactionCompleted = true;
        String msg  = null;
        for (int i = 0; i < nRes; i++) {
            boolean isProxy = false;
            Resource currResource = (Resource)resourceObjects.get(i);

            // If the current Resource in the browse is not in the registered
            // state, skip over it.

            if (resourceStates.get(i).equals(
                    ResourceStatus.Registered)) {
                processed++;
                boolean heuristicRaised = false;

                // We determine here whether the object is a proxy because
                // the object may not exist when the commit returns.

                // COMMENT(Ram J) the instanceof operation should be replaced
                // by a is_local() call, once the local object contract is
                // implemented.
                if (!(currResource instanceof com.sun.jts.jtsxa.OTSResourceImpl)) {
                    ProxyChecker checkProxy = Configuration.getProxyChecker();
                    isProxy = checkProxy.isProxy(currResource);
                }

                // Change the current Resource's state to completing.

                resourceStates.set(i,ResourceStatus.Completing);

                // Tell the resource to commit.
                // Catch any exceptions here; keep going
                // until no exception is left.

                int rollbackRetriesLeft = commitRetries;
                boolean exceptionThrown = true;
                while (exceptionThrown) {
                    try {
						if(_logger.isLoggable(Level.FINER))
                    	{
							_logger.logp(Level.FINER,"RegisteredResources",
									"distributeRollback()",
									"Before invoking rollback on resource = "+
									currResource);
                    	}
                        currResource.rollback();
						if(_logger.isLoggable(Level.FINER))
                    	{
							_logger.logp(Level.FINER,"RegisteredResources",
									"distributeRollback()",
									"After invoking rollback on resource = "+
									currResource);
                    	}
                        exceptionThrown = false;
                    } catch (Throwable exc) {

                        if (exc instanceof TRANSACTION_ROLLEDBACK) {

                            // If the exception is TRANSACTION_ROLLED back,
                            // then continue.
                            exceptionThrown = false;

                        } else if (exc instanceof HeuristicRollback) {

                            // If the exception is Heuristic Rollback,
                            // remember that a heuristic exception
                            // has been raised.
                            heuristicException = true;
                            heuristicRaised = true;
                            exceptionThrown = false;
                            heuristicRollback++;

                        } else if (exc instanceof HeuristicCommit ||
                                   exc instanceof HeuristicHazard ||
                                   exc instanceof HeuristicMixed) {

                            // If the exception is Heuristic Rollback, Mixed
                            // or Hazard, remember that a heuristic exception
                            // has been raised, and also that damage has
                            // occurred.
                            heuristicException = true;
                            heuristicMixed = !(exc instanceof HeuristicHazard);
                            heuristicRaised = true;
                            exceptionThrown = false;

                            // Work around the fact that org.omg.CosTransactions.ResourceOperations#rollback
                            // does not declare HeuristicRollback exception
                            if (exc instanceof HeuristicHazard && exc.getCause() instanceof XAException && 
                                    ((XAException)exc.getCause()).errorCode == XAException.XA_HEURRB) {
                                heuristicRollback++;
                            }

                        } else if (exc instanceof INV_OBJREF ||
                                   exc instanceof OBJECT_NOT_EXIST){

                            //GDH added NOT_EXIST
                            // If the exception is INV_OBJREF, then the target
                            // Resource object must have already rolled back.
                            exceptionThrown = false;

                        } else if (!(exc instanceof TRANSIENT) &&
                                   !(exc instanceof COMM_FAILURE)) {

                            // If the exception is neither TRANSIENT or
                            // COMM_FAILURE, it is unexpected, so display
                            // a message and give up with this Resource.
							_logger.log(Level.SEVERE,
									"jts.exception_on_resource_operation",
                                     new java.lang.Object[]
                                          { exc.toString(), "rollback"});

							  msg = LogFormatter.getLocalizedMessage(_logger,
							 				"jts.exception_on_resource_operation",
                                           new java.lang.Object[] { exc.toString(),
												"rollback"});
                              exceptionThrown = false;
                              transactionCompleted = false;

                        } else if (rollbackRetriesLeft > 0 || infiniteRetry) {

                            // For TRANSIENT or COMM_FAILURE, wait for a while,
                            // then retry the rollback.

                            if (!infiniteRetry) {
                                rollbackRetriesLeft--;
                            }

                            try {
                                Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                            } catch( Throwable e ) {}

                        } else {

                            // If the retry limit has been exceeded, end the
                            // process with a fatal error.
			    _logger.log(Level.SEVERE,"jts.retry_limit_exceeded",
	                                new java.lang.Object[] {commitRetries, "rollback"});

			    msg = LogFormatter.getLocalizedMessage(_logger,
					"jts.retry_limit_exceeded",
	                       		new java.lang.Object[] {commitRetries, "rollback"});
												
                            exceptionThrown = false;
                            transactionCompleted = false;
                        }
                    }
                }

                if (heuristicRaised) {

                    // Either mark the participant as having raised a
                    // heuristic exception, or as completed.

                    resourceStates.set(i,ResourceStatus.Heuristic);
                    if (logRecord != null) {
                        if (!(currResource instanceof OTSResourceImpl)) {
                            if (heuristicLogSection == null)
                                heuristicLogSection =
                                        logRecord.createSection(HEURISTIC_LOG_SECTION_NAME);

                            logRecord.addObject(heuristicLogSection,currResource);
                        }
                    }

                } else {

                    success++;
                    // If completed, and the object is a proxy,
                    // release the proxy now.

                    resourceStates.set(i,ResourceStatus.Completed);
                    if (isProxy) {
                        currResource._release();
                    }
                }
            }
        }


        // The browse is complete.
        // If a heuristic exception was raised, perform forget processing.
        // This will then throw the appropriate heuristic exception
        // to the caller.

        if (heuristicException)
            distributeForget(commitRetries, infiniteRetry, 
                    (((heuristicRollback + success) == processed)? false : true), heuristicMixed);

        if (!transactionCompleted) {
            if (coord != null)
                RecoveryManager.addToIncompleTx(coord, false);
            if (msg !=  null)
                throw  new org.omg.CORBA.INTERNAL(msg);
            else
                throw  new org.omg.CORBA.INTERNAL();
        }

        // Otherwise just return normally.
    }

    /**
     * Distributes forget messages to all Resources in the heuristic state.
     * <p>
     * (i.e. only those which have thrown heuristic exceptions on prepare,
     * commit or rollback).
     * <p>
     * All Resources then have their state set to completed.
     *
     * @param retries   The number of times to retry the forget operation.
     * @param infinite  indicates infinite retry.
     *
     * @return
     *
     * @exception HeuristicMixed  Indicates that heuristic decisions have been
     *   taken which have resulted in part of the transaction
     *   being rolled back.
     * @exception HeuristicHazard  Indicates that heuristic decisions may have
     *   been taken which have resulted in part of the transaction being
     *   rolled back.
     *
     * @see
     */
    private void distributeForget(int retries, boolean infinite,
            boolean heuristicHazard, boolean heuristicMixed) throws HeuristicMixed, HeuristicHazard {

        // Force the log record to ensure that all
        // heuristic Resources are logged.

        if (logRecord != null) {
            logRecord.write(true);
        }

        // Browse through the remaining participants, informing them that they
        // may forget the heuristic information at this point

        for (int i = 0; i < nRes; i++) {
            boolean isProxy = false;
            // If the current Resource in the browse is not in the heuristic
            // state, skip over it.

            if ((ResourceStatus)resourceStates.get(i) ==
                    ResourceStatus.Heuristic) {

                Resource currResource = (Resource)resourceObjects.get(i);

                // We determine here whether the object is a proxy because
                // the object may not exist when the forget returns.

                // COMMENT(Ram J) the instanceof operation should be replaced
                // by a is_local() call, once the local object contract is
                // implemented.
                if(!(currResource instanceof com.sun.jts.jtsxa.OTSResourceImpl)) {
                    ProxyChecker checkProxy = Configuration.getProxyChecker();
                    isProxy = checkProxy.isProxy(currResource);
                }

                // Tell the resource to forget.

                int retriesLeft = retries;
                boolean exceptionThrown = true;
                while (exceptionThrown) {
                    try {
                        currResource.forget();
                        exceptionThrown = false;
                    } catch (Throwable exc) {

                        if (exc instanceof INV_OBJREF ||
                                exc instanceof OBJECT_NOT_EXIST) {

                            // If the exception is INV_OBJREF, then the target
                            // Resource object must have already forgotten.
                            exceptionThrown = false;

                        } else if (!(exc instanceof COMM_FAILURE) &&
                                   !(exc instanceof TRANSIENT)) {

                            // If the exception is neither TRANSIENT or
                            // COMM_FAILURE, it is unexpected, so display
                            // a message and give up with this Resource.
                            //$ CHECK WITH DSOM FOLKS FOR OTHER EXCEPTIONS
                              exceptionThrown = false;

                        } else if (retriesLeft > 0 || infinite) {

                            // For TRANSIENT or COMM_FAILURE, wait for a while,
                            // then retry the forget.
                            if (!infinite) {
                                retriesLeft--;
                            }

                            try {
                                Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                            } catch( Throwable e ) {}

                        } else {

                            // If the retry limit has been exceeded,
                            // end the process with a fatal error.
			    _logger.log(Level.SEVERE,"jts.retry_limit_exceeded",
	                                new java.lang.Object[] { retries, "forget"});
			    String msg = LogFormatter.getLocalizedMessage(_logger,
					"jts.retry_limit_exceeded",
	                       		new java.lang.Object[] {retries, "forget"});
			    throw  new org.omg.CORBA.INTERNAL(msg);
                        }
                    }
                }

                // Set the state of the Resource to completed
                // after a successful forget. If the
                // Resource is a proxy, release the reference.

                resourceStates.set(i,ResourceStatus.Completed);
                if (isProxy) {
                    currResource._release();
                }
            }
        }

        // If the original commit or rollback threw a HeuristicHazard
        // or HeuristicMixed exception was raised, throw it now.

        if (heuristicMixed) {
            HeuristicMixed exc = new HeuristicMixed();
            throw exc;
        } else if (heuristicHazard) {
            HeuristicHazard exc = new HeuristicHazard();
            throw exc;
        }
    }

    /**
     * Distributes commitSubtransaction messages to all registered
     * SubtransactionAwareResources.
     *
     * @param parent  The parent's Coordinator reference.
     *
     * @return
     *
     * @exception TRANSACTION_ROLLEDBACK  The subtransaction could not be
     *   committed. Some participants may have committed and some may have
     *   rolled back.
     *
     * @see
     */
    void distributeSubcommit(Coordinator parent)
            throws TRANSACTION_ROLLEDBACK {

        boolean exceptionRaised = false;

        // Browse through the participants, committing them. The following is
        // intended to be done asynchronously as a group of operations.

        for (int i = 0; i < nRes; i++) {
            boolean isProxy = false;
            SubtransactionAwareResource currResource =
                (SubtransactionAwareResource)resourceObjects.get(i);

            // COMMENT(Ram J) the instanceof operation should be replaced
            // by a is_local() call, once the local object contract is
            // implemented.
            if(!(currResource instanceof com.sun.jts.jtsxa.OTSResourceImpl)) {
                ProxyChecker checkProxy = Configuration.getProxyChecker();
                isProxy = checkProxy.isProxy(currResource);
            }

           // Tell the object to commit.

            try {
                currResource.commit_subtransaction(parent);
            } catch (Throwable exc) {

                // Check for exceptions.
                // If the TRANSACTION_ROLLEDBACK exception was raised,
                // remember it.

                if (exc instanceof TRANSACTION_ROLLEDBACK) {
                    exceptionRaised = true;
                }
            }

            // Change the state of the object.

            resourceStates.set(i,ResourceStatus.Completed);

            // If the Resource is a proxy, release it.

            if (isProxy) {
                currResource._release();
            }
        }

        // If an exception was raised, return it to the caller.

        if (exceptionRaised) {
            throw new TRANSACTION_ROLLEDBACK(0,CompletionStatus.COMPLETED_YES);
        }
    }

    /**
     * Distributes rollbackSubtransaction messages to all registered
     * SubtransactionAwareResources.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    void distributeSubrollback() {
        // Browse through the participants, rolling them back. The following is
        // intended to be done asynchronously as a group of operations.


        for (int i = 0; i < nRes; i++) {
            boolean isProxy = false;
            SubtransactionAwareResource currResource =
                (SubtransactionAwareResource) resourceObjects.get(i);

            // COMMENT(Ram J) the instanceof operation should be replaced
            // by a is_local() call, once the local object contract is
            // implemented.
            if(!(currResource instanceof com.sun.jts.jtsxa.OTSResourceImpl)) {
                ProxyChecker checkProxy = Configuration.getProxyChecker();
                isProxy = checkProxy.isProxy(currResource);
            }

            // Tell the object to roll back.

            try {
                currResource.rollback_subtransaction();
            } catch (Throwable exc) {}

            resourceStates.set(i,ResourceStatus.Completed);

            // If the Resource is a proxy, release it.

            if (isProxy) {
                currResource._release();
            }
        }
    }

    /**
     * Fills in the given Vector objects with the currently registered Resource
     * objects and their corresponding states.
     *
     * @param resources  The object to hold the objects.
     * @param states     The object to hold the states.
     *
     * @return
     *
     * @see
     */
    /* COMMENT(Ram J) - we do not support Admin package anymore.
    void getResources(ResourceSequenceHolder resources,
                      ResourceStatusSequenceHolder states) {

        resources.value = new Resource[resourceObjects.size()];
        states.value = new ResourceStatus[resourceObjects.size()];

        resourceObjects.copyInto(resources.value);
        resourceStates.copyInto(states.value);
    }
    */

    /**
     * Distributes the one phase commit.
     *
     * @param none
     *
     * @return void
     *
     * @see
     */
    void commitOnePhase() throws HeuristicMixed, HeuristicHazard {

        boolean infiniteRetry = true;

        boolean heuristicRaisedSetStatus = false;
        boolean heuristicExceptionFlowForget = false;
        boolean isProxy = false;

        boolean    heuristicMixed = false;
        boolean    heuristicHazard = false;
        boolean    rollback_occurred = false;
        boolean    outstanding_resources = true;
        int        retry_limit;
        int        no_of_attempts;

        // First, get the retry count.

        /**
        if (commitRetries == -1 && commitRetryVar != null) {
            try {
                commitRetries = Integer.parseInt(commitRetryVar);
            } catch (Throwable exc) {
				_logger.log(Level.SEVERE,"jts.exception_on_resource_operation",
                    new java.lang.Object[]
                        { exc.toString(), "CommitOnePhase commitRetryVar" });
				
				 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.exception_on_resource_operation",
					                    new java.lang.Object[]
										{ exc.toString(), "CommitOnePhase commitRetryVar" });
				  throw  new org.omg.CORBA.INTERNAL(msg);
            }
            infiniteRetry = false;
        }
        **/
        int commitRetries = Configuration.getRetries();
        if (commitRetries >= 0)
            infiniteRetry = false;


        // Check we only have one resource!
        // If not return
        if (nRes > 1) {
			_logger.log(Level.SEVERE,"jts.exception_on_resource_operation",
	                new java.lang.Object[] { "commitOnePhase", ">1 Resource"});
			 String msg = LogFormatter.getLocalizedMessage(_logger,
				 						"jts.exception_on_resource_operation",
	                					new java.lang.Object[]
										{ "commitOnePhase", ">1 Resource"});
			 throw  new org.omg.CORBA.INTERNAL(msg);
        }

        // Now we know we have one resource we can use similar
        // logic to the 'commit' method

        Resource currResource = (Resource) resourceObjects.get(0);

        // If the single resource is not in the registered state,
        // we can return too!
        // (Ram Jeyaraman) Should this condition check be !=  and then return ?
        // Previously the IF block had  '==' operator and empty block
        if ((ResourceStatus) resourceStates.get(0) !=
                ResourceStatus.Registered) {
            return;
        }

        // We determine here whether the object is a proxy because
        // the object may not exist when the commit returns.

        // COMMENT(Ram J) the instanceof operation should be replaced
        // by a is_local() call, once the local object contract is
        // implemented.
        if(!(currResource instanceof com.sun.jts.jtsxa.OTSResourceImpl)) {
            ProxyChecker checkProxy = Configuration.getProxyChecker();
            isProxy = checkProxy.isProxy(currResource);
        }

        // Change the current Resource's state to completing.

        resourceStates.set(0,ResourceStatus.Completing);

        // Tell the resource to commit.
        // Catch any exceptions here; keep going until no exception is left.

        int commitRetriesLeft = commitRetries;

        boolean exceptionThrownTryAgain = true;

        while (exceptionThrownTryAgain) {
            try {
		        if(_logger.isLoggable(Level.FINEST))
                {
					_logger.logp(Level.FINEST,"RegisteredResources","commitOnePhase()",
				  			"Before invoking commit_one_phase() on resource:" + 
							currResource );
                }

                currResource.commit_one_phase();

		        if(_logger.isLoggable(Level.FINEST))
                {
					_logger.logp(Level.FINEST,"RegisteredResources","commitOnePhase()",
                         	"After invoking commit_one_phase() on resource:" +
							currResource );
                }
                resourceStates.set(0,ResourceStatus.Completed);
                exceptionThrownTryAgain = false;
            } catch (Throwable exc) {

                if (exc instanceof TRANSACTION_ROLLEDBACK) {
                    //
                    // The resource rolled back - remember this.
                    //
                    rollback_occurred = true;
                    resourceStates.set(0,ResourceStatus.Completed);
                    exceptionThrownTryAgain = false;

                } else if (exc instanceof HeuristicHazard) {

                    // If the exception is Heuristic Rollback,
                    // Mixed or Hazard, remember that a heuristic exception
                    // has been raised, and also that damage has occurred.

						
						//IASRI START 4722883
						/**
                    	heuristicExceptionFlowForget = true;
                    	heuristicRaisedSetStatus = true;
                    	exceptionThrownTryAgain = false;
                    	heuristicMixed = false;
						**/
	   		XAException e = (XAException) ((Throwable)exc).getCause();
       			if ((e!= null) && (e.errorCode >= XAException.XA_RBBASE && e.errorCode <= XAException.XA_RBEND)) {
                    		rollback_occurred = true;
                    		resourceStates.set(0,ResourceStatus.Completed);
                    		exceptionThrownTryAgain = false;
			} else {
                    		heuristicExceptionFlowForget = true;
                    		heuristicRaisedSetStatus = true;
                    		exceptionThrownTryAgain = false;
            			if ((e!= null) && (e.errorCode == XAException.XA_HEURCOM)) 
                    		    heuristicHazard = false;
                                else
                    		    heuristicHazard = true;
            			if ((e!= null) && (e.errorCode == XAException.XA_HEURMIX)) 
                    		    heuristicMixed = true;
                                else
                    		    heuristicMixed = false;
			}
						//IASRI END 4722883
		

                } else if (exc instanceof INV_OBJREF ||
                        exc instanceof OBJECT_NOT_EXIST) {

                    // If the exception is INV_OBJREF, then the target Resource
                    // object must have already committed.  (Probably contacted
                    // the resource on a previous attempt but its OK response
                    // was lost due to a failure.)

                    resourceStates.set(0,ResourceStatus.Completed);
                    exceptionThrownTryAgain = false;

                } else if (exc instanceof NotPrepared) {

                    // If the exception is NotPrepared, then the target
                    // Resource is probably trying to indicate that it does not
                    // support one phase commit. Considered switching to
                    // 2PC commit for a retry here but as commit one phase is
                    // part of our specification then something is fishy
                    // if it is not supported: Throw an error.
					_logger.log(Level.SEVERE,"jts.exception_on_resource_operation",
                            new java.lang.Object[] { exc.toString(),
							"commit one phase"});
					 String msg = LogFormatter.getLocalizedMessage(_logger,
					 							"jts.exception_on_resource_operation",
                            					new java.lang.Object[] { exc.toString(),
												"commit one phase"});
					 throw  new org.omg.CORBA.INTERNAL(msg);

                } else if (!(exc instanceof TRANSIENT) &&
                        !(exc instanceof COMM_FAILURE)) {

                    // If the exception has not been mentione yet and is
                    // neither of the two below, it is unexpected,
                    // so display a message and give up with this Resource.
					_logger.log(Level.SEVERE,"jts.exception_on_resource_operation",
                            new java.lang.Object[] { exc.toString(),
							"commit one phase"});
					 String msg = LogFormatter.getLocalizedMessage(_logger,
					 							"jts.exception_on_resource_operation",
                            					new java.lang.Object[] { exc.toString(),
												"commit one phase"});
					 throw  new org.omg.CORBA.INTERNAL(msg);

                } else if (commitRetriesLeft > 0 || infiniteRetry) {

                     // For TRANSIENT or COMM_FAILURE, wait for a while,
                     // then retry the commit.
                     if (!infiniteRetry) {
                        commitRetriesLeft--;
                    }

                    try {
                        Thread.sleep(Configuration.COMMIT_RETRY_WAIT);
                    } catch (Throwable e) {}

                } else {

                    // If the retry limit has been exceeded, end the
                    // process with a fatal error.
                    // GDH Did consider carefully here whether the exception
                    // should be perculated back to the client. With 2PC
                    // because we can take action on the other resources we
                    // carry on and subsequently pass an exception to the
                    // client. But here there is little point in doing
                    // this as all recoverable work has either
                    // been done or not by this point - we have a
                    // serious problem so stop now. If the JTS was built
                    // into an application server (rather than being a
                    // toolkit for standalone apps) then we would replace
                    // all the FATAL_ERROR actions such as the one below
                    // with the actino required by the application server.
		    _logger.log(Level.SEVERE,"jts.retry_limit_exceeded",
	                        new java.lang.Object[] { commitRetries, "commitOnePhase"});
		    String msg = LogFormatter.getLocalizedMessage(_logger,
				"jts.retry_limit_exceeded",
				 new java.lang.Object[] {commitRetries,"commitOnePhase"});
		    throw  new org.omg.CORBA.INTERNAL(msg);
                }
            } // end of catch block for exceptions
        } // end while exception being raised (GDH)

        // Either mark the participant as having raised a heuristic exception,
        // or as completed.

        if (heuristicRaisedSetStatus) {
            resourceStates.set(0,ResourceStatus.Heuristic);
            if (logRecord != null) {
                // (The distributeForget method forces the log)
                if (!(currResource instanceof OTSResourceImpl)) {
                    if (heuristicLogSection == null)
                        heuristicLogSection =
                            logRecord.createSection(HEURISTIC_LOG_SECTION_NAME);
                    logRecord.addObject(heuristicLogSection,currResource);
                }
            }
        } else {
            //  Otherwise we are completed, if the object is a proxy,
            //  release the proxy now.

            resourceStates.set(0,ResourceStatus.Completed);
            if (isProxy) {
                currResource._release();
            }
        }

        // If a heuristic exception was raised, perform forget processing.
        // This will then throw the appropriate heuristic exception
        // to the caller.

        if (heuristicExceptionFlowForget) {
          distributeForget(commitRetries, infiniteRetry, heuristicHazard, heuristicMixed);
          // throw is done in method above
        }

        if (rollback_occurred) {
            throw new TRANSACTION_ROLLEDBACK(0,
                                             CompletionStatus.COMPLETED_YES);
        }

        // Otherwise just return normally.
    }

    // START IASRI 4662745
    /**
    public static void setCommitRetryVar(String commitRetryString)
    {
            if (commitRetries == -1 && commitRetryVar != null) {
            try {
                commitRetries = Integer.parseInt(commitRetryVar);
            } catch (Exception e) {}
        }
    }
    **/
    // END IASRI 4662745
}

