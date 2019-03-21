package fish.payara.microprofile.openapi.test.util;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.eclipse.microprofile.openapi.models.Constructible;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fish.payara.microprofile.openapi.impl.model.OpenApiBuilderTest;

public class JsonUtils {

    public static JsonNode path(JsonNode root, String path) {
        return path(root, path.split("\\."));
    }

    public static JsonNode path(JsonNode root, String... pathElements) {
        JsonNode current = root;
        if (pathElements == null || pathElements.length == 0) {
            return root;
        }
        for (int i = 0; i < pathElements.length; i++) {
            String nameOrIndex = pathElements[i];
            if (current != null) {
                if (nameOrIndex.startsWith("[") && nameOrIndex.endsWith("]")) {
                    current = current.get(Integer.parseInt(nameOrIndex.substring(1, nameOrIndex.length() - 1)));
                } else if (!nameOrIndex.isEmpty() && Character.isDigit(nameOrIndex.charAt(0))) {
                    current = current.get(Integer.parseInt(nameOrIndex));
                } else {
                    current = current.get(nameOrIndex);
                }
            }
            if (current == null) {
                String subPath = String.join(".", Arrays.copyOf(pathElements, i)) + ".??? " + pathElements[i] + " ???";
                fail("Missing path `" + String.join(".", pathElements) + "` at `" + subPath + "` in :\n"
                        + prettyPrint(root));
            }
        }
        return current;
    }

    public static String prettyPrint(JsonNode node) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return String.valueOf(node); // best we can do
        }
    }

    public static ObjectNode toJson(Constructible node) {
        return OpenApiBuilderTest.JSON_MAPPER.valueToTree(node);
    }
}
