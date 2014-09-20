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
// Module:      Terminator.java
//
// Description: Transaction Terminator object implementation.
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

import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.CosTransactions.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;
/**
 * The TerminatorImpl interface is our implementation of the
 * standard Terminator
 * interface. It provides operations to complete a transaction, either
 * requesting commitment, or demanding rollback. The TerminatorImpl in this
 * design should be a pseudo-object, as we do not want its reference to be
 * passed to other processes, and we always want it to be created locally.
 *
 * @version 0.01
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
//----------------------------------------------------------------------------

class TerminatorImpl extends TerminatorPOA implements Terminator {

    private static POA poa = null;
    private org.omg.CosTransactions.Terminator thisRef = null;

    CoordinatorTerm coordTerm = null;

    // this is needed to cleanup properly on completion and to avoid leaks.
    ControlImpl control = null;
   	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(TerminatorImpl.class, LogDomains.TRANSACTION_LOGGER);
    /**
     * Default TerminatorImpl constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    TerminatorImpl() {}

    /**
     * Creates and initialises a new TerminatorImpl, given the Coordinator
     * object for the transaction, and a flag to indicate whether the
     * TerminatorImpl represents a subtransaction.
     *
     * @param coordinator  The Coordinator for the transaction.
     * @param subtran  A flag indicating whether the transaction is a child.
     *
     * @return
     *
     * @see
     */
    TerminatorImpl (CoordinatorImpl coordinator, boolean subtran) {

        // Initialise the instance variables.

         coordTerm = new CoordinatorTerm(coordinator, subtran);
    }

    /**
     * sets the control object that points to this terminator.
     *
     * @param control the control object this terminator belongs to.
     */
     void setControl(ControlImpl control) {
        /* This method has been newly added (Ram J) */
        this.control = control;
     }

    /**
     * This implements the checked behaviour for threads calling
     * the terminator object's completion methods directly.
     */
    private void preCompletionCheck()
        throws TRANSACTION_ROLLEDBACK, INVALID_TRANSACTION {

        /* This method has been newly added (Ram J) */

        StatusHolder status = new StatusHolder();

        // This line assigns the value to the status
        control.getLocalTID(status);

        // check if the transaction is active, else throw exception

        if (status.value != Status.StatusActive) {

            if( status.value == Status.StatusRolledBack) {
                TRANSACTION_ROLLEDBACK exc =
                    new TRANSACTION_ROLLEDBACK(0,
                                               CompletionStatus.COMPLETED_NO);
                throw exc;
            }

            INVALID_TRANSACTION exc =
                new INVALID_TRANSACTION(MinorCode.Completed,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }

        // check if there is no outstanding requests, else throw exception
        // CHECK(Ram J) should a check for active thread associations
        // be done here as well ??
        if (control.isOutgoing()) {

            INVALID_TRANSACTION exc =
                new INVALID_TRANSACTION(MinorCode.DeferredActivities,
                                        CompletionStatus.COMPLETED_NO);
            throw exc;
        }
    }

    /**
     * Requests that the transaction controlled by the Terminator object be
     * committed.
     * The commit is passed on the the CoordinatorTerm object. If the heuristic
     * report flag is set, any heuristic exception raised by the
     * root Coordinator is returned to the caller,
     * otherwise any heuristic exception is discarded.
     *
     * This operation is part of the OMG interface and must not return
     * any exceptions other than those defined in the OMG interface.
      *
     * @param reportHeuristics  Indicates whether heuristic exceptions
     *   should be passed to the caller.
     *
     * @return
     *
     * @exception HeuristicHazard  Heuristic action may have been taken by a
     *   participant in the transaction.
     * @exception HeuristicMixed  Heuristic action has been
     *   taken by a participant in the transaction so part
     *   of the transaction has been rolled back.
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    synchronized public void commit(boolean reportHeuristics)
        throws HeuristicMixed, HeuristicHazard, TRANSACTION_ROLLEDBACK {

        // for checked transaction behaviour (Ram J)
        preCompletionCheck();

        // Try to commit the transaction.  If the client does not want a
        // heuristic report, then the transaction can be completed promptly.

        // If the client does not want heuristic reporting, and an
        // exception was raised, and it is a Heuristic exception, forget it.
        // Any other exception that is raised is returned to the client.

        try {
            coordTerm.commit(!reportHeuristics);
        } catch (HeuristicMixed exc) {
            if (reportHeuristics) {
                control.destroy(); // added (Ram J) for memory Leak fix
                throw exc;
            }
        } catch (HeuristicHazard exc) {
            if (reportHeuristics) {
                control.destroy(); // added (Ram J) for memory Leak fix
                throw exc;
            }
        } catch (TRANSACTION_ROLLEDBACK exc) {
            control.destroy(); // added (Ram J) for memory Leak fix
            throw exc;
        } catch(LogicErrorException exc) {
            control.destroy(); // added (Ram J) for memory Leak fix
            INTERNAL ex2 = new INTERNAL(MinorCode.LogicError,
                                        CompletionStatus.COMPLETED_NO);
            throw ex2;
        } catch (INTERNAL exc) { // added (Ram J) percolate up system excs
            control.destroy();
            throw (INTERNAL) exc;
        }

        control.destroy(); // added (Ram J) for memory Leak fix
    }

    /**
     * Demands that the transaction represented by the Terminator object
     * be rolled back.
     * No heuristics are reported by this operation so if the root Coordinator
     * raises a heuristic exception, it is cleared before the operation
     * returns to the caller.
     *
     * This operation is part of the OMG interface and must not return
     * any exceptions other than those defined in the OMG interface.
     *
     * @param
     *
     * @return
     *
     * @exception SystemException  The operation failed.
     *
     * @see
     */
    public void rollback() throws SystemException {

        // for checked transaction behaviour (Ram J)
        preCompletionCheck();

        // Roll the transaction back.
        // If a Heuristic exception was raised, forget it.
        // Any other exception that is raised is returned to the client.

        try {
            coordTerm.rollback();
        } catch (HeuristicMixed exc) {
            control.destroy(); // added (Ram J) for memory Leak fix
        } catch (HeuristicHazard exc) {
            control.destroy(); // added (Ram J) for memory Leak fix
        } catch (TRANSACTION_ROLLEDBACK exc) {
            control.destroy(); // added (Ram J) for memory Leak fix
            throw exc;
        } catch (LogicErrorException exc) {
            control.destroy(); // added (Ram J) for memory Leak fix
            INTERNAL ex2 = new INTERNAL(MinorCode.LogicError,
                                        CompletionStatus.COMPLETED_NO);
            throw ex2;
        } catch (INTERNAL exc) { // added (Ram J) percolate up system excs
            control.destroy();
            throw (INTERNAL) exc;
        }

        control.destroy(); // added (Ram J) for memory Leak fix

    }

    /**
     * Returns the CORBA Object which represents this object.
     *
     * @param
     *
     * @return  The CORBA object.
     *
     * @see
     */
    synchronized final Terminator object() {
        if (thisRef == null) {
            if (poa == null) {
                poa = Configuration.getPOA("transient"/*#Frozen*/);
            }

            try {
                poa.activate_object(this);
                thisRef =
                    TerminatorHelper.narrow(poa.servant_to_reference(this));
                //thisRef = (Terminator) this;
            } catch(ServantAlreadyActive saexc) {
				_logger.log(Level.SEVERE,"jts.create_terminator_object_error",saexc);
				String msg = LogFormatter.getLocalizedMessage(_logger,
										 "jts.create_terminator_object_error");
				throw  new org.omg.CORBA.INTERNAL(msg);
            } catch(ServantNotActive snexc) {
				_logger.log(Level.SEVERE,"jts.create_terminator_object_error",snexc);
				String msg = LogFormatter.getLocalizedMessage(_logger,
										 "jts.create_terminator_object_error");
				throw  new org.omg.CORBA.INTERNAL(msg);
            } catch(Exception exc) {
				_logger.log(Level.SEVERE,"jts.create_terminator_object_error",exc);
				String msg = LogFormatter.getLocalizedMessage(_logger,
										 "jts.create_terminator_object_error");
				throw  new org.omg.CORBA.INTERNAL(msg);
            }
        }

        return thisRef;
    }

    /**
     * Destroys the TerminatorImpl object.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    synchronized final void destroy() {
        if (poa != null && thisRef != null) {
            try {
                poa.deactivate_object(poa.reference_to_id(thisRef));
                thisRef = null;
            } catch (Exception exc) {
				_logger.log(Level.WARNING,"jts.object_destroy_error","Terminator");
            }
        }

    }

    /*
     * These methods are there to satisy the compiler. At some point
     * when we move towards a tie based model, the org.omg.Corba.Object
     * interface method implementation below shall be discarded.
     */

    public org.omg.CORBA.Object _duplicate() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public void _release() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public boolean _is_a(String repository_id) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public boolean _is_equivalent(org.omg.CORBA.Object that) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public boolean _non_existent() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public int _hash(int maximum) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public Request _request(String operation) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public Request _create_request(Context ctx,
				   String operation,
				   NVList arg_list,
				   NamedValue result) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public Request _create_request(Context ctx,
				   String operation,
				   NVList arg_list,
				   NamedValue result,
				   ExceptionList exceptions,
				   ContextList contexts) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public org.omg.CORBA.Object _get_interface_def() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public org.omg.CORBA.Policy _get_policy(int policy_type) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public org.omg.CORBA.DomainManager[] _get_domain_managers() {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }

    public org.omg.CORBA.Object _set_policy_override(
            org.omg.CORBA.Policy[] policies,
            org.omg.CORBA.SetOverrideType set_add) {
        throw new org.omg.CORBA.NO_IMPLEMENT("This is a locally constrained object.");
    }
}
