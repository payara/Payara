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
package fish.payara.security.openid.controller;

import static fish.payara.security.openid.api.OpenIdConstant.CLIENT_ID;
import static fish.payara.security.openid.api.OpenIdConstant.DISPLAY;
import static fish.payara.security.openid.api.OpenIdConstant.NONCE;
import static fish.payara.security.openid.api.OpenIdConstant.PROMPT;
import static fish.payara.security.openid.api.OpenIdConstant.REDIRECT_URI;
import static fish.payara.security.openid.api.OpenIdConstant.RESPONSE_MODE;
import static fish.payara.security.openid.api.OpenIdConstant.RESPONSE_TYPE;
import static fish.payara.security.openid.api.OpenIdConstant.SCOPE;
import static fish.payara.security.openid.api.OpenIdConstant.STATE;
import static fish.payara.security.openid.api.OpenIdConstant.RESOURCE;
import fish.payara.security.openid.api.OpenIdState;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import fish.payara.security.openid.domain.OpenIdNonce;
import java.io.IOException;
import static java.util.logging.Level.FINEST;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.security.enterprise.AuthenticationStatus;
import static javax.security.enterprise.AuthenticationStatus.SEND_CONTINUE;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import static org.glassfish.common.util.StringHelper.isEmpty;

/**
 * Controller for Authentication endpoint
 *
 * @author Gaurav Gupta
 */
@ApplicationScoped
public class AuthenticationController {

    @Inject
    private StateController stateController;

    @Inject
    private NonceController nonceController;

    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());

    /**
     * (1) The RP (Client) sends a request to the OpenId Connect Provider (OP)
     * to authenticates the End-User using the Authorization Code Flow and
     * authorization Code is returned from the Authorization Endpoint.
     * <br>
     * (2) Authorization Server authenticates the End-User, obtains End-User
     * Consent/Authorization and sends the End-User back to the Client with an
     * Authorization Code.
     *
     *
     * @param configuration
     * @param request
     * @param response
     * @return
     */
    public AuthenticationStatus authenticateUser(
            OpenIdConfiguration configuration,
            HttpServletRequest request,
            HttpServletResponse response) {

        /**
         * Client prepares an authentication request and redirect to the
         * Authorization Server. if query param value is invalid then OpenId
         * Connect provider redirect to error page (hosted in OP domain).
         */
        UriBuilder authRequest
                = UriBuilder.fromUri(configuration.getProviderMetadata().getAuthorizationEndpoint())
                        .queryParam(SCOPE, configuration.getScopes())
                        .queryParam(RESPONSE_TYPE, configuration.getResponseType())
                        .queryParam(CLIENT_ID, configuration.getClientId())
                        .queryParam(REDIRECT_URI, configuration.buildRedirectURI(request));

        OpenIdState state = new OpenIdState();
        authRequest.queryParam(STATE, state.getValue());
        stateController.store(state, configuration, request, response);

        // add nonce for replay attack prevention
        if (configuration.isUseNonce()) {
            OpenIdNonce nonce = new OpenIdNonce();
            // use a cryptographic hash of the value as the nonce parameter
            String nonceHash = nonceController.getNonceHash(nonce);
            authRequest.queryParam(NONCE, nonceHash);
            nonceController.store(nonce, configuration, request, response);

        }
        if (!isEmpty(configuration.getResponseMode())) {
            authRequest.queryParam(RESPONSE_MODE, configuration.getResponseMode());
        }
        
        // add resource parameter if not empty
        if (!isEmpty(configuration.getResource())) {
            authRequest.queryParam(RESOURCE, configuration.getResource());
        }
        
        if (!isEmpty(configuration.getDisplay())) {
            authRequest.queryParam(DISPLAY, configuration.getDisplay());
        }
        if (!isEmpty(configuration.getPrompt())) {
            authRequest.queryParam(PROMPT, configuration.getPrompt());
        }

        configuration.getExtraParameters().forEach(authRequest::queryParam);

        String authUrl = authRequest.toString();
        LOGGER.log(FINEST, "Redirecting for authentication to {0}", authUrl);
        try {
            response.sendRedirect(authUrl);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return SEND_CONTINUE;
    }

}
