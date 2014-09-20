/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.webservices.connector;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author Lukas Jungmann
 */
public final class LogUtils {

    private static final String LOGMSG_PREFIX = "AS-WSCONNECTOR";

    @LogMessagesResourceBundle
    public static final String LOG_MESSAGES = "org.glassfish.webservices.connector.LogMessages";

    @LoggerInfo(subsystem = "WEBSERVICES", description = "Web Services Connector Logger", publish = true)
    public static final String LOG_DOMAIN = "javax.enterprise.webservices.connector";

    private static final Logger LOGGER = Logger.getLogger(LOG_DOMAIN, LOG_MESSAGES);

    public static Logger getLogger() {
        return LOGGER;
    }
    
    @LogMessageInfo(
            message = "Invalid Deployment Descriptors element {0} value {1}.",
            comment = "{0} - prefix, {1} - localname",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String INVALID_DESC_MAPPING_FAILURE = LOGMSG_PREFIX + "-00046";

    @LogMessageInfo(
            message = "Following exception was thrown",
            level = "FINE")
    public static final String EXCEPTION_THROWN = LOGMSG_PREFIX + "-00050";

    @LogMessageInfo(
            message = "JAX-WS RI specific descriptor ({1}) is found in the archive {0} and \\n"
                    + "hence Enterprise Web Service (109) deployment is disabled for this archive"
                    + " to avoid duplication of services.",
            comment = "{0} - archive name, {1} - descriptor path",
            level = "INFO")
    public static final String DEPLOYMENT_DISABLED = LOGMSG_PREFIX + "-00057";

    @LogMessageInfo(
            message = "Handler class {0} specified in deployment descriptor not found.",
            comment = "{0} - class name",
            level = "WARNING")
    public static final String DDHANDLER_NOT_FOUND = LOGMSG_PREFIX + "-00201";

    @LogMessageInfo(
            message = "Handler class {0} specified in handler file {1} cannot be loaded.",
            comment = "{0} - class name, {1} - file name",
            level = "WARNING")
    public static final String HANDLER_FILE_HANDLER_NOT_FOUND = LOGMSG_PREFIX + "-00202";

    @LogMessageInfo(
            message = "Warning : Web service endpoint {0} is not tied to a component.",
            comment = "{0} - endpoint name",
            level = "INFO")
    public static final String WS_NOT_TIED_TO_COMPONENT = LOGMSG_PREFIX + "-00203";

    @LogMessageInfo(
            message = "Warning: Web service endpoint {0} component link {1} is not valid.",
            comment = "{0} - endpoint name, {1} - link name",
            level = "INFO")
    public static final String WS_COMP_LINK_NOT_VALID = LOGMSG_PREFIX + "-00204";

    @LogMessageInfo(
            message = "URL mapping for web service {0} already exists. Is port-component-name in webservices.xml correct?",
            comment = "{0} - endpoint name",
            level = "SEVERE",
            cause = "Invalid port-component-name value in webservices.xml.",
            action = "Fix port-component-name element in webservices.xml.")
    public static final String WS_URLMAPPING_EXISTS = LOGMSG_PREFIX + "-00205";
}
