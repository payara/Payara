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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author steve
 */
public class SimpleSAMConfig implements ServerAuthConfig {
    
    private final String layer;
    private final String appContext;
    private final CallbackHandler handler;
    private final Map<String,String> constructedProperties;
    private volatile ServerAuthModule sam;
    private Class samClass;


    SimpleSAMConfig(String layer, String appContext, CallbackHandler handler, Map<String,String> properties, Class samClass) {
        this.layer = layer;
        this.appContext = appContext;
        this.handler = handler;
        this.constructedProperties = properties;
        this.samClass = samClass;
        
    }

    @Override
    public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject, Map properties) throws AuthException {
        // combine constructed properties with passed in properties
        if (constructedProperties != null)
            properties.putAll(constructedProperties);

        ServerAuthModule localSam = sam;
        if (localSam == null || properties.containsKey(JASPICWebListenerHelper.SAM_PER_REQUEST_PROPERTY)) {
            try {
                localSam = (ServerAuthModule)samClass.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(SimpleSAMConfig.class.getName()).log(Level.SEVERE, null, ex);
                AuthException ae = new AuthException("Unable to instantiate an instance of the provided SAM class");
                ae.initCause(ex);
                throw ae;
            }
        }
        ServerAuthModule sam = this.sam;     
        if (sam == null) {
            synchronized (this) {
                this.sam = localSam;
            }
        }
        return new SimpleSAMAuthContext(authContextID, serviceSubject, properties, handler, localSam);
    }

    @Override
    public String getMessageLayer() {
        return layer;
    }

    @Override
    public String getAppContext() {
        return appContext;
    }

    @Override
    public String getAuthContextID(MessageInfo messageInfo) {
        return null;
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isProtected() {
        return false;
    }
    
}
