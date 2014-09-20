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
import javax.security.auth.callback.CallbackHandler;

/**
 * This interface describes a module that can be configured
 * for a ClientAuthContext.  The main purpose of this module
 * is to secure requests and to validate received responses.
 *
 * <p> A module implementation must assume it may be used
 * to issue different requests as different clients.
 * It is the module implementation's responsibility to properly
 * store and restore any state as necessary.
 * A module that does not need to do so
 * may remain completely stateless.
 *
 *  <p> Modules are passed a shared state Map that can be used
 * to save state across a sequence of calls from <code>secureRequest</code>
 * to <code>validateResponse</code> to <code>disposeSubject</code>.
 * The same Map instance is guaranteed to be passed to all methods
 * in the call sequence.  Furthermore, it should be assumed that
 * each call sequence is passed its own unique shared state Map instance.
 *
 * @version %I%, %G%
 */
public interface ClientAuthModule {

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
     * Secure a request message.
     *
     * <p> Attach authentication credentials to an initial request,
     * sign/encrypt a request, or respond to a server challenge, for example.
     *
     * @param param an authentication parameter that encapsulates the
     *          client request and server response objects.
     *
     * @param subject the subject may be used by configured modules
     *		to obtain Principals and credentials necessary to
     *		secure the request, or null.  If null, the module may
     *		use a CallbackHandler to obtain any information necessary
     *		to secure the request.
     *
     * @param sharedState a Map for modules to save state across
     *		a sequence of calls from <code>secureRequest</code>
     *		to <code>validateResponse</code> to <code>disposeSubject</code>.
     *
     * @exception AuthException if the operation failed.
     */
    void secureRequest(AuthParam param,
			Subject subject,
			Map sharedState)
		throws AuthException;

    /**
     * Validate received response.
     *
     * <p> Validation may include verifying signature in response,
     * or decrypting response contents, for example.
     *
     * @param param an authentication parameter that encapsulates the
     *          client request and server response objects.
     *
     * @param subject the subject may be used by configured modules
     *		to store the Principals and credentials related
     *		to the identity validated in the response.
     *
     * @param sharedState a Map for modules to save state across
     *		a sequence of calls from <code>secureRequest</code>
     *		to <code>validateResponse</code> to <code>disposeSubject</code>.
     *
     * @exception AuthException if the operation failed.
     */
    void validateResponse(AuthParam param,
			Subject subject,
			Map sharedState)
		throws AuthException;

    /**
     * Dispose of the Subject.
     *
     * <p> Remove Principals or credentials from the Subject object
     * that were stored during <code>validateResponse</code>.
     *
     * @param subject Subject instance to be disposed.
     *
     * @param sharedState a Map for modules to save state across
     *		a sequence of calls from <code>secureRequest</code>
     *		to <code>validateResponse</code> to <code>disposeSubject</code>.
     *
     * @exception AuthException if the operation failed.
     */
    void disposeSubject(Subject subject, Map sharedState)
		throws AuthException;
}
