package fish.payara.microprofile.openapi.impl.model.media;

import org.eclipse.microprofile.openapi.models.media.XML;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class XMLImpl extends ExtensibleImpl implements XML {

    protected String name;
    protected String namespace;
    protected String prefix;
    protected Boolean attribute;
    protected Boolean wrapped;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public XML name(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public XML namespace(String namespace) {
        setNamespace(namespace);
        return this;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public XML prefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    @Override
    public Boolean getAttribute() {
        return attribute;
    }

    @Override
    public void setAttribute(Boolean attribute) {
        this.attribute = attribute;
    }

    @Override
    public XML attribute(Boolean attribute) {
        setAttribute(attribute);
        return this;
    }

    @Override
    public Boolean getWrapped() {
        return wrapped;
    }

    @Override
    public void setWrapped(Boolean wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public XML wrapped(Boolean wrapped) {
        setWrapped(wrapped);
        return this;
    }

}
