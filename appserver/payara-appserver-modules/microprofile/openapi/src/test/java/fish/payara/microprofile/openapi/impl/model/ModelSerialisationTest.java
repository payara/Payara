package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.ModelTestUtil.newCallback;
import static fish.payara.microprofile.openapi.impl.model.ModelTestUtil.newCallbackRef;
import static fish.payara.microprofile.openapi.impl.model.ModelTestUtil.newPathItemRef;
import static org.junit.Assert.assertNotNull;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;
import fish.payara.microprofile.openapi.spec.NodeType;
import fish.payara.microprofile.openapi.spec.OpenAPIValidator;

/**
 * A test that should help detect (and thereby avoid) changes in the model that do serialise to an non valid Open API
 * document. This can happen quite easily since we use "automatic" serialisation with jackson that picks up getters
 * (unless explicitly told not to). This might not seem a big thing at first but adding anything to the document that
 * isn't expected or allowed makes the resulting Open API document invalid. Therefore this test creates model objects,
 * serialises them to JSON and validates the tree against the Open API specification as modeled by {@link NodeType}.
 */
public class ModelSerialisationTest {

    private static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.createJson();

    @Test
    public void callbackWithRef() {
        assertValid(NodeType.Reference, newCallbackRef("#/components/schemas/SomePayload"));
    }

    @Test
    public void callbackWithPathItem() {
        assertValid(NodeType.Callback, newCallback("foo", newPathItemRef("#bar")));
    }

    @Test
    public void callbackWithExtension() {
        assertValid(NodeType.Callback, newCallback("foo", newPathItemRef("#bar"))
                .addExtension("hello", "world"));
    }

    @Test
    public void exampleWithExtension() {
        Example ex = new ExampleImpl();
        ex = ex.addExtension("x-hello", "world");
        assertValid(NodeType.Example, ex);
    }

    private static void assertValid(NodeType type, Constructible node) {
        assertNotNull("The example tested should not be null", node);
        OpenAPIValidator.validate(type, JSON_MAPPER.valueToTree(node));
    }

}
