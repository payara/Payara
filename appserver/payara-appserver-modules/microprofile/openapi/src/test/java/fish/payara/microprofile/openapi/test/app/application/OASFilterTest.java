package fish.payara.microprofile.openapi.test.app.application;

import static org.eclipse.microprofile.openapi.OASFactory.createObject;
import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import fish.payara.microprofile.openapi.test.util.JsonUtils;

/**
 * In response to {@link https://github.com/payara/Payara/issues/3724} this tests checks that using a {@link OASFilter}
 * to add a {@link Schema} with additional properties is successful.
 */
@OpenAPIDefinition(
        info = @Info(title = "title", version = "version"))
public class OASFilterTest extends OpenApiApplicationTest {

    @Override
    protected Class<? extends OASFilter> filterClass() {
        return AdditionalPropertiesOASFilter.class;
    }

    public static class AdditionalPropertiesOASFilter implements OASFilter {

        @Override
        public void filterOpenAPI(final OpenAPI openAPI) {
            openAPI.getComponents().getSchemas().put("SimpleMap", createMapKey());
        }

        private static Schema createMapKey() {
            return createObject(Schema.class)
                    .type(SchemaType.OBJECT)
                    .additionalPropertiesSchema(createObject(Schema.class).type(SchemaType.STRING));
        }
    }

    @Test
    public void additionalPropertiesAreAddedByFilter() {
        ObjectNode openAPI = getOpenAPIJson();
        assertEquals("string", JsonUtils.path(openAPI, "components.schemas.SimpleMap.additionalProperties.type").textValue());
    }
}
