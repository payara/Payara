/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.security.jaspic;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author steve
 */
class SimpleSAMAuthContext implements ServerAuthContext {

    ServerAuthModule sam;
    CallbackHandler handler;
    Map<String,String> options;

    SimpleSAMAuthContext(String authContextID, Subject serviceSubject, Map<String,String> properties, CallbackHandler handler, ServerAuthModule sam) throws AuthException {
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
