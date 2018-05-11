package fish.payara.microprofile.openapi.impl.model.media;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Discriminator;

public class DiscriminatorImpl implements Discriminator {

    protected String propertyName;
    protected Map<String, String> mapping = new HashMap<>();

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Discriminator propertyName(String propertyName) {
        setPropertyName(propertyName);
        return this;
    }

    @Override
    public Map<String, String> getMapping() {
        return mapping;
    }

    @Override
    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Discriminator mapping(Map<String, String> mapping) {
        setMapping(mapping);
        return this;
    }

    @Override
    public Discriminator addMapping(String name, String value) {
        mapping.put(name, value);
        return this;
    }

}
