/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.util;

import com.sun.enterprise.naming.spi.NamingObjectFactory;
import com.sun.enterprise.naming.spi.NamingUtils;
import com.sun.enterprise.naming.util.JndiInitializationNamingObjectFactory;

import org.glassfish.logging.annotation.LogMessageInfo;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

import javax.naming.Context;
import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;

import static com.sun.enterprise.naming.util.LogFacade.logger;
import static org.glassfish.common.util.ObjectInputOutputStreamFactoryFactory.getFactory;;

/**
 * This is a utils class for refactoring the following method.
 */

@Service
@Singleton
public class NamingUtilsImpl implements NamingUtils {
    @LogMessageInfo(message = "Exception in NamingManagerImpl copyMutableObject(): {0}",
    cause = "Problem with serialising or deserialising of the object",
    action = "Check the class hierarchy to see if all the classes are Serializable.")
    public static final String EXCEPTION_COPY_MUTABLE = "AS-NAMING-00006";

    public NamingObjectFactory createSimpleNamingObjectFactory(String name,
        Object value) {
        return new SimpleNamingObjectFactory(name, value);
    }

    public NamingObjectFactory createLazyNamingObjectFactory(String name,
        String jndiName, boolean cacheResult) {
        return new JndiNamingObjectFactory(name, jndiName, cacheResult);
    }

    public NamingObjectFactory createLazyInitializationNamingObjectFactory(String name, String jndiName,
            boolean cacheResult){
        return new JndiInitializationNamingObjectFactory(name, jndiName, cacheResult);
    }

    public NamingObjectFactory createCloningNamingObjectFactory(String name,
        Object value) {
        return new CloningNamingObjectFactory(name, value);
    }

    public NamingObjectFactory createCloningNamingObjectFactory(String name,
        NamingObjectFactory delegate) {
        return new CloningNamingObjectFactory(name, delegate);
    }

    public NamingObjectFactory createDelegatingNamingObjectFactory(String name,
        NamingObjectFactory delegate, boolean cacheResult) {
        return new DelegatingNamingObjectFactory(name, delegate, cacheResult);
    }
    
    public Object makeCopyOfObject(Object obj) {
        if ( !(obj instanceof Context) && (obj instanceof Serializable) ) {
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "** makeCopyOfObject:: " + obj);
            }
            
            try {
                // first serialize the object
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = getFactory().createObjectOutputStream(bos);
                oos.writeObject(obj);
                oos.flush();
                byte[] data = bos.toByteArray();
                oos.close();
                bos.close();

                // now deserialize it
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                final ObjectInputStream ois = getFactory().createObjectInputStream(bis);
                obj = AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws IOException, ClassNotFoundException {
                        return ois.readObject();
                    }
                });
                return obj;
            } catch (Exception ex) {
                logger.log(Level.SEVERE, EXCEPTION_COPY_MUTABLE, ex);
                RuntimeException re = new RuntimeException("Cant copy Serializable object:", ex);
                throw re;
            }
        } else {
            // XXX no copy ?
            return obj;
        }
    }
}
