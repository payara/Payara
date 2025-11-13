/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.asadmin;

import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;
import fish.payara.samples.Unstable;

import org.glassfish.embeddable.CommandResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Verifies the correctness of the {@code SetHazelcastConfiguration} command.
 */
@Category(Unstable.class)
// Fails from two reasons:
// 1) Requires completely new domain. Side effects of other tests break this one.
// 2) On JDK8 fails because of usage of @Category annotation which has problems with
//    this bug: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8209742 (fixed in JDK11)
public class SetHazelcastConfigurationTest extends AsadminTest {

    private HazelcastRuntimeConfiguration config;

    @Before
    public void setUp() {
        config = getDomainExtensionByType(HazelcastRuntimeConfiguration.class);
    }


    @Test
    public void autoIncrementPort() {
        CommandResult result = asadmin("set-hazelcast-configuration", "--autoIncrementPort", "true");
        assertSuccess(result);
        assertTrue(config.getAutoIncrementPort());
        result = asadmin("set-hazelcast-configuration", "--autoIncrementPort", "false");
        assertSuccess(result);
        assertFalse(config.getAutoIncrementPort());
    }


    @Test
    public void dataGridEncryptionWarning() {
        CommandResult result = asadmin("set-hazelcast-configuration", "--encryptdatagrid", "true");
        assertWarning(result);
        assertContains("Could not find datagrid-key", result.getOutput());
        result = asadmin("set-hazelcast-configuration", "--encryptdatagrid", "false");
        assertSuccess(result);
    }
}
