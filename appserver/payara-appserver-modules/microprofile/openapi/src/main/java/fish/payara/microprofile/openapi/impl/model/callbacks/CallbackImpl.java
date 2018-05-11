package fish.payara.microprofile.openapi.impl.model.callbacks;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;

import fish.payara.microprofile.openapi.impl.model.PathItemImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;

public class CallbackImpl extends LinkedHashMap<String, PathItem> implements Callback {

    private static final long serialVersionUID = 5549098533131353142L;

    protected String ref;
    protected Map<String, Object> extensions = new HashMap<>();

    @Override
    public Callback addPathItem(String name, PathItem item) {
        this.put(name, item);
        return this;
    }

    @Override
    public String getRef() {
        return this.ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/callbacks/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public Callback ref(String ref) {
        setRef(ref);
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

    public static void merge(org.eclipse.microprofile.openapi.annotations.callbacks.Callback from, Callback to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        if (!from.callbackUrlExpression().isEmpty()) {
            PathItem pathItem = new PathItemImpl();
            to.addPathItem(from.callbackUrlExpression(), pathItem);
            if (from.operations() != null) {
                for (CallbackOperation callbackOperation : from.operations()) {
                    applyCallbackOperationAnnotation(pathItem, callbackOperation, override);
                }
            }
        }
    }

    private static void applyCallbackOperationAnnotation(PathItem pathItem, CallbackOperation annotation,
            boolean override) {
        HttpMethod method = HttpMethod.valueOf(annotation.method());
        if (method != null) {
            Operation operation = ModelUtils.getOrCreateOperation(pathItem, method);
            operation.setDescription(mergeProperty(operation.getDescription(), annotation.description(), override));
            operation.setSummary(mergeProperty(operation.getSummary(), annotation.summary(), override));
        }
    }

}
