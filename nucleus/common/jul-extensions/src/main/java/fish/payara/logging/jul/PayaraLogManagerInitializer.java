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

import java.io.IOException;
import java.util.Properties;

import static fish.payara.logging.jul.PayaraLoggingConstants.CLASS_LOG_MANAGER;
import static fish.payara.logging.jul.PayaraLoggingConstants.JVM_OPT_LOGGING_MANAGER;

/**
 * @author David Matejcek
 */
public class PayaraLogManagerInitializer {

    public static synchronized boolean tryToSetAsDefault() {
        return tryToSetAsDefault(null);
    }


    public static synchronized boolean tryToSetAsDefault(final Properties configuration) {
        PayaraLoggingTracer.stacktrace(PayaraLogManagerInitializer.class, "tryToSetAsDefault(" + configuration + ")");
        if (System.getProperty(JVM_OPT_LOGGING_MANAGER) != null) {
            PayaraLoggingTracer.trace(PayaraLogManagerInitializer.class, "The Log Manager implementation is already configured.");
            return false;
        }
        // will not work if anyone already called LogManager.getLogManager in the same context!
        final Thread currentThread = Thread.currentThread();
        final ClassLoader old = currentThread.getContextClassLoader();
        try {

            // context classloader is used to load the class if not found by system cl.
            final ClassLoader newClassLoader = PayaraLogManagerInitializer.class.getClassLoader();
            currentThread.setContextClassLoader(newClassLoader);

            // avoid any direct references to prevent static initializer of LogManager class
            // until everything is set.
            System.setProperty(JVM_OPT_LOGGING_MANAGER, CLASS_LOG_MANAGER);
            final Class<?> logManagerClass = newClassLoader.loadClass(CLASS_LOG_MANAGER);
            PayaraLoggingTracer.trace(PayaraLogManagerInitializer.class,
                () -> "Will initialize log manager " + logManagerClass);

            return PayaraLogManager.initialize(configuration);
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Could not initialize logging system.", e);
        } finally {
            currentThread.setContextClassLoader(old);
        }
    }
}
