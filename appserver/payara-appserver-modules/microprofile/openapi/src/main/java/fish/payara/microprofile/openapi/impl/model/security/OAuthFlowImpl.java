/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class OAuthFlowImpl extends ExtensibleImpl<OAuthFlow> implements OAuthFlow {

    private String authorizationUrl;
    private String tokenUrl;
    private String refreshUrl;
    private Map<String, String> scopes = createMap();

    @SuppressWarnings("unchecked")
    public static OAuthFlow createInstance(AnnotationModel annotation) {
        OAuthFlow from = new OAuthFlowImpl();
        from.setAuthorizationUrl(annotation.getValue("authorizationUrl", String.class));
        from.setTokenUrl(annotation.getValue("tokenUrl", String.class));
        from.setRefreshUrl(annotation.getValue("refreshUrl", String.class));
        from.setExtensions(parseExtensions(annotation));
        List<AnnotationModel> scopesAnnotation = annotation.getValue("scopes", List.class);
        if (scopesAnnotation != null) {
            Map<String, String> scopes = createMap();
            for (AnnotationModel scopeAnnotation : scopesAnnotation) {
                scopes.put(
                        scopeAnnotation.getValue("name", String.class),
                        scopeAnnotation.getValue("description", String.class)
                );
            }
            from.setScopes(scopes);
        }
        return from;
    }

    @Override
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    @Override
    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
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
    public String getRefreshUrl() {
        return refreshUrl;
    }

    @Override
    public void setRefreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
    }

    @Override
    public Map<String, String> getScopes() {
        return readOnlyView(scopes);
    }

    @Override
    public void setScopes(Map<String, String> scopes) {
        this.scopes = createMap(scopes);
    }

    @Override
    public OAuthFlow addScope(String scope, String description) {
        if (scope != null) {
            if (scopes == null){
                scopes = createMap();
            }
            scopes.put(scope, description);
        }
        return this;
    }

    @Override
    public void removeScope(String scope) {
        if (scopes != null) {
            scopes.remove(scope);
        }
    }

    public static void merge(OAuthFlow from, OAuthFlow to, boolean override) {
        if (from == null) {
            return;
        }
        to.setTokenUrl(mergeProperty(to.getTokenUrl(), from.getTokenUrl(), override));
        if (from.getScopes() != null) {
            to.setScopes(mergeProperty(to.getScopes(), from.getScopes(), override));
        }
        to.setRefreshUrl(mergeProperty(to.getRefreshUrl(), from.getRefreshUrl(), override));
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        to.setAuthorizationUrl(mergeProperty(to.getAuthorizationUrl(), from.getAuthorizationUrl(), override));
    }

}
