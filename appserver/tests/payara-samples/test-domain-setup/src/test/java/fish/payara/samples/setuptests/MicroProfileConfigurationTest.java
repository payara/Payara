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

package fish.payara.samples.setuptests;

import fish.payara.samples.CliCommands;
import fish.payara.samples.ServerOperations;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.logging.Logger;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author lprimak
 */
@RunWith(Arquillian.class)
public class MicroProfileConfigurationTest {
    private static final Logger logger = Logger.getLogger(MicroProfileConfigurationTest.class.getName());

    @Test
    public void setupMicroprofile() {
        if (ServerOperations.isServer()) {
            if (CliCommands.payaraGlassFish("create-system-properties", "fish.payara.test.create-insecure-endpoint=true") != 0) {
                throw new IllegalStateException("Can't create insecure endpoints property");
            }
            List<String> output = new ArrayList<>();
            CliCommands.payaraGlassFish(asList("get-metrics-configuration"), output);
            if (output.size() != 3) {
                throw new IllegalStateException("Can't get current micproprofile state");
            }
            String header = output.get(0);
            int securityIndex = header.indexOf("Security");
            String securityString = output.get(1).substring(securityIndex).replaceFirst("[ \t\n].*", "");
            boolean security = Boolean.parseBoolean(securityString);
            if (security) {
                logger.info("Microprofile metrics already secure");
                return;
            }

            boolean success = true;
            success &= CliCommands.payaraGlassFish(asList("set-microprofile-healthcheck-configuration", "--securityenabled", "true",
                    "--endpoint", "mphealth")) == 0;
            success &= CliCommands.payaraGlassFish(asList("set-openapi-configuration", "--securityenabled", "true")) == 0;
            success &= CliCommands.payaraGlassFish(asList("set-metrics-configuration", "--securityenabled", "true",
                    "--endpoint", "mpmetrics")) == 0;

            if (!success) {
                throw new IllegalStateException("Can't configure microprofile");
            }
        }
    }
}
