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

import java.util.EnumSet;
import java.util.Set;
import javax.security.enterprise.credential.RememberMeCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import static javax.security.enterprise.identitystore.IdentityStore.ValidationType.VALIDATE;

/**
 * This is a basic identity store that will always validate as a valid result. This class does not add the user
 * to any security groups.
 * <p>
 * This identity store validates a {@link RememberMeCredential} and presumes that the the token passes to the
 * credential upon creation is a valid one and does no further validation. If further validation is required
 * then it may be useful to send a JAX-RS request to the OAuth provider.
 * <p>
 * If an {@link fish.payara.security.oauth2.annotation.OAuth2AuthenticationDefinition} is declared and their are
 * no other {@link IdentityStore} definitions implemented then this will be used as a fall-back.
 * If other {@link IdentityStore} definitions are available but none validate {@link RememberMeCredential} then 
 * {@link CredentialValidationResult.NOT_VALIDATED_RESULT} will be returned to the authentication mechanism.
 * 
 * @author jonathan coustick
 * @since 4.1.2.182
 */
public class OAuthIdentityStore implements IdentityStore {
    
    /**
     * Returns a valid {@link CredentialValidationResult}.
     * <p>
     * If further validation is required this method should be overridden in a sub-class
     * or alternative {@link IdentityStore}. Calling {@link RememberMeCredential#getToken()}
     * on the credential passed in will get the authorisation token which can be used to get
     * more information about the user from the OAuth provider by sending a GET request to
     * an endpoint i.e. https://oauthprovider/user&token=exampletoken.
     * @param credential
     * @return 
     */
    public CredentialValidationResult validate(RememberMeCredential credential){
        return new CredentialValidationResult(credential.toString());
    }
    
    @Override
    public int priority(){
        return 200;
    }
    
    
    @Override
    public Set<ValidationType> validationTypes() {
        return EnumSet.of(VALIDATE);
    }
}
