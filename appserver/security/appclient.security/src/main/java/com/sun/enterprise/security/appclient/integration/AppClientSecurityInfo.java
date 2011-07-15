/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package com.sun.enterprise.security.appclient.integration;

import java.util.List;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import org.glassfish.appclient.client.acc.config.MessageSecurityConfig;
import org.glassfish.appclient.client.acc.config.TargetServer;
import org.jvnet.hk2.annotations.Contract;

/**
 * The Interface usable by AppClient Container for configuring the
 * Security Runtime.
 * 
 * @author Kumar Jayanti
 */
@Contract
public interface AppClientSecurityInfo {
    
    public enum CredentialType { 
        USERNAME_PASSWORD, CERTIFICATE, ALL
    };

    
    /**
     * Initialize Security Runtime for the AppContainerr (Stores, SecurityManager, JSR 196 etc)
     * 
     * @param container the Appclient Configuration Object
     * @param handler the CallbackHandler
     * @param appclientCredType The CredentialType of the Appclient
     * @param username the static username if any was configured
     * @param password the static password if any was configured
     * @Param isJWS set to true if it is Java WebStart client
     * @Param useGUIAuth flag when set to true indicates the use of GUI Authentication
     */
    public void initializeSecurity(
            List<TargetServer> tServers,
            List<MessageSecurityConfig> msgSecConfigs,
            CallbackHandler handler,
            CredentialType appclientCredType,
            String username, char[] password,
            boolean isJWS, boolean useGUIAuth);

    
    /**
     * @param type the credential type
     * @return the integer encoding for this type
     */
    public int getCredentialEncoding(CredentialType type);
   
    /**
     * Do a client login using the CredentialType
     * @param credType
     * @return
     */
    public Subject  doClientLogin(CredentialType credType);

    /**
     * Clears the Client's current Security Context.
     */
    public void clearClientSecurityContext();

    /**
     * Check if the Login attempt was cancelled.
     * @return boolean indicating whether the login attempt was cancelled.
     */
    public boolean isLoginCancelled();
}
