/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * {@link BroadcastingFormatter} which is able to print colored logs.
 *
 * @since 4.1.1.173
 * @author Steve Millidge (Payara Foundation)
 * @author David Matejcek
 */
public abstract class AnsiColorFormatter extends BroadcastingFormatter {

    // This was required, because we need 3 decimal numbers of the second fraction
    // DateTimeFormatter.ISO_LOCAL_DATE_TIME prints just nonzero values
    private static final DateTimeFormatter iSO_LOCAL_TIME = new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2).optionalStart()
        .appendFraction(NANO_OF_SECOND, 3, 3, true)
        .toFormatter(Locale.ROOT);

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .append(iSO_LOCAL_TIME)
        .toFormatter(Locale.ROOT);

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(ISO_LOCAL_DATE_TIME)
        .appendOffsetId()
        .toFormatter(Locale.ROOT);

    private AnsiColor loggerColor;
    private HashMap<Level,AnsiColor> colors;
    private boolean ansiColor;
    private DateTimeFormatter dateTimeFormatter;

    /**
     * Creates the formatter, initialized from (starting from highest priority)
     * <ul>
     * <li>logging configuration.
     * <li>JVM options
     * </ul>
     */
    public AnsiColorFormatter() {
        final LogManager manager = LogManager.getLogManager();
        final String color = manager.getProperty(this.getClass().getCanonicalName() + ".ansiColor");
        if (Boolean.TRUE.toString().equalsIgnoreCase(color)) {
            ansiColor = true;
        }
        colors = new HashMap<>();
        colors.put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
        colors.put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
        colors.put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
        final String infoColor = manager.getProperty(this.getClass().getCanonicalName() + ".infoColor");
        if (infoColor != null) {
            try {
                colors.put(Level.INFO, AnsiColor.valueOf(infoColor));
            } catch (final IllegalArgumentException iae) {
                colors.put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
            }
        }
        String colorProp = manager.getProperty(this.getClass().getCanonicalName() + ".warnColor");
        if (colorProp != null) {
            try {
                colors.put(Level.WARNING, AnsiColor.valueOf(colorProp));
            } catch (final IllegalArgumentException iae) {
                colors.put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
            }
        }
        colorProp = manager.getProperty(this.getClass().getCanonicalName() + ".severeColor");
        if (colorProp != null) {
            try {
                colors.put(Level.SEVERE, AnsiColor.valueOf(colorProp));
            } catch (final IllegalArgumentException iae) {
                colors.put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
            }
        }

        loggerColor = getLoggerColor(manager);
        dateTimeFormatter = DEFAULT_DATETIME_FORMATTER;
    }


    private AnsiColor getLoggerColor(final LogManager manager) {
        final String key = this.getClass().getCanonicalName() + ".loggerColor";
        final String colorProp = manager.getProperty(key);
        if (colorProp != null) {
            try {
                return AnsiColor.valueOf(colorProp);
            } catch (final IllegalArgumentException e) {
                PayaraLoggingTracer.error(getClass(), "Invalid property: " + key + ": " + e);
            }
        }
        return AnsiColor.BOLD_INTENSE_BLUE;
    }

    /**
     * Enables/disables ANSI coloring in logs
     *
     * @param ansiColor true to enable
     */
    public void setAnsiColor(final boolean ansiColor) {
        this.ansiColor = ansiColor;
    }

    /**
     * @return true if ANSI coloring is enabled (default: true)
     */
    protected boolean isAnsiColor() {
        return ansiColor;
    }

    /**
     * @param loggerColor {@link AnsiColor} used for the logger name.
     */
    public void setLoggerColor(final AnsiColor loggerColor) {
        this.loggerColor = loggerColor;
    }

    /**
     * @return {@link AnsiColor} for the logger name value
     */
    public AnsiColor getLoggerColor() {
        return loggerColor;
    }

    /**
     * @param mapping colors used for log levels
     */
    public void setLevelColors(final Map<Level, AnsiColor> mapping) {
        this.colors = new HashMap<>(mapping);
    }

    /**
     * @param level
     * @return {@link AnsiColor} for the level value or null if {@link #isAnsiColor()} returns false.
     */
    protected AnsiColor getLevelColor(final Level level) {
        if (!isAnsiColor()) {
            return null;
        }
        return colors.get(level);
    }

    /**
     * @return {@link DateTimeFormatter} used for timestamps
     */
    public final DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    /**
     * @param dateTimeFormatter {@link DateTimeFormatter} used for timestamps
     */
    public final void setDateTimeFormatter(final DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter == null ? DEFAULT_DATETIME_FORMATTER : dateTimeFormatter;
    }

    /**
     * @param format The date format to set for records. See {@link DateTimeFormatter} for details.
     */
    public final void setDateTimeFormatter(final String format) {
        setDateTimeFormatter(format == null ? DEFAULT_DATETIME_FORMATTER : DateTimeFormatter.ofPattern(format));
    }
}
