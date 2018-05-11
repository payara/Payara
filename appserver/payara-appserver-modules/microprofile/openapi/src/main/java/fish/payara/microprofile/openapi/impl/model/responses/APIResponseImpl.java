package fish.payara.microprofile.openapi.impl.model.responses;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.media.ContentImpl;

public class APIResponseImpl extends ExtensibleImpl implements APIResponse {

    protected String description;
    protected Map<String, Header> headers = new HashMap<>();
    protected Content content = new ContentImpl();
    protected Map<String, Link> links = new HashMap<>();
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
    public APIResponse description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public Map<String, Header> getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    @Override
    public APIResponse headers(Map<String, Header> headers) {
        setHeaders(headers);
        return this;
    }

    @Override
    public APIResponse addHeader(String name, Header header) {
        headers.put(name, header);
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
    public APIResponse content(Content content) {
        setContent(content);
        return this;
    }

    @Override
    public Map<String, Link> getLinks() {
        return links;
    }

    @Override
    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    @Override
    public APIResponse links(Map<String, Link> links) {
        setLinks(links);
        return this;
    }

    @Override
    public APIResponse addLink(String name, Link link) {
        links.put(name, link);
        return this;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/responses/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public APIResponse ref(String ref) {
        setRef(ref);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.responses.APIResponse from, APIResponse to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
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
