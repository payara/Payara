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
package fish.payara.microprofile.openapi.impl.model.security;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class OAuthFlowsImpl extends ExtensibleImpl implements OAuthFlows {

    protected OAuthFlow implicit;
    protected OAuthFlow password;
    protected OAuthFlow clientCredentials;
    protected OAuthFlow authorizationCode;

    @Override
    public OAuthFlow getImplicit() {
        return implicit;
    }

    @Override
    public void setImplicit(OAuthFlow implicit) {
        this.implicit = implicit;
    }

    @Override
    public OAuthFlows implicit(OAuthFlow implicit) {
        setImplicit(implicit);
        return this;
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
    public OAuthFlows password(OAuthFlow password) {
        setPassword(password);
        return this;
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
    public OAuthFlows clientCredentials(OAuthFlow clientCredentials) {
        setClientCredentials(clientCredentials);
        return this;
    }

    @Override
    public OAuthFlow getAuthorizationCode() {
        return authorizationCode;
    }

    @Override
    public void setAuthorizationCode(OAuthFlow authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    @Override
    public OAuthFlows authorizationCode(OAuthFlow authorizationCode) {
        setAuthorizationCode(authorizationCode);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.security.OAuthFlows from, OAuthFlows to,
            boolean override) {
        if (from == null) {
            return;
        }
        if (!isAnnotationNull(from.password())) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.password(), flow, override);
            to.setPassword(mergeProperty(to.getPassword(), flow, override));
        }
        if (!isAnnotationNull(from.authorizationCode())) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.authorizationCode(), flow, override);
            to.setAuthorizationCode(mergeProperty(to.getAuthorizationCode(), flow, override));
        }
        if (!isAnnotationNull(from.clientCredentials())) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.clientCredentials(), flow, override);
            to.setClientCredentials(mergeProperty(to.getClientCredentials(), flow, override));
        }
        if (!isAnnotationNull(from.implicit())) {
            OAuthFlow flow = new OAuthFlowImpl();
            OAuthFlowImpl.merge(from.implicit(), flow, override);
            to.setImplicit(mergeProperty(to.getImplicit(), flow, override));
        }
    }

}
