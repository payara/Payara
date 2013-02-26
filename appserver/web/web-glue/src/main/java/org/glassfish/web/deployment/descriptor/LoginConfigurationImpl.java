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

package org.glassfish.web.deployment.descriptor;


import com.sun.enterprise.deployment.web.LoginConfiguration;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
//END OF IASRI 4660482 


/**
 * I dictate how the web app I belong to should be logged into.
 * @author Danny Coward
 */

public class LoginConfigurationImpl extends Descriptor implements LoginConfiguration {

    static final Logger _logger = com.sun.enterprise.web.WebContainer.logger;

    @LogMessageInfo(
            message = "An authentication method was not defined in the web.xml descriptor. " +
                    "Using default BASIC for login configuration.",
            level = "WARNING")
    public static final String AUTH_METHOD_NOT_FOUND = "AS-WEB-GLUE-00277";

    @LogMessageInfo(
            message = "[{0}] is not a valid authentication method",
            level = "WARNING")
    public static final String EXCEPTION_AUTH_METHOD = "AS-WEB-GLUE-00278";

    /** teh client authenticates using http basic authentication. */
    public static final String AUTHENTICATION_METHOD_BASIC = LoginConfiguration.BASIC_AUTHENTICATION;
    /** Digest authentication. */
    public static final String AUTHENTICATION_METHOD_DIGEST = LoginConfiguration.DIGEST_AUTHENTICATION;
    /** FOrm authentication. */
    public static final String AUTHENTICATION_METHOD_FORM = LoginConfiguration.FORM_AUTHENTICATION;
    /** The client sends a certificate. */
    public static final String AUTHENTICATION_METHOD_CLIENT_CERTIFICATE = LoginConfiguration.CLIENT_CERTIFICATION_AUTHENTICATION;

    private String authenticationMethod;
    private String realmName = "";
    private String formLoginPage = "";
    private String formErrorPage = "";
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(LoginConfigurationImpl.class);

    /** Return my authentication method. */
    public String getAuthenticationMethod() {
	if (this.authenticationMethod == null) {
            //START OF IASRI 4660482 - warning log if authentication method isn't defined in descriptor
            _logger.log(Level.WARNING, AUTH_METHOD_NOT_FOUND);
            //END OF IASRI 4660482 
	    this.authenticationMethod = AUTHENTICATION_METHOD_BASIC;
	}
	return this.authenticationMethod;
    }

    /** Sets my authentication method. */
    public void setAuthenticationMethod(String authenticationMethod) {
	
	if ( this.isBoundsChecking() )  {
	
	    if (!LoginConfiguration.BASIC_AUTHENTICATION.equals(authenticationMethod)
		&& !LoginConfiguration.DIGEST_AUTHENTICATION.equals(authenticationMethod)
		    && !LoginConfiguration.FORM_AUTHENTICATION.equals(authenticationMethod)
			&& !LoginConfiguration.CLIENT_CERTIFICATION_AUTHENTICATION.equals(authenticationMethod) ) {

            throw new IllegalArgumentException(
                    MessageFormat.format(
                            _logger.getResourceBundle().getString(EXCEPTION_AUTH_METHOD),
                            authenticationMethod));
		
	    }
	}
	this.authenticationMethod = authenticationMethod;
	
    }

    /** Obtain the realm the server should use for basic authentication. */
    public String getRealmName() {
	if (this.realmName == null) {
	    this.realmName = "";
	}
	return this.realmName;
    }
    
    /** Set the realm the server should use for basic authentication. */
    public void setRealmName(String realmName) {
	this.realmName = realmName;
    }
    
    /** Get the name of the login page for form login. */
    public String getFormLoginPage() {
	if (this.formLoginPage == null) {
	    this.formLoginPage = "";
	}
	return this.formLoginPage;
    }
     /** Set the name of the login page for form login. */
    public void setFormLoginPage(String formLoginPage) {
	this.formLoginPage = formLoginPage;
    }
    
    /** Get the name of the error page for form login. */
    public String getFormErrorPage() {
	if (this.formErrorPage == null) {
	    this.formErrorPage = "";
	}	
	return this.formErrorPage;
    }
    /** Set the name of the error page for form login. */
    public void setFormErrorPage(String formErrorPage) {
	this.formErrorPage = formErrorPage;
    }

    /** My representation as a formatted String.*/
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("LoginConfig:(").append(authenticationMethod).append(" ").append(
            realmName).append(" ").append(formLoginPage).append(" ").append(formErrorPage).append(")");
    }

    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof LoginConfigurationImpl) {
            LoginConfigurationImpl other = (LoginConfigurationImpl)obj;
            result = getAuthenticationMethod().equals(other.getAuthenticationMethod()) &&
                    getRealmName().equals(other.getRealmName()) &&
                    getFormLoginPage().equals(other.getFormLoginPage()) &&
                    getFormErrorPage().equals(other.getFormErrorPage());
        } 
        return result;
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + getAuthenticationMethod().hashCode();
        
        if (getRealmName().length() > 0) {
            hashCode = 31 * hashCode + getRealmName().hashCode();
        }
        if (getFormLoginPage().length() > 0) {
            hashCode = 31 * hashCode + getFormLoginPage().hashCode();
        }
        if (getFormErrorPage().length() > 0) {
            hashCode = 31 * hashCode + getFormErrorPage().hashCode();
        }
        return hashCode;
    }
}
