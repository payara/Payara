package fish.payara.microprofile.openapi.impl.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.openapi.models.Extensible;

public abstract class ExtensibleTreeMap<V, T extends Extensible<T>> extends TreeMap<String, V>
        implements Extensible<T> {

    protected Map<String, Object> extensions = new LinkedHashMap<>();

    protected ExtensibleTreeMap() {
        super();
    }

    protected ExtensibleTreeMap(Map<String, ? extends V> items) {
        super(items);
    }

    @Override
    public final Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public final void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T addExtension(String name, Object value) {
        if (value != null) {
            this.extensions.put(name, value);
        }
        return (T) this;
    }

    @Override
    public final void removeExtension(String name) {
        this.extensions.remove(name);
    }
}
