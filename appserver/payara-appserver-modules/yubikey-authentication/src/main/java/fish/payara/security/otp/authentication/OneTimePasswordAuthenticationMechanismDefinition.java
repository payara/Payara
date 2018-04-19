/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

/**
 *
 * @author Mark Wareham
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OneTimePasswordAuthenticationMechanismDefinition {
    
    @Nonbinding
    LoginToContinue loginToContinue();
    
}
