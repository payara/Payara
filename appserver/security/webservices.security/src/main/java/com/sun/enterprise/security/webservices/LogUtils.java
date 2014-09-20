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
package com.sun.enterprise.security.webservices;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author lukas
 */
public class LogUtils {
private static final String LOGMSG_PREFIX = "AS-WSSECURITY";

    @LogMessagesResourceBundle
    public static final String LOG_MESSAGES = "com.sun.enterprise.security.webservices.LogMessages";

    @LoggerInfo(subsystem = "WEBSERVICES", description = "Web Services Security Logger", publish = true)
    public static final String LOG_DOMAIN = "javax.enterprise.webservices.security";

    private static final Logger LOGGER = Logger.getLogger(LOG_DOMAIN, LOG_MESSAGES);

    public static Logger getLogger() {
        return LOGGER;
    }

    @LogMessageInfo(
            message = "Request processing failed.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String NEXT_PIPE = LOGMSG_PREFIX + "-00001";

    @LogMessageInfo(
            message = "SEC2002: Container-auth: wss: Error validating request.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ERROR_REQUEST_VALIDATION = LOGMSG_PREFIX + "-00002";

    @LogMessageInfo(
            message = "SEC2003: Container-auth: wss: Error securing response.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ERROR_RESPONSE_SECURING = LOGMSG_PREFIX + "-00003";

    @LogMessageInfo(
            message = "SEC2004: Container-auth: wss: Error securing request.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ERROR_REQUEST_SECURING = LOGMSG_PREFIX + "-00004";

    @LogMessageInfo(
            message = "SEC2005: Container-auth: wss: Error validating response.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String ERROR_RESPONSE_VALIDATION = LOGMSG_PREFIX + "-00005";

    @LogMessageInfo(
            message = "SEC2006: Container-auth: wss: Not a SOAP message context.",
            level = "WARNING",
            cause = "unknown",
            action = "unknown")
    public static final String NOT_SOAP = LOGMSG_PREFIX + "-00006";

    @LogMessageInfo(
            message = "EJB Webservice security configuration Failure.",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String EJB_SEC_CONFIG_FAILURE = LOGMSG_PREFIX + "-00007";

    @LogMessageInfo(
            message = "Servlet Webservice security configuration Failure",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String SERVLET_SEC_CONFIG_FAILURE = LOGMSG_PREFIX + "-00008";

    @LogMessageInfo(
            message = "BASIC AUTH username/password http header parsing error for {0}",
            comment = "{0} - endpont",
            level = "WARNING",
            cause = "unknown",
            action = "unknown")
    public static final String BASIC_AUTH_ERROR = LOGMSG_PREFIX + "-00009";

    @LogMessageInfo(
            message = "Servlet Webservice security configuration Failure",
            comment = "{0} - endpont",
            level = "WARNING",
            cause = "unknown",
            action = "unknown")
    public static final String CLIENT_CERT_ERROR = LOGMSG_PREFIX + "-00010";

    @LogMessageInfo(
            message = "Following exception was thrown:",
            level = "SEVERE",
            cause = "unknown",
            action = "unknown")
    public static final String EXCEPTION_THROWN = LOGMSG_PREFIX + "-00011";

}
