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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.container.common.impl.util;

import com.sun.enterprise.container.common.spi.util.GlassFishInputStreamHandler;
import com.sun.enterprise.container.common.spi.util.GlassFishOutputStreamHandler;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.logging.LogDomains;
import org.jvnet.hk2.annotations.Service;

import java.io.*;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
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

	private static final Logger _logger = LogDomains.getLogger(
			JavaEEIOUtilsImpl.class, LogDomains.JNDI_LOGGER, false);

	private final Collection<GlassFishOutputStreamHandler> outputHandlers = new CopyOnWriteArraySet<>();

	private final Collection<GlassFishInputStreamHandler> inputHandlers = new CopyOnWriteArraySet<>();

        @Override
	public ObjectInputStream createObjectInputStream(InputStream is,
			boolean resolveObject, ClassLoader loader, long uniqueId) throws Exception {
		return new GlassFishObjectInputStream(inputHandlers, is, loader, resolveObject, uniqueId);
	}

        @Override
	public ObjectOutputStream createObjectOutputStream(OutputStream os,
			boolean replaceObject) throws IOException {
		return new GlassFishObjectOutputStream(outputHandlers, os, replaceObject);
	}

        @Override
	public byte[] serializeObject(Object obj, boolean replaceObject)
			throws java.io.IOException {

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
			 ObjectOutputStream oos = createObjectOutputStream(bos, replaceObject)) {
			oos.writeObject(obj);
			oos.flush();
			return bos.toByteArray();
		} catch (java.io.NotSerializableException notSerEx) {
			throw notSerEx;
		} catch (Exception th) {
			throw new IOException(th);
		}
	}

	@Override
	public Object deserializeObject(byte[] data, boolean resolveObject, ClassLoader appClassLoader) throws Exception {
		return deserializeObject(data, resolveObject, appClassLoader, 0L);
	}

	@Override
	public Object deserializeObject(byte[] data, boolean resolveObject,
			ClassLoader appClassLoader, long uniqueId) throws Exception {

		try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
			 ObjectInputStream ois = createObjectInputStream(bis, resolveObject, appClassLoader, uniqueId)) {
			return ois.readObject();
		} catch (Exception ex) {
			_logger.log(Level.FINE, "Error during deserialization", ex);
			throw ex;
		}
	}

	@Override
	public void addGlassFishOutputStreamHandler(GlassFishOutputStreamHandler handler) {
		outputHandlers.add(handler);
	}

	@Override
	public void removeGlassFishOutputStreamHandler(GlassFishOutputStreamHandler handler) {
		outputHandlers.remove(handler);
	}

	@Override
	public void addGlassFishInputStreamHandler(GlassFishInputStreamHandler handler) {
		inputHandlers.add(handler);
	}

	@Override
	public void removeGlassFishInputStreamHandler(GlassFishInputStreamHandler handler) {
		inputHandlers.remove(handler);
	}
}
