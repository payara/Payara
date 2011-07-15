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

package com.sun.enterprise.jbi.serviceengine.util.soap;

import java.util.logging.Level;


/**
 * This is a constants repository and is used by all SOAP binding components running in
 * the JBI container.
 *
 * @author Sun Microsystems, Inc.
 */
public interface SOAPConstants
{
    /**
     * Default value for max threads.
     */
    int DEFAULT_MAX_THREADS = 10;

    /**
     * Default value for min threads.
     */
    int DEFAULT_MIN_THREADS = 2;

    /**
     * Default Thread sleep time in milliseconds.
     */
    int DEFAULT_THREAD_SLEEP_TIME = 100;

    /**
     * Message Context property which holds fault string.
     */
    String FAULT_STRING_PROPERTY_NAME = "FAULT_STRING";

    /**
     * Message Context property which holds fault actor name.
     */
    String FAULT_CODE_PROPERTY_NAME = "FAULT_ACTOR";

    /**
     * Message Context property which holds SOAP header information.
     */
    String HEADER_PROPERTY_NAME = "SoapHeader";

    /**
     * Message Context property which holds soap action value.
     */
    String SOAP_ACTION_PROPERTY_NAME = "SOAP_ACTION_URI";

    /**
     * Message context property which hold http response code.
     */
    String HTTP_RESPONSE_CODE = "HTTP_RESPONSE_CODE";

    /**
     * Indicates that an exception has been thrown while the request was being processed.
     */
    int INTERNAL_SERVER_ERROR = 401;

    /**
     * Indicates that the service could not be invoked in the specified time.
     */
    int SERVICE_TIMEOUT = 403;

    /**
     * Indicates that the request does not conform to the request format.
     */
    int BAD_REQUEST_FORMAT = 404;

    /**
     * Indicates that the endpoint does not support the current operation.
     */
    int OPERATION_NOT_SUPPORTED = 405;

    /**
     * Indicates that an endpoint has been deployed for the service URL and that engine
     * has not activated the inbound service channel.
     */
    int SERVICE_NOT_ACTIVATED = 406;

    /**
     * Indicates that the request message conforms to XML standards but not to SOAP
     * standards.
     */
    int INVALID_SOAP_REQUEST_MESSAGE = 412;

    /**
     * Indicates that an error has been set in the Message Exchange.
     */
    int JBI_ERROR = 103;

    /**
     * Indicates that a fault has been set in the Message Exchange.
     */
    int JBI_FAULT = 102;

    /**
     * Indicates that the message has been successfully processed.
     */
    int JBI_SUCCESS = 101;

    /**
     * Indicates that the message processing has been completed.
     */
    int JBI_DONE = 100;

    /**
     * In_Only Pattern name.
     */
    String IN_ONLY_PATTERN = "in-only";

    /**
     * Robust In_Only Pattern name.
     */
    String ROBUST_IN_ONLY_PATTERN = "robust-in-only";

    /**
     * In_Optional_Out Pattern name.
     */
    String IN_OPTIONAL_OUT_PATTERN = "in-opt-out";

    /**
     * In_Out Pattern name.
     */
    String IN_OUT_PATTERN = "in-out";

    /**
     * Out_Only Pattern name.
     */
    String OUT_ONLY_PATTERN = "out-only";

    /**
     * Out_In Pattern name.
     */
    String ROBUST_OUT_ONLY_PATTERN = "robust-out-only";

    /**
     * Out_In Pattern name.
     */
    String OUT_OPTIONAL_IN_PATTERN = "out-opt-in";

    /**
     * Out_In Pattern name.
     */
    String OUT_IN_PATTERN = "out-in";

    /**
     * HTTP Scheme identifier.
     */
    String HTTP_SCHEME = "http";

    /**
     * HTTPS Scheme identifier.
     */
    String HTTPS_SCHEME = "https";

    /**
     * RPC Style.
     */
    String RPC_STYLE = "rpc";

    /**
     * Document Style.
     */
    String DOCUMENT_STYLE = "document";

    /**
     * Property which holds the registry File Name.
     */
    String REGISTRY_FILE_NAME = "REGISTRY_FILE_NAME";

    /**
     * Property which holds the registry file location.
     */
    String REGISTRY_FILE_LOCATION = "REGISTRY_FILE_LOCATION";

    /**
     * Schema directory name.
     */
    String SCHEMA_DIR_NAME = "schema";

    /**
     * UTF 8.
     */
    String UTF_8 = "utf-8";

    /**
     * UTF-16 representation.
     */
    String UTF_16 = "utf-16";

    /**
     * XML Content Type.
     */
    String XML_CONTENT_TYPE = "text/xml";

    /**
     * XML Content Type value which also contains the charset.
     */
    String XML_CONTENT_TYPE_CHARSET_UTF8 = "text/xml;charset=utf-8";

    /**
     * XML Content Type value which also contains the charset.
     */
    String XML_CONTENT_TYPE_CHARSET_UTF16 = "text/xml;charset=utf-16";

    // SOAP Fault Codes.

    /**
     * Server Fault Code.
     */
    String SERVER_FAULT_CODE = "Server";

    /**
     * Client Fault Code.
     */
    String CLIENT_FAULT_CODE = "Client";

    /**
     * Fault String.
     */
    String FAULT_STRING = "Exception";
    String FAULT_CODE_ELEMENT = "faultcode";
    String FAULT_STRING_ELEMENT = "faultstring";
    String FAULT_DETAIL_ELEMENT = "detail";
    String FAULT_MESSAGE_ELEMENT = "message";
    
    /**
     * Version Mismatch Fault Code.
     */
    String VERSION_MISMATCH_FAULT_CODE = "VersionMismatch";

    /**
     * Default Log Level.
     */
    Level DEFAULT_LOG_LEVEL = Level.INFO;

    /**
     * SOAP Package Name.
     */
    String SOAP_PACKAGE_NAME = "com.sun.jbi.binding.soap";

    /**
     * Package name for thread framework.
     */
    String THREAD_PACKAGE_NAME = SOAP_PACKAGE_NAME + ".threads";

    /**
     * Package name for configuration classes.
     */
    String CONFIG_PACKAGE_NAME = SOAP_PACKAGE_NAME + ".config";

    /**
     * Package name for classes handling outbound requests.
     */
    String OUTBOUND_PACKAGE_NAME = SOAP_PACKAGE_NAME + ".outbound";

    /**
     * Package name for utility classes.
     */
    String UTIL_PACKAGE_NAME = SOAP_PACKAGE_NAME + ".util";

    /**
     * NEW_LINE_CHARACTER.
     */
    String NEW_LINE = System.getProperty("line.separator");

    /**
     * HTTP_SERVLET_REQUEST_PROPERTY.
     */
    String HTTP_SERVLET_REQUEST = "http_servlet_request";

    /**
     * Default Operation name.
     */
    String DEFAULT_OPERATION_NAME = "default";

    /**
     * Default Operation name.
     */
    String DEFAULT_OPERATION_PREFIX = "ns208";

    /**
     * Default Operation name.
     */
    String DEFAULT_XML_NS_SCHEME = "xmlns";
    
    /**
     * Subject.
     */
    String SUBJECT = "subject";

    /**
     * JAXP schema language.
     */
    String JAXP_SCHEMA_LANGUAGE =
        "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    /**
     * XML schema version.
     */
    String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    /**
     * Attribute indicating JAXP schema source.
     */
    String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    /**
     * Name of the deployment schema file.
     */
    String DEPLOY_SCHEMA_FILE = "endpoints.xsd";
    
    
    String SOAP_PREFIX = "Soap";
    String SOAP_ENVELOPE = SOAP_PREFIX + ":Envelope";
    String SOAP_HEADER = SOAP_PREFIX + ":Header";
    String SOAP_BODY = SOAP_PREFIX + ":Body";
    String SOAP_FAULT = SOAP_PREFIX + ":Fault";
    String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";

    int SOAP_BODY_BINDING = 0;
    int SOAP_HEADER_BINDING = 1;
    int SOAP_ATTACHMENT_BINDING = 2;
}
