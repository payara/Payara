/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */

package fish.payara.security.otp.identitystores;

import com.yubico.client.v2.ResponseStatus;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import static javax.security.enterprise.identitystore.IdentityStore.ValidationType.VALIDATE;

/**
 *
 * @author Mark Wareham
 */
public class YubikeyIdentityStore implements IdentityStore {

    private static final Logger LOG = Logger.getLogger(YubikeyIdentityStore.class.getName());
    
    private final YubicoClient yubicoClient;
    private final YubikeyIdentityStoreDefinition definition;
    
    public YubikeyIdentityStore(YubikeyIdentityStoreDefinition definition) {
        this.definition = definition;
        yubicoClient = YubicoClientFactory.getYubicoClient(this.definition);
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (!(credential instanceof YubikeyCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        YubikeyCredential oneTimePasswordCredential = ((YubikeyCredential) credential);
        try {
            String oneTimePassword = oneTimePasswordCredential.getOneTimePasswordString();
            
            if (!YubicoClient.isValidOTPFormat(oneTimePassword)) {
                return CredentialValidationResult.INVALID_RESULT;
            }
            ResponseStatus verificationStatus = yubicoClient.verify(oneTimePassword).getStatus();
            LOG.log(Level.FINE, "Yubico server reported {1}", verificationStatus.name());

            switch (verificationStatus) {
                case BAD_OTP:
                case REPLAYED_OTP:
                case BAD_SIGNATURE:
                case NO_SUCH_CLIENT:
                    return CredentialValidationResult.INVALID_RESULT;
                case MISSING_PARAMETER:
                case OPERATION_NOT_ALLOWED:
                case BACKEND_ERROR:
                case NOT_ENOUGH_ANSWERS:
                case REPLAYED_REQUEST:
                    LOG.log(Level.WARNING, "Yubico reported {0}",
                            verificationStatus.name());
                    return CredentialValidationResult.NOT_VALIDATED_RESULT;
                case OK:
                    break;//carry on.
                default:
                    LOG.log(Level.SEVERE, "Unknown/new yubico return status");
            }

            
            CredentialValidationResult credentialValidationResult = new CredentialValidationResult(
                    oneTimePasswordCredential.getPublicID());
            
            return credentialValidationResult;
        
        } catch (YubicoVerificationException | YubicoValidationFailure ex) {
            LOG.log(Level.SEVERE, null, ex);
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
    }
    
    @Override
    public Set<ValidationType> validationTypes() {
        return EnumSet.of(VALIDATE);
        //This IdentityStore does not provide groups.
    }

    @Override
    public int priority() {
        return definition.priority();
    }
}
