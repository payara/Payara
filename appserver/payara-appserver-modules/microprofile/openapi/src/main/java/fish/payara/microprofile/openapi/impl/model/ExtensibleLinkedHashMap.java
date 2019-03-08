package fish.payara.microprofile.openapi.impl.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Extensible;

public abstract class ExtensibleLinkedHashMap<K, V, T extends Extensible<T>> extends LinkedHashMap<K, V>
implements Extensible<T> {

    protected Map<String, Object> extensions = new HashMap<>();

    protected ExtensibleLinkedHashMap() {
        super();
    }

    protected ExtensibleLinkedHashMap(Map<? extends K, ? extends V> items) {
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
        this.extensions.put(name, value);
        return (T) this;
    }

    @Override
    public final void removeExtension(String name) {
        this.extensions.remove(name);
    }
}
