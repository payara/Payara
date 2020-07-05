/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.samples.rest.management;

import static org.junit.Assert.assertFalse;

import java.util.Scanner;

import org.junit.Test;
import org.junit.Ignore;

/**
 * Tests to run against the server log
 */
@Ignore
public class ServerLogTest extends RestManagementTest {

    /**
     * A test to make sure that while the server is starting there are no WARNING or ERROR messages in the server log.
     */
    @Test
    public void when_domain_started_expect_no_logged_warnings() {
        String log = viewLog();

        try (Scanner scanner = new Scanner(log)) {
            boolean startupLine = false;
            boolean warningFound = false;
            boolean printLine = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Only count lines after the JVM invocation
                if (line.contains("JVM invocation")) {
                    startupLine = true;
                }
                if (startupLine) {
                    System.err.println(line);
                    // Flag a warning if found
                    if (line.contains("WARNING") || line.contains("ERROR")) {
                        warningFound = true;
                        printLine = true;
                    }
                    if (warningFound) {
                        // Print all the following lines until an empty line
                        if (printLine) {
                            System.err.println(line);
                        }
                        if (line.isEmpty() || line.matches("\\s*")) {
                            printLine = false;
                        }
                    }
                }
                // Don't search messages after startup
                if (line.contains("startup time")) {
                    startupLine = false;
                }
            }
            assertFalse("Warning found in the log", warningFound);
        }
    }

    /**
     * @return the contents of the server log
     */
    private String viewLog() {
        return target.path("view-log")
            .request()
            .get(String.class);
    }
}
