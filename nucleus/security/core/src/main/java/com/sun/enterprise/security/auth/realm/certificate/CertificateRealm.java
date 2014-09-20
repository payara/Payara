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

package com.sun.enterprise.security.auth.realm.certificate;

import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.login.DistinguishedPrincipalCredential;
import java.util.*;
import java.util.logging.Level;

import javax.security.auth.Subject;

import org.glassfish.security.common.Group;
//import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;

import com.sun.enterprise.security.auth.realm.IASRealm;
import java.security.Principal;
import javax.security.auth.callback.Callback;


import org.jvnet.hk2.annotations.Service;
import sun.security.x509.X500Name;


/**
 * Realm wrapper for supporting certificate authentication.
 *
 * <P>The certificate realm provides the security-service functionality
 * needed to process a client-cert authentication. Since the SSL processing,
 * and client certificate verification is done by NSS, no authentication
 * is actually done by this realm. It only serves the purpose of being
 * registered as the certificate handler realm and to service group
 * membership requests during web container role checks.
 *
 * <P>There is no JAAS LoginModule corresponding to the certificate realm,
 * therefore this realm does not require the jaas-context configuration
 * parameter to be set. The purpose of a JAAS LoginModule is to implement
 * the actual authentication processing, which for the case of this
 * certificate realm is already done by the time execution gets to Java.
 *
 * <P>The certificate realm needs the following properties in its
 * configuration: None.
 *
 * <P>The following optional attributes can also be specified:
 * <ul>
 *   <li>assign-groups - A comma-separated list of group names which
 *       will be assigned to all users who present a cryptographically
 *       valid certificate. Since groups are otherwise not supported
 *       by the cert realm, this allows grouping cert users
 *       for convenience.
 * </ul>
 *
 */

@Service
public final class CertificateRealm extends IASRealm
{
    // Descriptive string of the authentication type of this realm.
    public static final String AUTH_TYPE = "certificate";
    private Vector<String> defaultGroups = new Vector<String>();

    // Optional link to a realm to verify group (possibly user, later)
    // public static final String PARAM_USEREALM = "use-realm";

    // Optional ordered list of possible elements to use as user name
    // from X.500 name.
    //    public static final String PARAM_NAMEFIELD = "name-field";
    //    private String[] nameFields = null;
    //    private static final String LINK_SEP = ",";

    /**
     * Initialize a realm with some properties.  This can be used
     * when instantiating realms from their descriptions.  This
     * method is invoked from Realm during initialization.
     *
     * @param props Initialization parameters used by this realm.
     * @exception BadRealmException If the configuration parameters
     *     identify a corrupt realm.
     * @exception NoSuchRealmException If the configuration parameters
     *     specify a realm which doesn't exist.
     *
     */
    protected void init(Properties props)
        throws BadRealmException, NoSuchRealmException
    {
        super.init(props);
        String[] groups = addAssignGroups(null);
        if (groups != null && groups.length > 0) {
            for (String gp : groups) {
                defaultGroups.add(gp);
            }
        }

        String jaasCtx = props.getProperty(IASRealm.JAAS_CONTEXT_PARAM);
        if (jaasCtx != null) {
            this.setProperty(IASRealm.JAAS_CONTEXT_PARAM, jaasCtx);
        }


        /* future enhacement; allow using subset of DN as name field;
           requires RI fixes to handle subject & principal names
           consistently
        String nameLink = props.getProperty(PARAM_NAMEFIELD);
        if (nameLink == null) {
            Util.debug(log, "CertificateRealm: No "+PARAM_NAMEFIELD+
                       " provided, will use X.500 name.");
        } else {

            StringTokenizer st = new StringTokenizer(nameLink, LINK_SEP);
            int n = st.countTokens();
            nameFields = new String[n];

            for (int i=0; i<n; i++) {
                nameFields[i] = (String)st.nextToken();
            }
        }
        */

        /* future enhancement; allow linking to other realm such that
           user presence verification and group membership can be
           defined/shared; requires RI fixes to consistently check role
           memberships and RI fixes to allow multiple active realms.
        String link = props.getProperty(PARAM_USEREALM);
        if (link != null) {
            this.setProperty(PARAM_USEREALM, link);
            Util.debug(log, "CertificateRealm : "+PARAM_USEREALM+"="+link);
        }
        */
    }


    /**
     * Returns a short (preferably less than fifteen characters) description
     * of the kind of authentication which is supported by this realm.
     *
     * @return Description of the kind of authentication that is directly
     *     supported by this realm.
     */
    public String getAuthType()
    {
        return AUTH_TYPE;
    }
    

    /**
     * Returns the name of all the groups that this user belongs to.
     *
     * @param username Name of the user in this realm whose group listing
     *     is needed.
     * @return Enumeration of group names (strings).
     * @exception InvalidOperationException thrown if the realm does not
     *     support this operation - e.g. Certificate realm does not support
     *     this operation.
     *
     */
    public Enumeration getGroupNames(String username)
        throws NoSuchUserException, InvalidOperationException
    {
        // This is called during web container role check, not during
        // EJB container role cheks... fix RI for consistency.

        // Groups for cert users is empty by default unless some assign-groups
        // property has been specified (see init()).
        return defaultGroups.elements();
    }
    

    /**
     * Returns name of JAAS context used by this realm. Overrides default
     * Realm behavior of this method. The certificate realm does not
     * require (or support) a LoginModule. This method should never get
     * called unless there is a configuration error, so this overloading
     * is provided to log an error message.  See class documentation.
     *
     * @return null
     *
     */
    /*public String getJAASContext()
    {
    _logger.warning("certrealm.nojaas");
    return null;
    }*/

    /**
     * Complete authentication of certificate user.
     *
     * <P>As noted, the certificate realm does not do the actual
     * authentication (signature and cert chain validation) for
     * the user certificate, this is done earlier in NSS. This method
     * simply sets up the security context for the user in order to
     * properly complete the authentication processing.
     *
     * <P>If any groups have been assigned to cert-authenticated users
     * through the assign-groups property these groups are added to
     * the security context for the current user.
     *
     * @param subject The Subject object for the authentication request.
     * @param x500name The X500Name object from the user certificate.
     *
     
    public void authenticate(Subject subject, X500Name x500name)
    {
        // It is important to use x500name.getName() in order to be
        // consistent with web containers view of the name - see bug
        // 4646134 for reasons why this matters.
        
        String name = x500name.getName();

        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest(
                "Certificate realm setting up security context for: "+ name);
        }

        if (defaultGroups != null) {
	    Set principalSet = subject.getPrincipals();
	    Enumeration e = defaultGroups.elements();
	    while (e.hasMoreElements()) {
		principalSet.add(new Group((String) e.nextElement()));
	    }
	}
        
        SecurityContext securityContext =
	    new SecurityContext(name, subject);
        
	SecurityContext.setCurrent(securityContext);
    }*/

    /**
     * Complete authentication of certificate user.
     *
     * <P>As noted, the certificate realm does not do the actual
     * authentication (signature and cert chain validation) for
     * the user certificate, this is done earlier in NSS. This method
     * simply sets up the security context for the user in order to
     * properly complete the authentication processing.
     *
     * <P>If any groups have been assigned to cert-authenticated users
     * through the assign-groups property these groups are added to
     * the security context for the current user.
     *
     * @param subject The Subject object for the authentication request.
     * @param x500name The X500Name object from the user certificate.
     *
     */
    public void authenticate(Subject subject, X500Name x500name)
    {
        // It is important to use x500name.getName() in order to be
        // consistent with web containers view of the name - see bug
        // 4646134 for reasons why this matters.
        
        String name = x500name.getName();

        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest(
                "Certificate realm setting up security context for: "+ name);
        }

        if (defaultGroups != null) {
	    Set<Principal> principalSet = subject.getPrincipals();
	    Enumeration<String> e = defaultGroups.elements();
	    while (e.hasMoreElements()) {
		principalSet.add(new Group(e.nextElement()));
	    }
	}
        if (!subject.getPrincipals().isEmpty()) {
            DistinguishedPrincipalCredential dpc = new DistinguishedPrincipalCredential(x500name);
            subject.getPublicCredentials().add(dpc);
        }
        
        SecurityContext securityContext =
	    new SecurityContext(name, subject);

	SecurityContext.setCurrent(securityContext);
        /*AppServSecurityContext secContext = Util.getDefaultHabitat().getByContract(AppServSecurityContext.class);
        AppServSecurityContext securityContext = secContext.newInstance(name, subject);
        securityContext.setCurrentSecurityContext(securityContext);*/
        
    }

    /**
     * <p> A <code>LoginModule</code> for <code>CertificateRealm</code>
     * can instantiate and pass a <code>AppContextCallback</code>
     * to <code>handle</code> method of the passed
     * <code>CallbackHandler</code> to retrieve the application
     * name information.
     */
    public final static class AppContextCallback implements Callback {
        private String moduleID;

        /**
         * Get the fully qualified module name. The module name consists
         * of the application name (if not a singleton) followed by a '#'
         * and the name of the module.
         *
         * <p>
         *
         * @return the application name.
         */
        public String getModuleID() {
            return moduleID;
        }

        /**
         * Set the fully qualified module name. The module name consists
         * of the application name (if not a singleton) followed by a '#'
         * and the name of the module.
         * 
         */
        public void setModuleID(String moduleID) {
            this.moduleID = moduleID;
        }
    }

}
