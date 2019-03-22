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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.realm.certificate;

import static java.util.Arrays.asList;
import static java.util.logging.Level.FINEST;

import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;

import org.glassfish.security.common.Group;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.BaseRealm;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.login.DistinguishedPrincipalCredential;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;

/**
 * Realm wrapper for supporting certificate authentication.
 *
 * <P>
 * The certificate realm provides the security-service functionality needed to process a client-cert authentication.
 * Since the SSL processing, and client certificate verification is done by NSS, no authentication is actually done by
 * this realm. It only serves the purpose of being registered as the certificate handler realm and to service group
 * membership requests during web container role checks.
 *
 * <P>
 * There is no JAAS LoginModule corresponding to the certificate realm, therefore this realm does not require the
 * jaas-context configuration parameter to be set. The purpose of a JAAS LoginModule is to implement the actual
 * authentication processing, which for the case of this certificate realm is already done by the time execution gets to
 * Java.
 *
 * <P>
 * The certificate realm needs the following properties in its configuration: None.
 *
 * <P>
 * The following optional attributes can also be specified:
 * <ul>
 * <li>assign-groups - A comma-separated list of group names which will be assigned to all users who present a
 * cryptographically valid certificate. Since groups are otherwise not supported by the cert realm, this allows grouping
 * cert users for convenience.
 * </ul>
 *
 */
@Service
public final class CertificateRealm extends BaseRealm {

    // Descriptive string of the authentication type of this realm.
    public static final String AUTH_TYPE = "certificate";
    
    private List<String> defaultGroups = new LinkedList<>();

    /**
     * Initialize a realm with some properties. This can be used when instantiating realms from their descriptions. This
     * method is invoked from Realm during initialization.
     *
     * @param props Initialization parameters used by this realm.
     * @exception BadRealmException If the configuration parameters identify a corrupt realm.
     * @exception NoSuchRealmException If the configuration parameters specify a realm which doesn't exist.
     *
     */
    @Override
    protected void init(Properties props) throws BadRealmException, NoSuchRealmException {
        super.init(props);

        String[] groups = addAssignGroups(null);
        if (groups != null && groups.length > 0) {
            defaultGroups.addAll(asList(groups));
        }

        String jaasCtx = props.getProperty(JAAS_CONTEXT_PARAM);
        if (jaasCtx != null) {
            setProperty(JAAS_CONTEXT_PARAM, jaasCtx);
        }
    }

    /**
     * Returns a short (preferably less than fifteen characters) description of the kind of authentication which is
     * supported by this realm.
     *
     * @return Description of the kind of authentication that is directly supported by this realm.
     */
    @Override
    public String getAuthType() {
        return AUTH_TYPE;
    }

    /**
     * Returns the name of all the groups that this user belongs to.
     *
     * @param username Name of the user in this realm whose group listing is needed.
     * @return Enumeration of group names (strings).
     * @exception InvalidOperationException thrown if the realm does not support this operation - e.g. Certificate realm
     * does not support this operation.
     * @throws com.sun.enterprise.security.auth.realm.NoSuchUserException
     *
     */
    @Override
    public Enumeration<String> getGroupNames(String username) throws NoSuchUserException, InvalidOperationException {
        // This is called during web container role check, not during
        // EJB container role cheks... fix RI for consistency.

        // Groups for cert users is empty by default unless some assign-groups
        // property has been specified (see init()).
        return Collections.enumeration(defaultGroups);
    }

    /**
     * Returns the name of all the groups that this user belongs to.
     *
     * @param subject The Subject object for the authentication request.
     * @param principal The Principal object from the user certificate.
     *
     */
    public void authenticate(Subject subject, Principal principal) {
        // It is important to use x500name.getName() in order to be
        // consistent with web containers view of the name - see bug
        // 4646134 for reasons why this matters.
        String name = principal.getName();

        _logger.log(FINEST, "Certificate realm setting up security context for: {0}", name);

        if (defaultGroups != null) {
            Set<Principal> principalSet = subject.getPrincipals();
            for (String groupName : defaultGroups) {
                principalSet.add(new Group(groupName));
            }
        }

        if (!subject.getPrincipals().isEmpty()) {
            subject.getPublicCredentials().add(new DistinguishedPrincipalCredential(principal));
        }

        SecurityContext.setCurrent(new SecurityContext(name, subject));
    }

    /**
     * <p>
     * A <code>LoginModule</code> for <code>CertificateRealm</code> can instantiate and pass a
     * <code>AppContextCallback</code> to <code>handle</code> method of the passed <code>CallbackHandler</code> to retrieve
     * the application name information.
     */
    public final static class AppContextCallback implements Callback {

        private String moduleID;

        /**
         * Get the fully qualified module name. The module name consists of the application name (if not a singleton) followed
         * by a '#' and the name of the module.
         *
         * <p>
         *
         * @return the application name.
         */
        public String getModuleID() {
            return moduleID;
        }

        /**
         * Set the fully qualified module name. The module name consists of the application name (if not a singleton) followed
         * by a '#' and the name of the module.
         * 
         * @param moduleID
         */
        public void setModuleID(String moduleID) {
            this.moduleID = moduleID;
        }
    }

}
