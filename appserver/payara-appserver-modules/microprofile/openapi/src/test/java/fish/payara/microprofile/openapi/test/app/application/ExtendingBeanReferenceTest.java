/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

/**
 * See issue https://github.com/payara/Payara/issues/3937
 *  
 * @author Jan Bernitt
 */
@Path("/testopenapi")
public class ExtendingBeanReferenceTest extends OpenApiApplicationTest {

    public static class FooResponseBase {
        private String fooStringFromBase;

        public String getFooStringFromBase() {
            return fooStringFromBase;
        }

        public void setFooStringFromBase(String fooStringFromBase) {
            this.fooStringFromBase = fooStringFromBase;
        }
    }

    @Schema(name="FooResponse", description="Example response")
    public static class FooResponse extends FooResponseBase {

        @Schema(required = true)
        private String fooString;

        public String getFooString() {
            return fooString;
        }

        public void setFooString(String fooString) {
            this.fooString = fooString;
        }
    }

    @GET
    @APIResponse(responseCode = "200", description = "Description of the example service response.",
        content = @Content(mediaType = "application/json",
        schema = @Schema(implementation = FooResponse.class)))
    public Response getFooWithImplementation() {
        return Response.ok("foo").build();
    }

    @POST
    @APIResponse(responseCode = "200", description = "Description of the example service response.",
        content = @Content(mediaType = "application/json",
        schema = @Schema(allOf = {FooResponse.class})))
    public Response getFooWithAllOf() {
        return Response.ok("foo").build();
    }

    @Test
    public void extendedClassIsReferenced() {
        ObjectNode document = getOpenAPIJson();
        assertEquals("#/components/schemas/FooResponse",
                path(document, "components.schemas.FooResponse.allOf.[0].$ref"));
        assertEquals("#/components/schemas/FooResponse",
                path(document, "paths./test/testopenapi.get.responses.'200'.content.application/json.schema.allOf.[0].$ref"));
    }
}
