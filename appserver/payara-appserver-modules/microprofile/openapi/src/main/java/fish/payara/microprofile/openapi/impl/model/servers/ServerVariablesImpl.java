package fish.payara.microprofile.openapi.impl.model.servers;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;

public class ServerVariablesImpl extends LinkedHashMap<String, ServerVariable> implements ServerVariables {

    private static final long serialVersionUID = 8869393484826870024L;

    protected Map<String, Object> extensions = new HashMap<>();

    @Override
    public ServerVariables addServerVariable(String name, ServerVariable item) {
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

    public static void merge(org.eclipse.microprofile.openapi.annotations.servers.ServerVariable from,
            ServerVariables to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        org.eclipse.microprofile.openapi.models.servers.ServerVariable variable = new ServerVariableImpl();
        variable.setDefaultValue(mergeProperty(variable.getDefaultValue(), from.defaultValue(), override));
        variable.setDescription(mergeProperty(variable.getDefaultValue(), from.description(), override));
        if (from.enumeration() != null && from.enumeration().length != 0) {
            if (variable.getEnumeration() == null) {
                variable.setEnumeration(new ArrayList<>());
            }
            for (String value : from.enumeration()) {
                if (!variable.getEnumeration().contains(value)) {
                    variable.addEnumeration(value);
                }
            }
        }
        if ((to.containsKey(from.name()) && override) || !to.containsKey(from.name())) {
            to.addServerVariable(from.name(), variable);
        }
    }

}
