/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;


/**
 * A special {@link Formatter} able to notify some delegate about the {@link LogRecord} which passed
 * through this instance.
 *
 * @author David Matejcek
 */
public abstract class PayaraLogFormatter extends Formatter {

    // This was required, because we need 3 decimal numbers of the second fraction
    // DateTimeFormatter.ISO_LOCAL_DATE_TIME prints just nonzero values
    /** Example: 15:35:40.123 */
    protected static final DateTimeFormatter ISO_LOCAL_TIME = new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2).optionalStart()
        .appendFraction(NANO_OF_SECOND, 3, 3, true)
        .toFormatter(Locale.ROOT);

    /** Example: 2011-12-03T15:35:40.123 */
    protected static final DateTimeFormatter ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .append(ISO_LOCAL_TIME)
        .toFormatter(Locale.ROOT);

    /** Example: 2011-12-03T15:35:40.123+01:00 */
    protected static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(ISO_LOCAL_DATE_TIME)
        .appendOffsetId()
        .toFormatter(Locale.ROOT);

    private boolean printSequenceNumber;
    private boolean printSource;
    private DateTimeFormatter timestampFormatter = DEFAULT_DATETIME_FORMATTER;

    /**
     * Creates an instance and initializes defaults from log manager's configuration
     *
     * @param timestampFormatter
     * @param printSource
     */
    public PayaraLogFormatter(final boolean printSource, final DateTimeFormatter timestampFormatter) {
        this.printSource = printSource;
        this.timestampFormatter = timestampFormatter;
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
    }


    /**
     * Creates an instance and initializes defaults from log manager's configuration
     *
     * @param handlerId
     * @param timestampFormatter
     * @param printSource
     */
    public PayaraLogFormatter(final HandlerId handlerId, final boolean printSource,
        final DateTimeFormatter timestampFormatter) {
        this.printSource = printSource;
        this.timestampFormatter = timestampFormatter;
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
        configure(this, FormatterConfigurationHelper.forHandlerId(handlerId));
    }


    /**
     * Creates an instance and initializes defaults from log manager's configuration
     *
     * @param handlerId
     */
    public PayaraLogFormatter(final HandlerId handlerId) {
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
        configure(this, FormatterConfigurationHelper.forHandlerId(handlerId));
    }


    /**
     * Creates an instance and initializes defaults from log manager's configuration
     */
    public PayaraLogFormatter() {
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
    }


    private static void configure(final PayaraLogFormatter formatter, final FormatterConfigurationHelper helper) {
        formatter.printSequenceNumber = helper.getBoolean("printSequenceNumber", formatter.printSequenceNumber);
        formatter.printSource = helper.getBoolean("printSource", formatter.printSource);
        formatter.timestampFormatter = helper.getDateTimeFormatter("timestampFormat", formatter.timestampFormatter);
    }


    /**
     * Formats the record.
     *
     * @param record
     * @return formatted record, final record for output
     */
    protected abstract String formatRecord(LogRecord record);


    /**
     * @param printSequenceNumber true enables printing the log record sequence number
     */
    public void setPrintSequenceNumber(final boolean printSequenceNumber) {
        this.printSequenceNumber = printSequenceNumber;
    }


    /**
     * @return true enables printing the log record sequence number
     */
    public boolean isPrintSequenceNumber() {
        return printSequenceNumber;
    }


    /**
     * @param printSource if true, the source class and method will be printed to the output (but
     *            only if they are set)
     */
    public void setPrintSource(final boolean printSource) {
        this.printSource = printSource;
    }


    /**
     * @return if true, the source class and method will be printed to the output (but
     *         only if they are set)
     */
    public boolean isPrintSource() {
        return printSource;
    }


    /**
     * @return {@link DateTimeFormatter} used for timestamps
     */
    public final DateTimeFormatter getTimestampFormatter() {
        return timestampFormatter;
    }


    /**
     * @param timestampFormatter {@link DateTimeFormatter} used for timestamps. Null sets default.
     */
    public final void setTimestampFormatter(final DateTimeFormatter timestampFormatter) {
        this.timestampFormatter = timestampFormatter == null ? DEFAULT_DATETIME_FORMATTER : timestampFormatter;
    }


    /**
     * @param format The date format to set for records. Null sets default.
     *            See {@link DateTimeFormatter} for details.
     */
    public final void setTimestampFormatter(final String format) {
        setTimestampFormatter(format == null ? DEFAULT_DATETIME_FORMATTER : DateTimeFormatter.ofPattern(format));
    }


    @Override
    public String formatMessage(final LogRecord record) {
        throw new UnsupportedOperationException("String formatMessage(LogRecord record)");
    }


    @Override
    public final String format(final LogRecord record) {
        return formatRecord(record);
    }


    /**
     * @param record if null, this method returns null too
     * @return a record's message plus printed stacktrace if some throwable is present.
     */
    protected String getPrintedMessage(final EnhancedLogRecord record) {
        if (record == null) {
            return null;
        }
        final String message = record.getMessage();
        final String stackTrace = record.getThrownStackTrace();
        if (message == null || message.isEmpty()) {
            return stackTrace;
        }
        if (stackTrace == null) {
            return message;
        }
        return message + System.lineSeparator() + stackTrace;
    }
}
