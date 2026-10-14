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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.HashMap;
import java.util.logging.Logger;

import static fish.payara.microprofile.telemetry.tracing.ejb.iiop.OpenTelemetryIiopInterceptorFactory.OPENTELEMETRY_IIOP_ID;


/**
 * IIOP Server Interceptor that defers SERVER span creation to
 * {@link OpenTelemetryService#applyDeferredContext()}, called from
 * {@code BaseContainer.preInvoke()} once the application invocation context is
 * on the stack and the correct per-app tracer is available.
 *
 * <p><b>Remote calls only.</b> This interceptor only fires for genuinely remote
 * IIOP calls originating from a separate JVM. Same-JVM calls to a co-located
 * EJB are dispatched directly by the ORB's collocation optimisation and bypass
 * the PortableInterceptor stack entirely, so no span is created for those.
 * To force full IIOP dispatch for same-JVM calls (useful during testing),
 * set the JVM property
 * {@code com.sun.corba.ee.ORBAllowLocalOptimization=false} on the server.
 */
public class OpenTelemetryIiopServerInterceptor extends LocalObject implements ServerRequestInterceptor {

    private static final Logger LOGGER = Logger.getLogger(OpenTelemetryIiopServerInterceptor.class.getName());

    private final ServiceLocator serviceLocator;

    public OpenTelemetryIiopServerInterceptor(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        // Noop
    }

    @Override
    public void receive_request(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        OpenTelemetryService openTelemetryService = getOpenTelemetryService();
        if (openTelemetryService == null) {
            return;
        }

        // Extract the W3C carrier from the OTel service context if the client propagated one.
        // An empty carrier is used when the client did not propagate any context — in that case
        // applyDeferredContext() will create a root (parentless) SERVER span.
        HashMap<String, String> carrier = new HashMap<>();
        try {
            ServiceContext serviceContext = serverRequestInfo.get_request_service_context(OPENTELEMETRY_IIOP_ID);
            if (serviceContext != null) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(serviceContext.context_data);
                     ObjectInput in = new OpenTelemetryIiopObjectInputStream(bis)) {
                    OpenTelemetryIiopTextMap textMap = (OpenTelemetryIiopTextMap) in.readObject();
                    carrier.putAll(textMap);
                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.warning("Failed to deserialise IIOP OTel context: " + e.getMessage());
                    // Continue with empty carrier — still create a root span
                }
            }
        } catch (BAD_PARAM ignored) {
            // No OTel service context propagated by the client — proceed with empty carrier
        }

        // Defer span creation: the application invocation context is not yet on the stack.
        // BaseContainer.preInvoke() will call applyDeferredContext() after
        // invocationManager.preInvoke() establishes it.
        String operation = serverRequestInfo.operation();
        openTelemetryService.collectDeferredContext(
                carrier,
                operation,
                SpanKind.SERVER,
                Attributes.of(
                        AttributeKey.stringKey("rpc.system.name"), "corba",
                        AttributeKey.stringKey("rpc.method"), operation));
    }

    @Override
    public void send_reply(ServerRequestInfo serverRequestInfo) {
        endSpan(null);
    }

    @Override
    public void send_exception(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        // sending_exception() is the server-side API; extract the CORBA type ID
        // from the Any's TypeCode, e.g. "IDL:omg.org/CORBA/COMM_FAILURE:1.0"
        String exceptionId = exceptionId(serverRequestInfo);
        endSpan(new RuntimeException(exceptionId));
    }

    private static String exceptionId(ServerRequestInfo info) {
        try {
            return info.sending_exception().type().id();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public void send_other(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        endSpan(null);
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void destroy() {
    }

    private void endSpan(Throwable error) {
        OpenTelemetryService openTelemetryService = getOpenTelemetryService();
        if (openTelemetryService != null) {
            openTelemetryService.endDeferredSpan(error);
        }
    }

    private OpenTelemetryService getOpenTelemetryService() {
        ServiceHandle<OpenTelemetryService> handle = serviceLocator.getServiceHandle(OpenTelemetryService.class);
        if (handle != null && handle.isActive()) {
            return handle.getService();
        }
        return null;
    }
}
