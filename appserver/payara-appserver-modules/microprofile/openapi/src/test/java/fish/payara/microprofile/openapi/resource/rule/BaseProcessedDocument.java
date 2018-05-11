package fish.payara.microprofile.openapi.resource.rule;

import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;

public class BaseProcessedDocument extends ProcessedDocument {

    public BaseProcessedDocument() {
        // Apply base processor
        new BaseProcessor("/testlocation_123").process(this, null);
    }

}