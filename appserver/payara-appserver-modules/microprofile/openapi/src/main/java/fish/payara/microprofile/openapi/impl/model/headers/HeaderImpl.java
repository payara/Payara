package fish.payara.microprofile.openapi.impl.model.headers;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;

public class HeaderImpl extends ExtensibleImpl implements Header {

    protected String ref;
    protected String description;
    protected Boolean required;
    protected Boolean deprecated;
    protected Boolean allowEmptyValue;
    protected Style style;
    protected Boolean explode;
    protected Schema schema;
    protected Map<String, Example> examples = new HashMap<>();
    protected Object example;
    protected Content content;

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/headers/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public Header ref(String ref) {
        setRef(ref);
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Header description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public Boolean getRequired() {
        return required;
    }

    @Override
    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public Header required(Boolean required) {
        setRequired(required);
        return this;
    }

    @Override
    public Boolean getDeprecated() {
        return deprecated;
    }

    @Override
    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public Header deprecated(Boolean deprecated) {
        setDeprecated(deprecated);
        return this;
    }

    @Override
    public Boolean getAllowEmptyValue() {
        return allowEmptyValue;
    }

    @Override
    public void setAllowEmptyValue(Boolean allowEmptyValue) {
        this.allowEmptyValue = allowEmptyValue;
    }

    @Override
    public Header allowEmptyValue(Boolean allowEmptyValue) {
        setAllowEmptyValue(allowEmptyValue);
        return this;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public Header style(Style style) {
        setStyle(style);
        return this;
    }

    @Override
    public Boolean getExplode() {
        return explode;
    }

    @Override
    public void setExplode(Boolean explode) {
        this.explode = explode;
    }

    @Override
    public Header explode(Boolean explode) {
        setExplode(explode);
        return this;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public Header schema(Schema schema) {
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
    public Header examples(Map<String, Example> examples) {
        setExamples(examples);
        return this;
    }

    @Override
    public Header addExample(String key, Example examplesItem) {
        this.examples.put(key, examplesItem);
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
    public Header example(Object example) {
        setExample(example);
        return this;
    }

    @Override
    public Content getContent() {
        return content;
    }

    @Override
    public void setContent(Content content) {
        this.content = content;
    }

    @Override
    public Header content(Content content) {
        setContent(content);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.headers.Header from, Header to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setRequired(mergeProperty(to.getRequired(), from.required(), override));
        to.setAllowEmptyValue(mergeProperty(to.getAllowEmptyValue(), from.allowEmptyValue(), override));
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.deprecated(), override));
        if (!isAnnotationNull(from.schema())) {
            if (to.getSchema() == null) {
                to.setSchema(new SchemaImpl());
            }
            SchemaImpl.merge(from.schema(), to.getSchema(), override, currentSchemas);
        }
    }

}
