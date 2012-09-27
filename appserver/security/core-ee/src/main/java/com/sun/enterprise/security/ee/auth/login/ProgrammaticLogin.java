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

package com.sun.enterprise.security.ee.auth.login;

import com.sun.appserv.security.ProgrammaticLoginPermission;
import com.sun.enterprise.security.SecurityServicesUtil;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
                         
import com.sun.logging.LogDomains;
import com.sun.enterprise.security.auth.login.LoginContextDriver;


import com.sun.enterprise.security.UsernamePasswordStore;
import com.sun.enterprise.security.web.integration.WebProgrammaticLogin;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.security.common.SecurityConstants;
import com.sun.enterprise.security.common.Util;

/**
 * Implement programmatic login.
 *
 * <P>This class allows deployed applications to supply a name and
 * password directly to the security service. This info will be used
 * to attempt to login to the current realm. If authentication succeeds,
 * a security context is established as this user.
 *
 * <P>This allows applications to programmatically handle authentication.
 * The use of this mechanism is not recommended since it bypasses the
 * standard J2EE mechanisms and places all burden on the application
 * developer.
 *
 * <P>Invoking this method requires the permission
 * ProgrammaticLoginPermission with the method name being invoked.
 *
 * <P>There are two forms of the login method, one which includes the HTTP
 * request and response objects for use by servlets and one which can be used
 * by EJBs.
 *
 * 
 */
@Service
@PerLookup
public class ProgrammaticLogin 
{
    private WebProgrammaticLogin webProgrammaticLogin;
    
    private static final Logger logger =
        LogDomains.getLogger(ProgrammaticLogin.class, LogDomains.SECURITY_LOGGER);

    private static ProgrammaticLoginPermission plLogin =
        new ProgrammaticLoginPermission("login");

    private static ProgrammaticLoginPermission plLogout =
        new ProgrammaticLoginPermission("logout");

    private static final String DEFAULT_WEBPROGRAMMATICLOGIN_IMPL="com.sun.web.security.WebProgrammaticLoginImpl";
    

    /*V3:Commented 
     private static boolean isServer =
        (ApplicationServer.getServerContext() != null);*/
    
    private static javax.security.auth.callback.CallbackHandler handler = new com.sun.enterprise.security.auth.login.LoginCallbackHandler(false);
    
    public ProgrammaticLogin() {
        if (SecurityServicesUtil.getInstance() != null) {
            resolveWebProgrammaticLogin();
        }
    }

    /**
     * Password should be used as a char[]
     */

    @Deprecated
    public Boolean login(final String user, final String password,
        final String realm, boolean errors) throws Exception
    {
        return login(user, password.toCharArray(),realm, errors);
    }
    /**
     * Attempt to login.
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject.
     *
     * <p>On client side, realm and errors parameters will be ignored and
     * the actual login will not occur until we actually access a resource
     * requiring a login.  And a java.rmi.AccessException with 
     * COBRA NO_PERMISSION will occur when actual login is failed.
     *
     * <P>This method is intented primarily for EJBs wishing to do
     * programmatic login. If servlet code used this method the established
     * identity will be propagated to EJB calls but will not be used for
     * web container manager authorization. In general servlets should use
     * the servlet-specific version of login instead.
     *
     * @param user User name.
     * @param password Password for user.
     * @param realm the realm name in which the user should be logged in.
     * @param errors errors=true, propagate any exception encountered to the user
     * errors=false, no exceptions are propagated.
     * @return Boolean containing true or false to indicate success or
     *     failure of login.
     * @throws Exception any exception encountered during Login.
     */
    public Boolean login(final String user, final char[] password,
        final String realm, boolean errors) throws Exception
    {
        Boolean authenticated = null;
        // check permission to login
        try {

            // exception thrown on failure
            checkLoginPermission(user); 

            // try to login. doPrivileged is used since application code does
            // not have permissions to process the jaas login.
            authenticated = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    public java.lang.Boolean run() {
                    // if realm is null, LCD will log into the default realm
                        if (((SecurityServicesUtil.getInstance() != null) && SecurityServicesUtil.getInstance().isServer()) 
                                || Util.isEmbeddedServer()){
                            LoginContextDriver.login(user, password, realm);
                        } else {
                            int type = SecurityConstants.USERNAME_PASSWORD;
                            
                            //should not set realm here
                            // Bugfix# 6387278. The UsernamePasswordStore 
                            // abstracts the thread-local/global details
                            UsernamePasswordStore.set(user, password);

                            try {
                                LoginContextDriver.doClientLogin(type, handler);
                            } finally {
                                // For security, if thread-local no need to 
                                // save the username/password state
                                UsernamePasswordStore.resetThreadLocalOnly();
                            }
                        }
                        return Boolean.valueOf(true);
                    }
                });
        } catch (Exception e) {
            logger.log(Level.SEVERE,"prog.login.failed", e);
            if(errors == true){ // propagate the exception ahead
                throw e;    
            } else{
                authenticated = Boolean.valueOf(false);
            }
        }        
        return authenticated;
    }

    /*
     * Use of the char[] as password is encouraged
     */
    @Deprecated
    public Boolean login(final String user, final String password)
    {
        return login(user, password.toCharArray());
    }

    /** Attempt to login.
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject.
     *
     * <p>On client side, the actual login will not occur until we actually
     * access a resource requiring a login.  And a java.rmi.AccessException
     * with COBRA NO_PERMISSION will occur when actual login is failed.
     *
     * <P>This method is intented primarily for EJBs wishing to do
     * programmatic login. If servlet code used this method the established
     * identity will be propagated to EJB calls but will not be used for
     * web container manager authorization. In general servlets should use
     * the servlet-specific version of login instead.
     *
     * @param user User name.
     * @param password Password for user.
     * @return Boolean containing true or false to indicate success or
     *     failure of login.
     */
    public Boolean login(final String user, final char[] password)
    {
        // call login with realm-name = null and request for errors = false
        Boolean authenticated = null;
        try{
            authenticated = login(user, password, null, false);
        } catch(Exception e){
            // sanity checking, will never come here
            authenticated = Boolean.valueOf(false); 
        }
        return authenticated;
    }


    /*
     * Use of the char[] as password is encouraged
     */
    @Deprecated
    public Boolean login(final String user, final String password,
                         final String realm,
                         final HttpServletRequest request,
                         final HttpServletResponse response, boolean errors)
                         throws Exception {
        return login(user, password.toCharArray(), realm, request, response, errors);
    }
    /** Attempt to login. This method is specific to servlets (and JSPs).
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject. In addition, the
     * principal stored in the request is set to the user name. If a session
     * is available, its principal is also set to the user provided.
     *
     * @returns Boolean containing true or false to indicate success or
     *     failure of login.
     * @param realm
     * @param errors
     * @param user User name.
     * @param password Password for user.
     * @param request HTTP request object provided by caller application. It
     *     should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It
     *     should be an instance of HttpServletResponse.
     * @throws Exception any exceptions encountered during login
     * @return Boolean indicating true for successful login and false otherwise
     */
    public Boolean login(final String user, final char[] password,
                         final String realm, 
                         final HttpServletRequest request,
                         final HttpServletResponse response, boolean errors)
                         throws Exception
    {
        Boolean authenticated = null;
        try{
            // check permission to login        
            checkLoginPermission(user);
            // try to login. doPrivileged is used since application code does
            // not have permissions to process the jaas login.
            authenticated = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    public Boolean run() {
                        return webProgrammaticLogin.login(user, password, realm,
                                                          request, response);
                    }
                });
        } catch(Exception e){
            if(errors != true){
                authenticated = Boolean.valueOf(false);
            } else{
                throw e;
            }
        }            
        return authenticated;
    }

    /*
     * Use of char[] as password is encouraged
     */
    @Deprecated
    public Boolean login(final String user, final String password,
                     final HttpServletRequest request,
                     final HttpServletResponse response) {
        return login(user, password.toCharArray(), request, response);

    }

    /**
     * Attempt to login. This method is specific to servlets (and JSPs).
     *
     * <P>Upon successful return from this method the SecurityContext will
     * be set in the name of the given user as its Subject. In addition, the
     * principal stored in the request is set to the user name. If a session
     * is available, its principal is also set to the user provided.
     *
     * @param user User name.
     * @param password Password for user.
     * @param request HTTP request object provided by caller application. It
     *     should be an instance of HttpRequestFacade.
     * @param response HTTP response object provided by called application. It
     *     should be an instance of HttpServletResponse.
     * @return Boolean containing true or false to indicate success or
     *     failure of login.
     *
     */
    public Boolean login(final String user, final char[] password,
                         final HttpServletRequest request,
                         final HttpServletResponse response)
    {
        Boolean authenticated = null;    
        try{
            // pass a null realmname and errors=false
            authenticated = login(user, password, null, request, response, false);
        }catch (Exception e){
            // sanity check will never come here
            authenticated = Boolean.valueOf(false);
        }
        return authenticated;
    }
    /**
     * Attempt to logout.
     * @returns Boolean containing true or false to indicate success or
     *     failure of logout.
     *
     */
    public Boolean logout()
    {
        Boolean loggedout = null;
        try{
           loggedout = logout(false);
        } catch(Exception e){
            // sanity check will never come here
            loggedout = Boolean.valueOf(false);
        }
        return loggedout;
    }
    /**
     * Attempt to logout.
     * @param errors, errors = true, the method will propagate the exceptions 
     * encountered while logging out, errors=false will return a Boolean value
     * of false indicating failure of logout
     * @return Boolean containing true or false to indicate success or
     *     failure of logout.
     * @throws Exception encountered while logging out, if errors==false
     *
     */
    public Boolean logout(boolean errors) throws Exception
    {
        Boolean loggedout = null;
        // check logout permission
        try{
            checkLogoutPermission();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public java.lang.Object run() {
                    //V3:Commentedif (isServer) {
                    if (SecurityServicesUtil.getInstance() != null && 
                            SecurityServicesUtil.getInstance().isServer()) {
                        LoginContextDriver.logout();
                    } else {
                        // Reset the username/password state on logout
                        UsernamePasswordStore.reset();

                        LoginContextDriver.doClientLogout();
                        //If user try to access a protected resource after here
                        //then it will prompt for password in appclient or
                        //just fail in standalone client.
                    }
                    return null;
                }
            });
            loggedout = Boolean.valueOf(true);
        } catch (Exception e) {
            logger.log(Level.WARNING, "prog.logout.failed", e);
            if(errors){
                throw e;
            } else{
                loggedout = Boolean.valueOf(false);
            }
        }
        return loggedout;
    }

    /**
     * Attempt to logout. Also removes principal from request (and session
     * if available).
     *
     * @returns Boolean containing true or false to indicate success or
     *     failure of logout.
     *
     */
    public Boolean logout(final HttpServletRequest request,
                          final HttpServletResponse response)
    {
        Boolean loggedout = null;
        try{
            loggedout = logout(request, response, false);
        }catch(Exception e){
            // sanity check, will never come here
            loggedout = Boolean.valueOf(false);
        }
        return loggedout;
    }

    /**
     * Attempt to logout. Also removes principal from request (and session
     * if available).
     * @param errors, errors = true, the method will propagate the exceptions 
     * encountered while logging out, errors=false will return a Boolean value
     * of false indicating failure of logout
     *
     * @return Boolean containing true or false to indicate success or
     *     failure of logout.
     * @throws Exception, exception encountered while logging out and if errors
     * == true
     */
    public Boolean logout(final HttpServletRequest request,
                          final HttpServletResponse response, 
                          boolean errors) throws Exception
    {
        // check logout permission
        Boolean loggedout = null;
        try{
            checkLogoutPermission();
            loggedout = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                public Boolean run() throws Exception{
                    return webProgrammaticLogin.logout(request, response);
                }
            });
        }catch(Exception e){
            if(errors){
                throw e;
            }else{
                loggedout = Boolean.valueOf(false);
            }
        }
        return loggedout;
    }

    
    /**
     * Check whether caller has login permission.
     *
     */   
    private void checkLoginPermission(String user) throws Exception
    {
        try {
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, "ProgrammaticLogin.login() called for user: " 
                    + user);
            }
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(plLogin);
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
    private void checkLogoutPermission() throws Exception
    {
        try {
            if(logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, "ProgrammaticLogin.logout() called.");
            }
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(plLogout);
            }
            
        } catch (Exception e) {
            logger.warning("prologout.noperm");
            throw e;
        }
    }
    
    private void resolveWebProgrammaticLogin() {
        ServiceLocator habitat = SecurityServicesUtil.getInstance().getHabitat();
        this.webProgrammaticLogin = habitat.getService(WebProgrammaticLogin.class);
    }


}
