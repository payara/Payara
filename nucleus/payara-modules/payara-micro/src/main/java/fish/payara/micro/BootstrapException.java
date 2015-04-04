/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.micro;

/**
 *
 * @author steve
 */
public class BootstrapException extends Exception {
    
    public BootstrapException(String message, Throwable t) {
        super(message,t);
    }
    
}
