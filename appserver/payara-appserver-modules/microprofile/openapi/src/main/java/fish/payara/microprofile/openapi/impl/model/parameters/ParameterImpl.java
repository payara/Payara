package fish.payara.microprofile.openapi.impl.model.parameters;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;

public class ParameterImpl extends ExtensibleImpl implements Parameter {

    protected String name;
    protected In in;
    protected String description;
    protected Boolean required;
    protected Boolean deprecated;
    protected Boolean allowEmptyValue;
    protected String ref;

    protected Style style;
    protected Boolean explode;
    protected Boolean allowReserved;
    protected Schema schema;
    protected Map<String, Example> examples = new HashMap<>();
    protected Object example;
    protected Content content;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Parameter name(String name) {
        setName(name);
        return this;
    }

    @Override
    public In getIn() {
        return in;
    }

    @Override
    public void setIn(In in) {
        this.in = in;
    }

    @Override
    public Parameter in(In in) {
        setIn(in);
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
    public Parameter description(String description) {
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
    public Parameter required(Boolean required) {
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
    public Parameter deprecated(Boolean deprecated) {
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
    public Parameter allowEmptyValue(Boolean allowEmptyValue) {
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
    public Parameter style(Style style) {
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
    public Parameter explode(Boolean explode) {
        setExplode(explode);
        return this;
    }

    @Override
    public Boolean getAllowReserved() {
        return allowReserved;
    }

    @Override
    public void setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
    }

    @Override
    public Parameter allowReserved(Boolean allowReserved) {
        setAllowReserved(allowReserved);
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
    public Parameter schema(Schema schema) {
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
    public Parameter examples(Map<String, Example> examples) {
        setExamples(examples);
        return this;
    }

    @Override
    public Parameter addExample(String key, Example example) {
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
    public Parameter example(Object example) {
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
    public Parameter content(Content content) {
        setContent(content);
        return this;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/parameters/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public Parameter ref(String ref) {
        setRef(ref);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.parameters.Parameter from, Parameter to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        to.setName(mergeProperty(to.getName(), from.name(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        if (from.in() != null && from.in() != ParameterIn.DEFAULT) {
            to.setIn(mergeProperty(to.getIn(), Parameter.In.valueOf(from.in().name()), override));
        }
        to.setRequired(mergeProperty(to.getRequired(), from.required(), override));
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.deprecated(), override));
        to.setAllowEmptyValue(mergeProperty(to.getAllowEmptyValue(), from.allowEmptyValue(), override));
        if (from.style() != null && from.style() != ParameterStyle.DEFAULT) {
            to.setStyle(mergeProperty(to.getStyle(), Style.valueOf(from.style().name()), override));
        }
        if (from.explode() != null && from.explode() != Explode.DEFAULT) {
            to.setExplode(mergeProperty(to.getExplode(), false, override));
        }
        to.setAllowReserved(mergeProperty(to.getAllowReserved(), from.allowReserved(), override));
        if (!isAnnotationNull(from.schema())) {
            if (to.getSchema() == null) {
                to.setSchema(new SchemaImpl());
            }
            SchemaImpl.merge(from.schema(), to.getSchema(), override, currentSchemas);
        }
        to.setExample(mergeProperty(to.getExample(), from.example(), override));
        if (from.examples() != null) {
            for (ExampleObject exampleObject : from.examples()) {
                Example example = new ExampleImpl();
                ExampleImpl.merge(exampleObject, example, override);
                to.addExample(exampleObject.name(), example);
            }
        }
        if (from.content() != null) {
            for (org.eclipse.microprofile.openapi.annotations.media.Content content : from.content()) {
                if (to.getContent() == null) {
                    to.setContent(new ContentImpl());
                }
                ContentImpl.merge(content, to.getContent(), override, null);
            }
        }
    }

}
