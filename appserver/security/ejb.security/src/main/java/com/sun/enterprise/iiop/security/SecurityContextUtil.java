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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.iiop.security;

import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.orb.ORB;
import com.sun.enterprise.common.iiop.security.SecurityContext;
import com.sun.enterprise.security.CORBAObjectPermission;
import com.sun.enterprise.security.auth.WebAndEjbToJaasBridge;
import com.sun.logging.LogDomains;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.security.jacc.PolicyFactory;
import java.net.Socket;
import java.util.logging.Level;
import javax.security.auth.Subject;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.enterprise.iiop.api.ProtocolManager;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Service;

import static com.sun.corba.ee.spi.presentation.rmi.StubAdapter.isLocal;
import static com.sun.corba.ee.spi.presentation.rmi.StubAdapter.isStub;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

/**
 * This class provides has the helper methods to deal with the SecurityContext. This represents the
 * SecurityServiceImpl of V2
 * 
 * @author Nithya Subramanian
 */
@Service
@Singleton
public class SecurityContextUtil implements PostConstruct {

    public static final int STATUS_PASSED = 0;
    public static final int STATUS_FAILED = 1;
    public static final int STATUS_RETRY = 2;

    private static java.util.logging.Logger _logger = LogDomains.getLogger(SecurityContextUtil.class, LogDomains.SECURITY_LOGGER);

    private static String IS_A = "_is_a";

    @Inject
    private GlassFishORBHelper orbHelper;

    @Inject
    private SecurityMechanismSelector securityMechanismSelector;

    @Override
    public void postConstruct() {
    }

    /**
     * This is called by the CSIv2 interceptor on the client before sending the IIOP message.
     * 
     * @param the effective_target field of the PortableInterceptor ClientRequestInfo object.
     * @return a SecurityContext which is marshalled into the IIOP msg by the CSIv2 interceptor.
     */
    public SecurityContext getSecurityContext(org.omg.CORBA.Object effectiveTarget) throws InvalidMechanismException, InvalidIdentityTokenException {
        IOR ior = ((ORB) orbHelper.getORB()).getIOR(effectiveTarget, false);
        
        if (isStub(effectiveTarget) && isLocal(effectiveTarget)) {
            // XXX: Workaround for non-null connection object ri for local invocation.
            ConnectionExecutionContext.setClientThreadID(Thread.currentThread().threadId());
            return null;
        }

        try {
            return securityMechanismSelector.selectSecurityContext(ior);
        } catch (InvalidMechanismException ime) { // let this pass ahead
            _logger.log(SEVERE, "iiop.invalidmechanism_exception", ime);
            throw new InvalidMechanismException(ime.getMessage());
        } catch (InvalidIdentityTokenException iite) {
            _logger.log(SEVERE, "iiop.invalididtoken_exception", iite);
            throw new InvalidIdentityTokenException(iite.getMessage());
            // let this pass ahead
        } catch (SecurityMechanismException sme) {
            _logger.log(SEVERE, "iiop.secmechanism_exception", sme);
            throw new RuntimeException(sme.getMessage());
        }
    }

    /**
     * This is called by the CSIv2 interceptor on the client after a reply is received.
     * 
     * @param the reply status from the call. The reply status field could indicate an authentication
     * retry. The following is the mapping of PI status to the reply_status field
     * PortableInterceptor::SUCCESSFUL -> STATUS_PASSED PortableInterceptor::SYSTEM_EXCEPTION ->
     * STATUS_FAILED PortableInterceptor::USER_EXCEPTION -> STATUS_PASSED
     * PortableInterceptor::LOCATION_FORWARD -> STATUS_RETRY PortableInterceptor::TRANSPORT_RETRY ->
     * STATUS_RETRY
     * @param the effective_target field of the PI ClientRequestInfo object.
     */
    public static void receivedReply(int reply_status, org.omg.CORBA.Object effective_target) {
        if (reply_status == STATUS_FAILED) {
            _logger.log(FINE, "Failed status");
            // what kind of exception should we throw?
            throw new RuntimeException("Target did not accept security context");
        } 
        
        if (reply_status == STATUS_RETRY) {
            _logger.log(FINE, "Retry status");
        } else {
            _logger.log(FINE, "Passed status");
        }
    }

    /**
     * This is called by the CSIv2 interceptor on the server after receiving the IIOP message. If
     * authentication fails a FAILED status is returned. If a FAILED status is returned the CSIV2
     * Intercepter will marshal the MessageError service context and throw the NO_PERMISSION exception.
     * 
     * @param the SecurityContext which arrived in the IIOP message.
     * @return the status
     */
    public int setSecurityContext(SecurityContext context, byte[] objectId, String method, Socket socket) {
        _logger.log(Level.FINE, "ABOUT TO EVALUATE TRUST");

        try {
            // First check if the client sent the credentials as required by the object's CSIv2 policy.
            // EvaluateTrust will throw an exception if client did not conform to security policy.
            SecurityContext securityContext = securityMechanismSelector.evaluateTrust(context, objectId, socket);
            if (securityContext == null) {
                return STATUS_PASSED;
            }
            
            // Authenticate the client. An Exception is thrown if login fails.
            // SecurityContext is set on current thread if login succeeds.
            authenticate(securityContext.getSubject(), securityContext.getCredentialClass());

            // Authorize the client for non-EJB objects.
            // Auth for EJB objects is done in BaseContainer.preInvoke().
            if (authorizeCORBA(objectId, method)) {
                return STATUS_PASSED;
            }
            
            return STATUS_FAILED;
            
        } catch (Exception e) {
            if (!method.equals(IS_A)) {
                if (_logger.isLoggable(FINE)) {
                    _logger.log(FINE, "iiop.authenticate_exception", e.toString());
                }
                _logger.log(Level.FINE, "Authentication Exception", e);
            }
            
            return STATUS_FAILED;
        }
    }
    
    /**
     * Authenticate the user with the specified subject and credential class.
     */
    private void authenticate(Subject subject, Class<?> credentialClass) throws SecurityMechanismException {
        try {
            WebAndEjbToJaasBridge.login(subject, credentialClass);
        } catch (Exception e) {
            if (_logger.isLoggable(SEVERE)) {
                _logger.log(SEVERE, "iiop.login_exception", e.toString());
            }
            _logger.log(FINE, "Login Exception", e);
            
            throw new SecurityMechanismException("Cannot login user:" + e.getMessage());
        }
    }

    // return true if authorization succeeds, false otherwise.
    private boolean authorizeCORBA(byte[] objectId, String method) throws Exception {

        ProtocolManager protocolManager = orbHelper.getProtocolManager();

        // Check to make sure protocolManager is not null.
        // This could happen during server initialization or if this call
        // is on a callback object in the client VM.
        if (protocolManager == null) {
            return true;
        }

        // Check if target is an EJB
        if (protocolManager.getEjbDescriptor(objectId) != null) {
            return true; // an EJB object
        }

        com.sun.enterprise.security.SecurityContext securityContext = com.sun.enterprise.security.SecurityContext.getCurrent();

        // Check if policy gives principal the permissions
        boolean result = PolicyFactory.getPolicyFactory().getPolicy().implies(
                new CORBAObjectPermission("*", method),
                securityContext.getPrincipalSet());

        if (_logger.isLoggable(FINE)) {
            _logger.log(FINE, "CORBA Object permission evaluation result=" + result + " for method=" + method);
        }

        return result;
    }

    /**
     * This is called by the CSIv2 interceptor on the server before sending the reply.
     * 
     * @param the SecurityContext which arrived in the IIOP message.
     */
    public void sendingReply(SecurityContext context) {
        // NO OP
    }

    /**
     * This is called on the server to unset the security context this is introduced to prevent the
     * re-use of the thread security context on re-use of the thread.
     */
    public static void unsetSecurityContext(boolean isLocal) {
        // Logout method from LoginContext not called as we dont want to unset the 
        // appcontainer context
        if (!isLocal) {
            com.sun.enterprise.security.SecurityContext.setCurrent(null);
        }
    }

}
