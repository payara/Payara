package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.models.Extensible;

public abstract class ExtensibleImpl implements Extensible {

    protected Map<String, Object> extensions = new HashMap<>();

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

    public static void merge(Extension from, Extensible to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (to.getExtensions() == null) {
            to.setExtensions(new LinkedHashMap<>());
        }
        if (from.name() != null && !from.name().isEmpty()) {
            Object value = mergeProperty(to.getExtensions().get(from.name()), convertExtensionValue(from.value()),
                    override);
            to.getExtensions().put(from.name(), value);
        }
    }

    public static Object convertExtensionValue(String value) {
        if (value == null) {
            return null;
        }
        // Could be an array
        if (value.contains(",")) {
            // Remove leading and trailing brackets, then parse to an array
            String[] possibleArray = value.replaceAll("^[\\[\\{\\(]", "").replaceAll("[\\]\\}\\)]$", "").split(",");

            if (possibleArray.length > 1) {
                return possibleArray;
            }
        }
        return value;
    }

}