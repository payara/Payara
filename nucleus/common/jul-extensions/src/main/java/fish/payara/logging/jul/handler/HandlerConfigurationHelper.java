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

package fish.payara.logging.jul.handler;

import fish.payara.logging.jul.cfg.ConfigurationHelper;
import fish.payara.logging.jul.formatter.HandlerId;
import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.lang.reflect.Constructor;
import java.util.function.Function;
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

    private static final Function<String, Formatter> STR_TO_FORMATTER = STR_TO_CLASS.andThen(Formatter.class::cast);

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
        return parseOrSupply("formatter", () -> createNewFormatter("formatter", defaultFormatterClass), STR_TO_FORMATTER);
    }


    private <F extends Formatter> F createNewFormatter(final String key, final Class<F> clazz) {
        try {
            final Constructor<F> constructor = getFormatterConstructor(clazz);
            if (constructor == null) {
                // All formatters must have default constructor
                return clazz.newInstance();
            }
            return constructor.newInstance(handlerId);
        } catch (ReflectiveOperationException | RuntimeException e) {
            handleError(e, key, clazz);
            return null;
        }
    }


    private <F extends Formatter> Constructor<F> getFormatterConstructor(final Class<F> formatterClass) {
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
