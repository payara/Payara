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

package com.sun.ejb.containers;


import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;

import com.sun.ejb.EjbInvocation;
import com.sun.ejb.Container;

import org.glassfish.ejb.api.EjbEndpointFacade;


import java.util.logging.Level;
import java.util.logging.Logger;


/*
 * @author Kenneth Saks
 */

public class EjbEndpointFacadeImpl implements EjbEndpointFacade {

    private BaseContainer container_;
    private InvocationManager invManager_;
    private static Logger logger_ = EjbContainerUtilImpl.getLogger();


    public EjbEndpointFacadeImpl(BaseContainer container, EjbContainerUtil util) {
        container_ = container;
        invManager_ = util.getInvocationManager();
    }


    public ClassLoader getEndpointClassLoader() {

        return container_.getClassLoader();

    }


    public ComponentInvocation startInvocation() {

        // We need to split the preInvoke tasks into stages since handlers
        // need access to java:comp/env and method authorization must take
        // place before handlers are run.  Note that the application
        // classloader was set much earlier when the invocation first arrived
        // so we don't need to set it here.
        EjbInvocation inv = container_.createEjbInvocation();

        // Do the portions of preInvoke that don't need a Method object.
        inv.isWebService = true;
        inv.container = container_;
        inv.transactionAttribute = Container.TX_NOT_INITIALIZED;

        // AS per latest spec change, the MessageContext object in WebSvcCtxt
        // should be the same one as used in the ejb's interceptors'
        // TODO    
        // inv.setContextData(wsCtxt);

        // In all cases, the WebServiceInvocationHandler will do the
        // remaining preInvoke tasks : getContext, preInvokeTx, etc.
        invManager_.preInvoke(inv);

        return inv;

    }

    
    public void endInvocation(ComponentInvocation inv) {

        try {
            EjbInvocation ejbInv = (EjbInvocation) inv;

            // Only use container version of postInvoke if we got past
            // assigning an ejb instance to this invocation.  This is
            // because the web service invocation does an InvocationManager
            // preInvoke *before* assigning an ejb instance.  So, we need
            // to ensure that InvocationManager.postInvoke is always
            // called.  It was cleaner to keep this logic in this class
            // and WebServiceInvocationHandler rather than change the
            // behavior of BaseContainer.preInvoke and
            // BaseContainer.postInvoke.


            if( ejbInv.ejb != null ) {
                container_.webServicePostInvoke(ejbInv);
            } else {
                invManager_.postInvoke(inv);
            }

        } catch(Throwable t) {
            logger_.log(Level.WARNING,
                       "Unexpected error in EJB WebService endpoint post processing", t);
        }

    }

}
