/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.config.serverbeans;

import java.io.IOException;
import java.security.KeyStoreException;
import org.jvnet.hk2.annotations.Contract;

@Contract()
/**
 * Definition of some utility behavior that needs to be invoked from
 * config classes in admin/config-api but implemented elsewhere (in a module
 * with dependencies that we do not want to add to admin/config-api). 
 * 
 * @author Tim Quinn
 */
public interface SecureAdminHelper {
    
    /**
     * Returns the DN for the given DN or alias value.
     * 
     * @param value the user-specified value
     * @param isAlias whether the value is an alias or the DN itself
     * @return the DN
     */
    public String getDN(String value, boolean isAlias) throws IOException, KeyStoreException;
    
    /**
     * Makes sure that the specified username is an admin user and that the
     * specified password alias exists. Note that implementations of this 
     * method should not make sure that the username and the password pointed
     * to by the alias actually match a valid admin user in the admin realm. That
     * check is done by the normal authorization logic when the username and
     * the actual password are used.
     * 
     * @param username
     * @param passwordAlias 
     * @throws Exception if eiher the username or the password alias is not valid
     */
    public void validateInternalUsernameAndPasswordAlias(String username, String passwordAlias);
    
    /**
     * Reports whether any admin user exists which has an empty password.
     * 
     * @return true if any admin user exists with an empty password; false otherwise
     * @throws Exception 
     */
    public boolean isAnyAdminUserWithoutPassword() throws Exception;
    
    /**
     * An exception indicating a user-correctable error that occurred as
     * a secure admin command executed.
     * <p>
     * The secure admin commands can detect such errors and report just the
     * exception's message and not the exception as well (which would clutter
     * the report back to the admin client).
     */
    public class SecureAdminCommandException extends RuntimeException {
        
        public SecureAdminCommandException(String message) {
            super(message);
        }
    }
}
