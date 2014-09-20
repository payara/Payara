/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.services;

/**
 * A place for everything.  Everything in its place
 * @author bnevins
 */
class Constants {
    ///////////////////////////////////////////////////////////////////////////
    /////           Token  Names   ////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    static final String DATE_CREATED_TN = "DATE_CREATED";
    static final String AS_ADMIN_PATH_TN = "AS_ADMIN_PATH";
    static final String CREDENTIALS_TN = "CREDENTIALS";
    static final String SERVICE_NAME_TN = "NAME";
    static final String SERVICE_TYPE_TN = "TYPE";
    static final String CFG_LOCATION_TN = "LOCATION";
    static final String ENTITY_NAME_TN = "ENTITY_NAME";
    static final String DISPLAY_NAME_TN = "DISPLAY_NAME";
    static final String FQSN_TN = "FQSN";
    static final String AS_ADMIN_USER_TN = "AS_ADMIN_USER";
    static final String AS_ADMIN_PASSWORD_TN = "AS_ADMIN_PASSWORD";
    static final String AS_ADMIN_MASTERPASSWORD_TN = "AS_ADMIN_MASTERPASSWORD";
    //static final String PASSWORD_FILE_PATH_TN        = "PASSWORD_FILE_PATH";
    static final String TIMEOUT_SECONDS_TN = "TIMEOUT_SECONDS";
    static final String OS_USER_TN = "OS_USER";
    static final String PRIVILEGES_TN = "PRIVILEGES";
    static final String START_COMMAND_TN = "START_COMMAND";
    static final String RESTART_COMMAND_TN = "RESTART_COMMAND";
    static final String STOP_COMMAND_TN = "STOP_COMMAND";
    static final String LOCATION_ARGS_START_TN = "LOCATION_ARGS_START";
    static final String LOCATION_ARGS_STOP_TN = "LOCATION_ARGS_STOP";
    static final String LOCATION_ARGS_RESTART_TN = "LOCATION_ARGS_RESTART";
    static final String CREDENTIALS_START_TN = "CREDENTIALS_START";
    static final String CREDENTIALS_STOP_TN = "CREDENTIALS_STOP";
    static final String SERVICEUSER_STOP_TN = "SERVICEUSER_STOP";
    static final String SERVICEUSER_START_TN = "SERVICEUSER_START";
    ///////////////////////////////////////////////////////////////////////////
    /////           Other Constants     ///////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    static final String START_ARG_START = "<startargument>";
    static final String STOP_ARG_START = "<stopargument>";
    static final String START_ARG_END = "</startargument>";
    static final String STOP_ARG_END = "</stopargument>";
    static final String TRACE_PREPEND = "TRACE:  ";
    static final String DRYRUN_PREPEND = "DRYRUN:  ";
    static final String README = "PlatformServices.log";
    static final String SERVICE_NAME_PREFIX = "application/GlassFish/";
    static final String ETC = "/etc";
    static final String INITD = "/etc/init.d";
    static final String REGEXP_PATTERN_BEGIN = "[KS][0-9][0-9]?";

    static final boolean LINUX_HACK;

    static {
        LINUX_HACK = System.getProperty("user.name").indexOf("nevins") >= 0 &&
                Boolean.parseBoolean(System.getenv("LINUX_HACK"));
    }
}
