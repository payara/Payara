package fish.payara.microprofile.openapi.test.app.annotation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

/**
 * A resource to test that the @Tag and @Tags annotations are handled correctly.
 */
@Path("/tags")
@Tag(name = "new", description = "Created from the class.")
@Tags(refs = { "tag1" })
public class TagTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/override")
    @Tag(name = "override", description = "Overriden.")
    @Tag(ref = "tag2")
    public String getOverriden() {
        return null;
    }

    @Test
    public void overridenTagTest() {
        TestUtils.testTag(document, "/test/tags/override", HttpMethod.GET, "override", "Overriden.");
        TestUtils.testTag(document, "/test/tags/override", HttpMethod.GET, "tag2", "the second tag.");
        TestUtils.testNotTag(document, "/test/tags/override", HttpMethod.GET, "tag1");
        TestUtils.testNotTag(document, "/test/tags/override", HttpMethod.GET, "new");
    }

    @GET
    @Path("/inherit")
    public String getInherited() {
        return null;
    }

    @Test
    public void inheritedTagTest() {
        TestUtils.testTag(document, "/test/tags/inherit", HttpMethod.GET, "new", "Created from the class.");
        TestUtils.testTag(document, "/test/tags/inherit", HttpMethod.GET, "tag1", "the first tag.");
    }

    @GET
    @Path("/ignore")
    @Tag
    public String getIgnored() {
        return null;
    }

    @GET
    @Path("/ignore2")
    @Tags
    public String getIgnored2() {
        return null;
    }

    @Test
    public void ignoredTagsTest() {
        TestUtils.testNotTag(document, "/test/tags/ignore", HttpMethod.GET, "tag1");
        TestUtils.testNotTag(document, "/test/tags/ignore", HttpMethod.GET, "tag2");
        TestUtils.testNotTag(document, "/test/tags/ignore", HttpMethod.GET, "new");
        TestUtils.testNotTag(document, "/test/tags/ignore2", HttpMethod.GET, "tag1");
        TestUtils.testNotTag(document, "/test/tags/ignore2", HttpMethod.GET, "tag2");
        TestUtils.testNotTag(document, "/test/tags/ignore2", HttpMethod.GET, "new");
    }

}