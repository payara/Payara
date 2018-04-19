/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.identitystores;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 *
 * @author Mark Wareham
 */

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE)
public @interface YubikeyIdentityStoreDefinition {
    
    //TODO is a realm needed?
    
    String yubikeyAPIClientID();    

    String yubikeyAPIKey();
    
    String serverType();
    
    String serverEnpoint();  
}
