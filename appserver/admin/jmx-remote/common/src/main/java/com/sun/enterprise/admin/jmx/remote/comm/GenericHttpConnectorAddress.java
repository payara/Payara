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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/comm/GenericHttpConnectorAddress.java,v 1.3 2005/12/25 04:26:31 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:31 $
*/

package com.sun.enterprise.admin.jmx.remote.comm;

/** An interface to extend the basic ConnectorAddress to include the HTTP information.
 */
public interface GenericHttpConnectorAddress extends ConnectorAddress {
	/**
	 * Returns the host name as a String. The interpretation is to be documented
	 * by the implementation (DNS/IP address).
	 * @return String	representing the host
	 */
	String getHost();
	/**
	 * Sets the host to the given parameter.
	 * @param host		represents the host name to set to.
	 */
	void setHost(String host);
	/** Returns the port for this instance of ConnectorAddress.
	 * @return integer representing the port number
	 */
	int getPort();
	/** Sets throws port to given integer.
	 * @param port			integer indicating the port number
	 */
	void setPort(int port);
	
	/** Returns the {@link AuthenticationInfo} related to this ConnectorAddress.
	 * AuthenticationInfo is to be handled appropriately by the implementing class.
	 * @return		instance of AuthenticationInfo class
	 */
	AuthenticationInfo getAuthenticationInfo();
	/** Sets the AuthenticationInfo for this ConnectorAddress.
	 * @param authInfo		instance of AuthenticationInfo
	 */
	void setAuthenticationInfo(AuthenticationInfo authInfo);
}
