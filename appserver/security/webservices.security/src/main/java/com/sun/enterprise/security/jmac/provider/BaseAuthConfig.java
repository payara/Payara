/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.jmac.provider; 

import com.sun.enterprise.security.jauth.*;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityDescriptor;
import com.sun.enterprise.deployment.runtime.common.ProtectionDescriptor;
import com.sun.enterprise.security.webservices.LogUtils;

import com.sun.xml.rpc.spi.runtime.StreamingHandler;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class is the container's base interface to the AuthConfig subsystem
 * to get AuthContext objects on which to invoke message layer authentication
 * providers. It is not intended to be layer or web services specific (see
 * getMechanisms method at end). 
 * The ServerAuthConfig and ClientAuthConfig classes extend this class.
 */
public class BaseAuthConfig {

    private static final Logger logger = LogUtils.getLogger();

    private Object defaultContext_;

    // holds protected msd that applies to all methods (if there is one)
    private MessageSecurityDescriptor superMSD_;
    private int superIndex_;

    private ArrayList contexts_;

    private ArrayList messageSecurityDescriptors_;

    private ArrayList contextsForOpcodes_;

    private HashMap contextsForOpNames_;

    private boolean onePolicy_;

    private final Object contextLock = new Object();

    private ExplicitNull explicitNull = new ExplicitNull();

    protected BaseAuthConfig(Object context) {

	defaultContext_ = context;
	superMSD_ = null;
	superIndex_ = -1;

	messageSecurityDescriptors_ = null;
	contexts_ = null;
	contextsForOpcodes_ = null;
	contextsForOpNames_ = null;
	
	onePolicy_ = true;

	if(logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE, "WSS: New BAC defaultContext: {0}", defaultContext_);
	}
    }

    protected BaseAuthConfig (ArrayList descriptors, ArrayList authContexts) {

	defaultContext_ = null;
	superMSD_ = null;
	superIndex_ = -1;

	messageSecurityDescriptors_ = descriptors;
	contexts_ = authContexts;
	contextsForOpcodes_ = null;
	contextsForOpNames_ = null;

	onePolicy_ = true;

	for (int i = 0; i < descriptors.size(); i++) {

	    MessageSecurityDescriptor msd = 
		(MessageSecurityDescriptor) descriptors.get(i);

	    // determine if all the different messageSecurityDesriptors have the
	    // same policy which will help us interpret the effective policy if
	    // we cannot determine the opcode of a request at runtime.

	    for (int j = 0; j < descriptors.size(); j++) {
		if (j != i && !policiesAreEqual
		    (msd,((MessageSecurityDescriptor) descriptors.get(j)))) {
		    onePolicy_ = false;
		}
	    }
	}

	for (int i = 0; defaultContext_ == null && i < descriptors.size(); i++) {

	    MessageSecurityDescriptor msd = (MessageSecurityDescriptor) descriptors.get(i);

	    AuthPolicy requestPolicy = 
		getAuthPolicy(msd.getRequestProtectionDescriptor());

	    AuthPolicy responsePolicy = 
		getAuthPolicy(msd.getResponseProtectionDescriptor());

	    boolean noProtection = (!requestPolicy.authRequired() && 
				    !responsePolicy.authRequired());

	    // if there is one policy, and it is null set the associated context as the
	    // defaultContext used for all messages.
	    if (i==0 && onePolicy_ && noProtection) {
		defaultContext_ = explicitNull;
		break;
	    }

	    ArrayList mDs = msd.getMessageDescriptors();

	    for (int j=0; mDs != null && j < mDs.size(); j++) {
		
		MessageDescriptor mD = (MessageDescriptor) mDs.get(j);
		MethodDescriptor methD = mD.getMethodDescriptor();

		// if any msd covers all methods and operations.
		if ((mD.getOperationName() == null && methD == null) ||
                        (methD != null && methD.getStyle() == 1)) {

		    if (onePolicy_) {
			// if there is only one policy make it the default.
			defaultContext_ = contexts_.get(i);
			if (defaultContext_ == null) {
			    defaultContext_ = explicitNull;
			}
			break;
		    } else if (superIndex_ == -1) {
			// if it has a noProtection policy make it the default.
			if (noProtection) {
			    defaultContext_ = explicitNull;
			} else {
			    superIndex_ = i;
			}
		    } else if (!policiesAreEqual(msd,((MessageSecurityDescriptor) 
						      descriptors.get(superIndex_)))) {
			// if there are conflicting policies that cover all methods
			// set the default policy to noProtection
			defaultContext_ = explicitNull;
			superIndex_ = -1;
			break;
		    }
		}
	    }
	}
	// if there is protected policy that applies to all methods remember the descriptor.
	// Note that the corresponding policy is not null, and thus it is not the default.
	// wherever there is a conflicting policy the effective policy will be noProtection.
	if (superIndex_ >=0) {
	    superMSD_ = (MessageSecurityDescriptor) descriptors.get(superIndex_);
	}

	if(logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE, "WSS: new BAC defaultContext_: {0} superMSD index: {1} onePolicy_: {2}", new Object[]{defaultContext_, superIndex_, onePolicy_});
	}
    }

    protected static AuthPolicy getAuthPolicy(ProtectionDescriptor pd) {
	int sourceAuthType = AuthPolicy.SOURCE_AUTH_NONE;
	boolean recipientAuth = false;
	boolean beforeContent = false;
	if (pd != null) {
	    String source = pd.getAttributeValue
		(ProtectionDescriptor.AUTH_SOURCE);
	    if (source != null) { 
		if (source.equals(AuthPolicy.SENDER)) {
		    sourceAuthType = AuthPolicy.SOURCE_AUTH_SENDER;
		} else if (source.equals(AuthPolicy.CONTENT)) {
		    sourceAuthType = AuthPolicy.SOURCE_AUTH_CONTENT;
		}
	    }
	    String recipient = pd.getAttributeValue
		(ProtectionDescriptor.AUTH_RECIPIENT);
	    if (recipient != null) { 
		recipientAuth = true;
		if (recipient.equals(AuthPolicy.BEFORE_CONTENT)) {
		    beforeContent = true;
		} else if (recipient.equals(AuthPolicy.AFTER_CONTENT)) {
		    beforeContent = false;
		}
	    }
	}
	return new AuthPolicy(sourceAuthType,recipientAuth,beforeContent);
    }

    private static boolean
	isMatchingMSD(MethodDescriptor targetMD, MessageSecurityDescriptor mSD) {
	ArrayList messageDescriptors = mSD.getMessageDescriptors();
	if (messageDescriptors.isEmpty()) {
	    // If this happens the dd is invalid.
	    // Unfortunately the deployment system does not catch such problems.
	    // This case will be treated the same as if there was an empty message
	    // element, and the deployment will be allowed to succeed.
	    return true;
	}

	for (int i=0; i<messageDescriptors.size(); i++) {
	    MessageDescriptor nextMD = 
		(MessageDescriptor) messageDescriptors.get(i);
	    MethodDescriptor mD = nextMD.getMethodDescriptor();
	    String opName = nextMD.getOperationName();

	    if (opName == null && (mD == null || mD.implies(targetMD))){
		return true;
	    }
	}

	return false;
    }

    private static boolean
	policiesAreEqual(MessageSecurityDescriptor reference,
			 MessageSecurityDescriptor other) {
	if (!getAuthPolicy
	    (reference.getRequestProtectionDescriptor()).equals
	    (getAuthPolicy(other.getRequestProtectionDescriptor())) ||

	    !getAuthPolicy
	    (reference.getResponseProtectionDescriptor()).equals
	    (getAuthPolicy(other.getResponseProtectionDescriptor()))) {
	    
	    return false;
	}
	return true;
    }

    /* When method argument is null, returns the default AC
     * if there is one, or the onePolicy shared by all methods
     * if there is one, or throws an error.
     * method is called with null argument when the method
     * cannot be determined (e.g. when the message is encrypted)
     */
    private Object getContextForMethod(Method m) {
	Object rvalue = null;
	synchronized(contextLock) {
	    if (defaultContext_ != null) { 
		rvalue = defaultContext_;
		if(logger.isLoggable(Level.FINE)){
		    logger.log(Level.FINE, "WSS: ForMethod returning default_context: {0}", rvalue);
		}
		return rvalue;
	    }
	}
	if (m != null) {
	    int match = -1;
	    MethodDescriptor targetMD = new MethodDescriptor(m);
	    for (int i = 0; i < messageSecurityDescriptors_.size(); i++) {
		if (isMatchingMSD(targetMD,(MessageSecurityDescriptor) 
				  messageSecurityDescriptors_.get(i))) {
		    if (match < 0) { 
			match = i;
		    } else if (!policiesAreEqual
			       ((MessageSecurityDescriptor) 
				messageSecurityDescriptors_.get(match),
				(MessageSecurityDescriptor)
				messageSecurityDescriptors_.get(i))) {

			// set to unprotected because of conflicting policies

			rvalue = explicitNull;
			match = -1;
			if(logger.isLoggable(Level.FINE)){
			    logger.log(Level.FINE, "WSS: ForMethod detected conflicting policies: {0}.{1}", new Object[]{match, i});
			}
			break;
		    }
		}
	    }
	    if (match >= 0) {
		rvalue = contexts_.get(match);
		if (rvalue == null) {
		    rvalue = explicitNull;
		}
		if(logger.isLoggable(Level.FINE)){
		    logger.log(Level.FINE, "WSS: ForMethod returning matched context: {0}", rvalue);
		}
	    }
	} else if (onePolicy_ && contexts_.size() > 0) {
 	    // ISSUE: since the method is undefined we will not be 
	    // able to tell if the defined policy covers this method.
 	    // We will be optimistic and try the policy, because
 	    // the server will reject the call if the method is not
 	    // covered by the policy.
 	    // If the policy is not null, there remains a problem at the 
	    // client on the response side, as it is possible that the 
	    // client will enforce the wrong policy on the response.
 	    // For this reason, messages in sun-application-client.xml 
	    // should be keyed by operation-name.

	    rvalue = contexts_.get(0);
	    if(logger.isLoggable(Level.FINE)){
		logger.log(Level.FINE, "WSS: ForMethod resorting to first context: {0}", rvalue);
	    }
 
	} else {
	    if(logger.isLoggable(Level.FINE)){
	        logger.fine("WSS: Unable to select policy for SOAP Message");
            }
	    throw new RuntimeException("Unable to select policy for Message");
	}
	return rvalue;
    }

    private Object getExplicitContextForOpCode(StreamingHandler handler, 
	int opcode) throws ClassNotFoundException, NoSuchMethodException {

	Object rvalue = null;

	synchronized(contextLock) {

	    if (contextsForOpcodes_ == null && defaultContext_ == null) {

		// one time initialization of the opcode to authContext array.

		boolean onePolicyForAll = onePolicy_;

		Method m = null;
		for (int i = 0; i == 0 || m != null; i++) {
		    if (i == 0) {
			contextsForOpcodes_ = new ArrayList();
		    }
		    if (handler != null) {
			m = handler.getMethodForOpcode(i);
		    }
		    if (m != null) {
			Object o = getContextForMethod(m);
			contextsForOpcodes_.add(o);

			// if we find a method that is not covered by a method
			// descriptor (i.e. has an implicit nullPolicy), 
			// then we switch off onePolicyForAll (note that 
			// ServerAuthConfigs with one policy being the
			// null policy, are not constructed.

			if (o == null) {
			    onePolicyForAll = false;
			}
		    }
		}
		if (onePolicyForAll && contextsForOpcodes_.size() > 0) {
		    defaultContext_ = contextsForOpcodes_.get(0);
		}
	    }
	    if (defaultContext_ != null) {
		rvalue = defaultContext_;
		if(logger.isLoggable(Level.FINE)){
		    logger.log(Level.FINE, "WSS: ForOpCode returning default_context: {0}", rvalue);
		}
	    }
	}

	if (rvalue == null) {
	    if (opcode >=0 && opcode < contextsForOpcodes_.size()) {
		rvalue = contextsForOpcodes_.get(opcode);
	    } else if (opcode < 0) {
		//we don't know the opcode, so lets try to see if
		//there is a policy that applies to all opcodes.
		rvalue = getContextForMethod(null);
	    }
	}
	return rvalue;
    }

    protected Object getContextForOpCode(StreamingHandler handler, int opcode) 
	throws ClassNotFoundException, NoSuchMethodException {
	Object rvalue = getExplicitContextForOpCode(handler,opcode);
	if (rvalue != null && rvalue instanceof ExplicitNull) {
	    rvalue = null;
	}
	return rvalue;
    }
    private static String getOpName(SOAPMessage message) {

	String rvalue = null;

	// first look for a SOAPAction header. 
	// this is what .net uses to identify the operation

	MimeHeaders headers = message.getMimeHeaders();
	if (headers != null) {
	    String[] actions = headers.getHeader("SOAPAction");
	    if (actions != null && actions.length > 0) {
		rvalue = actions[0];
		if (rvalue != null && rvalue.equals("\"\"")) {
		    rvalue = null;
		}
	    }
	} 

	// if that doesn't work then we default to trying the name
	// of the first child element of the SOAP envelope.

	if (rvalue == null) {
	    Name name = getName(message);
	    if (name != null) {
		rvalue = name.getLocalName();
	    }
	}
	
	return rvalue;
    }

    private static String getOpName(SOAPMessageContext soapMC) {

	String rvalue;

	// first look for a the property value in the context
	QName qName = (QName) soapMC.get(MessageContext.WSDL_OPERATION);
	if (qName != null) {
	    rvalue = qName.getLocalPart();
	} else {
	    rvalue = getOpName(soapMC.getMessage());
	}

	return rvalue;
    }

    private Object getContextForOpName(String operation) {

	synchronized(contextLock) {
	    if (contextsForOpNames_ == null) {

		// one time initialization of the opName to authContext array.

		contextsForOpNames_ = new HashMap();
		for (int i=0; messageSecurityDescriptors_ != null &&
			 i < messageSecurityDescriptors_.size(); i++) {
		    
		    MessageSecurityDescriptor mSD = (MessageSecurityDescriptor)
			messageSecurityDescriptors_.get(i);

		    ArrayList mDs = mSD.getMessageDescriptors();

		    for (int j=0; mDs != null && j < mDs.size(); j++) {

			MessageDescriptor mD = (MessageDescriptor) mDs.get(j);
			String opName = mD.getOperationName();

			if (opName != null) {

			    if (contextsForOpNames_.containsKey(opName)) {

				Integer k = (Integer) contextsForOpNames_.get(opName);
				if (k != null) {

				    MessageSecurityDescriptor other =
					(MessageSecurityDescriptor)
					messageSecurityDescriptors_.get(k.intValue());

				    // set to null if different policies on operation

				    if (!policiesAreEqual(mSD,other)) {
					contextsForOpNames_.put(opName,null);
				    }
				}
			    } else if (superMSD_!=null && !policiesAreEqual(mSD,superMSD_)){
				// set to null if operation policy differs from superPolicy
				contextsForOpNames_.put(opName,null);
			    } else {
				contextsForOpNames_.put(opName,Integer.valueOf(i));
			    }
			}
		    }
		}
	    }
	}

	Object rvalue = null; 
	if (operation != null) {
	    if (contextsForOpNames_.containsKey(operation)) {
		Integer k = (Integer) contextsForOpNames_.get(operation);
		if (k != null) {
		    rvalue = contexts_.get(k.intValue());
		} 
	    } else if (superIndex_ >= 0) {
                // if there is a msb that matches all methods, use the
                // associatedContext
                rvalue = contexts_.get(superIndex_);
	    }

            if (rvalue == null) {
                // else return explicitNull under the assumption
                // that methodName was known, and no match was found 
                rvalue = explicitNull;
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "WSS: ForOpName={0} context: {1}", new Object[]{operation, rvalue});
            }
	} 
	return rvalue;
    }
    
    // DO NOT CALL THIS ON THE SERVER SIDE, as it will return a null
    // context if there is no default context and there isn't a message 
    // element defined with the corresponding operation name (even though the
    // corresponding method may be protected). 
    //
    // This method is intended to be used by clients where it serves as a 
    // work-around for not being able to map the message to the method (due 
    // to lack of access to a streaming handler equivalent). 
    //
    // This method will not be called when the handler argument passed in 
    // a call to getContext or getContextForOpCode is not null. 
    // Thus, server-side calls to these methods must pass a non-null 
    // handler argument.

    private Object getContextForMessage(SOAPMessage message) {
	
	String opName = getOpName(message);

	Object rvalue = getContextForOpName(opName);
	if (rvalue == null) {

	    // opName is not mapped or msg body is encrypted, and the best 
	    // we can do is try to return a policy that applies to all 
	    // operations, if there is one.
	    
	    rvalue = getContextForMethod(null);

	}
	return rvalue;
    }
    
    protected Object getContext(StreamingHandler handler,SOAPMessage message){

	Object rvalue = null;

	synchronized(contextLock) {
	    if (defaultContext_ != null) { 
		rvalue = defaultContext_;
	    }
	}
 
	if (rvalue == null) {

	    if (handler == null) {

		// lack of handler precludes mapping to opcode, so we will
		// look for an opName based mapping.

		rvalue = getContextForMessage(message);

	    } else {

		int opCode = handler.getOpcodeForRequestMessage(message);

		if (opCode == -1) {

		    // msg body is encrypted, and the best we can do is try 
		    // to return a policy that applies to all opcodes.

		    rvalue = getContextForMethod(null);

		} else {
		    
		    try {
			rvalue = getExplicitContextForOpCode(handler,opCode);
		    
			// if unable to get context by opcode 
			// see if a context was defined for the opName.
		    
			if (rvalue == null) {

			    rvalue = getContextForMessage(message);

			}
		    } catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		    } catch (NoSuchMethodException nsme) {
			throw new RuntimeException(nsme);
		    }
		}
	    }
	}

	if (rvalue != null && rvalue instanceof ExplicitNull) {
	    rvalue = null;
	}

	if(logger.isLoggable(Level.FINE)){
	    logger.log(Level.FINE, "WSS: getContext returning: {0}", rvalue);
	}

	return rvalue;
    }

    // used by jaxws system handler delegates and handlers
    protected Object getContext(SOAPMessageContext soapMC) {

	Object rvalue = null;

	synchronized(contextLock) {
	    if (defaultContext_ != null) { 
		rvalue = defaultContext_;
	    }
	}

	if (rvalue == null) {

	    Method m = getMethod(soapMC);
	    String opName = null;

	    if (m != null) {
		rvalue = getContextForMethod(m);
	    }

	    if (rvalue == null) {
		opName = getOpName(soapMC);
		if (opName != null) {
		    rvalue = getContextForOpName(opName);
		}
	    }

	    if (rvalue == null && (m == null || opName == null)) {

		//we were unable to determine either method or
		// opName, so lets see if one policy applies to all

		rvalue = getContextForMethod(null);
	    }
	}
	
	if (rvalue != null && rvalue instanceof ExplicitNull) {
	    rvalue = null;
	}

	return rvalue;
    }

    private static Name getName(SOAPMessage message) {
	Name rvalue = null;
	SOAPPart soap = message.getSOAPPart();
	if (soap != null) {
	    try {
		SOAPEnvelope envelope = soap.getEnvelope(); 
		if (envelope != null) {
		    SOAPBody body = envelope.getBody();
		    if (body != null) {
			Iterator it = body.getChildElements();
			while (it.hasNext()) {
			    Object o = it.next();
			    if (o instanceof SOAPElement) {
				rvalue = ((SOAPElement) o).getElementName(); 
				break;
			    }
			}
		    }
		}
	    } catch (SOAPException se) {
		if(logger.isLoggable(Level.FINE)){
		    logger.log(Level.FINE,"WSS: Unable to get SOAP envelope",
			       se);
		}
	    }
	}
	
	return rvalue;
    }

    public static Method getMethod(SOAPMessageContext soapMC) {

        // It should never come here
	return null;
    }

    // each instance of AuthConfig maps to one provider 
    // configuration, either via a message-security-binding, or a default 
    // provider-config.

    // mechanisms are temporarily encapsulated here, until a method that
    // returns the list of supported mechanisms is added to 
    // jauth.ServerAuthContext and jauth.ClientAuthContext.
    public QName[] getMechanisms() {
	return mechanisms;
    }

    // WSS security header QName 
    private static QName mechanisms[] = new QName[] {
	new QName( "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security", "wsse") };

    // internal class used to differentiate not protected from policy undefined or
    // not determined.

    static class ExplicitNull {

	ExplicitNull() {
	}

        @Override
	public boolean equals(Object other) {
	    return (other != null && other instanceof ExplicitNull ? true : false);
	}

        @Override
        public int hashCode() {
            return super.hashCode();
        }
        @Override
	public String toString() {
	    return "ExplicitNull";
	}
    }
}
