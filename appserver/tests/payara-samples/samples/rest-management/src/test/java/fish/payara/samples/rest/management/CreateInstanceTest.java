/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;
import static fish.payara.samples.rest.management.RestManagementTest.INSTANCE_NAME;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author lprimak
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class CreateInstanceTest {
    @Deployment
    public static WebArchive deploy() {
        return PayaraTestShrinkWrap.getWebArchive();
    }

    @Test
    public void createInstance() {
        List<String> output = new ArrayList<>();
        CliCommands.payaraGlassFish(Arrays.asList("list-instances"), output);
        List<String> instances = output.stream().map(this::firstWord).collect(Collectors.toList());
        if (!instances.contains(INSTANCE_NAME)) {
            output.clear();
            CliCommands.payaraGlassFish(Arrays.asList("list-nodes"), output);
            String node = output.stream().map(this::firstWord).findFirst().get();
            CliCommands.payaraGlassFish("create-instance", "--node", node, INSTANCE_NAME, "--terse");
        }
    }

    String firstWord(String line) {
        if (line == null) {
            return line;
        }
        String[] words = line.split(" ");
        if (words.length > 0) {
            return words[0];
        } else {
            return line;
        }
    }
}
