/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.identitystores;

import com.yubico.client.v2.YubicoClient;

/**
 *
 * @author Mark Wareham
 */
public class YubicoClientFactory {

    
    private static final YubicoClientFactory INSTANCE = new YubicoClientFactory();

    private YubicoClient createYubicoClient(YubikeyIdentityStoreDefinition definition) {
        return YubicoClient.getClient(Integer.valueOf(definition.yubikeyAPIClientID()), 
                definition.yubikeyAPIKey());
    }
    
    public static YubicoClient getYubicoClient(YubikeyIdentityStoreDefinition definition) {
        return INSTANCE.createYubicoClient(definition);
    }

   
}
