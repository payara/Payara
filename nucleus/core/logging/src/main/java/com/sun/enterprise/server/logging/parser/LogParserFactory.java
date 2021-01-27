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
// Portions Copyright [2016-2020] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.server.logging.parser;

import fish.payara.logging.jul.LogFormatHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class LogParserFactory {

    private static final Logger LOG = Logger.getLogger(LogParserFactory.class.getName());
    static final String NEWLINE = System.getProperty("line.separator");

    private enum LogFormat {
        UNIFORM_LOG_FORMAT,
        ODL_LOG_FORMAT,
        UNKNOWN_LOG_FORMAT
    }

    private static final LogParserFactory SINGLETON = new LogParserFactory();
    private final LogFormatHelper logFormatHelper;

    public static LogParserFactory getInstance() {
        return SINGLETON;
    }


    private LogParserFactory() {
        logFormatHelper = new LogFormatHelper();
    }

    public LogParser createLogParser(final File logFile) throws LogParserException, IOException {
        final String firstLine;
        try (BufferedReader reader = createReader(logFile)) {
            firstLine = reader.readLine();
        }
        final LogFormat format = detectLogFormat(firstLine);
        LOG.fine(() -> "Detected log format=" + format + " for line: " + firstLine);
        switch (format) {
            case UNIFORM_LOG_FORMAT:
                return new UniformLogParser(logFile.getName());
            case ODL_LOG_FORMAT:
                return new ODLLogParser(logFile.getName());
            default:
                return new RawLogParser(logFile.getName());
        }
    }


    private BufferedReader createReader(File logFile) throws IOException {
        if (logFormatHelper.isCompressedFile(logFile.getName())) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(logFile))));
        }
        return new BufferedReader(new FileReader(logFile));
    }


    private LogFormat detectLogFormat(final String line) {
        if (line != null) {
            if (logFormatHelper.isODLFormatLogHeader(line)) {
                return LogFormat.ODL_LOG_FORMAT;
            } else if (logFormatHelper.isUniformFormatLogHeader(line)) {
                return LogFormat.UNIFORM_LOG_FORMAT;
            }
        }
        return LogFormat.UNKNOWN_LOG_FORMAT;
    }

}
