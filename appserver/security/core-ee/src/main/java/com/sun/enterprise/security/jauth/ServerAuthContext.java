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

package com.sun.enterprise.security.jauth;

/**
 * This ServerAuthContext class manages AuthModules that may be used
 * to validate client requests.  A caller typically uses this class
 * in the following manner:
 *
 * <ol>
 * <li> Retrieve an instance of this class via AuthConfig.getServerAuthContext.
 * <li> Receive initial client request and pass it to <i>validateRequest</i>.
 *      <br>
 *	Configured plug-in modules validate credentials present in request
 *	(for example, decrypt and verify a signature).
 *	If credentials valid and sufficient, return.
 *	Otherwise throw an AuthException.
 * <li> Authentication complete.
 *      <br>
 *      Perform authorization check on authenticated identity and,
 *	if successful, dispatch to requested service application.
 * <li> Service application finished.
 * <li> Invoke <i>secureResponse</i>.
 *      <br>
 *	Configured modules secure response (sign and encrypt it, for example).
 * <li> Send final response to client.
 * <li> The <i>disposeSubject</i> method may be invoked it necessary
 *	to clean up any authentication state in the Subject.
 * </ol>
 *
 * <p> An instance may reuse module instances it previous created.
 * As a result a single module instance may be used to process
 * different requests from different clients.
 * It is the module implementation's responsibility to properly
 * store and restore any state necessary to associate new requests
 * with previous responses.  A module that does not need to do so
 * may remain completely stateless.
 *
 * <p> Instances of this class have custom logic to determine
 * what modules to invoke, and in what order.  In addition,
 * this custom logic may control whether subsequent modules are invoked
 * based on the success or failure of previously invoked modules.
 *
 * <p> The caller is responsible for passing in a state Map
 * that can be used by underlying modules to save state across
 * a sequence of calls from <code>validateRequest</code>
 * to <code>secureResponse</code> to <code>disposeSubject</code>.
 * The same Map instance must be passed to all methods in the call sequence.
 * Furthermore, each call sequence should be passed its own unique
 * shared state Map instance.
 *
 * @version %I%, %G%
 * @see AuthConfig
 * @see SOAPAuthParam
 */
public interface ServerAuthContext {

    /**
     * Authenticate a client request.
     * (decrypt the message and verify a signature, for exmaple).
     *
     * <p> This method invokes configured modules to authenticate the request.
     *
     * @param param an authentication parameter that encapsulates the
     *          client request and server response objects.
     *
     * @param subject the subject may be used by configured modules
     *		to store and Principals and credentials validated
     *		in the request.
     *
     * @param sharedState a Map for modules to save state across
     *          a sequence of calls from <code>validateRequest</code>
     *          to <code>secureResponse</code> to <code>disposeSubject</code>.
     *
     * @exception PendingException if the operation is pending
     *		(for example, when a module issues a challenge).
     *		The module must have updated the response object
     *		in the AuthParam input parameter.
     *
     * @exception FailureException if the authentication failed.
     *		The module must have updated the response object
     *		in the AuthParam input parameter.
     *
     * @exception AuthException if the operation failed.
     */
    void validateRequest(AuthParam param,
			javax.security.auth.Subject subject,
			java.util.Map sharedState)
		throws AuthException;

    /**
     * Secure the response to the client
     * (sign and encrypt the response, for example).
     *
     * <p> This method invokes configured modules to secure the response.
     *
     * @param param an authentication parameter that encapsulates the
     *          client request and server response objects
     *
     * @param subject the subject may be used by configured modules
     *		to obtain credentials needed to secure the response, or null.
     *		If null, the module may use a CallbackHandler to obtain
     *		the necessary information.
     *
     * @param sharedState a Map for modules to save state across
     *          a sequence of calls from <code>validateRequest</code>
     *          to <code>secureResponse</code> to <code>disposeSubject</code>.
     *
     * @exception AuthException if the operation failed.
     */
    void secureResponse(AuthParam param,
			javax.security.auth.Subject subject,
			java.util.Map sharedState)
		throws AuthException;

    /**
     * Dispose of the Subject
     * (remove Principals or credentials from the Subject object
     * that were stored during <code>validateRequest</code>).
     *
     * <p> This method invokes configured modules to dispose the Subject.
     *
     * @param subject the subject to be disposed.
     *
     * @param sharedState a Map for modules to save state across
     *          a sequence of calls from <code>validateRequest</code>
     *          to <code>secureResponse</code> to <code>disposeSubject</code>.
     *
     * @exception AuthException if the operation failed.
     */
    void disposeSubject(javax.security.auth.Subject subject,
			java.util.Map sharedState)
		throws AuthException;

    /**
     * modules manage sessions
     * used by calling container to determine if it should delegate session 
     * management (including the mapping of requests to authentication
     * results established from previous requests) to the underlying 
     * authentication modules of the context. 
     * <p>
     * When this method returns true,
     * the container should call validate on every request, and as such
     * may depend on the invoked modules to determine when a request 
     * pertains to an existing authentication session.
     * <p> 
     * When this method returns false,
     * the container may employ is own session management functionality, and 
     * may use this functionality to recognize when an exiting request 
     * is to be interpretted in the context of an existing authentication
     * session. 
     *
     * @return true if the context should be allowed to manage sessions, and
     * false if session management (if it is to occur) must be performed by 
     * the container.
     *
     * @exception AuthException if the operation failed.
     */
    boolean managesSessions(java.util.Map sharedState)
		throws AuthException;

}
