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
 * This interface stores the status of the authentication.
 * @author Harish Prabandham
 */

public interface AuthenticationStatus extends java.io.Serializable {
    public static final int AUTH_SUCCESS = 0; // Authentication Successful
    public static final int AUTH_FAILURE = 1; // Authentication Failed
    public static final int AUTH_CONTINUE = 2; // Continue the Authentication
    public static final int AUTH_EXPIRED = 3; // Credentials have expired.

    /**
     * This method returns the status of the authentication
     * @return An integer value indicating the status of the authentication
     */
    public int  getStatus();
    
    /**
     * This is the value returned by the Authenticator when the status
     * is AUTH_CONTINUE. This data should give an indication to the 
     * client on what else it should send to the server to complete the
     * authentication.
     * 
     * @return An array of bytes indicating the additional information
     * needed to complete the authentication.
     */
    public byte[] getContinuationData();
    
    /**
     * This is the value returned by the Authenticator when the status
     * is AUTH_CONTINUE. This data should give an indication to the 
     * client on specific authentication it needs to adopt to continue
     * on with the authentication.
     * 
     * @return An array of bytes indicating the authentication specific
     * information needed to complete the authentication.
     */
    public byte[] getAuthSpecificData();
}




