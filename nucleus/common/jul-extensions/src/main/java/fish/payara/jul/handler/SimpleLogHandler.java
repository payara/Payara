/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jul.handler;

import fish.payara.jul.cfg.LogProperty;
import fish.payara.jul.env.LoggingSystemEnvironment;
import fish.payara.jul.formatter.OneLineFormatter;

import java.io.PrintStream;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;


/**
 * The simplest possible log handler.
 * <p>
 * Similar to {@link java.util.logging.ConsoleHandler} except
 * <ul>
 * <li>it uses STDOUT instead of STDERR by default, but you can use also different {@link PrintStream}.
 * <li>uses {@link OneLineFormatter} by default
 * </ul>
 *
 * @author David Matejcek
 */
public class SimpleLogHandler extends StreamHandler {

    /**
     * Configures the instance with properties prefixed by the name of this class.
     */
    public SimpleLogHandler() {
        final HandlerConfigurationHelper helper = HandlerConfigurationHelper.forHandlerClass(getClass());
        if (helper.getBoolean(SimpleLogHandlerProperty.USE_ERROR_STREAM, false)) {
            setOutputStream(new UncloseablePrintStream(LoggingSystemEnvironment.getOriginalStdErr()));
        } else {
            setOutputStream(new UncloseablePrintStream(LoggingSystemEnvironment.getOriginalStdOut()));
        }
        setFormatter(helper.getFormatter(OneLineFormatter.class));
    }


    /**
     * Configures the instance with properties prefixed by the name of this class
     * and sets the explicit {@link PrintStream}
     *
     * @param printStream
     */
    public SimpleLogHandler(final PrintStream printStream) {
        super(printStream, new OneLineFormatter());
    }


    /**
     * Publishes the record and calls {@link #flush()}
     */
    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    /**
     * Executes {@link #flush()}
     */
    @Override
    public void close() {
        flush();
    }


    /**
     * {@link Handler#close()} closes also stream it used for the output, but we don't want to close
     * STDOUT and STDERR
     */
    private static final class UncloseablePrintStream extends PrintStream {

        private UncloseablePrintStream(PrintStream out) {
            super(out, false);
        }

        @Override
        public void close() {
            // don't close
        }
    }


    /**
     * Configuration property set of this handler.
     */
    public enum SimpleLogHandlerProperty implements LogProperty {

        /** Use STDERR instead of STDOUT */
        USE_ERROR_STREAM("useErrorStream"),
        /** Class of the {@link Formatter} used with this handler */
        FORMATTER(HandlerConfigurationHelper.FORMATTER.getPropertyName()),
        ;

        private final String propertyName;

        SimpleLogHandlerProperty(final String propertyName) {
            this.propertyName = propertyName;
        }


        @Override
        public String getPropertyName() {
            return propertyName;
        }

        /**
         * @return full name using the {@link SimpleLogHandler} class.
         */
        public String getPropertyFullName() {
            return getPropertyFullName(SimpleLogHandler.class);
        }
    }
}
