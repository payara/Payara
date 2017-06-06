/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package fish.payara.arquillian.container.payara.embedded.app;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBossEmbeddedIntegrationTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class IntegrationEarTestCase {
    
    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {
        return create(EnterpriseArchive.class)
            .addAsModule(
                create(JavaArchive.class)
                    .addClasses(
                        NoInterfaceEJB.class,
                        NameProvider.class)
                    .addAsManifestResource(INSTANCE, "beans.xml"))
            .addAsModule(
                create(WebArchive.class)
                    .addClass(IntegrationEarTestCase.class)
                    .addAsWebInfResource(INSTANCE, "beans.xml"));
    }

    @EJB
    private NoInterfaceEJB bean;

    @Test
    public void shouldBeAbleToInjectEJBAsInstanceVariable() throws Exception {
        assertNotNull("Verify that the Bean has been injected", bean);

        assertEquals("Arquillian", bean.getName());
    }
}
