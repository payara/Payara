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
// Module:      SenderReceiver.java
//
// Description: Transaction context propagation class.
//
// Product:     com.sun.jts.CosTransactions
//
// Author:      Simon Holdsworth
//
// Date:        June, 1997
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
import org.omg.CORBA.TSIdentificationPackage.*;
import org.omg.CosTransactions.*;
import org.omg.CosTSPortability.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
/**
 * The SenderRecevier class is our implemention of the OTS Sender and Receiver
 * classes.
 * <p>
 * Their method are implemented here as passthroughs to avoid dependency on the
 * CosTSPortability package in com.ibm.jts.implement. This is because
 * CosTSPortability is a deprecated interface in the OMG specification.
 *
 * @version 0.1
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
 */

//----------------------------------------------------------------------------
// CHANGE HISTORY
//
// Version By     Change Description
//   0.1   SAJH   Initial implementation.
//----------------------------------------------------------------------------

class SenderReceiver implements Sender, Receiver {

    private static SenderReceiver sendRec = new SenderReceiver();
	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(SenderReceiver.class, LogDomains.TRANSACTION_LOGGER);

    /**
     * Default constructor.
     *
     * @param
     *
     * @return
     *
     * @see
     */
    SenderReceiver() {}

    /**
     * Pass the operation through to the CurrentTransaction class.
     *
     * @param id      The request identifier.
     * @param holder  The completed context object.
     *
     * @return
     *
     * @exception TRANSACTION_ROLLEDBACK  The current transaction
     *   has been rolled back.  The message should not be sent and
     *   TRANSACTION_ROLLEDBACK should be returned to the caller.
     * @exception TRANSACTION_REQUIRED  There is no current transaction.
     *
     * @see
     */
    public void sending_request(int id, PropagationContextHolder holder)
            throws TRANSACTION_ROLLEDBACK, TRANSACTION_REQUIRED {

        if (_logger.isLoggable(Level.FINE)) {
            if (holder.value != null) {
		_logger.log(Level.FINE,"In sending_request"+
			":"+id+","+holder.value.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"In sending_request"+ ":"+id+","+holder);
            }
        }

        CurrentTransaction.sendingRequest(id, holder);

        if (_logger.isLoggable(Level.FINE)) {
            if (holder.value != null) {
		_logger.log(Level.FINE,"Out sending_request"+
			":"+id+","+holder.value.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"Out sending_request"+ ":"+id+","+holder);
            }
        }
    }

    /**
     * Pass the operation through to the CurrentTransaction class.
     *
     * @param id       The request identifier.
     * @param context  The PropagationContext from the message.
     * @param ex       The exception on the message.
     *
     * @return
     *
     * @exception WrongTransaction  The context returned on the reply is for a
     *   different transaction from the current one on the thread.
     *
     * @see
     */
    public void received_reply(int id, PropagationContext context,
            org.omg.CORBA.Environment ex)
            throws org.omg.CORBA.WrongTransaction {

        if (_logger.isLoggable(Level.FINE)) {
            if (context != null) {
		_logger.log(Level.FINE,"In received_reply"+
			":"+id+","+context.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"In received_reply"+ ":"+id+", null context");
            }
        }

        CurrentTransaction.receivedReply(id, context, ex);

        if (_logger.isLoggable(Level.FINE)) {
            if (context != null) {
		_logger.log(Level.FINE,"Out received_reply"+
			":"+id+","+context.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"Out received_reply"+ ":"+id+", null context");
            }
        }
    }

    /**
     * Pass the operation through to the CurrentTransaction class.
     *
     * @param id       The request identifier.
     * @param context  The PropagationContext from the message.
     *
     * @return
     *
     * @see
     */
    public void received_request(int id, PropagationContext context) {

        if (_logger.isLoggable(Level.FINE)) {
            if (context != null) {
		_logger.log(Level.FINE,"In received_request"+
			":"+id+","+context.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"In received_request"+ ":"+id+", null context");
            }
        }

        CurrentTransaction.receivedRequest(id, context);

        if (_logger.isLoggable(Level.FINE)) {
            if (context != null) {
		_logger.log(Level.FINE,"Out received_request"+
			":"+id+","+context.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"Out received_request"+ ":"+id+", null context");
            }
        }
    }

    /**
     * Pass the operation through to the CurrentTransaction class.
     *
     * @param id      The request identifier.
     * @param holder  The context to be returned on the reply.
     *
     * @return
     *
     * @exception INVALID_TRANSACTION  The current transaction has
     *   outstanding work on this reply, and has been marked rollback-only,
     *   or the reply is returning when a different transaction is active
     *   from the one active when the request was imported.
     * @exception TRANSACTION_ROLLEDBACK  The current transaction has
     *   already been rolled back.
     *
     * @see
     */
    public void sending_reply(int id, PropagationContextHolder holder)
        throws INVALID_TRANSACTION, TRANSACTION_ROLLEDBACK {

        if (_logger.isLoggable(Level.FINE)) {
            if (holder.value != null) {
		_logger.log(Level.FINE,"In sending_reply"+
			":"+id+","+holder.value.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"In sending_reply"+ ":"+id+","+holder);
            }
        }

        CurrentTransaction.sendingReply(id, holder);

        if (_logger.isLoggable(Level.FINE)) {
            if (holder.value != null) {
		_logger.log(Level.FINE,"Out sending_reply"+
			":"+id+","+holder.value.current.otid.formatID);
            } else {
		_logger.log(Level.FINE,"Out sending_reply"+ ":"+id+","+holder);
            }
        }
    }

    /**
     * Identifies an instance of this class to the TSIdentification object.
     *
     * @param ident  The TSIdentification object.
     *
     * @return
     *
     * @see
     */
    static void identify(TSIdentification ident) {
        try {
            ident.identify_sender(sendRec);
            ident.identify_receiver(sendRec);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"Sender/Receiver "+ sendRec +
                                   " successfully identified");
            }
        } catch(AlreadyIdentified exc) {
			_logger.log(Level.FINE,"jts.already_indetified_communication_manager");
        } catch (NotAvailable exc) {
			_logger.log(Level.WARNING,"jts.unable_to_indetify_communication_manager");
        }
    }

    private void debugMessage(String msg, int id, PropagationContext ctx) {
		//System.err.print is not removed as debug Message will no more be
		//used.
		_logger.log(Level.FINE,msg+";"+id);
        if (ctx == null) {
			_logger.log(Level.FINE,"");
        } else {
			_logger.log(Level.FINE,"," + ctx.current.otid.formatID);
        }
    }
}
