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
package fish.payara.microprofile.openapi.impl.model.security;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class SecurityRequirementImpl extends LinkedHashMap<String, List<String>> implements SecurityRequirement {

    private static final long serialVersionUID = -677783376083861245L;

    public static SecurityRequirement createInstance(AnnotationModel annotation, ApiContext context) {
        SecurityRequirement from = new SecurityRequirementImpl();
        String name = annotation.getValue("name", String.class);
        List<String> scopes = annotation.getValue("scopes", List.class);
        from.addScheme(name, scopes != null ? scopes : Collections.emptyList());
        return from;
    }

    public static SecurityRequirement createInstances(AnnotationModel annotationModel, ApiContext context) {
        SecurityRequirement securityRequirement = new SecurityRequirementImpl();
        List<AnnotationModel> annotations = annotationModel.getValue("value", ArrayList.class);
        if (annotations != null) {
            for (AnnotationModel annotation : annotations) {
                String name = annotation.getValue("name", String.class);
                List<String> scopes = annotation.getValue("scopes", List.class);
                securityRequirement.addScheme(name, scopes != null ? scopes : Collections.emptyList());
            }
        }
        return securityRequirement;
    }

    public SecurityRequirementImpl() {
        super();
    }

    public SecurityRequirementImpl(Map<? extends String, ? extends List<String>> items) {
        super(items);
    }

    @Override
    public SecurityRequirement addScheme(String name, String item) {
        this.put(name, item == null ? createList() : Arrays.asList(item));
        return this;
    }

    @Override
    public SecurityRequirement addScheme(String name, List<String> item) {
        this.put(name, item == null ? createList() : item);
        return this;
    }

    @Override
    public SecurityRequirement addScheme(String name) {
        this.put(name, createList());
        return this;
    }

    @Override
    public void removeScheme(String securitySchemeName) {
        this.remove(securitySchemeName);
    }

    @Override
    public Map<String, List<String>> getSchemes() {
        return readOnlyView(this);
    }

    @Override
    public void setSchemes(Map<String, List<String>> items) {
        clear();
        putAll(items);
    }

    public static void merge(SecurityRequirement from, SecurityRequirement to) {
        if (from == null) {
            return;
        }
        for (String name : from.getSchemes().keySet()) {
            if (name != null && !name.isEmpty()) {
                to.addScheme(name, from.getSchemes().get(name));
            }
        }
    }

}
