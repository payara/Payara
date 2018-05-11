package fish.payara.microprofile.openapi.impl.model.security;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.security.Scopes;

public class ScopesImpl extends LinkedHashMap<String, String> implements Scopes {

    private static final long serialVersionUID = -615440059031779085L;

    protected Map<String, Object> extensions = new HashMap<>();

    @Override
    public Scopes addScope(String name, String item) {
        this.put(name, item);
        return this;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public void addExtension(String name, Object value) {
        this.extensions.put(name, value);
    }

}
