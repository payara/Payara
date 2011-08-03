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

import com.sun.enterprise.deployment.web.UserDataConstraint;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;


/**
    * I represent the information about how the web application's data should be protected. 
    *
    * @author Danny Coward
    */
    
public class UserDataConstraintImpl extends Descriptor implements UserDataConstraint {
    /** The transport is unspecified.*/
    public static final String TRANSPORT_GUARANTEE_NONE = UserDataConstraint.NONE_TRANSPORT;
    /** HTTP.*/
    public static final String TRANSPORT_GUARANTEE_INTEGRAL = UserDataConstraint.INTEGRAL_TRANSPORT;
    /** HTTPS */
    public static final String TRANSPORT_GUARANTEE_CONFIDENTIAL = UserDataConstraint.CONFIDENTIAL_TRANSPORT;

    /** JACC Specific **/
    public static final String TRANSPORT_GUARANTEE_CLEAR = UserDataConstraint.CLEAR;
    private static final String[] transportGuaranteeChoices = {
	TRANSPORT_GUARANTEE_NONE,
	TRANSPORT_GUARANTEE_INTEGRAL,
	TRANSPORT_GUARANTEE_CONFIDENTIAL,
    };
    private String transportGuarantee;
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(UserDataConstraintImpl.class);

    /**
    * Return a String array of my static transport types.
    */
    public static final String[] getTransportGuaranteeChoices() {
	return  transportGuaranteeChoices;
    }
    
    /**
    * Return my transport type.
    */
    public String getTransportGuarantee() {
	if (transportGuarantee == null) {
	   transportGuarantee = TRANSPORT_GUARANTEE_NONE;
	}
	return transportGuarantee;
    }

    public String[] getUnacceptableTransportGuarantees(){
	String acceptable = getTransportGuarantee();
	if(acceptable.equals(TRANSPORT_GUARANTEE_NONE))
	   return (String[]) null;
	else if (acceptable.equals(TRANSPORT_GUARANTEE_INTEGRAL)){
	    String[] ret = new String[] {TRANSPORT_GUARANTEE_CLEAR,  TRANSPORT_GUARANTEE_CONFIDENTIAL };
	    return ret;
	} else if (acceptable.equals(TRANSPORT_GUARANTEE_CONFIDENTIAL)){
	    String[] ret = new String[] {TRANSPORT_GUARANTEE_CLEAR,  TRANSPORT_GUARANTEE_INTEGRAL };
	    return ret;
	}
	return (String[]) null;
    }
    /**
    * Sets my transport type to the given value. Throws an illegal argument exception
    * if the value is not allowed.
    */
    public void setTransportGuarantee(String transportGuarantee) {
	if (this.isBoundsChecking()) {
	    if ( !UserDataConstraint.NONE_TRANSPORT.equals(transportGuarantee)
		&& !UserDataConstraint.INTEGRAL_TRANSPORT.equals(transportGuarantee)
		    && !UserDataConstraint.CONFIDENTIAL_TRANSPORT.equals(transportGuarantee)) {
		throw new IllegalArgumentException(localStrings.getLocalString(
									       "enterprise.deployment.exceptiontransportguarentee",
									       "{0} is not a valid transport guarantee", new Object[] {transportGuarantee}));  
	    }
	}
	this.transportGuarantee = transportGuarantee;
    }
    
    /**
    * Returns a formatted String of my state.
    */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("UserDataConstraint ");
	toStringBuffer.append(" description ").append(super.getDescription());
	toStringBuffer.append(" transportGuarantee ").append(getTransportGuarantee());
    }
}
