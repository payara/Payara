/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.internal.EnhancedLogRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Fast formatter usable in tests or even in production if you need only simple logs with time,
 * level and messages.
 *
 * @author David Matejcek
 */
public class OneLineFormatter extends Formatter {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final int sizeOfLevel;
    private final int sizeOfThread;
    private final int sizeOfClass;


    /**
     * Configures the formatter.
     */
    public OneLineFormatter() {
        this.sizeOfLevel = getIntPropertyValue("sizeOfLevel", 7);
        this.sizeOfThread = getIntPropertyValue("sizeOfThread", 20);
        this.sizeOfClass = getIntPropertyValue("sizeOfClass", 60);
    }

    protected int getIntPropertyValue(final String localKey, final int defaultValue) {
        final String value = getPropertyValue(localKey);
        return value == null ? defaultValue : Integer.parseInt(localKey);
    }

    protected String getPropertyValue(final String localKey) {
        return LogManager.getLogManager().getProperty(getPropertyKey(localKey));
    }

    protected String getPropertyKey(final String localKey) {
        return getClass().getName() + '.' + localKey;
    }


    @Override
    public String format(final LogRecord record) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append(TS_FORMAT.format(LocalTime.now()));
        addPadded(record.getLevel(), this.sizeOfLevel, sb);
        addPadded(getThreadName(record), this.sizeOfThread, sb);
        addPadded(record.getSourceClassName(), this.sizeOfClass, sb);
        sb.append('.').append(record.getSourceMethodName());
        sb.append(' ').append(formatMessage(record));

        if (record.getThrown() != null) {
            sb.append(LINE_SEPARATOR);
            sb.append(getStackTrace(record.getThrown()));
        }

        return sb.append(LINE_SEPARATOR).toString();
    }


    /**
     * This method overrides the original {@link Formatter#formatMessage(LogRecord)} method - it is
     * null-safe and faster.
     */
    @Override
    public String formatMessage(final LogRecord record) {
        if (record == null) {
            return null;
        }
        final String template = getTemplateFromResourceBundle(record.getResourceBundle(), record.getMessage());
        final Object[] parameters = record.getParameters();
        if (template == null || parameters == null || parameters.length == 0) {
            return template;
        }
        try {
            return MessageFormat.format(template, parameters);
        } catch (final Exception ex) {
            return template;
        }
    }


    private StringBuilder addPadded(final Object value, final int size, final StringBuilder sb) {
        final String text = String.valueOf(value);
        sb.append(' ');
        sb.append(getPad(text, size));
        sb.append(text);
        return sb;
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


    /**
     * @param record
     * @return a thread name for logging purposes
     */
    protected String getThreadName(final LogRecord record) {
        if (record instanceof EnhancedLogRecord) {
            return ((EnhancedLogRecord) record).getThreadName();
        }
        return Thread.currentThread().getName();
    }


    /**
     * @param throwable
     * @return null or a stack trace printed to a string
     */
    protected String getStackTrace(final Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.getBuffer().toString();
        } catch (final IOException e) {
            return "ERR: "+ getClass().getSimpleName() + " failed to generate the stacktrace.";
        }
    }


    private String getTemplateFromResourceBundle(final ResourceBundle bundle, final String key) {
        if (bundle == null || key == null) {
            return key;
        }
        try {
            return bundle.getString(key);
        } catch (final MissingResourceException ex) {
            return key;
        }
    }
}
