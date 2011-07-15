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
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/comm/ConnectionFactory.java,v 1.3 2005/12/25 04:26:31 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:31 $
*/

package com.sun.enterprise.admin.jmx.remote.comm;

/** A factory class to create new instances of {@link IConnection} 
 * interface.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.1
 */
public class ConnectionFactory {
	private ConnectionFactory() {
	}
	
	/** Returns the newly created connection (instance of {@link IConnection} 
	 * to the servlet. Note that the Servlet has to be up and running. If the
	 * server/servlet does not respond, IOException results. Note that this is
	 * by default a connection to request a resource on server side. It is not
	 * guaranteed that the connection is kept alive after it is used to send the data.
	 * Clients are expected to create the instances of IConnection as and when
	 * required, by calling this method.
	 * @param h		an instance of {@link HttpConnectorAddress} that encapsulates
	 *				the data required to create the connection
	 * @return an instance of IConnection interface
	 * @throws an instance of IOException if the attempt to connect fails
	 */
	public static IConnection createConnection(HttpConnectorAddress h) throws 
		java.io.IOException {
		return new ServletConnection(h);
	}
}
