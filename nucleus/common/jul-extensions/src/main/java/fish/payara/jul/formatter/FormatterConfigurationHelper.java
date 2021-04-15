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

package fish.payara.jul.formatter;

import fish.payara.jul.cfg.ConfigurationHelper;

import java.util.logging.Formatter;

/**
 * This is a tool to help with parsing the logging.properties file to configure formatters.
 * <p>
 * It respects JUL configuration standards, so ie. each formatter knows best how to configure itself,
 * but still can use this helper to parse properties directly to objects instead of plain strings.
 * Helper also supports custom error handlers.
 *
 * @author David Matejcek
 */
public class FormatterConfigurationHelper extends ConfigurationHelper {

    public static FormatterConfigurationHelper forHandlerId(final HandlerId handlerId) {
        return new FormatterConfigurationHelper(handlerId.getPropertyPrefix() + ".formatter",
            ERROR_HANDLER_PRINT_TO_STDERR);
    }


    public static FormatterConfigurationHelper forFormatterClass(final Class<? extends Formatter> formatterClass) {
        return new FormatterConfigurationHelper(formatterClass.getName(), ERROR_HANDLER_PRINT_TO_STDERR);
    }


    public FormatterConfigurationHelper(final Class<? extends Formatter> clazz) {
        this(clazz.getName(), ERROR_HANDLER_PRINT_TO_STDERR);
    }


    /**
     * @param prefix Usually a canonical class name
     * @param errorHandler
     */
    public FormatterConfigurationHelper(final String prefix, final LoggingPropertyErrorHandler errorHandler) {
        super(prefix, errorHandler);
    }


    public AnsiColor getAnsiColor(final String key, final AnsiColor defaultColor) {
        return parse(key, defaultColor, AnsiColor::valueOf);
    }
}
