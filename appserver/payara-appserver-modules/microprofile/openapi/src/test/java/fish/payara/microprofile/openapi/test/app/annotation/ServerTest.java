package fish.payara.microprofile.openapi.test.app.annotation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.ServerVariable;
import org.eclipse.microprofile.openapi.annotations.servers.Servers;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that the @Server and @Servers annotations are handled
 * correctly.
 */
@Path("/servers")
@Server(description = "override me", url = "http://override-me")
@Servers(value = { @Server(description = "also override me", url = "http://also-override-me") })
public class ServerTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/override")
    @Server(description = "overridden", url = "http://overriden", variables = {
            @ServerVariable(name = "name", description = "description", defaultValue = "value", enumeration = { "TEST",
                    "ENUM" }) })
    public String getOverriden() {
        return null;
    }

    @Test
    public void overridenServerTest() {
        org.eclipse.microprofile.openapi.models.servers.Server server = TestUtils.testServer(document,
                "/test/servers/override", HttpMethod.GET, "http://overriden", "overridden");
        TestUtils.testNotServer(document, "/test/servers/override", HttpMethod.GET, "http://override-me");
        TestUtils.testNotServer(document, "/test/servers/override", HttpMethod.GET, "http://also-override-me");
        TestUtils.testServerContainsVariable(server, "name", "description", "value", "TEST");
        TestUtils.testServerContainsVariable(server, "name", "description", "value", "ENUM");
    }

    @GET
    @Path("/inherit")
    public String getInherited() {
        return null;
    }

    @Test
    public void inheritedServerTest() {
        TestUtils.testNotServer(document, "/test/servers/inherit", HttpMethod.GET, "http://overriden");
        TestUtils.testServer(document, "/test/servers/inherit", HttpMethod.GET, "http://override-me", "override me");
        TestUtils.testServer(document, "/test/servers/inherit", HttpMethod.GET, "http://also-override-me",
                "also override me");
    }

    @GET
    @Path("/ignore")
    @Server
    public String getIgnored() {
        return null;
    }

    @GET
    @Path("/ignore2")
    @Servers
    public String getIgnored2() {
        return null;
    }

    @Test
    public void ignoredServerTest() {
        TestUtils.testNotServer(document, "/test/servers/ignore", HttpMethod.GET, "http://overriden");
        TestUtils.testNotServer(document, "/test/servers/ignore", HttpMethod.GET, "http://override-me");
        TestUtils.testNotServer(document, "/test/servers/ignore", HttpMethod.GET, "http://also-override-me");
        TestUtils.testNotServer(document, "/test/servers/ignore2", HttpMethod.GET, "http://overriden");
        TestUtils.testNotServer(document, "/test/servers/ignore2", HttpMethod.GET, "http://override-me");
        TestUtils.testNotServer(document, "/test/servers/ignore2", HttpMethod.GET, "http://also-override-me");
    }

}