/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 * {@link LogoutDefinition} annotation defines logout and RP session management
 * configuration in openid connect client.
 *
 * @author jGauravGupta
 */
@Retention(RUNTIME)
public @interface LogoutDefinition {
    
    /**
     * The Microprofile Config key for the post logout redirect URI is <code>{@value}</code>
     */
    public static final String OPENID_MP_POST_LOGOUT_REDIRECT_URI = "payara.security.openid.logout.redirectURI";
    
    /**
     * Optional. the RP is requesting that the End-User's User Agent be
     * redirected after a logout has been performed. The value MUST have been
     * previously registered with the OP, either using the
     * post_logout_redirect_uris Registration parameter or via another
     * mechanism.
     *
     * To set this using Microprofile Config use
     * {@code payara.security.openid.logout.redirectURI}
     *
     * @return
     */
    String redirectURI() default "";

    /**
     * The Microprofile Config key for session timeout on the expiry of Access
     * Tokens is <code>{@value}</code>.
     */
    public static final String OPENID_MP_LOGOUT_ON_ACCESS_TOKEN_EXPIRY = "payara.security.openid.logout.access.token.expiry";

    /**
     * Session timeout on the expiry of Access Token.
     *
     * @return
     */
    boolean accessTokenExpiry() default false;

    /**
     * The Microprofile Config key for session timeout on the expiry of Identity
     * Tokens is <code>{@value}</code>.
     */
    public static final String OPENID_MP_LOGOUT_ON_IDENTITY_TOKEN_EXPIRY = "payara.security.openid.logout.access.token.expiry";

    /**
     * Session timeout on the expiry of Identity Token.
     *
     * @return
     */
    boolean identityTokenExpiry() default false;

}
