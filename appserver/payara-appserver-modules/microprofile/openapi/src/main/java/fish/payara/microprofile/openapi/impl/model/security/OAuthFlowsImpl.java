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

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class OAuthFlowsImpl extends ExtensibleImpl<OAuthFlows> implements OAuthFlows {

    private OAuthFlow implicit;
    private OAuthFlow password;
    private OAuthFlow clientCredentials;
    private OAuthFlow authorizationCode;

    public static OAuthFlows createInstance(AnnotationModel annotation) {
        OAuthFlows from = new OAuthFlowsImpl();
        from.setExtensions(parseExtensions(annotation));
        AnnotationModel implicitAnnotation = annotation.getValue("implicit", AnnotationModel.class);
        if (implicitAnnotation != null) {
            from.setImplicit(OAuthFlowImpl.createInstance(implicitAnnotation));
        }
        AnnotationModel passwordAnnotation = annotation.getValue("password", AnnotationModel.class);
        if (passwordAnnotation != null) {
            from.setPassword(OAuthFlowImpl.createInstance(passwordAnnotation));
        }
        AnnotationModel clientCredentialsAnnotation = annotation.getValue("clientCredentials", AnnotationModel.class);
        if (clientCredentialsAnnotation != null) {
            from.setClientCredentials(OAuthFlowImpl.createInstance(clientCredentialsAnnotation));
        }
        AnnotationModel authorizationCodeAnnotation = annotation.getValue("authorizationCode", AnnotationModel.class);
        if (authorizationCodeAnnotation != null) {
            from.setAuthorizationCode(OAuthFlowImpl.createInstance(authorizationCodeAnnotation));
        }
        return from;
    }

    @Override
    public OAuthFlow getImplicit() {
        return implicit;
    }

    @Override
    public void setImplicit(OAuthFlow implicit) {
        this.implicit = implicit;
    }

    @Override
    public OAuthFlow getPassword() {
        return password;
    }

    @Override
    public void setPassword(OAuthFlow password) {
        this.password = password;
    }

    @Override
    public OAuthFlow getClientCredentials() {
        return clientCredentials;
    }

    @Override
    public void setClientCredentials(OAuthFlow clientCredentials) {
        this.clientCredentials = clientCredentials;
    }

    @Override
    public OAuthFlow getAuthorizationCode() {
        return authorizationCode;
    }

    @Override
    public void setAuthorizationCode(OAuthFlow authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public static void merge(OAuthFlows from, OAuthFlows to, boolean override) {
        if (from == null) {
            return;
        }
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        if (from.getPassword() != null) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.getPassword(), flow, override);
            to.setPassword(mergeProperty(to.getPassword(), flow, override));
        }
        if (from.getAuthorizationCode() != null) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.getAuthorizationCode(), flow, override);
            to.setAuthorizationCode(mergeProperty(to.getAuthorizationCode(), flow, override));
        }
        if (from.getClientCredentials() != null) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.getClientCredentials(), flow, override);
            to.setClientCredentials(mergeProperty(to.getClientCredentials(), flow, override));
        }
        if (from.getImplicit() != null) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.getImplicit(), flow, override);
            to.setImplicit(mergeProperty(to.getImplicit(), flow, override));
        }
    }

}
