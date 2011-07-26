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
import java.security.PrivateKey;
import java.security.cert.Certificate;
import javax.security.auth.callback.Callback;
import javax.security.auth.x500.X500Principal;

/**
 * Callback for private key and corresponding certificate chain.
 *
 * @version %I%, %G%
 */
public class PrivateKeyCallback 
        extends javax.security.auth.message.callback.PrivateKeyCallback {

    /**
     * Marker interface for private key request types.
     */
    public static interface Request extends
            javax.security.auth.message.callback.PrivateKeyCallback.Request { };

    /**
     * Request type for private keys that are identified via an alias.
     */
    public static class AliasRequest extends
            javax.security.auth.message.callback.PrivateKeyCallback.AliasRequest
            implements Request {

        /**
         * Construct an AliasRequest with an alias.
         *
         * <p> The alias is used to directly identify the private key
         * to be returned.  The corresponding certificate chain for the
         * private key is also returned.
         *
         * <p> If the alias is null,
         * the handler of the callback relies on its own default.
         *
         * @param alias name identifier for the private key, or null.
         */
        public AliasRequest(String alias) {
            super(alias);
        }
    }

    /**
     * Request type for private keys that are identified via a SubjectKeyID
     */
    public static class SubjectKeyIDRequest extends
            javax.security.auth.message.callback.PrivateKeyCallback.SubjectKeyIDRequest
            implements Request {

        /**
         * Construct a SubjectKeyIDRequest with an subjectKeyID.
         *
         * <p> The subjectKeyID is used to directly identify the private key
         * to be returned.  The corresponding certificate chain for the
         * private key is also returned.
         *
         * <p> If the subjectKeyID is null,
         * the handler of the callback relies on its own default.
         *
         * @param subjectKeyID identifier for the private key, or null.
         */
        public SubjectKeyIDRequest(byte[] subjectKeyID) {
            super(subjectKeyID);
        }
    }

    /**
     * Request type for private keys that are identified via an
     * issuer/serial number.
     */
    public static class IssuerSerialNumRequest extends
           javax.security.auth.message.callback.PrivateKeyCallback.IssuerSerialNumRequest
           implements Request {

        /**
         * Constructs a IssuerSerialNumRequest with an issuer/serial number.
         *
         * <p> The issuer/serial number are used to identify a
         * public key certificate.  The corresponding private key
         * is returned in the callback.  The corresponding certificate chain
         * for the private key is also returned.
         *
         * If the issuer/serialNumber parameters are null,
         * the handler of the callback relies on its own defaults.
         *
         * @param issuer the X500Principal name of the certificate issuer,
         *                or null.
         *
         * @param serialNumber the serial number of the certificate,
         *                or null.
         */
        public IssuerSerialNumRequest(X500Principal issuer,
                                        BigInteger serialNumber) {
            super(issuer, serialNumber);
        }
    }

    /**
     * Constructs this PrivateKeyCallback with a private key Request object.
     *
     * <p> The <i>request</i> object identifies the private key
     * to be returned.  The corresponding certificate chain for the
     * private key is also returned.
     *
     * <p> If the <i>request</i> object is null,
     * the handler of the callback relies on its own default.
     *
     * @param request identifier for the private key, or null.
     */
    public PrivateKeyCallback(Request request) {
        super(request);
    }

    /**
     * Get the Request object which identifies the private key to be returned.
     *
     * @return the Request object which identifies the private key
     *		to be returned, or null.  If null, the handler of the callback
     *		relies on its own default.
     */
    public Request getRequest() {
	return (Request)super.getRequest();
    }
}
