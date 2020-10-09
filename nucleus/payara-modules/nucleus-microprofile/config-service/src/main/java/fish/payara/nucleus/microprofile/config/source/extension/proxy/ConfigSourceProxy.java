package fish.payara.nucleus.microprofile.config.source.extension.proxy;

import java.util.HashMap;
import java.util.Map;

import fish.payara.nucleus.microprofile.config.source.extension.ExtensionConfigSource;

public class ConfigSourceProxy implements ExtensionConfigSource {

    private final String name;
    private ExtensionConfigSource delegate;

    public ConfigSourceProxy(String name) {
        this.name = name;
        this.delegate = null;
    }

    public void setDelegate(ExtensionConfigSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<String, String> getProperties() {
        if (delegate != null) {
            return delegate.getProperties();
        }
        return new HashMap<>();
    }

    @Override
    public String getValue(String propertyName) {
        if (delegate != null) {
            return delegate.getValue(propertyName);
        }
        return null;
    }

    @Override
    public String getName() {
        if (delegate != null) {
            return delegate.getName();
        }
        return name;
    }

    @Override
    public boolean setValue(String name, String value) {
        if (delegate != null) {
            return delegate.setValue(name, value);
        }
        return false;
    }

    @Override
    public boolean deleteValue(String name) {
        if (delegate != null) {
            return delegate.deleteValue(name);
        }
        return false;
    }
    
}
