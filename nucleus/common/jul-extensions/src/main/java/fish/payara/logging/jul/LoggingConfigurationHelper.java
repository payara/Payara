/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.logging.jul;

import fish.payara.logging.jul.internal.PayaraLoggingTracer;

import java.io.File;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * @author David Matejcek
 */
public class LoggingConfigurationHelper {

    public static final LoggingPropertyErrorHandler PRINT_TO_STDERR = (k, v, e) -> {
        PayaraLoggingTracer.error(LoggingConfigurationHelper.class, "Invalid value for the key: " + k + ": " + v, e);
    };

    private static final Function<String, Integer> STR_TO_POSITIVE_INT = v -> {
        final Integer value = Integer.valueOf(v);
        if (value >= 0) {
            return value;
        }
        throw new NumberFormatException("Value must be higher or equal to zero!");
    };


    private static final Function<String, ?> STR_TO_CLASS = v -> {
        try {
            final ClassLoader logManagerCL = LogManager.getLogManager().getClass().getClassLoader();
            if (logManagerCL == null) {
                return Thread.currentThread().getContextClassLoader().loadClass(v).newInstance();
            }
            return logManagerCL.loadClass(v).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException("Formatter instantiation failed!", e);
        }
    };


    private static final Function<String, Formatter> STR_TO_FORMATTER = STR_TO_CLASS.andThen(Formatter.class::cast);
    private static final Function<String, Handler> STR_TO_HANDLER = STR_TO_CLASS.andThen(Handler.class::cast);


    private static final Function<String, DateTimeFormatter> STR_TO_DF = v -> {
        final DateTimeFormatter df = DateTimeFormatter.ofPattern(v);
        // test that it is able to format this type.
        df.format(ZonedDateTime.now());
        return df;
    };


    private static final Function<String, List<String>> STR_TO_LIST = v -> {
        if (v == null || v.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(v.split("[\\s]*,[\\s]*"));
    };


    private final LogManager manager;
    private final String prefix;
    private final LoggingPropertyErrorHandler errorHandler;


    public LoggingConfigurationHelper(final LoggingPropertyErrorHandler errorHandler) {
        this((String) null, errorHandler);
    }


    public LoggingConfigurationHelper(final Class<?> clazz, final LoggingPropertyErrorHandler errorHandler) {
        this(clazz.getName(), errorHandler);
    }


    public LoggingConfigurationHelper(final String prefix, final LoggingPropertyErrorHandler errorHandler) {
        this.manager = LogManager.getLogManager();
        this.prefix = prefix == null ? "" : prefix + ".";
        this.errorHandler = errorHandler;
    }


    public String getString(final String key, final String defaultValue) {
        return parse(key, defaultValue, Function.identity());
    }


    public Integer getInteger(final String key, final Integer defaultValue) {
        return parse(key, defaultValue, Integer::valueOf);
    }

    public Integer getNonNegativeInteger(final String key, final Integer defaultValue) {
        return parse(key, defaultValue, STR_TO_POSITIVE_INT);
    }


    public Boolean getBoolean(final String key, final Boolean defaultValue) {
        return parse(key, defaultValue, Boolean::valueOf);
    }


    public Level getLevel(final String key, final Level defaultValue) {
        return parse(key, defaultValue, Level::parse);
    }


    public File getFile(final String key, final File defaultValue) {
        return parse(key, defaultValue, File::new);
    }


    public DateTimeFormatter getDateTimeFormatter(final String key, final DateTimeFormatter defaultValue) {
        return parse(key, defaultValue, STR_TO_DF);
    }


    public Formatter getFormatter(final String key, final String defaultFormatterClass) {
        return parseOrSupply(key, () -> STR_TO_FORMATTER.apply(defaultFormatterClass), STR_TO_FORMATTER);
    }

    public Handler getHandler(final String key, final String defaultFormatterClass) {
        return parseOrSupply(key, () -> STR_TO_HANDLER.apply(defaultFormatterClass), STR_TO_HANDLER);
    }

    public Charset getCharset(final String key, final Charset defaultValue) {
        return parse(key, defaultValue, Charset::forName);
    }


    public List<String> getList(final String key, final String defaultValue) {
        return parseOrSupply(key, () -> STR_TO_LIST.apply(defaultValue), STR_TO_LIST);
    }


    public <T> T getSomething(final String key, final T defaultValue, final Function<String, T> parser) {
        return parse(key, defaultValue, parser);
    }


    /**
     * Note: if you want to use untrimmed value, use the {@link LogManager#getProperty(String)}
     * directly.
     *
     * @param key
     * @return trimmed value for the key or null
     */
    private String getProperty(final String key) {
        final String value = manager.getProperty(key);
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ?  null : trimmed;
    }


    private <T> T parse(final String key, final T defaultValue, final Function<String, T> converter) {
        final Supplier<T> defaultValueSupplier = () -> defaultValue;
        return parseOrSupply(key, defaultValueSupplier, converter);
    }


    private <T> T parseOrSupply(final String key, final Supplier<T> defaultValueSupplier, final Function<String, T> converter) {
        final String realKey = prefix + key;
        final String property = getProperty(realKey);
        if (property == null) {
            return defaultValueSupplier.get();
        }
        try {
            return converter.apply(property);
        } catch (final Exception e) {
            if (errorHandler != null) {
                errorHandler.handle(realKey, property, e);
            }
            return defaultValueSupplier.get();
        }
    }


    @FunctionalInterface
    public interface LoggingPropertyErrorHandler {
        void handle(String key, Object value, Exception e);
    }
}
