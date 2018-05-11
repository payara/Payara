package fish.payara.microprofile.openapi.impl.model.parameters;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;

public class RequestBodyImpl extends ExtensibleImpl implements RequestBody {

    protected String description;
    protected Content content = new ContentImpl();
    protected Boolean required;
    protected String ref;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public RequestBody description(String description) {
        setDescription(description);
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
    public RequestBody content(Content content) {
        setContent(content);
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
    public RequestBody required(Boolean required) {
        setRequired(required);
        return this;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/requestBodies/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public RequestBody ref(String ref) {
        setRef(ref);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.parameters.RequestBody from, RequestBody to,
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
        if (from.content() != null) {
            for (org.eclipse.microprofile.openapi.annotations.media.Content content : from.content()) {
                if (to.getContent() == null) {
                    to.setContent(new ContentImpl());
                }
                ContentImpl.merge(content, to.getContent(), override, currentSchemas);
            }
        }

    }

}
