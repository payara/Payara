package fish.payara.microprofile.openapi.test.app;

import static org.junit.Assert.assertNotNull;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;
import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.spec.OpenAPIValidator;

public abstract class OpenAPIApplicationTest {

    private static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.createJson();

    protected OpenAPI document;

    @Before
    public void createDocument() {
        document = ApplicationProcessedDocument.createDocument(getClass());
    }

    @Test
    public void documentIsValid() {
        assertNotNull(document);
        OpenAPIValidator.validate(JSON_MAPPER.valueToTree(document));
    }
}
