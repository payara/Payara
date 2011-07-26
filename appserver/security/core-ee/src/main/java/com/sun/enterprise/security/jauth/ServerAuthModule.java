/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.callback.CallbackHandler;

/**
 * This interface describes a module that can be configured
 * for a ServerAuthContext.  The main purpose of this module
 * is to validate client requests and to secure responses back to the client.
 *
 * <p> A module implementation must assume it may be shared
 * across different requests from different clients.
 * It is the module implementation's responsibility to properly
 * store and restore any state necessary to associate new requests
 * with previous responses.  A module that does not need to do so
 * may remain completely stateless.
 *
 * <p> Modules are passed a shared state Map that can be used
 * to save state across a sequence of calls from <code>validateRequest</code>
 * to <code>secureResponse</code> to <code>disposeSubject</code>.
 * The same Map instance is guaranteed to be passed to all methods
 * in the call sequence.  Furthermore, it should be assumed that
 * each call sequence is passed its own unique shared state Map instance.
 *
 * @version %I%, %G%
 */
public interface ServerAuthModule {

    /**
     * Initialize this module with a policy to enforce,
     * a CallbackHandler, and administrative options.
     *
     * <p> Either the the request policy or the response policy (or both)
     * must be non-null.  
     *
     * @param requestPolicy the request policy this module is to enforce,
     *		which may be null.
     *
     * @param responsePolicy the response policy this module is to enforce,
     *		which may be null.
     *
     * @param handler CallbackHandler used to request information
     *		from the caller.
     *
     * @param options administrative options.
     */
    void initialize(AuthPolicy requestPolicy,
		AuthPolicy responsePolicy,
		CallbackHandler handler,
		Map options);

    /**
     * Authenticate a client request.
     *
     * <p> The AuthParam input parameter encapsulates the client request and
     * server response objects.  This ServerAuthModule validates the client
     * request object (decrypts content and verifies a signature, for example).
     *
     * @param param an authentication parameter that encapsulates the
     *          client request and server response objects.
     *
     * @param subject the subject may be used by configured modules
     *		to store and Principals and credentials validated
     *		in the request.
     *
     * @param sharedState a Map for modules to save state across
     *		a sequence of calls from <code>validateRequest</code>
     *		to <code>secureResponse</code> to <code>disposeSubject</code>.
     *
     * @exception PendingException if the operation is pending
     *		(for example, when a module issues a challenge).
     *		The module must have updated the response object
     *		in the AuthParam.
     *
     * @exception FailureException if the authentication failed.
     *		The module must have updated the response object
     *		in the AuthParam.
     *
     * @exception AuthException if the operation failed.
     */
    void validateRequest(AuthParam param,
			Subject subject,
			Map sharedState)
		throws AuthException;

    /**
     * Secure the response to the client
     * (sign and encrypt the response, for example).
     *
     * @param param an authentication parameter that encapsulates the
     *          client request and server response objects.
     *
     * @param subject the subject may be used by configured modules
     *		to obtain credentials needed to secure the response, or null.
     *		If null, the module may use a CallbackHandler to obtain
     *		the necessary information.
     *
     * @param sharedState a Map for modules to save state across
     *		a sequence of calls from <code>validateRequest</code>
     *		to <code>secureResponse</code> to <code>disposeSubject</code>.
     *
     * @exception AuthException if the operation failed.
     */
    void secureResponse(AuthParam param,
			Subject subject,
			Map sharedState)
		throws AuthException;

    /**
     * Dispose of the Subject.
     *
     * <p> Remove Principals or credentials from the Subject object
     * that were stored during <code>validateRequest</code>.
     *
     * @param subject the Subject instance to be disposed.
     *
     * @param sharedState a Map for modules to save state across
     *		a sequence of calls from <code>validateRequest</code>
     *		to <code>secureResponse</code> to <code>disposeSubject</code>.
     *
     * @exception AuthException if the operation failed.
     */
    void disposeSubject(Subject subject, Map sharedState)
		throws AuthException;
}
