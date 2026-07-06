/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.test.app.application;

import com.fasterxml.jackson.databind.JsonNode;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.util.JsonUtils;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * Test to verify that using {@code hidden} does not cause errors as suggested
 * by PAYARA-3259.
 */
@Path("/example")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HiddenTest extends OpenApiApplicationTest {

    @Schema
    public class User {

        private Long id;

        private String email;

        @Schema(hidden = true)
        private String passwordHash;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }
    
    class Teacher extends User {
        
        private int secretPin;

        public int getSecretPin() {
            return secretPin;
        }

        public void setSecretPin(int secretPin) {
            this.secretPin = secretPin;
        }
        
        
        
    }

    @GET
    @Path("/{name}")
    @Operation(hidden = true)
    public Response hello(@PathParam("name") String name) {
        JsonObject message = Json.createObjectBuilder()
                .add("message", "hello" + name)
                .build();
        return Response.ok(message).build();
    }

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(
            responseCode = "400",
            description = "NotFound",
            content = @Content(
                    schema = @Schema(implementation = User.class)
            )
    )
    @APIResponse(
            responseCode = "900",
            description = "Invalid",
            content = @Content(
                    schema = @Schema(implementation = User.class, hidden = true)
            )
    )
    public User sayHello(
            @Parameter(
                    name = "userId",
                    description = "ID of user",
                    required = true
            )
            @PathParam("userId") Long userId,
            @Parameter(
                    name = "password",
                    description = "Password of user",
                    required = true,
                    hidden = true
            )
            @PathParam("password") String password) {
        return new User();
    }

    @Test
    public void hiddenOperationDoesNotCauseErrors() {
        assertNotNull(path(getOpenAPIJson(), "paths./test/example/{name}"));
    }

    @Test
    public void hiddenSchemaPropertyDoesNotCauseErrors() {
        System.out.println(getOpenAPIJson());
        JsonNode properties = JsonUtils.path(getOpenAPIJson(), "components.schemas.User.properties");
        assertEquals("number", properties.get("id").get("type").textValue());
        assertEquals("string", properties.get("email").get("type").textValue());
        assertNull(properties.get("passwordHash"));
    }
    
    @Test
    public void hiddenSchemaDoesNotCauseErrors() {
        JsonNode responses = JsonUtils.path(getOpenAPIJson(), "paths./test/example/{userId}.get.responses");
        assertNotNull(responses.get("400").get("content").get("application/json").get("schema"));
        assertNull(responses.get("900").get("content").get("application/json").get("schema"));
    }
    
    
    @Test
    public void hiddenParamDoesNotCauseErrors() {
        JsonNode parameters = JsonUtils.path(getOpenAPIJson(), "paths./test/example/{userId}.get.parameters");
        assertEquals(1, parameters.size());
    }
}
