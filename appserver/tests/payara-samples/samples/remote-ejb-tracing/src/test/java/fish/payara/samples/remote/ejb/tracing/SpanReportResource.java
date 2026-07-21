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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JAX-RS resource that exposes collected span data from {@link InMemorySpanExporter}
 * as a simple text report for {@code @RunAsClient} test assertions.
 *
 * Endpoints:
 * <ul>
 *   <li>{@code GET /api/spans/reset} — clears the exporter</li>
 *   <li>{@code GET /api/spans/report} — returns collected spans as text lines</li>
 * </ul>
 */
@Path("/spans")
public class SpanReportResource {

    @Inject
    InMemorySpanExporter exporter;

    @GET
    @Path("/reset")
    @Produces(MediaType.TEXT_PLAIN)
    public String reset() {
        exporter.reset();
        return "reset";
    }

    /**
     * Returns collected spans as text, one per line in the form:
     * {@code name|kind|status|traceId|parentSpanId}
     *
     * The client test parses this to assert on individual fields.
     */
    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_PLAIN)
    public String report() {
        List<SpanData> spans = exporter.getSpans();
        if (spans.isEmpty()) {
            return "EMPTY";
        }
        return spans.stream()
                .map(s -> s.getName()
                        + "|" + s.getKind().name()
                        + "|" + s.getStatus().getStatusCode().name()
                        + "|" + s.getSpanContext().getTraceId()
                        + "|" + s.getSpanContext().getSpanId()
                        + "|" + s.getParentSpanContext().getSpanId()
                        + "|" + (s.getEvents().stream()
                                .anyMatch(e -> "exception".equals(e.getName())) ? "HAS_EXCEPTION" : "NO_EXCEPTION"))
                .collect(Collectors.joining("\n"));
    }
}
