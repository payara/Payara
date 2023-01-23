package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.databind.JsonNode;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.junit.Test;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

@Path("/exceptions")
public class ExceptionMapperTest extends OpenApiApplicationTest {

    @Schema(description = "A custom exception")
    class MyException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    class MyExceptionMapper implements ExceptionMapper<MyException> {
        @Override
        @APIResponseSchema(value = MyException.class, responseCode = "500")
        public Response toResponse(MyException exception) {
            return null;
        }
    }

    @GET
    public String exceptionalMethod() throws MyException {
        return null;
    }

    @POST
    public String nonExceptionalMethod() throws Exception {
        return null;
    }

    @Test
    public void testMappedException() {
        JsonNode exceptionalResponse = path(getOpenAPIJson(), "paths./test/exceptions.get.responses.500");
        assertNotNull(exceptionalResponse);
        assertEquals("MyException", exceptionalResponse.get("description").textValue());
    }

    /**
     * Tests that exception mappers don't leak onto the wrong methods
     */
    @Test
    public void testOnlyExceptionalMethodsMapped() {
        assertNull(path(getOpenAPIJson(), "paths./test/exceptions.post.responses").get("500"));
    }

}
