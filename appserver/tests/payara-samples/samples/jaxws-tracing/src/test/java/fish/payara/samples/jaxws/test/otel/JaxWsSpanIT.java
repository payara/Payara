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
package fish.payara.samples.jaxws.test.otel;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.jaxws.endpoint.ejb.JAXWSEndPointImplementation;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that both EJB-based and servlet-based JAX-WS endpoints produce a
 * SERVER span and mark it ERROR on SOAP faults.
 *
 * <p>Runs in-container so {@link InMemorySpanExporter} is injected directly.
 * Calls are made over the loopback HTTP interface to avoid collocation optimisations.
 *
 * <p>Both endpoint types must produce a {@link SpanKind#SERVER} span whose name
 * is {@code "POST <requestUri>"} — no SOAP-specific overrides.
 */
@RunWith(Arquillian.class)
@NotMicroCompatible("JAX-WS not supported on Micro")
public class JaxWsSpanIT {

    @ArquillianResource
    private URL baseUrl;

    @Inject
    private InMemorySpanExporter exporter;

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "jaxws-span-it.war")
                // EJB endpoint
                .addPackage(JAXWSEndPointImplementation.class.getPackage())
                // Servlet endpoint
                .addPackage(fish.payara.samples.jaxws.endpoint.servlet.JAXWSEndPointImplementation.class.getPackage())
                .addClasses(
                        InMemorySpanExporter.class,
                        InMemorySpanExporter.Provider.class,
                        JaxWsOtelConfigSource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporter.Provider.class)
                .addAsServiceProvider(ConfigSource.class, JaxWsOtelConfigSource.class)
                .addAsLibraries(
                        Maven.resolver()
                                .loadPomFromFile("pom.xml")
                                .resolve("org.assertj:assertj-core")
                                .withTransitivity()
                                .asFile());
    }

    @Before
    public void reset() {
        exporter.reset();
    }

    // -------------------------------------------------------------------------
    // EJB endpoint — deployed at server root via EjbWSAdapter (no StandardWrapper)
    // -------------------------------------------------------------------------

    @Test
    public void ejbEndpoint_serverSpanCreated() throws Exception {
        ejbEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for EJB endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(server.getName()).startsWith("POST /");
    }

    @Test
    public void ejbEndpoint_serverSpanIsErrorOnSoapFault() throws Exception {
        try {
            ejbEndpoint().sayHiWithFault("Payara");
        } catch (Exception ignored) { }

        List<SpanData> spans = exporter.getSpans();
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for EJB endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(server.getStatus().getStatusCode())
                .describedAs("EJB endpoint SERVER span should be ERROR on SOAP fault")
                .isEqualTo(StatusCode.ERROR);
        assertSoapFaultAttributes(server);
    }

    // -------------------------------------------------------------------------
    // Servlet endpoint — deployed inside WAR, goes through StandardWrapper
    // -------------------------------------------------------------------------

    @Test
    public void servletEndpoint_serverSpanCreated() throws Exception {
        servletEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for servlet endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(server.getName()).startsWith("POST /");
    }

    @Test
    public void servletEndpoint_serverSpanIsErrorOnSoapFault() throws Exception {
        try {
            servletEndpoint().sayHiWithFault("Payara");
        } catch (Exception ignored) { }

        List<SpanData> spans = exporter.getSpans();
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for servlet endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(server.getStatus().getStatusCode())
                .describedAs("Servlet endpoint SERVER span should be ERROR on SOAP fault")
                .isEqualTo(StatusCode.ERROR);
        assertSoapFaultAttributes(server);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** EJB endpoints are published at the server root (not under the WAR context path). */
    private fish.payara.samples.jaxws.endpoint.ejb.JAXWSEndPointInterface ejbEndpoint() throws Exception {
        URL wsdlUrl = new URL(baseUrl.getProtocol(), baseUrl.getHost(), baseUrl.getPort(),
                "/JAXWSEndPointImplementationService/JAXWSEndPointImplementation?wsdl");
        Service service = Service.create(wsdlUrl,
                new QName("http://ejb.endpoint.jaxws.samples.payara.fish/",
                        "JAXWSEndPointImplementationService"));
        return service.getPort(fish.payara.samples.jaxws.endpoint.ejb.JAXWSEndPointInterface.class);
    }

    /** Servlet endpoints are published under the WAR context path. */
    private fish.payara.samples.jaxws.endpoint.servlet.JAXWSEndPointInterface servletEndpoint() throws Exception {
        URL wsdlUrl = new URL(baseUrl, "JAXWSEndPointImplementationService?wsdl");
        Service service = Service.create(wsdlUrl,
                new QName("http://servlet.endpoint.jaxws.samples.payara.fish/",
                        "JAXWSEndPointImplementationService"));
        return service.getPort(fish.payara.samples.jaxws.endpoint.servlet.JAXWSEndPointInterface.class);
    }

    private static final AttributeKey<String> SOAP_FAULT_CODE = AttributeKey.stringKey("soap.fault.code");
    private static final AttributeKey<String> SOAP_FAULT_STRING = AttributeKey.stringKey("soap.fault.string");

    private void assertSoapFaultAttributes(SpanData server) {
        assertThat(server.getAttributes().get(SOAP_FAULT_CODE))
                .describedAs("soap.fault.code should be present on fault span")
                .isNotNull()
                .isNotEmpty();
        assertThat(server.getAttributes().get(SOAP_FAULT_STRING))
                .describedAs("soap.fault.string should be present on fault span")
                .isNotNull()
                .isNotEmpty();
    }

    private SpanData findServer(List<SpanData> spans) {
        return spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .filter(s -> s.getName().startsWith("POST "))
                .findFirst().orElse(null);
    }

    private String names(List<SpanData> spans) {
        return spans.stream()
                .map(s -> s.getName() + "(" + s.getKind() + ")")
                .toList().toString();
    }
}
