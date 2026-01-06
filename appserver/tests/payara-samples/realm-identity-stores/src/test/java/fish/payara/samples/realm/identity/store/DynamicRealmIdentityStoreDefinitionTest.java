/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.realm.identity.store;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.realm.identity.store.common.AuthoritiesConstants;
import fish.payara.samples.realm.identity.store.common.Person;
import fish.payara.samples.realm.identity.store.common.PersonController;
import fish.payara.samples.realm.identity.store.common.PersonControllerClient;

import java.net.URL;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static fish.payara.samples.realm.identity.store.DynamicRealmIdentityStoreAppConfig.REALM_NAME;
import static fish.payara.samples.realm.identity.store.common.AuthoritiesConstants.ADMIN;
import static fish.payara.samples.realm.identity.store.common.AuthoritiesConstants.DEFAULT_PASSWORD;
import static fish.payara.samples.realm.identity.store.common.AuthoritiesConstants.DEFAULT_USER;
import static fish.payara.samples.realm.identity.store.common.PersonControllerClientHelper.getBasicPersonControllerClient;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.valid4j.matchers.http.HttpResponseMatchers.hasStatus;

/**
 * @author Gaurav Gupta
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class DynamicRealmIdentityStoreDefinitionTest {

    @ArquillianResource
    private URL deploymentUrl;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return create(WebArchive.class)
                .addClasses(
                        DynamicRealmIdentityStoreAppConfig.class,
                        Person.class,
                        PersonController.class,
                        AuthoritiesConstants.class
                )
                .addAsWebInfResource("all-beans.xml", "beans.xml");
    }

    @Before
    public void addTestUser() {
        ServerOperations.addUserToContainerIdentityStore(REALM_NAME, DEFAULT_USER, ADMIN);
    }

    @Test
    public void testWithCorrectCredentials() throws Exception {
        PersonControllerClient client = getBasicPersonControllerClient(deploymentUrl, DEFAULT_USER, DEFAULT_PASSWORD);
        Response response = client.createPerson(Person.DEFAULT_INSTANCE);
        assertThat(response, hasStatus(CREATED));
    }

    @Test
    public void testWithIncorrectCredentials() throws Exception {
        PersonControllerClient client = getBasicPersonControllerClient(deploymentUrl, DEFAULT_USER, "invalid_passwd");
        try {
            client.createPerson(Person.DEFAULT_INSTANCE);
            fail("/api/person could be accessed without proper security credentials");
        } catch (WebApplicationException wae) {
            assertNotNull(wae);
            assertThat(wae.getResponse(), hasStatus(UNAUTHORIZED));
        }
    }

}
