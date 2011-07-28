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

package com.sun.enterprise.security.auth.login.common;

import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.security.auth.callback.*;


/**
 * This is the default callback handler provided by the application
 * client container. The container tries to use the application specified 
 * callback handler (if provided). If there is no callback handler or if
 * the handler cannot be instantiated then this default handler is used.
 */
public class ServerLoginCallbackHandler implements CallbackHandler 
{
    private static final String GP_CB="javax.security.auth.message.callback.GroupPrincipalCallback";
    private static final String GPCBH_UTIL = "com.sun.enterprise.security.jmac.callback.ServerLoginCBHUtil";
    private static final String GPCBH_UTIL_METHOD="processGroupPrincipal";
    private String username = null;
    private String password = null;
    private String moduleID = null;

    public ServerLoginCallbackHandler(String username, String password) {
	this.username = username;
	this.password = password;
    }

    public ServerLoginCallbackHandler(String username, String password, String moduleID) {
	this.username = username;
	this.password = password;
        this.moduleID = moduleID;
    }
    
    public ServerLoginCallbackHandler(){
    }
    
    public void setUsername(String user){
	username = user;
    }
    
    public void setPassword(String pass){
	password = pass;
    }

    public void setModuleID(String moduleID) {
        this.moduleID = moduleID;
    }

    
    /**
     * This is the callback method called when authentication data is
     * required. It either pops up a dialog box to request authentication
     * data or use text input.
     * @param the callback object instances supported by the login module.
     */
    public void handle(Callback[] callbacks) throws IOException,
					UnsupportedCallbackException
    {
	for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback){
		NameCallback nme = (NameCallback)callbacks[i];
		nme.setName(username);
	    } else if (callbacks[i] instanceof PasswordCallback){
		PasswordCallback pswd = (PasswordCallback)callbacks[i];
		pswd.setPassword(password.toCharArray());
	    } else if (callbacks[i] instanceof CertificateRealm.AppContextCallback){
                ((CertificateRealm.AppContextCallback) callbacks[i]).setModuleID(moduleID);
            } else if (GP_CB.equals(callbacks[i].getClass().getName())){
                processGroupPrincipal(callbacks[i]);
            } else {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
	}
    }

    private static void processGroupPrincipal(Callback callback) throws UnsupportedCallbackException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class clazz = loader.loadClass(GPCBH_UTIL);
            Method meth = clazz.getMethod(GPCBH_UTIL_METHOD, Callback.class);
            meth.invoke(null, callback);
        } catch (IllegalAccessException ex) {
            throw new UnsupportedCallbackException(callback);
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedCallbackException(callback);
        } catch (InvocationTargetException ex) {
            throw new UnsupportedCallbackException(callback);
        } catch (NoSuchMethodException ex) {
            throw new UnsupportedCallbackException(callback);
        } catch (SecurityException ex) {
            throw new UnsupportedCallbackException(callback);
        } catch (ClassNotFoundException ex) {
             throw new UnsupportedCallbackException(callback);
        }

    }

    
}

