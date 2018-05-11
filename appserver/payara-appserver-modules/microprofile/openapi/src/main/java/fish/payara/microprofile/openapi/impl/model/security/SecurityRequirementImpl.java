package fish.payara.microprofile.openapi.impl.model.security;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;

public class SecurityRequirementImpl extends LinkedHashMap<String, List<String>> implements SecurityRequirement {

    private static final long serialVersionUID = -677783376083861245L;

    @Override
    public SecurityRequirement addScheme(String name, String item) {
        this.put(name, Arrays.asList(item));
        return this;
    }

    @Override
    public SecurityRequirement addScheme(String name, List<String> item) {
        this.put(name, item);
        return this;
    }

    @Override
    public SecurityRequirement addScheme(String name) {
        this.put(name, new ArrayList<>());
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement from,
            SecurityRequirement to, boolean override) {
        if (isAnnotationNull(from) || from.scopes().length == 0) {
            return;
        }
        if (from.name() != null && !from.name().isEmpty()) {
            to.addScheme(from.name(), Arrays.asList(from.scopes()));
        }
    }

}
