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

package com.sun.enterprise.security.auth.login.common;

import com.sun.enterprise.util.LocalStringManagerImpl;
/**
 * LoginException is thrown by the LoginContext class whenever
 * the following happens: <UL>
 * <LI> If the client is unable to authenticate successfully with the 
 * </UL>
 * @see com.sun.enterprise.security.auth.AuthenticationStatus
 * @author Harish Prabandham
 * @author Harpreet Singh
 */

public class LoginException extends SecurityException {

    private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(LoginException.class);

    private boolean status = false;


    /**
     * Create a new LoginException object with the given message
     * @param The message indicating why authentication failed.
     */
    public LoginException(String message) {
	super(message);
    }

    
    /**
     * Create a new LoginException object with the given authentication
     * value.
     * @param The AuthenticationStatus object
     */
    public LoginException(boolean as){
	super(localStrings.getLocalString("enterprise.security.login_failed", 
					   "Login Failed."));
	status = as;
    }

    
    /**
     * Returns the status of the Authentication.
     */
    public boolean getStatus(){
	return status;
    }
    
}




