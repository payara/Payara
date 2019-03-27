package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

/**
 * Test for scenario given in PAYARA-3162 (Annotations from interfaces were not recognised).
 **/
public class HelloControllerTest extends OpenApiApplicationTest {

    interface HelloResource {

        @GET
        String sayHello();
    }

    @Path("/hello")
    static class HelloController implements HelloResource {
        @Override
        public String sayHello() {
            return "Hello World";
        }
    }

    @Test
    public void methodsCanBeAnnotatedOnInterface() {
        JsonNode sayHello = path(getOpenAPIJson(), "paths./test/hello.get");
        assertNotNull(sayHello);
        assertEquals("sayHello", sayHello.get("operationId").textValue());
        JsonNode schema = path(sayHello, "responses.default.content.*/*.schema");
        assertNotNull(schema);
        assertEquals("string", schema.get("type").textValue());
    }

    @Path("/welcome")
    interface WelcomeResource {

        @GET
        String sayWelcome();
    }

    static class WelcomeController implements WelcomeResource {

        @Override
        public String sayWelcome() {
            return "Welcome!";
        }
    }

    @Test
    public void methodsAndPathCanBeAnnotatedOnInterface() {
        JsonNode sayWelcome = path(getOpenAPIJson(), "paths./test/welcome.get");
        assertNotNull(sayWelcome);
        assertEquals("sayWelcome", sayWelcome.get("operationId").textValue());
        JsonNode schema = path(sayWelcome, "responses.default.content.*/*.schema");
        assertNotNull(schema);
        assertEquals("string", schema.get("type").textValue());
    }
}
