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

package com.sun.enterprise.security.auth;

/**
 * This class implements an AuthenticationStatus object.
 * @author Harish Prabandham
 */

public class AuthenticationStatusImpl implements AuthenticationStatus {
    private String realmName; // Name of the Realm
    private String authMethod; // Method used for Authentication.
    private String principalName; // String form of the Principal.
    private int status; // Status
    
    /**
     * This constructs a new AuthenticationStatus object.
     * @param The name of the principal
     * @param The name of the realm that authenticated the principal
     * @param The method used for authenticating the principal
     * @param The status of the authentication
     */
    public AuthenticationStatusImpl(String principalName, String authMethod,
				    String realm,
				    int status) {
	this.principalName = principalName;
	this.authMethod = authMethod;
	this.status = status;
	this.realmName = realm;
    }

    /**
     * This method returns the status of the authentication
     * @return An integer value indicating the status of the authentication
     */
    public int getStatus() {
	return status;
    }

    /** 
     * This method returns a byte array of zero length, since there's
     * no continuation data needed for passphrase based authentication.
     * @return A byte array of zero length.
     */
    public byte[] getContinuationData() {
	return new byte[0];
    }

    /** 
     * This method returns a byte array of zero length, since there's
     * no auth specific data needed for passphrase based authentication.
     * @return A byte array of zero length.
     */
    public byte[] getAuthSpecificData() {
	return new byte[0];
    }

    /** 
     * This method returns the name of realm where the authentication was
     * performed.
     * @return A java.lang.String representation of the realm.
     */
    public String getRealmName() {
	return realmName;
    }
    
    /** 
     * This method returns the "method" used to perform authentication 
     * @return A java.lang.String representation of the method used. In 
     * passphrase based authentication it returns the string "password".
     */
    public String getAuthMethod() {
	return authMethod;
    }

    /** 
     * This method returns the string representation of the principal
     * that was authenticated.
     * @return A java.lang.String representation of the Principal. 
     */
    public String getPrincipalName() {
	return principalName;
    }
}
