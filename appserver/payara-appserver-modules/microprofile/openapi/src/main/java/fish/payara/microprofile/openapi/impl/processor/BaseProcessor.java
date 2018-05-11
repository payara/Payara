package fish.payara.microprofile.openapi.impl.processor;

import java.util.Map.Entry;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.PathItemImpl;
import fish.payara.microprofile.openapi.impl.model.info.InfoImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;

public class BaseProcessor implements OASProcessor {

    private final String applicationPath;

    public BaseProcessor(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    @Override
    public void process(OpenAPI api, OpenApiConfiguration config) {

        // Set the OpenAPI version if it hasn't been set
        if (api.getOpenapi() == null) {
            api.setOpenapi("3.0.0");
        }

        // Set the info if it hasn't been set
        if (api.getInfo() == null) {
            api.setInfo(new InfoImpl().title("Deployed Resources").version("1.0.0"));
        }

        // Add the default server
        api.addServer(new ServerImpl().url("http://localhost:8080" + applicationPath));

        // Add the config specified servers
        if (config != null && config.getServers().size() > 0) {
            config.getServers().forEach(url -> api.addServer(new ServerImpl().url(url)));
        }

        // Add the path servers
        if (config != null && !config.getPathServerMap().isEmpty()) {
            for (Entry<String, String> entry : config.getPathServerMap().entrySet()) {
                if (!api.getPaths().containsKey(entry.getKey())) {
                    api.getPaths().addPathItem(entry.getKey(), new PathItemImpl());
                }
                api.getPaths().get(entry.getKey()).addServer(new ServerImpl().url(entry.getValue()));
            }
        }

        // Add the operation servers
        if (config != null && !config.getOperationServerMap().isEmpty()) {
            for (Entry<String, String> entry : config.getOperationServerMap().entrySet()) {

                // For each operation in the tree, add the server if the operation id matches
                for (PathItem pathItem : api.getPaths().values()) {
                    for (Operation operation : pathItem.readOperations()) {
                        if (operation.getOperationId().equals(entry.getKey())) {
                            operation.addServer(new ServerImpl().url(entry.getValue()));
                        }
                    }
                }
            }
        }
    }

}