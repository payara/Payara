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

import java.util.Arrays;
import java.util.Map;

/**
 * OIDC client configuration
 * 
 * @author Gaurav Gupta
 */
public class OidcConfiguration {

    private String clientID;
    private char[] clientSecret;
    private String redirectURI;
    private String scopes;
    private String responseType;
    private String responseMode;
    private Map<String, String> extraParameters;
    private String prompt;
    private String display;
    private boolean useNonce;
    private OidcProviderMetadata providerMetadata;
    private OidcTokenEncryptionMetadata encryptionMetadata;

    public String getClientID() {
        return clientID;
    }

    public OidcConfiguration setClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    public char[] getClientSecret() {
        return clientSecret;
    }

    public OidcConfiguration setClientSecret(char[] clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    public OidcConfiguration setRedirectURI(String redirectURI) {
        this.redirectURI = redirectURI;
        return this;
    }

    public String getScopes() {
        return scopes;
    }

    public OidcConfiguration setScopes(String scopes) {
        this.scopes = scopes;
        return this;
    }

    public String getResponseType() {
        return responseType;
    }

    public OidcConfiguration setResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public OidcConfiguration setResponseMode(String responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    public Map<String, String> getExtraParameters() {
        return extraParameters;
    }

    public OidcConfiguration setExtraParameters(Map<String, String> extraParameters) {
        this.extraParameters = extraParameters;
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public OidcConfiguration setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public String getDisplay() {
        return display;
    }

    public OidcConfiguration setDisplay(String display) {
        this.display = display;
        return this;
    }

    public boolean isUseNonce() {
        return useNonce;
    }

    public OidcConfiguration setUseNonce(boolean useNonce) {
        this.useNonce = useNonce;
        return this;
    }

    public OidcProviderMetadata getProviderMetadata() {
        return providerMetadata;
    }

    public OidcConfiguration setProviderMetadata(OidcProviderMetadata providerMetadata) {
        this.providerMetadata = providerMetadata;
        return this;
    }

    public OidcTokenEncryptionMetadata getEncryptionMetadata() {
        return encryptionMetadata;
    }

    public OidcConfiguration setEncryptionMetadata(OidcTokenEncryptionMetadata encryptionMetadata) {
        this.encryptionMetadata = encryptionMetadata;
        return this;
    }

    @Override
    public String toString() {
        return OidcConfiguration.class.getSimpleName()
                + "{"
                + "clientID=" + clientID
                + ", clientSecret=" + Arrays.toString(clientSecret)
                + ", redirectURI=" + redirectURI
                + ", scopes=" + scopes
                + ", responseType=" + responseType
                + ", responseMode=" + responseMode
                + ", extraParameters=" + extraParameters
                + ", prompt=" + prompt
                + ", display=" + display
                + ", useNonce=" + useNonce
                + ", providerMetadata=" + providerMetadata
                + ", encryptionMetadata=" + encryptionMetadata
                + '}';
    }


}
