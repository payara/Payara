/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.identitystores;

import com.yubico.client.v2.ResponseStatus;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

/**
 *
 * @author Mark Wareham
 */
public class YubikeyIdentityStore implements IdentityStore{
    
    private static final Logger LOG = Logger.getLogger(YubikeyIdentityStore.class.getName());
    
    private final YubicoClient yubicoClient;
    private final YubikeyIdentityStoreDefinition definition;

    public YubikeyIdentityStore(YubikeyIdentityStoreDefinition definition) {
        this.definition = definition;
        yubicoClient = YubicoClientFactory.getYubicoClient(this.definition);
    }
    
    @Override
    public CredentialValidationResult validate(Credential credential) {
        if(credential instanceof OneTimePasswordCredential){
            try {
                String oneTimePassword = ((OneTimePasswordCredential)credential).getOneTimePasswordString();
                if (!YubicoClient.isValidOTPFormat(oneTimePassword)) {
                    return CredentialValidationResult.INVALID_RESULT;
                }
                ResponseStatus verificationStatus = yubicoClient.verify(oneTimePassword).getStatus();
                switch (verificationStatus){
                    case BAD_OTP :
                    case REPLAYED_OTP :
                    case BAD_SIGNATURE :
                    case NO_SUCH_CLIENT :
                        LOG.log(Level.INFO, "Yubico reported {0}", verificationStatus.name());
                        return CredentialValidationResult.INVALID_RESULT;
                    case MISSING_PARAMETER :
                    case OPERATION_NOT_ALLOWED :
                    case BACKEND_ERROR :
                    case NOT_ENOUGH_ANSWERS :
                    case REPLAYED_REQUEST :
                        LOG.log(Level.WARNING, "Yubico reported {0}", verificationStatus.name());
                        return CredentialValidationResult.NOT_VALIDATED_RESULT;
                    case OK :
                        //carry on.
                }
                Set<String> groups = new HashSet<String>();
                groups.add("yubikeyrole");
                return new CredentialValidationResult(oneTimePassword, groups);
            } catch (YubicoVerificationException | YubicoValidationFailure ex) {
                LOG.log(Level.SEVERE, null, ex);
                return CredentialValidationResult.NOT_VALIDATED_RESULT;
            }
        }else{
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
    }
}
