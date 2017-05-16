/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates.
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
package fish.payara.nucleus.requesttracing.api.extension;

import fish.payara.nucleus.requesttracing.api.RequestTracingCdiInterceptor;
import fish.payara.nucleus.requesttracing.api.Traced;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * @author mertcaliskan
 */
public class RequesttracingCoreCDIExtension implements Extension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addInterceptorBinding(Traced.class);
        AnnotatedType<RequestTracingCdiInterceptor> cpat = bm.createAnnotatedType(RequestTracingCdiInterceptor.class);
        bbd.addAnnotatedType(cpat);
    }
}