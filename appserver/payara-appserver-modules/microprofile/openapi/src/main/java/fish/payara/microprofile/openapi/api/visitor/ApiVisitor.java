/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2025] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.api.visitor;

import org.glassfish.hk2.classmodel.reflect.AnnotatedElement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.MethodModel;

public interface ApiVisitor {

    /**
     * Generic representation of each of these functions.
     */
    @FunctionalInterface
    interface VisitorFunction<AnnotationModel, E extends AnnotatedElement> {
        void apply(AnnotationModel annotation, E element, ApiContext context);
    }

    // JAX-RS method types

    void visitGET(AnnotationModel get, MethodModel element, ApiContext context);

    void visitPOST(AnnotationModel post, MethodModel element, ApiContext context);

    void visitPUT(AnnotationModel put, MethodModel element, ApiContext context);

    void visitDELETE(AnnotationModel delete, MethodModel element, ApiContext context);

    void visitHEAD(AnnotationModel head, MethodModel element, ApiContext context);

    void visitOPTIONS(AnnotationModel options, MethodModel element, ApiContext context);

    void visitPATCH(AnnotationModel patch, MethodModel element, ApiContext context);

    // JAX-RS data types

    void visitProduces(AnnotationModel produces, AnnotatedElement element, ApiContext context);

    void visitConsumes(AnnotationModel produces, AnnotatedElement element, ApiContext context);

    // JAX-RS parameter types

    void visitQueryParam(AnnotationModel param, AnnotatedElement element, ApiContext context);

    void visitPathParam(AnnotationModel param, AnnotatedElement element, ApiContext context);

    void visitFormParam(AnnotationModel param, AnnotatedElement element, ApiContext context);

    void visitHeaderParam(AnnotationModel param, AnnotatedElement element, ApiContext context);

    void visitCookieParam(AnnotationModel param, AnnotatedElement element, ApiContext context);

    // Bean Validation annotations

    void visitNotEmpty(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitNotBlank(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitSize(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitDecimalMax(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitDecimalMin(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitMax(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitMin(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitNegative(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitNegativeOrZero(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitPositive(AnnotationModel param, AnnotatedElement element, ApiContext context);
    void visitPositiveOrZero(AnnotationModel param, AnnotatedElement element, ApiContext context);

    // OpenAPI annotations

    void visitOpenAPI(AnnotationModel definition, AnnotatedElement element, ApiContext context);

    void visitSchema(AnnotationModel schema, AnnotatedElement element, ApiContext context);

    void visitExtension(AnnotationModel extension, AnnotatedElement element, ApiContext context);

    void visitExtensions(AnnotationModel extensions, AnnotatedElement element, ApiContext context);

    void visitOperation(AnnotationModel operation, AnnotatedElement element, ApiContext context);

    void visitCallback(AnnotationModel callback, AnnotatedElement element, ApiContext context);

    void visitCallbacks(AnnotationModel callbacks, AnnotatedElement element, ApiContext context);

    void visitRequestBody(AnnotationModel requestBody, AnnotatedElement element, ApiContext context);

    void visitRequestBodySchema(AnnotationModel requestBodySchema, AnnotatedElement element, ApiContext context);

    void visitAPIResponse(AnnotationModel apiResponse, AnnotatedElement element, ApiContext context);

    void visitAPIResponses(AnnotationModel apiResponses, AnnotatedElement element, ApiContext context);

    void visitAPIResponseSchema(AnnotationModel apiResponseSchema, AnnotatedElement element, ApiContext context);

    void visitParameter(AnnotationModel parameter, AnnotatedElement element, ApiContext context);

    void visitParameters(AnnotationModel parameters, AnnotatedElement element, ApiContext context);

    void visitExternalDocumentation(AnnotationModel externalDocs, AnnotatedElement element, ApiContext context);

    void visitServer(AnnotationModel server, AnnotatedElement element, ApiContext context);

    void visitServers(AnnotationModel servers, AnnotatedElement element, ApiContext context);

    void visitTag(AnnotationModel tag, AnnotatedElement element, ApiContext context);

    void visitTags(AnnotationModel tags, AnnotatedElement element, ApiContext context);

    void visitSecurityScheme(AnnotationModel securityScheme, AnnotatedElement element, ApiContext context);

    void visitSecuritySchemes(AnnotationModel securitySchemes, AnnotatedElement element, ApiContext context);

    void visitSecurityRequirement(AnnotationModel securityRequirement, AnnotatedElement element,
            ApiContext context);

    void visitSecurityRequirements(AnnotationModel securityRequirements, AnnotatedElement element,
            ApiContext context);

    void visitSecurityRequirementSet(AnnotationModel securityRequirement, AnnotatedElement element,
                                  ApiContext context);

    void visitSecurityRequirementSets(AnnotationModel securityRequirements, AnnotatedElement element,
                                   ApiContext context);
}