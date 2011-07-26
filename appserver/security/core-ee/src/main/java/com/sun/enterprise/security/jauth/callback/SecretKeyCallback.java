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

package com.sun.enterprise.security.jauth.callback;

import java.math.BigInteger;
import java.security.Principal;
import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;

/**
 * Callback for private key and corresponding certificate chain.
 *
 * @version %I%, %G%
 */
public class SecretKeyCallback 
        extends javax.security.auth.message.callback.SecretKeyCallback {

    /**
     * Marker interface for private key request types.
     */
    public static interface Request extends
            javax.security.auth.message.callback.SecretKeyCallback.Request { };

    /**
     * Request type for secret keys that are identified via an alias.
     */
    public static class AliasRequest extends
            javax.security.auth.message.callback.SecretKeyCallback.AliasRequest
            implements Request {

        /**
         * Construct an AliasRequest with an alias.
         *
         * <p> The alias is used to directly identify the secret key
         * to be returned.
         *
         * <p> If the alias is null,
         * the handler of the callback relies on its own default.
         *
         * @param alias name identifier for the secret key, or null.
         */
        public AliasRequest(String alias) {
            super(alias);
        }
    }

    /**
     * Constructs this SecretKeyCallback with a secret key Request object.
     *
     * <p> The <i>request</i> object identifies the secret key
     * to be returned.
     *
     * If the alias is null, the handler of the callback
     * relies on its own default.
     *
     * @param request request object identifying the secret key, or null.
     */
    public SecretKeyCallback(Request request) {
	super(request);
    }

    /**
     * Get the Request object which identifies the secret key to be returned.
     *
     * @return the Request object which identifies the private key
     *		to be returned, or null.  If null, the handler of the callback
     *		relies on its own deafult.
     */
    public Request getRequest() {
	return (Request)super.getRequest();
    }
}
