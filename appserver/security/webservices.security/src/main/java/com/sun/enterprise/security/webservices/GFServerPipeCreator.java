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

package com.sun.enterprise.security.webservices;

import java.util.HashMap;


import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.policy.PolicyMap;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.PolicyMapKey;

import com.sun.xml.wss.provider.wsit.PipeConstants;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;


/**
 * This is used by JAXWSContainer to return proper 196 security and
 *  app server monitoing pipes to the StandAlonePipeAssembler and 
 *  TangoPipeAssembler
 */
@Service
@Singleton
public class GFServerPipeCreator extends org.glassfish.webservices.ServerPipeCreator {
    
    private static final String SECURITY_POLICY_NAMESPACE_URI_SUBMISSION = 
            "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy";
    private static final String SECURITY_POLICY_NAMESPACE_URI_SPECVERSION= 
            "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702";       
    
    public GFServerPipeCreator(){
        super();
    }
    
    public void init(WebServiceEndpoint ep) {
        super.init(ep);
    }
    public Pipe createSecurityPipe(PolicyMap map, SEIModel sei,
            WSDLPort port, WSEndpoint owner, Pipe tail) {

	HashMap props = new HashMap();

	props.put(PipeConstants.POLICY,map);
	props.put(PipeConstants.SEI_MODEL,sei);
	props.put(PipeConstants.WSDL_MODEL,port);
	props.put(PipeConstants.ENDPOINT,owner);
	props.put(PipeConstants.SERVICE_ENDPOINT,endpoint);
	props.put(PipeConstants.NEXT_PIPE,tail);
        props.put(PipeConstants.CONTAINER, owner.getContainer());
        if (isSecurityEnabled(map, port)) {
		endpoint.setSecurePipeline();
        }

        return new CommonServerSecurityPipe(props, tail, isHttpBinding);
    }    
    
//    @Override
//    public @NotNull
//    Tube createSecurityTube(ServerTubelineAssemblyContext ctxt) {
//        HashMap props = new HashMap();
//
//        /*TODO V3 enable
//	props.put(PipeConstants.POLICY,map);
//	props.put(PipeConstants.SEI_MODEL,sei);
//	props.put(PipeConstants.WSDL_MODEL,port);
//	props.put(PipeConstants.ENDPOINT,owner);
//	props.put(PipeConstants.SERVICE_ENDPOINT,endpoint);
//	props.put(PipeConstants.NEXT_PIPE,tail);
//        props.put(PipeConstants.CONTAINER, owner.getContainer());
//        if (isSecurityEnabled(map, port)) {
//		endpoint.setSecurePipeline();
//        }*/
//
//        return new CommonServerSecurityTube(props, ctxt.getTubelineHead(), isHttpBinding);
//    }
    /**
     * Checks to see whether WS-Security is enabled or not.
     *
     * @param policyMap policy map for {@link this} assembler
     * @param wsdlPort wsdl:port
     * @return true if Security is enabled, false otherwise
     */
    //TODO - this code has been copied from PipelineAssemblerFactoryImpl.java and needs
    //to be maintained in both places.  In the future, code needs to be moved somewhere
    //where it can be invoked from both places.
    public static  boolean isSecurityEnabled(PolicyMap policyMap, WSDLPort wsdlPort) {
        if (policyMap == null || wsdlPort == null)
            return false;

        try {
            PolicyMapKey endpointKey = policyMap.createWsdlEndpointScopeKey(wsdlPort.getOwner().getName(),
                    wsdlPort.getName());
            Policy policy = policyMap.getEndpointEffectivePolicy(endpointKey);
            
            if ((policy != null) && (policy.contains(SECURITY_POLICY_NAMESPACE_URI_SPECVERSION) ||
                    policy.contains(SECURITY_POLICY_NAMESPACE_URI_SUBMISSION))) {
                return true;
            }

            for (WSDLBoundOperation wbo : wsdlPort.getBinding().getBindingOperations()) {
                PolicyMapKey operationKey = policyMap.createWsdlOperationScopeKey(wsdlPort.getOwner().getName(),
                        wsdlPort.getName(),
                        wbo.getName());
                policy = policyMap.getOperationEffectivePolicy(operationKey);
                if ((policy != null) && (policy.contains(SECURITY_POLICY_NAMESPACE_URI_SPECVERSION) ||
                    policy.contains(SECURITY_POLICY_NAMESPACE_URI_SUBMISSION)))
                    return true;

                policy = policyMap.getInputMessageEffectivePolicy(operationKey);
                if ((policy != null) && (policy.contains(SECURITY_POLICY_NAMESPACE_URI_SPECVERSION) ||
                    policy.contains(SECURITY_POLICY_NAMESPACE_URI_SUBMISSION)))
                    return true;

                policy = policyMap.getOutputMessageEffectivePolicy(operationKey);
                if ((policy != null) && (policy.contains(SECURITY_POLICY_NAMESPACE_URI_SPECVERSION) ||
                    policy.contains(SECURITY_POLICY_NAMESPACE_URI_SUBMISSION)))
                    return true;

                policy = policyMap.getFaultMessageEffectivePolicy(operationKey);
                if ((policy != null) && (policy.contains(SECURITY_POLICY_NAMESPACE_URI_SPECVERSION) ||
                    policy.contains(SECURITY_POLICY_NAMESPACE_URI_SUBMISSION)))
                    return true;
            }
        } catch (PolicyException e) {
            return false;
        }

        return false;
    }

}
