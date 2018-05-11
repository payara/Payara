package fish.payara.microprofile.openapi.resource.rule;

import static java.util.Collections.singleton;

import fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor;
import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;
import fish.payara.microprofile.openapi.resource.classloader.ApplicationClassLoader;
import fish.payara.microprofile.openapi.test.app.TestApplication;
import fish.payara.microprofile.openapi.test.app.data.SchemaComponentTest;

public class ApplicationProcessedDocument extends ProcessedDocument {

    public ApplicationProcessedDocument() {
        // Apply base processor
        new BaseProcessor("/testlocation_123").process(this, null);

        ClassLoader appClassLoader = new ApplicationClassLoader(new TestApplication(),
                singleton(SchemaComponentTest.class));

        // Apply application processor
        new ApplicationProcessor(appClassLoader).process(this, null);
    }

}