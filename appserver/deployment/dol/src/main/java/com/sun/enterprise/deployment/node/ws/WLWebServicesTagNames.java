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

package com.sun.enterprise.deployment.node.ws;

/**
 * Tag names that appear inside weblogic-webservices.xml
 *
 * @author Rama Pulavarthi
 */
public class WLWebServicesTagNames {

    public static final String WEB_SERVICES = "weblogic-webservices";
    public static final String WEB_SERVICE = "webservice-description";

    public static final String PORT_COMPONENT = "port-component";
    public static final String WEB_SERVICE_DESCRIPTION_NAME = "webservice-description-name";
    public static final String WSDL_PUBLISH_FILE = "wsdl-publish-file";
    public static final String WEBSERVICE_TYPE = "webservice-type";
    public static final String SERVICE_ENDPOINT_ADDRESS = "service-endpoint-address";
    public static final String WEBSERVICE_CONTEXTPATH = "webservice-contextpath";
    public static final String WEBSERVICE_SERVICEURI="webservice-serviceuri";
    public static final String WSDL = "wsdl";
    public static final String WSDL_EXPOSED = "exposed";
    public static final String STREAM_ATTACHMENTS = "stream-attachments";
    public static final String VALIDATE_REQUEST = "validate-request";

    public static final String RELIABILITY_CONFIG = "reliability-config";
    public static final String INACTIVITY_TIMEOUT = "inactivity-timeout";
    public static final String BASE_RETRANSMISSION_INTERVAL = "base-retransmission-interval";
    public static final String RETRANSMISSION_EXPONENTIAL_BACKOFF = "retransmission-exponential-backoff";
    public static final String ACKNOWLEDGEMENT_INTERVAL = "acknowledgement-interval";
    public static final String SEQUENCE_EXPIRATION = "sequence-expiration";
    public static final String BUFFER_RETRY_COUNT = "buffer-retry-count";
    public static final String BUFFER_RETRY_DELAY = "buffer-retry-delay";

    public static final String SERVICE_REFERENCE_DESCRIPTION="service-reference-description";
    public static final String SERVICE_REFERENCE_WSDL_URL="wsdl-url";
    public static final String SERVICE_REFERENCE_PORT_INFO="port-info";
    public static final String SERVICE_REFERENCE_PORT_NAME="port-name";


    //Unsupported config
    public static final String WEBSERVICE_SECURITY = "webservice-security";
    public static final String DEPLOYMENT_LISTENER_LIST = "deployment-listener-list";
    public static final String TRANSACTION_TIMEOUT="transaction-tiemout";
    public static final String CALLBACK_PROTOCOL = "callback-protocol";
    public static final String HTTP_FLUSH_RESPONSE = "http-flush-resposne";
    public static final String HTTP_RESPONSE_BUFFERSIZE = "http-response-buffersize";


}
