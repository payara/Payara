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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.security.appclient.integration.AppClientSecurityInfo;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.common.ClientSecurityContext;


import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.security.auth.Subject;
/**
 * This is the callback object that gets called when a protected resource
 * needs to be accessed and authentication information is needed. Pops up
 * a UI to input username and password.
 */
public class HttpAuthenticator extends Authenticator 
{
    public static final boolean debug = false;

    private static Logger _logger = Logger.getLogger(HttpAuthenticator.class.getName());

    private final AppClientSecurityInfo.CredentialType loginType;
    private final AppClientSecurityInfo securityInfo;

    /**
     * Create the authenticator.
     */
    public HttpAuthenticator(final AppClientSecurityInfo secInfo,
            final AppClientSecurityInfo.CredentialType loginType) {
        this.securityInfo = secInfo;
        this.loginType = loginType;
    }

    /**
     * This is called when authentication is needed for a protected
     * web resource. It looks for the authentication data in the subject.
     * If the data is not found then login is invoked on the login context.
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() 
    {
	String user = null;
	char[] password = null;
	Subject subject = null;

	String scheme = getRequestingScheme();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("scheme=" + scheme);
            _logger.fine("requesting prompt=" + getRequestingPrompt());
            _logger.fine("requesting protocol=" + getRequestingProtocol());
        }

	ClientSecurityContext cont = ClientSecurityContext.getCurrent();
	subject = (cont != null) ? cont.getSubject() : null;
	user = getUserName(subject);
	password = getPassword(subject);
	if(user == null || password == null) {
	    try {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Initiating login again...");
                }
                
		securityInfo.doClientLogin(
                loginType);
		cont = ClientSecurityContext.getCurrent();
		subject = cont.getSubject();
		user = getUserName(subject);
		password = getPassword(subject);
	    } catch(Exception e) {
                _logger.log(Level.FINE, "Exception " + e.toString(), e);
	        return null;
	    }
	}
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Username:" + user);
        }
	return new PasswordAuthentication(user, password);
    }

    /**
     * Return the username from the subject.
     */
    private String getUserName(Subject s) {
	String user = null; 
	if(s == null)
	    return null;
	Set principalSet = s.getPrincipals();
	Iterator itr = principalSet.iterator();
	if(itr.hasNext()) {
	    Principal p = (Principal) itr.next();
	    user = p.getName();
	}
	return user;
    }

    /**
     * Return the password for the subject.
     */
    private char[] getPassword(Subject s) {
	char[] password = null;
	if(s == null)
	    return null;
	Set credentials = s.getPrivateCredentials();
	Iterator credIter = credentials.iterator();
	if(credIter.hasNext()) {
	    Object o = credIter.next();
	    if(o instanceof PasswordCredential) {
		PasswordCredential pc = (PasswordCredential) o;
		// CHECK REALM.
	        password = pc.getPassword();
	    }
	}
	return password;
    }
}

