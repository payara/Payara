/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.clustered.singleton;

import fish.payara.samples.clustered.singleton.api.AnnotatedSingletonAPI;
import fish.payara.samples.clustered.singleton.api.InterceptedSingletonAPI;
import fish.payara.samples.clustered.singleton.api.Secondary;
import fish.payara.samples.clustered.singleton.api.SingletonAPI;
import java.io.File;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author lprimak
 */
@RunWith(Arquillian.class)
public class ClusteredSingletonTest {
    private static final Logger log = Logger.getLogger(ClusteredSingletonTest.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cst.war")
                .addPackages(false, "fish.payara.samples.clustered.singleton",
                        "fish.payara.samples.clustered.singleton.api",
                        "fish.payara.samples.clustered.singleton.interceptor")
                .addAsWebInfResource(new File(WEBAPP_SRC, "WEB-INF/ejb-jar.xml"))
                .addAsWebInfResource(new File(WEBAPP_SRC, "WEB-INF/glassfish-ejb-jar.xml"));
    }

    @Test
    public void descriptorApi() {
        assertThat(descAPI1.getHello(), startsWith("Descriptor EJB Hello"));
        assertThat(descAPI1.getState(), equalTo(descAPI2.getState()));
    }

    @Test
    public void annotatedApi() {
        assertThat(annotatedApi1.getHello(), startsWith("Clustered Annotated API EJB Hello"));
        assertThat(annotatedApi1.getState(), equalTo(annotatedApi2.getState()));
    }

    @Test
    public void cdiApi() {
        assertThat(cdiApi1.getHello(), startsWith("CDI Bean Hello"));
        assertThat(cdiApi1.getState(), equalTo(cdiApi2.getState()));
    }

    @Test(expected = EJBException.class)
    public void twoMethodsNotEqual() {
        assertThat(annotatedApi1.getHello(), not(equalTo(annotatedApi2.getHello())));
    }

    @Test
    public void interceptor() {
        assertThat(interceptedAPI.getHello(), startsWith("Intercepted Annotated EJB Hello"));
        interceptedAPI.waitForTimer();
        assertThat(interceptedAPI.isConsistent(), is(true));
    }


    private @EJB(lookup = "java:module/ClusteredSingletonEjbXml1") SingletonAPI descAPI1;
    private @EJB(lookup = "java:module/ClusteredSingletonEjbXml2") SingletonAPI descAPI2;

    private @EJB(lookup = "java:module/ClusteredSingletonAnnotatedEJB1") AnnotatedSingletonAPI annotatedApi1;
    private @EJB(lookup = "java:module/ClusteredSingletonAnnotatedEJB2") AnnotatedSingletonAPI annotatedApi2;

    private @EJB InterceptedSingletonAPI interceptedAPI;
    private @Inject SingletonAPI cdiApi1;
    private @Inject @Secondary SingletonAPI cdiApi2;

    public static final String WEBAPP_SRC = "src/main/webapp";
}
