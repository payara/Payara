/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.loader;

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
    private static final String SHARED_LOGMESSAGE_RESOURCE =
            "org.glassfish.web.loader.LogMessages";

    @LoggerInfo(subsystem="WEB", description="WEB Util Logger", publish=true)
    private static final String WEB_UTIL_LOGGER = "javax.enterprise.web.util";

    public static final Logger LOGGER =
            Logger.getLogger(WEB_UTIL_LOGGER, SHARED_LOGMESSAGE_RESOURCE);

    private LogFacade() {}

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final String prefix = "AS-WEB-UTIL-";

    @LogMessageInfo(
            message = "Resource '{0}' is missing",
            level = "SEVERE",
            cause = "A naming exception is encountered",
            action = "Check the list of resources")
    public static final String MISSING_RESOURCE = prefix + "00001";

    @LogMessageInfo(
            message = "Failed tracking modifications of '{0} : {1}",
            level = "SEVERE",
            cause = "A ClassCastException is encountered",
            action = "Check if the object is an instance of the class")
    public static final String FAILED_TRACKING_MODIFICATIONS = prefix + "00002";

    @LogMessageInfo(
            message = "WebappClassLoader.findClassInternal({0}) security exception: {1}",
            level = "WARNING",
            cause = "An AccessControlException is encountered",
            action = "Check if the resource is accessible")
    public static final String FIND_CLASS_INTERNAL_SECURITY_EXCEPTION = prefix + "00003";

    @LogMessageInfo(
            message = "Security Violation, attempt to use Restricted Class: {0}",
            level = "INFO")
    public static final String SECURITY_EXCEPTION = prefix + "00004";

    @LogMessageInfo(
            message = "Class {0} has unsupported major or minor version numbers, which are greater than those found in the Java Runtime Environment version {1}",
            level = "WARNING")
    public static final String UNSUPPORTED_VERSION = prefix + "00005";

    @LogMessageInfo(
            message = "Unable to load class with name [{0}], reason: {1}",
            level = "WARNING")
    public static final String UNABLE_TO_LOAD_CLASS = prefix + "00006";

    @LogMessageInfo(
            message = "The web application [{0}] registered the JDBC driver [{1}] but failed to unregister it when the web application was stopped. To prevent a memory leak, the JDBC Driver has been forcibly unregistered.",
            level = "WARNING")
    public static final String CLEAR_JDBC = prefix + "00007";

    @LogMessageInfo(
            message = "JDBC driver de-registration failed for web application [{0}]",
            level = "WARNING")
    public static final String JDBC_REMOVE_FAILED = prefix + "00008";

    @LogMessageInfo(
            message = "Exception closing input stream during JDBC driver de-registration for web application [{0}]",
            level = "WARNING")
    public static final String JDBC_REMOVE_STREAM_ERROR = prefix + "00009";

    @LogMessageInfo(
            message = "This web container has not yet been started",
            level = "WARNING")
    public static final String NOT_STARTED = prefix + "00010";

    @LogMessageInfo(
            message = "Failed to check for ThreadLocal references for web application [{0}]",
            level = "WARNING")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_FAIL = prefix + "00011";

    @LogMessageInfo(
            message = "Unable to determine string representation of key of type [{0}]",
            level = "SEVERE",
            cause = "An Exception occurred",
            action = "Check the exception for error")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_KEY = prefix + "00012";

    @LogMessageInfo(
            message = "Unknown",
            level = "INFO")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_UNKNOWN = prefix + "00013";

    @LogMessageInfo(
            message = "Unable to determine string representation of value of type [{0}]",
            level = "SEVERE",
            cause = "An Exception occurred",
            action = "Check the exception for error")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_BAD_VALUE = prefix + "00014";

    @LogMessageInfo(
            message = "The web application [{0}] created a ThreadLocal with key of type [{1}] (value [{2}]). The ThreadLocal has been correctly set to null and the key will be removed by GC.",
            level = "FINE")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS_DEBUG = prefix + "00015";

    @LogMessageInfo(
            message = "The web application [{0}] created a ThreadLocal with key of type [{1}] (value [{2}]) and a value of type [{3}] (value [{4}]) but failed to remove it when the web application was stopped. Threads are going to be renewed over time to try and avoid a probable memory leak.",
            level = "SEVERE",
            cause = "Failed to remove a ThreadLocal when the web application was stopped",
            action = "Threads are going to be renewed over time to try and avoid a probable memory leak.")
    public static final String CHECK_THREAD_LOCALS_FOR_LEAKS = prefix + "00016";

    @LogMessageInfo(
            message = "Failed to find class sun.rmi.transport.Target to clear context class loader for web application [{0}]. This is expected on non-Sun JVMs.",
            level = "INFO")
    public static final String CLEAR_RMI_INFO = prefix + "00017";

    @LogMessageInfo(
            message = "Failed to clear context class loader referenced from sun.rmi.transport.Target for web application [{0}]",
            level = "WARNING")
    public static final String CLEAR_RMI_FAIL = prefix + "00018";

    @LogMessageInfo(
            message = "Removed [{0}] ResourceBundle references from the cache for web application [{1}]",
            level = "FINE")
    public static final String CLEAR_REFERENCES_RESOURCE_BUNDLES_COUNT = prefix + "00019";

    @LogMessageInfo(
            message = "Failed to clear ResourceBundle references for web application [{0}]",
            level = "SEVERE",
            cause = "An Exception occurred",
            action = "Check the exception for error")
    public static final String CLEAR_REFERENCES_RESOURCE_BUNDLES_FAIL = prefix + "00020";

    @LogMessageInfo(
            message = "Illegal JAR entry detected with name {0}",
            level = "INFO")
    public static final String ILLEGAL_JAR_PATH = prefix + "00021";

    @LogMessageInfo(
            message = "Unable to validate JAR entry with name {0}",
            level = "INFO")
    public static final String VALIDATION_ERROR_JAR_PATH = prefix + "00022";

    @LogMessageInfo(
            message = "Unable to create {0}",
            level = "WARNING")
    public static final String UNABLE_TO_CREATE = prefix + "00023";

    @LogMessageInfo(
            message = "Unable to delete {0}",
            level = "WARNING")
    public static final String UNABLE_TO_DELETE = prefix + "00024";

    @LogMessageInfo(
            message = "Unable to read data for class with name [{0}]",
            level = "WARNING")
    public static final String READ_CLASS_ERROR = prefix + "00025";

    @LogMessageInfo(
            message = "Unable to purge bean classes from BeanELResolver",
            level = "WARNING")
    public static final String UNABLE_PURGE_BEAN_CLASSES = prefix + "00026";

    @LogMessageInfo(
            message = "extra-class-path component [{0}] is not a valid pathname",
            level = "SEVERE",
            cause = "A naming exception is encountered",
            action = "Check the list of resources")
    public static final String CLASSPATH_ERROR = prefix + "00027";

    @LogMessageInfo(
            message = "The clearReferencesStatic is not consistent in context.xml for virtual servers",
            level = "WARNING")
    public static final String INCONSISTENT_CLEAR_REFERENCE_STATIC = prefix + "00028";

    @LogMessageInfo(
            message = "class-loader attribute dynamic-reload-interval in sun-web.xml not supported",
            level = "WARNING")
    public static final String DYNAMIC_RELOAD_INTERVAL = prefix + "00029";

    @LogMessageInfo(
            message = "Property element in sun-web.xml has null 'name' or 'value'",
            level = "WARNING")
    public static final String NULL_WEB_PROPERTY = prefix + "00030";

    @LogMessageInfo(
            message = "Ignoring invalid property [{0}] = [{1}]",
            level = "WARNING")
    public static final String INVALID_PROPERTY = prefix + "00031";

    @LogMessageInfo(
            message = "The xml element should be [{0}] rather than [{1}]",
            level = "INFO")
    public static final String UNEXPECTED_XML_ELEMENT = prefix + "00032";

    @LogMessageInfo(
            message = "This is an unexpected end of document",
            level = "WARNING")
    public static final String UNEXPECTED_END_DOCUMENT = prefix + "00033";

    @LogMessageInfo(
            message = "Unexpected type of ClassLoader. Expected: java.net.URLClassLoader, got: {0}",
            level = "WARNING")
    public static final String WRONG_CLASSLOADER_TYPE = prefix + "00034";

    @LogMessageInfo(
            message = "Unable to load class {0}, reason: {1}",
            level = "FINE")
    public static final String CLASS_LOADING_ERROR = prefix + "00035";

    @LogMessageInfo(
            message = "Invalid URLClassLoader path component: [{0}] is neither a JAR file nor a directory",
            level = "WARNING")
    public static final String INVALID_URL_CLASS_LOADER_PATH = prefix + "00036";

    @LogMessageInfo(
            message = "Error trying to scan the classes at {0} for annotations in which a ServletContainerInitializer has expressed interest",
            level = "SEVERE",
            cause = "An IOException is encountered",
            action = "Verify if the path is correct")
    public static final String IO_ERROR = prefix + "00037";

    @LogMessageInfo(
            message = "Ignoring [{0}] during Tag Library Descriptor (TLD) processing",
            level = "WARNING")
    public static final String TLD_PROVIDER_IGNORE_URL = prefix + "00038";

    @LogMessageInfo(
            message = "Unable to determine TLD resources for [{0}] tag library, because class loader [{1}] for [{2}] is not an instance of java.net.URLClassLoader",
            level = "WARNING")
    public static final String UNABLE_TO_DETERMINE_TLD_RESOURCES = prefix + "00039";
}
