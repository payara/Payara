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

import fish.payara.security.openid.api.AccessToken;
import fish.payara.security.openid.api.IdentityToken;
import fish.payara.security.openid.api.OpenIdClaims;
import static fish.payara.security.openid.api.OpenIdConstant.SUBJECT_IDENTIFIER;
import fish.payara.security.openid.api.OpenIdContext;
import fish.payara.security.openid.api.RefreshToken;
import java.util.Optional;
import javax.enterprise.context.SessionScoped;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * An injectable interface that provides access to access token, identity token,
 * claims and OpenId Connect provider related information.
 *
 * @author Gaurav Gupta
 */
@SessionScoped
public class OpenIdContextImpl implements OpenIdContext {

    private String callerName;
    private String callerGroups;
    private String tokenType;
    private AccessToken accessToken;
    private IdentityToken identityToken;
    private RefreshToken refreshToken;
    private Long expiresIn;
    private JsonObject claims;
    private JsonObject providerMetadata;

    @Override
    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    @Override
    public String getCallerGroups() {
        return callerGroups;
    }

    public void setCallerGroups(String callerGroups) {
        this.callerGroups = callerGroups;
    }

    @Override
    public String getSubject() {
        return (String) getIdentityToken().getClaim(SUBJECT_IDENTIFIER);
    }

    @Override
    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken token) {
        this.accessToken = token;
    }

    @Override
    public IdentityToken getIdentityToken() {
        return identityToken;
    }

    public void setIdentityToken(IdentityToken identityToken) {
        this.identityToken = identityToken;
    }

    @Override
    public Optional<RefreshToken> getRefreshToken() {
        return Optional.ofNullable(refreshToken);
    }

    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public Optional<Long> getExpiresIn() {
        return Optional.ofNullable(expiresIn);
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public JsonObject getClaimsJson() {
        if (claims == null) {
            return Json.createObjectBuilder().build();
        }
        return claims;
    }

    @Override
    public OpenIdClaims getClaims() {
        return new OpenIdClaims(getClaimsJson());
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
