/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.junit.Test;

import java.util.List;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@Path("/bean-validation")
public class BeanValidationTest extends OpenApiApplicationTest {

    @POST
    @Path("/notemptystring/{param}")
    public void notEmptyStringTest(@PathParam("param") @NotEmpty String value) {
    }

    @POST
    @Path("/notemptyarray/{param}")
    public void notEmptyArrayTest(@PathParam("param") @NotEmpty List<String> value) {
    }

    @POST
    @Path("/notemptyobject/{param}")
    public void notEmptyObjectTest(@PathParam("param") @NotEmpty Object value) {
    }

    @POST
    @Path("/notblank/{param}")
    public void notBlankArrayTest(@PathParam("param") @NotBlank String value) {
    }

    @POST
    @Path("/sizearray/{param}")
    public void sizeArrayTest(@PathParam("param") @Size(min = 1, max = 3) List<String> value) {
    }

    @POST
    @Path("/sizeobject/{param}")
    public void sizeObjectTest(@PathParam("param") @Size(min = 1, max = 3) Object value) {
    }

    @POST
    @Path("/decimalmax/{param}")
    public void decimalMaxTest(@PathParam("param") @DecimalMax(value = "3", inclusive = false) int value) {
    }

    @POST
    @Path("/decimalmin/{param}")
    public void decimalMinTest(@PathParam("param") @DecimalMin(value = "1", inclusive = false) int value) {
    }

    @POST
    @Path("/max/{param}")
    public void maxTest(@PathParam("param") @Max(value = 4) int value) {
    }

    @POST
    @Path("/min/{param}")
    public void minTest(@PathParam("param") @Min(value = 2) int value) {
    }

    @POST
    @Path("/negative/{param}")
    public void negativeTest(@PathParam("param") @Negative int value) {
    }

    @POST
    @Path("/negativeorzero/{param}")
    public void negativeOrZeroTest(@PathParam("param") @NegativeOrZero int value) {
    }

    @POST
    @Path("/positive/{param}")
    public void positiveTest(@PathParam("param") @Positive int value) {
    }

    @POST
    @Path("/positiveorzero/{param}")
    public void positiveOrZeroTest(@PathParam("param") @PositiveOrZero int value) {
    }

    @POST
    public void noPathParam(@Valid @NotEmpty List<String> myList) {
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    public void getNoPathParam(@NotEmpty List<String> myList) {
    }

    @PUT
    @Consumes(MediaType.WILDCARD)
    public void putNoPathParam(@Valid @NotEmpty List<String> myList) {
    }

    @POST
    @Path("/positiveorzeroquery")
    public void positiveOrZeroQueryTest(@QueryParam("param") @PositiveOrZero int value) {
    }

    @POST
    @Path("/positiveorzeromultiplequery")
    public void positiveOrZeroMultipleQueryTest(@QueryParam("param") @PositiveOrZero int value, @QueryParam("param2") @PositiveOrZero int value2) {
    }

    @POST
    @Path("/positiveorzerowildcard")
    @Consumes(MediaType.WILDCARD)
    public void positiveOrZeroWildcardQueryTest(@PositiveOrZero int value, @QueryParam("param") @PositiveOrZero int value2) {
    }

    @DELETE
    public void delete(@Valid @NotEmpty List<String> myList) {
    }

    @POST
    @Path("/notemptyheader")
    public void notEmptyHeaderTest(@HeaderParam("X-Test") @NotEmpty String header) {}

    @Test
    public void notEmptyTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/notemptystring/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());
        List<String> result = link.get("parameters").findValuesAsText("minLength");

        assertEquals(1, result.size());
        assertTrue(result.contains("1"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/notemptyarray/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());
        result = link.get("parameters").findValuesAsText("minItems");

        assertEquals(1, result.size());
        assertTrue(result.contains("1"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/notemptyobject/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());
        result = link.get("parameters").findValuesAsText("minProperties");

        assertEquals(1, result.size());
        assertTrue(result.contains("1"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/notemptyheader.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());
        result = link.get("parameters").findValuesAsText("minLength");

        assertEquals(1, result.size());
        assertTrue(result.contains("1"));
        assertEquals("X-Test", link.get("parameters").get(0).get("name").asText());
    }

    @Test
    public void notBlankTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/notblank/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());
        List<String> result = link.get("parameters").findValuesAsText("pattern");

        assertEquals(1, result.size());
        assertTrue(result.contains("\\S"));
    }

    @Test
    public void sizeTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/sizearray/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        List<String> result = link.get("parameters").findValuesAsText("minItems");
        assertEquals(1, result.size());
        assertTrue(result.contains("1"));

        result = link.get("parameters").findValuesAsText("maxItems");
        assertEquals(1, result.size());
        assertTrue(result.contains("3"));


        link = path(getOpenAPIJson(), "paths./test/bean-validation/sizeobject/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        result = link.get("parameters").findValuesAsText("minProperties");
        assertEquals(1, result.size());
        assertTrue(result.contains("1"));

        result = link.get("parameters").findValuesAsText("maxProperties");
        assertEquals(1, result.size());
        assertTrue(result.contains("3"));
    }

    @Test
    public void decimalTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/decimalmax/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        List<String> result = link.get("parameters").findValuesAsText("maximum");
        assertEquals(1, result.size());
        assertTrue(result.contains("3"));

        result = link.get("parameters").findValuesAsText("exclusiveMaximum");
        assertEquals(1, result.size());
        assertTrue(result.contains("true"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/decimalmin/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        result = link.get("parameters").findValuesAsText("minimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("1"));

        result = link.get("parameters").findValuesAsText("exclusiveMinimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("true"));
    }

    @Test
    public void maxTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/max/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        List<String> result = link.get("parameters").findValuesAsText("maximum");
        assertEquals(1, result.size());
        assertTrue(result.contains("4"));
    }

    @Test
    public void minTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/min/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        List<String> result = link.get("parameters").findValuesAsText("minimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("2"));
    }

    @Test
    public void negativeTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/negative/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        List<String> result = link.get("parameters").findValuesAsText("maximum");
        assertEquals(1, result.size());
        assertTrue(result.contains("0"));

        result = link.get("parameters").findValuesAsText("exclusiveMaximum");
        assertEquals(1, result.size());
        assertTrue(result.contains("true"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/negativeorzero/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        result = link.get("parameters").findValuesAsText("maximum");
        assertEquals(1, result.size());
        assertTrue(result.contains("0"));

        result = link.get("parameters").findValuesAsText("exclusiveMaximum");
        assertEquals(0, result.size());
    }

    @Test
    public void positiveTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation/positive/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        List<String> result = link.get("parameters").findValuesAsText("minimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("0"));

        result = link.get("parameters").findValuesAsText("exclusiveMinimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("true"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/positiveorzero/{param}.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        result = link.get("parameters").findValuesAsText("minimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("0"));

        result = link.get("parameters").findValuesAsText("exclusiveMinimum");
        assertEquals(0, result.size());

        link = path(getOpenAPIJson(), "paths./test/bean-validation/positiveorzeroquery.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        result = link.get("parameters").findValuesAsText("minimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("0"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/positiveorzeromultiplequery.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        result = link.get("parameters").findValuesAsText("minimum");
        assertEquals(2, result.size());
        assertTrue(result.contains("0"));
        assertEquals("param", link.get("parameters").get(0).get("name").asText());
        assertEquals("param2", link.get("parameters").get(1).get("name").asText());


        link = path(getOpenAPIJson(), "paths./test/bean-validation/positiveorzerowildcard.post.requestBody.content");
        assertNotNull(link);

        result = link.get("*/*").findValuesAsText("minimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("0"));

        link = path(getOpenAPIJson(), "paths./test/bean-validation/positiveorzerowildcard.post");
        assertNotNull(link);
        assertTrue(link.get("parameters").isArray());

        result = link.get("parameters").findValuesAsText("minimum");
        assertEquals(1, result.size());
        assertTrue(result.contains("0"));
        assertEquals("param", link.get("parameters").get(0).get("name").asText());
    }

    @Test
    public void noPathParameterTest() {
        JsonNode link = path(getOpenAPIJson(), "paths./test/bean-validation.post");
        assertNotNull(link);

        JsonNode schema = path(getOpenAPIJson(), "paths./test/bean-validation.post.requestBody.content.application/json.schema");
        assertNotNull(schema);
        assertEquals("1", schema.get("minItems").asText());

        assertThrows(AssertionError.class, () -> path(getOpenAPIJson(), "paths./test/bean-validation.get.requestBody.content.*/*.schema"));


        JsonNode schema3 = path(getOpenAPIJson(), "paths./test/bean-validation.put.requestBody.content.*/*.schema");
        assertNotNull(schema3);
        assertEquals("1", schema3.get("minItems").asText());
    }
}
