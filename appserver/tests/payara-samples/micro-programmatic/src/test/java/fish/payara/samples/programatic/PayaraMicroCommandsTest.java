/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.samples.programatic;

import fish.payara.micro.*;
import fish.payara.micro.boot.*;
import org.junit.*;

/**
 *
 * @author jonathan coustick
 */
public class PayaraMicroCommandsTest {
    @Test
    public void bootCommandtest() throws Exception {
        PayaraMicroBoot microBoot = PayaraMicroLauncher.getBootClass();
        microBoot.setNoHazelcast(true);
        microBoot.setPreBootHandler((t) -> {
            ClusterCommandResult result = t.run("set", "configs.config.server-config.health-check-service-configuration.enabled=true");
            Assert.assertEquals(ClusterCommandResult.ExitStatus.SUCCESS, result.getExitStatus());
            Assert.assertNull(result.getFailureCause());
            System.out.println(result.getOutput());
        });
        microBoot.setPostBootHandler((t) -> {
            ClusterCommandResult result = t.run("get-healthcheck-configuration");
            Assert.assertEquals(ClusterCommandResult.ExitStatus.SUCCESS, result.getExitStatus());
            Assert.assertNull(result.getFailureCause());
            Assert.assertTrue(result.getOutput().contains("Health Check Service Configuration is enabled?: true"));
        });
        System.out.println("Starting Payara Micro");
        microBoot.setHttpAutoBind(true);
        microBoot.bootStrap();

        try {
            microBoot.setPreBootHandler((t) -> {
                t.run("set-healthcheck-configuration", "--enabled", "false");
            });
            Assert.fail("Should not be able to add preboot comand postboot");
        } catch (IllegalStateException ex) {
            // Expected
        }
        try {
            microBoot.setPostBootHandler((t) -> {
                t.run("set-healthcheck-configuration", "--enabled", "false");
            });
            Assert.fail("Should not be able to add preboot comand postboot");
        } catch (IllegalStateException ex) {
            // Expected
        }
        System.out.println("Shutting down Payara Micro");
        microBoot.shutdown();

        microBoot.setPreBootHandler((t) -> {
                t.run("set-healthcheck-configuration", "--enabled", "false");
            });

            microBoot.setPostBootHandler((t) -> {
                t.run("set-healthcheck-configuration", "--enabled", "false");
            });

        System.out.println("Shutting down Payara Micro");
        microBoot.shutdown();
    }
}
