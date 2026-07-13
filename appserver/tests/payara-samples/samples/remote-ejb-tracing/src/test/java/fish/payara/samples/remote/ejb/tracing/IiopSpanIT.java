/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.samples.remote.ejb.tracing.web.IiopTracingResource;
import fish.payara.samples.remote.ejb.tracing.web.JaxrsApp;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies IIOP server interceptor span behaviour using genuine cross-JVM remote calls.
 *
 * <p>The test runs {@code @RunAsClient} and makes IIOP calls directly from the test JVM
 * to the server's IIOP port (3700).  Because the test JVM is separate from the server,
 * the ORB's collocation optimisation does not apply and the PortableInterceptors fire
 * for every business-method invocation.  Span data is retrieved via
 * {@link SpanReportResource} over HTTP.</p>
 *
 * <p>Active tests:</p>
 * <ul>
 *   <li>{@link #serverSpanHasCorrectNameAndKind} — SERVER span name equals CORBA operation name</li>
 *   <li>{@link #serverSpanRecordsErrorOnException} — ERROR status set in {@code send_exception}</li>
 * </ul>
 * {@link #clientSpanCreatedAsParentOfServerSpan} is disabled until the IIOP client
 * interceptor is also fixed (no CLIENT span is created yet).</p>
 */
@RunWith(PayaraArquillianTestRunner.class)
@RunAsClient
@NotMicroCompatible
public class IiopSpanIT {

    private static final String WAR_NAME = "remote-ejb-tracing.war";
    private static final String IIOP_HOST = "localhost";
    private static final int IIOP_PORT = 3700;

    @ArquillianResource
    private URI baseUri;

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, WAR_NAME)
                .addClasses(
                        EjbRemote.class,
                        Ejb.class,
                        JaxrsApp.class,
                        IiopTracingResource.class,
                        InMemorySpanExporter.class,
                        InMemorySpanExporter.Provider.class,
                        SpanReportResource.class,
                        OtelConfigSource.class)
                .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporter.Provider.class)
                .addAsServiceProvider(ConfigSource.class, OtelConfigSource.class)
                .addAsLibraries(
                        Maven.resolver()
                                .loadPomFromFile("pom.xml")
                                .resolve("org.assertj:assertj-core")
                                .withTransitivity()
                                .asFile());
    }

    @Before
    public void resetExporter() {
        api("spans/reset").request().get(String.class);
    }

    // ---------------------------------------------------------------------------
    // Test A: SERVER span name and attributes
    // ---------------------------------------------------------------------------

    /**
     * A genuine cross-JVM IIOP call must produce a SERVER span whose name equals
     * the CORBA operation name.
     */
    @Test
    public void serverSpanHasCorrectNameAndKind() throws Exception {
        lookupEjb().nonAnnotatedMethod();

        List<String[]> spans = getSpanReport();
        String[] serverSpan = findSpan(spans, "SERVER");

        assertNotNull("Expected a SERVER span, got: " + formatReport(spans), serverSpan);
        assertEquals(
                "SERVER span name should be CORBA operation name, not 'rmi'. Got: " + serverSpan[0],
                "nonAnnotatedMethod", serverSpan[0]);
    }

    // ---------------------------------------------------------------------------
    // Test B: SERVER span error status on exception
    // ---------------------------------------------------------------------------

    /**
     * When the remote EJB throws, the IIOP server interceptor's {@code send_exception}
     * must set ERROR status and record the CORBA exception type.
     */
    @Test
    public void serverSpanRecordsErrorOnException() throws Exception {
        try {
            lookupEjb().throwsException();
        } catch (Exception expected) {
            // EJBException wrapping RuntimeException expected
        }

        List<String[]> spans = getSpanReport();
        String[] serverSpan = findSpan(spans, "SERVER");

        assertNotNull("Expected a SERVER span, got: " + formatReport(spans), serverSpan);
        assertEquals(
                "SERVER span should have ERROR status on exception, got: " + serverSpan[2],
                "ERROR", serverSpan[2]);
    }

    // ---------------------------------------------------------------------------
    // Test C: CLIENT span (disabled — client interceptor not yet fixed)
    // ---------------------------------------------------------------------------

    // @Test  — the CLIENT span requires the Payara IIOP client interceptor to fire on the
    //          calling side.  That interceptor is registered only in the server ORB, not in
    //          a plain @RunAsClient test JVM.  To exercise it, the call must originate from
    //          within the server (e.g. another EJB calling a remote EJB) with collocation
    //          optimisation disabled (-Dcom.sun.corba.ee.ORBAllowLocalOptimization=false),
    //          or via the Jakarta EE Application Client Container which bootstraps the full
    //          Payara ORB.  Deferred to a follow-up within FISH-13995.
    public void clientSpanCreatedAsParentOfServerSpan() throws Exception {
        lookupEjb().nonAnnotatedMethod();

        List<String[]> spans = getSpanReport();
        String[] serverSpan = findSpan(spans, "SERVER");
        String[] clientSpan = findSpan(spans, "CLIENT");

        assertNotNull("Expected a SERVER span, got: " + formatReport(spans), serverSpan);
        assertNotNull("Expected a CLIENT span, got: " + formatReport(spans), clientSpan);
        assertEquals("CLIENT and SERVER must share the same trace ID",
                clientSpan[3], serverSpan[3]);
        assertEquals("SERVER span's parent must be the CLIENT span",
                clientSpan[4], serverSpan[5]);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private EjbRemote lookupEjb() throws Exception {
        Properties props = new Properties();
        props.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.enterprise.naming.SerialInitContextFactory");
        props.setProperty("org.omg.CORBA.ORBInitialHost", IIOP_HOST);
        props.setProperty("org.omg.CORBA.ORBInitialPort", String.valueOf(IIOP_PORT));
        return (EjbRemote) new InitialContext(props)
                .lookup("java:global/remote-ejb-tracing/Ejb");
    }

    /** Each row: [name, kind, status, traceId, spanId, parentSpanId, exceptionFlag] */
    private List<String[]> getSpanReport() {
        String report = api("spans/report").request().get(String.class);
        if ("EMPTY".equals(report.trim())) {
            return List.of();
        }
        return Arrays.stream(report.split("\n"))
                .map(line -> line.split("\\|"))
                .collect(Collectors.toList());
    }

    private String[] findSpan(List<String[]> spans, String kind) {
        return spans.stream()
                .filter(row -> row.length > 1 && kind.equals(row[1]))
                .findFirst()
                .orElse(null);
    }

    private String formatReport(List<String[]> spans) {
        return spans.stream().map(row -> String.join("|", row)).collect(Collectors.joining(", "));
    }

    private WebTarget api(String path) {
        return ClientBuilder.newClient().target(baseUri).path("api").path(path);
    }
}
