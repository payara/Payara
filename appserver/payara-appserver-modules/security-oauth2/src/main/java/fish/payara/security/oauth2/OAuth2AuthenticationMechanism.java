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
package fish.payara.security.oauth2;

import fish.payara.security.oauth2.annotation.OAuth2AuthenticationDefinition;
import fish.payara.security.oauth2.api.OAuth2State;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AutoApplySession;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import sun.reflect.Reflection;

/**
 * The AuthenticationMechanism used for authenticate users
 *
 * @author jonathan coustick
 * @since 4.1.2.172
 */
@AutoApplySession
@Typed(OAuth2AuthenticationMechanism.class)
public class OAuth2AuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger logger = Logger.getLogger("OAuth2Mechanism");

    private String authEndpoint;
    private String tokenEndpoint;
    private String clientID;
    private char[] clientSecret;
    private String redirectURI;
    private String scopes;
    private String[] extraParameters;

    @Inject
    OAuth2StateHolder state;

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    public OAuth2AuthenticationMechanism() {
        //no-op constuctor
    }

    public OAuth2AuthenticationMechanism(OAuth2AuthenticationDefinition definition) {
        setDefinition(definition);

    }

    public OAuth2AuthenticationMechanism setDefinition(OAuth2AuthenticationDefinition definition) {
        authEndpoint = definition.authEndpoint();
        tokenEndpoint = definition.tokenEndpoint();
        clientID = definition.clientId();
        clientSecret = definition.clientSecret().toCharArray();
        redirectURI = definition.redirectURI();
        scopes = definition.scopes();
        extraParameters = definition.extraParameters();
        return this;
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext)
            throws AuthenticationException {        
        
        if (httpMessageContext.isProtected() && request.getUserPrincipal() == null){
            //Needs to login and has not already done so
            return redirectForAuth(httpMessageContext);   
        }
        
        
        String recievedState = request.getParameter("state");
        if (request.getRequestURL().toString().equals(redirectURI) && recievedState != null) {
            //In the process of logging in
            if (recievedState.equals(state.getState())) {
                return validateCallback(request, httpMessageContext);
            } else {
                logger.log(Level.WARNING, "Inconsistent recieved state");
                return httpMessageContext.notifyContainerAboutLogin(CredentialValidationResult.NOT_VALIDATED_RESULT);
            }
        } else {
            
            // Is not trying to login, has not logged in already and does not need to login
            return httpMessageContext.doNothing();
        }
    }

    /**
     * Called when the callback URL is hit, this gets the authorisation token and logs the user in
     * @param request
     * @param context
     * @return 
     */
    private AuthenticationStatus validateCallback(HttpServletRequest request, HttpMessageContext context){
        logger.log(Level.SEVERE, "User Authenticated, now getting authorisation token");
                Client jaxrsClient = ClientBuilder.newClient();

                //Creates a new JAX-RS form with all paramters
                Form form = new Form()
                        .param("client_id", clientID)
                        .param("client_secret", new String(clientSecret))
                        .param("code", request.getParameter("code"))
                        .param("state", state.getState());
                if (redirectURI != null && !redirectURI.isEmpty()) {
                    form.param("redirect_uri", redirectURI);
                }
                if (scopes != null && !scopes.isEmpty()) {
                    form.param("scopes", scopes);
                }
                for (String extra : extraParameters) {
                    String[] parts = extra.split("=");
                    form.param(parts[0], parts[1]);
                }

                WebTarget target = jaxrsClient.target(tokenEndpoint);
                Response oauthResponse = target.request()
                        .accept(MediaType.APPLICATION_JSON)
                        .header("referer", request.getRequestURL().toString())
                        .post(Entity.form(form));

                // Get back the result of the REST request
                String result = oauthResponse.readEntity(String.class);
                JsonObject object = Json.createReader(new StringReader(result)).readObject();
                logger.log(Level.SEVERE, result);
                if (oauthResponse.getStatus() != 200) {

                    String error = object.getString("error", "Unknown Error");
                    String errorDescription = object.getString("error_description", "Unknown");
                    logger.log(Level.WARNING, "[OAUTH-001] Error occurred authenticating user: {0} caused by {1}", new Object[]{error, errorDescription});
                    return context.notifyContainerAboutLogin(CredentialValidationResult.INVALID_RESULT);
                } else {

                    state.setToken(object.getString("access_token"));
                    RememberMeCredential credential = new RememberMeCredential(result);
                    CredentialValidationResult validationResult = identityStoreHandler.validate(credential);
                    return context.notifyContainerAboutLogin(validationResult);
                }
    }
    
    
    /**
     * If the user is not logged in then redirect them to OAuth provider
     * @param context
     * @return 
     */
    private AuthenticationStatus redirectForAuth(HttpMessageContext context) {
        logger.log(Level.SEVERE, "Redirecting for authentication");
        StringBuilder authTokenRequest = new StringBuilder(authEndpoint);
        authTokenRequest.append("?client_id=").append(clientID);
        authTokenRequest.append("&state=").append(state.getState());
        authTokenRequest.append("&response_type=code");
        if (redirectURI != null && !redirectURI.isEmpty()) {
            authTokenRequest.append("&redirect_uri=").append(redirectURI);
        }
        if (scopes != null && !scopes.isEmpty()) {
            authTokenRequest.append("&scopes=").append(scopes);
        }
        for (String extra : extraParameters) {
            authTokenRequest.append("&").append(extra);
        }

        return context.redirect(authTokenRequest.toString());
    }

}
