/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.rolesallowed.unprotected.methods;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import java.net.URI;
import org.junit.Assert;
import org.junit.Test;

import jakarta.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Test that verifies the automatic propagation of baggage items across process boundaries when using Remote EJBs.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class RemoteEjbClientIT {
    @ArquillianResource
    private URI uri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addPackages(true, RemoteEjbClientIT.class.getPackage());
    }

    @Test
    public void executeHelloServiceBeanPermitAllMethodWithoutAuthIT() {
        Properties contextProperties = new Properties();
        contextProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialHost", "localhost");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialPort", "3700");

        try {
            Context context = new InitialContext(contextProperties);

            HelloServiceRemote ejb = (HelloServiceRemote) context.lookup(String.format(
                    "java:global%sHelloServiceBean!fish.payara.samples.rolesallowed.unprotected.methods.HelloServiceRemote",
                    uri.getPath()));

            System.out.println(ejb.sayHello());
            Assert.assertTrue(ejb.sayHello().equalsIgnoreCase("Hello Anonymous!"));
        } catch (NamingException ne) {
            // Print the exception, so we know, where it failed
            ne.printStackTrace();
            Assert.fail("Failed performing lookup:\n" + ne.getCause());
        }
    }

    @Test
    public void executeHelloServiceBeanRolesAllowedMethodWithoutAuthIT() {
        Properties contextProperties = new Properties();
        contextProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialHost", "localhost");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialPort", "3700");

        try {
            Context context = new InitialContext(contextProperties);
            HelloServiceRemote ejb = (HelloServiceRemote) context.lookup(String.format(
                    "java:global%sHelloServiceBean!fish.payara.samples.rolesallowed.unprotected.methods.HelloServiceRemote",
                    uri.getPath()));

            try {
                // Should fail
                System.out.println(ejb.secureSayHello());
                Assert.fail("Managed to access secured method without being authenticated");
            } catch (EJBAccessException ejbAccessException) {
                System.out.println("Successfully prevented from accessing method without being authenticated");
            }
        } catch (NamingException ne) {
            Assert.fail("Failed performing lookup:\n" + ne.getCause());
        }
    }

    @Test
    public void lookupProtectedHelloServiceBeanWithoutAuthIT() {
        Properties contextProperties = new Properties();
        contextProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialHost", "localhost");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialPort", "3700");

        try {
            Context context = new InitialContext(contextProperties);

            // Should fail
            ProtectedHelloServiceRemote ejb = (ProtectedHelloServiceRemote) context.lookup(String.format(
                    "java:global%sProtectedHelloServiceBean!fish.payara.samples.rolesallowed.unprotected.methods.ProtectedHelloServiceRemote",
                    uri.getPath()));
            Assert.fail("Managed to access fully-secured EJB without being authenticated");
        } catch (NamingException ne) {
            Assert.assertTrue("Lookup seems to have failed for an unexpected reason. " +
                            "Expected message to contain \"CORBA NO_PERMISSION\"",
                    ne.toString().contains("CORBA NO_PERMISSION"));
        }
    }

}
