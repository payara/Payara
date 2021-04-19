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

package fish.payara.jul.handler;

import fish.payara.jul.cfg.ConfigurationHelper;
import fish.payara.jul.cfg.LogProperty;
import fish.payara.jul.formatter.HandlerId;
import fish.payara.jul.tracing.PayaraLoggingTracer;

import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;

/**
 * This is a tool to help with parsing the logging.properties file to configure handlers.
 * <p>
 * It respects JUL configuration standards, so ie. each formatter knows best how to configure itself,
 * but still can use this helper to parse properties directly to objects instead of plain strings.
 * Helper also supports custom error handlers.
 *
 * @author David Matejcek
 */
public class HandlerConfigurationHelper extends ConfigurationHelper {

    /**
     * Handler's property for formatter
     */
    public static final LogProperty FORMATTER = () -> "formatter";
    private final HandlerId handlerId;

    public static HandlerConfigurationHelper forHandlerClass(final Class<? extends Handler> handlerClass) {
        return new HandlerConfigurationHelper(HandlerId.forHandlerClass(handlerClass));
    }


    public HandlerConfigurationHelper(final HandlerId handlerId) {
        super(handlerId.getPropertyPrefix(), ERROR_HANDLER_PRINT_TO_STDERR);
        this.handlerId = handlerId;
    }


    /**
     * @param defaultFormatterClass
     * @return preconfigured {@link Formatter}, defaults are defined by the formatter and properties
     */
    public Formatter getFormatter(final Class<? extends Formatter> defaultFormatterClass) {
        final Supplier<Formatter> defaultSupplier = () -> createNewFormatter(defaultFormatterClass);
        final Function<String, Formatter> converter = value -> createNewFormatter(value);
        return parseOrSupply(FORMATTER, defaultSupplier, converter);
    }


    @SuppressWarnings("unchecked")
    private <F extends Formatter> F createNewFormatter(final String className) {
        final Class<Formatter> formatterClass = findClass(className);
        return (F) createNewFormatter(formatterClass);
    }


    @SuppressWarnings("unchecked")
    private  <F extends Formatter> Class<F> findClass(final String className) {
        if (className == null) {
            return null;
        }
        final ClassLoader classLoader = getClassLoader();
        try {
            return (Class<F>) classLoader.loadClass(className);
        } catch (ClassCastException | ClassNotFoundException | NoClassDefFoundError e) {
            PayaraLoggingTracer.error(ConfigurationHelper.class, "Classloader: " + classLoader, e);
            throw new IllegalStateException("Formatter instantiation failed! ClassLoader used: " + classLoader, e);
        }
    }


    private ClassLoader getClassLoader() {
        final ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
        if (threadCL != null) {
            return threadCL;
        }
        return getClass().getClassLoader();
    }


    private <F extends Formatter> F createNewFormatter(final Class<F> clazz) {
        try {
            final Constructor<F> constructor = getFormatterConstructorForHandler(clazz);
            if (constructor == null) {
                // All formatters must have default constructor
                return clazz.newInstance();
            }
            return constructor.newInstance(handlerId);
        } catch (ReflectiveOperationException | RuntimeException e) {
            handleError(e, FORMATTER.getPropertyFullName(handlerId.getPropertyPrefix()), clazz);
            return null;
        }
    }


    private <F extends Formatter> Constructor<F> getFormatterConstructorForHandler(final Class<F> formatterClass) {
        try {
            return formatterClass.getConstructor(HandlerId.class);
        } catch (NoSuchMethodException | SecurityException e) {
            PayaraLoggingTracer.trace(getClass(),
                "This formatter doesn't support configuration by handler's formatter properties subset: "
                    + formatterClass + ", so we will use formatter's default constructor");
            return null;
        }
    }
}
