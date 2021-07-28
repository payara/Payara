/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import fish.payara.security.openid.api.IdentityToken;
import static fish.payara.security.openid.api.OpenIdConstant.ISSUER_IDENTIFIER;
import static fish.payara.security.openid.api.OpenIdConstant.SUBJECT_IDENTIFIER;
import static fish.payara.security.openid.api.OpenIdConstant.AUDIENCE;
import static fish.payara.security.openid.api.OpenIdConstant.AUTHORIZED_PARTY;
import fish.payara.security.openid.domain.OpenIdConfiguration;
import java.util.List;
import static java.util.Objects.isNull;

/**
 * Validates the ID token received from the Refresh token response
 *
 * @author Gaurav Gupta
 */
public class RefreshedIdTokenClaimsSetVerifier extends TokenClaimsSetVerifier {

    private final IdentityToken previousIdToken;

    public RefreshedIdTokenClaimsSetVerifier(IdentityToken previousIdToken, OpenIdConfiguration configuration) {
        super(configuration);
        this.previousIdToken = previousIdToken;
    }

    /**
     * Validate ID Token's claims received from the Refresh token response
     *
     * @param claims
     * @throws com.nimbusds.jwt.proc.BadJWTException
     */
    @Override
    public void verify(JWTClaimsSet claims) throws BadJWTException {

        String previousIssuer = (String) previousIdToken.getClaim(ISSUER_IDENTIFIER);
        String newIssuer = (String) claims.getIssuer();
        if (newIssuer == null || !newIssuer.equals(previousIssuer)) {
            throw new IllegalStateException("iss Claim Value MUST be the same as in the ID Token issued when the original authentication occurred.");
        }

        String previousSubject = (String) previousIdToken.getClaim(SUBJECT_IDENTIFIER);
        String newSubject = (String) claims.getSubject();
        if (newSubject == null || !newSubject.equals(previousSubject)) {
            throw new IllegalStateException("sub Claim Value MUST be the same as in the ID Token issued when the original authentication occurred.");
        }

        List<String> previousAudience = (List<String>) previousIdToken.getClaim(AUDIENCE);
        List<String> newAudience = claims.getAudience();
        if (newAudience == null || !newAudience.equals(previousAudience)) {
            throw new IllegalStateException("aud Claim Value MUST be the same as in the ID Token issued when the original authentication occurred.");
        }

        if(isNull(claims.getIssueTime())) {
            throw new IllegalStateException("iat Claim Value must not be null.");
        }

        String previousAzp = (String) previousIdToken.getClaim(AUTHORIZED_PARTY);
        String newAzp = (String) claims.getClaim(AUTHORIZED_PARTY);
        if (previousAzp == null ? newAzp != null : !previousAzp.equals(newAzp)) {
            throw new IllegalStateException("azp Claim Value MUST be the same as in the ID Token issued when the original authentication occurred.");
        }

        // if the ID Token contains an auth_time Claim, its value MUST represent the time of the original authentication - not the time that the new ID token is issued,
    }

}
