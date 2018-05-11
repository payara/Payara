package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;

public class ExternalDocumentationImpl extends ExtensibleImpl implements ExternalDocumentation {

    protected String description;
    protected String url;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ExternalDocumentation description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public ExternalDocumentation url(String url) {
        setUrl(url);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.ExternalDocumentation from, ExternalDocumentation to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setUrl(mergeProperty(to.getUrl(), from.url(), override));
    }

}