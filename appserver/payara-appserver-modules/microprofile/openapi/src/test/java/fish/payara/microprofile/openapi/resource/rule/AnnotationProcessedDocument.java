package fish.payara.microprofile.openapi.resource.rule;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

import fish.payara.microprofile.openapi.impl.processor.AnnotationProcessor;
import fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor;
import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;
import fish.payara.microprofile.openapi.resource.classloader.ApplicationClassLoader;
import fish.payara.microprofile.openapi.test.app.TestApplication;
import fish.payara.microprofile.openapi.test.app.annotation.OpenAPIDefinitionTest;
import fish.payara.microprofile.openapi.test.app.data.SchemaComponentTest;

public class AnnotationProcessedDocument extends ProcessedDocument {

    public AnnotationProcessedDocument() {
        // Apply base processor
        new BaseProcessor("/testlocation_123").process(this, null);

        ClassLoader appClassLoader = new ApplicationClassLoader(new TestApplication(),
                asList(SchemaComponentTest.class, OpenAPIDefinitionTest.class).stream().collect(toSet()));

        // Apply application processor
        new ApplicationProcessor(appClassLoader).process(this, null);

        // Apply annotation processor
        new AnnotationProcessor(appClassLoader).process(this, null);
    }

}