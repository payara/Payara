/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.api.visitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.Servers;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;

public interface ApiVisitor {

    /**
     * Generic representation of each of these functions.
     */
    @FunctionalInterface
    interface VisitorFunction<A extends Annotation, E extends AnnotatedElement> {
        void apply(A annotation, E element, ApiContext context);
    }

    // JAX-RS annotations

    void visitGET(GET get, Method element, ApiContext context);

    void visitPOST(POST post, Method element, ApiContext context);

    void visitPUT(PUT put, Method element, ApiContext context);

    void visitDELETE(DELETE delete, Method element, ApiContext context);

    void visitHEAD(HEAD head, Method element, ApiContext context);

    void visitOPTIONS(OPTIONS options, Method element, ApiContext context);

    void visitPATCH(PATCH patch, Method element, ApiContext context);

    void visitProduces(Produces produces, AnnotatedElement element, ApiContext context);

    void visitConsumes(Consumes produces, AnnotatedElement element, ApiContext context);

    void visitQueryParam(QueryParam param, AnnotatedElement element, ApiContext context);

    void visitPathParam(PathParam param, AnnotatedElement element, ApiContext context);

    void visitFormParam(FormParam param, AnnotatedElement element, ApiContext context);

    void visitHeaderParam(HeaderParam param, AnnotatedElement element, ApiContext context);

    void visitCookieParam(CookieParam param, AnnotatedElement element, ApiContext context);
    
    // OpenAPI annotations

    void visitOpenAPI(OpenAPIDefinition definition, AnnotatedElement element, ApiContext context);

    void visitSchema(Schema schema, AnnotatedElement element, ApiContext context);

    void visitExtension(Extension extension, AnnotatedElement element, ApiContext context);

    void visitOperation(Operation operation, AnnotatedElement element, ApiContext context);

    void visitCallback(Callback callback, AnnotatedElement element, ApiContext context);

    void visitCallbacks(Callbacks callbacks, AnnotatedElement element, ApiContext context);

    void visitRequestBody(RequestBody requestBody, AnnotatedElement element, ApiContext context);

    void visitAPIResponse(APIResponse apiResponse, AnnotatedElement element, ApiContext context);

    void visitAPIResponses(APIResponses apiResponses, AnnotatedElement element, ApiContext context);

    void visitParameter(Parameter parameter, AnnotatedElement element, ApiContext context);

    void visitExternalDocumentation(ExternalDocumentation externalDocs, AnnotatedElement element, ApiContext context);

    void visitServer(Server server, AnnotatedElement element, ApiContext context);

    void visitServers(Servers servers, AnnotatedElement element, ApiContext context);

    void visitTag(Tag tag, AnnotatedElement element, ApiContext context);

    void visitTags(Tags tags, AnnotatedElement element, ApiContext context);

    void visitSecurityScheme(SecurityScheme securityScheme, AnnotatedElement element, ApiContext context);

    void visitSecuritySchemes(SecuritySchemes securitySchemes, AnnotatedElement element, ApiContext context);

    void visitSecurityRequirement(SecurityRequirement securityRequirement, AnnotatedElement element,
            ApiContext context);

    void visitSecurityRequirements(SecurityRequirements securityRequirements, AnnotatedElement element,
            ApiContext context);

}