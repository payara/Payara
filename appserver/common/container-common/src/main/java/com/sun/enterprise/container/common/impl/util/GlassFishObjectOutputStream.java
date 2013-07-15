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

package com.sun.enterprise.container.common.impl.util;

import com.sun.logging.LogDomains;

import com.sun.enterprise.util.Utility;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.internal.api.Globals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.container.common.spi.util.GlassFishOutputStreamHandler;
import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;

import org.glassfish.common.util.ObjectInputOutputStreamFactory;
import org.glassfish.common.util.ObjectInputOutputStreamFactoryFactory;

/**
 * A class that is used to save conversational state
 * 
 * @author Mahesh Kannan
 */
class GlassFishObjectOutputStream extends java.io.ObjectOutputStream {
	private static Logger _logger = LogDomains.getLogger(
			GlassFishObjectOutputStream.class, LogDomains.JNDI_LOGGER);

	static final int EJBID_OFFSET = 0;
	static final int INSTANCEKEYLEN_OFFSET = 8;
	static final int INSTANCEKEY_OFFSET = 12;

	private static final byte HOME_KEY = (byte) 0xff;

	private ObjectInputOutputStreamFactory outputStreamHelper;

	private Collection<GlassFishOutputStreamHandler> handlers;
	
	GlassFishObjectOutputStream(Collection<GlassFishOutputStreamHandler> handlers, OutputStream out, boolean replaceObject)
			throws IOException {
		super(out);
		this.handlers = handlers;
		
		if (replaceObject == true) {
			enableReplaceObject(replaceObject);
		}

		outputStreamHelper = ObjectInputOutputStreamFactoryFactory.getFactory();
		
	}

	/**
	 * This code is needed to serialize non-Serializable objects that can be
	 * part of a bean's state. See EJB2.0 section 7.4.1.
	 */
	protected Object replaceObject(Object obj) throws IOException {
		Object result = obj;

		if (obj instanceof IndirectlySerializable) {
			result = ((IndirectlySerializable) obj).getSerializableObjectFactory();
		} else if (obj instanceof Context) {
			result = new SerializableJNDIContext((Context) obj);
		} else {
			for (GlassFishOutputStreamHandler handler : handlers) {
				Object r = handler.replaceObject(obj);
				if (r != null) {
					result = r;
					break;
				}
			}
		}

		return result;
	}

	@Override
	protected void annotateClass(Class<?> cl) throws IOException {
		outputStreamHelper.annotateClass(this, cl);
	}

}

final class SerializableJNDIContext implements SerializableObjectFactory {
	private String name;

	SerializableJNDIContext(Context ctx) throws IOException {
		try {
			// Serialize state for a jndi context. The spec only requires
			// support for serializing contexts pointing to java:comp/env
			// or one of its subcontexts. We also support serializing the
			// references to the the default no-arg InitialContext, as well
			// as references to the the contexts java: and java:comp. All
			// other contexts will either not serialize correctly or will
			// throw an exception during deserialization.
			this.name = ctx.getNameInNamespace();
		} catch (NamingException ex) {
			IOException ioe = new IOException();
			ioe.initCause(ex);
			throw ioe;
		}
	}

	public Object createObject() throws IOException {
		try {
			if ((name == null) || (name.length() == 0)) {
				return new InitialContext();
			} else {
				return Globals.getDefaultHabitat()
						.<GlassfishNamingManager>getService(GlassfishNamingManager.class)
						.restoreJavaCompEnvContext(name);
			}
		} catch (NamingException namEx) {
			IOException ioe = new IOException();
			ioe.initCause(namEx);
			throw ioe;
		}
	}

}
