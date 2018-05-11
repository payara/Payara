package fish.payara.microprofile.openapi.impl.processor;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.merge;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;

public class FileProcessor implements OASProcessor {

    private File file;
    private ObjectMapper mapper;

    public FileProcessor(ClassLoader appClassLoader) {
        try {
            if (appClassLoader.getResource("META-INF/openapi.json") != null) {
                file = new File(appClassLoader.getResource("META-INF/openapi.json").toURI());
                mapper = ObjectMapperFactory.createJson();
            } else if (appClassLoader.getResource("META-INF/openapi.yaml") != null) {
                file = new File(appClassLoader.getResource("META-INF/openapi.yaml").toURI());
                mapper = ObjectMapperFactory.createYaml();
            } else if (appClassLoader.getResource("META-INF/openapi.yml") != null) {
                file = new File(appClassLoader.getResource("META-INF/openapi.yml").toURI());
                mapper = ObjectMapperFactory.createYaml();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(OpenAPI api, OpenApiConfiguration config) {
        if (file != null) {
            OpenAPI readResult = null;
            try {
                readResult = mapper.readValue(file, OpenAPIImpl.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            merge(readResult, api, false);
        }
    }

}