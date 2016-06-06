/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.security.jaspic;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessagePolicy.TargetPolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author steve
 */
class SimpleSAMAuthContext implements ServerAuthContext {

    ServerAuthModule sam;
    CallbackHandler handler;
    Map options;

    SimpleSAMAuthContext(String authContextID, Subject serviceSubject, Map properties,CallbackHandler handler, ServerAuthModule sam) throws AuthException {
        this.sam = sam;
        this.handler = handler;
        this.options = properties;
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        MessagePolicy requestPolicy =
                    new MessagePolicy(new MessagePolicy.TargetPolicy[]{
                        new MessagePolicy.TargetPolicy((MessagePolicy.Target[]) null,
                        new MessagePolicy.ProtectionPolicy() {

                            public String getID() {
                                return MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;
                            }
                        })}, true);
        sam.initialize(requestPolicy, null, handler, options);
        return sam.validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return sam.secureResponse(messageInfo, serviceSubject);
     }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        sam.cleanSubject(messageInfo, subject);
    }
    
}
