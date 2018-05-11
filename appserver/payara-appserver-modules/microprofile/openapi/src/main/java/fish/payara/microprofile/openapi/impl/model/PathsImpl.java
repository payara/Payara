package fish.payara.microprofile.openapi.impl.model;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;

public class PathsImpl extends TreeMap<String, PathItem> implements Paths {

    private static final long serialVersionUID = -3876996963579977405L;

    protected Map<String, Object> extensions = new HashMap<>();

    @Override
    public Paths addPathItem(String name, PathItem item) {
        super.put(name, item);
        return this;
    }

    @Override
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void addExtension(String name, Object value) {
        extensions.put(name, value);
    }

    @Override
    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public static void merge(Paths from, Paths to, boolean override) {
        if (from == null || to == null) {
            return;
        }
        from.entrySet().forEach(entry -> {
            if (!to.containsKey(entry.getKey())) {
                to.addPathItem(entry.getKey(), entry.getValue());
            } else {
                ModelUtils.merge(entry.getValue(), to.get(entry.getKey()), override);
            }
        });
    }

}