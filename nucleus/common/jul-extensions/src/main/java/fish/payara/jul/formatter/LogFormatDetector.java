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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]

package fish.payara.jul.formatter;

import fish.payara.jul.tracing.PayaraLoggingTracer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonReader;


/**
 * Detector of known standardized log formats.
 */
public class LogFormatDetector {

    private static final String GZIP_EXTENSION = ".gz";
    private static final int ODL_SUBSTRING_LEN = 5;
    private static final String ODL_LINE_BEGIN_REGEX = "\\[[\\-\\:\\d]{4}";
    private static final Pattern ODL_PATTERN = Pattern.compile(ODL_LINE_BEGIN_REGEX);

    /**
     * {@link Pattern} string for usual time format: HH:mm:ss.SSS
     */
    public static final String P_TIME = "\\d\\d:\\d\\d:\\d\\d.\\d\\d\\d";
    /**
     * {@link Pattern} string for usual time zone format: 02:00 or Z
     */
    public static final String P_TIMEZONE = "([0-9:.+-]{6}|Z)";
    /**
     * {@link Pattern} string for usual ISO-8601 timestamp format: 2021-05-20T12:45:33.123Z
     */
    public static final String P_TIMESTAMP = "[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T" + P_TIME + P_TIMEZONE;

    /**
     * {@link Pattern} string for {@link Level} name: usually upper case letters.
     */
    public static final String P_LEVEL_NAME = "[A-Z]+";
    /**
     * {@link Pattern} string for {@link Level} value: usually int value.
     */
    public static final String P_LEVEL_VALUE = "[0-9]{3,4}";
    /**
     * {@link Pattern} string for usual {@link Logger} name: if present, contains letters and dots.
     */
    public static final String P_LOGGER_NAME = "[a-z.]*";
    /**
     * {@link Pattern} string for usual message key used with resource bundles: if present, letters,
     * dots and numbers.
     */
    public static final String P_MESSAGE_KEY = "[a-zA-Z0-9.]*";
    /**
     * {@link Pattern} string for a product id: any non-mandatory text.
     */
    public static final String P_PRODUCT_ID = ".*";

    /**
     * @param logFile
     * @return full class name of the concrete detected {@link Formatter} or null if the file is
     *         null or could not be read.
     */
    public String detectFormatter(final File logFile) {
        if (logFile == null || !logFile.canRead()) {
            return null;
        }
        final String firstLine;
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            firstLine = br.readLine();
        } catch (Exception e) {
            PayaraLoggingTracer.error(getClass(), e.getMessage(), e);
            return null;
        }

        return detectFormatter(firstLine);
    }


    /**
     * @param firstLine
     * @return null for unknown file format, full class name otherwise.
     */
    public String detectFormatter(final String firstLine) {
        if (firstLine == null || firstLine.isEmpty()) {
            return null;
        }
        if (isODLFormatLogHeader(firstLine)) {
            return ODLLogFormatter.class.getName();
        }
        if (isUniformFormatLogHeader(firstLine)) {
            return UniformLogFormatter.class.getName();
        }
        if (isJSONFormatLogHeader(firstLine)) {
            return JSONLogFormatter.class.getName();
        }
        if (isOneLineLFormatLogHeader(firstLine)) {
            return OneLineFormatter.class.getName();
        }
        return null;
    }


    /**
     * @param firstLine
     * @return true if the given line is probably a beginning of a ODL log record.
     */
    public boolean isODLFormatLogHeader(final String firstLine) {
        return firstLine.length() > ODL_SUBSTRING_LEN
            && ODL_PATTERN.matcher(firstLine.substring(0, ODL_SUBSTRING_LEN)).matches()
            && countOccurrences(firstLine, '[') > 4;
    }


    /**
     * @param firstLine
     * @return true if the given line is probably a beginning of a {@link OneLineFormatter}'s log record.
     */
    public boolean isOneLineLFormatLogHeader(final String firstLine) {
        return firstLine.matches(P_TIME + "\\s+[A-Z]{4,10}\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+.+");
    }


    /**
     * @param firstLine
     * @return true if the given line is probably a beginning of a Uniform log record.
     */
    public boolean isUniformFormatLogHeader(final String firstLine) {
        return firstLine.startsWith("[#|") && countOccurrences(firstLine, '|') > 4;
    }


    /**
     * @param logRecord String to test if json
     * @return true if the line is a valid JSON log record
     */
    public boolean isJSONFormatLogHeader(final String logRecord) {
        if (logRecord.length() < 10 || !logRecord.startsWith("{\"")) {
            return false;
        }
        try (JsonReader reader = Json.createReader(new StringReader(logRecord))) {
            reader.read();
            return true;
        } catch (final Exception ex) {
            return false;
        }
    }

    /**
     * Determines whether the given file is compressed (name ends with .gz).
     *
     * @param filename
     * @return true if the filename ends with {@value #GZIP_EXTENSION}
     */
    public boolean isCompressedFile(final String filename) {
        return filename.endsWith(GZIP_EXTENSION);
    }


    private int countOccurrences(final String firstLine, final char typicalCharacter) {
        int count = 0;
        for (int i = 0; i < firstLine.length(); i++) {
            if (firstLine.charAt(i) == typicalCharacter) {
                count++;
            }
        }
        return count;
    }
}
