package fish.payara.microprofile.openapi.test.app;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.toJson;
import static org.junit.Assert.assertNotNull;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fish.payara.microprofile.openapi.resource.rule.ApplicationProcessedDocument;
import fish.payara.microprofile.openapi.spec.OpenApiValidator;

public abstract class OpenApiApplicationTest {

    private OpenAPI document;
    private ObjectNode jsonDocument;

    @Before
    public void createDocument() {
        Class<?> filter = filterClass();
        document = filter == null 
                ? ApplicationProcessedDocument.createDocument(getClass())
                : ApplicationProcessedDocument.createDocument(filterClass(), getClass(), filterClass());
        jsonDocument = toJson(document);
    }

    protected Class<? extends OASFilter> filterClass() {
        return null; // override to customise
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
        OpenApiValidator.validate(jsonDocument);
    }

}
