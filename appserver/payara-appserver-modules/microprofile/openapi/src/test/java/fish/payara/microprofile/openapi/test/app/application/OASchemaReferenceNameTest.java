/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.util.JsonUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Referenced components in the "Paths" section of the generated OpenAPI definition should be primarily named as defined
 * in the the 'Schema.name' field of the {@link Schema} annotation. Simple class name was used all the time instead.
 *  {@link https://github.com/payara/Payara/issues/4490}
 */
@Path("/schema-ref-test")
public class OASchemaReferenceNameTest extends OpenApiApplicationTest {

    @GET
    @Path("/user")
    @APIResponse(description = "get user")
    public UserDto getUser() {
        return new UserDto();
    }

    @GET
    @Path("/address")
    @APIResponse(description = "get address")
    public AddressDto getAddress() {
        return new AddressDto();
    }

    @Test
    public void schemaComponentReferenceNaming() {
        final JsonNode userNode = JsonUtils.path(getOpenAPIJson(),
                "paths./test/schema-ref-test/user.get.responses.default.content.*/*.schema.$ref");
        assertNotNull(userNode);
        final String expectedUserComponentName = String.format("#/components/schemas/%s", UserDto.SCHEMA_NAME);
        assertEquals(expectedUserComponentName, userNode.asText());

        // If the component name inside the Schema annotation is empty, simple class name is taken.
        // Reference naming should follow the same pattern
        final JsonNode addressNode = JsonUtils.path(getOpenAPIJson(),
                "paths./test/schema-ref-test/address.get.responses.default.content.*/*.schema.$ref");
        assertNotNull(addressNode);
        final String expectedAddressComponentName = String.format("#/components/schemas/%s", AddressDto.class.getSimpleName());
        assertEquals(expectedAddressComponentName, addressNode.asText());
    }

    @Schema(name = UserDto.SCHEMA_NAME)
    public static class UserDto {
        private static final String SCHEMA_NAME = "User";

        @Schema
        private AddressDto addressDto;
    }

    @Schema
    public static class AddressDto {
    }
}
