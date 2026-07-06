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
package fish.payara.microprofile.openapi.impl.model.links;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.UNKNOWN_ELEMENT_NAME;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class LinkImpl extends ExtensibleImpl<Link> implements Link {

    private String operationRef;
    private String operationId;
    private Map<String, Object> parameters = createMap();
    private Object requestBody;
    private String description;
    private String ref;
    private Server server;

    public static Link createInstance(AnnotationModel annotation, ApiContext context) {
        Link from = new LinkImpl();
        from.setOperationRef(annotation.getValue("operationRef", String.class));
        from.setOperationId(annotation.getValue("operationId", String.class));
        List<AnnotationModel> parametersAnnotation = annotation.getValue("parameters", List.class);
        if (parametersAnnotation != null) {
            for (AnnotationModel parameterAnnotation : parametersAnnotation) {
                from.addParameter(
                        parameterAnnotation.getValue("name", String.class),
                        parameterAnnotation.getValue("expression", String.class)
                );
            }
        }
        from.setRequestBody(annotation.getValue("requestBody", String.class));
        from.setDescription(annotation.getValue("description", String.class));
        from.setExtensions(parseExtensions(annotation));
        String ref = annotation.getValue("ref", String.class);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        AnnotationModel serverAnnotation = annotation.getValue("server", AnnotationModel.class);
        if(serverAnnotation != null) {
            from.setServer(ServerImpl.createInstance(serverAnnotation, context));
        }
        return from;
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public String getOperationRef() {
        return operationRef;
    }

    @Override
    public void setOperationRef(String operationRef) {
        this.operationRef = operationRef;
    }

    @Override
    public Object getRequestBody() {
        return requestBody;
    }

    @Override
    public void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public String getOperationId() {
        return operationId;
    }

    @Override
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public Map<String, Object> getParameters() {
        return readOnlyView(parameters);
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = createMap(parameters);
    }

    @Override
    public Link addParameter(String name, Object parameter) {
        if (parameter != null) {
            if (parameters == null) {
                parameters = createMap();
            }
            parameters.put(name, parameter);
        }
        return this;
    }

    @Override
    public void removeParameter(String name) {
        if (parameters != null) {
            parameters.remove(name);
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/links/" + ref;
        }
        this.ref = ref;
    }

    public static void merge(Link from, Link to, boolean override) {
        if (from == null) {
            return;
        }
        if (from.getRef() != null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        to.setOperationId(mergeProperty(to.getOperationId(), from.getOperationId(), override));
        to.setOperationRef(mergeProperty(to.getOperationRef(), from.getOperationRef(), override));
        to.setRequestBody(mergeProperty(to.getRequestBody(), from.getRequestBody(), override));
        for (String parameterName : from.getParameters().keySet()) {
            applyLinkParameter(parameterName, from.getParameters().get(parameterName), to.getParameters(), to::addParameter);
        }
    }

    public static void merge(String linkName, Link link, Map<String, Link> links,
            boolean override) {
        if (link == null) {
            return;
        }

        // Get the link name
        if (linkName == null || linkName.isEmpty()) {
            linkName = UNKNOWN_ELEMENT_NAME;
        }

        // Get or create the link
        Link model = links.getOrDefault(linkName, new LinkImpl());
        links.put(linkName, model);

        // Merge the annotation
        merge(link, model, override);

        // If the merged annotation has a reference, set the name to the reference
        if (model.getRef() != null) {
            links.remove(linkName);
            links.put(model.getRef().split("/")[3], model);
        }
    }

    private static void applyLinkParameter(String parameterName, Object parameter, Map<String, Object> linkParameters, BiConsumer<String, Object> addParameter) {

        // Get the parameter name
        if (parameterName == null || parameterName.isEmpty()) {
            parameterName = UNKNOWN_ELEMENT_NAME;
        }

        // Create the object
        Object model = linkParameters.get(parameterName);
        model = mergeProperty(model, parameter, true);
        addParameter.accept(parameterName, model);
    }

}
