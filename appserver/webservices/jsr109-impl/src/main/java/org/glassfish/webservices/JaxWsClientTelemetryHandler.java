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
 * Version 2] license."  If you don't indicate a single license of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.webservices;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;

import fish.payara.opentracing.OpenTelemetryService;
import fish.payara.opentracing.PropagationHelper;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 * Client-side JAX-WS SOAP handler that creates an OTel CLIENT span and injects
 * W3C {@code traceparent}/{@code tracestate} headers into the outbound SOAP request.
 */
class JaxWsClientTelemetryHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String HELPER_KEY = PropagationHelper.class.getName() + ".jaxws-client";
    private static final String PAYARA_SUBSYSTEM = "payara.subsystem";
    private static final Logger LOGGER = Logger.getLogger(JaxWsClientTelemetryHandler.class.getName());

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        if (Boolean.TRUE.equals(context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))) {
            startClientSpan(context);
        } else {
            finishClientSpan(context, false);
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        finishClientSpan(context, true);
        return true;
    }

    @Override
    public void close(MessageContext context) {
        // Safety net: if the inbound handler never fired (e.g. transport error), end the span here.
        PropagationHelper helper = (PropagationHelper) context.get(HELPER_KEY);
        if (helper != null) {
            helper.end();
            helper.close();
        }
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    private void startClientSpan(SOAPMessageContext context) {
        ServiceLocator locator = Globals.getDefaultBaseServiceLocator();
        if (locator == null) {
            return;
        }
        ServiceHandle<OpenTelemetryService> handle = locator.getServiceHandle(OpenTelemetryService.class);
        if (handle == null || !handle.isActive() || !handle.getService().isEnabled()) {
            return;
        }
        OpenTelemetryService otelService = handle.getService();

        String endpointAddress = (String) context.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        URI uri = parseUri(endpointAddress);

        var spanBuilder = otelService.getCurrentTracer()
                .spanBuilder("POST")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "POST")
                .setAttribute(PAYARA_SUBSYSTEM, "jakarta-xml-ws")
                .setParent(Context.current());

        if (uri != null) {
            spanBuilder.setAttribute(UrlAttributes.URL_FULL, endpointAddress);
            spanBuilder.setAttribute(ServerAttributes.SERVER_ADDRESS, uri.getHost());
            if (uri.getPort() != -1) {
                spanBuilder.setAttribute(ServerAttributes.SERVER_PORT, (long) uri.getPort());
            }
        }

        Span span = spanBuilder.startSpan();
        PropagationHelper helper = PropagationHelper.start(span, null);
        context.put(HELPER_KEY, helper);
        context.setScope(HELPER_KEY, MessageContext.Scope.HANDLER);

        Map<String, List<String>> headers = (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
        if (headers == null) {
            headers = new HashMap<>();
            context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
            context.setScope(MessageContext.HTTP_REQUEST_HEADERS, MessageContext.Scope.HANDLER);
        }
        Map<String, List<String>> outboundHeaders = headers;
        otelService.getCurrentSdk().getPropagators().getTextMapPropagator().inject(
                Context.current(),
                outboundHeaders,
                (carrier, key, value) -> carrier.put(key, List.of(value))
        );
    }

    private void finishClientSpan(SOAPMessageContext context, boolean fault) {
        PropagationHelper helper = (PropagationHelper) context.get(HELPER_KEY);
        if (helper == null) {
            return;
        }
        if (fault) {
            helper.span().setStatus(StatusCode.ERROR, "SOAP Fault");
        }
        helper.end();
        helper.close();
        context.remove(HELPER_KEY);
    }

    private static URI parseUri(String address) {
        if (address == null) {
            return null;
        }
        try {
            return new URI(address);
        } catch (URISyntaxException e) {
            LOGGER.log(Level.FINE, "Could not parse JAX-WS endpoint address as URI: {0}", address);
            return null;
        }
    }
}
