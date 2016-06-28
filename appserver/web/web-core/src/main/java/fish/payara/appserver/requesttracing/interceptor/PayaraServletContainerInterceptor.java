/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.requesttracing.interceptor;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import fish.payara.nucleus.requesttracing.domain.ServletRequestEvent;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.web.valve.ServletContainerInterceptor;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @author mertcaliskan
 *
 * Interceptor that will be invoked for before and after of all Servlet calls. It's the main intercepting point for the
 * whole request tracing mechanism.
 */
@Service(name = "rt-interceptor-servlet")
@RunLevel(StartupRunLevel.VAL)
public class PayaraServletContainerInterceptor implements ServletContainerInterceptor {

    private static final String EVENT_KEY = "PayaraRequestEvent";

    @Inject
    private RequestTracingService service;

    @Inject
    private Domain domain;

    @Inject
    private Server server;

    @PostConstruct
    void postConstruct() {
        System.out.println("class: " + this.getClass().getName()  + " - loader:" + this.getClass().getClassLoader().getClass().getName());
    }

    public void preInvoke(Request request, Response response) {
        if (!service.isRequestTracingEnabled()) {
            return;
        }
        ServletRequestEvent requestEvent = new ServletRequestEvent();
        requestEvent.setServer(server.getName());
        requestEvent.setDomain(domain.getName());
        requestEvent.setUrl(request.getRequestURL().toString());
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            List<String> headers = Collections.list(request.getHeaders(headerName));
            requestEvent.getHeaders().put(headerName, headers);
        }
        requestEvent.setFormMethod(request.getMethod());
        requestEvent.setEventType(EventType.SERVLET);

        request.setNote(EVENT_KEY, requestEvent);
    }

    public void postInvoke(Request request, Response response) {
        if (!service.isRequestTracingEnabled()) {
            return;
        }
        RequestEvent requestEvent = (RequestEvent) request.getNote(EVENT_KEY);
        if (requestEvent != null) {
            requestEvent.setElapsedTime(System.currentTimeMillis() - requestEvent.getTimestamp());
            service.traceRequestEvent(requestEvent);
        }
    }
}
