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

package com.sun.jts.pi;

import java.lang.Object;
import java.lang.reflect.Method;

import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.CodecPackage.*;

import com.sun.corba.ee.spi.presentation.rmi.StubAdapter;
import org.omg.CORBA.portable.InputStream;
import org.omg.PortableInterceptor.Current;

import org.omg.CORBA.*;
import org.omg.CosTransactions.*;
import org.omg.PortableInterceptor.*;

import org.omg.CosTSPortability.Sender;
import org.omg.CosTSPortability.Receiver;

import org.omg.CosTSInteroperation.TAG_OTS_POLICY;

import com.sun.jts.CosTransactions.CurrentTransaction;
import com.sun.corba.ee.impl.txpoa.TSIdentificationImpl;
import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.ior.ObjectKeyTemplate;
import com.sun.corba.ee.spi.ior.ObjectAdapterId;
import com.sun.corba.ee.spi.ior.iiop.IIOPProfile;
import com.sun.corba.ee.spi.orb.ORBVersionFactory;
import com.sun.corba.ee.spi.oa.rfm.ReferenceFactoryManager;
import com.sun.corba.ee.spi.misc.ORBConstants;
 
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;

/**
 * This is the implementation of the JTS PI-based client/server interceptor.
 * This will be called during request/reply invocation path.
 *
 * @author Ram Jeyaraman 11/11/2000, $Author: sankara $
 * @version 1.0, $Revision: 1.7 $ on $Date: 2007/04/02 09:07:59 $
 */
public class InterceptorImpl extends org.omg.CORBA.LocalObject
        implements ClientRequestInterceptor, ServerRequestInterceptor {

    // class attributes

    private static final String name = "com.sun.jts.pi.Interceptor";
    private static final int TransactionServiceId = 0;
    private static final SystemException SYS_EXC =
        new SystemException("", 0, CompletionStatus.COMPLETED_MAYBE) {};

    public static final int NO_REPLY_SLOT = 0;
    public static final int NULL_CTX_SLOT = 1;

    public static final Object PROPER_CTX = new Object();
    public static final Object NULL_CTX = new Object();

    public static final Object REPLY = new Object();
    public static final Object NO_REPLY = new Object();

    public static final String CLIENT_POLICY_CHECKING =
        "com.sun.jts.pi.CLIENT_POLICY_CHECKING";
    public static final String INTEROP_MODE = "com.sun.jts.pi.INTEROP_MODE";
    // The ReferenceFactoryManager from the orb.
    private static ReferenceFactoryManager rfm = null;
	
	/*
		Logger to log transaction messages
	*/  
	 static Logger _logger = LogDomains.getLogger(InterceptorImpl.class, LogDomains.TRANSACTION_LOGGER);

    public static final ThreadLocal otsThreadLocal =
        new ThreadLocal() {
            protected java.lang.Object initialValue() {
                Object[] threadLocalState = new Object[2];
                // IASRI 4698847 START
                //threadLocalState[NO_REPLY_SLOT] = new Stack();
                //threadLocalState[NULL_CTX_SLOT] = new Stack();
                threadLocalState[NO_REPLY_SLOT] = new ArrayListStack();
                threadLocalState[NULL_CTX_SLOT] = new ArrayListStack();
                // IASRI 4698847 END
                return threadLocalState;
            }
        };

    private static PropagationContext nullContext, dummyContext = null;

    // static initializer block

    static {
	    /**
        Any any = ORB.init().create_any();
        any.insert_boolean(false);
        nullContext = new PropagationContext(
            0,
            new TransIdentity(null, null, new otid_t(0, 0, new byte[0])),
            new TransIdentity[0],
            any);

        any.insert_boolean(true);
        dummyContext = new PropagationContext(
            -1,
            new TransIdentity(null, null, new otid_t(-1, 0, new byte[0])),
            new TransIdentity[0],
            any);
	    ***/
    }

    private static ORB txOrb = null;

    // instance attributes

    Current pic = null;
    Codec codec = null;
    int[] slotIds = null;
    TSIdentification tsi = null;
    TSIdentificationImpl tsiImpl = null;
    Sender sender = null;
    Receiver receiver = null;
    private boolean checkPolicy = true;
    private boolean interopMode = true;

    // constructor

    public InterceptorImpl(Current pic, Codec codec, int[] slotIds,
            TSIdentification tsi) {
        this.pic = pic;
        this.codec = codec;
        this.slotIds = slotIds;
        this.tsi = tsi;
        this.tsiImpl = (TSIdentificationImpl) tsi;
        if (this.tsiImpl != null) {
            this.sender = this.tsiImpl.getSender();
            this.receiver = this.tsiImpl.getReceiver();
        }

        // check if client side checking is disabled. This allows client side
        // policy checking to be disabled (for testing purposes).
        String prop = System.getProperty(CLIENT_POLICY_CHECKING, "true");
        this.checkPolicy = prop.equals("true");

        // get the transaction interoperability mode.
        prop = System.getProperty(INTEROP_MODE, "true");
        this.interopMode = prop.equals("true");

        if (_logger.isLoggable(Level.FINE))
            _logger.log(Level.FINE, "Transaction INTEROP Mode: " + this.interopMode);

    }

    // proprietary hook for the J2EE RI. This is currently required since the
    // RI does JTS initialization after the ORB.init() call.

    public void setTSIdentification(TSIdentification tsi) {

        if (tsi == null) {
            return;
        }

        this.tsi = tsi;
        this.tsiImpl = (TSIdentificationImpl) tsi;
        this.sender = this.tsiImpl.getSender();
        this.receiver = this.tsiImpl.getReceiver();
    }

    // implementation of the Interceptor interface.

    public String name() {
	return InterceptorImpl.name;
    }

    public void destroy() {}

    // implementation of the ClientInterceptor interface.

   public void send_request(ClientRequestInfo ri) throws ForwardRequest {

        // do IOR policy checking.

        TaggedComponent otsComp = null;
        try {
            otsComp = ri.get_effective_component(TAG_OTS_POLICY.value);
        } catch (BAD_PARAM e) {
            // ignore
        }

        short otsPolicyValue = -1;

        if (otsComp == null) {
            // in the case of J2EE RI, all published IORs must have an
            // associated OTS policy component. The only exception being the
            // location forwarded IORs returned by ORBD. Until a time, the ORBD
            // is capable of transcribing the target POA policies into the
            // location forwarded IOR, treat the absence of an OTS policy
            // component as being equivalent to ADAPTS. Once the ORBD is
            // able to support OTS policy components, the absence of an OTS
            // policy component must be treated as FORBIDS.
            otsPolicyValue = OTSPolicyImpl._ADAPTS.value();
        } else {
            // TypeCode typeCode = ORB.init().get_primitive_tc(TCKind.tk_short);
            TypeCode typeCode = txOrb.get_primitive_tc(TCKind.tk_short);
            Any any = null;
            try {
                any = this.codec.decode_value(otsComp.component_data, typeCode);
            } catch (TypeMismatch e) {
                throw new INTERNAL();
            } catch (FormatMismatch e) {
                throw new INTERNAL();
            }
            otsPolicyValue = OTSPolicyValueHelper.extract(any);
        }

        // TransactionService is not available.

        if (this.tsiImpl == null || this.sender == null) {
            if (otsPolicyValue == REQUIRES.value && this.checkPolicy) {
                throw new TRANSACTION_UNAVAILABLE();
            }
            return;
        }

        // TransactionService is available.

        /*
        // Call JTS proprietary interceptor to see if there is a current tx.
        PropagationContextHolder hctx = new PropagationContextHolder();
        sender.sending_request(ri.request_id(), hctx);

        if (hctx.value == null) { // no tx context
            if (otsPolicyValue == REQUIRES.value && this.checkPolicy) {
                throw new TRANSACTION_REQUIRED();
            }
            return;
        }
        */

        // Check to see if there is a current transaction.
        boolean isTxAssociated = CurrentTransaction.isTxAssociated();
        if (!isTxAssociated) { // no tx context
            if (otsPolicyValue == REQUIRES.value && this.checkPolicy) {
                throw new TRANSACTION_REQUIRED();
            }
            return;
        }

        if (_logger.isLoggable(Level.FINE)) {
			_logger.log(Level.FINE," sending_request["+ ri.request_id() + 
					"] : " + ri.operation() + ", ThreadName : " +
                Thread.currentThread().toString());
        }

        // a current tx is available. Create service context.

        if (otsPolicyValue == FORBIDS.value && this.checkPolicy) {
            throw new INVALID_TRANSACTION();
        }

        PropagationContextHolder hctx = new PropagationContextHolder();

        // if target object is co-located, no need to send tx context.
        // This optimization uses a dummy context to flag the local case, so
        // that the server receive point shall ignore the context (after doing
        // appropriate policy checking). The net gain is that the activation of
        // coordinator object is avoided.
        // Note, this currently has issues with respect to checked behavior.
        // Currently, checked behaviour is disabled and shall be reinstated
        // once OTS RTF redrafts the OTS spec based on PI. An issue needs to be
        // filed.
        org.omg.CORBA.Object target = ri.effective_target();
        if ( StubAdapter.isStub(target) && StubAdapter.isLocal(target) ) { 
            // target is local
            // load a dummy context and discard the current tx context.
            hctx.value = dummyContext;
        } else if (this.interopMode == false) { // target is remote
            // load a null context and discard the current tx context.
            hctx.value = nullContext;
        } else {
		    /*
	    IOR ior = ((com.sun.corba.ee.spi.orb.ORB)txOrb).getIOR( target, false ) ;
	    IIOPProfile prof = ior.getProfile() ;
	    ObjectKeyTemplate oktemp = prof.getObjectKeyTemplate() ;
	    if (oktemp.getORBVersion().equals(ORBVersionFactory.getFOREIGN()))
	    {
                hctx.value = nullContext;
	    } else {
	        ObjectAdapterId oaid = oktemp.getObjectAdapterId() ;
	        String[] adapterName = oaid.getAdapterName() ;
	        boolean isEjbCall = isEjbAdapterName(adapterName);
	        if (!isEjbCall) {
                    hctx.value = nullContext;
	        } else {
                    // Call JTS proprietary interceptor to get current tx.
                    sender.sending_request(ri.request_id(), hctx);
	        } */
                sender.sending_request(ri.request_id(), hctx);
	    // }
        }

        // add service context.

        //Any any = ORB.init().create_any();
        Any any = txOrb.create_any();
        PropagationContextHelper.insert(any, hctx.value);
        byte[] ctxData = null;
        try {
            ctxData = this.codec.encode_value(any);
        } catch (InvalidTypeForEncoding e) {
            throw new INTERNAL();
        }

        ServiceContext svc = new ServiceContext(TransactionServiceId, ctxData);
        ri.add_request_service_context(svc, false);
    }

    public void send_poll(ClientRequestInfo ri) {
        // do nothing.
    }

    public void receive_reply(ClientRequestInfo ri) {

        // check if a tx svc context was received.

        ServiceContext svc = null;
        try {
            svc = ri.get_reply_service_context(TransactionServiceId);
        } catch (BAD_PARAM e) {
            return; // do nothing (no tx service context in reply).
            // REVISIT If a valid tx context was sent, and none was received
            // back, then the checked transaction behaviour will cause the
            // transaction to fail.
        }

	if (svc == null) {
	    return;
	}
        // a tx svc context is available.

        // check if TransactionService is available.
        if (this.tsiImpl == null || this.sender == null) {
            throw new TRANSACTION_ROLLEDBACK(0, CompletionStatus.COMPLETED_YES);
        }

        if (_logger.isLoggable(Level.FINE)) {
			_logger.log(Level.FINE,"   received_reply[" + ri.request_id() + "] : " +
	                ri.operation() + ", ThreadName : " +
					Thread.currentThread().toString());
        }

        // read the propagation context

        Any any = null;
        try {
            TypeCode typeCode = PropagationContextHelper.type();
            any = this.codec.decode_value(svc.context_data, typeCode);
        } catch (TypeMismatch e) {
            throw new INTERNAL(0, CompletionStatus.COMPLETED_YES);
        } catch (FormatMismatch e) {
            throw new INTERNAL(0, CompletionStatus.COMPLETED_YES);
        }
        PropagationContext ctx = PropagationContextHelper.extract(any);

        // Set up the Environment instance with exception information.
	// The exception can be a SystemException or an UnknownUserException.

	Environment env = null;
        if (txOrb != null) {
            env = txOrb.create_environment();
        } else {
            // This shouldn't happen, but we'll be cautious
            env = ORB.init().create_environment();
        }

	env.exception(null);

        // call the OTS proprietary hook.

        try {
	    sender.received_reply(ri.request_id(), ctx, env);
	} catch (org.omg.CORBA.WrongTransaction ex) {
            throw new INVALID_TRANSACTION(0, CompletionStatus.COMPLETED_YES);
	}
    }

    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {

        // check if a tx svc context was received.

        ServiceContext svc = null;
        try {
            svc = ri.get_reply_service_context(TransactionServiceId);
        } catch (BAD_PARAM e) {
            return; // do nothing (no tx service context in reply).
            // REVISIT Exception replies may not carry a tx context back,
            // as a result checked transaction behaviour will cause the
            // transaction to fail.
        }
	catch(Exception e){
		return;
	}

	if (svc == null) {
	    return;
	}

        // a tx svc context is available.

        // Set up the Environment instance with exception information.
	// The exception can be a SystemException or an UnknownUserException.
	Environment env = null;
        if (txOrb != null) {
            env = txOrb.create_environment();
        } else {
            // This shouldn't happen, but we'll be cautious
            env = ORB.init().create_environment();
        }

        SystemException exception = null;
        Any any = ri.received_exception();
        InputStream strm = any.create_input_stream();
        String repId = ri.received_exception_id();
        strm.read_string(); // read repId
        int minorCode = strm.read_long(); // read minorCode
        CompletionStatus completionStatus = // read completionStatus
            CompletionStatus.from_int(strm.read_long());
        if (repId.indexOf("UNKNOWN") != -1) { // user exception ?
	    if (minorCode == 1) { // read minorCode
                // user exception
	    } else { // system exception
                exception = SYS_EXC;
	    }
        } else { // system exception
            exception = SYS_EXC;
        }
	env.exception(exception);

        // check if TransactionService is available.
        if (this.tsiImpl == null || this.sender == null) {
            throw new TRANSACTION_ROLLEDBACK(0, completionStatus);
        }

        // read the propagation context

        try {
            TypeCode typeCode = PropagationContextHelper.type();
            any = this.codec.decode_value(svc.context_data, typeCode);
        } catch (TypeMismatch e) {
            throw new INTERNAL(0, completionStatus);
        } catch (FormatMismatch e) {
            throw new INTERNAL(0, completionStatus);
        }
        PropagationContext ctx = PropagationContextHelper.extract(any);

        // call the OTS proprietary hook.

        try {
	    sender.received_reply(ri.request_id(), ctx, env);
	} catch (org.omg.CORBA.WrongTransaction ex) {
            throw new INVALID_TRANSACTION(0, completionStatus);
	}
    }

    public void receive_other(ClientRequestInfo ri) throws ForwardRequest {

        // check if a tx svc context was received.

        ServiceContext svc = null;
        try {
            svc = ri.get_reply_service_context(TransactionServiceId);
        } catch (BAD_PARAM e) {
            return; // do nothing (no tx service context in reply).
            // REVISIT If a valid tx context was sent, and none was received
            // back, then the checked transaction behaviour will cause the
            // transaction to fail.
        }

	if (svc == null) {
	    return;
	}
        // a tx svc context is available.

        // check if TransactionService is available.
        if (this.tsiImpl == null || this.sender == null) {
            throw new TRANSACTION_ROLLEDBACK(0, CompletionStatus.COMPLETED_NO);
        }

        // read the propagation context

        Any any = null;
        try {
            TypeCode typeCode = PropagationContextHelper.type();
            any = this.codec.decode_value(svc.context_data, typeCode);
        } catch (TypeMismatch e) {
            throw new INTERNAL(0, CompletionStatus.COMPLETED_NO);
        } catch (FormatMismatch e) {
            throw new INTERNAL(0, CompletionStatus.COMPLETED_NO);
        }
        PropagationContext ctx = PropagationContextHelper.extract(any);

        // Set up the Environment instance with exception information.
	// The exception can be a SystemException or an UnknownUserException.

	Environment env = null;
        if (txOrb != null) {
            env = txOrb.create_environment();
        } else {
            // This shouldn't happen, but we'll be cautious
            env = ORB.init().create_environment();
        }

	env.exception(null);

        // call the OTS proprietary hook.

        try {
	    sender.received_reply(ri.request_id(), ctx, env);
	} catch (org.omg.CORBA.WrongTransaction ex) {
            throw new INVALID_TRANSACTION(0, CompletionStatus.COMPLETED_NO);
	}
    }

    // implementation of the ServerInterceptor interface.

    public void receive_request_service_contexts(ServerRequestInfo ri)
            throws ForwardRequest {

        // since this could be called on a seperate thread, we need to
        // transfer the svc context to the request PICurrent slots.
        // But for now, since we know that this is called by the same thread
        // as the target, we do not do it. But we should at some point.
        // do policy checking.

        OTSPolicy otsPolicy = null;
        try {
            otsPolicy = (OTSPolicy) ri.get_server_policy(OTS_POLICY_TYPE.value);
        } catch (INV_POLICY e) {
            // ignore. This will be treated as FORBIDS.
        }

        short otsPolicyValue = -1;

        if (otsPolicy == null) {
            // Once J2EE RI moves to POA based policy mechanism, default of
            // FORBIDS shall be used. Until then, we will use ADAPTS.
            //otsPolicyValue = OTSPolicyImpl._FORBIDS.value();
            otsPolicyValue = OTSPolicyImpl._ADAPTS.value();
        } else {
            otsPolicyValue = otsPolicy.value();
        }

        // get the tx contxt, if one was received.

        ServiceContext svc = null;
        try {
            svc = ri.get_request_service_context(TransactionServiceId);
        } catch (BAD_PARAM e) {
            // ignore, svc == null will be handled later.
        }

        // set threadLocal slot to indicate whether tx svc ctxt
        // was received or not. (svc == null) ==> NO_REPLY is true.

        if (svc == null) {
            setThreadLocalData(NO_REPLY_SLOT, NO_REPLY);
        } else {
            setThreadLocalData(NO_REPLY_SLOT, REPLY);
        }

        // initially set the thread local slot to indicate proper ctxt.
        // if the null ctx is received, then it will be set later in the method.
        setThreadLocalData(NULL_CTX_SLOT, PROPER_CTX);

        try {

            // TransactionService is not available.

            if (this.tsiImpl == null || this.receiver == null) {
                if (svc != null || otsPolicyValue == REQUIRES.value) {
                    throw new TRANSACTION_UNAVAILABLE();
                }
                return;
            }

            // TransactionService is available.

            // no tx context was received.
            if (svc == null) {
                if (otsPolicyValue == REQUIRES.value) {
                    throw new TRANSACTION_REQUIRED();
                }
                return;
            }

            // a tx ctx was received.

            // check policy
            if (otsPolicyValue == FORBIDS.value) {
                throw new INVALID_TRANSACTION();
            }

            if (_logger.isLoggable(Level.FINE)) {
				_logger.log(Level.FINE," received_request[" +
                    ri.request_id() + "] : " + ri.operation() +
                    ", ThreadName : " + Thread.currentThread().toString());
            }

            // Create service context.

            // sanity check.
            if (svc.context_id != TransactionServiceId) {
                throw new INVALID_TRANSACTION();
            }

            Any any = null;
            try {
                TypeCode typeCode = PropagationContextHelper.type();
                any = this.codec.decode_value(svc.context_data, typeCode);
            } catch (TypeMismatch e) {
                throw new INTERNAL();
            } catch (FormatMismatch e) {
                throw new INTERNAL();
            }
            PropagationContext ctx = PropagationContextHelper.extract(any);

            // check if a 'dummyContext' is present (local optimization).
            // If so, return.

            if (isDummyContext(ctx)) {
                // do nothing, since it is the same client thread which already
                // has the tx context association.
                // NOTE There is a chance that the 'nullContext' could be mistaken
                // to be a 'dummyContext', which may cause a valid 'nullContext'
                // to be ignored (!). But let's hope there won't be a collision :)

                // no need to send a reply ctx
                getThreadLocalData(NO_REPLY_SLOT); // pop item
                setThreadLocalData(NO_REPLY_SLOT, NO_REPLY); // push item
                return;
            }

            // check if a 'nullContext' was received,
            // and set the threadlocal data appropriately.

            if (isNullContext(ctx)) {
                // indicate a null context
                getThreadLocalData(NULL_CTX_SLOT); // pop item
                setThreadLocalData(NULL_CTX_SLOT, NULL_CTX); // push item
                // no need to send a reply ctx
                getThreadLocalData(NO_REPLY_SLOT); // pop item
                setThreadLocalData(NO_REPLY_SLOT, NO_REPLY); // push item
                return;
            } else if (this.interopMode == false) {
                getThreadLocalData(NULL_CTX_SLOT); // pop item
                setThreadLocalData(NULL_CTX_SLOT, NULL_CTX); // push item
            }

            // call the proprietary hook
            receiver.received_request(ri.request_id(), ctx);

        } catch (RuntimeException r) {
            // The server send point will not be called if the server receive
            // point raises an exception. So, do the cleanup.
            // ie., restore thread local data
            getThreadLocalData(NO_REPLY_SLOT);
            getThreadLocalData(NULL_CTX_SLOT);
            throw r;
        }
    }

    public void receive_request(ServerRequestInfo ri)
            throws ForwardRequest {
        // do nothing.
    }

    private void processServerSendPoint(
            ServerRequestInfo ri, CompletionStatus completionStatus) {

        // clear the null ctx indicator
        getThreadLocalData(NULL_CTX_SLOT);

        // see if a reply ctx needs to be sent.

        Object no_reply = getThreadLocalData(NO_REPLY_SLOT);

        if (no_reply == NO_REPLY) {
            return;
        }

        // TransactionService is not available.

        if (this.tsiImpl == null || this.receiver == null) {
            if (no_reply == REPLY) {
                // would the TransactionService go down during request
                // processing ? Maybe.
                throw new TRANSACTION_ROLLEDBACK(0, completionStatus);
            }
            return;
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"   sending_reply["+ ri.request_id() + "] : " +
                ri.operation() + ", ThreadName : " +
                Thread.currentThread().toString());
        }

        // call the proprietary OTS interceptor.

        PropagationContextHolder ctxh = new PropagationContextHolder();
        receiver.sending_reply(ri.request_id(), ctxh);

        if (ctxh.value == null) {
            // no tx context available. This should not happen since a tx ctx
            // was received.
            throw new TRANSACTION_ROLLEDBACK(0, completionStatus);
        }

        // create the service context and set it in the reply.

        //Any any = ORB.init().create_any();
        Any any = txOrb.create_any();
        PropagationContextHelper.insert(any, ctxh.value);
        byte[] ctxData = null;
        try {
            ctxData = this.codec.encode_value(any);
        } catch (InvalidTypeForEncoding e) {
            throw new INTERNAL(0, completionStatus);
        }
        ServiceContext svc = new ServiceContext(TransactionServiceId, ctxData);
        ri.add_reply_service_context(svc, false);
    }

    public void send_reply(ServerRequestInfo ri) {
        processServerSendPoint(ri, CompletionStatus.COMPLETED_YES);
    }

    public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
        Any any = ri.sending_exception();
        InputStream strm = any.create_input_stream();
        strm.read_string(); // repId
        strm.read_long(); // minorCode
        CompletionStatus completionStatus =
            CompletionStatus.from_int(strm.read_long());

        processServerSendPoint(ri, completionStatus);
    }

    public void send_other(ServerRequestInfo ri) throws ForwardRequest {
        processServerSendPoint(ri, CompletionStatus.COMPLETED_NO);
    }

    // helper static methods.

    public static boolean isTxCtxtNull() {
        Object[] threadLocalState = (Object[]) otsThreadLocal.get();
        // IASRI 4698847 START
        //Stack stack = (Stack) threadLocalState[NULL_CTX_SLOT];
        /*
        if (stack.empty()) {
            return true;
        }
        */
        // return ((Integer) stack.peek() == NULL_CTX); 
        ArrayListStack stack = (ArrayListStack) threadLocalState[NULL_CTX_SLOT];
        return (stack.peek() == NULL_CTX);
        // IASRI 4698847 END
    }

    public static boolean isNullContext(PropagationContext ctx) {
        return (ctx.current.coord == null && ctx.current.term == null);
    }

    public static boolean isDummyContext(PropagationContext ctx) {
        boolean proceed = false;
        try {
            proceed =
                (ctx.implementation_specific_data.extract_boolean() == true);
        } catch (BAD_OPERATION e) {
            return false;
        }
        return (proceed && isNullContext(ctx) && ctx.timeout == -1);
    }

    public static void setThreadLocalData(int slot, Object data) {
        Object[] threadLocalState = (Object[]) otsThreadLocal.get();
        // IASRI 4698847 START
        //((Stack) threadLocalState[slot]).push(data); 
        ((ArrayListStack) threadLocalState[slot]).push(data); 
        // IASRI 4698847 END
    }

    public static Object getThreadLocalData(int slot) {
        Object[] threadLocalState = (Object[]) otsThreadLocal.get();
        // IASRI 4698847 START
        //return (Integer) ((Stack) threadLocalState[slot]).pop();
        return ((ArrayListStack) threadLocalState[slot]).pop();
        // IASRI 4698847 END
    }

    public static void setOrb(ORB orb) {
        txOrb = orb;
        Any any = txOrb.create_any();
        any.insert_boolean(false);
        nullContext = new PropagationContext(
            0,
            new TransIdentity(null, null, new otid_t(0, 0, new byte[0])),
            new TransIdentity[0],
            any);

        any.insert_boolean(true);
        dummyContext = new PropagationContext(
            -1,
            new TransIdentity(null, null, new otid_t(-1, 0, new byte[0])),
            new TransIdentity[0],
            any);
	try {
	    rfm = (ReferenceFactoryManager)orb.resolve_initial_references(
	          ORBConstants.REFERENCE_FACTORY_MANAGER ) ;
	} catch (Exception ex) {
	    _logger.log(Level.WARNING,ex.getMessage(), ex);
	}
    }

     public static boolean isEjbAdapterName( String[] adapterName ) {
         boolean result = false ;
	 if (rfm != null)
	     result = rfm.isRfmName( adapterName ) ;
         return result ;
     }

}
