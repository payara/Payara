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

package com.sun.enterprise.security.common;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.integration.AppServSecurityContext;
import java.security.Principal;
import javax.security.auth.Subject;

import org.glassfish.security.common.PrincipalImpl;
//V3:Comment import com.sun.enterprise.ServerConfiguration;

import java.util.logging.*;
import com.sun.logging.*;


/**
 * This class represents the security context on the client side.
 * For usage of the IIOP_CLIENT_PER_THREAD_FLAG flag, see
 * UsernamePasswordStore. When set to false, the volatile
 * field sharedCsc is used to store the context.
 *
 * @see UsernamePasswordStore
 * @author Harpreet Singh
 *
 */
public final class ClientSecurityContext extends AbstractSecurityContext {
    
    private static final Logger _logger = SecurityLoggerInfo.getLogger();

    public static final String IIOP_CLIENT_PER_THREAD_FLAG =
        "com.sun.appserv.iiopclient.perthreadauth";

    // Bug Id: 4787940
    private static final boolean isPerThreadAuth = 
            Boolean.getBoolean(IIOP_CLIENT_PER_THREAD_FLAG);

    // either the thread local or shared version will be used
    private static ThreadLocal localCsc =
        isPerThreadAuth ? new ThreadLocal() : null;
    private static volatile ClientSecurityContext sharedCsc;

    /**
     * This creates a new ClientSecurityContext object.
     * @param The name of the user.
     * @param The Credentials of the user.
     */
    public ClientSecurityContext(String userName, 
				 Subject s) {

	this.initiator = new PrincipalImpl(userName);
	this.subject = s ;
    }

    /**
     * Initialize the SecurityContext & handle the unauthenticated
     * principal case
     
    public static ClientSecurityContext init() {
	ClientSecurityContext sc = getCurrent();
	if (sc == null) { // there is no current security context
            // create a default one if
	    sc = generateDefaultSecurityContext();
        }
	return sc;
    }*/
    
   /*
    private static ClientSecurityContext generateDefaultSecurityContext() {
	final String PRINCIPAL_NAME = "auth.default.principal.name";
	final String PRINCIPAL_PASS = "auth.default.principal.password";
	
        
	//ServerConfiguration config = ServerConfiguration.getConfiguration();
	//String username = config.getProperty(PRINCIPAL_NAME, "guest");
	//String password = config.getProperty(PRINCIPAL_PASS, "guest123");
	
        //Temporary hardcoding to make V3 code for WebProfile compile
        String username ="guest";
        char[] password = new char[]{'g','e','t','s','t','1','2','3'};
        synchronized (ClientSecurityContext.class) {
            // login & all that stuff..
            try {
                final Subject subject = new Subject();
                final PasswordCredential pc = new PasswordCredential(username,
                        password, "default");
                AppservAccessController.doPrivileged(new PrivilegedAction() {
                    public java.lang.Object run() {
                        subject.getPrivateCredentials().add(pc);
                        return null;
                    }
                });
                // we do not need to generate any credential as authorization
                // decisions are not being done on the appclient side.
                ClientSecurityContext defaultCSC =
                    new ClientSecurityContext(username, subject);
                setCurrent(defaultCSC);
                return defaultCSC;
            } catch(Exception e) {
                _logger.log(Level.SEVERE,
                            "java_security.gen_security_context", e);
                return null;
            }
        }
    }
    */

    /**
     * This method gets the SecurityContext stored here.  If using a
     * per-thread authentication model, it gets the context from
     * Thread Local Store (TLS) of the current thread. If not using a
     * per-thread authentication model, it gets the singleton context.
     *
     * @return The current Security Context stored here. It returns
     *      null if SecurityContext could not be found.
     */
    public static ClientSecurityContext getCurrent() {
        if (isPerThreadAuth) {
            return (ClientSecurityContext) localCsc.get();
        } else {
            return sharedCsc;
        }
    }

    /**
     * This method sets the SecurityContext to be stored here.
     * 
     * @param The Security Context that should be stored.
     */
    public static void setCurrent(ClientSecurityContext sc) {
        if (isPerThreadAuth) {
            localCsc.set(sc);
        } else {
            sharedCsc = sc;
        }
    } 

    /**
     * This method returns the caller principal. 
     * This information may be redundant since the same information 
     * can be inferred by inspecting the Credentials of the caller.
     * 
     * @return The caller Principal. 
     */
    public Principal getCallerPrincipal() {
	return initiator;
    }

    
    public Subject getSubject() {
	return subject;
    }

    public String toString() {
	return "ClientSecurityContext[ " + "Initiator: " + initiator +
	    "Subject " + subject + " ]";
    }
    
    //added for CR:6620388
    public static boolean hasEmtpyCredentials(ClientSecurityContext sc) {
        if (sc == null) {
            return true;
        }
        Subject s = sc.getSubject();
        if (s == null) {
            return true;
        }
        if (s.getPrincipals().isEmpty()) {
            return true;
        }
        return false;
    }

    public AppServSecurityContext newInstance(String userName, Subject subject, String realm) {
        //TODO:V3 ignoring realm in this case
        return new ClientSecurityContext(userName, subject);
    }

    public AppServSecurityContext newInstance(String userName, Subject subject) {
        return new ClientSecurityContext(userName, subject);
    }

    public void setCurrentSecurityContext(AppServSecurityContext context) {
        if (context instanceof ClientSecurityContext) {
            setCurrent((ClientSecurityContext)context);
            return;
        }
        throw new IllegalArgumentException("Expected ClientSecurityContext, found " + context);
    }

    public AppServSecurityContext getCurrentSecurityContext() {
         return getCurrent();
    }

    public void setUnauthenticatedSecurityContext() {
        throw new UnsupportedOperationException("Not supported yet in V3.");
    }

    public void setSecurityContextWithPrincipal(Principal principal) {
        throw new UnsupportedOperationException("Not supported yet in V3.");
    }
    

}







