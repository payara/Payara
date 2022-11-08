/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 */

package fish.payara.microprofile.jwtauth.cdi;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.json.JsonValue;
import javax.security.enterprise.SecurityContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;

import fish.payara.microprofile.jwtauth.eesecurity.JWTAuthenticationMechanism;
import fish.payara.microprofile.jwtauth.jwt.JsonWebTokenImpl;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
class JsonWebTokenProducer {
    @Produces
    @RequestScoped
    @Typed({JsonWebTokenImpl.class, JsonWebToken.class}) // so it's not eligible for injection as Principal
    JsonWebTokenImpl currentJwt(SecurityContext securityContext, HttpServletRequest request) {
        Principal principal = securityContext.getCallerPrincipal();
        if (principal != null && principal instanceof JsonWebTokenImpl) {
            return (JsonWebTokenImpl) principal;
        }
        if (request.getAttribute(JWTAuthenticationMechanism.INVALID_JWT_TOKEN) != null) {
            return INVALID_JWT_TOKEN;
        }
        return EMPTY_JWT_TOKEN;
    }

    private final static JsonWebTokenImpl EMPTY_JWT_TOKEN = new JsonWebTokenImpl(null, Collections.emptyMap());

    static final JsonWebTokenImpl INVALID_JWT_TOKEN = new JsonWebTokenImpl(null, Collections.emptyMap()) {
        void throwOnInvalidToken() {
            throw new NotAuthorizedException("Presented JWT token is invalid");
        }

        @Override
        public Map<String, JsonValue> getClaims() {
            throwOnInvalidToken();
            return super.getClaims();
        }

        @Override
        public <T> T getClaim(String claimName) {
            throwOnInvalidToken();
            return super.getClaim(claimName);
        }

        @Override
        public Set<String> getClaimNames() {
            throwOnInvalidToken();
            return super.getClaimNames();
        }

        @Override
        public String getRawToken() {
            throwOnInvalidToken();
            return super.getRawToken();
        }

        @Override
        public String getIssuer() {
            throwOnInvalidToken();
            return super.getIssuer();
        }

        @Override
        public Set<String> getAudience() {
            throwOnInvalidToken();
            return super.getAudience();
        }

        @Override
        public String getSubject() {
            throwOnInvalidToken();
            return super.getSubject();
        }

        @Override
        public String getTokenID() {
            throwOnInvalidToken();
            return super.getTokenID();
        }

        @Override
        public long getExpirationTime() {
            throwOnInvalidToken();
            return super.getExpirationTime();
        }

        @Override
        public long getIssuedAtTime() {
            throwOnInvalidToken();
            return super.getIssuedAtTime();
        }

        @Override
        public Set<String> getGroups() {
            throwOnInvalidToken();
            return super.getGroups();
        }

        @Override
        public boolean containsClaim(String claimName) {
            throwOnInvalidToken();
            return super.containsClaim(claimName);
        }

        @Override
        public <T> T getClaim(Claims claim) {
            throwOnInvalidToken();
            return super.getClaim(claim);
        }

        @Override
        public <T> Optional<T> claim(String claimName) {
            throwOnInvalidToken();
            return super.claim(claimName);
        }

        @Override
        public <T> Optional<T> claim(Claims claim) {
            throwOnInvalidToken();
            return super.claim(claim);
        }
    };
}
