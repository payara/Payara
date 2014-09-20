/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.client.utils;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author jdlee
 */
public class RestClientLogging {
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.admin.rest.client.utils.LogMessages";
    @LoggerInfo(subsystem = "REST", description = "REST Client Logger", publish = true)
    public static final String REST_CLIENT_LOGGER = "javax.enterprise.admin.rest.client";
    public static final Logger logger = Logger.getLogger(REST_CLIENT_LOGGER, SHARED_LOGMESSAGE_RESOURCE);
    @LogMessageInfo(
            message = "An unsupported encoding was requested: {0}.",
            cause = "The input supplied can not be encoded in the requested encoding.",
            action = "Verify that the input is valid.",
            level = "SEVERE")
    public static final String REST_CLIENT_ENCODING_ERROR = "NCLS-RSCL-00001";
    @LogMessageInfo(
            message = "An error occurred while processing an XML document.",
            cause = "The input provided could not be read as an XML document.",
            action = "Verify that the document provided is a valid XML document.", 
            level = "SEVERE")
    public static final String REST_CLIENT_XML_STREAM_ERROR = "NCLS-RSCL-00002";
    @LogMessageInfo(
            message = "An I/O exception occurred.",
            cause = "An error occured while closing an InputStream.",
            action = "The error is not recoverable.", 
            level = "SEVERE")
    public static final String REST_CLIENT_IO_ERROR = "NCLS-RSCL-00003";
    @LogMessageInfo(
            message = "An error occurred while processing a JSON object.",
            cause = "An invalid JSON string was provided and could not be read.",
            action = "Verify that the JSON string is valid and retry the request.", 
            level = "SEVERE")
    public static final String REST_CLIENT_JSON_ERROR = "NCLS-RSCL-00004";
}
