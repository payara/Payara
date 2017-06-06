/*
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
// Portions Copyright [2016-2017] [Payara Foundation]
package fish.payara.arquillian.container.payara.remote;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test to serve as a regression test for ARQ-658.
 * <p>
 * The deployment created by this class, does not have an application.xml file.
 * As a result, the context root is created by GlassFish,
 * instead of being specified by the developer.
 * Such context roots do not begin with a forward slash,
 * and the Arquillian REST client should recognize them.
 * <p>
 * The class is a converse test for the PayaraRestDeployEarTest class,
 * which adds an application.xml file with a user-specified context root.
 *
 * @author Vineet Reynolds
 */
@RunWith(Arquillian.class)
public class PayaraDeployWithoutAppXmlTest {
    
    @Inject
    private Client client;

    @Deployment
    public static EnterpriseArchive createTestArchive() {
        return create(EnterpriseArchive.class, "test.ear")
                .addAsLibrary(ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(Client.class, PayaraDeployWithoutAppXmlTest.class)
                .addAsManifestResource(INSTANCE, ArchivePaths.create("beans.xml")));
    }

    @Test
    public void testClient() {
        assertNotNull(client);
    }
}

class Client {

}
