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

package org.glassfish.admin.amx.logging;

import org.glassfish.admin.amx.util.SetUtil;

import java.util.Collections;
import java.util.Set;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
	Names of log level modules.
	@see Logging
    @since AppServer 9.0
 */
@Taxonomy(stability = Stability.EXPERIMENTAL)
public class LogModuleNames
{
	protected LogModuleNames()	{}
	
	public static final String ROOT_KEY					= "Root";
	public static final String SERVER_KEY				= "Server";
	public static final String EJB_CONTAINER_KEY		= "EJBContainer";
    public static final String CMP_CONTAINER_KEY		= "CMPContainer";
    public static final String MDB_CONTAINER_KEY		= "MDBContainer";
    public static final String WEB_CONTAINER_KEY		= "WebContainer";
    public static final String CLASSLOADER_KEY			= "Classloader";
    public static final String CONFIGURATION_KEY		= "Configuration";
    public static final String NAMING_KEY				= "Naming";
    public static final String SECURITY_KEY				= "Security";
	public static final String JTS_KEY					= "JTS";
	public static final String JTA_KEY					= "JTA";
    public static final String ADMIN_KEY				= "Admin";
    public static final String DEPLOYMENT_KEY			= "Deployment";
    public static final String VERIFIER_KEY				= "Verifier";
    public static final String JAXR_KEY					= "JAXR";
    public static final String JAXRPC_KEY				= "JAXRPC";
    public static final String SAAJ_KEY					= "SAAJ";
    public static final String CORBA_KEY				= "CORBA";
    public static final String JAVAMAIL_KEY				= "Javamail";
    public static final String JMS_KEY					= "JMS";
    public static final String CONNECTOR_KEY			= "Connector";
    public static final String JDO_KEY					= "JDO";
    public static final String CMP_KEY					= "CMP";
    public static final String UTIL_KEY					= "Util";
    public static final String RESOURCE_ADAPTER_KEY		= "ResourceAdapter";
    public static final String SYNCHRONIZATION_KEY		= "Synchronization";
    public static final String NODE_AGENT_KEY			= "NodeAgent";
    
    /**
     */
    public static final Set<String> ALL_NAMES =
        Collections.unmodifiableSet( SetUtil.newSet( new String[] 
        {
            ROOT_KEY,
            SERVER_KEY,
            EJB_CONTAINER_KEY,
            CMP_CONTAINER_KEY,
            MDB_CONTAINER_KEY,
            WEB_CONTAINER_KEY,
            CLASSLOADER_KEY,
            CONFIGURATION_KEY,
            NAMING_KEY,
            SECURITY_KEY,
            JTS_KEY,
            JTA_KEY,
            ADMIN_KEY,
            DEPLOYMENT_KEY,
            VERIFIER_KEY,
            JAXR_KEY,
            JAXRPC_KEY,
            SAAJ_KEY,
            CORBA_KEY,
            JAVAMAIL_KEY,
            JMS_KEY,
            CONNECTOR_KEY,
            JDO_KEY,
            CMP_KEY,
            UTIL_KEY,
            RESOURCE_ADAPTER_KEY,
            SYNCHRONIZATION_KEY,
            NODE_AGENT_KEY,
        }));
}

