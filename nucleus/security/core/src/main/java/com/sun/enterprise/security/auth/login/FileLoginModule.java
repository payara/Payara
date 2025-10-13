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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.login;

import static java.util.logging.Level.FINE;

import javax.security.auth.login.LoginException;

import com.sun.enterprise.security.BasePasswordLoginModule;
import com.sun.enterprise.security.auth.realm.file.FileRealm;

/**
 * File realm login module.
 *
 * <P>
 * Provides a file-based implementation of a password login module. Processing is delegated to the FileRealm class.
 *
 * @see com.sun.enterprise.security.auth.realm.file.FileRealm
 *
 */
public class FileLoginModule extends BasePasswordLoginModule {

    /**
     * Perform file authentication. Delegates to FileRealm.
     *
     * @throws LoginException If login fails (JAAS login() behavior).
     *
     */
    @Override
    protected void authenticateUser() throws LoginException {
        String[] groups = getRealm(FileRealm.class, "filelm.badrealm").authenticate(_username, getPasswordChar());

        if (groups == null) { // JAAS behavior
            throw new LoginException(sm.getString("filelm.faillogin", _username));
        }

        if (LOGGER.isLoggable(FINE)) {
            LOGGER.log(FINE, "File login succeeded for: {0}", _username);
        }

        commitUserAuthentication(groups);
    }

}
