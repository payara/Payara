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

import fish.payara.security.oauth2.api.OAuth2State;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import javax.enterprise.context.SessionScoped;

/**
 * Class to hold state of OAuth2
 *
 * @author jonathan
 * @since 4.1.2.172
 */
//@SessionScoped
public class OAuth2StateHolder implements OAuth2State {

    /**
     * A random string used to ensure the return value from the remote endpoint is correct and prevent CSRF.
     */
    private String state;
    private String token;
    private String bearer;
    private Optional<String> scope;
    private Optional<String> refreshToken;
    private Optional<String> expiresIn;

    public OAuth2StateHolder() {
        state = UUID.randomUUID().toString();
        scope = Optional.empty();
        refreshToken = Optional.empty();
        expiresIn = Optional.empty();
    }

    public OAuth2StateHolder(String state) {
        this.state = state;
        scope = Optional.empty();
        refreshToken = Optional.empty();
        expiresIn = Optional.empty();
    }

    @Override
    public String getBearer() {
        return bearer;
    }

    public void setBearer(String bearer) {
        this.bearer = bearer;
    }

    @Override
    public Optional<String> getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = Optional.of(scope);
    }

    @Override
    public Optional<String> getRefreshToken() {
        return refreshToken;
    }

    @Override
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = Optional.of(refreshToken);
    }

    @Override
    public Optional<String> getExpiresIn() {
        return expiresIn;
    }

    @Override
    public void setExpiresIn(String expiresIn) {
        this.expiresIn = Optional.of(expiresIn);
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getToken() {
        return token;
    }

}
