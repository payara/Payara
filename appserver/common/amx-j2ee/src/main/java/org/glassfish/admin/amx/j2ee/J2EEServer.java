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

package org.glassfish.admin.amx.j2ee;

import javax.management.ObjectName;
import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;


/**
 */
@AMXMBeanMetadata(type=J2EETypes.J2EE_SERVER)
public interface J2EEServer extends J2EELogicalServer
{
	/**
	    Restart the server.
	    <b>Enterprise Edition only.</b>
	 */
 	@ManagedOperation
	public void restart();
	
	/**
		Note that the Attribute name is case-sensitive
		"deployedObjects" as defined by JSR 77.
		
	 	@return the ObjectNames as Strings
	 */
 	@ManagedAttribute
	public String[]	getdeployedObjects();
	
	
	/**
		In 8.1, there will only ever be one JVM for a J2EEServer.
		Note that the Attribute name is case-sensitive
		"javaVMs" as defined by JSR 77.
		
	 	@return the ObjectNames as Strings
	 */
 	@ManagedAttribute
	public String[]	getjavaVMs();
	
	/**
		There is always a single JVM for a J2EEServer.
		@return JVM
	 */
 	@ManagedAttribute
	public String		getjvm();
	
	/**
		Note that the Attribute name is case-sensitive
		"resources" as defined by JSR 77.
		
	 	@return the ObjectNames as Strings
	 */
 	@ManagedAttribute
	public String[]		getresources();
	
	
	/**
		Note that the Attribute name is case-sensitive
		"serverVendor" as defined by JSR 77.
		
	 	@return the server vendor, a free-form String
	 */
 	@ManagedAttribute
	public String		getserverVendor();
	
	/**
		Note that the Attribute name is case-sensitive
		"serverVersion" as defined by JSR 77.
		
	 	@return the server version, a free-form String
	 */
 	@ManagedAttribute
	public String		getserverVersion();


 	@ManagedAttribute
    @Description( "Get the ObjectName of the corresponding config MBean, if any" )
    public ObjectName getCorrespondingConfig();
}



