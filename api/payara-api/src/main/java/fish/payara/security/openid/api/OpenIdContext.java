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
package fish.payara.security.openid.api;

import java.io.Serializable;
import java.util.Optional;
import javax.json.JsonObject;

/**
 * An injectable interface that provides access to access token, identity token,
 * claims and OpenId Connect provider related information.
 *
 * @author Gaurav Gupta
 */
public interface OpenIdContext extends Serializable {

    /**
     * Gets the caller name of the validated caller
     *
     * @return
     */
    String getCallerName();

    /**
     * Gets the groups associated with the caller
     *
     * @return
     */
    String getCallerGroups();

    /**
     * Subject Identifier. A locally unique and never reassigned identifier
     * within the Issuer for the End-User, which is intended to be consumed by
     * the Client
     *
     * @return
     */
    String getSubject();

    /**
     * Gets the token type value. The value MUST be Bearer or another token_type
     * value that the Client has negotiated with the Authorization Server.
     *
     * @return
     */
    String getTokenType();

    /**
     * Gets the authorization token that was received from the OpenId Connect
     * provider
     *
     * @return
     */
    AccessToken getAccessToken();

    /**
     * Gets the identity token that was received from the OpenId Connect
     * provider
     *
     * @return
     */
    IdentityToken getIdentityToken();

    /**
     * Returns the refresh token that can be used to get a new access token
     *
     * @return
     */
    Optional<RefreshToken> getRefreshToken();

    /**
     * Return the time that the access token is granted for, if it is set to
     * expire
     *
     * @return
     */
    Optional<Long> getExpiresIn();

    /**
     * Gets the User Claims that was received from the userinfo endpoint
     *
     * @return the claims json
     */
    JsonObject getClaimsJson();

    /**
     * Gets the User Claims that was received from the userinfo endpoint
     *
     * @return the {@link OpenIdClaims} instance
     */
    OpenIdClaims getClaims();

    /**
     * The OpenId Connect Provider's metadata document fetched via provider URI.
     *
     * @return
     */
    JsonObject getProviderMetadata();

}
