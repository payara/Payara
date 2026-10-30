/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.samples.resource.fish6449;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// fix for FISH-6449 reverts parts of fix for FISH-2678, we need to reverify it
@RunWith(Arquillian.class)
public class MetricsOnEjbJaxRsResourceTest {
    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(JaxrsApplication.class, TimedStatelessResource.class)
                .addClasses(CustomInterceptor.class, InjectInterceptedBean.class)
                .addClasses(InterceptedBean.class, InterceptedStateless.class)
                .addClasses(CustomInterceptorWithConstructorInjection.class)
                .addAsWebInfResource("beans-with-interceptor.xml", "META-INF/beans.xml");
    }

    @ArquillianResource
    URI base;

    @Inject
    InterceptedStateless stateless;

    @Inject
    InterceptedBean intercepted;

    @Test
    public void testResourceInstatiates() {
        var target = ClientBuilder.newBuilder().build().target(base).path("metered/stateless");
        var output = target.request().get(String.class);
        assertEquals("works", output);
    }

    @Test
    public void interceptedEjbInjectingInterceptorInstantiates() {
        assertNotNull(stateless.test());
    }

    @Test
    public void interceptedCdiBeanInjectingInterceptorInstantiates() {
        assertNotNull(intercepted.test());
    }
}
