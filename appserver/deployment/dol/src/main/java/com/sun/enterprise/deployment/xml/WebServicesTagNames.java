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

/**
 * Repository of all the JSR 109 deployment descriptor elements
 *
 * @author Kenneth Saks
 */
public class WebServicesTagNames {

    public static final String IBM_NAMESPACE = "http://www.ibm.com/webservices/xsd";
    
    public static final String TRANSPORT_GUARANTEE = "transport-guarantee";
    public static final String CALL_PROPERTY = "call-property";
    public static final String WEB_SERVICE_ENDPOINT = "webservice-endpoint";
    public static final String ENDPOINT_ADDRESS_URI = "endpoint-address-uri";
    public static final String EJB_LINK = "ejb-link";
    public static final String CLIENT_WSDL_PUBLISH_URL="wsdl-publish-location";
    public static final String FINAL_WSDL_URL = "final-wsdl-location";
    public static final String SERVLET_LINK = "servlet-link";
    public static final String SERVLET_IMPL_CLASS = "servlet-impl-class";
    public static final String SERVICE_IMPL_BEAN = "service-impl-bean";
    public static final String SERVICE_QNAME = "service-qname";
    public static final String WEB_SERVICES_CLIENT = "webservicesclient";
    public static final String WEB_SERVICES = "webservices";
    public static final String WEB_SERVICE = "webservice-description";
    public static final String COMPONENT_SCOPED_REFS = "component-scoped-refs";
    public static final String SERVICE_REF = "service-ref";
    public static final String SERVICE_REF_NAME = "service-ref-name";
    public static final String SERVICE_INTERFACE = "service-interface";
    public static final String SERVICE_ENDPOINT_INTERFACE = 
        "service-endpoint-interface";
    public static final String WEB_SERVICE_DESCRIPTION_NAME = 
        "webservice-description-name";
    public static final String WSDL_PORT = "wsdl-port";
    public static final String RESPECT_BINDING = "respect-binding";
    public static final String RESPECT_BINDING_ENABLED = "enabled";
    public static final String ADDRESSING = "addressing";
    public static final String ADDRESSING_ENABLED = "enabled";
    public static final String ADDRESSING_REQUIRED = "required";
    public static final String ADDRESSING_RESPONSES = "responses";
    public static final String WSDL_SERVICE = "wsdl-service";
    public static final String ENABLE_MTOM= "enable-mtom";
    public static final String MTOM_THRESHOLD= "mtom-threshold";
    public static final String PROTOCOL_BINDING = "protocol-binding";
    public static final String HANDLER_CHAINS = "handler-chains";
    public static final String HANDLER_CHAIN= "handler-chain";
    public static final String SERVICE_NAME_PATTERN = "service-name-pattern";
    public static final String PORT_NAME_PATTERN = "port-name-pattern";
    public static final String PROTOCOL_BINDINGS = "protocol-bindings";
    public static final String PORT_INFO = "port-info";
    public static final String STUB_PROPERTY = "stub-property";
    public static final String TIE_CLASS = "tie-class";
    public static final String DEBUGGING_ENABLED = "debugging-enabled";
    public static final String SERVICE_IMPL_CLASS = "service-impl-class";
    public static final String WSDL_FILE = "wsdl-file";
    public static final String WSDL_OVERRIDE = "wsdl-override";
    public static final String JAXRPC_MAPPING_FILE = "jaxrpc-mapping-file";
    public static final String JAXRPC_MAPPING_FILE_ROOT = "java-wsdl-mapping";
    public static final String JAVA_XML_TYPE_MAPPING = "java-xml-type-mapping";
    public static final String EXCEPTION_MAPPING = "exception-mapping";
    public static final String SERVICE_INTERFACE_MAPPING = "service-interface-mapping";
    public static final String SERVICE_ENDPOINT_INTERFACE_MAPPING = "service-endpoint-interface-mapping";

    public static final String PORT_COMPONENT = "port-component";
    public static final String PORT_COMPONENT_NAME = "port-component-name";
    public static final String PORT_COMPONENT_REF = "port-component-ref";
    public static final String PORT_COMPONENT_LINK = "port-component-link";
    public static final String NAMESPACE_URI = "namespaceURI";
    public static final String LOCAL_PART = "localpart";
    public static final String PACKAGE_TYPE = "package-type";
    public static final String MAPPED_NAME = "mapped-name";
    public static final String SERVICE_REF_TYPE="service-ref-type";

    public static final String COMPONENT_NAME = "component-name";
    public static final String ENDPOINT_ADDRESS = "endpoint-address";   

    public static final String HANDLER = "handler";
    public static final String HANDLER_NAME = "handler-name";
    public static final String HANDLER_CLASS = "handler-class";
    public static final String INIT_PARAM = "init-param";
    public static final String INIT_PARAM_NAME = "param-name";
    public static final String INIT_PARAM_VALUE = "param-value";
    public static final String SOAP_HEADER = "soap-header";
    public static final String SOAP_ROLE = "soap-role";
    public static final String HANDLER_PORT_NAME = "port-name";

    // security
    public static final String AUTH_SOURCE = "auth-source";
    public static final String AUTH_RECIPIENT = "auth-recipient";
    public static final String REQUEST_PROTECTION = "request-protection";
    public static final String RESPONSE_PROTECTION = "response-protection";
    public static final String MESSAGE = "message";
    public static final String OPERATION_NAME = "operation-name";
    public static final String MESSAGE_SECURITY = "message-security";
    public static final String MESSAGE_SECURITY_BINDING =
        "message-security-binding";
    public static final String AUTH_LAYER = "auth-layer";
    public static final String PROVIDER_ID = "provider-id";
}
