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

import fish.payara.security.openid.domain.OpenIdConfiguration;
import static fish.payara.security.openid.api.OpenIdConstant.ERROR_DESCRIPTION_PARAM;
import static fish.payara.security.openid.api.OpenIdConstant.ERROR_PARAM;
import static fish.payara.security.openid.api.OpenIdConstant.SUBJECT_IDENTIFIER;
import java.io.StringReader;
import static java.util.Objects.nonNull;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import fish.payara.security.openid.api.OpenIdContext;

/**
 * Controller for Token endpoint
 *
 * @author Gaurav Gupta
 */
@ApplicationScoped
public class UserInfoController {

    @Inject
    private OpenIdContext context;

    private static final String APPLICATION_JWT = "application/jwt";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TYPE = "Bearer ";

    private static final Logger LOGGER = Logger.getLogger(UserInfoController.class.getName());

    /**
     * (6) The RP send a request with the Access Token to the UserInfo Endpoint
     * and requests the claims about the End-User.
     *
     * @param configuration
     * @param accessToken
     * @return the claims json object
     */
    public JsonObject getUserInfo(OpenIdConfiguration configuration, String accessToken) {
        LOGGER.finest("Sending the request to the userinfo endpoint");
        JsonObject userInfo;

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(configuration.getProviderMetadata().getUserinfoEndpoint());
        Response response = target.request()
                .accept(APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER_TYPE + accessToken)
                // 5.5.  Requesting Claims using the "claims" Request Parameter ??
                .get();

        String responseBody = response.readEntity(String.class);

        String contentType = response.getHeaderString(CONTENT_TYPE);
        if (response.getStatus() == Status.OK.getStatusCode()) {
            if (nonNull(contentType) && contentType.contains(APPLICATION_JSON)) {
                // Successful UserInfo Response
                userInfo = Json.createReader(new StringReader(responseBody)).readObject();
            } else if (nonNull(contentType) && contentType.contains(APPLICATION_JWT)) {
                throw new UnsupportedOperationException("application/jwt content-type not supported for userinfo endpoint");
                //If the UserInfo Response is signed and/or encrypted, then the Claims are returned in a JWT and the content-type MUST be application/jwt. The response MAY be encrypted without also being signed. If both signing and encryption are requested, the response MUST be signed then encrypted, with the result being a Nested JWT, ??
                //If signed, the UserInfo Response SHOULD contain the Claims iss (issuer) and aud (audience) as members. The iss value SHOULD be the OP's Issuer Identifier URL. The aud value SHOULD be or include the RP's Client ID value.
            } else {
                throw new IllegalStateException("Invalid response received from userinfo endpoint with content-type : " + contentType);
            }
        } else {
            // UserInfo Error Response
            JsonObject responseObject = Json.createReader(new StringReader(responseBody)).readObject();
            String error = responseObject.getString(ERROR_PARAM, "Unknown Error");
            String errorDescription = responseObject.getString(ERROR_DESCRIPTION_PARAM, "Unknown");
            LOGGER.log(WARNING, "Error occurred in fetching user info: {0} caused by {1}", new Object[]{error, errorDescription});
            throw new IllegalStateException("Error occurred in fetching user info");
        }
        validateUserInfoClaims(userInfo);
        return userInfo;
    }

    private void validateUserInfoClaims(JsonObject userInfo) {
        /**
         * Check the token substitution attacks : The sub Claim in the UserInfo
         * Response must be verified to exactly match the sub claim in the ID
         * Token.
         */
        if (!context.getSubject().equals(userInfo.getString(SUBJECT_IDENTIFIER))) {
            throw new IllegalStateException("UserInfo Response is invalid as sub claim must match with the sub Claim in the ID Token");
        }
    }

}
