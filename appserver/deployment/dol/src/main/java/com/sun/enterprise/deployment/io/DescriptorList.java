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

package com.sun.enterprise.deployment.io;

import org.glassfish.api.deployment.archive.ArchiveType;

/**
 * Repository of descriptors
 * This class will evolve to provide a comprhensive list of
 * descriptors for any given type of j2ee application or
 * stand-alone module.
 *
 * @author Sreenivas Munnangi
 */

public class DescriptorList {

	private final static String [] earList = {
		DescriptorConstants.APPLICATION_DD_ENTRY,
		DescriptorConstants.S1AS_APPLICATION_DD_ENTRY
	};

	private final static String [] ejbList = {
		DescriptorConstants.EJB_DD_ENTRY,
		DescriptorConstants.S1AS_EJB_DD_ENTRY,
		DescriptorConstants.S1AS_CMP_MAPPING_DD_ENTRY,
		DescriptorConstants.EJB_WEBSERVICES_JAR_ENTRY
	};

	private final static String [] warList = {
		DescriptorConstants.WEB_DD_ENTRY,
		DescriptorConstants.S1AS_WEB_DD_ENTRY,
		DescriptorConstants.WEB_WEBSERVICES_JAR_ENTRY,
		DescriptorConstants.JAXRPC_JAR_ENTRY
	};

	private final static String [] rarList = {
		DescriptorConstants.RAR_DD_ENTRY,
		DescriptorConstants.S1AS_RAR_DD_ENTRY
	};

	private final static String [] carList = {
		DescriptorConstants.APP_CLIENT_DD_ENTRY,
		DescriptorConstants.S1AS_APP_CLIENT_DD_ENTRY
	};

	public final static String [] getDescriptorsList (ArchiveType moduleType) {
		if (moduleType == null) return null;
		if (moduleType.equals(org.glassfish.deployment.common.DeploymentUtils.earType())) {
			return (String[])earList.clone();
		} else if (moduleType.equals(org.glassfish.deployment.common.DeploymentUtils.ejbType())) {
			return (String[])ejbList.clone();
		} else if (moduleType.equals(org.glassfish.deployment.common.DeploymentUtils.warType())) {
			return (String[])warList.clone();
		} else if (moduleType.equals(org.glassfish.deployment.common.DeploymentUtils.rarType())) {
			return (String[])rarList.clone();
		} else if (moduleType.equals(org.glassfish.deployment.common.DeploymentUtils.carType())) {
			return (String[])carList.clone();
		}
		return null;
	}
}
