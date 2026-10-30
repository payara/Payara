/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import fish.payara.security.openid.api.OpenIdConstant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
import static fish.payara.security.openid.api.OpenIdConstant.EXPIRES_IN;
import static java.util.Arrays.asList;
import java.util.List;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 *
 * @author Gaurav Gupta
 */
@Path("/oidc-provider{subject:/subject-[^/]+|}")
public class OidcProvider {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TYPE = "Bearer";

    private static final String AUTH_CODE_VALUE = "sample_auth_code";
    private static final String ACCESS_TOKEN_VALUE = "sample_access_token";
    public static final String CLIENT_ID_VALUE = "sample_client_id";
    public static final String CLIENT_SECRET_VALUE = "sample_client_secret";

    public static final String USER_GROUPS_LIST_KEY = "test.openid.userGroupsList";
    public static final String ROLES_IN_USERINFO_KEY = "fish.payara.test.openid.rolesInUserInfoEndpoint";
    public static final String EXPIRES_IN_SECONDS_KEY = "fish.payara.test.openid.expiresInSeconds";

    @Inject @ConfigProperty(name = ROLES_IN_USERINFO_KEY, defaultValue = "false")
    boolean rolesInUserInfoEndpoint;

    @Inject @ConfigProperty(name = USER_GROUPS_LIST_KEY, defaultValue = "all")
    List<String> userGroups;

    @Inject @ConfigProperty(name = EXPIRES_IN_SECONDS_KEY, defaultValue = "3600")
    Integer expiresInSeconds;

    @PathParam("subject")
    String subject;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return getSubject();
    }

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
                    .map(line -> {
                        String tenant = subject;
                        return line.replaceAll("\\$\\{tenant\\}", tenant);  // replace ${tenant} with empty value or subject
                    })
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

        OidcProvider.nonce = nonce;
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
            JWTClaimsSet.Builder jstClaimsBuilder = new JWTClaimsSet.Builder()
                    .issuer("http://localhost:8080/openid-server/webresources/oidc-provider" + subject)
                    .subject(getSubject())
                    .audience(asList(CLIENT_ID_VALUE))
                    .expirationTime(new Date(now.getTime() + 1000 * 60 * 10))
                    .notBeforeTime(now)
                    .issueTime(now)
                    .jwtID(UUID.randomUUID().toString())
                    .claim(NONCE, nonce);
            if (!rolesInUserInfoEndpoint) {
                jstClaimsBuilder.claim(OpenIdConstant.GROUPS, userGroups);
            }
            JWTClaimsSet jwtClaims = jstClaimsBuilder.build();

            PlainJWT idToken = new PlainJWT(jwtClaims);
            jsonBuilder.add(IDENTITY_TOKEN, idToken.serialize());
            jsonBuilder.add(ACCESS_TOKEN, ACCESS_TOKEN_VALUE);
            jsonBuilder.add(TOKEN_TYPE, BEARER_TYPE);
            jsonBuilder.add(EXPIRES_IN, 1000);
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
            jsonBuilder.add(SUBJECT_IDENTIFIER, getSubject())
                    .add("name", "Gaurav")
                    .add("family_name", "Gupta   ")
                    .add("given_name", "Gaurav Gupta")
                    .add("profile", "https://abc.com/+jGauravGupta")
                    .add("picture", "https://abc.com/photo.jpg")
                    .add("email", "gaurav.gupta.jc@gmail.com")
                    .add("email_verified", true)
                    .add("gender", "male")
                    .add("locale", "en");
            if (rolesInUserInfoEndpoint) {
                JsonArrayBuilder groupsBuilder = Json.createArrayBuilder();
                userGroups.forEach(g -> {
                    groupsBuilder.add(g);
                });
                jsonBuilder.add(OpenIdConstant.GROUPS, groupsBuilder);
            }
        } else {
            jsonBuilder.add(ERROR_PARAM, "invalid_access_token");
            builder = Response.serverError();
        }
        return builder.entity(jsonBuilder.build().toString()).build();
    }

    private String getSubject() {
        String subjectPrefix = "/subject-";
        return subject != null && subject.startsWith(subjectPrefix) ? subject.substring(subjectPrefix.length()) : "sample_subject";
    }

    private String getTenant() {
        String subjectPrefix = "/subject-";
        return subject != null && subject.startsWith(subjectPrefix) ? subject.substring(subjectPrefix.length()) : "";
    }

}
