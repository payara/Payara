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

package com.sun.jts.jta;

import java.util.*;
import javax.transaction.Synchronization;

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;
import org.omg.PortableServer.POA;
import com.sun.jts.CosTransactions.Configuration;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;

/**
 * An implementation of org.omg.CosTransactions.Synchronization
 * this object is activated at creation time and de-activated
 * when after_completion is called
 *
 * @author Tony Ng
 */
public class SynchronizationImpl extends SynchronizationPOA
        implements org.omg.CosTransactions.Synchronization {

    private Vector syncs;
    private Vector interposedSyncs;
    private POA poa;
    private org.omg.CosTransactions.Synchronization corbaRef = null;
    private TransactionState state = null;
	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(SynchronizationImpl.class, LogDomains.TRANSACTION_LOGGER);

    public SynchronizationImpl() {
        syncs = new Vector();
        interposedSyncs = new Vector();
        poa = Configuration.getPOA("transient"/*#Frozen*/);
    }

    public SynchronizationImpl(TransactionState state) {
        this();
        this.state = state;
    }

    public void addSynchronization(Synchronization sync, 
                                   boolean interposed) {
        if (!interposed)
            syncs.addElement(sync);
        else
            interposedSyncs.addElement(sync);
    }

    public void before_completion() {
        // Regular syncs first then the interposed syncs
        Enumeration e = syncs.elements();
        while (e.hasMoreElements()) {
            Synchronization sync = (Synchronization) e.nextElement();
            try {
                sync.beforeCompletion();
            } catch (RuntimeException rex) {
                try {
                    state.setRollbackOnly();
                } catch (Exception ex1) {
		    _logger.log(Level.WARNING,
		        "jts.unexpected_error_occurred_in_after_completion",ex1);
                }
		_logger.log(Level.WARNING,
		    "jts.unexpected_error_occurred_in_after_completion",rex);
                throw rex;
            } catch (Exception ex) {
				_logger.log(Level.WARNING,
						"jts.unexpected_error_occurred_in_after_completion",ex);
            }
        }
        Enumeration e1 = interposedSyncs.elements();
        while (e1.hasMoreElements()) {
            Synchronization sync = (Synchronization) e1.nextElement();
            try {
                sync.beforeCompletion();
            } catch (RuntimeException rex) {
                try {
                    state.setRollbackOnly();
                } catch (Exception ex1) {
		    _logger.log(Level.WARNING,
		        "jts.unexpected_error_occurred_in_after_completion",ex1);
                }
		_logger.log(Level.WARNING,
		    "jts.unexpected_error_occurred_in_after_completion",rex);
                throw rex;
            } catch (Exception ex) {
				_logger.log(Level.WARNING,
						"jts.unexpected_error_occurred_in_after_completion",ex);
            }
        }
        state.beforeCompletion();
    }

    public void after_completion(Status status) {
        try {
            int result = TransactionManagerImpl.mapStatus(status);
            // Interposed Syncs First and then the regular syncs
            Enumeration e1 = interposedSyncs.elements();
            while (e1.hasMoreElements()) {
                Synchronization sync = (Synchronization) e1.nextElement();
                try {
                    sync.afterCompletion(result);
                } catch (Exception ex) {
					_logger.log(Level.WARNING,
							"jts.unexpected_error_occurred_in_after_completion",ex);
                }
            }
            Enumeration e = syncs.elements();
            while (e.hasMoreElements()) {
                Synchronization sync = (Synchronization) e.nextElement();
                try {
                    sync.afterCompletion(result);
                } catch (Exception ex) {
					_logger.log(Level.WARNING,
							"jts.unexpected_error_occurred_in_after_completion",ex);
                }
            }
        } finally {
            try {
                // deactivate object
                if (corbaRef != null) {
                    if (poa == null) {
                        poa = Configuration.getPOA("transient"/*#Frozen*/);
                    }
                    poa.deactivate_object(poa.reference_to_id(corbaRef));
                }
            } catch (Exception ex) {
				_logger.log(Level.WARNING,
						"jts.unexpected_error_occurred_in_after_completion",ex);
            }
        }
    }

    public org.omg.CosTransactions.Synchronization getCORBAReference() {
        if (poa == null) {
            poa = Configuration.getPOA("transient"/*#Frozen*/);
        }

        if (corbaRef == null) {
            try {
                poa.activate_object(this);
                corbaRef = SynchronizationHelper.narrow(
                            poa.servant_to_reference(this));
                //corbaRef = (org.omg.CosTransactions.Synchronization) this;
            } catch (Exception ex) {
				_logger.log(Level.SEVERE,
						"jts.unexpected_error_in_getcorbareference",ex);
            }
        }

        return corbaRef;
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
