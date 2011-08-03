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

package com.sun.enterprise.security.deployment;

import com.sun.enterprise.deployment.PoolManagerConstants;
import com.sun.enterprise.deployment.xml.ConnectorTagNames;
import org.glassfish.deployment.common.Descriptor;

/**
 * This class encapsulates the xml tags: description, auth-mech-type and
 * credential-interface in the connector specification.
 * @author Sheetal Vartak
 */
public class AuthMechanism extends Descriptor {

    private int authMechVal;
    private String credInterface;

    /**
     * Default constructor.
     */
    public AuthMechanism(){}

    /**
     * Initializes the data members.
     * @param description description
     * @param authMechVal authentication mechanism type.
     * @param credInterface credential interface type.
     */
    public AuthMechanism(String description, int authMechVal,
                         String credInterface) {
        super.setDescription(description);
      	this.authMechVal = authMechVal;
        this.credInterface = credInterface;
    }

    /**
     * Set the credential interface.
     * @param cred the interface.
     */
    public void setCredentialInterface(String cred) {
        credInterface = cred;
    }

    /**
     * Get the credential interface.
     * @return credInterface the interface.
     */
    public String getCredentialInterface() {
        return credInterface;
    }

   /** 
    * Get the description
    * @return description.
    */
    public String getDescription(){
        return super.getDescription();
    }

    /** 
     * Sets the description
     * @param description.
     */
    public void setDescription(String description){
        super.setDescription(description);
    }

   /** 
    * Get the auth-mech-type
    * @return authMechVal the authentication mechanism type
    */
    public String getAuthMechType() {
        if(authMechVal == PoolManagerConstants.BASIC_PASSWORD)
	    return ConnectorTagNames.DD_BASIC_PASSWORD;
        else
            return ConnectorTagNames.DD_KERBEROS;
    }

    public static int getAuthMechInt(String value){
        if(value.trim().equals(ConnectorTagNames.DD_BASIC_PASSWORD)){
            return PoolManagerConstants.BASIC_PASSWORD;
        }else if((value.trim()).equals(ConnectorTagNames.DD_KERBEROS)){
	        return PoolManagerConstants.KERBV5;
        }else{
            throw new IllegalArgumentException("Invalid auth-mech-type");// put this in localStrings...
        }
    }
    
    /**
     * Get the authentication mechanism value.
     */
    public int getAuthMechVal() { 
        return authMechVal;
    }

    /**
     * Set the authentication mechanism value.
     */
    public void setAuthMechVal(int value) { 
        authMechVal = value;
    }

    /**
     * Set the authentication mechanism value.
     */
    public void setAuthMechVal(String value) { 
        if((value.trim()).equals(ConnectorTagNames.DD_BASIC_PASSWORD))
	    authMechVal = PoolManagerConstants.BASIC_PASSWORD;
        else if((value.trim()).equals(ConnectorTagNames.DD_KERBEROS))
	    authMechVal = PoolManagerConstants.KERBV5;
	else throw new IllegalArgumentException("Invalid auth-mech-type");// put this in localStrings...
    }
}
