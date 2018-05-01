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
package fish.payara.security.oauth2.api;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

/**
 * Interface for the access token response returned by the OAuth provider
 * @author jonathan coustick
 * @since 4.1.2.182
 */
public interface OAuth2AccessToken extends Serializable {
 
    
       /**
     * Gets the authorisation token that was received from the OAuth provider
     *
     * @return
     */
    public String getAccessToken();

    /**
     * Sets the access token that is to be used to verify the user with the OAuth provider once they are logged in.
     * @param token
     */
    public void setAccessToken(String token);

    /**
     * Gets the scope that the user has been given permission for your application to use with the OAuth provider
     *
     * @return
     */
    public Optional<String> getScope();

    /**
     * Returns the refresh token that can be used to get a new access token
     *
     * @return
     */
    public Optional<String> getRefreshToken();

    /**
     * Sets the refresh token that can be used to renew the access token
     * @param refreshToken
     */
    public void setRefreshToken(String refreshToken);

    /**
     * Return the time that the access token is granted for, if it is set to expire
     *
     * @return
     */
    public Optional<Integer> getExpiresIn();

    /**
     * Sets the time that the access token is granted for
     *
     * @param expiresIn
     */
    public void setExpiresIn(Integer expiresIn);

    /**
     * Gets the time that the access token was last set
     *
     * @return
     */
    public Instant getTimeSet();
}
