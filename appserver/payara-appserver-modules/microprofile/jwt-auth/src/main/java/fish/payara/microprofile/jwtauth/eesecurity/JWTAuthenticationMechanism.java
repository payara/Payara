/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.jwtauth.eesecurity;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.config.Names;

import java.util.Optional;
import java.util.Properties;

import static jakarta.security.enterprise.identitystore.CredentialValidationResult.Status.VALID;

/**
 * This authentication mechanism reads a JWT token from an HTTP header and passes it
 * to an {@link IdentityStore} for validation. 
 * 
 * @author Arjan Tijms
 */
public class JWTAuthenticationMechanism implements HttpAuthenticationMechanism {

    public static final String CONFIG_TOKEN_HEADER_AUTHORIZATION = "Authorization";
    public static final String CONFIG_TOKEN_HEADER_COOKIE = "Cookie";

    private final String configJwtTokenHeader; // either Authorization (default) or Cookie
    private final String configJwtTokenCookie; // name of the token cookie

    public JWTAuthenticationMechanism() {
        // load default names and behavior
        // https://download.eclipse.org/microprofile/microprofile-jwt-auth-1.2/microprofile-jwt-auth-spec-1.2.html#_jwt_and_http_headers
        Optional<Properties> properties = SignedJWTIdentityStore.readVendorProperties();
        Config config = ConfigProvider.getConfig();
        configJwtTokenHeader = SignedJWTIdentityStore.readConfig(Names.TOKEN_HEADER, properties, config, CONFIG_TOKEN_HEADER_AUTHORIZATION);
        if ((!CONFIG_TOKEN_HEADER_AUTHORIZATION.equals(configJwtTokenHeader))
                && (!CONFIG_TOKEN_HEADER_COOKIE.equals(configJwtTokenHeader))) {
            throw new DeploymentException("Configuration " + Names.TOKEN_HEADER + " must be either "
                    + CONFIG_TOKEN_HEADER_AUTHORIZATION + " or " + CONFIG_TOKEN_HEADER_COOKIE
                    + ", but is " + configJwtTokenHeader);
        }
        configJwtTokenCookie = SignedJWTIdentityStore.readConfig(Names.TOKEN_COOKIE, properties, config, "Bearer");
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) throws AuthenticationException {

        // Don't limit processing of JWT to protected pages (httpMessageContext.isProtected())
        // as MP TCK requires JWT being parsed (if provided) even if not in protected pages.
        IdentityStoreHandler identityStoreHandler = CDI.current().select(IdentityStoreHandler.class).get();

        SignedJWTCredential credential = getCredential(request);

        if (credential != null) {

            CredentialValidationResult result = identityStoreHandler.validate(credential);
            if (result.getStatus() == VALID) {
                httpMessageContext.getClientSubject()
                        .getPrincipals()
                        .add(result.getCallerPrincipal());
                return httpMessageContext.notifyContainerAboutLogin(result);
            }

            return httpMessageContext.responseUnauthorized();
        }

        return httpMessageContext.doNothing();
    }

    private SignedJWTCredential getCredential(HttpServletRequest request) {
        Optional<String> token = Optional.empty();
        if (CONFIG_TOKEN_HEADER_AUTHORIZATION.equals(configJwtTokenHeader)) {
            // use Authorization header
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = Optional.of(authorizationHeader.substring("Bearer ".length()));
            }
        } else {
            // use Cookie header
            String bearerMark = ";" + configJwtTokenCookie + "=";
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null && cookieHeader.startsWith("$Version=") && cookieHeader.contains(bearerMark)) {
                token = Optional.of(cookieHeader.substring(cookieHeader.indexOf(bearerMark) + bearerMark.length()));
            }
        }

        return token.map(t -> createSignedJWTCredential(t))
                .orElse(null);
    }

    private SignedJWTCredential createSignedJWTCredential(String token) {
        if (token != null && !token.isEmpty()) {
            return new SignedJWTCredential(token);
        }

        return null;
    }
}
