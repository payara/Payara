/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security;

import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.common.AppservAccessController;
import java.security.PrivilegedAction;

import com.sun.enterprise.security.auth.login.ClientPasswordLoginModule;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.common.SecurityConstants;
import java.util.logging.*;
import com.sun.logging.*;

/** 
 * This class is kept for CTS. Ideally we should move away from it.
 * The login can be done via the following call:
 * <pre>
 * // Initialize the ORB.
 * try {
 *   LoginContext lc = new LoginContext();
 *   lc.login("john", "john123");
 * } catch (LoginException le) {
 *   le.printStackTrace();
 * }
 *
 *
 * </PRE>
 *
 * Ideally the login should be done with the system property -Dj2eelogin.name and -Dj2eelogin.password
 *
 * @author Harpreet Singh (hsingh@eng.sun.com)
 */

public final class LoginContext {
    
    private static Logger _logger=null;
    static{
       _logger = SecurityLoggerInfo.getLogger();
   }

    private boolean guiAuth = false;

    // declaring this different from the Appcontainer as 
    // this will be called from standalone clients.
    public javax.security.auth.callback.CallbackHandler handler = null;
    
    /**
     * Creates the LoginContext with the defauly callback handler
     */
    public LoginContext () {
	handler = new com.sun.enterprise.security.auth.login.LoginCallbackHandler(guiAuth);
    }
    
    /** 
     * Login method to login username and password
     */
    public void login(String user, String pass) throws LoginException{
	final String username = user;
	final String password = pass;
	AppservAccessController.doPrivileged(new PrivilegedAction() {
	    public java.lang.Object run() {
		
		System.setProperty(ClientPasswordLoginModule.LOGIN_NAME,
				   username);
		System.setProperty(ClientPasswordLoginModule.LOGIN_PASSWORD, 
				   password);

		    return null;
		}
	    });
	// Since this is  a private api and the user is not supposed to use
	// this. We use the default the LoginCallbackHandler.
	LoginContextDriver.doClientLogin(SecurityConstants.USERNAME_PASSWORD,handler);
    }
    
    /** This method has been provided to satisfy the CTS Porting Package 
     * requirement for logging in a certificate
     */
    public void login(String username, byte[] authData) 
	throws LoginException{
    
	    // do nothing
    }

}



