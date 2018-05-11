package fish.payara.microprofile.openapi.impl.model.examples;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.examples.Example;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class ExampleImpl extends ExtensibleImpl implements Example {

    protected String summary;
    protected String description;
    protected Object value;
    protected String externalValue;
    protected String ref;

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public Example summary(String summary) {
        setSummary(summary);
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
    public Example description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public Example value(Object value) {
        setValue(value);
        return this;
    }

    @Override
    public String getExternalValue() {
        return externalValue;
    }

    @Override
    public void setExternalValue(String externalValue) {
        this.externalValue = externalValue;
    }

    @Override
    public Example externalValue(String externalValue) {
        setExternalValue(externalValue);
        return this;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/examples/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public Example ref(String ref) {
        setRef(ref);
        return this;
    }

    public static void merge(ExampleObject from, Example to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setSummary(mergeProperty(to.getSummary(), from.summary(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setValue(mergeProperty(to.getValue(), from.value(), override));
        to.setExternalValue(mergeProperty(to.getExternalValue(), from.externalValue(), override));
    }

}