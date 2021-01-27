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

package fish.payara.logging.jul.internal;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Useful to trace logging, what it does, how it is configured, what initialized it.
 * Useful for server developers.
 *
 * @author David Matejcek
 */
public final class PayaraLoggingTracer {

    private static final PrintStream OUT = System.out;
    private static final PrintStream ERR = System.err;
    private static volatile boolean tracingEnabled = Boolean.getBoolean("fish.payara.logging.jul.tracingEnabled");

    private PayaraLoggingTracer() {
        // hidden constructor
    }


    /**
     * Call this method to enable/disable tracing of the logging system. The effect is immediate.
     *
     * @param tracingEnabled
     */
    public static void setTracing(final boolean tracingEnabled) {
        PayaraLoggingTracer.tracingEnabled = tracingEnabled;
    }


    /**
     * @return true if the tracing of the logging system is enabled.
     */
    public static boolean isTracingEnabled() {
        return tracingEnabled;
    }


    /**
     * Logs the message to STDOUT if the tracing is enabled.
     *
     * @param source
     * @param message
     */
    public static synchronized void trace(final Class<?> source, final Supplier<String> message) {
        if (tracingEnabled) {
            trace(source, message.get());
        }
    }


    /**
     * Logs the message to STDOUT if the tracing is enabled.
     *
     * @param source
     * @param message
     */
    public static synchronized void trace(final Class<?> source, final String message) {
        if (tracingEnabled) {
            OUT.println(source.getCanonicalName() + ": " + message);
            OUT.flush();
        }
    }


    /**
     * Logs a "DON'T PANIC" message and generated RuntimeException with the message parameter
     * to STDOUT if the tracing is enabled.
     *
     * @param source
     * @param message
     */
    public static synchronized void stacktrace(final Class<?> source, final String message) {
        if (tracingEnabled) {
            OUT.println(
                source.getCanonicalName() + ": Don't panic, following stacktrace is only to see what invoked this!");
            new RuntimeException(message).printStackTrace(OUT);
            OUT.flush();
        }
    }


    /**
     * Logs the message to STDERR.
     *
     * @param source
     * @param message
     */
    public static synchronized void error(final Class<?> source, final String message) {
        ERR.println(source.getCanonicalName() + ": " + message);
        ERR.flush();
    }


    /**
     * Logs the message and the exception's stacktrace to STDERR.
     *
     * @param source
     * @param message
     * @param e
     */
    public static synchronized void error(final Class<?> source, final String message, final Throwable t) {
        ERR.println(source.getCanonicalName() + ": " + message);
        t.printStackTrace(ERR);
        ERR.flush();
    }
}
