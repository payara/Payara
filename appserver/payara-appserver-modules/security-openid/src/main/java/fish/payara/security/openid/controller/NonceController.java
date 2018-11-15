/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.openid.controller;

import com.nimbusds.jose.util.Base64URL;
import static fish.payara.security.openid.OpenIdUtil.DEFAULT_HASH_ALGORITHM;
import static fish.payara.security.openid.OpenIdUtil.not;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import fish.payara.security.openid.domain.OpenIdNonce;
import static fish.payara.security.openid.http.HttpStorageController.getInstance;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static java.util.Objects.requireNonNull;
import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import org.glassfish.common.util.StringHelper;

/**
 * Controller to manage nonce state and create the nonce hash.
 *
 * @author Gaurav Gupta
 */
@ApplicationScoped
public class NonceController {

    private static final String NONCE_KEY = "oidc.nonce";

    public void store(OpenIdNonce nonce, OpenIdConfiguration configuration, HttpMessageContext context) {
        if (configuration.isUseNonce()) {
            getInstance(configuration, context)
                    .store(NONCE_KEY, nonce.getValue(), null);
        }
    }

    public OpenIdNonce get(OpenIdConfiguration configuration, HttpMessageContext context) {

        return getInstance(configuration, context)
                .getAsString(NONCE_KEY)
                .filter(not(StringHelper::isEmpty))
                .map(OpenIdNonce::new)
                .orElse(null);
    }

    public void remove(OpenIdConfiguration configuration, HttpMessageContext context) {

        getInstance(configuration, context)
                .remove(NONCE_KEY);
    }

    public String getNonceHash(OpenIdNonce nonce) {
        requireNonNull(nonce, "OpenId nonce value must not be null");

        String nonceHash;
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM);
            md.update(nonce.getValue().getBytes(US_ASCII));
            byte[] hash = md.digest();
            nonceHash = Base64URL.encode(hash).toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No MessageDigest instance found with the specified algorithm for nonce hash", ex);
        }
        return nonceHash;
    }
}
