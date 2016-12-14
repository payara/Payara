/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.naming;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import java.util.logging.Logger;

/**
/**
 *
 * Provides the logging facilities.
 *
 * @author Shing Wai Chan
 */
public class LogFacade {
    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE =
            "org.apache.naming.resources.LogMessages";

    @LoggerInfo(subsystem="WEB", description="WEB Naming Logger", publish=true)
    public static final String WEB_NAMING_LOGGER = "javax.enterprise.web.naming";

    public static final Logger LOGGER =
            Logger.getLogger(WEB_NAMING_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final String prefix = "AS-WEB-NAMING-";

    @LogMessageInfo(
            message = "Canonical Pathname cannot be null",
            level = "FINE")
    public static final String FILE_RESOURCES_NULL_CANONICAL_PATH = prefix + "00001";

    @LogMessageInfo(
            message = "Outside webapp not allowed {0} {1} {2}",
            level = "FINE")
    public static final String FILE_RESOURCES_NOT_ALLOWED = prefix + "00002";

    @LogMessageInfo(
            message = "Absolute Pathname cannot be null {0} {1}",
            level = "FINE")
    public static final String FILE_RESOURCES_NULL_ABS_PATH = prefix + "00003";

    @LogMessageInfo(
            message = "Canonical pathname {0} equals to absolute pathname {1} {2}",
            level = "FINE")
    public static final String FILE_RESOURCES_PATH_EQUALS_ABS_PATH = prefix + "00004";

    @LogMessageInfo(
            message = "File cannot be read {0}",
            level = "FINE")
    public static final String FILE_RESOURCES_NOT_EXIST = prefix + "00005";

    @LogMessageInfo(
            message = "Could not get dir listing for {0}",
            level = "WARNING",
            cause = "Some IO error occurred such as bad file permissions",
            action = "Verify the file descriptors")
    public static final String FILE_RESOURCES_LISTING_NULL = prefix + "00006";

    @LogMessageInfo(
            message = "Document base {0} does not exist or is not a readable directory",
            level = "INFO")
    public static final String FILE_RESOURCES_BASE = prefix + "00007";

    @LogMessageInfo(
            message = "Document base cannot be null",
            level = "INFO")
    public static final String RESOURCES_NULL = prefix + "00008";

    @LogMessageInfo(
            message = "Resource {0} not found",
            level = "INFO")
    public static final String RESOURCES_NOT_FOUND = prefix + "00009";

    @LogMessageInfo(
            message = "Name {0} is already bound in this Context",
            level = "INFO")
    public static final String RESOURCES_ALREADY_BOUND = prefix + "00010";

    @LogMessageInfo(
            message = "Bind failed: {0}",
            level = "INFO")
    public static final String RESOURCES_BIND_FAILED = prefix + "00011";

    @LogMessageInfo(
            message = "Unbind failed: {0}",
            level = "INFO")
    public static final String RESOURCES_UNBIND_FAILED = prefix + "00012";

    @LogMessageInfo(
            message = "Failed to rename [{0}] to [{1}]",
            level = "INFO")
    public static final String RESOURCES_RENAME_FAIL = prefix + "00013";

    @LogMessageInfo(
            message = "Unknown context name : {0}",
            level = "INFO")
    public static final String UNKNOWN_CONTEXT = prefix + "00014";

    @LogMessageInfo(
            message = "No naming context bound to this thread",
            level = "INFO")
    public static final String NO_CONTEXT_BOUND_TO_THREAD = prefix + "00015";

    @LogMessageInfo(
            message = "No naming context bound to this class loader",
            level = "INFO")
    public static final String NO_CONTEXT_BOUND_TO_CL = prefix + "00016";

    @LogMessageInfo(
            message = "Name is not bound to a Context",
            level = "INFO")
    public static final String CONTEXT_EXPECTED = prefix + "00017";

    @LogMessageInfo(
            message = "Name is not bound to a Context",
            level = "WARNING")
    public static final String FAIL_RESOLVING_REFERENCE = prefix + "00018";

    @LogMessageInfo(
            message = "Name is not bound to a Context",
            level = "INF")
    public static final String NAME_NOT_BOUND = prefix + "00019";

    @LogMessageInfo(
            message = "Context is read only",
            level = "INFO")
    public static final String READ_ONLY = prefix + "00020";

    @LogMessageInfo(
            message = "Name is not valid",
            level = "INFO")
    public static final String INVALID_NAME = prefix + "00021";

    @LogMessageInfo(
            message = "Name {0} is already bound in this Context",
            level = "INFO")
    public static final String ALREADY_BOUND = prefix + "00022";

    @LogMessageInfo(
            message = "Can't generate an absolute name for this namespace",
            level = "INFO")
    public static final String NO_ABSOLUTE_NAME = prefix + "00023";

    @LogMessageInfo(
            message = "Unable to restore original system properties",
            level = "WARNING")
    public static final String UNABLE_TO_RESTORE_ORIGINAL_SYS_PROPERTIES = prefix + "00024";

    @LogMessageInfo(
            message = "This context must be accessed through a java: URL",
            level = "FINE")
    public static final String NO_JAVA_URL = prefix + "00025";

    @LogMessageInfo(
            message = "Exception closing WAR File {0}",
            level = "WARNING")
    public static final String EXCEPTION_CLOSING_WAR = prefix + "00026";

    @LogMessageInfo(
            message = "Doc base must point to a WAR file",
            level = "INFO")
    public static final String NOT_WAR = prefix + "00027";

    @LogMessageInfo(
            message = "Invalid or unreadable WAR file : {0}",
            level = "INFO")
    public static final String INVALID_WAR = prefix + "00028";
}
