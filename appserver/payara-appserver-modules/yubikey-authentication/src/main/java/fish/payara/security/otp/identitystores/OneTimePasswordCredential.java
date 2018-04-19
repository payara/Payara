/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.otp.identitystores;

import javax.security.enterprise.credential.Credential;

/**
 *
 * @author Mark Wareham
 */
public class OneTimePasswordCredential implements Credential{
    
    private String oneTimePasswordString;

    public OneTimePasswordCredential(String oneTimePasswordString) {
        this.oneTimePasswordString = oneTimePasswordString;
    }
    
    public String getOneTimePasswordString() {
        return oneTimePasswordString;
    }

    public void clearCredential (){
        oneTimePasswordString=null;
    }
    
}
