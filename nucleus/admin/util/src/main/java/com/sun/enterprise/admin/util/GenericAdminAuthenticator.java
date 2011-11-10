/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.util;

import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.SecureAdminInternalUser;
import com.sun.enterprise.config.serverbeans.SecureAdminPrincipal;
import com.sun.logging.LogDomains;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.security.auth.realm.file.FileRealmUser;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.*;
import com.sun.enterprise.admin.util.AdminConstants;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.enterprise.util.net.NetUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Level;

import org.glassfish.api.container.Sniffer;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.internal.api.*;
import org.glassfish.security.common.Group;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.Habitat;

import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import javax.management.remote.JMXAuthenticator;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.Set;
import java.io.File;
import java.security.KeyStore;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.component.PostConstruct;

/** Implementation of {@link AdminAccessController} that delegates to LoginContextDriver.
 *  @author Kedar Mhaswade (km@dev.java.net)
 *  This is still being developed. This particular implementation both authenticates and authorizes
 *  the users directly or indirectly. <p>
 *  <ul>
 *    <li> Authentication works by either calling FileRealm.authenticate() or by calling LoginContextDriver.login </li>
 *    <li> The admin users in case of administration file realm are always in a fixed group called "asadmin". In case
 *         of LDAP, the specific group relationships are enforced. </li>
 *  </ul>
 *  Note that admin security is tested only with FileRealm and LDAPRealm.
 *  @see com.sun.enterprise.security.cli.LDAPAdminAccessConfigurator
 *  @see com.sun.enterprise.security.cli.CreateFileUser
 *  @since GlassFish v3
 */
@Service
@ContractProvided(JMXAuthenticator.class)
public class GenericAdminAuthenticator implements AdminAccessController, JMXAuthenticator, PostConstruct {
    @Inject
    Habitat habitat;
    
    @Inject(name="security", optional=true)
    Sniffer snif;

    @Inject(name=ServerEnvironment.DEFAULT_INSTANCE_NAME)
    volatile SecurityService ss;

    @Inject(name=ServerEnvironment.DEFAULT_INSTANCE_NAME)
    volatile AdminService as;

    @Inject
    LocalPassword localPassword;

    @Inject
    ServerContext sc;

    @Inject
    Domain domain;

    @Inject
    private AuthTokenManager authTokenManager;

    // filled in just-in-time only if needed for secure admin traffic
    private SSLUtils sslUtils = null;

    private SecureAdmin secureAdmin;

    @Inject
    ServerEnvironment serverEnv;

    private static LocalStringManagerImpl lsm = new LocalStringManagerImpl(GenericAdminAuthenticator.class);
    
    private static final Logger logger = LogDomains.getLogger(GenericAdminAuthenticator.class,
            LogDomains.ADMIN_LOGGER);

    private KeyStore truststore = null;

    /** maps server alias to the Principal for the cert with that alias from the truststore */
    private Map<String,Principal> serverPrincipals = new HashMap<String,Principal>();

    @Override
    public void postConstruct() {
        secureAdmin = domain.getSecureAdmin();
        
        // Ensure that the admin password is set as required
        if (as.usesFileRealm()) {
            try {
                AuthRealm ar = as.getAssociatedAuthRealm();
                if (FileRealm.class.getName().equals(ar.getClassname())) {
                    String adminKeyFilePath = ar.getPropertyValue("file");
                    FileRealm fr = new FileRealm(adminKeyFilePath);
                    if (!fr.hasAuthenticatableUser()) {
                        String emsg = lsm.getLocalString("secure.admin.empty.password",
                            "The server requires a valid admin password to be set before it can start. Please set a password using the change-admin-password command.");
                        logger.log(Level.SEVERE, emsg);
                        throw new IllegalStateException(emsg);
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                throw new RuntimeException(ex);
            }

        }
        
    }


    /** Ensures that authentication and authorization works as specified in class documentation.
     *
     * @param user String representing the user name of the user doing an admin opearation
     * @param password String representing clear-text password of the user doing an admin operation
     * @param realm String representing the name of the admin realm for given server
     * @param originHost the host from which the request was sent
     * @return AdminAcessController.Access level of access to grant
     * @throws LoginException
     */
    @Override
    public AdminAccessController.Access loginAsAdmin(String user, String password,
            String realm, final String originHost) throws LoginException {
        return loginAsAdmin(user, password, realm,
                originHost, Collections.EMPTY_MAP, null);
    }

    /** Ensures that authentication and authorization works as specified in class documentation.
     *
     * @param user String representing the user name of the user doing an admin opearation
     * @param password String representing clear-text password of the user doing an admin operation
     * @param realm String representing the name of the admin realm for given server
     * @param originHost the host from which the request was sent
     * @param candidateAdminIndicator String containing the special admin indicator (null if absent)
     * @param requestPrincipal Principal, typically as reported by the secure transport delivering the admin request
     * @return AdminAcessController.Access level of access to grant
     * @throws LoginException
     */
    @Override
    public synchronized AdminAccessController.Access loginAsAdmin(String user, String password, String realm,
            final String originHost, final Map<String,String> authRelatedHeaders,
            final Principal requestPrincipal) throws LoginException {
                
        /*
         * If the request includes the domain ID header, make sure its value
         * matches the configured value for this server.
         */
        final SpecialAdminIndicatorChecker adminIndicatorChecker =
                new SpecialAdminIndicatorChecker(secureAdmin, logger, authRelatedHeaders, originHost);
        if (adminIndicatorChecker.result() == SpecialAdminIndicatorChecker.Result.MISMATCHED) {
            /*
             * The checker will have logged a warning if the ID is mismatched,
             * so there's no need to do so again here.
             */
            return AdminAccessController.Access.NONE;
        }
        
        final boolean isLocal = isLocalPassword(user, password); //local password gets preference
        if (isLocal) {
            logger.fine("Accepted locally-provisioned password authentication");
            return AdminAccessController.Access.FULL;
        }
        
        /*
         * See if the request has a valid limited-use auth. token.
         */
        final boolean isTokenAuth = authenticateUsingOneTimeToken(
                authRelatedHeaders.get(SecureAdmin.Util.ADMIN_ONE_TIME_AUTH_TOKEN_HEADER_NAME));
        if (isTokenAuth) {
            logger.log(Level.FINE, "Authenticated using one-time auth token");
            return AdminAccessController.Access.FULL;
        }
                
        /*
         * See if the request has an authorized SSL cert.
         */
        final boolean isCertAuth  = authorizeUsingCert(requestPrincipal);
        if (isCertAuth) {
            logger.log(Level.FINE, "Authenticated SSL client auth principal {0}", requestPrincipal.getName());
            return AdminAccessController.Access.FULL;
        }
        
        /*
         * If the request is remote make sure we are OK with that, then 
         * see if the request has username/password credentials.
         */
        Access access = checkRemoteAccess(originHost, 
                adminIndicatorChecker.result() == SpecialAdminIndicatorChecker.Result.MATCHED);
        if (access != Access.FULL) {
            logger.log(Level.FINE, "Rejected remote access attempt, returning {0}", access.name());
            return access;
        }
        if (as.usesFileRealm()) {
            final boolean isUsernamePasswordAuth  = handleFileRealm(user, password);
            logger.log(Level.FINE, "Not an otherwise \"trusted sender\"; file realm user authentication {1} for admin user {0}",
                    new Object[] {user, isUsernamePasswordAuth ? "passed" : "failed"});
            if (isUsernamePasswordAuth) {
                if (serverEnv.isInstance()) {
                    if (isAuthorizedInternalUser(user)) {
                        access = Access.FULL;
                        logger.log(Level.FINE, "Granting access to this instance; user is set up as an internal admin user");
                    } else {
                        /*
                         * Reject normal admin user/password log-in to an instance.
                         */
                        access = Access.NONE;
                        logger.log(Level.FINE, "Rejecting the admin request to this instance; using user/password admin auth but the user is not set up as an internal admin user");
                    }
                } else {
                    logger.log(Level.FINE, "Granting admin access for this request to the DAS; user/password authenticated as a valid admin account");
                }
                logger.log(Level.FINE, "Authorized {0} access for user {1}",
                    new Object[] {access, user});

            } else {
                /*
                 * User did not authenticate. As a guard against a potential
                 * attack encode the failed username.
                 */
                String msg;
                String userToDisplay;
                String extraMsg = "";
                try {
                    userToDisplay = URLEncoder.encode(user, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    userToDisplay = "???";
                    extraMsg = ex.getLocalizedMessage();
                }
                msg = lsm.getLocalString("authentication.failed", "User [{0}] from host {1} does not have administration access", 
                            userToDisplay, originHost) + extraMsg;
                logger.log(Level.INFO, msg);
                access = Access.NONE;
            }
            return access;
        } else {
            //now, delegate to the security service
            ClassLoader pc = null;
            boolean hack = false;
            boolean authenticated = false;
            try {
                pc = Thread.currentThread().getContextClassLoader();
                if (!sc.getCommonClassLoader().equals(pc)) { //this is per Sahoo
                    Thread.currentThread().setContextClassLoader(sc.getCommonClassLoader());
                    hack = true;
                }
                Inhabitant<SecurityLifecycle> sl = habitat.getInhabitantByType(SecurityLifecycle.class);
                sl.get();
                if (snif!=null) {
                    snif.setup(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY) + "/modules/security", Logger.getAnonymousLogger());
                }
                LoginContextDriver.login(user, password.toCharArray(), realm);
                authenticated = true;
                final boolean isConsideredInAdminGroup = (
                        (as.getAssociatedAuthRealm().getGroupMapping() == null)
                        || ensureGroupMembership(user, realm));
                return isConsideredInAdminGroup
                    ? Access.FULL 
                    : Access.NONE;
           } catch(Exception e) {
//              LoginException le = new LoginException("login failed!");
//              le.initCause(e);
//              thorw le //TODO need to work on this, this is rather too ugly
                return AdminAccessController.Access.NONE;
           } finally {
                if (hack)
                    Thread.currentThread().setContextClassLoader(pc);
            }
        }
    }

    /**
     * Return the access to be granted, if the user turns out to be a valid
     * admin user, based on whether the request is local or remote and whether
     * this is the DAS or an instance.
     * <p>
     * If this is the DAS, then secure admin must be on to accept remote requests
     * from users.  If this an instance, then it's possible that the DAS is
     * using username/password authentication in its admin messages to the 
     * instances.  In that case, we make sure that the admin indicator header
     * was sent with the request and that its value matches the value in this
     * server's config.
     * 
     * @return the access to be granted to the user if subsequently authorized
     */
    private Access checkRemoteAccess(final String originHost,
            final boolean adminIndicatorCheckerMatched) {
        Access grantedAccess;
        if (serverEnv.isDas()) {
            if ( NetUtils.isThisHostLocal(originHost) 
                 ||
                 SecureAdmin.Util.isEnabled(secureAdmin) ) {
                grantedAccess = Access.FULL;
            } else {
                logger.log(Level.FINE, "Forbidding the admin request to the DAS; the request is remote and secure admin is not enabled");
                grantedAccess = Access.FORBIDDEN;
            }
        } else {
            /*
             * This is an instance.  Insist that the admin identifier was
             * present in the request and matched ours.
             */
            if (adminIndicatorCheckerMatched) {
                grantedAccess = Access.FULL;
                logger.log(Level.FINE, "Granting access for the admin request to this instance; the request contained the correct unique ID");
            } else {
                grantedAccess = Access.NONE;
                logger.log(Level.FINE, "Rejecting access for the admin request to this instance; the request lacked the unique ID or contained an incorrect one");
            }
        }
        return grantedAccess;
    }

    private boolean isAuthorizedInternalUser(final String username) {
        for (SecureAdminInternalUser u : SecureAdmin.Util.secureAdminInternalUsers(secureAdmin)) {
            if (u.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean authorizeUsingCert(final Principal reqPrincipal) throws LoginException {
        if (reqPrincipal == null) {
            return false;
        }
        try {
            if (isPrincipalAuthorized(reqPrincipal)) {
                logger.log(Level.FINE, "Cert {0} recognized as authorized admin cert", reqPrincipal.toString());
                return true;
            }
            logger.log(Level.FINE, "Authenticated cert {0} is not separately authorized for admin operations", 
                    reqPrincipal.toString());
            return false;
        } catch (Exception ex) {
            final LoginException loginEx = new LoginException();
            loginEx.initCause(ex);
            throw loginEx;
        }
    }

    private boolean isPrincipalAuthorized(final Principal reqPrincipal) {
        final String principalName = reqPrincipal.getName();
        for (SecureAdminPrincipal configPrincipal : SecureAdmin.Util.secureAdminPrincipals(secureAdmin, habitat)) {
            if (configPrincipal.getDn().equals(principalName)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean authenticateUsingOneTimeToken(
            final String oneTimeAuthToken) {
        return oneTimeAuthToken == null ? false : authTokenManager.consumeToken(oneTimeAuthToken);
    }


    private boolean ensureGroupMembership(String user, String realm) {
        try {
            SecurityContext secContext = SecurityContext.getCurrent();
            Set ps = secContext.getPrincipalSet(); //before generics
            for (Object principal : ps) {
                if (principal instanceof Group) {
                    Group group = (Group) principal;
                    if (group.getName().equals(AdminConstants.DOMAIN_ADMIN_GROUP_NAME))
                        return true;
                }
            }
            logger.fine("User is not the member of the special admin group");
            return false;
        } catch(Exception e) {
            logger.log(Level.FINE, "User is not the member of the special admin group: {0}", e.getMessage());
            return false;
        }

    }

    private boolean handleFileRealm(String user, String password) throws LoginException {
        /* I decided to handle FileRealm  as a special case. Maybe it is not such a good idea, but
           loading the security subsystem for FileRealm is really not required.
         * If no user name was supplied, assume the default admin user name,
         * if there is one.
         */
        if (user == null || user.length() == 0) {
            String defuser = getDefaultAdminUser();
            if (defuser != null) {
                user = defuser;
                logger.log(Level.FINE, "Using default user: {0}", defuser);
            } else
                logger.fine("No default user");
        }

        try {
            AuthRealm ar = as.getAssociatedAuthRealm();
            if (FileRealm.class.getName().equals(ar.getClassname())) {
                String adminKeyFilePath = ar.getPropertyValue("file");
                FileRealm fr = new FileRealm(adminKeyFilePath);
                FileRealmUser fru = (FileRealmUser)fr.getUser(user);
                for (String group : fru.getGroups()) {
                    if (group.equals(AdminConstants.DOMAIN_ADMIN_GROUP_NAME))
                        return fr.authenticate(user, password.toCharArray()) != null; //this is indirect as all admin-keyfile users are in group "asadmin"
                }
                return false;
            }
        } catch(NoSuchUserException ue) {
            return false;       // if fr.getUser fails to find the user name
        } catch(Exception e) {
            LoginException le =  new LoginException (e.getMessage());
            le.initCause(e);
            throw le;
        }
        return false;
    }

    /**
     * Return the default admin user.  A default admin user only
     * exists if the admin realm is a file realm and the file
     * realm contains exactly one user.  If so, that's the default
     * admin user.
     */
    private String getDefaultAdminUser() {
        AuthRealm realm = as.getAssociatedAuthRealm();
        if (realm == null) {
            //this is really an assertion -- admin service's auth-realm-name points to a non-existent realm
            throw new RuntimeException("Warning: Configuration is bad, realm: " + as.getAuthRealmName() + " does not exist!");
        }
        if (! FileRealm.class.getName().equals(realm.getClassname())) {
            logger.fine("CAN'T FIND DEFAULT ADMIN USER: IT'S NOT A FILE REALM");
            return null;  // can only find default admin user in file realm
        }
        String pv = realm.getPropertyValue("file");  //the property named "file"
        File   rf = null;
        if (pv == null || !(rf=new File(pv)).exists()) {
            //an incompletely formed file property or the file property points to a non-existent file, can't allow access
            logger.fine("CAN'T FIND DEFAULT ADMIN USER: THE KEYFILE DOES NOT EXIST");
            return null;
        }
        try {
            FileRealm fr = new FileRealm(rf.getAbsolutePath());
            Enumeration users = fr.getUserNames();
            if (users.hasMoreElements()) {
                String au = (String) users.nextElement();
                if (!users.hasMoreElements()) {
                    FileRealmUser fru = (FileRealmUser)fr.getUser(au);
                    for (String group : fru.getGroups()) {
                        if (group.equals(AdminConstants.DOMAIN_ADMIN_GROUP_NAME))
                            // there is only one admin user, in the right group, default to it
                            logger.log(Level.FINE, "Attempting access using default admin user: {0}", au);
                            return au;
                    }
                }
            }
        } catch(Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Check whether the password is the local password.
     * We ignore the user name but could check whether it's
     * a valid admin user name.
     */
    private boolean isLocalPassword(String user, String password) {
        if (!localPassword.isLocalPassword(password)) {
            logger.finest("Password is not the local password");
            return false;
        }
        logger.fine("Allowing access using local password");
        return true;
    }

    /**
     * The JMXAUthenticator's authenticate method.
     */
    @Override
    public Subject authenticate(Object credentials) {
        String user = "", password = "";
        String host = null;
        if (credentials instanceof String[]) {
            // this is supposed to be 2-string array with user name and password
            String[] up = (String[])credentials;
            if (up.length == 1) {
                user = up[0];
            } else if (up.length >= 2) {
                user = up[0];
                password = up[1];
                if (password == null)
                    password = "";
            }
            if (up.length > 2) {
                host = up[2];
            }
        }

        String realm = as.getSystemJmxConnector().getAuthRealmName(); //yes, for backward compatibility;
        if (realm == null)
            realm = as.getAuthRealmName();

        try {
            AdminAccessController.Access result = this.loginAsAdmin(user, password, realm, host);
            if (result == AdminAccessController.Access.NONE) {
                String msg = lsm.getLocalString("authentication.failed",
                        "User [{0}] from host {1} does not have administration access", user, host);
                throw new SecurityException(msg);
            }
            // TODO Do we need to build a Subject so JMX can enforce monitor-only vs. manage permissions?
            return null; //for now;
        } catch (LoginException e) {
            throw new SecurityException(e);
        }
    }
    
    /*
     * If the admin client sent the unique domain identifier in a header then
     * that should mean the request came from another GlassFish server in this
     * domain. Make sure that the value, if present, matches the one in 
     * this server's domain config.  If they do not match then reject the 
     * message - it came from a domain other than this server's.
     * 
     * Note that we don't insist that every request have the domain identifier.  For 
     * example, requests from asadmin will not include the domain ID.  But if
     * the domain ID is present in the request it needs to match the 
     * configured ID.
     */
    private static class SpecialAdminIndicatorChecker {
        
        private static enum Result {
            NOT_IN_REQUEST,
            MATCHED,
            MISMATCHED
        }
        
        private final Result _result;
        
        private SpecialAdminIndicatorChecker(
                final SecureAdmin sa,
                final Logger logger,
                final Map<String,String> authRelatedHeaders,
                final String originHost) {
            final String requestSpecialAdminIndicator = 
                    authRelatedHeaders.get(SecureAdmin.Util.ADMIN_INDICATOR_HEADER_NAME);
            if (requestSpecialAdminIndicator != null) {
                if (requestSpecialAdminIndicator.equals(
                        SecureAdmin.Util.configuredAdminIndicator(sa))) {
                    _result = Result.MATCHED;
                    logger.fine("Admin request contains expected domain ID");
                } else {
                    final String msg = lsm.getLocalString("foreign.domain.ID", 
                    "An admin request arrived from {0} with the domain identifier {1} which does not match the domain identifier {2} configured for this server's domain; rejecting the request",
                    originHost, requestSpecialAdminIndicator, sa.getSpecialAdminIndicator());
                    logger.log(Level.WARNING, msg);
                    _result = Result.MISMATCHED;
                }
            } else {
                logger.fine("Admin request contains no domain ID; this is OK - continuing");
                _result = Result.NOT_IN_REQUEST;
            }
        }
        
        private Result result() {
            return _result;
        }
        
    }
}
