/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.enterprise.naming.util.ObjectInputOutputStreamFactoryFactory.*;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import javax.naming.Context;

/**
 * This is a utils class for refactoring the following method.
 */

@Service
@Scoped(Singleton.class)
public class NamingUtilsImpl
    implements NamingUtils {

    static Logger _logger = LogFacade.getLogger();

    public NamingObjectFactory createSimpleNamingObjectFactory(String name,
        Object value) {
        return new SimpleNamingObjectFactory(name, value);
    }

    public NamingObjectFactory createLazyNamingObjectFactory(String name,
        String jndiName, boolean cacheResult) {
        return new JndiNamingObjectFactory(name, jndiName, cacheResult);
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
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "** makeCopyOfObject:: " + obj);
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

                _logger.log(Level.SEVERE,
                        "enterprise_naming.excep_in_copymutableobj", ex);

                RuntimeException re =
                        new RuntimeException("Cant copy Serializable object:");
                re.initCause(ex);
                throw re;
            }
        } else {
            // XXX no copy ?
            return obj;
        }
    }

    public OutputStream getMailLogOutputStream() {
        return new MailLogOutputStream();
    }
}
