package fish.payara.microprofile.openapi.impl.processor;

import java.util.Map.Entry;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;

public class ConfigPropertyProcessor implements OASProcessor {

    @Override
    public OpenAPI process(OpenAPI api, OpenApiConfiguration config) {

        // Add the schemas
        for (Entry<String, SchemaImpl> schemaEntry : config.getSchemaMap().entrySet()) {
            final SchemaImpl schema = schemaEntry.getValue();
            
            // The Java class name represented by the schema
            final String schemaClassName = schemaEntry.getKey();
            schema.setImplementation(schemaClassName);

            // Get the name to use as the key for the Schema
            String schemaName = schema.getName();
            if (schemaName == null || schemaName.isEmpty()) {
                schemaName = ModelUtils.getSimpleName(schemaClassName);
            }

            api.getComponents().addSchema(schemaName, schema);
        }

        return api;
    }
    
}
