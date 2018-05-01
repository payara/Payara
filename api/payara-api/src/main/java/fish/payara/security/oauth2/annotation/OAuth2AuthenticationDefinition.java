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
package fish.payara.security.oauth2.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;

import java.lang.annotation.Target;
import javax.validation.constraints.NotNull;

/**
 * This annotation is used for defining an OAuth2 authentication mechanism
 * 
 * @author jonathan coustick
 * @since 4.1.2.182
 */
@Target({TYPE, METHOD})
@Retention(RUNTIME)
public @interface OAuth2AuthenticationDefinition {
    
    /**
     * Required. The URL for the OAuth2 provider to provide authentication
     * <p>
     * This must be a https endpoint
     * @return 
     */
    @NotNull
    String authEndpoint();
    
    /**
     * Required. The URL for the OAuth2 provider to give the authorisation token
     * @return 
     */
    @NotNull
    String tokenEndpoint();
    
    /**
     * Required. The client identifier issued when the application was registered
     * @return the client identifier
     */
    @NotNull
    String clientId();
    
    /**
     * Required. The client secret
     * <p>
     * It is recommended to set this using an alias.
     * @return 
     * @see <a href="https://docs.payara.fish/documentation/payara-server/password-aliases/password-aliases-overview.html">Payara Password Aliases Documentation</a>
     */
    @NotNull
    String clientSecret();
    
    /**
     * The callback URI.
     * <p>
     * If supplied this must be equal to one set in the OAuth2 Authentication provider
     * @return 
     */
    String redirectURI() default "";
    
    /**
     * The scope that will be requested from the OAuth provider
     * @return 
     */
    String scope() default "";
    
    /**
     * An array of extra options that will be sent to the OAuth provider.
     * <p>
     * These must be in the form of {@code "key=value"} i.e.
     * <code> extraParameters={"key1=value", "key2=value2"} </code>
     * @return 
     */
    String[] extraParameters() default {};
}