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

package fish.payara.logging.jul.cfg;

import fish.payara.logging.jul.formatter.FormatterConfigurationHelper;

import java.util.function.Function;
import java.util.logging.Formatter;

/**
 * This is a tool to help with parsing the logging.properties file to configure handlers.
 * <p>
 * It respects JUL configuration standards, so ie. each formatter knows best how to configure itself,
 * but still can use this helper to parse properties directly to objects instead of plain strings.
 * Helper also supports custom error handlers.
 *
 * @author David Matejcek
 */
public class LoggingConfigurationHelper extends FormatterConfigurationHelper {

    private static final Function<String, Formatter> STR_TO_FORMATTER = STR_TO_CLASS.andThen(Formatter.class::cast);


    public LoggingConfigurationHelper() {
        super();
    }


    public LoggingConfigurationHelper(final Class<?> clazz) {
        super(clazz);
    }


    public Formatter getFormatter(final String key, final String defaultFormatterClass) {
        return parseOrSupply(key, () -> STR_TO_FORMATTER.apply(defaultFormatterClass), STR_TO_FORMATTER);
    }
}
