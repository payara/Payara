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
package fish.payara.microprofile.openapi.impl.model.servers;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.Map;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class ServerImpl extends ExtensibleImpl<Server> implements Server {

    private String url;
    private String description;
    private Map<String, ServerVariable> variables = createMap();

    public static Server createInstance(AnnotationModel annotation, ApiContext context) {
        Server from = new ServerImpl();
        from.setDescription(annotation.getValue("description", String.class));
        from.setExtensions(parseExtensions(annotation));
        from.setUrl(annotation.getValue("url", String.class));
        extractAnnotations(annotation, context, "variables", "name", ServerVariableImpl::createInstance, from::addVariable);
        return from;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
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
    public Map<String, ServerVariable> getVariables() {
        return readOnlyView(variables);
    }

    @Override
    public Server addVariable(String variableName, ServerVariable variable) {
        if (variableName != null && variable != null) {
            if (variables == null) {
                variables = createMap();
            }
            variables.put(variableName, variable);
        }
        return this;
    }

    @Override
    public void removeVariable(String variableName) {
        if (variables != null) {
            variables.remove(variableName);
        }
    }

    @Override
    public void setVariables(Map<String, ServerVariable> variables) {
        this.variables = createMap(variables);
    }

    public static void merge(Server from, Server to,
            boolean override) {
        if (from == null) {
            return;
        }
        to.setUrl(mergeProperty(to.getUrl(), from.getUrl(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        if (from.getVariables() != null) {
            for (String serverVariableName : from.getVariables().keySet()) {
                merge(
                        serverVariableName,
                        from.getVariables().get(serverVariableName),
                        ((ServerImpl) to).variables,
                        override
                );
            }
        }
    }

    public static void merge(String serverVariableName, ServerVariable from,
            Map<String, ServerVariable> to, boolean override) {
        if (from == null) {
            return;
        }
        org.eclipse.microprofile.openapi.models.servers.ServerVariable variable = new ServerVariableImpl();
        variable.setDefaultValue(mergeProperty(variable.getDefaultValue(), from.getDefaultValue(), override));
        variable.setDescription(mergeProperty(variable.getDescription(), from.getDescription(), override));
        variable.setExtensions(mergeProperty(variable.getExtensions(), from.getExtensions(), override));
        if (from.getEnumeration()!= null && !from.getEnumeration().isEmpty()) {
            if (variable.getEnumeration() == null) {
                variable.setEnumeration(createList());
            }
            for (String value : from.getEnumeration()) {
                variable.addEnumeration(value);
            }
        }
        if ((to.containsKey(serverVariableName) && override) || !to.containsKey(serverVariableName)) {
            to.put(serverVariableName, variable);
        }
    }

}
