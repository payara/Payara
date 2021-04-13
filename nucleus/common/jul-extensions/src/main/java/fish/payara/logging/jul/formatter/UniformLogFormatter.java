/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2016-2021] [Payara Foundation]

package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.cfg.LoggingSystemEnvironment;
import fish.payara.logging.jul.formatter.ExcludeFieldsSupport.SupplementalAttribute;
import fish.payara.logging.jul.i18n.MessageResolver;
import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static java.lang.System.lineSeparator;

/**
 * UniformLogFormatter conforms to the logging format defined by the
 * Log Working Group in Java Webservices Org.
 * The specified format is
 * "[#|DATETIME|LOG_LEVEL|PRODUCT_ID|LOGGER NAME|OPTIONAL KEY VALUE PAIRS|MESSAGE|#]\n"
 *
 * @author Hemanth Puttaswamy
 * @author David Matejcek - refactoring
 */
public class UniformLogFormatter extends AnsiColorFormatter {

    private static final int REC_BUFFER_CAPACITY = 512;

    private static final String INDENT = "  ";
    private static final char FIELD_SEPARATOR = '|';
    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]";
    private static final String PAIR_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = "=";

    private static final String LABEL_CLASSNAME = "ClassName";
    private static final String LABEL_METHODNAME = "MethodName";
    private static final String LABEL_RECORDNUMBER = "RecordNumber";

    private static final MessageResolver MSG_RESOLVER = new MessageResolver();

    private final ExcludeFieldsSupport excludeFieldsSupport;
    private String recordBeginMarker;
    private String recordEndMarker;
    private char recordFieldSeparator;
    private boolean multiLineMode;

    public UniformLogFormatter() {
        this.multiLineMode = true;
        this.excludeFieldsSupport = new ExcludeFieldsSupport();
        this.recordBeginMarker = RECORD_BEGIN_MARKER;
        this.recordEndMarker = RECORD_END_MARKER;
        this.recordFieldSeparator = FIELD_SEPARATOR;
    }

    @Override
    public String formatRecord(final LogRecord record) {
        return formatEnhancedLogRecord(MSG_RESOLVER.resolve(record));
    }

    /**
     * @param recordBeginMarker separates log records, marks beginning of the record. Default: {@value #RECORD_BEGIN_MARKER}
     */
    public void setRecordBeginMarker(final String recordBeginMarker) {
        this.recordBeginMarker = recordBeginMarker == null ? RECORD_BEGIN_MARKER : recordBeginMarker;
    }

    /**
     * @param recordEndMarker separates log records, marks ending of the record. Default: {@value #RECORD_END_MARKER}
     */
    public void setRecordEndMarker(final String recordEndMarker) {
        this.recordEndMarker = recordEndMarker == null ? RECORD_END_MARKER : recordEndMarker;
    }

    /**
     * @param recordFieldSeparator separates log record fields, default: {@value #FIELD_SEPARATOR}
     */
    public void setRecordFieldSeparator(final Character recordFieldSeparator) {
        this.recordFieldSeparator = recordFieldSeparator == null ? FIELD_SEPARATOR : recordFieldSeparator;
    }

    /**
     * @param multiLineMode the multiLineMode to set
     */
    public void setMultiLineMode(final boolean multiLineMode) {
        this.multiLineMode = multiLineMode;
    }

    /**
     * @param excludeFields the excludeFields to set
     */
    public void setExcludeFields(final String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }


    private String formatEnhancedLogRecord(final EnhancedLogRecord record) {
        try {
            final String message = getPrintedMessage(record);
            if (message == null) {
                return "";
            }

            final String timestamp = getDateTimeFormatter().format(record.getTime());
            final Level logLevel = record.getLevel();
            final StringBuilder output = new StringBuilder(REC_BUFFER_CAPACITY).append(recordBeginMarker);
            appendTimestamp(output, timestamp);
            appendLogLevel(output, logLevel);
            appendProductId(output);
            appendLoggerName(output, record.getLoggerName());
            appendDetails(output, record);

            if (multiLineMode) {
                output.append(lineSeparator());
                output.append(INDENT);
            }
            output.append(message);
            output.append(recordEndMarker);
            output.append(lineSeparator()).append(lineSeparator());
            return output.toString();
        } catch (final Exception e) {
            new ErrorManager().error("Error in formatting Logrecord", e, ErrorManager.FORMAT_FAILURE);
            return record.getMessage();
        }
    }


    private void appendTimestamp(final StringBuilder output, final String timestamp) {
        output.append(timestamp);
        output.append(recordFieldSeparator);
    }

    private void appendLogLevel(final StringBuilder output, final Level logLevel) {
        final AnsiColor levelColor = getLevelColor(logLevel);
        if (levelColor != null) {
            output.append(levelColor);
        }
        output.append(logLevel.getName());
        if (levelColor != null) {
            output.append(AnsiColor.RESET);
        }
        output.append(recordFieldSeparator);
    }

    private void appendProductId(final StringBuilder output) {
        final String productId = LoggingSystemEnvironment.getProductId();
        if (productId != null) {
            output.append(productId);
        }
        output.append(recordFieldSeparator);
    }

    private void appendLoggerName(final StringBuilder output, final String loggerName) {
        if (loggerName != null) {
            if (isAnsiColor()) {
                output.append(getLoggerColor());
            }
            output.append(loggerName);
            if (isAnsiColor()) {
                output.append(AnsiColor.RESET);
            }
        }
        output.append(recordFieldSeparator);
    }

    private void appendDetails(final StringBuilder output, final EnhancedLogRecord record) {
        if (!excludeFieldsSupport.isSet(SupplementalAttribute.TID)) {
            output.append("_ThreadID").append(VALUE_SEPARATOR).append(record.getThreadID()).append(PAIR_SEPARATOR);
            output.append("_ThreadName").append(VALUE_SEPARATOR).append(record.getThreadName()).append(PAIR_SEPARATOR);
        }

        // Include the raw long time stamp value in the log
        if (!excludeFieldsSupport.isSet(SupplementalAttribute.TIME_MILLIS)) {
            output.append("_TimeMillis").append(VALUE_SEPARATOR).append(record.getMillis()).append(PAIR_SEPARATOR);
        }

        // Include the integer level value in the log
        final Level level = record.getLevel();
        if (!excludeFieldsSupport.isSet(SupplementalAttribute.LEVEL_VALUE)) {
            output.append("_LevelValue").append(VALUE_SEPARATOR).append(level.intValue()).append(PAIR_SEPARATOR);
        }

        if (record.getMessageKey() != null) {
            output.append("_MessageID").append(VALUE_SEPARATOR).append(record.getMessageKey()).append(PAIR_SEPARATOR);
        }

        if (isPrintSource()) {
            final String sourceClassName = record.getSourceClassName();
            if (sourceClassName != null) {
                output.append(LABEL_CLASSNAME).append(VALUE_SEPARATOR).append(sourceClassName).append(PAIR_SEPARATOR);
            }

            final String sourceMethodName = record.getSourceMethodName();
            if (sourceMethodName != null) {
                output.append(LABEL_METHODNAME).append(VALUE_SEPARATOR).append(sourceMethodName).append(PAIR_SEPARATOR);
            }
        }

        if (isPrintRecordNumber()) {
            final long recNumber = record.getSequenceNumber();
            output.append(LABEL_RECORDNUMBER).append(VALUE_SEPARATOR).append(recNumber).append(PAIR_SEPARATOR);
        }

        output.append(recordFieldSeparator);
    }
}
