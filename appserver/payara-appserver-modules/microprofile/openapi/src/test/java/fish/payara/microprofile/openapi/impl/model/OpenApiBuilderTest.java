package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.toJson;
import static org.eclipse.microprofile.openapi.OASFactory.createObject;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;
import fish.payara.microprofile.openapi.spec.OpenApiValidator;

public abstract class OpenApiBuilderTest {

    public static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.createJson();

    protected abstract void setupBaseDocument(OpenAPI document);

    private OpenAPI document;

    @Before
    public void setupDocument() {
        document = createObject(OpenAPI.class);
        Info info = createObject(Info.class);
        info.setTitle("title");
        info.setVersion("version");
        document.setInfo(info);
        document.setOpenapi("3.0.0");
        setupBaseDocument(document);
    }

    public OpenAPI getDocument() {
        return document;
    }

    public ObjectNode getOpenAPIJson() {
        return toJson(getDocument());
    }

    @Test
    public void baseJsonDocumentIsValid() {
        assertValid(getDocument());
    }

    protected static void assertValid(OpenAPI document) {
        OpenApiValidator.validate(toJson(document));
    }
}
