/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.node.runtime;

import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.WebService;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds default login configuration to all Web Service EJB nodes
 *
 * @author lprimak
 */
public class DefaultWebServiceLoginConfigNode extends RuntimeDescriptorNode {
    @Override
    protected Map getDispatchTable() {
        @SuppressWarnings("unchecked")
        Map<String, String> table = super.getDispatchTable();
        table.put(RuntimeTagNames.AUTH_METHOD, "setAuthMethod");
        table.put(RuntimeTagNames.REALM, "setRealm");
        return table;
    }

    @Override
    public Object getDescriptor() {
        return this;
    }

    @Override
    public boolean endElement(XMLElement element) {
        log.log(Level.FINEST, "End Element: {0}", element.getQName());
        if (authMethod != null && element.getQName().equals(RuntimeTagNames.WEBSERVICE_DEFAULT_LOGIN_CONFIG)) {
            EjbBundleDescriptor descriptor = (EjbBundleDescriptor) getParentNode().getDescriptor();
            for (WebService wsDesc : descriptor.getWebServices().getWebServices()) {
                for (WebServiceEndpoint endpoint : wsDesc.getEndpoints()) {
                    if (!endpoint.hasAuthMethod()) {
                        log.log(Level.FINE, "Default Login for Web Service Endpoint: {0}, Method {1}, Realm {2}",
                                new Object[]{ endpoint.getName(), authMethod, realm});
                        endpoint.setAuthMethod(authMethod);
                        endpoint.setRealm(realm);
                    }
                }
            }
        }
        return super.endElement(element);
    }

    public void setRealm(String realm) {
        log.log(Level.FINEST, "Global Setting Realm: {0}", realm);
        this.realm = realm;
    }

    public void setAuthMethod(String authMethod) {
        log.log(Level.FINEST, "Global Setting Auth Method: {0}", authMethod);
        this.authMethod = authMethod;
    }

    private String realm;
    private String authMethod;
    private static final Logger log = Logger.getLogger(DefaultWebServiceLoginConfigNode.class.getName());
}
