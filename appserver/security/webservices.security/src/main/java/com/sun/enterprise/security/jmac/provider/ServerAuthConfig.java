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

package com.sun.enterprise.security.jmac.provider; 

import com.sun.enterprise.security.jauth.*;
import java.util.ArrayList;

import javax.security.auth.callback.CallbackHandler;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityDescriptor;
import com.sun.enterprise.deployment.runtime.common.MessageSecurityBindingDescriptor;

//import com.sun.xml.rpc.spi.runtime.StreamingHandler;

import com.sun.xml.rpc.spi.runtime.StreamingHandler;
import javax.xml.soap.SOAPMessage;

/**
 * This class is the client container's interface to the AuthConfig subsystem
 * to get AuthContext objects on which to invoke message layer authentication
 * providers. It is not intended to be layer or web services specific (see
 * getMechanisms method at end).
 */
public class ServerAuthConfig extends BaseAuthConfig {

    private ServerAuthConfig(ServerAuthContext defaultContext) {
	super(defaultContext);
    }

    private ServerAuthConfig (ArrayList descriptors, ArrayList authContexts) {
	super(descriptors,authContexts);
    }

    public static ServerAuthConfig getConfig
	(String authLayer, MessageSecurityBindingDescriptor binding, 
	 CallbackHandler cbh) throws AuthException {
	ServerAuthConfig rvalue = null;
	String provider = null;
	ArrayList descriptors = null;
	ServerAuthContext defaultContext = null;
	if (binding != null) {
	    String layer = binding.getAttributeValue
		(MessageSecurityBindingDescriptor.AUTH_LAYER);
	    if (authLayer != null && layer.equals(authLayer)) {
		provider = binding.getAttributeValue
		    (MessageSecurityBindingDescriptor.PROVIDER_ID);
		descriptors = binding.getMessageSecurityDescriptors();
	    }
	}
	if (descriptors == null || descriptors.size() == 0) {
	    defaultContext = getAuthContext(authLayer,provider,null,null,cbh);
	    if (defaultContext != null) {
		rvalue = new ServerAuthConfig(defaultContext);
	    }
	} else {
	    boolean hasPolicy = false;
	    ArrayList authContexts = new ArrayList();
	    for (int i = 0; i < descriptors.size(); i++) {
		MessageSecurityDescriptor msd = 
		    (MessageSecurityDescriptor) descriptors.get(i);
		AuthPolicy requestPolicy = 
		    getAuthPolicy(msd.getRequestProtectionDescriptor());
		AuthPolicy responsePolicy = 
		    getAuthPolicy(msd.getResponseProtectionDescriptor());
 		if (requestPolicy.authRequired()||responsePolicy.authRequired()) {
		    authContexts.add
			(getAuthContext
			 (authLayer,provider,requestPolicy,responsePolicy,cbh));
		    hasPolicy = true;
		} else {
		    authContexts.add(null);
		}
	    }
	    if (hasPolicy) {
		rvalue = new ServerAuthConfig(descriptors,authContexts);
	    }
	}
	return rvalue;
    }

    private static ServerAuthContext getAuthContext 
	(String layer, String provider, AuthPolicy requestPolicy, 
	 AuthPolicy responsePolicy,CallbackHandler cbh) throws AuthException {
	AuthConfig authConfig = AuthConfig.getAuthConfig();
	return authConfig.getServerAuthContext
	    (layer,provider,requestPolicy,responsePolicy,cbh);
    }

   public ServerAuthContext 
	getAuthContext(StreamingHandler handler, SOAPMessage message) {
	return (ServerAuthContext) getContext(handler,message);
    }

    public ServerAuthContext getAuthContext
	(javax.xml.ws.handler.soap.SOAPMessageContext context) {
	return (ServerAuthContext) getContext(context);
    }

   public ServerAuthContext 
	getAuthContextForOpCode(StreamingHandler handler, int opcode) throws
	    ClassNotFoundException, NoSuchMethodException {
	return (ServerAuthContext) getContextForOpCode(handler,opcode);
    }

}
