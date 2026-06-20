/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.telemetry.tracing.ejb.iiop;

import fish.payara.opentracing.OpenTelemetryService;
import fish.payara.opentracing.OpenTracingService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.logging.Logger;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import static fish.payara.microprofile.telemetry.tracing.ejb.iiop.OpenTelemetryIiopClientInterceptor.OPENTELEMETRY_IIOP_ID;


/**
 * IIOP Server Interceptor for propagating OpenTracing SpanContext to Payara Server.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class OpenTelemetryIiopServerInterceptor extends LocalObject implements ServerRequestInterceptor {

    private static final Logger LOGGER = Logger.getLogger(OpenTelemetryIiopServerInterceptor.class.getName());

    private OpenTracingService openTracingService;
    private final ThreadLocal<Scope> currentScope = new ThreadLocal<>();
    private final ThreadLocal<Span> currentSpan = new ThreadLocal<>();

    public OpenTelemetryIiopServerInterceptor(OpenTracingService openTracingService) {
        this.openTracingService = openTracingService;
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        // Noop
    }

    @Override
    public void receive_request(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        ServiceContext serviceContext;

        if (!tracerAvailable()) {
            return;
        }

        try {
            serviceContext = serverRequestInfo.get_request_service_context(OPENTELEMETRY_IIOP_ID);
            if (serviceContext == null) {
                return;
            }
        } catch (BAD_PARAM e) {
            // No OTel context was propagated by the client
            return;
        }

        HashMap<String, String> contextMap;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serviceContext.context_data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            contextMap = (HashMap<String, String>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new ForwardRequest(e.getMessage(), serverRequestInfo);
        }

        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        TextMapGetter<HashMap<String, String>> getter = new TextMapGetter<HashMap<String, String>>() {
            @Override
            public Iterable<String> keys(HashMap<String, String> carrier) {
                return carrier.keySet();
            }

            @Override
            public String get(HashMap<String, String> carrier, String key) {
                return carrier.get(key);
            }
        };

        Context parentContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), contextMap, getter);
        Tracer tracer = openTelemetry.getTracerProvider().get(OpenTelemetryService.INSTRUMENTATION_SCOPE_NAME);
        Span span = tracer.spanBuilder("rmi")
                .setParent(parentContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("component", "ejb")
                .startSpan();

        if (currentScope.get() != null) {
            LOGGER.warning("Overlapping traced RMI operations identified, please report");
        }

        currentSpan.set(span);
        currentScope.set(span.makeCurrent());
    }

    @Override
    public void send_reply(ServerRequestInfo serverRequestInfo) {
        closeScope();
    }

    @Override
    public void send_exception(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        closeScope();
    }

    @Override
    public void send_other(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        closeScope();
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void destroy() {

    }

    private void closeScope() {
        Scope scope = currentScope.get();
        if (scope != null) {
            scope.close();
            currentScope.remove();
        }
        Span span = currentSpan.get();
        if (span != null) {
            span.end();
            currentSpan.remove();
        }
    }

    private boolean tracerAvailable() {
        if (openTracingService == null) {
            return false;
        }
        return openTracingService.isEnabled();
    }
}
