/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package org.glassfish.admin.rest.composite.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.CompositeResource;

/**
 *
 * @author jdlee
 */
public class RestResourceMetadata {
    private Map<String, RestMethodMetadata> resourceMethods = new HashMap<String, RestMethodMetadata>();
    private List<String> subResources = new ArrayList<String>();
    private CompositeResource context;

    public RestResourceMetadata(CompositeResource context) {
        this.context = context;
        processClass();
    }

    public Map<String, RestMethodMetadata> getResourceMethods() {
        return resourceMethods;
    }

    public void setResourceMethods(Map<String, RestMethodMetadata> resourceMethods) {
        this.resourceMethods = resourceMethods;
    }

    public List<String> getSubResources() {
        return subResources;
    }

    public void setSubResources(List<String> subResources) {
        this.subResources = subResources;
    }

    private void processClass() {
        for (Method m : context.getClass().getDeclaredMethods()) {
            Annotation designator = getMethodDesignator(m);

            if (designator != null) {
                final String httpMethod = designator.annotationType().getSimpleName();
                RestMethodMetadata rmm = new RestMethodMetadata(context, m, designator);
                if (resourceMethods.containsKey(httpMethod)) {
                    throw new RuntimeException("Multiple " + httpMethod + " methods found on resource: " +
                            context.getClass().getName());
                }
                resourceMethods.put(httpMethod, rmm);
            }

            final Path path = m.getAnnotation(Path.class);
            if (path != null) {
                subResources.add(context.getUriInfo().getAbsolutePathBuilder().build().toASCIIString() + "/" + path.value());
            }
        }
    }

    private Annotation getMethodDesignator(Method method) {
        Annotation a = method.getAnnotation(GET.class);
        if (a == null) {
            a = method.getAnnotation(POST.class);
            if (a == null) {
                a = method.getAnnotation(DELETE.class);
                if (a == null) {
                    a = method.getAnnotation(OPTIONS.class);
                }
            }
        }

        return a;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();

        if (!resourceMethods.isEmpty()) {
            final JSONObject methods = new JSONObject();
            for (Map.Entry<String, RestMethodMetadata> entry : resourceMethods.entrySet()) {
                methods.put(entry.getKey(), entry.getValue().toJson());
            }

            o.put("resourceMethods", methods);
        }

        if (!subResources.isEmpty()) {
            o.put("subResources", subResources);
        }

        return o;
    }
}
