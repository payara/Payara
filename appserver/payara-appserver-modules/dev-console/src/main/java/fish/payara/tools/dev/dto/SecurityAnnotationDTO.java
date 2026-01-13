/*
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.tools.dev.dto;

import fish.payara.tools.dev.model.AuditInfo;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class SecurityAnnotationDTO {

    private String className;
    private String methodName;

    private List<String> httpMethods;
    private List<String> paths;
    private List<String> produces;
    private List<String> security;

    public SecurityAnnotationDTO(Object element, AuditInfo entry) {
        extractClassAndMethod(element);
        extractAnnotations(entry);
    }

    private void extractClassAndMethod(Object element) {
        try {
            if (element instanceof AnnotatedMethod<?> am) {
                Method m = am.getJavaMember();
                this.className = m.getDeclaringClass().getName();
                this.methodName = m.getName();
            }
            else if (element instanceof AnnotatedType<?> at) {
                this.className = at.getJavaClass().getName();
                this.methodName = "(class-level)";
            }
        } catch (Exception e) {
            this.className = "Unknown";
            this.methodName = "Unknown";
        }
    }

    private void extractAnnotations(AuditInfo entry) {

        // SECURITY ANNOTATIONS
        this.security = entry.getSecurityAnnotations().stream()
                .map(a -> a.annotationType().getSimpleName())
                .collect(Collectors.toList());

        // HTTP METHOD ANNOTATIONS (like @GET, @POST, @PUT, ...)
        this.httpMethods = entry.getHttpMethodAnnotations().stream()
                .map(a -> a.annotationType().getSimpleName())
                .collect(Collectors.toList());

        // @Path
        this.paths = entry.getPathAnnotations().stream()
                .map(a -> ((Path) a).value())
                .collect(Collectors.toList());

        // @Produces
        this.produces = entry.getProducesAnnotations().stream()
                .flatMap(a -> Arrays.stream(((Produces) a).value()))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------
    // GETTERS (required for JSON serialization)
    // ---------------------------------------------

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getHttpMethods() {
        return httpMethods;
    }

    public List<String> getPaths() {
        return paths;
    }

    public List<String> getProduces() {
        return produces;
    }

    public List<String> getSecurity() {
        return security;
    }

    @Override
    public String toString() {
        return "SecurityAnnotationDTO{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", httpMethods=" + httpMethods +
                ", paths=" + paths +
                ", produces=" + produces +
                ", security=" + security +
                '}';
    }
}
