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

package com.sun.enterprise.security.common;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.glassfish.security.common.PrincipalImpl;
import org.jvnet.hk2.annotations.Contract;

/**
 * created to maintain backward compatibility with V2 while avoiding
 * split packages.
 * @author kumar.jayanti
 */
@Contract
public interface AppservPasswordLoginModuleInterface extends LoginModule {

    
    /**
     *
     * <P>This is a convenience method which can be used by subclasses
     *
     * <P>Note that this method is called after the authentication
     * has succeeded. If authentication failed do not call this method.
     * 
     * Global instance field succeeded is set to true by this method.
     *
     * @param groups String array of group memberships for user (could be
     *     empty). 
     */
    public  void commitUserAuthentication (final String[] groups);
    

    /**
     * @return the subject being authenticated.
     * use case:
     * A custom login module could overwrite commit() method, and call getSubject()
     * to get subject being authenticated inside its commit(). Custom principal
     * then can be added to subject. By doing this,custom principal will be stored
     * in calling thread's security context and participate in following Appserver's
     * authorization.
     *
     */
    public Subject getSubject();
    
    
    /**
     * Set the Login Module that needs to be used for the AuthenticateUser call
     * @param userDefinedLoginModule the userdefined login module
     */
    public void setLoginModuleForAuthentication(LoginModule userDefinedLoginModule);
    

    /**
     * Meant for extracting the container provided username and password
     * This method is called from the LoginModule before the actual call to login
     * so that the username and password are available during Custom login module's
     * call to authenticateUser()
     */
    public void extractCredentials() throws LoginException;
    
        /**
     * @return the username sent by container - is made available to the custom 
     * login module using the protected _username field.
     * Use Case: A custom login module could use the username to validate against
     * a realm of users
     */
    public String getUsername();
    
    /**
     * @return the password sent by container - is made available to the custom 
     * login module using the protected _password field.
     * Use Case: A custom login module could use the password to validate against
     * a custom realm of usernames and passwords
     */
    public String getPassword();
    
    /**
     * @return the currentRealm - for backward compatability
     */
    public Object getCurrentRealm();
    
    /**
     * @return the succeeded state - for backward compatability
     */
    public boolean isSucceeded();
    
    /**
     * @return the commitsucceeded state - for backward compatability
     */
    public boolean isCommitSucceeded();
    
    
    /**
     * @return the UserPrincipal - for backward compatability
     */
    public PrincipalImpl getUserPrincipal();
    
    /**
     * @return the groupList - for backward compatability
     */
    public String[] getGroupsList();
}
