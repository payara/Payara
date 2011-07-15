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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.util.DOLUtils;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This descriptor holds the security configuration of an EJB IOR.
 *
 */

public class EjbIORConfigurationDescriptor implements Serializable {
    // For backward compatiblity with CTS, equalsIgnoreCase should be used
    // for comparison with NONE, SUPPORTED, REQUIRED.
    public static final String NONE = "NONE";
    public static final String SUPPORTED = "SUPPORTED";
    public static final String REQUIRED = "REQUIRED";

    public static final String USERNAME_PASSWORD = "username_password";
    public static final String DEFAULT_REALM = "default";

    private String integrity = SUPPORTED;
    private String confidentiality = SUPPORTED;
    private String establishTrustInTarget = SUPPORTED;
    private String establishTrustInClient = SUPPORTED;
    private String authenticationMethod = USERNAME_PASSWORD;
    private String realmName = DEFAULT_REALM;
    private String callerPropagation = SUPPORTED;
    private boolean required = false;
       
    static Logger _logger = DOLUtils.getDefaultLogger();
    
    /** 
    * Default constructor. 
    */
    public EjbIORConfigurationDescriptor() {
	try {	
	    if (Boolean.getBoolean("interop.ssl.required")) {
		integrity = REQUIRED;
		confidentiality = REQUIRED;
		establishTrustInClient = REQUIRED;
		establishTrustInTarget = SUPPORTED;
	    }

            if (Boolean.getBoolean("interop.authRequired.enabled")) {
		required = true;
		authenticationMethod = USERNAME_PASSWORD;
	    }
	} catch(Throwable ioe) {
         _logger.log(Level.WARNING,"enterprise.deployment_ioexcp",ioe);
            
	    // ignore
	}
    }

    public EjbIORConfigurationDescriptor(boolean enableUsernamePassword) {
	if(enableUsernamePassword) {
	    required = true;
	    authenticationMethod = USERNAME_PASSWORD;
	}
    }

    /**
     * Get the value of the integrity element. Default value is "supported".
     * @return the value (one of supported, required, none).
     */
    public String getIntegrity() {
	return integrity;
    }

    /**
     * Set the value of the integrity element to the specified value.
     * @param the value (one of supported, required, none).
     */
    public void setIntegrity(String val) {
	if(!val.equalsIgnoreCase(NONE) && !val.equalsIgnoreCase(SUPPORTED) && 
	    !val.equalsIgnoreCase(REQUIRED)) {
	    throw new RuntimeException("Incorrect value for integrity:" + val);
	}

	integrity = val;
    }

    /**
     * Get the value of the confidentiality element. 
     * Default value is "supported".
     * @return the value (one of supported, required, none).
     */
    public String getConfidentiality() {
	return confidentiality;
    }

    /**
     * Set the value of the confidentiality element to the specified value.
     * @param the value (one of supported, required, none).
     */
    public void setConfidentiality(String val) {
	if(!val.equalsIgnoreCase(NONE) && !val.equalsIgnoreCase(SUPPORTED) && 
	    !val.equalsIgnoreCase(REQUIRED)) {
	    throw new RuntimeException("Incorrect value for confidentiality:" +
					val);
	}
	confidentiality = val;
    }
    
    /**
     * Get the value of establishTrustInTarget in the transport layer.
     * The default value is "supported".
     * @return the value (required, supported, or none)
     */
    public String getEstablishTrustInTarget() {
	return establishTrustInTarget;
    }

    /**
     * Set the value of establishTrustInTarget in the transport layer.
     * @param the value (required, supported, or none)
     */
    public void setEstablishTrustInTarget(String val) {
	if(!val.equalsIgnoreCase(NONE) && !val.equalsIgnoreCase(SUPPORTED)) {
	    throw new RuntimeException("Incorrect value for " + 
	        "establishTrustInTarget:" + val);
	}

	establishTrustInTarget = val;
    }

    /**
     * Get the value of establishTrustInClient in the transport layer.
     * The default value is "supported".
     * @return the value (required, supported, or none)
     */
    public String getEstablishTrustInClient() {
	return establishTrustInClient;
    }

    /**
     * Set the value of establishTrustInClient in the transport layer.
     * @param the value (required, supported, or none)
     */
    public void setEstablishTrustInClient(String val) {
	if(!val.equalsIgnoreCase(NONE) && !val.equalsIgnoreCase(SUPPORTED) && 
	    !val.equalsIgnoreCase(REQUIRED)) {
	    throw new RuntimeException("Incorrect value for " + 
	    	"establishTrustInClient:" + val);
	}

	establishTrustInClient = val;
    }

    /**
     * Return the authentication method used to authenticate clients.
     * The default value is "username_password".
     * @return the authentication method.
     */
    public String getAuthenticationMethod() {
	return authenticationMethod;
    }

    /**
     * Set the authentication method used to authenticate clients.
     * @param the authentication method.
     */
    public void setAuthenticationMethod(String val) {
	if(!val.equalsIgnoreCase(USERNAME_PASSWORD) && !val.equalsIgnoreCase(NONE)) {
	    throw new RuntimeException("Incorrect value for " + 
	    	"authentication method:" + val);
	}
	authenticationMethod = val;
    }

    /**
     * Return the realm name to authenticate the caller in.
     * The default value is "default".
     * @return the realm name.
     */
    public String getRealmName() {
	return realmName;
    }

    /**
     * Set the realm name to authenticate the caller in.
     * @param the realm name.
     */
    public void setRealmName(String val) {
	realmName = val; 
    }

    /**
     * Return the value of identity assertion in the SAS_Context layer.
     * @return the value (one of none, required or supported).
     */
    public String getCallerPropagation() {
	return callerPropagation;
    }

    /**
     * Set the value of identity assertion in the SAS_Context layer.
     * @param the value (one of none, required or supported).
     */
    public void setCallerPropagation(String val) {
	if(!val.equalsIgnoreCase(NONE) && !val.equalsIgnoreCase(SUPPORTED) && 
	    !val.equalsIgnoreCase(REQUIRED)) {
	    throw new RuntimeException("Incorrect value for callerPropagation:" + val);
	}
	callerPropagation = val;
    }

    /**
     * Get whether the establishTrustInClient element is required
     * in the AS_context.
     * @return the value (true or false).
     */
    public boolean isAuthMethodRequired() {
	return required;
    }

    /**
     * Set whether the establishTrustInClient element should be required
     * in the AS_context.
     * @param the value (true or false).
     */
    public void setAuthMethodRequired(boolean val) {
	required = val;
    }
    

    /**
     * Set whether the establishTrustInClient element should be required
     * in the AS_context.
     * @param the value (true or false).
     */
    public void setAuthMethodRequired(String val) {
	required = Boolean.valueOf(val).booleanValue();
    }    

    /**
    * Returns a formatted String of the attributes of this object.
    */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("\n integrity ").append(integrity);
	toStringBuffer.append( "\n confidentiality " ).append( confidentiality);
	toStringBuffer.append( "\n establishTrustInTarget ").append(establishTrustInTarget);
	toStringBuffer.append( "\n establishTrustInClient ").append(establishTrustInClient);
	toStringBuffer.append( "\n callerPropagation ").append(callerPropagation);
	toStringBuffer.append( "\n realm ").append(realmName);
	toStringBuffer.append( "\n authenticationMethod ").append(authenticationMethod).append("\n");
    }
}
    
