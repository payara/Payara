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
package fish.payara.security.openid.domain;

import java.io.Serializable;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import static java.util.Objects.isNull;
import javax.enterprise.context.SessionScoped;
import org.glassfish.common.util.StringHelper;
import static org.glassfish.common.util.StringHelper.isEmpty;

/**
 * Creates a random nonce as a character sequence of the specified byte length
 * and base64 url encoded.
 *
 * @author Gaurav Gupta
 */
@SessionScoped
public class OpenIdNonce implements Serializable {

    /**
     * The default byte length of randomly generated nonce.
     */
    public static final int DEFAULT_BYTE_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String value;

    public OpenIdNonce() {
        this(DEFAULT_BYTE_LENGTH);
    }

    /**
     * Creates a new nonce with the given nonce value.
     *
     * @param value The nonce value. Must not be {@code null} or empty.
     */
    public OpenIdNonce(String value) {
        if (StringHelper.isEmpty(value)) {
            throw new IllegalArgumentException("The nonce value can't be null or empty");
        }
        this.value = value;
    }

    /**
     * @param byteLength The byte length of the randomly generated value.
     */
    public OpenIdNonce(int byteLength) {
        if (byteLength < 1) {
            throw new IllegalArgumentException("The byte length value must be greater than one");
        }
        byte[] array = new byte[byteLength];
        SECURE_RANDOM.nextBytes(array);
        value = new String(Base64.getUrlEncoder().withoutPadding().encode(array), UTF_8);
    }

    /**
     *
     * @return The generated random nonce.
     */
    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (isNull(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OpenIdNonce other = (OpenIdNonce) obj;
        return Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return getValue();
    }

    /**
     * Convert String nonce value to Nonce object
     *
     * @param value The nonce value. Must not be {@code null} or empty.
     * @return The Nonce instance
     */
    public static OpenIdNonce valueOf(String value) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException("The nonce value can't be null or empty");
        }
        return new OpenIdNonce(value);
    }
}
