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

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

import java.util.List;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class ServerVariableImpl extends ExtensibleImpl<ServerVariable> implements ServerVariable {

    private String description;
    private String defaultValue;

    protected List<String> enumeration = createList();

    @SuppressWarnings("unchecked")
    public static ServerVariable createInstance(AnnotationModel annotation, ApiContext context) {
        ServerVariable from = new ServerVariableImpl();
        from.setDescription(annotation.getValue("description", String.class));
        from.setExtensions(parseExtensions(annotation));
        from.setDefaultValue(annotation.getValue("defaultValue", String.class));
        List<String> enumeration = annotation.getValue("enumeration", List.class);
        if (enumeration != null) {
            from.setEnumeration(enumeration);
        }
        return from;
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
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public List<String> getEnumeration() {
        return readOnlyView(enumeration);
    }

    @Override
    public void setEnumeration(List<String> enumeration) {
        this.enumeration = createList(enumeration);
    }

    @Override
    public ServerVariable addEnumeration(String enumeration) {
        if (enumeration != null) {
            if (this.enumeration == null) {
                this.enumeration = createList();
            }
            if (!this.enumeration.contains(enumeration)) {
                this.enumeration.add(enumeration);
            }
        }
        return this;
    }

    @Override
    public void removeEnumeration(String enumeration) {
        if (this.enumeration != null) {
            this.enumeration.remove(enumeration);
        }
    }

}
