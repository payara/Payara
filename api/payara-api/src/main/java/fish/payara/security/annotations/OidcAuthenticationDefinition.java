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
package fish.payara.security.annotations;

import fish.payara.security.oidc.api.DisplayType;
import static fish.payara.security.oidc.api.DisplayType.PAGE;
import static fish.payara.security.oidc.api.OidcConstant.EMAIL_SCOPE;
import static fish.payara.security.oidc.api.OidcConstant.OPENID_SCOPE;
import static fish.payara.security.oidc.api.OidcConstant.PROFILE_SCOPE;
import fish.payara.security.oidc.api.PromptType;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Repeatable;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;

import java.lang.annotation.Target;

/**
 * {@link OidcAuthenticationDefinition} annotation defines oidc client
 * configuration and The value of each parameter can be overwritten via mp
 * config properties.
 *
 * @author Gaurav Gupta
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
@Repeatable(OidcAuthenticationDefinitions.class)
public @interface OidcAuthenticationDefinition {

    /**
     * The Microprofile Config key for the provider uri is <code>{@value}</code>
     */
    public static final String OIDC_MP_PROVIDER_URI = "payara.security.oidc.provider.uri";

    /**
     * Required. The provider uri (
     * http://openid.net/specs/openid-connect-discovery-1_0.html ) to read /
     * discover the metadata of the oidc provider.
     *
     * @return
     */
    String providerUri() default "";

    /**
     * To override the oidc provider's metadata property discovered via
     * providerUri.
     *
     * @return
     */
    OidcProviderMetadata providerMetadata() default @OidcProviderMetadata;

    /**
     * The Microprofile Config key for the clientId is <code>{@value}</code>
     */
    public static final String OIDC_MP_CLIENT_ID = "payara.security.oidc.clientId";

    /**
     * Required. The client identifier issued when the application was
     * registered
     * <p>
     * To set this using Microprofile Config use
     * {@code payara.security.oidc.cliendId}
     *
     * @return the client identifier
     */
    String clientId() default "";

    /**
     * The Microprofile Config key for the client secret is <code>{@value}</code>
     */
    public static final String OIDC_MP_CLIENT_SECRET = "payara.security.oidc.clientSecret";

    /**
     * Required. The client secret
     * <p>
     * It is recommended to set this using an alias.
     * </p>
     * To set this using Microprofile Config use
     * {@code payara.security.oidc.clientSecret}
     *
     * @return
     * @see
     * <a href="https://docs.payara.fish/documentation/payara-server/password-aliases/password-aliases-overview.html">Payara
     * Password Aliases Documentation</a>
     */
    String clientSecret() default "";

    /**
     * The Microprofile Config key for the redirect URI is <code>{@value}</code>
     */
    public static final String OIDC_MP_REDIRECT_URI = "payara.security.oidc.redirectURI";

    /**
     * The redirect URI to which the response will be sent by OIDC Provider.
     * This URI must exactly match one of the Redirection URI values for the
     * Client pre-registered at the OpenID Provider.
     *
     * To set this using Microprofile Config use
     * {@code payara.security.oidc.redirectURI}
     *
     * @return
     */
    String redirectURI() default "";

    /**
     * The Microprofile Config key for the scope is <code>{@value}</code>
     *
     * <p>
     * The defined values are: profile, email, address, phone, and
     * offline_access.
     * </p>
     */
    public static final String OIDC_MP_SCOPE = "payara.security.oidc.scope";

    /**
     * Optional. The scope value defines the access privileges. The basic (and
     * required) scope for OpenID Connect is the openid scope.
     *
     * @return
     */
    String[] scope() default {OPENID_SCOPE, EMAIL_SCOPE, PROFILE_SCOPE};

    /**
     * The Microprofile Config key for the scope is <code>{@value}</code>
     *
     * <p>
     * The defined values are: profile, email, address, phone, and
     * offline_access.
     * </p>
     */
    public static final String OIDC_MP_RESPONSE_TYPE = "payara.security.oidc.responseType";

    /**
     * Optional. Response Type value defines the processing flow to be used. By
     * default, the value is code (Authorization Code Flow).
     *
     * @return
     */
    String responseType() default "code";

    /**
     * The Microprofile Config key for the responseMode is <code>{@value}</code>
     */
    public static final String OIDC_MP_RESPONSE_MODE = "payara.security.oidc.responseMode";

    /**
     * Optional. Informs the Authorization Server of the mechanism to be used
     * for returning parameters from the Authorization Endpoint.
     *
     * @return
     */
    String responseMode() default "";

    /**
     * The Microprofile Config key for the prompt is <code>{@value}</code>.
     *
     * <p>
     * Value is case sensitive and multiple values must be separated by space
     * delimiter. The defined values are: none, login, consent, select_account.
     * If this parameter contains 'none' with any other value, an error is
     * returned.
     * </p>
     *
     */
    public static final String OIDC_MP_PROMPT = "payara.security.oidc.prompt";

    /**
     * Optional. The prompt value specifies whether the authorization server
     * prompts the user for reauthentication and consent. If no value is
     * specified and the user has not previously authorized access, then the
     * user is shown a consent screen.
     *
     * @return
     */
    PromptType[] prompt() default {};

    /**
     * The Microprofile Config key for the display is <code>{@value}</code>.
     *
     * <p>
     * The defined values are: page, popup, touch, and wap. If the display
     * parameter is not specified then 'page' is the default display mode.
     * </p>
     *
     */
    public static final String OIDC_MP_DISPLAY = "payara.security.oidc.display";

    /**
     * Optional. The display value specifying how the authorization server
     * displays the authentication and consent user interface pages.
     *
     * @return
     */
    DisplayType display() default PAGE;


    /**
     * The Microprofile Config key for the nonce is <code>{@value}</code>.
     */
    public static final String OIDC_MP_NONCE = "payara.security.oidc.useNonce";

    /**
     * Optional. String value used to mitigate replay attacks.
     *
     * @return
     */
    boolean useNonce() default true;

    /**
     * An array of extra options that will be sent to the OAuth provider.
     * <p>
     * These must be in the form of {@code "key=value"} i.e.
     * <code> extraParameters={"key1=value", "key2=value2"} </code>
     *
     * @return
     */
    String[] extraParameters() default {};

    /**
     * The Microprofile Config key for the encryption algorithm is
     * <code>{@value}</code>.
     */
    public static final String OIDC_MP_CLIENT_ENC_ALGORITHM = "payara.security.oidc.client.encryption.algorithm";

    /**
     * The Microprofile Config key for the encryption method is
     * <code>{@value}</code>.
     */
    public static final String OIDC_MP_CLIENT_ENC_METHOD = "payara.security.oidc.client.encryption.method";

    /**
     * The Microprofile Config key for the private key jwks is
     * <code>{@value}</code>.
     */
    public static final String OIDC_MP_CLIENT_ENC_JWKS = "payara.security.oidc.client.encryption.jwks";

}
