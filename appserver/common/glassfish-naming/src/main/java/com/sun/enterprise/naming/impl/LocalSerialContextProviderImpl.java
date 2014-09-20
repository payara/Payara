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

import com.sun.enterprise.naming.util.NamingUtilsImpl;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.omg.CORBA.ORB;

import javax.naming.CompositeName;
import javax.naming.NamingException;
import javax.naming.Reference;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.logging.Level;

import static com.sun.enterprise.naming.util.LogFacade.logger;

/**
 * This class is the implementation of the local SerialContextProvider
 *
 * @author Sheetal Vartak
 */

public class LocalSerialContextProviderImpl extends SerialContextProviderImpl {
    @LogMessageInfo(message = "Exception occurred when instantiating LocalSerialContextProviderImpl: {0}",
    cause = "java.rmi.RemoteException",
    action = "Check server.log for details")
    public static final String LOCAL_PROVIDER_NULL = "AS-NAMING-00003";

    private NamingUtilsImpl namingUtils = new NamingUtilsImpl();

    private LocalSerialContextProviderImpl(TransientContext rootContext) throws RemoteException {
        super(rootContext);
    }

    static LocalSerialContextProviderImpl initProvider(TransientContext rootContext) {
        try {
            return new LocalSerialContextProviderImpl(rootContext);
        } catch (RemoteException re) {
            logger.log(Level.SEVERE, LOCAL_PROVIDER_NULL, re);
            return null;
        }
    }

    /**
     * overriding the super.bind() since we need to make a copy of the object
     * before it gets put into the rootContext
     * Remote Provider already does that since when a method is called
     * on a remote object (in our case the remote provider),
     * the copies of the method arguments get passed and not the real objects.
     */

    public void bind(String name, Object obj)
            throws NamingException, RemoteException {
        Object copyOfObj = namingUtils.makeCopyOfObject(obj);
        super.bind(name, copyOfObj);
    }


    /**
     * overriding the super.rebind() since we need to make a copy of the object
     * before it gets put into the rootContext.
     * Remote Provider already does that since when a method is called
     * on a remote object (in our case the remote provider),
     * the copies of the method arguments get passed and not the real objects.
     */

    public void rebind(String name, Object obj)
            throws NamingException, RemoteException {
        Object copyOfObj = namingUtils.makeCopyOfObject(obj);
        super.rebind(name, copyOfObj);
    }

    public Object lookup(String name)
            throws NamingException, RemoteException {
        Object obj = super.lookup(name);
        
        try {
            if (obj instanceof Reference) {
                Reference ref = (Reference) obj;

                if (ref.getFactoryClassName().equals
                        (GlassfishNamingManagerImpl.IIOPOBJECT_FACTORY)) {

                    ORB orb = ProviderManager.getProviderManager().getORB();

                    Hashtable env = new Hashtable();
                    if( orb != null ) {

                        env.put("java.naming.corba.orb", orb);

                    }


                    obj = javax.naming.spi.NamingManager.getObjectInstance
                            (obj, new CompositeName(name), null, env);
                    // NOTE : No copy object performed in this case
                    return obj;
                }

            }
       
        } catch (Exception e) {
            RemoteException re = new RemoteException("", e);
            throw re;

        }

        return namingUtils.makeCopyOfObject(obj);
    }
}
