/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.telemetry.tracing.jaxws;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import org.glassfish.webservices.monitoring.MonitorContext;
import org.glassfish.webservices.monitoring.MonitorFilter;
import org.jvnet.hk2.annotations.Service;

import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;


@Service(name = "jaxws-telemetry-tracing-filter")
public class JaxWsContainerRequestTelemetryTracingFilter implements MonitorFilter {

    private static final Logger logger = Logger.getLogger(JaxWsContainerRequestTelemetryTracingFilter.class.getName());

    @Override
    public void filterRequest(Packet pipeRequest, MonitorContext monitorContext) {
        // Nothing to do on the request path: the SERVER span is either owned by StandardWrapper
        // (servlet-based JAX-WS endpoints) or by EjbWebServiceServlet (EJB-based endpoints).
        // Both stash the span in PAYARA_OTEL_SERVER_SPAN before the JAX-WS pipeline runs.
        // Span name is kept as the HTTP route ("POST /path") — no SOAP-specific overrides.
    }

    @Override
    public void filterResponse(Packet pipeRequest, Packet pipeResponse, MonitorContext monitorContext) {
        Message message = pipeResponse.getMessage();

        if (message != null && message.isFault()) {
            SOAPFault fault = extractFault(message);

            String description = fault != null ? fault.getFaultString() : "SOAP fault";
            Span span = Span.current();
            // Include the fault string as the ERROR status description so it
            // appears directly in the trace without needing to open the span detail.
            span.setStatus(StatusCode.ERROR, description);

            if (fault != null) {
                // fault code (e.g. "SOAP-ENV:Server") as a low-cardinality attribute
                span.setAttribute(AttributeKey.stringKey("soap.fault.code"), fault.getFaultCode());
                span.setAttribute(AttributeKey.stringKey("soap.fault.string"), fault.getFaultString());
            }
        }
    }

    /**
     * Extracts the {@link SOAPFault} from the response message, or {@code null}
     * if the message cannot be parsed as a SOAP message.
     *
     * <p>Key fields on {@link SOAPFault}:
     * <ul>
     *   <li>{@code getFaultCode()} — qualified fault code, e.g. {@code "SOAP-ENV:Server"}
     *       or {@code "env:Receiver"} (SOAP 1.2)</li>
     *   <li>{@code getFaultString()} — human-readable description, typically the
     *       exception message from the endpoint implementation</li>
     *   <li>{@code getDetail()} — optional XML detail element with application-specific
     *       information (e.g. serialised exception, error codes)</li>
     * </ul>
     */
    private SOAPFault extractFault(Message message) {
        try {
            return message.copy().readAsSOAPMessage().getSOAPBody().getFault();
        } catch (SOAPException e) {
            logger.log(WARNING, "Could not extract SOAP fault from response message", e);
            return null;
        }
    }

}
