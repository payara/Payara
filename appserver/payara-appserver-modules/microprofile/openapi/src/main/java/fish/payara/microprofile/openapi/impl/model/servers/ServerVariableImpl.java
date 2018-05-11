package fish.payara.microprofile.openapi.impl.model.servers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.models.servers.ServerVariable;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class ServerVariableImpl extends ExtensibleImpl implements ServerVariable {

    protected List<String> enumeration = new ArrayList<>();
    protected String defaultValue;
    protected String description;

    @Override
    public List<String> getEnumeration() {
        return enumeration;
    }

    @Override
    public void setEnumeration(List<String> enumeration) {
        this.enumeration = enumeration;
    }

    @Override
    public ServerVariable enumeration(List<String> enumeration) {
        setEnumeration(enumeration);
        return this;
    }

    @Override
    public ServerVariable addEnumeration(String enumeration) {
        this.enumeration.add(enumeration);
        return this;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public ServerVariable defaultValue(String defaultValue) {
        setDefaultValue(defaultValue);
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
    public ServerVariable description(String description) {
        setDescription(description);
        return this;
    }

}
