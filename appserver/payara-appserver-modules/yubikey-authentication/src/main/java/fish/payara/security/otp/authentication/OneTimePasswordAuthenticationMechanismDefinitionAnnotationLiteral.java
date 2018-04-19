/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.authentication;

import javax.enterprise.util.AnnotationLiteral;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

/**
 *
 * @author Mark Wareham
 */
public class OneTimePasswordAuthenticationMechanismDefinitionAnnotationLiteral 
        extends AnnotationLiteral<OneTimePasswordAuthenticationMechanismDefinition> 
        implements OneTimePasswordAuthenticationMechanismDefinition {
    
    private final LoginToContinue loginToContinue;
    
    public OneTimePasswordAuthenticationMechanismDefinitionAnnotationLiteral(LoginToContinue loginToContinue) {
        System.out.println("OneTimePasswordAuthenticationMechanismDefinitionAnnotationLiteral.<init>()");
        this.loginToContinue = loginToContinue;
    }
    
    @Override
    public LoginToContinue loginToContinue() {
        return loginToContinue;
    }
    
}
