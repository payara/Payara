/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.jul.formatter;

import fish.payara.jul.record.EnhancedLogRecord;
import fish.payara.jul.record.MessageResolver;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.LogRecord;

/**
 * Fast formatter usable in tests or even in production if you need only simple logs with time,
 * level and messages.
 *
 * @author David Matejcek
 */
public class OneLineFormatter extends PayaraLogFormatter {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final MessageResolver MSG_RESOLVER = new MessageResolver();

    private int sizeOfLevel = 7;
    private int sizeOfThread = 20;
    private int sizeOfClass = 60;


    public OneLineFormatter(final HandlerId handlerId) {
        super(handlerId, true, TS_FORMAT);
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
        configure(this, FormatterConfigurationHelper.forHandlerId(handlerId));
    }


    /**
     * Creates an instance and initializes defaults from log manager's configuration
     */
    public OneLineFormatter() {
        super(true, TS_FORMAT);
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
    }


    private static void configure(OneLineFormatter formatter, final FormatterConfigurationHelper helper) {
        formatter.sizeOfLevel = helper.getNonNegativeInteger("size.level", formatter.sizeOfLevel);
        formatter.sizeOfThread = helper.getNonNegativeInteger("size.thread", formatter.sizeOfThread);
        formatter.sizeOfClass = helper.getNonNegativeInteger("size.class", formatter.sizeOfClass);
    }


    @Override
    public String formatRecord(final LogRecord record) {
        return formatEnhancedLogRecord(MSG_RESOLVER.resolve(record));
    }


    @Override
    public String formatMessage(final LogRecord record) {
        throw new UnsupportedOperationException("String formatMessage(LogRecord record)");
    }


    private String formatEnhancedLogRecord(final EnhancedLogRecord record) {
        if (record.getMessage() == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(256);
        sb.append(getTimestampFormatter().format(record.getTime()));
        addPadded(record.getLevel(), this.sizeOfLevel, sb);
        addPadded(record.getThreadName(), this.sizeOfThread, sb);
        if (isPrintSource()) {
            addPadded(record.getSourceClassName(), this.sizeOfClass, sb);
            sb.append('.');
            if (record.getSourceMethodName() != null) {
                sb.append(record.getSourceMethodName());
            }
        } else {
            addPadded(record.getLoggerName(), sizeOfClass, sb);
        }
        sb.append(' ').append(record.getMessage());

        if (record.getThrown() != null) {
            sb.append(LINE_SEPARATOR);
            sb.append(record.getThrownStackTrace());
        }

        return sb.append(LINE_SEPARATOR).toString();
    }


    private void addPadded(final Object value, final int size, final StringBuilder sb) {
        final String text = value == null ? "" : String.valueOf(value);
        sb.append(' ');
        sb.append(getPad(text, size));
        sb.append(text);
    }


    private char[] getPad(final String text, final int size) {
        final int countOfSpaces = size - text.length();
        if (countOfSpaces <= 0) {
            return new char[0];
        }
        final char[] spaces = new char[countOfSpaces];
        Arrays.fill(spaces, ' ');
        return spaces;
    }
}
