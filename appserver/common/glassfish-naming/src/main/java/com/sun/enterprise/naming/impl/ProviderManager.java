/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import org.omg.CORBA.ORB;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * This class is a facade for the remote and local SerialContextProvider The
 * need for this class arose from the fact that we wanted to have a way of
 * lazily initializing the Remote SerialContextProvider. The TransientContext
 * member field has to be shared across both remote and local
 * SerialContextProvider. It could have been a static variable but to avoid
 * issues of static variables with multiple threads, etc, this class has been
 * designed. The ORB needs to be initialized before the call to
 * initRemoteProvider()
 *
 * @author Sheetal Vartak
 */

public class ProviderManager {

    private static ProviderManager providerManager;

    private TransientContext rootContext = new TransientContext();

    private SerialContextProvider localProvider;

    // Set lazily once initRemoteProvider is called.  Only available
    // in server.
    private ORB orb;

    private ProviderManager() {}

    public synchronized static ProviderManager getProviderManager() {
	    if (providerManager  == null ) {
	        providerManager = new ProviderManager();
        }
	    return providerManager;
    }
    
    public TransientContext getTransientContext() {
        return rootContext;
    }

    public synchronized SerialContextProvider getLocalProvider() {
        if (localProvider == null) {
            localProvider = LocalSerialContextProviderImpl.initProvider(rootContext);
        }
        return localProvider;
    }

    public Remote initRemoteProvider(ORB orb) throws RemoteException {
        this.orb = orb;
        return RemoteSerialContextProviderImpl.initSerialContextProvider(orb, rootContext);
    }

    ORB getORB() {
        return orb;
    }
}
