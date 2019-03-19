package fish.payara.microprofile.openapi.test.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;
import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.spec.OpenAPIValidator;

public abstract class OpenAPIApplicationTest {

    private static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.createJson();

    private OpenAPI document;
    private ObjectNode jsonDocument;

    @Before
    public void createDocument() {
        document = ApplicationProcessedDocument.createDocument(getClass());
        jsonDocument = JSON_MAPPER.valueToTree(document);
    }

    public OpenAPI getDocument() {
        return document;
    }

    public ObjectNode getOpenAPIJson() {
        return jsonDocument;
    }

    @Test
    public void documentIsValid() {
        assertNotNull(document);
        OpenAPIValidator.validate(jsonDocument);
    }

    protected static JsonNode path(JsonNode root, String path) {
        return path(root, path.split("\\."));
    }

    protected static JsonNode path(JsonNode root, String... pathElements) {
        JsonNode current = root;
        for (int i = 0; i < pathElements.length; i++) {
            String nameOrIndex = pathElements[i];
            if (current != null) {
                if (nameOrIndex.startsWith("[") && nameOrIndex.endsWith("]")) {
                    current = current.get(Integer.parseInt(nameOrIndex.substring(1, nameOrIndex.length() - 1)));
                } else if (Character.isDigit(nameOrIndex.charAt(0))) {
                    current = current.get(Integer.parseInt(nameOrIndex));
                } else {
                    current = current.get(nameOrIndex);
                }
            }
            if (current == null) {
                String subPath = String.join(".", Arrays.copyOf(pathElements, i + 1));
                fail("Missing path `" + String.join(".", pathElements) + "` at `" + subPath + "` in :\n"
                        + prettyPrint(root));
            }
        }
        return current;
    }

    protected static String prettyPrint(JsonNode node) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return node.toString(); // best we can do
        }
    }
}
