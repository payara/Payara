/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.logging.jul.env;

import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.io.PrintStream;

/**
 * This class holds informations detected on the logging system startup, so whatever you will
 * change, this class should always have access to original values, interesting for the logging
 * system:
 * <ul>
 * <li>STDOUT
 * <li>STDERR
 * </ul>
 * It holds also other informations about the environment, required for logs.
 *
 * @author David Matejcek
 */
public final class LoggingSystemEnvironment {

    private static final PrintStream ORIGINAL_STD_ERR = System.err;
    private static final PrintStream ORIGINAL_STD_OUT = System.out;

    // These values are global for this JVM
    // Why they are not volatile? Because it affects JIT optimizations
    // and they are not so critical.
    private static String productId;
    private static boolean releaseParametersEarly;

    private LoggingSystemEnvironment() {
        // hidden
    }


    /**
     * Call this method before you do any changes in global JVM objects like {@link System#out}!
     */
    public static synchronized void initialize() {
        // this is a rather psychological trick to force developer to touch this class.
    }


    /**
     * @return the STDOUT {@link PrintStream} used at startup.
     */
    public static PrintStream getOriginalStdErr() {
        return ORIGINAL_STD_ERR;
    }


    /**
     * @return the STDOUT {@link PrintStream} used at startup.
     */
    public static PrintStream getOriginalStdOut() {
        return ORIGINAL_STD_OUT;
    }


    /**
     * Sets original values of the STDOUT and STDERR print streams back.
     */
    public static void resetStandardOutputs() {
        logSetter("Output streams reset to JVM defaults.");
        System.setOut(ORIGINAL_STD_OUT);
        System.setErr(ORIGINAL_STD_ERR);
    }

    /**
     * @return the name of the product. Can be null if not explicitly set.
     */
    public static String getProductId() {
        return productId;
    }


    /**
     * @param productId the name of the product. It is null by default.
     */
    public static void setProductId(final String productId) {
        logSetter("productId: " + productId);
        LoggingSystemEnvironment.productId = productId;
    }


    /**
     * @return if true, parameters are forgotten after they were used in the message.
     */
    public static boolean isReleaseParametersEarly() {
        return releaseParametersEarly;
    }


    /**
     * Note: This method is used internally.
     *
     * @param releaseParametersEarly if true, parameters are forgotten after they were used in the message.
     */
    public static void setReleaseParametersEarly(boolean releaseParametersEarly) {
        logSetter("releaseParametersEarly: " + releaseParametersEarly);
        LoggingSystemEnvironment.releaseParametersEarly = releaseParametersEarly;
    }


    /**
     * Reason for this method? Like {@link System} class setters, it's quite important to have a clue
     * which part of the JVM changed the setting.
     */
    private static void logSetter(final String message) {
        PayaraLoggingTracer.stacktrace(LoggingSystemEnvironment.class, message);
    }
}
