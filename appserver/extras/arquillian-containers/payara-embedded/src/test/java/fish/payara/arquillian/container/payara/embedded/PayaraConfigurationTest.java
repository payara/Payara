/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.arquillian.container.payara.embedded;

import org.junit.Test;

import fish.payara.arquillian.container.payara.embedded.PayaraConfiguration;

import static org.junit.Assert.assertTrue;

public class PayaraConfigurationTest {

    @Test
    public void testValidInstallRoot() throws Exception {
        String installRoot = "./src/test/resources/gfconfigs/installRoot";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstallRoot(installRoot);
        config.validate();
    }

    @Test(expected = RuntimeException.class)
    public void testInstallRootWithoutDirectories() throws Exception {
        String installRoot = "./src/test/resources/gfconfigs/";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstallRoot(installRoot);
        config.validate();
    }

    @Test
    public void testValidInstanceRoot() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/instanceRoot";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.validate();
    }

    @Test(expected = RuntimeException.class)
    public void testInstanceRootWithoutDirectories() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/emptydir";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.validate();
    }

    @Test
    public void testInstanceRootWithoutConfigXml() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/instanceRootNoConfigxml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.validate();
    }

    @Test
    public void testValidConfigXmlPath() throws Exception {
        String configXml = "./src/test/resources/gfconfigs/configxml/test-domain.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setConfigurationXml(configXml);
        config.validate();

        assertTrue(config.getConfigurationXml().startsWith("file:"));
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidConfigXmlPath() throws Exception {
        String configXml = "./src/test/resources/gfconfigs/emptydir/test-domain.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setConfigurationXml(configXml);
        config.validate();
    }

    @Test
    public void testInstanceRootAndConfigXml() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/instanceRoot";
        String configXml = "./src/test/resources/gfconfigs/configxml/test-domain.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.setConfigurationXml(configXml);
        config.validate();

        assertTrue(config.getConfigurationXml().startsWith("file:"));
    }

    @Test
    public void testValidResourcesXmlPath() throws Exception {
        String resourcesXml = "./src/test/resources/gfconfigs/resourcesxml/glassfish-resources.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setResourcesXml(resourcesXml);
        config.validate();
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidResourcesXmlPath() throws Exception {
        String resourcesXml = "./src/test/resources/gfconfigs/emptydir/glassfish-resources.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setResourcesXml(resourcesXml);
        config.validate();
    }
}
