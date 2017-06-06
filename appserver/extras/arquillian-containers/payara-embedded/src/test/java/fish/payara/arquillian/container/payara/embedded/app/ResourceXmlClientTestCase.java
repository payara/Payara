/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * GlassFishEmbeddedContainerTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class ResourceXmlClientTestCase {
    
    /**
     * Deployment for the test
     */
    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class).addClass(MyServlet.class);
    }

    @Test
    public void shouldBeAbleToDeployWebArchive(@ArquillianResource(MyServlet.class) URL baseURL) throws Exception {
        String body = readAllAndClose(new URL(baseURL, "Test").openStream());

        Assert.assertEquals("Verify that the servlet was deployed and returns expected result", MyServlet.MESSAGE, body);
    }

    private String readAllAndClose(InputStream is) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int read;
            while ((read = is.read()) != -1) {
                out.write(read);
            }
            
            return out.toString();
        }
    }
}