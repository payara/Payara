package fish.payara.microprofile.openapi.impl.processor;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.merge;

import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;

public class ModelReaderProcessor implements OASProcessor {

    private OASModelReader reader;

    @Override
    public void process(OpenAPI api, OpenApiConfiguration config) {
        try {
            if (config.getModelReader() != null) {
                reader = config.getModelReader().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        if (reader != null) {
            OpenAPI model = reader.buildModel();
            merge(model, api, true);
        }
    }

}