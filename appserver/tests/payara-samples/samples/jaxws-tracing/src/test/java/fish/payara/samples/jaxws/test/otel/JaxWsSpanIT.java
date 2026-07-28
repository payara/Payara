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
import jakarta.ejb.EJB;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
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
import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(JaxWsSpanIT.class.getName());

    @ArquillianResource
    private URL baseUrl;

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private InMemoryMetricExporter metricExporter;

    @EJB
    private JaxWsRefClientBean refBean;

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
                        InMemoryMetricExporter.class,
                        InMemoryMetricExporter.Provider.class,
                        JaxWsOtelConfigSource.class,
                        JAXWSEjbEndPointService.class,
                        JaxWsRefClientBean.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporter.Provider.class)
                .addAsServiceProvider(ConfigurableMetricExporterProvider.class, InMemoryMetricExporter.Provider.class)
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
        metricExporter.reset();
    }

    // -------------------------------------------------------------------------
    // EJB endpoint — deployed at server root via EjbWSAdapter (no StandardWrapper)
    // -------------------------------------------------------------------------

    @Test
    public void ejbEndpoint_serverSpanCreated() throws Exception {
        ejbEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("ejbEndpoint_serverSpanCreated", spans);
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for EJB endpoint, got: %s", names(spans))
                .isNotNull();
    }

    @Test
    public void ejbEndpoint_serverSpanIsErrorOnSoapFault() throws Exception {
        try {
            ejbEndpoint().sayHiWithFault("Payara");
        } catch (Exception ignored) { }

        List<SpanData> spans = exporter.getSpans();
        logSpans("ejbEndpoint_serverSpanIsErrorOnSoapFault", spans);
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
        logSpans("servletEndpoint_serverSpanCreated", spans);
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for servlet endpoint, got: %s", names(spans))
                .isNotNull();
    }

    @Test
    public void servletEndpoint_serverSpanIsErrorOnSoapFault() throws Exception {
        try {
            servletEndpoint().sayHiWithFault("Payara");
        } catch (Exception ignored) { }

        List<SpanData> spans = exporter.getSpans();
        logSpans("servletEndpoint_serverSpanIsErrorOnSoapFault", spans);
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
    // @WithSpan name enrichment — the SERVER span should be renamed
    // -------------------------------------------------------------------------

    @Test
    public void ejbEndpoint_withSpanRenamesServerSpan() throws Exception {
        ejbEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("ejbEndpoint_withSpanRenamesServerSpan", spans);
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for EJB endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(server.getName())
                .describedAs("@WithSpan(\"customOperation\") should rename the SERVER span, got: %s", names(spans))
                .isEqualTo("customOperation");
        assertThat(spans.stream().anyMatch(s -> s.getKind() == SpanKind.INTERNAL))
                .describedAs("No INTERNAL child span should be created for JAX-WS methods")
                .isFalse();
    }

    @Test
    public void servletEndpoint_withSpanRenamesServerSpan() throws Exception {
        servletEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("servletEndpoint_withSpanRenamesServerSpan", spans);
        SpanData server = findServer(spans);

        assertThat(server)
                .describedAs("Expected a SERVER span for servlet endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(server.getName())
                .describedAs("@WithSpan(\"customOperation\") should rename the SERVER span, got: %s", names(spans))
                .isEqualTo("customOperation");
        assertThat(spans.stream().anyMatch(s -> s.getKind() == SpanKind.INTERNAL))
                .describedAs("No INTERNAL child span should be created for JAX-WS methods")
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Outbound JAX-WS client — CLIENT span and trace context propagation
    // -------------------------------------------------------------------------

    @Test
    public void ejbEndpoint_clientSpanCreated() throws Exception {
        ejbEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("ejbEndpoint_clientSpanCreated", spans);
        SpanData client = findClient(spans);

        assertThat(client)
                .describedAs("Expected a CLIENT span for outbound JAX-WS call to EJB endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(client.getAttributes().get(HTTP_REQUEST_METHOD))
                .describedAs("CLIENT span must have http.request.method=POST")
                .isEqualTo("POST");
    }

    @Test
    public void servletEndpoint_clientSpanCreated() throws Exception {
        servletEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("servletEndpoint_clientSpanCreated", spans);
        SpanData client = findClient(spans);

        assertThat(client)
                .describedAs("Expected a CLIENT span for outbound JAX-WS call to servlet endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(client.getAttributes().get(HTTP_REQUEST_METHOD))
                .describedAs("CLIENT span must have http.request.method=POST")
                .isEqualTo("POST");
    }

    @Test
    public void ejbEndpoint_clientSpanPropagatesTraceContext() throws Exception {
        ejbEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("ejbEndpoint_clientSpanPropagatesTraceContext", spans);
        SpanData client = findClient(spans);
        SpanData server = findServer(spans);

        assertThat(client)
                .describedAs("Expected a CLIENT span for outbound JAX-WS call, got: %s", names(spans))
                .isNotNull();
        assertThat(server)
                .describedAs("Expected a SERVER span for EJB endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(client.getTraceId())
                .describedAs("CLIENT and SERVER spans must share the same trace ID — W3C traceparent must be propagated")
                .isEqualTo(server.getTraceId());
        assertThat(server.getParentSpanId())
                .describedAs("SERVER span's parent must be the CLIENT span")
                .isEqualTo(client.getSpanId());
    }

    @Test
    public void servletEndpoint_clientSpanPropagatesTraceContext() throws Exception {
        servletEndpoint().sayHi("Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("servletEndpoint_clientSpanPropagatesTraceContext", spans);
        SpanData client = findClient(spans);
        SpanData server = findServer(spans);

        assertThat(client)
                .describedAs("Expected a CLIENT span for outbound JAX-WS call, got: %s", names(spans))
                .isNotNull();
        assertThat(server)
                .describedAs("Expected a SERVER span for servlet endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(client.getTraceId())
                .describedAs("CLIENT and SERVER spans must share the same trace ID — W3C traceparent must be propagated")
                .isEqualTo(server.getTraceId());
        assertThat(server.getParentSpanId())
                .describedAs("SERVER span's parent must be the CLIENT span")
                .isEqualTo(client.getSpanId());
    }

    // -------------------------------------------------------------------------
    // @WebServiceRef pattern — container-managed port, dynamic endpoint address
    // -------------------------------------------------------------------------

    @Test
    public void ejbEndpoint_webServiceRef_clientSpanCreated() throws Exception {
        refBean.callEndpoint(ejbEndpointAddress(), "Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("ejbEndpoint_webServiceRef_clientSpanCreated", spans);
        SpanData client = findClient(spans);

        assertThat(client)
                .describedAs("Expected a CLIENT span for @WebServiceRef outbound JAX-WS call, got: %s", names(spans))
                .isNotNull();
        assertThat(client.getAttributes().get(HTTP_REQUEST_METHOD))
                .describedAs("CLIENT span must have http.request.method=POST")
                .isEqualTo("POST");
    }

    @Test
    public void ejbEndpoint_webServiceRef_clientSpanPropagatesTraceContext() throws Exception {
        refBean.callEndpoint(ejbEndpointAddress(), "Payara");

        List<SpanData> spans = exporter.getSpans();
        logSpans("ejbEndpoint_webServiceRef_clientSpanPropagatesTraceContext", spans);
        SpanData client = findClient(spans);
        SpanData server = findServer(spans);

        assertThat(client)
                .describedAs("Expected a CLIENT span for @WebServiceRef call, got: %s", names(spans))
                .isNotNull();
        assertThat(server)
                .describedAs("Expected a SERVER span for EJB endpoint, got: %s", names(spans))
                .isNotNull();
        assertThat(client.getTraceId())
                .describedAs("CLIENT and SERVER spans must share the same trace ID")
                .isEqualTo(server.getTraceId());
        assertThat(server.getParentSpanId())
                .describedAs("SERVER span's parent must be the CLIENT span")
                .isEqualTo(client.getSpanId());
    }

    // -------------------------------------------------------------------------
    // http.server.request.duration histogram
    // -------------------------------------------------------------------------

    private static final String HTTP_SERVER_REQUEST_DURATION = "http.server.request.duration";

    @Test
    public void ejbEndpoint_httpDurationHistogramRecorded() throws Exception {
        ejbEndpoint().sayHi("Payara");

        // The JAX-WS client fetches the WSDL (GET) before the SOAP POST — filter to POST only.
        HistogramPointData post = metricExporter.getHistogramPoints(HTTP_SERVER_REQUEST_DURATION).stream()
                .filter(p -> "POST".equals(p.getAttributes().get(HTTP_REQUEST_METHOD)))
                .findFirst().orElse(null);

        assertThat(post)
                .describedAs("EJB JAX-WS endpoint must emit an http.server.request.duration histogram point for POST")
                .isNotNull();
        assertThat(post.getSum())
                .describedAs("Histogram duration must be positive")
                .isGreaterThan(0.0);
    }

    @Test
    public void servletEndpoint_httpDurationHistogramRecorded() throws Exception {
        servletEndpoint().sayHi("Payara");

        // The JAX-WS client fetches the WSDL (GET) before the SOAP POST — filter to POST only.
        HistogramPointData post = metricExporter.getHistogramPoints(HTTP_SERVER_REQUEST_DURATION).stream()
                .filter(p -> "POST".equals(p.getAttributes().get(HTTP_REQUEST_METHOD)))
                .findFirst().orElse(null);

        assertThat(post)
                .describedAs("Servlet JAX-WS endpoint must emit an http.server.request.duration histogram point for POST")
                .isNotNull();
        assertThat(post.getSum())
                .describedAs("Histogram duration must be positive")
                .isGreaterThan(0.0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String ejbEndpointAddress() {
        return baseUrl.getProtocol() + "://" + baseUrl.getHost() + ":" + baseUrl.getPort()
                + "/JAXWSEndPointImplementationService/JAXWSEndPointImplementation";
    }

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

    private static final AttributeKey<String> HTTP_REQUEST_METHOD =
            AttributeKey.stringKey("http.request.method");

    /**
     * Finds the SERVER span for the SOAP POST request.
     * Filters by {@code http.request.method=POST} attribute so that WSDL GET spans
     * (which share the same SERVER kind) are excluded, and so that post-enrichment
     * span names (e.g. {@code "customOperation"}) are still found correctly.
     */
    private SpanData findServer(List<SpanData> spans) {
        return spans.stream()
                .filter(s -> s.getKind() == SpanKind.SERVER)
                .filter(s -> "POST".equals(s.getAttributes().get(HTTP_REQUEST_METHOD)))
                .findFirst().orElse(null);
    }

    /** Finds the CLIENT span for an outbound SOAP POST request. */
    private SpanData findClient(List<SpanData> spans) {
        return spans.stream()
                .filter(s -> s.getKind() == SpanKind.CLIENT)
                .filter(s -> "POST".equals(s.getAttributes().get(HTTP_REQUEST_METHOD)))
                .findFirst().orElse(null);
    }

    private String names(List<SpanData> spans) {
        return spans.stream()
                .map(s -> s.getName() + "(" + s.getKind() + ")")
                .toList().toString();
    }

    private void logSpans(String label, List<SpanData> spans) {
        StringBuilder sb = new StringBuilder("\n=== ").append(label).append(" (").append(spans.size()).append(" spans) ===");
        for (SpanData span : spans) {
            sb.append("\n  name=").append(span.getName())
              .append(" kind=").append(span.getKind())
              .append(" status=").append(span.getStatus().getStatusCode())
              .append(" traceId=").append(span.getTraceId())
              .append(" spanId=").append(span.getSpanId())
              .append(" parentSpanId=").append(span.getParentSpanId())
              .append(" attrs=").append(span.getAttributes());
        }
        LOG.fine(sb.toString());
    }
}
