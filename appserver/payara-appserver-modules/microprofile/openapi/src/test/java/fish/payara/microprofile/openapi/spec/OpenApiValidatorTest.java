package fish.payara.microprofile.openapi.spec;

import java.io.File;
import java.util.logging.Logger;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test that should verify the validity of the {@link OpenApiValidator} model by using it against some of the examples
 * given in the specification.
 */
public class OpenApiValidatorTest {

    private static final Logger LOGGER = Logger.getLogger(OpenApiValidatorTest.class.getName());

    @Test
    public void verifyExamplesPassVerification() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (NodeType type : NodeType.values()) {
            File exampleFile = new File("src/test/resources/" + type.name() + ".json");
            if (exampleFile.exists()) {
                LOGGER.info("Verifying model for " + type);
                JsonNode examples = mapper.readTree(exampleFile);
                if (examples.isArray()) {
                    for (JsonNode example : examples) {
                        OpenApiValidator.validate(type, example);
                    }
                } else {
                    OpenApiValidator.validate(type, examples);
                }
            }
        }
    }
}
