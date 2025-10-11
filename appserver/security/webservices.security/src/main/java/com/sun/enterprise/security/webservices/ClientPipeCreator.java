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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2022] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.webservices;

import static com.sun.enterprise.security.webservices.PipeConstants.ASSEMBLER_CONTEXT;
import static com.sun.enterprise.security.webservices.PipeConstants.BINDING;
import static com.sun.enterprise.security.webservices.PipeConstants.CONTAINER;
import static com.sun.enterprise.security.webservices.PipeConstants.ENDPOINT_ADDRESS;
import static com.sun.enterprise.security.webservices.PipeConstants.NEXT_PIPE;
import static com.sun.enterprise.security.webservices.PipeConstants.POLICY;
import static com.sun.enterprise.security.webservices.PipeConstants.SERVICE;
import static com.sun.enterprise.security.webservices.PipeConstants.SERVICE_REF;
import static com.sun.enterprise.security.webservices.PipeConstants.WSDL_MODEL;

import java.util.HashMap;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.jaspic.services.AuthConfigRegistrationWrapper;
import com.sun.xml.ws.api.pipe.ClientPipeAssemblerContext;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.assembler.metro.dev.ClientPipelineHook;
import com.sun.xml.ws.policy.PolicyMap;

/**
 * This is used by WSClientContainer to return proper JASPIC 196 security pipe to the
 * StandAlonePipeAssembler and TangoPipeAssembler
 */
public class ClientPipeCreator extends ClientPipelineHook {

    private ServiceReferenceDescriptor svcRef = null;

    public ClientPipeCreator() {
    }

    public ClientPipeCreator(ServiceReferenceDescriptor ref) {
        svcRef = ref;
    }

    @Override
    public Pipe createSecurityPipe(PolicyMap map, ClientPipeAssemblerContext ctxt, Pipe tail) {
        HashMap<String, Object> props = new HashMap<>();
        
        props.put(POLICY, map);
        props.put(WSDL_MODEL, ctxt.getWsdlModel());
        props.put(SERVICE, ctxt.getService());
        props.put(BINDING, ctxt.getBinding());
        props.put(ENDPOINT_ADDRESS, ctxt.getAddress());
        if (svcRef != null) {
            props.put(SERVICE_REF, svcRef);
        }
        props.put(NEXT_PIPE, tail);
        props.put(CONTAINER, ctxt.getContainer());
        props.put(ASSEMBLER_CONTEXT, ctxt);
        ClientSecurityPipe ret = new ClientSecurityPipe(props, tail);
        
        AuthConfigRegistrationWrapper listenerWrapper = ClientPipeCloser.getInstance().lookupListenerWrapper(svcRef);
        // there is a 1-1 mapping between Service_Ref and a ListenerWrapper
        if (listenerWrapper != null) {
            // override the listener that was created by the ConfigHelper CTOR :if one was already registered
            listenerWrapper.incrementReference();
            ret.getPipeHelper().setRegistrationWrapper(listenerWrapper);
        } else {
            // register a new listener
            ClientPipeCloser.getInstance().registerListenerWrapper(svcRef, ret.getPipeHelper().getRegistrationWrapper());
        }

        return ret;
    }

//    @Override
//    public @NotNull
//    Tube createSecurityTube(ClientTubelineAssemblyContext ctxt) {
//       
//        
//        HashMap propBag = new HashMap();
//        /*TODO V3 enable
//        propBag.put(PipeConstants.POLICY, map);
//        propBag.put(PipeConstants.WSDL_MODEL, ctxt.getWsdlModel());
//        propBag.put(PipeConstants.SERVICE, ctxt.getService());
//        propBag.put(PipeConstants.BINDING, ctxt.getBinding());
//        propBag.put(PipeConstants.ENDPOINT_ADDRESS, ctxt.getAddress());
//        propBag.put(PipeConstants.SERVICE_REF, svcRef);
//	propBag.put(PipeConstants.NEXT_PIPE,tail);
//        propBag.put(PipeConstants.CONTAINER,ctxt.getContainer());
//         */
//        ClientSecurityTube ret = new ClientSecurityTube(propBag, ctxt.getTubelineHead());
//        AuthConfigRegistrationWrapper listenerWrapper = ClientPipeCloser.getInstance().lookupListenerWrapper(svcRef);
//        //there is a 1-1 mapping between Service_Ref and a ListenerWrapper
//        if (listenerWrapper != null) {
//            //override the listener that was created by the ConfigHelper CTOR :if one was already registered
//            listenerWrapper.incrementReference();
//            ret.getPipeHelper().setRegistrationWrapper(listenerWrapper);
//        } else {
//            //register a new listener
//            ClientPipeCloser.getInstance().registerListenerWrapper(
//                    svcRef, ret.getPipeHelper().getRegistrationWrapper());
//        }
//  
//        return ret;
//        
//    }

}
