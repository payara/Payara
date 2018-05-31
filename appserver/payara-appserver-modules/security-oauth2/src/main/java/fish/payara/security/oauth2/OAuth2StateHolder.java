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
package fish.payara.security.oauth2;

import fish.payara.security.oauth2.api.OAuth2AccessToken;
import java.time.Instant;
import java.util.Optional;

/**
 * Class to hold state of OAuth2 token as returned by the provider
 *
 * @author jonathan
 * @since 4.1.2.172
 */
//@SessionScoped
public class OAuth2StateHolder implements OAuth2AccessToken {

    /**
     * A random string used to ensure the return value from the remote endpoint is correct and prevent CSRF.
     */
    private String token;
    private Optional<String> scope;
    private Optional<String> refreshToken;
    private Optional<Integer> expiresIn;
    private Instant timeset;

    public OAuth2StateHolder() {
        scope = Optional.empty();
        refreshToken = Optional.empty();
        expiresIn = Optional.empty();
    }

    @Override
    public Optional<String> getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = Optional.ofNullable(scope);
    }

    @Override
    public Optional<String> getRefreshToken() {
        return refreshToken;
    }

    @Override
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = Optional.ofNullable(refreshToken);
    }

    @Override
    public Optional<Integer> getExpiresIn() {
        return expiresIn;
    }

    @Override
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = Optional.ofNullable(expiresIn);
    }
    
    @Override
    public String getAccessToken() {
        return token;
    }

    @Override
    public void setAccessToken(String token) {
        this.token = token;
        timeset = Instant.now();
    }

    @Override
    public Instant getTimeSet() {
        return timeset;
    }

}
