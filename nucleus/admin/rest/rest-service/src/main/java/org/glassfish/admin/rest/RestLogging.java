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
package org.glassfish.admin.rest;

import java.util.logging.Logger;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 *
 * @author jdlee
 */
public class RestLogging {

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.admin.rest.LogMessages";
    @LoggerInfo(subsystem = "REST", description = "Main REST Logger", publish = true)
    public static final String REST_MAIN_LOGGER = "javax.enterprise.admin.rest";
    public static final Logger restLogger = Logger.getLogger(REST_MAIN_LOGGER, SHARED_LOGMESSAGE_RESOURCE);
    @LogMessageInfo(
            message = "Listening to REST requests at context: {0}/domain.",
            level = "INFO")
    public static final String REST_INTERFACE_INITIALIZED = "NCLS-REST-00001";
    @LogMessageInfo(
            message = "Incorrectly formatted entry in {0}: {1}",
            level = "INFO")
    public static final String INCORRECTLY_FORMATTED_ENTRY = "NCLS-REST-00002";
    @LogMessageInfo(
            message = "An error occurred while processing the request. Please see the server logs for details.",
            cause = "A runtime error occurred. Please see the log file for more details",
            action="See the log file for more details",
            level = "SEVERE")
    public static final String SERVER_ERROR = "NCLS-REST-00003";
    @LogMessageInfo(
            message="The class specified by generator does not implement DefaultsGenerator",
            cause="The generator does not implement the DefaultsGenerator interface",
            action="Modify the generator to implement the DefaultsGenerator interface",
            level="SEVERE")
    public static final String DOESNT_IMPLEMENT_DEFAULTS_GENERATOR = "NCLS-REST-00004";
    @LogMessageInfo(
            message="Unsupported fixed value.  Supported types are String, boolean, Boolean, int, Integer, long, Long, double, Double, float, and Float",
            cause="The RestModel property has specified an unsupported data type",
            action="Modify the model to use one of the supported types",
            level="SEVERE")
    public static final String UNSUPPORTED_FIXED_VALUE = "NCLS-REST-00005";
    @LogMessageInfo(
            message="Fixed value type does not match the property type",
            cause="The value for the given property can not be converted to the property's type",
            action="Check the input data",
            level="SEVERE")
    public static final String VALUE_DOES_NOT_MATCH_TYPE = "NCLS-REST-00006";
    @LogMessageInfo(message="Cannot marshal",
            cause="The system is unable to generate XML for the given object",
            action="Check the logs for more details",
            level="SEVERE")
    public static final String CANNOT_MARSHAL = "NCLS-REST-00007";
    @LogMessageInfo(message="Unexpected exception during command execution. {0}", level="WARNING")
    public static final String UNEXPECTED_EXCEPTION = "NCLS-REST-00008";
    @LogMessageInfo(message="Unable to delete directory {0}.  Will attempt deletion again upon JVM exit.",level="WARNING")
    public static final String UNABLE_DELETE_DIRECTORY = "NCLS-REST-00009";
    @LogMessageInfo(message="Unable to delete file %s.  Will attempt deletion again upon JVM exit.",level="WARNING")
    public static final String UNABLE_DELETE_FILE = "NCLS-REST-00010";
    @LogMessageInfo(message="{0}:  {1}",level="INFO")
    public static final String TIMESTAMP_MESSAGE = "NCLS-REST-00011";
    @LogMessageInfo(message="Compilation failed.", level="INFO")
    public static final String COMPILATION_FAILED = "NCLS-REST-00012";
    @LogMessageInfo(message="File creation failed: {0}",
            cause="The system was unable to create the specified file.",
            action="Verify that the filesystem is writable and has sufficient disk space",
            level="SEVERE")
    public static final String FILE_CREATION_FAILED = "NCLS-REST-00013";
    @LogMessageInfo(message="Directory creation failed: {0}", level="INFO")
    public static final String DIR_CREATION_FAILED = "NCLS-REST-00014";
    @LogMessageInfo(message="Unexpected exception during initilization.",
            cause="The system is unable to init ReST interface",
            action="Check the logs for more details",
            level="SEVERE")
    public static final String INIT_FAILED = "NCLS-REST-00015";
    @LogMessageInfo(message="I/O exception: {0}", 
            cause="See server log for details", 
            action="See server log for details.",
            level="SEVERE")
    public static final String IO_EXCEPTION = "NCLS-REST-00016";
}
