/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.appserver.rest.endpoints;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

/**
 * A model representing the endpoint specified by a method contains a
 * {@link RestEndpointModel#path path} and a
 * {@link RestEndpointModel#requestMethod request method}
 *
 * @author Matt Gill
 */
public class RestEndpointModel {

    /**
     * The path of the endpoint
     */
    private String path;

    /**
     * The request method associated with the endpoint
     */
    private String requestMethod;

    private RestEndpointModel(String path, String requestMethod) {
        this.path = path;
        this.requestMethod = requestMethod;
    }

    /**
     * Gets the endpoint of a given method, relative to the jersey application
     * root. Will return an endpoint path with a leading slash but no trailing
     * slashes (e.g. /test/path/{name})
     *
     * @param method the method to parse
     * @return a {@link RestEndpointModel} specific to the method
     */
    public static RestEndpointModel generateFromMethod(Method method) {
        // Get the request method off the bat
        String requestMethod = getRequestMethodAnnotation(method);
        if (requestMethod == null) {
            return null;
        }
        
        // Get the class the method is in
        Class enclosingClass = method.getDeclaringClass();

        // Get the path associated with the class
        String parentPath = getPathAnnotation(enclosingClass);

        // Get the path associated with the method
        String childPath = getPathAnnotation(method);

        String path = parentPath + childPath;
        if (childPath.equals("/")) {
            path = parentPath;
        }

        return new RestEndpointModel(path, requestMethod);
    }

    /**
     * Gets the path associated with an element. Removes all trailing slashes.
     *
     * @param element the annotated element
     * @return the path of the element
     */
    private static String getPathAnnotation(AnnotatedElement element) {
        Path annotation = element.getAnnotation(Path.class);

        if (annotation == null || annotation.value().isEmpty()) {
            return "/";
        }

        return "/" + annotation.value().replaceAll("^/", "").replaceAll("/$", "");
    }

    /**
     * Reads request method annotations to determine the request method.
     *
     * @param element the annotated element
     * @return the correct {@link HttpMethod}
     */
    private static String getRequestMethodAnnotation(AnnotatedElement element) {
        GET get = element.getAnnotation(GET.class);
        if (get != null) {
            return HttpMethod.GET;
        }
        POST post = element.getAnnotation(POST.class);
        if (post != null) {
            return HttpMethod.POST;
        }
        PUT put = element.getAnnotation(PUT.class);
        if (put != null) {
            return HttpMethod.PUT;
        }
        DELETE delete = element.getAnnotation(DELETE.class);
        if (delete != null) {
            return HttpMethod.DELETE;
        }
        HEAD head = element.getAnnotation(HEAD.class);
        if (head != null) {
            return HttpMethod.HEAD;
        }
        OPTIONS options = element.getAnnotation(OPTIONS.class);
        if (options != null) {
            return HttpMethod.OPTIONS;
        }
        return null;
    }

    /**
     * @return the path of the endpoint.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the {@link HttpMethod} of the endpoint.
     */
    public String getRequestMethod() {
        return requestMethod;
    }

}
