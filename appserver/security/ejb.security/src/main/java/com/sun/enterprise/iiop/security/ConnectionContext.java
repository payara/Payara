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

package com.sun.enterprise.iiop.security;

import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMech;
import java.io.*;
import java.net.Socket;

import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.transport.SocketInfo;




public final class ConnectionContext implements Serializable {
    private CompoundSecMech mechanism = null;
    private boolean sslClientAuth = false;
    private boolean ssl = false;
    private IOR ior = null;
    private transient Socket socket = null;
    private transient SocketInfo endpoint = null;

    /**
     * Default constructor.
     */
    public ConnectionContext() {
    }

    /**
     * Create the security mechanism context. This is stored in TLS.
     */
    public ConnectionContext(CompoundSecMech mech, IOR ior) {
	this.ior = ior;
	mechanism = mech;
    }

    /**
     * Return the IOR.
     */
     public IOR getIOR() {
	return ior;
     }

    /**
     * Set the IOR
     */
    public void setIOR(IOR ior) {
	this.ior = ior;
    }

    /**
     * Return the selected compound security mechanism.
     */
    public CompoundSecMech getMechanism() {
	return mechanism;
    }

    /**
     * Set the mechanism used for this invocation.
     */
    public void setMechanism(CompoundSecMech mech) {
	mechanism = mech;
    }

    /**
     * Return true if SSL client authentication has happened, false otherwise.
     */
    public boolean getSSLClientAuthenticationOccurred() {
	return sslClientAuth;
    }

    /**
     * Set true if SSL client authentication has happened.
     */
    public void setSSLClientAuthenticationOccurred(boolean val) {
	sslClientAuth = val;
    }

    /**
     * Return true if SSL was used to invoke the EJB.
     */
    public boolean getSSLUsed() {
	return ssl;
    }

    /**
     * Set true if SSL was used to invoke the EJB.
     */
    public void setSSLUsed(boolean val) {
	ssl = val;
    }

    public void setEndPointInfo(SocketInfo info) {
	endpoint = info;
    }

    public SocketInfo getEndPointInfo() {
	return endpoint;
    }

    /**
     * Return the socket for this connection.
     */
    public Socket getSocket() {
	return socket;
    }

    /**
     * Set the socket for this connection.
     */
    public void setSocket(Socket s) {
	socket = s;
    }

    public String toString() {
	String s = "sslClientAuth=" + sslClientAuth;
	s = s + " SSL=" + ssl;
	s = s + " ENDPOINT=" + endpoint;
	s = s + " mechanism=" + mechanism;
	s = s + " IOR=" + ior;
	s = s + " Socket=" + socket;
	return s;
    }
}


