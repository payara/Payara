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

package com.sun.ejb.base.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.logging.*;

import com.sun.ejb.spi.io.J2EEObjectStreamFactory;
import com.sun.logging.LogDomains;

public class IOUtils {

    private static final Logger _ejbLogger =
            LogDomains.getLogger(IOUtils.class, LogDomains.EJB_LOGGER);

    private static J2EEObjectStreamFactory _streamFactory =
            new J2EEObjectStreamFactoryImpl();

    public static final void setJ2EEObjectStreamFactory(
            J2EEObjectStreamFactory  factory)
    {
        _streamFactory = factory;
    }
    
    public static ObjectInputStream createObjectInputStream(
	    final InputStream is, final boolean resolveObject,
	    final ClassLoader loader)
	throws Exception
    {
	    return _streamFactory.createObjectInputStream(is, resolveObject, loader);
    }

    public static ObjectOutputStream createObjectOutputStream(
	    final OutputStream os, final boolean replaceObject)
	throws IOException
    {
	return _streamFactory.createObjectOutputStream(os, replaceObject);
    }

    public static final byte[] serializeObject(Object obj, boolean replaceObject)
	throws java.io.NotSerializableException, java.io.IOException
    {
        byte[] data = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = _streamFactory.createObjectOutputStream(
                bos, replaceObject);

            oos.writeObject(obj);
            oos.flush();
            data = bos.toByteArray();
        } catch (java.io.NotSerializableException notSerEx) {
            throw notSerEx;
        } catch (Exception th) {
            IOException ioEx = new IOException(th.toString());
            ioEx.initCause(th);
            throw ioEx;
        } finally {
            if (oos != null) {
                try { oos.close(); } catch (Exception ex) {}
            }
            try { bos.close(); } catch (Exception ex) {}
        }

	return data;
    }


     public static final Object deserializeObject(byte[] data, boolean resolveObject,
            ClassLoader appClassLoader) throws Exception {
          Object obj = null;
	ByteArrayInputStream bis = null;
	ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(data);
            ois = _streamFactory.createObjectInputStream(bis, resolveObject,
		        appClassLoader);
            obj = ois.readObject();
        } catch (Exception ex) {
            _ejbLogger.log(Level.FINE, "Error during deserialization", ex);
            throw ex;
        } finally {
            try { ois.close(); } catch (Exception ex) {
                _ejbLogger.log(Level.FINEST, "Error during ois.close()", ex);
            }
            try { bis.close(); } catch (Exception ex) {
                _ejbLogger.log(Level.FINEST, "Error during bis.close()", ex);
            }
	    }
        return obj;

     }

}
