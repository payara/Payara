package fish.payara.microprofile.openapi.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.processor.AnnotationProcessor;
import fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor;
import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;
import fish.payara.microprofile.openapi.impl.processor.FileProcessor;
import fish.payara.microprofile.openapi.impl.processor.FilterProcessor;
import fish.payara.microprofile.openapi.impl.processor.ModelReaderProcessor;

@Service(name = "microprofile-openapi-service")
@RunLevel(StartupRunLevel.VAL)
public class OpenApiService implements EventListener, PostConstruct, PreDestroy {

    private Map<ApplicationInfo, OpenAPI> models;

    @Inject
    private Events events;

    @Override
    public void postConstruct() {
        models = new LinkedHashMap<>();
        events.register(this);
    }

    @Override
    public void preDestroy() {
        events.unregister(this);
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.APPLICATION_STARTED)) {
            // Get the application information
            ApplicationInfo appInfo = (ApplicationInfo) event.hook();

            // Create all the relevant resources
            OpenApiConfiguration appConfig = new OpenApiConfiguration(appInfo.getAppClassLoader());
            models.put(appInfo, createOpenApiDocument(appInfo.getAppClassLoader(), getContextRoot(appInfo), appConfig));
        } else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo appInfo = (ApplicationInfo) event.hook();
            models.remove(appInfo);
        }
    }

    private String getContextRoot(ApplicationInfo appInfo) {
        return appInfo.getMetaData(WebBundleDescriptorImpl.class).getContextRoot();
    }

    /**
     * Gets the document for the most recently deployed application.
     */
    public OpenAPI getDocument() {
        if (models.isEmpty()) {
            return null;
        }
        ApplicationInfo lastInfo = null;
        for (ApplicationInfo info : models.keySet())
            lastInfo = info;
        return models.get(lastInfo);
    }

    private OpenAPI createOpenApiDocument(ClassLoader appClassLoader, String contextRoot, OpenApiConfiguration config) {
        OpenAPI document = new OpenAPIImpl();
        new ModelReaderProcessor().process(document, config);
        new FileProcessor(appClassLoader).process(document, config);
        new BaseProcessor(contextRoot).process(document, config);
        new ApplicationProcessor(appClassLoader).process(document, config);
        new AnnotationProcessor(appClassLoader).process(document, config);
        new FilterProcessor().process(document, config);
        return document;
    }

    /**
     * Retrieves an instance of this service from HK2.
     */
    public static OpenApiService getInstance() {
        return Globals.getStaticBaseServiceLocator().getService(OpenApiService.class);
    }

}