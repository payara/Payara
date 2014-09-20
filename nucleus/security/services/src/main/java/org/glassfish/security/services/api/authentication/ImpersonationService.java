/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.api.authentication;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.jvnet.hk2.annotations.Contract;

/**
 * The Impersonation Service
 */
@Contract
public interface ImpersonationService {
    /**
     * Impersonate a user, specifying the user and group principal names that
     * should be established in the resulting Subject.
     * 
     * Note that, that this method always behaves as if <bold>virtual</bold> were true in the case
     * that the underlying user store provider does not support user lookup.
     * 
     * @param user The username.
     * @param groups An array of group names.  If <bold>virtual</bold> is true, group principals will be created
     * using this array.  If <bold>virtual</bold> is false and groups is non-null, it will be used to filter the
     * groups returned by the configured UserStoreProvider.
     * @param subject An optional Subject to receive principals and credentials for the logged in user.
     * If provided, it will be returned as the return value; if not, a new Subject will be returned.
     * @param virtual  If true, simply create a subject with the given user and group names.  If false, configured
     * UserStoreProvider will be queried for the given username and a Subject created only if the user exists.  Groups
     * will be populated with the intersection of the groups parameter and the groups returned by the UserStoreProvider.
     * 
     * @return A Subject representing the impersonated user.
     * 
     * @throws LoginException
     */
    public Subject impersonate(String user, String[] groups, Subject subject, boolean virtual)
            throws LoginException;
}
