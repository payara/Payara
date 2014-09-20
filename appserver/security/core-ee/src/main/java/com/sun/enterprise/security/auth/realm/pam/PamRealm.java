/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.auth.realm.pam;

import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;

/**
 * Realm wrapper for supporting PAM based authentication for all Unix machines.
 * The PAM realm uses the Operating System's PAM login mechanism to authenticate
 * the applications with their OS usernames and passwords.
 * @author Nithya Subramanian
 */

@Service
public final class PamRealm extends AppservRealm {
    
    // Descriptive string of the authentication type of this realm.
    public static final String AUTH_TYPE = "pam";
    
    //Default PAM stack service set to sshd - since it is present in all
    //OSx by default
    private static final String PAM_SERVICE = "sshd";

    /**
     * Initialize a realm with some properties.  This can be used
     * when instantiating realms from their descriptions.  This
     * method may only be called a single time.  
     *
     * @param props Initialization parameters used by this realm.
     * @exception BadRealmException If the configuration parameters
     *     identify a corrupt realm.
     * @exception NoSuchRealmException If the configuration parameters
     *     specify a realm which doesn't exist.
     *
     */
    public synchronized void init(Properties props)
            throws BadRealmException, NoSuchRealmException {
        super.init(props);
        String jaasCtx = props.getProperty(JAAS_CONTEXT_PARAM);
        if (jaasCtx == null) {
            throw new BadRealmException("No jaas-context defined");
        }
        this.setProperty(JAAS_CONTEXT_PARAM, jaasCtx);
    }

    /**
     * @return Description of the kind of authentication that is directly
     *     supported by this realm.
     */
    public String getAuthType() {
        return AUTH_TYPE;
    }
    
    @Override
    public Enumeration getGroupNames(String username) throws NoSuchUserException {   
        try {
            Set<String> groupsSet = new PAM(PAM_SERVICE).getGroupsOfUser(username);
            return Collections.enumeration(groupsSet);
        } catch (PAMException ex) {
            Logger.getLogger(PamRealm.class.getName()).log(Level.SEVERE,
                    "pam_exception_getgroupsofuser", ex);
            return null;
        }       
    }

    /**
     * This method retreives the PAM service stack to be used by the Realm class
     * and Login Module uniformly
     * @return String = Pam Service
     */
    public String getPamService() {
        return PAM_SERVICE;
    }
}
