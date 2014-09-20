/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web;

/**
 * Static constants for this package.
 */

public final class Constants {

    public static final String Package = "com.sun.enterprise.web";

    /**
     * Path to web application context.xml configuration file.
     */

    public static final String WEB_CONTEXT_XML = "META-INF/context.xml";

    /**
     * The default location of the global context.xml file.
     *
     * Path to global context.xml (relative to instance root).
     */
    public static final String DEFAULT_CONTEXT_XML = "config/context.xml";

    /**
     * The default web application's deployment descriptor location.
     *
     * This path is relative to catalina.home i.e. the instance root directory.
     */
    public static final String DEFAULT_WEB_XML = "config/default-web.xml";

    /**
     * The system-assigned default web module's name/identifier.
     *
     * This has to be the same value as is in j2ee/WebModule.cpp.
     */
    public static final String DEFAULT_WEB_MODULE_NAME = "__default-web-module";

    /**
     * The separator character between an application name and the web
     * module name within the application.
     */
    public static final String NAME_SEPARATOR = ":";

    /**
     * The string to prefix to the name of the web module when a web module
     * is designated to be the default-web-module for a virtual server.
     *
     * This serves as a way to differentiate the web module from the
     * variant that is deployed as a 'default web module' at a context root
     * of "".
     */
    public static final String DEFAULT_WEB_MODULE_PREFIX = "__default-";

    /**
     * The Apache Jasper JSP servlet class name.
     */
    public static final String APACHE_JSP_SERVLET_CLASS =
        "org.apache.jasper.servlet.JspServlet";

    public static final String JSP_URL_PATTERN="*.jsp";


    public static final String REQUEST_START_TIME_NOTE =
        "com.sun.enterprise.web.request.startTime";

    public static final String ACCESS_LOG_PROPERTY = "accesslog";

    public static final String ACCESS_LOG_BUFFER_SIZE_PROPERTY =
        "accessLogBufferSize";

    public static final String ACCESS_LOG_WRITE_INTERVAL_PROPERTY =
        "accessLogWriteInterval";

    public static final String ACCESS_LOGGING_ENABLED = "accessLoggingEnabled";

    public static final String SSO_ENABLED = "sso-enabled";

    public static final String ERROR_REPORT_VALVE = "errorReportValve";

    // services attribute in ServletContext
    public static final String HABITAT_ATTRIBUTE = "org.glassfish.servlet.habitat";

    // WebModule attributes in ServletContext
    // available only during ServletContextListener.contextInitialized
    public static final String DEPLOYMENT_CONTEXT_ATTRIBUTE =
        "com.sun.enterprise.web.WebModule.DeploymentContext";
    public static final String IS_DISTRIBUTABLE_ATTRIBUTE = "com.sun.enterprise.web.WebModule.isDistributable";
    public static final String ENABLE_HA_ATTRIBUTE = "com.sun.enterprise.web.WebModule.enableHA";
}
