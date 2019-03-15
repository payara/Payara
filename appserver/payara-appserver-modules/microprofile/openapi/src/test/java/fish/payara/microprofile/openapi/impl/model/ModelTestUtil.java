package fish.payara.microprofile.openapi.impl.model;

import java.util.Collections;
import java.util.function.Supplier;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;

import fish.payara.microprofile.openapi.impl.model.callbacks.CallbackImpl;

public class ModelTestUtil {

    static Callback newCallbackRef(String ref) {
        return newRef(ref, CallbackImpl::new);
    }

    static Callback newCallback(String expression, PathItem item) {
        Callback callback = new CallbackImpl();
        callback.setPathItems(Collections.singletonMap(expression, item));
        return callback;
    }

    static PathItem newPathItemRef(String ref) {
        return newRef(ref, PathItemImpl::new);
    }

    private static <T extends Reference<T>> T newRef(String ref, Supplier<T> constructor) {
        T reference = constructor.get();
        reference.setRef(ref);
        return reference;
    }
}
