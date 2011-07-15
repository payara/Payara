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

package javax.security.auth.message;

import java.util.Map;

/**
 * The AuthStatus class is used to represent return values from Authentication
 * modules and Authentication Contexts. An AuthStatus value is returned when 
 * the module processing has established a corresponding request or response 
 * message within the message parameters exchanged with the runtime.
 *
 * @version %I%, %G%
 * @see Map
 */

public class AuthStatus {

    /**
     * Indicates that the message processing by the authentication module 
     * was successful and that the runtime is to proceed with its normal 
     * processing of the resulting message.
     */
    public static final AuthStatus SUCCESS = new AuthStatus(1);
    
    /**
     * Indicates that the message processing by the authentication module 
     * was NOT successful, and that the module replaced the application
     * message with an error message.
     */
    public static final AuthStatus FAILURE = new AuthStatus(2);

    /**
     * Indicates that the message processing by the authentication module 
     * was successful and that the runtime is to proceed by sending 
     * a message returned by the authentication module.
     */
    public static final AuthStatus SEND_SUCCESS = new AuthStatus(3);
    
    /**
     * Indicates that the message processing by the authentication module 
     * was NOT successful, that the module replaced the application
     * message with an error message, and that the runtime is to proceed
     * by sending the error message.
     */
    public static final AuthStatus SEND_FAILURE = new AuthStatus(4);

    /**
     * Indicates the message processing by the authentication module 
     * is NOT complete, that the module replaced the application
     * message with a security message, and that the runtime is to proceed
     * by sending the security message.
     */
    public static final AuthStatus SEND_CONTINUE = new AuthStatus(5);

    private int v;

    private AuthStatus() {
	v = 1;
    }

    private AuthStatus(int value) {
	v = value;
    }

}


