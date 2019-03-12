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
package fish.payara.microprofile.openapi.impl.model.links;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.UNKNOWN_ELEMENT_NAME;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.links.LinkParameter;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.servers.Server;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class LinkImpl extends ExtensibleImpl<Link> implements Link {

    protected String operationRef;
    protected String operationId;
    protected Map<String, Object> parameters = new HashMap<>();
    protected Object requestBody;
    protected String description;
    protected String ref;
    protected Server server;

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
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Link addParameter(String name, Object parameter) {
        if (parameter != null) {
            this.parameters.put(name, parameter);
        }
        return this;
    }

    @Override
    public void removeParameter(String name) {
        this.parameters.remove(name);
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

    public static void merge(org.eclipse.microprofile.openapi.annotations.links.Link from, Link to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setOperationId(mergeProperty(to.getOperationId(), from.operationId(), override));
        to.setOperationRef(mergeProperty(to.getOperationRef(), from.operationRef(), override));
        to.setRequestBody(mergeProperty(to.getRequestBody(), from.requestBody(), override));
        for (LinkParameter parameter : from.parameters()) {
            applyLinkParameter(parameter, to.getParameters());
        }
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.links.Link link, Map<String, Link> links,
            boolean override) {
        if (isAnnotationNull(link)) {
            return;
        }

        // Get the link name
        String linkName = link.name();
        if (link.name() == null || link.name().isEmpty()) {
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

    private static void applyLinkParameter(LinkParameter parameter, Map<String, Object> linkParameters) {

        // Get the parameter name
        String parameterName = parameter.name();
        if (parameterName == null || parameterName.isEmpty()) {
            parameterName = UNKNOWN_ELEMENT_NAME;
        }

        // Create the object
        Object model = linkParameters.get(parameterName);
        model = mergeProperty(model, parameter.expression(), true);
        linkParameters.put(parameterName, model);
    }

}
