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
package fish.payara.security.openid.domain;

import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * OpenId Connect client configuration
 *
 * @author Gaurav Gupta
 */
public class OpenIdConfiguration {

    private String clientId;
    private char[] clientSecret;
    private String redirectURI;
    private String scopes;
    private String responseType;
    private String responseMode;
    private Map<String, String> extraParameters;
    private String prompt;
    private String display;
    private boolean useNonce;
    private boolean useSession;
    private int jwksConnectTimeout;
    private int jwksReadTimeout;
    private OpenIdProviderMetadata providerMetadata;
    private OpenIdTokenEncryptionMetadata encryptionMetadata;
    private ClaimsConfiguration claimsConfiguration;
    private boolean tokenAutoRefresh;
    private int tokenMinValidity;

    private static final String BASE_URL_EXPRESSION = "${baseURL}";

    public String getClientId() {
        return clientId;
    }

    public OpenIdConfiguration setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public char[] getClientSecret() {
        return clientSecret;
    }

    public OpenIdConfiguration setClientSecret(char[] clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public String buildRedirectURI(HttpServletRequest request) {
        if (redirectURI.contains(BASE_URL_EXPRESSION)) {
            String baseURL = request.getRequestURL().substring(0, request.getRequestURL().length() - request.getRequestURI().length())
                    + request.getContextPath();
            return redirectURI.replace(BASE_URL_EXPRESSION, baseURL);
        }
        return redirectURI;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    public OpenIdConfiguration setRedirectURI(String redirectURI) {
        this.redirectURI = redirectURI;
        return this;
    }

    public String getScopes() {
        return scopes;
    }

    public OpenIdConfiguration setScopes(String scopes) {
        this.scopes = scopes;
        return this;
    }

    public String getResponseType() {
        return responseType;
    }

    public OpenIdConfiguration setResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public OpenIdConfiguration setResponseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public Map<String, String> getExtraParameters() {
        return extraParameters;
    }

    public OpenIdConfiguration setExtraParameters(Map<String, String> extraParameters) {
        this.extraParameters = extraParameters;
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public OpenIdConfiguration setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public String getDisplay() {
        return display;
    }

    public OpenIdConfiguration setDisplay(String display) {
        this.display = display;
        return this;
    }

    public boolean isUseNonce() {
        return useNonce;
    }

    public OpenIdConfiguration setUseNonce(boolean useNonce) {
        this.useNonce = useNonce;
        return this;
    }

    public boolean isUseSession() {
        return useSession;
    }

    public int getJwksConnectTimeout() {
        return jwksConnectTimeout;
    }

    public OpenIdConfiguration setJwksConnectTimeout(int jwksConnectTimeout) {
        this.jwksConnectTimeout = jwksConnectTimeout;
        return this;
    }

    public int getJwksReadTimeout() {
        return jwksReadTimeout;
    }

    public OpenIdConfiguration setJwksReadTimeout(int jwksReadTimeout) {
        this.jwksReadTimeout = jwksReadTimeout;
        return this;
    }

    public OpenIdConfiguration setUseSession(boolean useSession) {
        this.useSession = useSession;
        return this;
    }

    public OpenIdProviderMetadata getProviderMetadata() {
        return providerMetadata;
    }

    public OpenIdConfiguration setProviderMetadata(OpenIdProviderMetadata providerMetadata) {
        this.providerMetadata = providerMetadata;
        return this;
    }

    public ClaimsConfiguration getClaimsConfiguration() {
        return claimsConfiguration;
    }

    public OpenIdConfiguration setClaimsConfiguration(ClaimsConfiguration claimsConfiguration) {
        this.claimsConfiguration = claimsConfiguration;
        return this;
    }

    public OpenIdTokenEncryptionMetadata getEncryptionMetadata() {
        return encryptionMetadata;
    }

    public OpenIdConfiguration setEncryptionMetadata(OpenIdTokenEncryptionMetadata encryptionMetadata) {
        this.encryptionMetadata = encryptionMetadata;
        return this;
    }

    public boolean isTokenAutoRefresh() {
        return tokenAutoRefresh;
    }

    public OpenIdConfiguration setTokenAutoRefresh(boolean tokenAutoRefresh) {
        this.tokenAutoRefresh = tokenAutoRefresh;
        return this;
    }

    public int getTokenMinValidity() {
        return tokenMinValidity;
    }

    public OpenIdConfiguration setTokenMinValidity(int tokenMinValidity) {
        this.tokenMinValidity = tokenMinValidity;
        return this;
    }

    @Override
    public String toString() {
        return OpenIdConfiguration.class.getSimpleName()
                + "{"
                + "clientID=" + clientId
                + ", clientSecret=" + Arrays.toString(clientSecret)
                + ", redirectURI=" + redirectURI
                + ", scopes=" + scopes
                + ", responseType=" + responseType
                + ", responseMode=" + responseMode
                + ", extraParameters=" + extraParameters
                + ", prompt=" + prompt
                + ", display=" + display
                + ", useNonce=" + useNonce
                + ", useSession=" + useSession
                + ", providerMetadata=" + providerMetadata
                + ", claimsConfiguration=" + claimsConfiguration
                + ", encryptionMetadata=" + encryptionMetadata
                + ", tokenAutoRefresh=" + tokenAutoRefresh
                + ", tokenMinValidity=" + tokenMinValidity
                + '}';
    }

}
