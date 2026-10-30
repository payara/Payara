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
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

@OpenAPIDefinition(
        info = @Info(title = "title", version = "version"),
        components = @Components(
                callbacks = {
                    @Callback(name = "name1", ref = "ref1"),
                    @Callback(name = "name2", callbackUrlExpression = "http://callbackUrlExpression.com",
                            operations
                            = @CallbackOperation(method = "GET", summary = "summary",
                                    responses = {
                                        @APIResponse(
                                                responseCode = "200",
                                                description = "successful operation",
                                                content = @Content(
                                                        mediaType = "applictaion/json",
                                                        schema = @Schema(
                                                                type = SchemaType.ARRAY,
                                                                implementation = String.class
                                                        )
                                                )
                                        )}))
                }
        ))
@Path("/callbacks")
@Produces(MediaType.APPLICATION_JSON)
public class CallbacksTest extends OpenApiApplicationTest {

    @Test
    public void callbackReference() {
        JsonNode callback = path(getOpenAPIJson(), "components.callbacks.name1");
        assertEquals(1, callback.size());
        assertEquals("#/components/callbacks/ref1", callback.get("$ref").textValue());
    }

    @Test
    public void callbackObject() {
        JsonNode callback = path(getOpenAPIJson(), "components.callbacks.name2");
        assertNull(callback.get("$ref"));
        JsonNode operations = callback.get("http://callbackUrlExpression.com");
        assertNotNull(operations);
        JsonNode getOperation = operations.get("get");
        assertNotNull(getOperation);
        assertEquals("summary", getOperation.get("summary").textValue());
        JsonNode responses = getOperation.get("responses");
        assertNotNull(responses);
        JsonNode response200 = responses.get("200");
        assertNotNull(response200);
        assertEquals("successful operation", response200.get("description").textValue());
        assertNotNull(response200.get("content"));
    }
}
