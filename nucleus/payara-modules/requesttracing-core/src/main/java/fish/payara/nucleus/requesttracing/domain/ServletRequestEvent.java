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
package fish.payara.nucleus.requesttracing.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mertcaliskan
 *
 * Stores servlet specific event values
 * {@link fish.payara.nucleus.requesttracing.interceptor.PayaraServletContainerInterceptor}.
 */
public class ServletRequestEvent extends RequestEvent {

    private String url;
    private String formMethod;
    private Map<String, List<String>> headers = new HashMap<String, List<String>>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFormMethod() {
        return formMethod;
    }

    public void setFormMethod(String formMethod) {
        this.formMethod = formMethod;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "ServletRequestEvent{" +
                "url='" + url + '\'' +
                ", formMethod='" + formMethod + '\'' +
                ", headers=" + headers +
                ", " + super.toString() +
                "} ";
    }
}
