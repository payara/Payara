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

import org.glassfish.external.amx.AMX;
import org.glassfish.admin.amx.util.SetUtil;

import java.util.Set;

/**
	See JSR 77.3-1.<br>
 */
public final class J2EETypes
{
    private J2EETypes() {}
    
	/**
		The javax.management.ObjectName property key denoting the type of the MBean.
	 */
	public final static String	J2EE_TYPE_KEY			= "j2eeType";
	
	/**
		The ObjectName property key denoting the name of the MBean.
	 */
	public final static String	NAME_KEY			= AMX.NAME_KEY;
	
	public final static String	J2EE_DOMAIN					= "J2EEDomain";
	public final static String	J2EE_SERVER					= "J2EEServer";
	public final static String	J2EE_APPLICATION			= "J2EEApplication";
	public final static String	APP_CLIENT_MODULE			= "AppClientModule";
	public final static String	EJB_MODULE					= "EJBModule";
	public final static String	WEB_MODULE					= "WebModule";
	public final static String	RESOURCE_ADAPTER_MODULE		= "ResourceAdapterModule";
	public final static String	RESOURCE_ADAPTER		    = "ResourceAdapter";
	public final static String	ENTITY_BEAN					= "EntityBean";
	public final static String	STATEFUL_SESSION_BEAN		= "StatefulSessionBean";
	public final static String	STATELESS_SESSION_BEAN		= "StatelessSessionBean";
	public final static String	SINGLETON_SESSION_BEAN		= "SingletonSessionBean";
	public final static String	MESSAGE_DRIVEN_BEAN			= "MessageDrivenBean";
	public final static String	SERVLET						= "Servlet";
	public final static String	JAVA_MAIL_RESOURCE			= "JavaMailResource";
	public final static String	JCA_RESOURCE				= "JCAResource";
	public final static String	JCA_CONNECTION_FACTORY		= "JCAConnectionFactory";
	public final static String	JCA_MANAGED_CONNECTION_FACTORY	= "JCAManagedConnectionFactory";
	public final static String	JDBC_RESOURCE				= "JDBCResource";
	public final static String	JDBC_DATA_SOURCE			= "JDBCDataSource";
	public final static String	JDBC_DRIVER					= "JDBCDriver";
	public final static String	JMS_RESOURCE				= "JMSResource";
	public final static String	JNDI_RESOURCE				= "JNDIResource";
	public final static String	JTA_RESOURCE				= "JTAResource";
	public final static String	RMI_IIOP_RESOURCE			= "RMI_IIOPResource";
	public final static String	URL_RESOURCE				= "URLResource";
	public final static String	JVM					        = "JVM";
	
	
	/**
		@since AppServer 9.0
	 */
	public final static String	WEB_SERVICE_ENDPOINT	="WebServiceEndpoint";
	
	/**
		Set consisting of all standard JSR 77 j2eeTypes
	 */
	public static final Set<String>	ALL_STD	= 
	    SetUtil.newUnmodifiableStringSet(
		J2EE_DOMAIN,
		J2EE_SERVER,
		J2EE_APPLICATION,
		APP_CLIENT_MODULE,
		EJB_MODULE,
		WEB_MODULE,
		RESOURCE_ADAPTER_MODULE,
		ENTITY_BEAN,
		STATEFUL_SESSION_BEAN,
		STATELESS_SESSION_BEAN,
		MESSAGE_DRIVEN_BEAN,
		SERVLET,
		JAVA_MAIL_RESOURCE,
		JCA_RESOURCE,
		JCA_CONNECTION_FACTORY,
		JCA_MANAGED_CONNECTION_FACTORY,
		JDBC_RESOURCE,
		JDBC_DATA_SOURCE,
		JDBC_DRIVER,
		JMS_RESOURCE,
		JNDI_RESOURCE,
		JTA_RESOURCE,
		RMI_IIOP_RESOURCE,
		URL_RESOURCE,
		JVM,
		WEB_SERVICE_ENDPOINT  );


}
