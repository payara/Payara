/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fish.payara.ejb.opentracing.OpenTracingIiopInterceptorFactory.OPENTRACING_IIOP_ID;
import static fish.payara.opentracing.OpenTracingService.PAYARA_CORBA_RMI_TRACER_NAME;

/**
 * IIOP Client Interceptor for propagating OpenTracing SpanContext to Payara Server.
 *
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
public class OpenTracingIiopClientInterceptor extends LocalObject implements ClientRequestInterceptor {

    private OpenTracingService openTracingService;
    private Tracer tracer;

    public OpenTracingIiopClientInterceptor(OpenTracingService openTracingService) {
        this.openTracingService = openTracingService;
        // Register global tracer if it hasn't been already
        this.tracer = GlobalTracer.get();
        // Null check for opentracing should have been done by factory
        GlobalTracer.registerIfAbsent(() -> openTracingService.getTracer(PAYARA_CORBA_RMI_TRACER_NAME));
    }

    @Override
    public void send_request(ClientRequestInfo clientRequestInfo) throws ForwardRequest {
        // Double check we have a tracer and try and get one again if we don't
        GlobalTracer.registerIfAbsent(() -> openTracingService.getTracer(PAYARA_CORBA_RMI_TRACER_NAME));
        if (!GlobalTracer.isRegistered()) {
            return;
        }

        // Check if there's an active span
        Span activeSpan = tracer.activeSpan();
        if (activeSpan == null) {
            // Nothing to propagate, so simply return
            return;
        }

        // Inject active span context for propagation
        OpenTracingIiopTextMap textMap = new OpenTracingIiopTextMap();
        tracer.inject(activeSpan.context(), Format.Builtin.TEXT_MAP, textMap);

        // Convert text map to bytes and attach to service context
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(textMap);
            out.flush();
            ServiceContext serviceContext = new ServiceContext(OPENTRACING_IIOP_ID, bos.toByteArray());
            clientRequestInfo.add_request_service_context(serviceContext, true);
        } catch (IOException ex) {
            Logger.getLogger(OpenTracingIiopClientInterceptor.class.getName()).log(Level.SEVERE,
                    "Exception caught propagating span context");
        }
    }

    @Override
    public void send_poll(ClientRequestInfo ri) {
        // Noop
        return;
    }

    @Override
    public void receive_reply(ClientRequestInfo ri) {
        // Noop
        return;
    }


    @Override
    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
        // Noop
        return;
    }

    @Override
    public void receive_other(ClientRequestInfo ri) throws ForwardRequest {
        // Noop
        return;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void destroy() {
        // Noop
        return;
    }


}
