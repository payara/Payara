/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.container.common.impl.util;

import org.jvnet.hk2.annotations.Contract;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.container.common.spi.util.GlassFishOutputStreamHandler;
import com.sun.enterprise.container.common.spi.util.GlassFishInputStreamHandler;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.logging.LogDomains;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A contract that defines a set of methods to serialize / deserialze Java EE
 * objects (even if they are not directly serializable).
 * 
 * Some of the objects that are expected to be serialized / de-serialized are a)
 * Local EJB references b) EJB Handles c) JNDI (sub) contexts d) (Non
 * serializable) StatefulSessionBeans
 * 
 * @author Mahesh Kannan
 * 
 */
@Service
public class JavaEEIOUtilsImpl implements JavaEEIOUtils {

	private static Logger _logger = LogDomains.getLogger(
			JavaEEIOUtilsImpl.class, LogDomains.JNDI_LOGGER);

	@Inject
	ServiceLocator habitat;

	private Collection<GlassFishOutputStreamHandler> outputHandlers = new HashSet<GlassFishOutputStreamHandler>();

	private Collection<GlassFishInputStreamHandler> inputHandlers = new HashSet<GlassFishInputStreamHandler>();

	public ObjectInputStream createObjectInputStream(InputStream is,
			boolean resolveObject, ClassLoader loader) throws Exception {
		return new GlassFishObjectInputStream(inputHandlers, is, loader, resolveObject);
	}

	public ObjectOutputStream createObjectOutputStream(OutputStream os,
			boolean replaceObject) throws IOException {
		return new GlassFishObjectOutputStream(outputHandlers, os, replaceObject);
	}

	public byte[] serializeObject(Object obj, boolean replaceObject)
			throws java.io.IOException {

		byte[] data = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = createObjectOutputStream(bos, replaceObject);

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
				try {
					oos.close();
				} catch (Exception ex) {
				}
			}
			try {
				bos.close();
			} catch (Exception ex) {
			}
		}

		return data;
	}

	public Object deserializeObject(byte[] data, boolean resolveObject,
			ClassLoader appClassLoader) throws Exception {

		Object obj = null;
		ByteArrayInputStream bis = null;
		ObjectInputStream ois = null;
		try {
			bis = new ByteArrayInputStream(data);
			ois = createObjectInputStream(bis, resolveObject, appClassLoader);
			obj = ois.readObject();
		} catch (Exception ex) {
			_logger.log(Level.FINE, "Error during deserialization", ex);
			throw ex;
		} finally {
			try {
				ois.close();
			} catch (Exception ex) {
				_logger.log(Level.FINEST, "Error during ois.close()", ex);
			}
			try {
				bis.close();
			} catch (Exception ex) {
				_logger.log(Level.FINEST, "Error during bis.close()", ex);
			}
		}
		return obj;
	}

	public void addGlassFishOutputStreamHandler(GlassFishOutputStreamHandler handler) {
		outputHandlers.add(handler);

	}

	public void removeGlassFishOutputStreamHandler(GlassFishOutputStreamHandler handler) {
		outputHandlers.remove(handler);
	}

	public void addGlassFishInputStreamHandler(
			GlassFishInputStreamHandler handler) {
		inputHandlers.add(handler);
	}

	public void removeGlassFishInputStreamHandler(
			GlassFishInputStreamHandler handler) {
		inputHandlers.remove(handler);
	}

}
