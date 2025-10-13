/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.security.auth.login.common;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;

/**
 * This is the default callback handler provided by the application client container.
 * 
 * <p>
 * The container tries to use the application specified callback handler (if provided). If there is no callback handler
 * or if the handler cannot be instantiated then this default handler is used.
 */
public class ServerLoginCallbackHandler implements CallbackHandler {
    
    private static final String GP_CB = "jakarta.security.auth.message.callback.GroupPrincipalCallback";
    private static final String GPCBH_UTIL = "com.sun.enterprise.security.jaspic.callback.ServerLoginCBHUtil";
    private static final String GPCBH_UTIL_METHOD = "processGroupPrincipal";
    
    private String username;
    private char[] password;
    private String moduleID;

    public ServerLoginCallbackHandler(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public ServerLoginCallbackHandler(String username, char[] password, String moduleID) {
        this.username = username;
        this.password = password;
        this.moduleID = moduleID;
    }

    public ServerLoginCallbackHandler() {
    }

    public void setUsername(String user) {
        username = user;
    }

    public void setPassword(char[] pass) {
        password = pass;
    }

    public void setModuleID(String moduleID) {
        this.moduleID = moduleID;
    }

    /**
     * This is the callback method called when authentication data is required. It either pops up a dialog box to request
     * authentication data or use text input.
     * 
     * @param the callback object instances supported by the login module.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nme = (NameCallback) callback;
                nme.setName(username);
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback pswd = (PasswordCallback) callback;
                pswd.setPassword(password);
            } else if (callback instanceof CertificateRealm.AppContextCallback) {
                ((CertificateRealm.AppContextCallback) callback).setModuleID(moduleID);
            } else if (GP_CB.equals(callback.getClass().getName())) {
                processGroupPrincipal(callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private static void processGroupPrincipal(Callback callback) throws UnsupportedCallbackException {
        try {
            Thread.currentThread()
                  .getContextClassLoader()
                  .loadClass(GPCBH_UTIL)
                  .getMethod(GPCBH_UTIL_METHOD, Callback.class)
                  .invoke(null, callback);
            
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
            throw new UnsupportedCallbackException(callback);
        } 
    }

}
