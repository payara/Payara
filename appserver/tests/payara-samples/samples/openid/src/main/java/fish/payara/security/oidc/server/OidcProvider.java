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
package fish.payara.security.oidc.server;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import static fish.payara.security.openid.api.OpenIdConstant.ACCESS_TOKEN;
import static fish.payara.security.openid.api.OpenIdConstant.AUTHORIZATION_CODE;
import static fish.payara.security.openid.api.OpenIdConstant.CLIENT_ID;
import static fish.payara.security.openid.api.OpenIdConstant.CLIENT_SECRET;
import static fish.payara.security.openid.api.OpenIdConstant.CODE;
import static fish.payara.security.openid.api.OpenIdConstant.ERROR_PARAM;
import static fish.payara.security.openid.api.OpenIdConstant.GRANT_TYPE;
import static fish.payara.security.openid.api.OpenIdConstant.IDENTITY_TOKEN;
import static fish.payara.security.openid.api.OpenIdConstant.NONCE;
import static fish.payara.security.openid.api.OpenIdConstant.REDIRECT_URI;
import static fish.payara.security.openid.api.OpenIdConstant.RESPONSE_TYPE;
import static fish.payara.security.openid.api.OpenIdConstant.SCOPE;
import static fish.payara.security.openid.api.OpenIdConstant.STATE;
import static fish.payara.security.openid.api.OpenIdConstant.SUBJECT_IDENTIFIER;
import static fish.payara.security.openid.api.OpenIdConstant.TOKEN_TYPE;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import static java.util.Arrays.asList;
import java.util.Date;
import java.util.UUID;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 *
 * @author Gaurav Gupta
 */
@Path("/oidc-provider")
public class OidcProvider {

        private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TYPE = "Bearer";
    
    private static final String AUTH_CODE_VALUE = "sample_auth_code";
    private static final String ACCESS_TOKEN_VALUE = "sample_access_token";
    public static final String CLIENT_ID_VALUE = "sample_client_id";
    public static final String CLIENT_SECRET_VALUE = "sample_client_secret";
    private static final String SUBJECT_VALUE = "sample_subject";

    private static String nonce;

    private static final Logger LOGGER = Logger.getLogger(OidcProvider.class.getName());

    @Path("/.well-known/openid-configuration")
    @GET
    @Produces(APPLICATION_JSON)
    public Response getConfiguration() {
        String result = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("openid-configuration.json")) {
            result = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(joining("\n"));
        } catch (IOException ex) {
            LOGGER.log(SEVERE, null, ex);
        }
        return Response.ok(result)
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }

    @Path("/auth")
    @GET
    public Response authEndpoint(@QueryParam(CLIENT_ID) String clientId,
            @QueryParam(SCOPE) String scope,
            @QueryParam(RESPONSE_TYPE) String responseType,
            @QueryParam(NONCE) String nonce,
            @QueryParam(STATE) String state,
            @QueryParam(REDIRECT_URI) String redirectUri) throws URISyntaxException {

        StringBuilder returnURL = new StringBuilder(redirectUri);
        returnURL.append("?&" + STATE + "=").append(state);
        returnURL.append("&" + CODE + "=" + AUTH_CODE_VALUE);

        this.nonce = nonce;
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        if (!CODE.equals(responseType)) {
            jsonBuilder.add(ERROR_PARAM, "invalid_response_type");
            return Response.serverError().entity(jsonBuilder.build()).build();
        }
        if (!CLIENT_ID_VALUE.equals(clientId)) {
            jsonBuilder.add(ERROR_PARAM, "invalid_client_id");
            return Response.serverError().entity(jsonBuilder.build()).build();
        }
        return Response.seeOther(new URI(returnURL.toString())).build();
    }

    @Path("/token")
    @POST
    @Produces(APPLICATION_JSON)
    public Response tokenEndpoint(
            @FormParam(CLIENT_ID) String clientId,
            @FormParam(CLIENT_SECRET) String clientSecret,
            @FormParam(GRANT_TYPE) String grantType,
            @FormParam(CODE) String code,
            @FormParam(REDIRECT_URI) String redirectUri) {

        ResponseBuilder builder;
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        if (!CLIENT_ID_VALUE.equals(clientId)) {
            jsonBuilder.add(ERROR_PARAM, "invalid_client_id");
            builder = Response.serverError();
        } else if (!CLIENT_SECRET_VALUE.equals(clientSecret)) {
            jsonBuilder.add(ERROR_PARAM, "invalid_client_secret");
            builder = Response.serverError();
        } else if (!AUTHORIZATION_CODE.equals(grantType)) {
            jsonBuilder.add(ERROR_PARAM, "invalid_grant_type");
            builder = Response.serverError();
        } else if (!AUTH_CODE_VALUE.equals(code)) {
            jsonBuilder.add(ERROR_PARAM, "invalid_auth_code");
            builder = Response.serverError();
        } else {

            Date now = new Date();
            JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                    .issuer("http://localhost:8080/openid-server/webresources/oidc-provider")
                    .subject(SUBJECT_VALUE)
                    .audience(asList(CLIENT_ID_VALUE))
                    .expirationTime(new Date(now.getTime() + 1000 * 60 * 10))
                    .notBeforeTime(now)
                    .issueTime(now)
                    .jwtID(UUID.randomUUID().toString())
                    .claim(NONCE, nonce)
                    
                    .build();
            PlainJWT idToken = new PlainJWT(jwtClaims);
            jsonBuilder.add(IDENTITY_TOKEN, idToken.serialize());
            jsonBuilder.add(ACCESS_TOKEN, ACCESS_TOKEN_VALUE);
            jsonBuilder.add(TOKEN_TYPE, BEARER_TYPE);
            builder = Response.ok();
        }

        return builder.entity(jsonBuilder.build()).build();
    }
    
    @Path("/userinfo")
    @Produces(APPLICATION_JSON)
    @GET
    public Response userinfoEndpoint(@HeaderParam(AUTHORIZATION_HEADER) String authorizationHeader) {
        String accessToken = authorizationHeader.substring(BEARER_TYPE.length() + 1);
        
        ResponseBuilder builder;
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        if (ACCESS_TOKEN_VALUE.equals(accessToken)) {
            builder = Response.ok();
            jsonBuilder.add(SUBJECT_IDENTIFIER, SUBJECT_VALUE)
                    .add("name", "Gaurav")
                    .add("family_name", "Gupta   ")
                    .add("given_name", "Gaurav Gupta")
                    .add("profile", "https://abc.com/+jGauravGupta")
                    .add("picture", "https://abc.com/photo.jpg")
                    .add("email", "gaurav.gupta.jc@gmail.com")
                    .add("email_verified", true)
                    .add("gender", "male")
                    .add("locale", "en");
        } else {
            jsonBuilder.add(ERROR_PARAM, "invalid_access_token");
            builder = Response.serverError();
        }
        return builder.entity(jsonBuilder.build().toString()).build();
    }

}
