/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.requesttracing.api;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import org.glassfish.internal.api.Globals;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

/**
 * @author mertcaliskan
 */
@Interceptor
@Traced
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class RequestTracingCdiInterceptor implements Serializable {

    @AroundInvoke
    public Object traceCdiCall(InvocationContext ctx) throws Exception {
        RequestTracingService requestTracing = Globals.getDefaultHabitat().getService(RequestTracingService.class);
        if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
            RequestEvent requestEvent = new RequestEvent("InterceptedCdiRequest-ENTER");
            requestEvent.addProperty("TargetClass", ctx.getTarget().getClass().getName());
            requestEvent.addProperty("MethodName", ctx.getMethod().getName());
            requestTracing.traceRequestEvent(requestEvent);
        }
        Object proceed = ctx.proceed();
        if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
            RequestEvent requestEvent = new RequestEvent("InterceptedCdiRequest-EXIT");
            requestEvent.addProperty("TargetClass", ctx.getTarget().getClass().getName());
            requestEvent.addProperty("MethodName", ctx.getMethod().getName());
            requestTracing.traceRequestEvent(requestEvent);
        }
        return proceed;
    }
}