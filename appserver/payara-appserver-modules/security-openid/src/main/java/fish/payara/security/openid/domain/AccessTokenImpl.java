/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import fish.payara.security.openid.api.AccessToken;
import java.text.ParseException;
import static java.util.Collections.emptyMap;
import java.util.Map;
import fish.payara.security.openid.api.Scope;
import static fish.payara.security.openid.api.OpenIdConstant.EXPIRATION_IDENTIFIER;
import java.util.Date;
import static java.util.Objects.nonNull;

/**
 *
 * @author Gaurav Gupta
 */
public class AccessTokenImpl implements AccessToken {

    private final String token;

    private final AccessToken.Type type;

    private JWT tokenJWT;

    private Map<String, Object> claims;

    private final Long expiresIn;

    private final Scope scope;

    private final long createdAt;

    private OpenIdConfiguration configuration;

    public AccessTokenImpl(OpenIdConfiguration configuration, String tokenType, String token, Long expiresIn, String scopeValue) {
        this.configuration = configuration;
        this.token = token;
        try {
            this.tokenJWT = JWTParser.parse(token);
            this.claims = tokenJWT.getJWTClaimsSet().getClaims();
        } catch (ParseException ex) {
        }
        this.type = Type.valueOf(tokenType.toUpperCase());
        this.expiresIn = expiresIn;
        this.createdAt = System.currentTimeMillis();
        this.scope = Scope.parse(scopeValue);
    }

    public JWT getTokenJWT() {
        return tokenJWT;
    }

    @Override
    public boolean isExpired() {
        boolean expired = true;
        Date exp;
         if (nonNull(expiresIn)) {
            expired = System.currentTimeMillis() + configuration.getTokenMinValidity() > createdAt + (expiresIn * 1000);
        } else if(nonNull(exp = (Date) this.getClaim(EXPIRATION_IDENTIFIER))) {
            expired = System.currentTimeMillis() + configuration.getTokenMinValidity() > exp.getTime();
        } else {
            throw new IllegalStateException("Missing expiration time (exp) claim in access token");
        }
        return expired;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public Map<String, Object> getClaims() {
        if (claims == null) {
            return emptyMap();
        }
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    @Override
    public Object getClaim(String key) {
        return getClaims().get(key);
    }

    @Override
    public Long getExpirationTime() {
        return expiresIn;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    public boolean isEncrypted() {
        return tokenJWT != null && tokenJWT instanceof EncryptedJWT;
    }

    public boolean isSigned() {
        return tokenJWT != null && tokenJWT instanceof EncryptedJWT;
    }

    @Override
    public String toString() {
        return token;
    }

}
