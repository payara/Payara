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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.auth.realm;

import static com.sun.enterprise.security.auth.digest.api.Constants.A1;
import static com.sun.enterprise.security.auth.digest.api.Constants.RESPONSE;
import static java.util.logging.Level.SEVERE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.sun.enterprise.security.BaseRealm;
import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import com.sun.enterprise.security.auth.digest.api.Key;
import com.sun.enterprise.security.auth.digest.api.Password;
import com.sun.enterprise.security.auth.digest.impl.DigestProcessor;

/**
 * Base class for all realms wanting to support Digest based authentication.
 *
 * @author K.Venugopal@sun.com
 */
public abstract class DigestRealmBase extends BaseRealm implements DigestRealm {

    protected boolean validate(final Password passwd, DigestAlgorithmParameter[] params) {
        try {
            return new DigestValidatorImpl().validate(passwd, params);
        } catch (NoSuchAlgorithmException ex) {
            _logger.log(SEVERE, "invalid.digest.algo", ex);
        }

        return false;
    }

    private static class DigestValidatorImpl extends DigestProcessor {

        private DigestAlgorithmParameter data;
        private DigestAlgorithmParameter clientResponse;
        private DigestAlgorithmParameter key;
        private String algorithm = "MD5";

        @Override
        protected final boolean validate(Password passwd, DigestAlgorithmParameter[] params) throws NoSuchAlgorithmException {

            for (int i = 0; i < params.length; i++) {
                DigestAlgorithmParameter dap = params[i];
                if (A1.equals(dap.getName()) && (dap instanceof Key)) {
                    key = dap;
                } else if (RESPONSE.equals(dap.getName())) {
                    clientResponse = dap;
                } else {
                    data = dap;
                }
            }
            setPassword(passwd);

            try {
                byte[] p1 = valueOf(key);
                byte[] p2 = valueOf(data);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bos.write(p1);
                bos.write(":".getBytes());
                bos.write(p2);

                byte[] derivedKey = encode(MessageDigest.getInstance(algorithm).digest(bos.toByteArray())).getBytes();
                byte[] suppliedKey = clientResponse.getValue();
                
                boolean result = true;
                
                if (derivedKey.length == suppliedKey.length) {
                    for (int i = 0; i < derivedKey.length; i++) {
                        if (!(derivedKey[i] == suppliedKey[i])) {
                            result = false;
                            break;
                        }
                    }
                } else {
                    result = false;
                }
                
                return result;
            } catch (IOException ex) {
                _logger.log(SEVERE, "digest.error", new Object[] { ex.getMessage() });
            }

            return false;
        }
    }
}
