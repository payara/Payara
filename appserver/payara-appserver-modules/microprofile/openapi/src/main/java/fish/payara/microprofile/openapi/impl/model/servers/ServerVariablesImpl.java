/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.model.servers;

import fish.payara.microprofile.openapi.impl.model.ExtensibleTreeMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;

public class ServerVariablesImpl extends ExtensibleTreeMap<ServerVariable, ServerVariables> implements ServerVariables {

    private static final long serialVersionUID = 8869393484826870024L;

    public ServerVariablesImpl() {
        super();
    }

    public ServerVariablesImpl(Map<String, ServerVariable> variables) {
        super(variables);
    }

    @Override
    public ServerVariables addServerVariable(String name, ServerVariable item) {
        if (item != null) {
            this.put(name, item);
        }
        return this;
    }

    @Override
    public void removeServerVariable(String name) {
        remove(name);
    }

    @Override
    public Map<String, ServerVariable> getServerVariables() {
        return new ServerVariablesImpl(this);
    }

    @Override
    public void setServerVariables(Map<String, ServerVariable> items) {
        clear();
        putAll(items);
    }

    public static void merge(String serverVariableName, ServerVariable from,
            ServerVariables to, boolean override) {
        if (from == null) {
            return;
        }
        org.eclipse.microprofile.openapi.models.servers.ServerVariable variable = new ServerVariableImpl();
        variable.setDefaultValue(mergeProperty(variable.getDefaultValue(), from.getDefaultValue(), override));
        variable.setDescription(mergeProperty(variable.getDescription(), from.getDescription(), override));
        if (from.getEnumeration()!= null && !from.getEnumeration().isEmpty()) {
            if (variable.getEnumeration() == null) {
                variable.setEnumeration(new ArrayList<>());
            }
            for (String value : from.getEnumeration()) {
                variable.addEnumeration(value);
            }
        }
        if ((to.hasServerVariable(serverVariableName) && override) || !to.hasServerVariable(serverVariableName)) {
            to.addServerVariable(serverVariableName, variable);
        }
    }

}
