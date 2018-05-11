package fish.payara.microprofile.openapi.impl.model.media;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class MediaTypeImpl extends ExtensibleImpl implements MediaType {

    protected Schema schema;
    protected Map<String, Example> examples = new HashMap<>();
    protected Object example;
    protected Map<String, Encoding> encoding = new HashMap<>();

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public MediaType schema(Schema schema) {
        setSchema(schema);
        return this;
    }

    @Override
    public Map<String, Example> getExamples() {
        return examples;
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = examples;
    }

    @Override
    public MediaType examples(Map<String, Example> examples) {
        setExamples(examples);
        return this;
    }

    @Override
    public MediaType addExample(String key, Example example) {
        this.examples.put(key, example);
        return this;
    }

    @Override
    public Object getExample() {
        return example;
    }

    @Override
    public void setExample(Object example) {
        this.example = example;
    }

    @Override
    public MediaType example(Object example) {
        setExample(example);
        return this;
    }

    @Override
    public Map<String, Encoding> getEncoding() {
        return encoding;
    }

    @Override
    public void setEncoding(Map<String, Encoding> encoding) {
        this.encoding = encoding;
    }

    @Override
    public MediaType encoding(Map<String, Encoding> encoding) {
        setEncoding(encoding);
        return this;
    }

    @Override
    public MediaType addEncoding(String key, Encoding encodingItem) {
        this.encoding.put(key, encodingItem);
        return this;
    }

}
