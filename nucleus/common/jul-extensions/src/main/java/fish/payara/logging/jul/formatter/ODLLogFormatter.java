/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2017-2020] [Payara Foundation and/or affiliates]

package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.formatter.ExcludeFieldsSupport.SupplementalAttribute;
import fish.payara.logging.jul.i18n.MessageResolver;
import fish.payara.logging.jul.record.EnhancedLogRecord;
import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static java.lang.System.lineSeparator;

/**
 * ODLLogFormatter conforms to the logging format defined by the
 * Log Working Group in Oracle.
 * The specified format is
 * <pre>[[timestamp] [organization ID] [Message Type/Level] [Message ID] [Logger Name] [Thread ID] [User ID] [ECID] [Extra Attributes] [Message]]\n</pre>
 *
 * @author Naman Mehta
 * @author David Matejcek - refactoring
 */
public class ODLLogFormatter extends AnsiColorFormatter {

    private static final int REC_BUFFER_CAPACITY = 512;

    private static final String FIELD_BEGIN_MARKER = "[";
    private static final String FIELD_END_MARKER = "]";
    private static final String FIELD_SEPARATOR = " ";
    private static final String INDENT = "  ";

    private static final String LABEL_CLASSNAME = "CLASSNAME";
    private static final String LABEL_METHODNAME = "METHODNAME";
    private static final String LABEL_RECORDNUMBER = "RECORDNUMBER";

    private static final MessageResolver MSG_RESOLVER = new MessageResolver();

    private final ExcludeFieldsSupport excludeFieldsSupport;
    private final String recordFieldSeparator;
    private boolean multiLineMode;

    public ODLLogFormatter() {
        this.multiLineMode = true;
        this.excludeFieldsSupport = new ExcludeFieldsSupport();
        this.recordFieldSeparator = FIELD_SEPARATOR;
    }

    @Override
    public String formatRecord(final LogRecord record) {
        return formatEnhancedLogRecord(MSG_RESOLVER.resolve(record));
    }

    /**
     * @param excludeFields comma separated field names which should not be in the ouptut
     */
    public void setExcludeFields(final String excludeFields) {
        this.excludeFieldsSupport.setExcludeFields(excludeFields);
    }

    /**
     * @param multiLineMode true if the log message is on the next line. Default: true.
     */
    public void setMultiLineMode(final boolean multiLineMode) {
        this.multiLineMode = multiLineMode;
    }


    private String formatEnhancedLogRecord(final EnhancedLogRecord record) {
        try {
            final String message = getPrintedMessage(record);
            if (message == null) {
                return "";
            }
            final boolean multiLine = multiLineMode || message.contains(lineSeparator());
            final String timestamp = getDateTimeFormatter().format(record.getTime());
            final Level logLevel = record.getLevel();
            final String msgId = record.getMessageKey();
            final String loggerName = record.getLoggerName();
            final String threadName = record.getThreadName();
            final StringBuilder output = new StringBuilder(REC_BUFFER_CAPACITY);
            appendTimestamp(output, timestamp);
            appendProductId(output);
            appendLogLevel(output, logLevel);
            appendMessageKey(output, msgId);
            appendLoggerName(output, loggerName);
            appendThread(output, record.getThreadID(), threadName);
            appendMillis(output, record.getMillis());
            appendLogLevelAsInt(output, logLevel);
            appendSequenceNumber(output, record.getSequenceNumber());
            appendSource(output, record.getSourceClassName(), record.getSourceMethodName());

            if (multiLine) {
                output.append(FIELD_BEGIN_MARKER).append(FIELD_BEGIN_MARKER);
                output.append(lineSeparator());
                output.append(INDENT);
            }
            output.append(message);
            if (multiLine) {
                output.append(FIELD_END_MARKER).append(FIELD_END_MARKER);
            }
            output.append(lineSeparator()).append(lineSeparator());
            return output.toString();
        } catch (final Exception e) {
            PayaraLoggingTracer.error(getClass(), "Error in formatting Logrecord", e);
            return record.getMessage();
        }
    }

    private void appendTimestamp(final StringBuilder output, final String timestamp) {
        output.append(FIELD_BEGIN_MARKER);
        output.append(timestamp);
        output.append(FIELD_END_MARKER).append(recordFieldSeparator);
    }

    private void appendProductId(final StringBuilder output) {
        output.append(FIELD_BEGIN_MARKER);
        if (getProductId() != null) {
            output.append(getProductId());
        }
        output.append(FIELD_END_MARKER).append(recordFieldSeparator);
    }

    private void appendLogLevel(final StringBuilder output, final Level logLevel) {
        output.append(FIELD_BEGIN_MARKER);
        final AnsiColor levelColor = getLevelColor(logLevel);
        if (levelColor != null) {
            output.append(levelColor);
        }
        output.append(logLevel.getName());
        if (levelColor != null) {
            output.append(AnsiColor.RESET);
        }
        output.append(FIELD_END_MARKER).append(recordFieldSeparator);
    }

    private void appendMessageKey(final StringBuilder output, final String msgId) {
        output.append(FIELD_BEGIN_MARKER);
        if (msgId != null) {
            output.append(msgId);
        }
        output.append(FIELD_END_MARKER).append(recordFieldSeparator);
    }

    private void appendLoggerName(final StringBuilder output, final String loggerName) {
        output.append(FIELD_BEGIN_MARKER);
        if (loggerName != null) {
            if (isAnsiColor()) {
                output.append(getLoggerColor());
            }
            output.append(loggerName);
            if (isAnsiColor()) {
                output.append(AnsiColor.RESET);
            }
        }
        output.append(FIELD_END_MARKER).append(recordFieldSeparator);
    }

    private void appendThread(final StringBuilder output, final int threadId, final String threadName) {
        if (!excludeFieldsSupport.isSet(SupplementalAttribute.TID)) {
            output.append(FIELD_BEGIN_MARKER);
            output.append("tid: ").append("_ThreadID=").append(threadId).append(" _ThreadName=").append(threadName);
            output.append(FIELD_END_MARKER).append(recordFieldSeparator);
        }
    }

    private void appendMillis(final StringBuilder output, final long millis) {
        if (!excludeFieldsSupport.isSet(SupplementalAttribute.TIME_MILLIS)) {
            output.append(FIELD_BEGIN_MARKER);
            output.append("timeMillis: ").append(millis);
            output.append(FIELD_END_MARKER).append(recordFieldSeparator);
        }
    }

    private void appendLogLevelAsInt(final StringBuilder output, final Level logLevel) {
        if (!excludeFieldsSupport.isSet(SupplementalAttribute.LEVEL_VALUE)) {
            output.append(FIELD_BEGIN_MARKER);
            output.append("levelValue: ").append(logLevel.intValue());
            output.append(FIELD_END_MARKER).append(recordFieldSeparator);
        }
    }

    private void appendSequenceNumber(final StringBuilder output, final long sequenceNumber) {
        if (isPrintRecordNumber()) {
            output.append(FIELD_BEGIN_MARKER);
            output.append(LABEL_RECORDNUMBER).append(": ").append(sequenceNumber);
            output.append(FIELD_END_MARKER).append(recordFieldSeparator);
        }
    }

    private void appendSource(final StringBuilder output, final String className, final String methodName) {
        if (!isPrintSource()) {
            return;
        }
        if (className != null) {
            output.append(FIELD_BEGIN_MARKER);
            output.append(LABEL_CLASSNAME).append(": ").append(className);
            output.append(FIELD_END_MARKER).append(recordFieldSeparator);
        }
        if (methodName != null) {
            output.append(FIELD_BEGIN_MARKER);
            output.append(LABEL_METHODNAME).append(": ").append(methodName);
            output.append(FIELD_END_MARKER).append(recordFieldSeparator);
        }
    }
}
