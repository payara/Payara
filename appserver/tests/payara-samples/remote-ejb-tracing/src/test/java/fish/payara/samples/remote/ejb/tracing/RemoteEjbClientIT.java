/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.remote.ejb.tracing;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.remote.ejb.tracing.server.Ejb;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import java.net.URI;
import org.junit.Assert;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
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
@RunAsClient
@NotMicroCompatible
public class RemoteEjbClientIT {
    @ArquillianResource
    private URI uri;

    @Test
    public void executeRemoteEjbMethodIT() throws NamingException {
        Properties contextProperties = new Properties();
        contextProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialHost", "localhost");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
        // enable OpenTelemetry tracing so we get our OpenTracing instance
        System.setProperty("otel.sdk.disabled", "false");


        Context context = new InitialContext(contextProperties);
        EjbRemote ejb = (EjbRemote) context.lookup(String.format("java:global%sEjb", uri.getPath()));


        Tracer tracer = GlobalTracer.get();
        Span span = tracer.buildSpan("ExecuteEjb").start();
        try (Scope scope = tracer.activateSpan(span)) {
            span.setBaggageItem("Wibbles", "Wobbles");
            String baggageItems = ejb.annotatedMethod();
            Assert.assertTrue("Baggage items didn't match, received: " + baggageItems,
                    baggageItems.contains("\nWibbles : Wobbles\n"));

            span.setBaggageItem("Nibbles", "Nobbles");
            baggageItems = ejb.nonAnnotatedMethod();
            Assert.assertTrue("Baggage items didn't match, received: " + baggageItems,
                    baggageItems.contains("Wibbles : Wobbles")
                    && baggageItems.contains("Nibbles : Nobbles"));

            span.setBaggageItem("Bibbles", "Bobbles");
            baggageItems = ejb.shouldNotBeTraced();
            Assert.assertTrue("Baggage items didn't match, received: " + baggageItems,
                    baggageItems.contains("Wibbles : Wobbles")
                    && baggageItems.contains("Nibbles : Nobbles")
                    && baggageItems.contains("Bibbles : Bobbles"));

            baggageItems = ejb.editBaggageItems();
            Assert.assertTrue("Baggage items didn't match, received: " + baggageItems,
                    baggageItems.contains("Wibbles : Wabbles")
                    && baggageItems.contains("Nibbles : Nabbles")
                    && baggageItems.contains("Bibbles : Babbles"));
        } finally {
            span.finish();
        }
    }

    @Test
    public void transactionIdAddedAsBaggageIT() throws NamingException {
        Properties contextProperties = new Properties();
        contextProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialHost", "localhost");
        contextProperties.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
        // enable OpenTelemetry tracing so we get our OpenTracing instance
        System.setProperty("otel.sdk.disabled", "false");

        Context context = new InitialContext(contextProperties);
        EjbRemote ejb = (EjbRemote) context.lookup(String.format("java:global%sEjb", uri.getPath()));

        Tracer tracer = GlobalTracer.get();

        Span span = tracer.buildSpan("ExecuteEjb").start();
        try(Scope scope = tracer.activateSpan(span)) {
            String baggageItems = ejb.annotatedMethod();
            Assert.assertTrue("Baggage items didn't contain transaction ID, received: " + baggageItems,
                    baggageItems.contains("TX-ID"));
        } finally {
            span.finish();
        }
    }

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class).addClasses(EjbRemote.class, Ejb.class);
    }
}
