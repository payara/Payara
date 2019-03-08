/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.getHttpMethod;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.getOrCreateOperation;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.media.Schema;

import fish.payara.microprofile.openapi.impl.model.ExtensibleLinkedHashMap;
import fish.payara.microprofile.openapi.impl.model.OperationImpl;
import fish.payara.microprofile.openapi.impl.model.PathItemImpl;

public class CallbackImpl extends ExtensibleLinkedHashMap<String, PathItem, Callback> implements Callback {

    private static final long serialVersionUID = 5549098533131353142L;

    protected String ref;

    public CallbackImpl() {
        super();
    }

    public CallbackImpl(Map<? extends String, ? extends PathItem> items) {
        super(items);
    }

    @Override
    public Callback addPathItem(String name, PathItem item) {
        this.put(name, item);
        return this;
    }

    @Override
    public void removePathItem(String name) {
        remove(name);
    }

    @Override
    public Map<String, PathItem> getPathItems() {
        return new CallbackImpl(this);
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

    public static void merge(org.eclipse.microprofile.openapi.annotations.callbacks.Callback from, Callback to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        if (!from.callbackUrlExpression().isEmpty()) {
            PathItem pathItem = to.getOrDefault(from.callbackUrlExpression(), new PathItemImpl());
            to.addPathItem(from.callbackUrlExpression(), pathItem);
            if (from.operations() != null) {
                for (CallbackOperation callbackOperation : from.operations()) {
                    applyCallbackOperationAnnotation(pathItem, callbackOperation, override, currentSchemas);
                }
            }
        }
    }

    private static void applyCallbackOperationAnnotation(PathItem pathItem, CallbackOperation annotation,
            boolean override, Map<String, Schema> currentSchemas) {
        HttpMethod method = getHttpMethod(annotation.method());
        if (method != null) {
            Operation operation = getOrCreateOperation(pathItem, method);
            OperationImpl.merge(annotation, operation, override, currentSchemas);
        }
    }

}
