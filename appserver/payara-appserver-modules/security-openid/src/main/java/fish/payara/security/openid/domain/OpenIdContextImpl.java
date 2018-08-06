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

import fish.payara.security.openid.api.OpenIdClaims;
import static fish.payara.security.openid.api.OpenIdConstant.SUBJECT_IDENTIFIER;
import fish.payara.security.openid.api.OpenIdContext;
import static java.util.Collections.emptyMap;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.SessionScoped;
import javax.json.JsonObject;

/**
 * An injectable interface that provides access to access token, identity token,
 * claims and OpenId Connect provider related information.
 *
 * @author Gaurav Gupta
 */
@SessionScoped
public class OpenIdContextImpl implements OpenIdContext {

    private String tokenType;
    private String accessToken;
    private String identityToken;
    private Map<String, Object> identityTokenClaims;
    private Optional<String> refreshToken;
    private Optional<Integer> expiresIn;
    private JsonObject claims;
    private JsonObject providerMetadata;

    public OpenIdContextImpl() {
        refreshToken = Optional.empty();
        expiresIn = Optional.empty();
    }

    @Override
    public String getSubject() {
        return (String) identityTokenClaims.get(SUBJECT_IDENTIFIER);
    }

    @Override
    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    @Override
    public String getIdentityToken() {
        return identityToken;
    }

    public void setIdentityToken(String identityToken) {
        this.identityToken = identityToken;
    }

    @Override
    public Map<String, Object> getIdentityTokenClaims() {
        if (identityTokenClaims == null) {
            return emptyMap();
        }
        return identityTokenClaims;
    }

    public void setIdentityTokenClaims(Map<String, Object> identityTokenClaims) {
        this.identityTokenClaims = identityTokenClaims;
    }

    @Override
    public Optional<String> getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = Optional.ofNullable(refreshToken);
    }

    @Override
    public Optional<Integer> getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = Optional.ofNullable(expiresIn);
    }

    @Override
    public JsonObject getClaimsJson() {
        return claims;
    }

    @Override
    public OpenIdClaims getClaims() {
        if (claims == null) {
            return null;
        }
        return new OpenIdClaims(claims);
    }

    public void setClaims(JsonObject claims) {
        this.claims = claims;
    }

    @Override
    public JsonObject getProviderMetadata() {
        return providerMetadata;
    }

    public void setProviderMetadata(JsonObject providerMetadata) {
        this.providerMetadata = providerMetadata;
    }

}
