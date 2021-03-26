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

package fish.payara.acme;

import fish.payara.logging.jul.PayaraLogManagerInitializer;

import java.util.logging.LogManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static fish.payara.logging.jul.cfg.PayaraLoggingConstants.CLASS_LOG_MANAGER_JUL;
import static fish.payara.logging.jul.cfg.PayaraLoggingConstants.CLASS_LOG_MANAGER_PAYARA;
import static fish.payara.logging.jul.cfg.PayaraLoggingConstants.JVM_OPT_LOGGING_MANAGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * This test is extremely trivial. But it is not so simple ...
 * <p>
 * The problem is that the LogManager is set forever when anything in JVM tries to use the logging
 * system for the first time. And that is ServiceLoaderTestEngineRegistry of JUnit5 at this moment.
 * <p>
 * So we have three possible test methods, at least one should pass.
 *
 * @author David Matejcek
 */
public class PayaraLogManagerInitializerTest {

    @Test
    @DisabledIfSystemProperty(named = JVM_OPT_LOGGING_MANAGER, matches = ".+")
    void implicitJUL() {
        assertFalse(PayaraLogManagerInitializer.tryToSetAsDefault());
        assertEquals(CLASS_LOG_MANAGER_JUL, LogManager.getLogManager().getClass().getCanonicalName());
    }


    @Test
    @EnabledIfSystemProperty(named = JVM_OPT_LOGGING_MANAGER, matches = CLASS_LOG_MANAGER_JUL)
    void explicitJUL() {
        assertFalse(PayaraLogManagerInitializer.tryToSetAsDefault());
        assertEquals(CLASS_LOG_MANAGER_JUL, LogManager.getLogManager().getClass().getCanonicalName());
    }


    @Test
    @EnabledIfSystemProperty(named = JVM_OPT_LOGGING_MANAGER, matches = CLASS_LOG_MANAGER_PAYARA)
    void payara() {
        assertFalse(PayaraLogManagerInitializer.tryToSetAsDefault());
        assertEquals(CLASS_LOG_MANAGER_PAYARA, LogManager.getLogManager().getClass().getCanonicalName());
    }
}
