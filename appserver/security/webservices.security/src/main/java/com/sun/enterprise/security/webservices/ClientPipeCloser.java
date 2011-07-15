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

package com.sun.enterprise.security.webservices;

import java.util.Collections;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.security.jmac.config.ConfigHelper.AuthConfigRegistrationWrapper;
import java.util.Map;
import java.util.WeakHashMap;
import javax.security.auth.message.config.RegistrationListener;

public class ClientPipeCloser {
    
    private Map<ServiceReferenceDescriptor, AuthConfigRegistrationWrapper> svcRefListenerMap = 
        Collections.synchronizedMap(new WeakHashMap<ServiceReferenceDescriptor, AuthConfigRegistrationWrapper>());

    private ClientPipeCloser() {}
    
    private static final ClientPipeCloser INSTANCE = new ClientPipeCloser();
    
    public static  ClientPipeCloser getInstance() {
        return INSTANCE;
    }
    
    public void registerListenerWrapper(ServiceReferenceDescriptor desc, AuthConfigRegistrationWrapper wrapper) {
        svcRefListenerMap.put(desc,wrapper);
    }
    
    public AuthConfigRegistrationWrapper lookupListenerWrapper(ServiceReferenceDescriptor desc) {
        AuthConfigRegistrationWrapper listenerWrapper = (AuthConfigRegistrationWrapper) svcRefListenerMap.get(desc);
        return listenerWrapper;
    }
    
    public void removeListenerWrapper(AuthConfigRegistrationWrapper wrapper) {
       ServiceReferenceDescriptor entryToRemove = null;
       
       for (ServiceReferenceDescriptor svc : svcRefListenerMap.keySet()) {
           AuthConfigRegistrationWrapper wrp = svcRefListenerMap.get(svc);
           if (wrp == wrapper) {
              entryToRemove = svc;  
              break;
           }
       }
       if (entryToRemove != null) {
          svcRefListenerMap.remove(entryToRemove);
       }
    }
    
    public void cleanupClientPipe(ServiceReferenceDescriptor desc) {
        AuthConfigRegistrationWrapper listenerWrapper = (AuthConfigRegistrationWrapper) svcRefListenerMap.get(desc);
        if (listenerWrapper != null) {
            listenerWrapper.disable();
        }
        svcRefListenerMap.remove(desc);
    }
}
