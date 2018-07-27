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
package fish.payara.security.oidc.domain;

import static fish.payara.security.oidc.api.OidcConstant.ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static fish.payara.security.oidc.api.OidcConstant.ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static fish.payara.security.oidc.api.OidcConstant.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED;
import static fish.payara.security.oidc.api.OidcConstant.ISSUER;
import static fish.payara.security.oidc.api.OidcConstant.RESPONSE_TYPES_SUPPORTED;
import static fish.payara.security.oidc.api.OidcConstant.SCOPES_SUPPORTED;
import static fish.payara.security.oidc.api.OidcConstant.SUBJECT_TYPES_SUPPORTED;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import static javax.json.JsonValue.ValueType.STRING;

/**
 * OIDC provider information
 *
 * @author Gaurav Gupta
 */
public class OidcProviderMetadata {

    private JsonObject document;
    private final String issuerUri;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String jwksUri;
    private final Set<String> scopesSupported;
    private final Set<String> responseTypeSupported;
    private final Set<String> idTokenSigningAlgorithmsSupported;
    private final Set<String> idTokenEncryptionAlgorithmsSupported;
    private final Set<String> idTokenEncryptionMethodsSupported;
    private final Set<String> subjectTypesSupported;

    public OidcProviderMetadata(JsonObject document) {
        this.document = document;
        this.issuerUri = document.getString(ISSUER);
        this.scopesSupported = getValues(SCOPES_SUPPORTED);
        this.responseTypeSupported = getValues(RESPONSE_TYPES_SUPPORTED);
        this.idTokenSigningAlgorithmsSupported = getValues(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
        this.idTokenEncryptionAlgorithmsSupported = getValues(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
        this.idTokenEncryptionMethodsSupported = getValues(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);
        this.subjectTypesSupported = getValues(SUBJECT_TYPES_SUPPORTED);
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public OidcProviderMetadata setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        return this;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public OidcProviderMetadata setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        return this;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public OidcProviderMetadata setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
        return this;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public OidcProviderMetadata setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
        return this;
    }

    public JsonObject getDocument() {
        return document;
    }

    public OidcProviderMetadata setDocument(JsonObject document) {
        this.document = document;
        return this;
    }

    public Set<String> getScopesSupported() {
        return scopesSupported;
    }

    public Set<String> getResponseTypeSupported() {
        return responseTypeSupported;
    }

    public Set<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public Set<String> getIdTokenSigningAlgorithmsSupported() {
        return idTokenSigningAlgorithmsSupported;
    }

    public Set<String> getIdTokenEncryptionAlgorithmsSupported() {
        return idTokenEncryptionAlgorithmsSupported;
    }

    public Set<String> getIdTokenEncryptionMethodsSupported() {
        return idTokenEncryptionMethodsSupported;
    }

    private Set<String> getValues(String key) {
        JsonArray jsonArray = document.getJsonArray(key);
        if (isNull(jsonArray)) {
            return emptySet();
        } else {
            return jsonArray
                    .stream()
                    .filter(element -> element.getValueType() == STRING)
                    .map(element -> (JsonString) element)
                    .map(JsonString::getString)
                    .collect(toSet());
        }
    }

    @Override
    public String toString() {
        return OidcProviderMetadata.class.getSimpleName()
                + "{"
                + "providerUri=" + issuerUri
                + ", authorizationEndpoint=" + authorizationEndpoint
                + ", tokenEndpoint=" + tokenEndpoint
                + ", userinfoEndpoint=" + userinfoEndpoint
                + ", jwksUri=" + jwksUri
                + '}';
    }
    
}
