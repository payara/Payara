package fish.payara.microprofile.openapi.impl.model.tags;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.List;

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.ExternalDocumentationImpl;

public class TagImpl extends ExtensibleImpl implements Tag {

    protected String name;
    protected String description;
    protected ExternalDocumentation externalDocs;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Tag name(String name) {
        setName(name);
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
    public Tag description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    @Override
    public void setExternalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
    }

    @Override
    public Tag externalDocs(ExternalDocumentation externalDocs) {
        setExternalDocs(externalDocs);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.tags.Tag from, Tag to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setName(mergeProperty(to.getName(), from.name(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        if (!isAnnotationNull(from.externalDocs())) {
            if (to.getExternalDocs() == null) {
                to.setExternalDocs(new ExternalDocumentationImpl());
            }
            ExternalDocumentationImpl.merge(from.externalDocs(), to.getExternalDocs(), override);
        }
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.tags.Tag from, Operation to, boolean override,
            List<Tag> apiTags) {
        if (isAnnotationNull(from)) {
            return;
        }

        String tagName = from.name();

        Tag tag = getExistingTag(from.ref(), apiTags);
        if (tag == null) {
            tag = new TagImpl();
            apiTags.add(tag);
        } else {
            tagName = tag.getName();
        }
        merge(from, tag, override);
        to.addTag(tagName);
    }

    private static Tag getExistingTag(String name, List<Tag> apiTags) {
        Tag foundTag = null;
        for (Tag tag : apiTags) {
            if (tag.getName().equals(name)) {
                foundTag = tag;
            }
        }
        return foundTag;
    }

}