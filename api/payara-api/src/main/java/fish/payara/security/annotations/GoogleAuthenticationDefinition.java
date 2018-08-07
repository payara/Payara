/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.annotations;

import fish.payara.security.openid.api.DisplayType;
import static fish.payara.security.openid.api.DisplayType.PAGE;
import static fish.payara.security.openid.api.OpenIdConstant.EMAIL_SCOPE;
import static fish.payara.security.openid.api.OpenIdConstant.OPENID_SCOPE;
import static fish.payara.security.openid.api.OpenIdConstant.PROFILE_SCOPE;
import fish.payara.security.openid.api.PromptType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author jGauravGupta
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GoogleAuthenticationDefinition {

    /**
     * Optional. The provider uri (
     * http://openid.net/specs/openid-connect-discovery-1_0.html ) to read /
     * discover the metadata of the openid provider.
     *
     * @return
     */
    String providerURI() default "https://accounts.google.com";

    /**
     * To override the openid connect provider's metadata property discovered
     * via providerUri.
     *
     * @return
     */
    OpenIdProviderMetadata providerMetadata() default @OpenIdProviderMetadata;

    /**
     * Required. The client identifier issued when the application was
     * registered
     * <p>
     * To set this using Microprofile Config use
     * {@code payara.security.openid.cliendId}
     *
     * @return the client identifier
     */
    String clientId() default "";

    /**
     * Required. The client secret
     * <p>
     * It is recommended to set this using an alias.
     * </p>
     * To set this using Microprofile Config use
     * {@code payara.security.openid.clientSecret}
     *
     * @return
     * @see
     * <a href="https://docs.payara.fish/documentation/payara-server/password-aliases/password-aliases-overview.html">Payara
     * Password Aliases Documentation</a>
     */
    String clientSecret() default "";

    /**
     * The redirect URI to which the response will be sent by OpenId Connect
     * Provider. This URI must exactly match one of the Redirection URI values
     * for the Client pre-registered at the OpenID Provider.
     *
     * To set this using Microprofile Config use
     * {@code payara.security.openid.redirectURI}
     *
     * @return
     */
    String redirectURI() default "";

    /**
     * Optional. The scope value defines the access privileges. The basic (and
     * required) scope for OpenID Connect is the openid scope.
     *
     * @return
     */
    String[] scope() default {OPENID_SCOPE, EMAIL_SCOPE, PROFILE_SCOPE};

    /**
     * Optional. Response Type value defines the processing flow to be used. By
     * default, the value is code (Authorization Code Flow).
     *
     * @return
     */
    String responseType() default "code";

    /**
     * Optional. Informs the Authorization Server of the mechanism to be used
     * for returning parameters from the Authorization Endpoint.
     *
     * @return
     */
    String responseMode() default "";

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
     * Optional. The display value specifying how the authorization server
     * displays the authentication and consent user interface pages.
     *
     * @return
     */
    DisplayType display() default PAGE;

    /**
     * Optional. Enables string value used to mitigate replay attacks.
     *
     * @return
     */
    boolean useNonce() default true;

    /**
     * Optional. If enabled state & nonce value stored in session otherwise in
     * cookies.
     *
     * @return
     */
    boolean useSession() default true;

    /**
     * An array of extra options that will be sent to the OAuth provider.
     * <p>
     * These must be in the form of {@code "key=value"} i.e.
     * <code> extraParameters={"key1=value", "key2=value2"} </code>
     *
     * @return
     */
    String[] extraParameters() default {};
}
