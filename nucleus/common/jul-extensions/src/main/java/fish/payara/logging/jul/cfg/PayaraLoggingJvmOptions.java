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

/**
 * JVM options used to configure the Java Util Logging.
 * <p>
 * <b>Always remember - you cannot switch the LogManager used in the JVM once any runtime
 * touches the Logger class or the LogManager class.</b>
 * If you want to use PayaraLogManager, you need to set the system property
 * {@value #JVM_OPT_LOGGING_MANAGER} to {@value #CLASS_LOG_MANAGER_PAYARA} before it happens.
 *
 * @author David Matejcek
 */
// do not reference JUL classes from here, they can be initialized when you don't want it.
public final class PayaraLoggingJvmOptions {

    private PayaraLoggingJvmOptions() {
        // hidden constructor
    }

    /** Default JUL LogManager class name */
    public static final String CLASS_LOG_MANAGER_JUL = "java.util.logging.LogManager";
    /** Payara's JUL LogManager implementation class name */
    public static final String CLASS_LOG_MANAGER_PAYARA = "fish.payara.logging.jul.PayaraLogManager";

    /**
     * System property name defining LogManager implementation for the rest of the JVM runtime
     * existence.
     */
    public static final String JVM_OPT_LOGGING_MANAGER = "java.util.logging.manager";
    /**
     * System property name defining property file which will be automatically loaded on startup.
     * Usually it is named <code>logging.properties</code>
     */
    public static final String JVM_OPT_LOGGING_CFG_FILE = "java.util.logging.config.file";
    /**
     * System property telling the PayaraLogManager to use defaults if there would not be any
     * logging.properties neither set by {@value #JVM_OPT_LOGGING_CFG_FILE} nor available on classpath.
     * <p>
     * Defaults use the SimpleLogHandler and level INFO or level set by
     * {@value #JVM_OPT_LOGGING_CFG_DEFAULT_LEVEL}
     */
    public static final String JVM_OPT_LOGGING_CFG_USE_DEFAULTS = "java.util.logging.config.useDefaults";
    /**
     * If the PayaraLogManager would use defaults as configured by
     * the {@value #JVM_OPT_LOGGING_CFG_USE_DEFAULTS}, this system property tells him to use this
     * level and not the default INFO.
     */
    public static final String JVM_OPT_LOGGING_CFG_DEFAULT_LEVEL = "java.util.logging.config.defaultLevel";
}
