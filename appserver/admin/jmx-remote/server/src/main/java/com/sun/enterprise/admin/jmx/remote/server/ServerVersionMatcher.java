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

/* VersionMatcher.java
 * $Id: ServerVersionMatcher.java,v 1.3 2005/12/25 04:26:36 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:36 $
 * Indentation Information:
 * 0. Please (try to) preserve these settings.
 * 1. Tabs are preferred over spaces.
 * 2. In vi/vim -
 *		:set tabstop=4 :set shiftwidth=4 :set softtabstop=4
 * 3. In S1 Studio -
 *		1. Tools->Options->Editor Settings->Java Editor->Tab Size = 4
 *		2. Tools->Options->Indentation Engines->Java Indentation Engine->Expand Tabs to Spaces = False.
 *		3. Tools->Options->Indentation Engines->Java Indentation Engine->Number of Spaces per Tab = 4.
 */

package com.sun.enterprise.admin.jmx.remote.server;

import java.util.logging.Logger;
import com.sun.enterprise.admin.jmx.remote.protocol.Version;
/**
 *
 * @author  <a href="mailto:Kedar.Mhaswade@sun.com">Kedar Mhaswade</a>
 * @since S1AS8.0
 * @version $Revision: 1.3 $
 */
public class ServerVersionMatcher {
	
	private static final ServerVersionMatcher matcher = new ServerVersionMatcher();
	private static final Logger logger = Logger.getLogger("com.sun.enterprise.admin.jmx.remote.finer.logger");
	private ServerVersionMatcher() {
	}
	
	public static ServerVersionMatcher getMatcher() {
		return ( matcher );
	}
	/**
	 * Returns true if and only if the client version and server version are
	 * compatible. The server version matches with client version if and only if
	 * <ul>
	 * <li> Client Major Version is <= Server Major Version </li>
	 * <li> Client Minor Version is <= Server Minor Version </li>
	 * <li> Upgrade Data is compatible in both the versions </li>
	 * </ul>
	 */
	public boolean match(Version client, Version server) {
		return (majorCompatible(client, server) &&
				minorCompatible(client, server) &&
				upgradeCompatible(client, server) );
	}
	
	boolean majorCompatible(Version c, Version s) {
		//client version can at most equal server version
		boolean compatible = false;
		final int cmv = c.getMajorVersion();
		final int smv = s.getMajorVersion();
		if (cmv < smv) {
			logger.finer("S1AS JSR 160 - Using Backword compatibility, as client version: " +
			cmv + " is smaller than server version: " + smv);
			logger.finer("It is better to upgrade the client software");
			compatible = true;
		}
		else if (cmv == smv) {
			compatible = true;
		}
		else {
			logger.finer("S1AS JSR 160 - Version Compatibility failed, as client version: " +
			cmv + " is higher than server version: " + smv);
			logger.finer("Server software has to be upgraded to atleast major version: " + cmv);
			compatible = false;
		}
		return ( compatible );
	}

	boolean minorCompatible(Version c, Version s) {
		//client version can at most equal server version
		boolean compatible = false;
		final int cmv = c.getMinorVersion();
		final int smv = s.getMinorVersion();
		if (cmv < smv) {
			logger.finer("S1AS JSR 160 - Using Backword compatibility, as client version: " +
			cmv + " is smaller than server version: " + smv);
			logger.finer("It is better to upgrade the client software");
			compatible = true;
		}
		else if (cmv == smv) {
			compatible = true;
		}
		else {
			logger.finer("S1AS JSR 160 - Version Compatibility failed, as client version: " +
			cmv + " is higher than server version: " + smv);
			logger.finer("Server software has to be upgraded to atleast major version: " + cmv);
			compatible = false;
		}
		return ( compatible );
	}
	
	boolean upgradeCompatible(Version c, Version s) {
		final String[] cud = c.getUpgradeData();
		final String[] sud = s.getUpgradeData();
		boolean uc = true;
		uc = uc && (cud.length == sud.length); //lengths must be the same
		if (uc) {
			for (int i = 0 ; i < cud.length ; i++) {
				if (cud[i] != null && sud[i] != null) {
					uc = uc && cud[i].equals(sud[i]);
					if (!uc) {
						break;
					}
				}
			}
		}
		return ( uc );
	}
}
