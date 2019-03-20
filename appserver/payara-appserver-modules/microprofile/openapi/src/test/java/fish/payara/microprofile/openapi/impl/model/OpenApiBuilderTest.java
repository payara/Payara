package fish.payara.microprofile.openapi.impl.model;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.junit.Before;

import com.fasterxml.jackson.databind.ObjectMapper;

import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;

public abstract class OpenApiBuilderTest {

    public static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.createJson();

    protected abstract void setupBaseDocument(OpenAPI document);

    private OpenAPI document;

    @Before
    public void setupDocument() {
        document = OASFactory.createObject(OpenAPI.class);
        Info info = OASFactory.createObject(Info.class);
        info.setTitle("additionalProperties-render");
        info.setVersion("1.0");
        document.setInfo(info);
        document.setOpenapi("3.0.0");
        setupBaseDocument(document);
    }

    public OpenAPI getDocument() {
        return document;
    }
}
