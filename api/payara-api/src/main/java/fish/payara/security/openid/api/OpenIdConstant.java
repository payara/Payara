/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.security.openid.api;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import java.util.List;

/**
 * Contains constant specific to OpenId Connect specification
 * http://openid.net/specs/openid-connect-core-1_0.html
 *
 * @author Gaurav Gupta
 */
public interface OpenIdConstant {

    // Authorization Code request/response parameters
    public static final String RESPONSE_TYPE = "response_type";
    public static final String CLIENT_ID = "client_id";
    public static final String SCOPE = "scope";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String RESPONSE_MODE = "response_mode";
    public static final String RESOURCE = "resource";
    public static final String STATE = "state";
    public static final String NONCE = "nonce";
    public static final String DISPLAY = "display";
    public static final String PROMPT = "prompt";
    public static final String MAX_AGE = "max_age";
    public static final String UI_LOCALES = "ui_locales";
    public static final String CLAIMS_LOCALES = "claims_locales";
    public static final String ID_TOKEN_HINT = "id_token_hint";
    public static final String LOGIN_HINT = "login_hint";
    public static final String ACR_VALUES = "acr_values";
    public static final String CODE = "code";
    public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

    // Access Token request/response parameters
    public static final String GRANT_TYPE = "grant_type";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String IDENTITY_TOKEN = "id_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String EXPIRES_IN = "expires_in";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String ERROR_PARAM = "error";
    public static final String ERROR_DESCRIPTION_PARAM = "error_description";

    //claims
    public static final String ISSUER_IDENTIFIER = "iss";
    public static final String SUBJECT_IDENTIFIER = "sub";
    public static final String EXPIRATION_IDENTIFIER = "exp";
    public static final String AUDIENCE = "aud";
    public static final String AUTHORIZED_PARTY = "azp";
    public static final String ACCESS_TOKEN_HASH = "at_hash";

    // OpenID Provider Metadata
    public static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String TOKEN_ENDPOINT = "token_endpoint";
    public static final String USERINFO_ENDPOINT = "userinfo_endpoint";
    public static final String END_SESSION_ENDPOINT = "end_session_endpoint";
    public static final String REGISTRATION_ENDPOINT = "registration_endpoint";
    public static final String JWKS_URI = "jwks_uri";
    
    public static final String ISSUER = "issuer";
    public static final String SCOPES_SUPPORTED = "scopes_supported";
    public static final String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED = "id_token_encryption_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED = "id_token_encryption_enc_values_supported";
    public static final String RESPONSE_TYPES_SUPPORTED = "response_types_supported";
    public static final String RESPONSE_MODES_SUPPORTED = "response_modes_supported";
    public static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported";
    public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED = "token_endpoint_auth_signing_alg_values_supported";
    public static final String DISPLAY_VALUES_SUPPORTED = "display_values_supported";
    public static final String CLAIMS_SUPPORTED = "claims_supported";
    public static final String CLAIM_TYPES_SUPPORTED = "claim_types_supported";
    public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";

    public static final List<String> AUTHORIZATION_CODE_FLOW_TYPES
            = unmodifiableList(asList(
                    "code"
            ));
    public static final List<String> IMPLICIT_FLOW_TYPES
            = unmodifiableList(asList(
                    "id_token",
                    "id_token token"
            ));
    public static final List<String> HYBRID_FLOW_TYPES
            = unmodifiableList(asList(
                    "code id_token",
                    "code token",
                    "code id_token token"
            ));

    // Scopes
    public static final String OPENID_SCOPE = "openid"; //required
    public static final String PROFILE_SCOPE = "profile";
    public static final String EMAIL_SCOPE = "email";
    public static final String PHONE_SCOPE = "phone";
    public static final String OFFLINE_ACCESS_SCOPE = "offline_access";

    // profile scope claims
    public static final String NAME = "name";
    public static final String FAMILY_NAME = "family_name";
    public static final String GIVEN_NAME = "given_name";
    public static final String MIDDLE_NAME = "middle_name";
    public static final String NICKNAME = "nickname";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String GROUPS = "groups";
    public static final String PROFILE = "profile";
    public static final String PICTURE = "picture";
    public static final String WEBSITE = "website";
    public static final String GENDER = "gender";
    public static final String BIRTHDATE = "birthdate";
    public static final String ZONEINFO = "zoneinfo";
    public static final String LOCALE = "locale";
    public static final String UPDATED_AT = "updated_at";

    // email scope claims
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email_verified";

    // address scope claims
    public static final String ADDRESS = "address";
    
    // phone scope claims
    public static final String PHONE_NUMBER = "phone_number";
    public static final String PHONE_NUMBER_VERIFIED = "phone_number_verified";

    // Original user Request
    public static final String ORIGINAL_REQUEST = "oidc.original.request";
}
