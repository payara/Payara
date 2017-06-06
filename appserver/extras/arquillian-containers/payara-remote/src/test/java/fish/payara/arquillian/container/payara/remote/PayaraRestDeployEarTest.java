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

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies arquillian tests can run in client mode with this REST based container.
 *
 * @author <a href="http://community.jboss.org/people/aslak">Aslak Knutsen</a>
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
@RunWith(Arquillian.class)
public class PayaraRestDeployEarTest {
    
    private static final Logger log = Logger.getLogger(PayaraRestDeployEarTest.class.getName());

    /**
     * Deployment for the test
     */
    @Deployment(testable = false)
    public static Archive<?> getTestArchive() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
            .addClasses(GreeterServlet.class);
        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "test.jar")
            .addClasses(Greeter.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
            .setApplicationXML("application.xml")
            .addAsModule(war)
            .addAsModule(ejb);
        log.info(ear.toString(true));
        return ear;
    }

    @Test
    public void shouldBeAbleToDeployEnterpriseArchive() throws Exception {
        final String servletPath = GreeterServlet.class.getAnnotation(WebServlet.class).value()[0];

        final URLConnection response = new URL("http://localhost:8080/test" + servletPath).openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
        final String result = in.readLine();

        assertThat(result, equalTo("Hello"));
    }
}
