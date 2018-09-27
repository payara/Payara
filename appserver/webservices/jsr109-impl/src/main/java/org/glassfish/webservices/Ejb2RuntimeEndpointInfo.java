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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package org.glassfish.webservices;

import java.rmi.Remote;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.ejb.api.EJBInvocation;
import org.glassfish.ejb.api.EjbEndpointFacade;
import org.glassfish.internal.api.Globals;

import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.xml.rpc.spi.runtime.Tie;

/**
 * Runtime dispatch information about one ejb web service endpoint. This class must support
 * concurrent access, since a single instance will be used for all web service invocations through
 * the same ejb endpoint.
 *
 * @author Kenneth Saks
 */
public class Ejb2RuntimeEndpointInfo extends EjbRuntimeEndpointInfo {

    private Class tieClass;

    // Lazily instantiated and cached due to overhead of initialization.
    private Tie tieInstance;

    private Object serverAuthConfig;

    public Ejb2RuntimeEndpointInfo(WebServiceEndpoint webServiceEndpoint, EjbEndpointFacade ejbContainer, Object servant, Class tie) {

        super(webServiceEndpoint, ejbContainer, servant);
        tieClass = tie;

        if (Globals.getDefaultHabitat() != null) {
            SecurityService securityService = Globals.get(SecurityService.class);
            if (securityService != null) {
                serverAuthConfig = securityService.mergeSOAPMessageSecurityPolicies(webServiceEndpoint.getMessageSecurityBinding());
            }
        }

    }

    public AdapterInvocationInfo getHandlerImplementor() throws Exception {

        ComponentInvocation invocation = container.startInvocation();
        AdapterInvocationInfo adapterInvocationInfo = new AdapterInvocationInfo();
        adapterInvocationInfo.setInv(invocation);

        synchronized (this) {
            if (tieClass == null) {
                tieClass = Thread.currentThread().getContextClassLoader().loadClass(getEndpoint().getTieClassName());
            }
            if (tieInstance == null) {
                tieInstance = (Tie) tieClass.newInstance();
                tieInstance.setTarget((Remote) webServiceEndpointServant);
            }
        }

        EJBInvocation.class.cast(invocation).setWebServiceTie(tieInstance);
        adapterInvocationInfo.setHandler(tieInstance);

        return adapterInvocationInfo;
    }

    /**
     * Called after attempt to handle message. This is coded defensively so we attempt to clean up no
     * matter how much progress we made in getImplementor. One important thing is to complete the
     * invocation manager preInvoke().
     */
    @Override
    public void releaseImplementor(ComponentInvocation inv) {
        container.endInvocation(inv);
    }

    @Override
    public EjbMessageDispatcher getMessageDispatcher() {
        // message dispatcher is stateless, no need to synchronize, worse
        // case, we'll create too many.
        if (messageDispatcher == null) {
            messageDispatcher = new EjbWebServiceDispatcher();
        }

        return messageDispatcher;
    }

    public Object getServerAuthConfig() {
        return serverAuthConfig;
    }

}
