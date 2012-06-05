/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.config.serverbeans.SecureAdminPrincipal;
import com.sun.enterprise.security.auth.realm.file.FileRealmUser;
import com.sun.logging.LogDomains;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.module.common_impl.LogHelper;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.util.net.NetUtils;
import java.io.File;
import java.util.Map;
import java.util.logging.Level;

import org.glassfish.api.container.Sniffer;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.internal.api.*;
import org.glassfish.security.services.api.authentication.AuthenticationService;
import org.jvnet.hk2.annotations.ContractProvided;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.BaseServiceLocator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import javax.management.remote.JMXAuthenticator;
import java.util.logging.Logger;
import java.io.IOException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.security.KeyStore;
import java.security.Principal;
import java.util.*;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.security.common.Group;
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
    BaseServiceLocator habitat;
    
    @Inject @Named("security") @Optional
    Sniffer snif;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    volatile SecurityService ss;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    volatile AdminService as;

    @Inject
    LocalPassword localPassword;

    @Inject
    ServerContext sc;

    @Inject
    Domain domain;

    @Inject
    private AuthTokenManager authTokenManager;

    private SecureAdmin secureAdmin;

    @Inject
    ServerEnvironment serverEnv;
    
    @Inject
    private AuthenticationService authService;
    
    private static LocalStringManagerImpl lsm = new LocalStringManagerImpl(GenericAdminAuthenticator.class);
    
    private static final Logger logger = LogDomains.getLogger(GenericAdminAuthenticator.class,
            LogDomains.ADMIN_LOGGER);

    private KeyStore truststore = null;

    /** maps server alias to the Principal for the cert with that alias from the truststore */
    private Map<String,Principal> serverPrincipals = new HashMap<String,Principal>();

    @Override
    public synchronized void postConstruct() {
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
    public Subject loginAsAdmin(String user, String password,
            String realm, final String originHost) throws LoginException {
        if (user.isEmpty()) {
            user = getDefaultAdminUser();
        }
        if ( ! isInAdminGroup(user, realm)) {
            return null;
        }
        
        final Subject s = authenticate(user, password.toCharArray());
        return s;
    }
       
    
    /** Ensures that authentication and authorization works as specified in class documentation.
     *
     * @param user String representing the user name of the user doing an admin opearation
     * @param password String representing clear-text password of the user doing an admin operation
     * @param realm String representing the name of the admin realm for given server
     * @param request the Grizzly request containing the admin request
     * @return AdminAcessController.Access level of access to grant
     * @throws LoginException
     */
    @Override
    public Subject loginAsAdmin(
            Request request) throws LoginException {
        return loginAsAdmin(request, null);
    }
    
    @Override
    public Subject loginAsAdmin(
            Request request, String hostname) throws LoginException {
        final Subject s;
        try {
            s = authenticate(request, hostname);
        } catch (IOException ex) {
            final LoginException lex = new LoginException();
            lex.initCause(ex);
            throw lex;
        }
        
        return s;
    }
    
    @Override
    public AdminAccessController.Access chooseAccess(final Subject s, final String originHost) {
        final Collection<String> adminPrincipals = new ArrayList<String>();
        for (SecureAdminPrincipal saPrincipal : secureAdmin.getSecureAdminPrincipal()) {
            adminPrincipals.add(saPrincipal.getDn());
        }
        
        if ( ! isAuthenticatedAsAdmin(s, adminPrincipals)) {
            logger.log(Level.FINE, "User authenticated but not as an administrator; refusing admin access");
            return AdminAccessController.Access.NONE;
        }

        AdminAccessController.Access grantedAccess;
        
        
        /*
        * I've commented out the next line and the "else" block below as a
        * temporary workaround to allow cadmin commands to contact instances
        * directly.  Note that the secure-admin rules still apply: remote 
        * access is allowed only if secure admin has been enabled.
        */
//        if (serverEnv.isDas()) {
            if ( NetUtils.isThisHostLocal(originHost) 
                ||
                SecureAdmin.Util.isEnabled(secureAdmin) ) {
                grantedAccess = AdminAccessController.Access.FULL;
            } else {
                LogHelper.getDefaultLogger().log(Level.FINE, "Forbidding the admin request to the DAS; the request is remote and secure admin is not enabled");
                grantedAccess = AdminAccessController.Access.FORBIDDEN;
            }
//        } else {
//            /*
//             * This is an instance.  Insist that the admin identifier was
//             * present in the request and matched ours in order to grant full
//             * access.
//             */
//            if (adminIndicatorCheckerMatched) {
//                grantedAccess = Access.FULL;
//                logger.log(Level.FINE, "Granting access for the admin request to this instance; the request contained the correct unique ID");
//            } else {
//                grantedAccess = Access.READONLY;
//                logger.log(Level.FINE, "Granting read-only access for the admin request to this instance; full access was refused because the request lacked the unique ID or contained an incorrect one");
//            }
//        }
        return grantedAccess;
    }
    
    private boolean isInAdminGroup(final String user, final String realm) {
        return   (as.getAssociatedAuthRealm().getGroupMapping() == null)
               || ensureGroupMembership(user, realm);
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
    
    private Subject authenticate(final Request req, final String alternateHostname) throws IOException, LoginException {
        final AdminCallbackHandler cbh = new AdminCallbackHandler(req, 
                    secureAdmin.getSpecialAdminIndicator(),
                    authTokenManager,
                    alternateHostname,
                    getDefaultAdminUser(),
                    localPassword);
        Subject s = null;
        // TODO - remove the following classloader workaround
//        /*
//            * We have to temporarily set the class loader so the authentication
//            * service can find the login module(s).
//            */
//        final Thread thread = Thread.currentThread();
//        final ClassLoader origCL = thread.getContextClassLoader();
//        final ClassLoader ccl = sc.getCommonClassLoader();
        try {
//            if (origCL != ccl) {
//                thread.setContextClassLoader(ccl);
//            }
            s = authService.login(cbh, null);
            /*
             * Local commands which send a token also currently send a username
             * and password.  It's possible that the username and password worked
             * and the authentication service did not even use the LoginModule
             * that knows about tokens, in which case the Subject returned from 
             * the login invocation would be that for the provided username and
             * password, not for the token.  So here we explicitly retrieve the
             * Subject from the token manager if there is a token and if it
             * leads to a valid Subject.
             */
            final Subject tokenSubject = consumeTokenIfPresent(req);
            if (tokenSubject != null) {
                s = tokenSubject;
            }
        } catch (LoginException lex) {
            final String cmd = req.getContextPath();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "*** LoginException during auth for {7}\n  user={0}\n  dn={1}\n  tkn={2}\n  admInd={3}\n  restTkn={4}\n  host={5}",
                    new Object[] {cbh.pw().getUserName(), 
                                    cbh.clientPrincipal() == null ? "null" : cbh.clientPrincipal().getName(), 
                                    cbh.tkn(), cbh.adminIndicator(), cbh.restTkn(), cbh.remoteHost(), cmd});
            }
            return null;
//        } finally {
//            if (origCL != ccl) {
//                thread.setContextClassLoader(origCL);
//            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "*** Login worked\n  user={0}\n  dn={1}\n  tkn={2}\n  admInd={3}\n  restTkn={4}\n  host={5}",
                    new Object[] {cbh.pw().getUserName(),  
                                    cbh.clientPrincipal() == null ? "null" : cbh.clientPrincipal().getName(), 
                                    cbh.tkn(), cbh.adminIndicator(), cbh.restTkn(), cbh.remoteHost()});
        }
            
        return s;
    }
    
    private Subject consumeTokenIfPresent(final Request req) {
        Subject result = null;
        final String token = req.getHeader(SecureAdmin.Util.ADMIN_ONE_TIME_AUTH_TOKEN_HEADER_NAME);
        if (token != null) {
            result = authTokenManager.consumeToken(token);
        }
        return result;
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
    
    private Subject authenticate(final String user, final char[] password) throws LoginException {
        return authService.login(user, password, null);
    }
    
    private static boolean isAuthenticatedAsAdmin(final Subject s, 
            final Collection<String> adminPrincipalDNs) {
        if (s == null) {
            return false;
        }
        
        for (PasswordCredential pc : s.getPrivateCredentials(PasswordCredential.class)) {
            if (pc.getRealm().equals("admin-realm")) {
                logger.log(Level.FINE, "Granting admin access for this request; user/password authenticated as a valid admin account");
                return true;
            }
        }
        return authorizeUsingCert(adminPrincipalDNs, s.getPrincipals());
    }
    
    private static boolean authorizeUsingCert(
            final Collection<String> adminPrincipalDNs,
            final Collection<Principal> principals) {
        for (Principal p : principals) {
            if (isPrincipalAuthorized(adminPrincipalDNs, p)) {
                logger.log(Level.FINE, "Cert {0} recognized as authorized admin cert", p.toString());
                return true;
            }
            logger.log(Level.FINE, "Authenticated cert {0} is not separately authorized for admin operations", 
                    p.toString());
            return false;
        }
        return false;
    }

    private static synchronized boolean isPrincipalAuthorized(
            final Collection<String> adminPrincipalDNs, 
            final Principal reqPrincipal) {
        return adminPrincipalDNs.contains(reqPrincipal.getName());
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
            } else {
                try {
                    /*
                     * This method is used for JMX over RMI authentication, so
                     * we can find out the host from RMI.
                     */
                    host = RemoteServer.getClientHost();
                } catch (ServerNotActiveException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        String realm = as.getSystemJmxConnector().getAuthRealmName(); //yes, for backward compatibility;
        if (realm == null)
            realm = as.getAuthRealmName();

        try {
            final Subject s = this.loginAsAdmin(user, password, realm, host);
            final AdminAccessController.Access result = chooseAccess(s, host);
            if ( ! result.isOK()) {
                String msg = lsm.getLocalString("authentication.failed",
                        "User [{0}] from host {1} does not have administration access", user, host);
                logger.log(Level.INFO, msg);
                throw new SecurityException(msg);
            }
            return null;
        } catch (LoginException e) {
            throw new SecurityException(e);
        }
    }
}
