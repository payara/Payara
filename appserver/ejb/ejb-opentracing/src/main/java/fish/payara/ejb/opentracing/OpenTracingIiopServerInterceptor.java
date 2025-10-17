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
package fish.payara.ejb.opentracing;

import fish.payara.opentracing.OpenTracingService;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.logging.Logger;

import static fish.payara.ejb.opentracing.OpenTracingIiopInterceptorFactory.OPENTRACING_IIOP_ID;
import static fish.payara.opentracing.OpenTracingService.PAYARA_CORBA_RMI_TRACER_NAME;
import fish.payara.opentracing.ScopeManager;
import io.opentracing.Span;

/**
 * IIOP Server Interceptor for propagating OpenTracing SpanContext to Payara Server.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class OpenTracingIiopServerInterceptor extends LocalObject implements ServerRequestInterceptor {
    private static final Logger LOGGER = Logger.getLogger(OpenTracingIiopServerInterceptor.class.getName());

    private OpenTracingService openTracingService;
    private Tracer tracer;

    // Let's just guess that single request remain on the single thread as is not multiplexed
    private ThreadLocal<Scope> currentScope = new ThreadLocal<>();
    private ThreadLocal<Span> currentSpan = new ThreadLocal<>();

    public OpenTracingIiopServerInterceptor(OpenTracingService openTracingService) {
        this.openTracingService = openTracingService;

        // Null check for opentracing should have been done by factory
        if (openTracingService.isEnabled()) {
            this.tracer = openTracingService.getTracer(PAYARA_CORBA_RMI_TRACER_NAME);
        }
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {
        // Noop
        return;
    }

    @Override
    public void receive_request(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        // Double check we have a tracer
        if (!tracerAvailable()) {
            return;
        }

        ServiceContext serviceContext = serverRequestInfo.get_request_service_context(OPENTRACING_IIOP_ID);
        if (serviceContext == null) {
            return;
        }

        OpenTracingIiopTextMap openTracingIiopTextMap = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serviceContext.context_data);
             ObjectInput in = new OpenTracingIiopObjectInputStream(bis)) {
            openTracingIiopTextMap = (OpenTracingIiopTextMap) in.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new ForwardRequest(exception.getMessage(), serverRequestInfo);
        }

        Tracer.SpanBuilder spanBuilder = tracer.buildSpan("rmi")
                .withTag(Tags.COMPONENT.getKey(), "ejb");

        if (openTracingIiopTextMap != null) {
            SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, openTracingIiopTextMap);

            // Add the propagated span as a parent
            spanBuilder.asChildOf(spanContext);
        }


        Scope previousScope = currentScope.get();
        if (previousScope != null) {
            LOGGER.warning("Overlapping traced RMI operations identified, please report");
        }
        // Start the span and mark it as active
        Span currentSpan = spanBuilder.start();
        currentScope.set(tracer.activateSpan(currentSpan));
        this.currentSpan.set(currentSpan);
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

    private void closeScope() {
        if (tracer == null) {
            return;
        }
        // Double check we have a tracer
        Scope scope = currentScope.get();
        if (scope != null) {
            scope.close();
            currentScope.remove();
        }
        Span activeSpan = currentSpan.get();
        if (activeSpan != null) {
            activeSpan.finish();
            currentSpan.remove();
        }
    }

    private boolean tracerAvailable() {
        if (tracer == null) {
            // Null check for opentracing should have been done by factory
            if (!openTracingService.isEnabled()) {
                return false;
            }
            this.tracer = openTracingService.getTracer(PAYARA_CORBA_RMI_TRACER_NAME);
            if (tracer == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void destroy() {

    }
}
