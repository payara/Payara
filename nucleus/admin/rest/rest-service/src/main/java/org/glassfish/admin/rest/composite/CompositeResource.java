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
package org.glassfish.admin.rest.composite;

import com.sun.enterprise.v3.common.ActionReporter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestResource;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.security.common.Group;
import org.jvnet.hk2.component.Habitat;

/**
 *
 * @author jdlee
 */
public abstract class CompositeResource implements RestResource {

    @Context
    protected UriInfo uriInfo;
    @Inject
    protected Ref<Request> requestRef;
    @Inject
    protected Habitat habitat;
    protected Subject subject;
    private String authenticatedUser;
    protected CompositeUtil compositeUtil = CompositeUtil.instance();

    protected Subject getSubject() {
        if (subject == null) {
            Request req = requestRef.get();
            subject = (Subject) req.getAttribute(Constants.REQ_ATTR_SUBJECT);
        }
        
        return subject;
    }

    protected String getAuthenticatedUser() {
        if (authenticatedUser == null) {
            Subject s = getSubject();
            if (s != null) {
                for (Principal p : s.getPrincipals()) {
                    // TODO: This will be replaced with a proper check once the security team delivers the API
                    if (!(p instanceof Group)) {
                        authenticatedUser = p.getName();
                    }
                }
            }
        }

        return authenticatedUser;
    }

    @OPTIONS
    public String options() throws JSONException {
        List<MethodInfo> info = new ArrayList<MethodInfo>();
        for (Method method : getClass().getMethods()) {
            for (Annotation a : method.getAnnotations()) {
                String httpMethod = null;
                if (GET.class.isAssignableFrom(a.getClass())) {
                    httpMethod = "GET";
                } else if (POST.class.isAssignableFrom(a.getClass())) {
                    httpMethod = "POST";
                } else if (DELETE.class.isAssignableFrom(a.getClass())) {
                    httpMethod = "DELETE";
                }
                if (httpMethod != null) {
                    info.add(new MethodInfo(method.getName(), httpMethod, method.getReturnType().getSimpleName(), method.getParameterTypes()));
                }
            }
        }

        JSONObject metadata = new JSONObject();
        JSONArray methods = new JSONArray();
        for (MethodInfo m : info) {
            methods.put(m.toJson());
        }
        metadata.put("methods", methods);


        return metadata.toString(getFormattingIndentLevel());
    }

    private static class MethodInfo {

        String name;
        String httpMethod;
        String returnType;
        String[] parameterTypes;

        public MethodInfo(String name, String httpMethod, String returnType, Class<?>[] parameters) {
            this.name = name;
            this.httpMethod = httpMethod;
            this.returnType = returnType;
            if (parameters != null) {
                parameterTypes = new String[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    parameterTypes[i] = parameters[i].getName();
                }

            }
        }

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
//            o.put("name", name);
            o.put("httpMethod", httpMethod);
            o.put("returnType", returnType);
            JSONArray params = new JSONArray();
            if (parameterTypes != null) {
                for (String param : parameterTypes) {
                    params.put(param);
                }
            }
            o.put("parameters", params);

            return o;
        }
    }

    protected int getFormattingIndentLevel() {
        RestConfig rg = ResourceUtil.getRestConfig(habitat);
        if (rg == null) {
            return -1;
        } else {
            return Integer.parseInt(rg.getIndentLevel());
        }

    }

    /**
     * Execute an <code>AdminCommand</code> with no parameters
     * @param command
     * @return
     */
    protected ActionReporter executeCommand(String command) {
        return executeCommand(command, new ParameterMap());
    }

    /**
     * Execute an <code>AdminCommand</code> with the specified parameters.
     * @param command
     * @param parameters
     * @return
     */
    protected ActionReporter executeCommand(String command, ParameterMap parameters) {
        RestActionReporter ar = ResourceUtil.runCommand(command, parameters,
                Globals.getDefaultHabitat(), "", getSubject()); //TODO The last parameter is resultType and is not used. Refactor the called method to remove it
        if (ar.getActionExitCode().equals(ExitCode.FAILURE)) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).
                    entity(ar.getTopMessagePart().getMessage()).
                    build());
        }
        return ar;
    }

}
