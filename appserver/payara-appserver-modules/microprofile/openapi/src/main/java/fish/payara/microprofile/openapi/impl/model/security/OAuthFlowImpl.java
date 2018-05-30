/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.openapi.impl.model.security;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.Scopes;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class OAuthFlowImpl extends ExtensibleImpl implements OAuthFlow {

    protected String authorizationUrl;
    protected String tokenUrl;
    protected String refreshUrl;
    protected Scopes scopes;

    @Override
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    @Override
    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public OAuthFlow authorizationUrl(String authorizationUrl) {
        setAuthorizationUrl(authorizationUrl);
        return this;
    }

    @Override
    public String getTokenUrl() {
        return tokenUrl;
    }

    @Override
    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    @Override
    public OAuthFlow tokenUrl(String tokenUrl) {
        setTokenUrl(tokenUrl);
        return this;
    }

    @Override
    public String getRefreshUrl() {
        return refreshUrl;
    }

    @Override
    public void setRefreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
    }

    @Override
    public OAuthFlow refreshUrl(String refreshUrl) {
        setRefreshUrl(refreshUrl);
        return this;
    }

    @Override
    public Scopes getScopes() {
        return scopes;
    }

    @Override
    public void setScopes(Scopes scopes) {
        this.scopes = scopes;
    }

    @Override
    public OAuthFlow scopes(Scopes scopes) {
        setScopes(scopes);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.security.OAuthFlow from, OAuthFlow to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setTokenUrl(mergeProperty(to.getTokenUrl(), from.tokenUrl(), override));
        if (from.scopes() != null) {
            Scopes scopes = new ScopesImpl();
            for (OAuthScope scope : from.scopes()) {
                scopes.addScope(scope.name(), scope.description());
            }
            to.setScopes(mergeProperty(to.getScopes(), scopes, override));
        }
        to.setRefreshUrl(mergeProperty(to.getRefreshUrl(), from.refreshUrl(), override));
        to.setAuthorizationUrl(mergeProperty(to.getAuthorizationUrl(), from.authorizationUrl(), override));
    }

}
