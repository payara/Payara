package fish.payara.microprofile.openapi.impl.processor;

import static fish.payara.microprofile.openapi.impl.processor.utils.ProcessorUtils.getClassesFromLoader;

import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.api.visitor.ApiVisitor;
import fish.payara.microprofile.openapi.api.visitor.ApiWalker;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.visitor.ApplicationVisitor;
import fish.payara.microprofile.openapi.impl.visitor.OpenApiWalker;

public class ApplicationProcessor implements OASProcessor {

    private final Set<Class<?>> classes;

    public ApplicationProcessor(ClassLoader appClassLoader) {
        this.classes = getClassesFromLoader(appClassLoader);
    }

    @Override
    public void process(OpenAPI api, OpenApiConfiguration config) {
        ApiWalker apiWalker = new OpenApiWalker(api, (config == null) ? classes : config.getValidClasses(classes));
        ApiVisitor apiVisitor = new ApplicationVisitor();
        apiWalker.accept(apiVisitor);
    }

}