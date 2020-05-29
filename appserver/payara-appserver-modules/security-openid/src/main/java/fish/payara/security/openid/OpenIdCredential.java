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
package fish.payara.security.openid;

import static fish.payara.security.openid.api.OpenIdConstant.ACCESS_TOKEN;
import static fish.payara.security.openid.api.OpenIdConstant.IDENTITY_TOKEN;
import fish.payara.security.openid.api.AccessToken;
import fish.payara.security.openid.api.IdentityToken;
import static fish.payara.security.openid.api.OpenIdConstant.EXPIRES_IN;
import static fish.payara.security.openid.api.OpenIdConstant.SCOPE;
import static fish.payara.security.openid.api.OpenIdConstant.TOKEN_TYPE;
import fish.payara.security.openid.domain.AccessTokenImpl;
import fish.payara.security.openid.domain.IdentityTokenImpl;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import static java.util.Objects.nonNull;
import javax.json.JsonObject;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.Credential;

/**
 *
 * @author Gaurav Gupta
 */
public class OpenIdCredential implements Credential {

    private final HttpMessageContext httpContext;

    private final OpenIdConfiguration configuration;

    private final IdentityToken identityToken;

    private AccessToken accessToken;

    public OpenIdCredential(JsonObject tokensObject, HttpMessageContext httpContext, OpenIdConfiguration configuration) {
        this.httpContext = httpContext;
        this.configuration = configuration;

        this.identityToken = new IdentityTokenImpl(configuration, tokensObject.getString(IDENTITY_TOKEN));
        String accessTokenString = tokensObject.getString(ACCESS_TOKEN, null);
        Long expiresIn = null;
        if(nonNull(tokensObject.getJsonNumber(EXPIRES_IN))){
            expiresIn = tokensObject.getJsonNumber(EXPIRES_IN).longValue();
        }
        String tokenType = tokensObject.getString(TOKEN_TYPE, null);
        String scopeString = tokensObject.getString(SCOPE, null);
        if (nonNull(accessTokenString)) {
            accessToken = new AccessTokenImpl(configuration, tokenType, accessTokenString, expiresIn, scopeString);
        }
    }

    public IdentityToken getIdentityToken() {
        return identityToken;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public HttpMessageContext getHttpContext() {
        return httpContext;
    }

    public OpenIdConfiguration getConfiguration() {
        return configuration;
    }

}
