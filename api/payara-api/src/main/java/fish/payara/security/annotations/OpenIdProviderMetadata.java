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
package fish.payara.security.annotations;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link OpenIdProviderMetadata} annotation overrides the openid connect
 * provider's endpoint value, discovered using providerUri.
 *
 * @author Gaurav Gupta
 */
@Retention(RUNTIME)
public @interface OpenIdProviderMetadata {

    /**
     * The Microprofile Config key for the auth endpoint is
     * <code>{@value}</code>
     */
    public static final String OPENID_MP_AUTHORIZATION_ENDPOINT = "payara.security.openid.provider.authorizationEndpoint";

    /**
     * Required. The URL for the OAuth2 provider to provide authentication
     * <p>
     * This must be a https endpoint.
     * </p>
     * To set this using Microprofile Config use
     * {@code payara.security.openid.provider.authorizationEndpoint}.
     *
     * @return
     */
    String authorizationEndpoint() default "";

    /**
     * The Microprofile Config key for the token Endpoint is
     * <code>{@value}</code>
     */
    public static final String OPENID_MP_TOKEN_ENDPOINT = "payara.security.openid.provider.tokenEndpoint";

    /**
     * Required. The URL for the OAuth2 provider to give the authorization token
     * <p>
     * To set this using Microprofile Config use
     * {@code payara.security.openid.provider.tokenEndpoint}
     * </p>
     *
     * @return
     */
    String tokenEndpoint() default "";

    /**
     * The Microprofile Config key for the userinfo Endpoint is
     * <code>{@value}</code>
     */
    public static final String OPENID_MP_USERINFO_ENDPOINT = "payara.security.openid.provider.userinfoEndpoint";

    /**
     * Required. An OAuth 2.0 Protected Resource that returns Claims about the
     * authenticated End-User.
     * <p>
     * To set this using Microprofile Config use
     * {@code payara.security.openid.provider.userinfoEndpoint}
     * </p>
     *
     * @return
     */
    String userinfoEndpoint() default "";

    /**
     * The Microprofile Config key for the end session Endpoint is
     * <code>{@value}</code>
     */
    public static final String OPENID_MP_END_SESSION_ENDPOINT = "payara.security.openid.provider.endSessionEndpoint";

    /**
     * Optional. OP endpoint to notify that the End-User has logged out of the
     * site and might want to log out of the OP as well.
     * <p>
     * To set this using Microprofile Config use
     * {@code payara.security.openid.provider.endSessionEndpoint}
     * </p>
     *
     * @return
     */
    String endSessionEndpoint() default "";

    /**
     * The Microprofile Config key for the jwks uri is <code>{@value}</code>
     */
    public static final String OPENID_MP_JWKS_URI = "payara.security.openid.provider.jwksURI";

    /**
     * Required. An OpenId Connect Provider's JSON Web Key Set document
     * <p>
     * This contains the signing key(s) the RP uses to validate signatures from
     * the OP. The JWK Set may also contain the Server's encryption key(s),
     * which are used by RPs to encrypt requests to the Server.
     * </p>
     * To set this using Microprofile Config use
     * {@code payara.security.openid.provider.jwksURI}
     *
     * @return
     */
    String jwksURI() default "";
}
