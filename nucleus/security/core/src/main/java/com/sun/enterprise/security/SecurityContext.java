/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security;

import com.sun.enterprise.security.integration.AppServSecurityContext;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.enterprise.security.common.AbstractSecurityContext;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.Iterator;
import java.util.Set;

import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;

import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.PrincipalImpl;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.security.auth.login.DistinguishedPrincipalCredential;
//V3:Comment import com.sun.enterprise.server.ApplicationServer;
import java.security.AccessController;
import org.glassfish.api.admin.ServerEnvironment;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
* This  class that extends AbstractSecurityContext that gets 
 * stored in Thread Local Storage. If the current thread creates
 * child threads, the SecurityContext stored in the current 
 * thread is automatically propagated to the child threads.
 * 
 * This class is used on the server side to represent the 
 * security context.
 *
 * Thread Local Storage is a concept introduced in JDK1.2.
 * @see java.lang.ThreadLocal
 * @see java.lang.InheritableThreadLocal
 * 
 * @author Harish Prabandham
 * @author Harpreet Singh
 */
@Service
@PerLookup
public class SecurityContext extends AbstractSecurityContext  {
   
    private static Logger _logger=null;
    static {
        _logger=SecurityLoggerInfo.getLogger();
    }

    private static InheritableThreadLocal<SecurityContext> currentSecCtx =
        new InheritableThreadLocal<SecurityContext>();
    private static SecurityContext defaultSecurityContext = 
	generateDefaultSecurityContext();

    private static javax.security.auth.AuthPermission doAsPrivilegedPerm =
 	new javax.security.auth.AuthPermission("doAsPrivileged");
 
    // Did the client log in as or did the server generate the context 
    private boolean SERVER_GENERATED_SECURITY_CONTEXT = false;


    /* This creates a new SecurityContext object.
     * Note: that the docs for Subject state that the internal sets
     * (eg. the principal set) cannot be modified unless the caller 
     * has the modifyPrincipals AuthPermission. That said, there may
     * be some value to setting the Subject read only.
     * Note: changing the principals in the embedded subject (after
     * construction will likely cause problem in the principal set 
     * keyed HashMaps of EJBSecurityManager.
     * @param username The name of the user/caller principal.
     * @param subject contains the authenticated principals and credential.
     */
    public SecurityContext(String userName, Subject subject) {
	Subject s = subject;
	if (s == null) {
	    s = new Subject();
            if (_logger.isLoggable(Level.WARNING)) {
	        _logger.warning(SecurityLoggerInfo.nullSubjectWarning);
            }
	} 
	this.initiator = new PrincipalImpl(userName);
        final Subject sub = s;
        this.subject = (Subject)
            AppservAccessController.doPrivileged(new PrivilegedAction(){
                public java.lang.Object run() {
                    sub.getPrincipals().add(initiator);
                    return sub;
                }
            });
    }

    /**
     * Create a SecurityContext with given subject having
     * DistinguishedPrincipalCredential.
     * This is used for JMAC environment.
     * @param subject
     */
    public SecurityContext(Subject subject) {
        if (subject == null) {
            subject = new Subject();
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.warning(SecurityLoggerInfo.nullSubjectWarning);
            }
	} 

        final Subject fsub = subject;
        this.subject = subject;
        this.initiator = (Principal)
            AppservAccessController.doPrivileged(new PrivilegedAction(){
                public java.lang.Object run() {
                    Principal prin = null;
                    for (Object obj : fsub.getPublicCredentials()) {
                        if (obj instanceof DistinguishedPrincipalCredential) {
                            DistinguishedPrincipalCredential dpc =
                                    (DistinguishedPrincipalCredential) obj;
                            prin = dpc.getPrincipal();
                            break;
                        }
                    }
                    // for old auth module
                    if (prin == null) {
                        Iterator<Principal> prinIter = fsub.getPrincipals().iterator();
                        if (prinIter.hasNext()) {
                            prin = prinIter.next();
                        }
                    }
                    return prin;
                }
            });

            postConstruct();
    }
    
    private void initDefaultCallerPrincipal() {
         if (this.initiator == null) {
            this.initiator = getDefaultCallerPrincipal();
        }
    }

    public SecurityContext(String userName, Subject subject, String realm) {
	Subject s = subject;
	if (s == null) {
	    s = new Subject();
            if (_logger.isLoggable(Level.WARNING)) {
	        _logger.warning(SecurityLoggerInfo.nullSubjectWarning);
            }
	}
    PrincipalGroupFactory factory = Globals.getDefaultHabitat().getService(PrincipalGroupFactory.class);
    if (factory!=null) {
	    this.initiator = factory.getPrincipalInstance(userName, realm);
    }
        final Subject sub = s;
        this.subject = (Subject)
            AppservAccessController.doPrivileged(new PrivilegedAction(){
                public java.lang.Object run() {
                    sub.getPrincipals().add(initiator);
                    return sub;
                }
            });
    }
    
    /* private constructor for constructing default security context
     */
    public SecurityContext() {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Default CTOR of SecurityContext called");
        }
 	this.subject = new Subject();
	// delay assignment of caller principal until it is requested
 	this.initiator = null;
 	this.setServerGeneratedCredentials();
        // read only is only done for guest logins.
        AppservAccessController.doPrivileged(new PrivilegedAction(){
            @Override
            public Object run() {
                subject.setReadOnly();
                return null;
            }
        });
    }
    
    /**
     * Initialize the SecurityContext and handle the unauthenticated 
     * principal case
     */
    public static SecurityContext init(){
        SecurityContext sc = currentSecCtx.get();
        if(sc == null) { // there is no current security context...
            sc = defaultSecurityContext;
	}
        return sc;
    }
      
    public static SecurityContext getDefaultSecurityContext(){
        //unauthen. Security Context.
        return defaultSecurityContext; 
    }

    public static Subject getDefaultSubject(){
        //Subject of unauthen. Security Context.
        return defaultSecurityContext.subject; 
    }
     
    // get caller principal of unauthenticated Security Context
    public static Principal getDefaultCallerPrincipal(){
        synchronized(SecurityContext.class) {
	    if (defaultSecurityContext.initiator == null) { 
		String guestUser = null;
		try {
		    guestUser = (String)
			AppservAccessController.doPrivileged(new PrivilegedExceptionAction() {
				public java.lang.Object run() throws Exception {
                                    SecurityService securityService = SecurityServicesUtil.getInstance().getHabitat().getService(SecurityService.class,
                                            ServerEnvironment.DEFAULT_INSTANCE_NAME);
                                    if(securityService==null)   return null;
                                    return securityService.getDefaultPrincipal();
				}
			    });
		} catch (Exception e) {
		    _logger.log(Level.SEVERE,
				SecurityLoggerInfo.defaultUserLoginError, e);
		} finally {
		    if (guestUser == null) {
			guestUser = "ANONYMOUS";
		    }
		}
		defaultSecurityContext.initiator = new PrincipalImpl(guestUser);
	    }
	}
	return defaultSecurityContext.initiator;
    }
      
    private static SecurityContext generateDefaultSecurityContext() {
        synchronized (SecurityContext.class) {
	    try{
		return (SecurityContext) 
		    AppservAccessController.doPrivileged(new PrivilegedExceptionAction() {
			    public java.lang.Object run() throws Exception{
				return new SecurityContext();
			    }
			});
	    } catch(Exception e){
		_logger.log(Level.SEVERE,
			    SecurityLoggerInfo.defaultSecurityContextError,e);
	    }
	}
        return null;
    }
    
    /**
     * No need to unmarshall the unauthenticated principal....
     */
    public static void reset(SecurityContext sc){
	setCurrent(sc);
    }


    /**
     * This method gets the SecurityContext stored in the 
     * Thread Local Store (TLS) of the current thread. 
     * @return The current Security Context stored in TLS. It returns
     * null if SecurityContext could not be found in the current thread.
     */
    public static SecurityContext getCurrent() {
	SecurityContext sc = currentSecCtx.get();
 	if (sc == null) {
	    sc = defaultSecurityContext;
	}
 	return sc;
    }

    
    /**
     * This method sets the SecurityContext stored in the TLS. 
     * @param sc
     * The Security Context that should be stored in TLS.
     * This public static method needs to be protected
     * such that it can only be called by container code. Otherwise
     * it can be called by application code to set its subject (which the
     * EJB security manager will use to create a domain combiner,
     * and then everything the ejb does will be run as the
     * corresponding subject.
     */
    public static void setCurrent(SecurityContext sc) {

 	if (sc != null && sc != defaultSecurityContext) {
 
 	    SecurityContext current = currentSecCtx.get();
 
 	    if (sc != current) {
 
 		boolean permitted = false;
 
 		try {
		    java.lang.SecurityManager sm = System.getSecurityManager();
		    if (sm != null) {
			if(_logger.isLoggable(Level.FINE)){
			    _logger.fine("permission check done to set SecurityContext");
			}
			sm.checkPermission(doAsPrivilegedPerm);
		    }
 		    permitted = true;
 		} catch (java.lang.SecurityException se) {
 		    _logger.log(Level.SEVERE, SecurityLoggerInfo.securityContextPermissionError, se);
 		} catch (Throwable t) {
 		    _logger.log(Level.SEVERE, SecurityLoggerInfo.securityContextUnexpectedError, t);
 		}
 	    
 		if (permitted) {
		    currentSecCtx.set(sc);
		} else {
 		    _logger.severe(SecurityLoggerInfo.securityContextNotChangedError);
 		}
 	    }
 	} else {
	    currentSecCtx.set(sc);
	}
    }
   
    public static void setUnauthenticatedContext() {
 	currentSecCtx.set(defaultSecurityContext);
    }

    public boolean didServerGenerateCredentials (){
	return SERVER_GENERATED_SECURITY_CONTEXT;
    }
    
    private void setServerGeneratedCredentials(){
	SERVER_GENERATED_SECURITY_CONTEXT = true;
    }


    /**
     * This method returns the caller principal. 
     * This information may be redundant since the same information 
     * can be inferred by inspecting the Credentials of the caller. 
     * @return The caller Principal. 
     */
    public Principal getCallerPrincipal() {
	return this == defaultSecurityContext ? getDefaultCallerPrincipal() : initiator;
    }

    
    public Subject getSubject() {
	return subject;
    }


    public String toString() {
	return "SecurityContext[ " + "Initiator: " + 
	    initiator + "Subject " + subject + " ]";
    }

    public Set getPrincipalSet() {
        return subject.getPrincipals();       
    }

    public void postConstruct() {
        initDefaultCallerPrincipal();
    }

    public AppServSecurityContext newInstance(String userName, Subject subject, String realm) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SecurityContext: newInstance method called");
        }
        return new SecurityContext(userName, subject, realm);
    }

    public AppServSecurityContext newInstance(String userName, Subject subject) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SecurityContext: newInstance method called");
        }
        return new SecurityContext(userName, subject);
    }

    public void setCurrentSecurityContext(AppServSecurityContext context) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SecurityContext: setCurrentSecurityContext method called");
        }
        if (context == null) {
            setCurrent(null);
            return;
        } 
        if (context instanceof SecurityContext) {
            setCurrent((SecurityContext)context);
            return;
        }
        throw new IllegalArgumentException("Expected SecurityContext, found " + context);
    }

    public AppServSecurityContext getCurrentSecurityContext() {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SecurityContext: getCurrent() method called");
        }
        return getCurrent();
    }

    public void setUnauthenticatedSecurityContext() {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SecurityContext: setUnauthenticatedSecurityContext method called");
        }
        setUnauthenticatedContext();
    }

    public void setSecurityContextWithPrincipal(Principal principal) {
        SecurityContext ctx = getSecurityContextForPrincipal(principal);
        setCurrent(ctx);
    }
    
    //Moved from J2EEInstanceListener.java
    private SecurityContext getSecurityContextForPrincipal(final Principal p) {
        if (p == null) {
            return null;
        } else if (p instanceof SecurityContextProxy) {
            return ((SecurityContextProxy) p).getSecurityContext();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
                public SecurityContext run() {
                    Subject s = new Subject();
                    s.getPrincipals().add(p);
                    return new SecurityContext(p.getName(), s);
                }
            });
        }
    }
}
