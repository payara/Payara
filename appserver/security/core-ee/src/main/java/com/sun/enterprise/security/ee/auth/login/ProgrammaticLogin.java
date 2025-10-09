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
// Portions Copyright [2017-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.auth.login;

import static com.sun.enterprise.security.common.AppservAccessController.privilegedAlways;
import static com.sun.enterprise.security.common.SecurityConstants.USERNAME_PASSWORD;
import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.appserv.security.ProgrammaticLoginPermission;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.UsernamePasswordStore;
import com.sun.enterprise.security.auth.WebAndEjbToJaasBridge;
import com.sun.enterprise.security.auth.login.LoginCallbackHandler;
import com.sun.enterprise.security.auth.login.LoginContextDriver;
import com.sun.enterprise.security.common.Util;
import com.sun.enterprise.security.web.integration.WebProgrammaticLogin;
import com.sun.logging.LogDomains;

/**
 * Implement programmatic login.
 *
 * <P>
 * This class allows deployed applications to supply a name and password directly to the security service. This info
 * will be used to attempt to login to the current realm. If authentication succeeds, a security context is established
 * as this user.
 *
 * <P>
 * This allows applications to programmatically handle authentication. The use of this mechanism is not recommended
 * since it bypasses the standard Java EE mechanisms and places all burden on the application developer.
 *
 * <P>
 * Invoking this method requires the permission ProgrammaticLoginPermission with the method name being invoked.
 *
 * <P>
 * There are two forms of the login method, one which includes the HTTP request and response objects for use by servlets
 * and one which can be used by EJBs.
 *
 *
 */
@Service
@PerLookup
public class ProgrammaticLogin {

    private static final Logger logger = LogDomains.getLogger(ProgrammaticLogin.class, SECURITY_LOGGER);

    private static ProgrammaticLoginPermission plLogin = new ProgrammaticLoginPermission("login");
    private static ProgrammaticLoginPermission plLogout = new ProgrammaticLoginPermission("logout");

    private static CallbackHandler handler = new LoginCallbackHandler(false);
    private WebProgrammaticLogin webProgrammaticLogin;

    public ProgrammaticLogin() {
        if (SecurityServicesUtil.getInstance() != null) {
            resolveWebProgrammaticLogin();
        }
    }



    // ############################## EJB login methods ###############################


    /**
     * Attempt to login for EJB (either as client to login for a remote server, or on the server itself)
     *
     * <P>
     * Upon successful return from this method the SecurityContext will be set in the name of the given user as its Subject.
     *
     * <p>
     * On the client side, the actual login will not occur until we actually access a resource requiring a login. A
     * java.rmi.AccessException with COBRA NO_PERMISSION will occur when actual login is failed.
     *
     * <p>
     * This method is intented primarily for EJBs wishing to do programmatic login. If servlet code used this method the
     * established identity will be propagated to EJB calls but will not be used for web container manager authorization. In
     * general servlets should use the servlet-specific version of login instead.
     *
     * <p>
     * Note: Use of the char[] as password is encouraged
     *
     * @param user User name.
     * @param password Password for user.
     * @return Boolean containing true or false to indicate success or failure of login.
     */
    public Boolean login(String user, String password) {
        return login(user, password.toCharArray());
    }

    /**
     * Attempt to login for EJB (either client or server)
     *
     * <P>
     * Upon successful return from this method the SecurityContext will be set in the name of the given user as its Subject.
     *
     * <p>
     * On client side, the actual login will not occur until we actually access a resource requiring a login. And a
     * java.rmi.AccessException with COBRA NO_PERMISSION will occur when actual login is failed.
     *
     * <P>
     * This method is intented primarily for EJBs wishing to do programmatic login. If servlet code used this method the
     * established identity will be propagated to EJB calls but will not be used for web container manager authorization. In
     * general servlets should use the servlet-specific version of login instead.
     *
     * @param user User name.
     * @param password Password for user.
     * @return Boolean containing true or false to indicate success or failure of login.
     */
    public Boolean login(String user, char[] password) {
        // call login with realm-name = null and request for errors = false
        try {
            return login(user, password, null, false);
        } catch (Exception e) {
            // sanity checking, will never come here
            return false;
        }
    }

    /**
     * Password should be used as a char[]
     */
    public Boolean login(String user, String password, String realm, boolean errors) throws Exception {
        return login(user, password.toCharArray(), realm, errors);
    }

    /**
     * Attempt to login.
     *
     * <P>
     * Upon successful return from this method the SecurityContext will be set in the name of the given user as its Subject.
     *
     * <p>
     * On client side, realm and errors parameters will be ignored and the actual login will not occur until we actually
     * access a resource requiring a login. And a java.rmi.AccessException with COBRA NO_PERMISSION will occur when actual
     * login is failed.
     *
     * <P>
     * This method is intented primarily for EJBs wishing to do programmatic login. If servlet code used this method the
     * established identity will be propagated to EJB calls but will not be used for web container manager authorization. In
     * general servlets should use the servlet-specific version of login instead.
     *
     * @param user User name.
     * @param password Password for user.
     * @param realm the realm name in which the user should be logged in.
     * @param errors errors=true, propagate any exception encountered to the user errors=false, no exceptions are
     * propagated.
     * @return Boolean containing true or false to indicate success or failure of login.
     * @throws Exception any exception encountered during Login.
     */
    public Boolean login(String user, char[] password, String realm, boolean errors) throws Exception {
        Boolean authenticated = null;

        try {

            // Check permission to login. An exception is thrown on failure
            checkLoginPermission(user);

            // Try to login. doPrivileged is used since application code may not have permissions to process the JAAS login.
            authenticated = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    if (isServer()) {

                        // Login from Server

                        // Note: If realm is null, WebAndEjbToJaasBridge will log into the default realm
                        WebAndEjbToJaasBridge.login(user, password, realm);
                    } else {

                        // Login from Client

                        executeWithCredentials(user, password, () -> LoginContextDriver.doClientLogin(USERNAME_PASSWORD, handler));
                    }

                    return true;
                }
            });
        } catch (Exception e) {
            logger.log(SEVERE, "prog.login.failed", e);
            throwOrFalse(e, errors);
        }

        return authenticated;
    }

    /**
     * Attempt to logout for EJB.
     *
     * @returns Boolean containing true or false to indicate success or failure of logout.
     *
     */
    public Boolean logout() {
        try {
            return logout(false);
        } catch (Exception e) {
            // sanity check will never come here
            return false;
        }
    }

    /**
     * Attempt to logout for EJB.
     *
     * @param errors, errors = true, the method will propagate the exceptions encountered while logging out, errors=false
     * will return a Boolean value of false indicating failure of logout
     * @return Boolean containing true or false to indicate success or failure of logout.
     * @throws Exception encountered while logging out, if errors==false
     *
     */
    public Boolean logout(boolean errors) throws Exception {
        Boolean loggedout = null;
        // Check logout permission
        try {
            checkLogoutPermission();

            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public java.lang.Object run() {
                    if (SecurityServicesUtil.getInstance() != null && SecurityServicesUtil.getInstance().isServer()) {
                        WebAndEjbToJaasBridge.logout();
                    } else {
                        // Reset the username/password state on logout
                        UsernamePasswordStore.reset();

                        LoginContextDriver.doClientLogout();

                        // If a user try to access a protected resource after here
                        // then it will prompt for password in appclient or
                        // just fail in the standalone client.
                    }

                    return null;
                }
            });
            loggedout = true;
        } catch (Exception e) {
            logger.log(WARNING, "prog.logout.failed", e);
            loggedout = throwOrFalse(e, errors);
        }

        return loggedout;
    }



    // ############################## Servlet login methods ###############################


    /**
     * Attempt to login. This method is specific to the Servlet container.
     *
     * <P>
     * Upon successful return from this method the SecurityContext will be set in the name of the given user as its Subject.
     * In addition, the principal stored in the request is set to the user name. If a session is available, its principal is
     * also set to the user provided.
     *
     * <p>
     * Note: Use of the char[] as password is encouraged
     *
     * @returns Boolean containing true or false to indicate success or failure of login.
     * @param realm
     * @param errors
     * @param user User name.
     * @param password Password for user.
     * @param request HTTP request object provided by caller application. It should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It should be an instance of HttpServletResponse.
     * @throws Exception any exceptions encountered during login
     * @return Boolean indicating true for successful login and false otherwise
     */
    public Boolean login(String user, String password, String realm, HttpServletRequest request, HttpServletResponse response, boolean errors) throws Exception {
        return login(user, password.toCharArray(), realm, request, response, errors);
    }

    /*
     * Use of char[] as password is encouraged
     */
    public Boolean login(String user, String password, HttpServletRequest request, HttpServletResponse response) {
        return login(user, password.toCharArray(), request, response);
    }

    /**
     * Attempt to login. This method is specific to Servlets (and JSPs).
     *
     * <P>
     * Upon successful return from this method the SecurityContext will be set in the name of the given user as its Subject.
     * In addition, the principal stored in the request is set to the user name. If a session is available, its principal is
     * also set to the user provided.
     *
     * @param user User name.
     * @param password Password for user.
     * @param request HTTP request object provided by caller application. It should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It should be an instance of HttpServletResponse.
     * @return Boolean containing true or false to indicate success or failure of login.
     *
     */
    public Boolean login(String user, char[] password, HttpServletRequest request, HttpServletResponse response) {
        try {
            // pass a null realmname and errors=false
            return login(user, password, null, request, response, false);
        } catch (Exception e) {
            // sanity check will never come here
            return false;
        }
    }

    /**
     * Attempt to login. This method is specific to servlets (and JSPs).
     *
     * <P>
     * Upon successful return from this method the SecurityContext will be set in the name of the given user as its Subject.
     * In addition, the principal stored in the request is set to the user name. If a session is available, its principal is
     * also set to the user provided.
     *
     * @returns Boolean containing true or false to indicate success or failure of login.
     * @param realm
     * @param errors
     * @param user User name.
     * @param password Password for user.
     * @param request HTTP request object provided by caller application. It should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It should be an instance of HttpServletResponse.
     * @throws Exception any exceptions encountered during login
     * @return Boolean indicating true for successful login and false otherwise
     */
    public Boolean login(String user, char[] password, String realm, HttpServletRequest request, HttpServletResponse response, boolean errors) throws Exception {
        try {
            // Check permission to login
            checkLoginPermission(user);

            // Try to login. privilegedAlways is used since application code does
            // not have permissions to process the jaas login.
            return privilegedAlways(() -> webProgrammaticLogin.login(user, password, realm, request, response));
        } catch (Exception e) {
            return throwOrFalse(e, errors);
        }
    }

    /**
     * Attempt to logout. Also removes principal from request (and session if available).
     *
     * @returns Boolean containing true or false to indicate success or failure of logout.
     *
     */
    public Boolean logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            return logout(request, response, false);
        } catch (Exception e) {
            // sanity check, will never come here
            return false;
        }
    }

    /**
     * Attempt to logout. Also removes principal from request (and session if available).
     *
     * @param errors, errors = true, the method will propagate the exceptions encountered while logging out, errors=false
     * will return a Boolean value of false indicating failure of logout
     *
     * @return Boolean containing true or false to indicate success or failure of logout.
     * @throws Exception, exception encountered while logging out and if errors == true
     */
    public Boolean logout(HttpServletRequest request, HttpServletResponse response, boolean errors) throws Exception {
        try {
            // check logout permission
            checkLogoutPermission();

            return doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws Exception {
                    return webProgrammaticLogin.logout(request, response);
                }
            });
        } catch (Exception e) {
            return throwOrFalse(e, errors);
        }
    }





    // ############################## Private methods ###############################


    private void executeWithCredentials(String user, char[] password, Runnable action) {

        // Store the username and password in a global place (in TLS or a global instance variable). A
        // JAAS login module (such as ClientPasswordLoginModule) that is assumed to be configured can pick it up
        // from there. This is perhaps somewhat unorthodox, as normally you'd assume those come in via a callback.
        //
        // Note: Should not set realm here. See Bugfix# 6387278.
        // The UsernamePasswordStore abstracts the thread-local/global details
        UsernamePasswordStore.set(user, password);

        try {
            action.run();
        } finally {
            // If thread-local (TLS storage) happened to be used, there's no need to
            // keep the username/password state there at this point, so clean it out.
            UsernamePasswordStore.resetThreadLocalOnly();
        }
    }

    /**
     * Check whether caller has login permission.
     *
     */
    private void checkLoginPermission(String user) throws Exception {
        try {
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "ProgrammaticLogin.login() called for user: " + user);
            }

            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(plLogin);
            }

        } catch (Exception e) {
            logger.warning("proglogin.noperm");
            throw e;
        }
    }

    /**
     * Check if caller has logout permission.
     *
     */
    private void checkLogoutPermission() throws Exception {
        try {
            logger.log(FINE, "ProgrammaticLogin.logout() called.");

            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(plLogout);
            }

        } catch (Exception e) {
            logger.warning("prologout.noperm");
            throw e;
        }
    }

    private void resolveWebProgrammaticLogin() {
        webProgrammaticLogin = SecurityServicesUtil.getInstance().getHabitat().getService(WebProgrammaticLogin.class);
    }

    private boolean isServer() {
        return(((SecurityServicesUtil.getInstance() != null) && SecurityServicesUtil.getInstance().isServer()) || Util.isEmbeddedServer());
    }

    private boolean throwOrFalse(Exception e, boolean errors) throws Exception {
        if (errors) {
            throw e;
        }

        return false;
    }

}
