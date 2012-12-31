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
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.util.net.NetUtils;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.glassfish.api.container.Sniffer;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.security.services.api.authentication.AuthenticationService;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.LoginException;
import javax.security.auth.Subject;
import javax.management.remote.JMXAuthenticator;
import java.util.logging.Logger;
import java.io.IOException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.security.common.Group;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.internal.api.LocalPassword;
import org.glassfish.internal.api.RemoteAdminAccessException;
import org.glassfish.internal.api.ServerContext;

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
@ContractsProvided({JMXAuthenticator.class, AdminAccessController.class})
public class GenericAdminAuthenticator implements AdminAccessController, JMXAuthenticator, PostConstruct {
    
    @LoggerInfo(subsystem="ADMSEC", description="Admin security")
    private static final String ADMSEC_LOGGER_NAME = "javax.enterprise.system.tools.admin.security";

    static final Logger ADMSEC_LOGGER = Logger.getLogger(ADMSEC_LOGGER_NAME, 
            AdminLoggerInfo.SHARED_LOGMESSAGE_RESOURCE);
    
    @Inject
    ServiceLocator habitat;
    
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
                        ADMSEC_LOGGER.log(Level.SEVERE, AdminLoggerInfo.mSecureAdminEmptyPassword);
                        throw new IllegalStateException(ADMSEC_LOGGER.getResourceBundle()
                                .getString(AdminLoggerInfo.mSecureAdminEmptyPassword));
                    }
                }
            } catch (Exception ex) {
                ADMSEC_LOGGER.log(Level.SEVERE, AdminLoggerInfo.mUnexpectedException, ex);
                throw new RuntimeException(ex);
            }

        }
    }
    

    /** 
     * Attempts to authenticate the user as an administrator.
     *
     * @param user String representing the user name of the user doing an admin opearation
     * @param password String representing clear-text password of the user doing an admin operation
     * @param realm String representing the name of the admin realm for given server
     * @param originHost the host from which the request was sent
     * @return Subject representing the authenticated user
     * @throws LoginException if authentication fails
     * @throws RemoteAdminAccessException if the connection is remote but secure admin is disabled
     */
    @Override
    public Subject loginAsAdmin(String user, String password,
            String realm, final String originHost) throws LoginException {
        final Subject s = authenticate(user, password.toCharArray(), realm, originHost);
        return s;
    }
       
    
    /**
     * Attempts to authenticate the user as an administrator
     *
     * @param request the Grizzly request containing the admin request
     * @return Subject representing the authenticated user
     * @throws LoginException if authentication fails
     * @throws RemoteAdminAccessException if the connection is remote but secure admin is disabled
     */
    @Override
    public Subject loginAsAdmin(
            Request request) throws LoginException {
        return loginAsAdmin(request, null);
    }
    
    /**
     * Attempts to authenticate the user submitting the request as an administrator.
     * 
     * @param request the admin request
     * @param hostname the host from which the connection originated (if non-null, this hostname overrides the host in the request)
     * @return Subject representing the authenticated user
     * @throws LoginException if authentication fails
     * @throws RemoteAdminAccessException if the connection is remote but secure admin is disabled
     */
    @Override
    public Subject loginAsAdmin(
            Request request, String hostname) throws LoginException {
        final Subject s;
        try {
            s = authenticate(request, hostname);
            return s;
        } catch (IOException ex) {
            final LoginException lex = new LoginException();
            lex.initCause(ex);
            throw lex;
        }
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
            ADMSEC_LOGGER.fine("User is not a member of the special admin group");
            return false;
        } catch(Exception e) {
            ADMSEC_LOGGER.log(Level.FINE, "User is not a member of the special admin group: {0}", e);
            return false;
        }

    }
    
    private Subject authenticate(final Request req, final String alternateHostname) throws IOException, LoginException {
        final AdminCallbackHandler cbh = new AdminCallbackHandler(habitat,
                    req, 
                    alternateHostname,
                    getDefaultAdminUser(),
                    localPassword
        );
        Subject s;
        try {
            s = authService.login(cbh, null);
            /*
             * Enforce remote access restrictions, if any.
             */
            rejectRemoteAdminIfDisabled(cbh);
            consumeTokenIfPresent(req);
            if (ADMSEC_LOGGER.isLoggable(Level.FINE)) {
                ADMSEC_LOGGER.log(Level.FINE, "*** Login worked\n  user={0}\n  dn={1}\n  tkn={2}\n  admInd={3}\n  host={4}\n",
                        new Object[] {cbh.pw().getUserName(),  
                                        cbh.clientPrincipal() == null ? "null" : cbh.clientPrincipal().getName(), 
                                        cbh.tkn(), cbh.adminIndicator(), cbh.remoteHost()});
            }

            return s;
        } catch (RemoteAdminAccessException ex) {
            /*
             * Rethrow RemoteAdminAccessException explicitly to avoid it being
             * caught and processed by the LoginException catch block.
             */
            final String cmd = req.getContextPath();
            if (ADMSEC_LOGGER.isLoggable(Level.FINE)) {
                ADMSEC_LOGGER.log(Level.FINE, "*** RemoteAdminAccessException during auth for {5}\n  user={0}\n  dn={1}\n  tkn={2}\n  admInd={3}\n  host={4}\n",
                    new Object[] {cbh.pw().getUserName(), 
                                    cbh.clientPrincipal() == null ? "null" : cbh.clientPrincipal().getName(), 
                                    cbh.tkn(), cbh.adminIndicator(), cbh.remoteHost(),  cmd});
            }
            throw ex;
        } catch (LoginException lex) {
            final String cmd = req.getContextPath();
            if (ADMSEC_LOGGER.isLoggable(Level.FINE)) {
                ADMSEC_LOGGER.log(Level.FINE, "*** LoginException during auth for {5}\n  user={0}\n  dn={1}\n  tkn={2}\n  admInd={3}\n  host={4}\n",
                    new Object[] {cbh.pw().getUserName(), 
                                    cbh.clientPrincipal() == null ? "null" : cbh.clientPrincipal().getName(), 
                                    cbh.tkn(), cbh.adminIndicator(), cbh.remoteHost(),  cmd});
            }
            throw lex;
        }
    }
    
    private void rejectRemoteAdminIfDisabled(final String host) throws RemoteAdminAccessException {
        /*
         * Accept the request if secure admin is enabled or if the 
         * request is local.
         */
        if (SecureAdmin.Util.isEnabled(secureAdmin) || NetUtils.isThisHostLocal(host)) {
            return;
        }
        throw new RemoteAdminAccessException();
    }
    
    private void rejectRemoteAdminIfDisabled(final AdminCallbackHandler cbh) throws RemoteAdminAccessException {
        /*
         * If the secure admin config is not available then do not try to
         * enforce the remote access restrictions.
         */
        if (secureAdmin == null) {
            return;
        }
        /*
         * If the request contains the special admin indicator, then it's a
         * message from the DAS to an instance and it's OK for it to be remote
         * even if secure admin is not enabled.
         */
        if (secureAdmin.getSpecialAdminIndicator().equals(cbh.adminIndicator())) {
            return;
        }
        
        /*
         * If the request has an admin token then it can be a remote request
         * from an instance start-up (for example).  Accept it.
         */
        if (cbh.tkn() != null) {
            return;
        }
        rejectRemoteAdminIfDisabled(cbh.getRemoteHost());
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
     * exists if the admin realm is a file realm and the admin file
     * realm contains exactly one user in the admin group.  If so, that's the default
     * admin user.
     */
    private String getDefaultAdminUser() {
        AuthRealm realm = as.getAssociatedAuthRealm();
        if (realm == null) {
            /*
             * If for some reason there is no admin realm available return null
             * (instead of throwing an exception).
             */
            return null;
        }
        if (! FileRealm.class.getName().equals(realm.getClassname())) {
            ADMSEC_LOGGER.fine("CAN'T FIND DEFAULT ADMIN USER: IT'S NOT A FILE REALM");
            return null;  // can only find default admin user in file realm
        }
        String pv = realm.getPropertyValue("file");  //the property named "file"
        File   rf = null;
        if (pv == null || !(rf=new File(pv)).exists()) {
            //an incompletely formed file property or the file property points to a non-existent file, can't allow access
            ADMSEC_LOGGER.fine("CAN'T FIND DEFAULT ADMIN USER: THE KEYFILE DOES NOT EXIST");
            return null;
        }
        try {
            FileRealm fr = new FileRealm(rf.getAbsolutePath());
            String candidateDefaultAdminUser = null;
            for (Enumeration users = fr.getUserNames(); users.hasMoreElements();) {
                String au = (String) users.nextElement();
                FileRealmUser fru = (FileRealmUser)fr.getUser(au);
                for (String group : fru.getGroups()) {
                    if (group.equals(AdminConstants.DOMAIN_ADMIN_GROUP_NAME)) {
                        if (candidateDefaultAdminUser != null) {
                            ADMSEC_LOGGER.log(Level.FINE, "There are multiple admin users so we cannot use any as a default");
                            return null;
                        }
                        candidateDefaultAdminUser = au;
                    }
                }
            }
            if (candidateDefaultAdminUser == null) {
                ADMSEC_LOGGER.log(Level.FINE, "There are no admin users so we cannot use any as a default");
            } else {
                // there is only one admin user, in the right group, default to it
                ADMSEC_LOGGER.log(Level.FINE, "Will use \"{0}\", if needed, for a default admin user", candidateDefaultAdminUser);
            }
            return candidateDefaultAdminUser;
                
        } catch(Exception e) {
            ADMSEC_LOGGER.log(Level.WARNING, AdminLoggerInfo.mAdminUserSearchError, e);
            return null;
        }
    }
    
    private Subject authenticate(String user, final char[] password, final String realm, final String host) throws LoginException {
        if (user.isEmpty()) {
            user = getDefaultAdminUser();
        }
        if ( ! isInAdminGroup(user, realm)) {
            throw new LoginException();
        }
        
        Subject s;
        try {
            rejectRemoteAdminIfDisabled(host);
            s = authService.login(user, password, null);
            if (ADMSEC_LOGGER.isLoggable(Level.FINE)) {
            ADMSEC_LOGGER.log(Level.FINE, "*** Login worked\n  user={0}\n  host={1}\n",
                    new Object[] {user, host});
            }
            return s;
        } catch (RemoteAdminAccessException ex) {
            /*
             * Rethrow RemoteAdminAccessException explicitly to avoid it being
             * caught and processed by the LoginException catch block.
             */
            if (ADMSEC_LOGGER.isLoggable(Level.FINE)) {
                ADMSEC_LOGGER.log(Level.FINE, "*** RemoteAdminAccessException during auth\n  user={0}\n  host={1}\n  realm={2}\n",
                    new Object[] {user, host, realm});
            }
            throw ex;
        } catch (LoginException lex) {
            if (ADMSEC_LOGGER.isLoggable(Level.FINE)) {
                ADMSEC_LOGGER.log(Level.FINE, "*** LoginException during auth\n  user={0}\n  host={1}\n  realm={2}",
                    new Object[] {user, host, realm});
            }
            throw lex;
        }
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
            loginAsAdmin(user, password, realm, host);
            return null;
        } catch (LoginException e) {
            if (ADMSEC_LOGGER.isLoggable(Level.FINE)) {
                ADMSEC_LOGGER.log(Level.FINE, "*** LoginException during JMX auth\n  user={0}\n  host={1}\n  realm={2}",
                    new Object[] {user, host, realm});
            }
            throw new SecurityException(e);
        }
    }
}
