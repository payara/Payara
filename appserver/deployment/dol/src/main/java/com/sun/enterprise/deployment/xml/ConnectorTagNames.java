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

 package com.sun.enterprise.deployment.xml;
 
/** The XML tag names for the connector dtd
 * @author Vivek Nagar
 * @author Tony Ng
 */
public interface ConnectorTagNames extends TagNames {

    public static String CONNECTOR = "connector";
    public static String RESOURCE_ADAPTER = "resourceadapter";
    public static String AUTH_MECHANISM = "authentication-mechanism";
    public static String CREDENTIAL_INTF = "credential-interface";
    public static String AUTH_MECH_TYPE = "authentication-mechanism-type";
    public static String CONNECTION_FACTORY_INTF = 
        "connectionfactory-interface";
    public static String CONNECTION_FACTORY_IMPL = 
        "connectionfactory-impl-class";
    public static String CONNECTION_INTF = "connection-interface";
    public static String CONNECTION_IMPL = "connection-impl-class";
    public static String CONFIG_PROPERTY = "config-property";
    public static String CONFIG_PROPERTY_NAME = "config-property-name";
    public static String CONFIG_PROPERTY_TYPE = "config-property-type";
    public static String CONFIG_PROPERTY_VALUE = "config-property-value";
    public static String CONFIG_PROPERTY_IGNORE = "config-property-ignore";
    public static String CONFIG_PROPERTY_SUPPORTS_DYNAMIC_UPDATES = "config-property-supports-dynamic-updates";
    public static String CONFIG_PROPERTY_CONFIDENTIAL = "config-property-confidential";
    public static String EIS_TYPE = "eis-type";
    public static String MANAGED_CONNECTION_FACTORY = 
        "managedconnectionfactory-class";
    public static String REAUTHENTICATION_SUPPORT = "reauthentication-support";
    public static String SPEC_VERSION = "spec-version";
    public static String SECURITY_PERMISSION = "security-permission";
    public static String SECURITY_PERMISSION_SPEC = "security-permission-spec";
    public static String TRANSACTION_SUPPORT = "transaction-support";
    public static String VENDOR_NAME = "vendor-name";
    public static String VERSION = "version";
    public static String RESOURCEADAPTER_VERSION = "resourceadapter-version";
    public static String LICENSE_REQUIRED = "license-required";
    public static String LICENSE = "license";

    //connector1.5
    public static String OUTBOUND_RESOURCE_ADAPTER = "outbound-resourceadapter";
    public static String INBOUND_RESOURCE_ADAPTER = "inbound-resourceadapter";
    public static String CONNECTION_DEFINITION = "connection-definition";
    public static String RESOURCE_ADAPTER_CLASS = "resourceadapter-class";
    public static String MSG_ADAPTER = "messageadapter";
    public static String MSG_LISTENER = "messagelistener";
    public static String MSG_LISTENER_TYPE = "messagelistener-type";
    public static String REQUIRED_WORK_CONTEXT = "required-work-context";
    public static String ADMIN_OBJECT = "adminobject";
    public static String ADMIN_OBJECT_INTERFACE = "adminobject-interface";
    public static String ADMIN_OBJECT_CLASS = "adminobject-class";
    public static String ACTIVATION_SPEC = "activationspec";
    public static String ACTIVATION_SPEC_CLASS = "activationspec-class";
    public static String REQUIRED_CONFIG_PROP = "required-config-property";
    public static String CONNECTION = "connection";
    public static String CONNECTION_FACTORY = "connectionfactory";
    
    //FIXME.  the following are no longer valid. need clean up when 
    //inbound-ra-class is completed removed from the implementation
    public static String INBOUND_RESOURCE_ADAPTER_CLASS = "resourceadapter-class";
    public static String MSG_LISTENER_NAME = "messagelistener-name";
    
    // Connector DD element valid values
    public static final String DD_BASIC_PASSWORD    = "BasicPassword";
    public static final String DD_KERBEROS          = "Kerbv5";
    public static final String DD_NO_TRANSACTION    = "NoTransaction";
    public static final String DD_LOCAL_TRANSACTION = "LocalTransaction";
    public static final String DD_XA_TRANSACTION    = "XATransaction";    
}
