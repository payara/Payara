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

package fish.payara.acme.handler;

import fish.payara.logging.jul.PayaraLogManager;
import fish.payara.logging.jul.cfg.PayaraLogManagerConfiguration;
import fish.payara.logging.jul.cfg.SortedProperties;
import fish.payara.logging.jul.handler.SyslogHandler;
import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.util.logging.Level;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * @author David Matejcek
 */
public class SysLogHandlerTest {

    private static PayaraLogManagerConfiguration originalConfig;
    private SyslogHandler handler;

    @BeforeAll
    public static void backup() {
        originalConfig = PayaraLogManager.getLogManager().getConfiguration();
        System.out.println("Original configuration: " + originalConfig);
    }


    @AfterEach
    public void closeHandler() {
        if (handler != null) {
            handler.close();
        }
        PayaraLogManager.getLogManager().reconfigure(originalConfig);
    }


    /**
     * Ths test is dumb, it just uses the handler and expects it will not throw an exception.
     */
    @Test
    void standardUsage() throws Exception {
        final SortedProperties properties = new SortedProperties();
        properties.setProperty(SyslogHandler.class.getName() + ".useSystemLogging", "true");
        final PayaraLogManagerConfiguration cfg = new PayaraLogManagerConfiguration(properties);
        PayaraLogManager.getLogManager().reconfigure(cfg);
        handler = new SyslogHandler();
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.SEVERE, "This should log.");
        handler.publish(record);
        handler.publish(null);
        Thread.yield();
        handler.close();
    }
}
