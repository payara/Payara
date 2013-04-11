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

package com.sun.enterprise.security.auth.login;

import com.sun.enterprise.security.BasePasswordLoginModule;
import com.sun.enterprise.security.auth.realm.Realm;
import javax.security.auth.login.LoginException;

/**
 * Abstract base class for password-based login modules.
 * This class is provided for
 * backward compatibility and is a candidate for deprecation.
 *
 */
@Deprecated
public abstract class PasswordLoginModule extends BasePasswordLoginModule
{

    /**
     * Maintain RI compatibility.
     *
     * <P>This is a convenience method which can be used by subclasses
     * to complete the steps required by RI legacy authentication code.
     * Most of this should go away if a clean JAAS/Subject based
     * infrastructure is provided. But for now this must be done.
     *
     * <P>Note that this method is called after the authentication
     * has succeeded. If authentication failed do not call this method.
     * 
     * <P>A lot of the RI behavior is still present here. Some of the
     * most notable points to remember:
     * <ul>
     *  <li>Global instance field succeeded is set to true by this method.
     *
     * @param username Name of authenticated user.
     * @param password Password of this user.
     * @param theRealm Current Realm object for this authentication.
     * @param groups String array of group memberships for user (could be
     *     empty). 
     * @returns void
     *
     */
    @Deprecated
    public final void commitAuthentication(String username,
                                        String password,
                                        Realm theRealm,
                                        String[] groups)
    {
        commitUserAuthentication(groups);
    }

   /**
     * Since the passwords are to be stored as to have char[]
     * BT: 6946553.
     * Retaining the other method for backward compatability
     *
     * @param username Name of authenticated user.
     * @param password Password of this user in char[].
     * @param theRealm Current Realm object for this authentication.
     * @param groups String array of group memberships for user (could be
     *     empty).
     * @returns void
     *
     */
    public final void commitAuthentication(String username,
                                        char[] password,
                                        Realm theRealm,
                                        String[] groups)
    {
        commitUserAuthentication(groups);
    }
    /**
     * Older implementations can implement authenticate. While new implementation
     * calls authenticateUser
     * @throws LoginException
     */
    protected final void authenticateUser () throws LoginException{
        authenticate();
    }

    /**
     * Perform authentication decision.
     * Method returns silently on success and returns a LoginException
     * on failure.
     * To be implmented by sub-classes
     * @return void authenticate returns silently on successful authentication.
     * @throws com.sun.enterprise.security.LoginException on authentication failure.
     *
     */
    abstract protected void authenticate()
        throws LoginException;
}
