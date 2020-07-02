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
package fish.payara.microprofile.openapi.impl.model.security;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor.getValue;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import org.glassfish.hk2.classmodel.reflect.EnumModel;

public class SecuritySchemeImpl extends ExtensibleImpl<SecurityScheme> implements SecurityScheme {

    private SecurityScheme.Type type;
    private String description;
    private String name;
    private String ref;

    private SecurityScheme.In in;
    private String scheme;
    private String bearerFormat;
    private OAuthFlows flows;
    private String openIdConnectUrl;

    private String apiKeyName;

    public static SecurityScheme createInstance(AnnotationModel annotation) {
        SecuritySchemeImpl from = new SecuritySchemeImpl();
        EnumModel type = getValue("type", EnumModel.class, annotation);
        if (type != null) {
            from.setType(SecurityScheme.Type.valueOf(type.getValue()));
        }
        from.setDescription(getValue("description", String.class, annotation));
        from.setName(getValue("securitySchemeName", String.class, annotation));
        String ref = getValue("ref", String.class, annotation);
        if (ref != null && !ref.isEmpty()) {
            from.setRef(ref);
        }
        EnumModel in = getValue("in", EnumModel.class, annotation);
        if (in != null) {
            from.setIn(SecurityScheme.In.valueOf(in.getValue()));
        }
        from.setScheme(getValue("scheme", String.class, annotation));
        from.setBearerFormat(getValue("bearerFormat", String.class, annotation));
        AnnotationModel flowsAnnotation = getValue("flows", AnnotationModel.class, annotation);
        if (flowsAnnotation != null) {
            from.setFlows(OAuthFlowsImpl.createInstance(flowsAnnotation));
        }
        from.setOpenIdConnectUrl(getValue("openIdConnectUrl", String.class, annotation));
        from.setApiKeyName(getValue("apiKeyName", String.class, annotation));
        return from;
    }

    @Override
    public SecurityScheme.Type getType() {
        return type;
    }

    @Override
    public void setType(SecurityScheme.Type type) {
        this.type = type;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public SecurityScheme.In getIn() {
        return in;
    }

    @Override
    public void setIn(SecurityScheme.In in) {
        this.in = in;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public String getBearerFormat() {
        return bearerFormat;
    }

    @Override
    public void setBearerFormat(String bearerFormat) {
        this.bearerFormat = bearerFormat;
    }

    @Override
    public OAuthFlows getFlows() {
        return flows;
    }

    @Override
    public void setFlows(OAuthFlows flows) {
        this.flows = flows;
    }

    @Override
    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    @Override
    public void setOpenIdConnectUrl(String openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/securitySchemes/" + ref;
        }
        this.ref = ref;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    public static void merge(SecurityScheme from, SecurityScheme to, boolean override) {
        if (from == null) {
            return;
        }

        if (from.getRef() != null && !from.getRef().isEmpty()) {
            applyReference(to, from.getRef());
            return;
        }

        if (from instanceof SecuritySchemeImpl) {
            to.setName(mergeProperty(to.getName(), ((SecuritySchemeImpl) from).getApiKeyName(), override));
        }

        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setScheme(mergeProperty(to.getScheme(), from.getScheme(), override));
        to.setBearerFormat(mergeProperty(to.getBearerFormat(), from.getBearerFormat(), override));
        to.setOpenIdConnectUrl(mergeProperty(to.getOpenIdConnectUrl(), from.getOpenIdConnectUrl(), override));
        if (from.getIn() != null) {
            to.setIn(mergeProperty(to.getIn(), from.getIn(), override));
        }
        if (from.getType() != null) {
            to.setType(mergeProperty(to.getType(), from.getType(), override));
        }
        if (from.getFlows() != null) {
            OAuthFlows flows = new OAuthFlowsImpl();
            OAuthFlowsImpl.merge(from.getFlows(), flows, override);
            to.setFlows(mergeProperty(to.getFlows(), flows, override));
        }
    }

}
