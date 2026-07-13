/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.openapi.impl.model.callbacks;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import static fish.payara.microprofile.openapi.impl.model.ExtensibleImpl.parseExtensions;
import fish.payara.microprofile.openapi.impl.model.ExtensibleTreeMap;
import fish.payara.microprofile.openapi.impl.model.OperationImpl;
import fish.payara.microprofile.openapi.impl.model.PathItemImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.getHttpMethod;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.getOrCreateOperation;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class CallbackImpl extends ExtensibleTreeMap<PathItem, Callback> implements Callback {

    private static final long serialVersionUID = 5549098533131353142L;

    private String ref;
    
    private String urlExpression;

    private List<Operation> operations = createList();

    public static Callback createInstance(AnnotationModel annotation, ApiContext context) {
        CallbackImpl from = new CallbackImpl();
        from.setExtensions(parseExtensions(annotation));
        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        String urlExpression = annotation.getValue("callbackUrlExpression", String.class);
        if (urlExpression != null && !urlExpression.isEmpty()) {
            from.setUrlExpression(urlExpression);
            List<Operation> operations = createList();
            extractAnnotations(annotation, context, "operations", OperationImpl::createInstance, operations::add);
            from.setOperations(operations);
        }
        return from;
    }

    public CallbackImpl() {
        super();
    }

    public CallbackImpl(Map<String, ? extends PathItem> items) {
        super(items);
    }

    @Override
    public Callback addPathItem(String name, PathItem item) {
        if (item != null) {
            this.put(name, item);
        }
        return this;
    }

    @Override
    public void removePathItem(String name) {
        remove(name);
    }

    @Override
    public Map<String, PathItem> getPathItems() {
        return readOnlyView(this);
    }

    @Override
    public void setPathItems(Map<String, PathItem> items) {
        clear();
        putAll(items);
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

    public String getUrlExpression() {
        return urlExpression;
    }

    public void setUrlExpression(String urlExpression) {
        this.urlExpression = urlExpression;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = createList(operations);
    }

    public static void merge(Callback from, Callback to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        if (from.getRef()!= null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }
        if (from instanceof CallbackImpl) {
            CallbackImpl fromImpl = (CallbackImpl) from;
            String urlExpression = fromImpl.getUrlExpression();
            if (urlExpression != null && !urlExpression.isEmpty()) {
                PathItem pathItem = to.getPathItems().getOrDefault(urlExpression, new PathItemImpl());
                to.addPathItem(urlExpression, pathItem);
                if (fromImpl.getOperations() != null) {
                    for (Operation callbackOperation : fromImpl.getOperations()) {
                        applyCallbackOperationAnnotation(pathItem, callbackOperation,
                                override, context);
                    }
                }
            }
        }
    }

    private static void applyCallbackOperationAnnotation(PathItem pathItem, Operation callbackOperation,
            boolean override, ApiContext context) {
        if (callbackOperation instanceof OperationImpl) {
            OperationImpl callbackOperationImpl = (OperationImpl) callbackOperation;
            if (callbackOperationImpl.getMethod() != null) {
                HttpMethod method = getHttpMethod(callbackOperationImpl.getMethod());
                if (method != null) {
                    Operation operation = getOrCreateOperation(pathItem, method);
                    OperationImpl.merge(callbackOperation, operation, override, context);
                }
            }
        }
    }

}
