/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2022] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.api.Globals;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.openapi.api.OpenAPIBuildException;
import fish.payara.microprofile.openapi.impl.admin.OpenApiServiceConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;

@Service(name = "microprofile-openapi-service")
@Singleton
public class OpenApiService {

    private boolean enabled;
    private boolean securityEnabled;
    private boolean withCorsHeaders;

    private volatile OpenAPI cachedResult;

    private Map<String, OpenAPISupplier> documents;

    public OpenApiService() {
        this.documents = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void initConfig() {
        OpenApiServiceConfiguration config = Globals.get(OpenApiServiceConfiguration.class);
        this.enabled = Boolean.valueOf(config.getEnabled());
        this.securityEnabled = Boolean.valueOf(config.getSecurityEnabled());
        this.withCorsHeaders = Boolean.valueOf(config.getCorsHeaders());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public void setSecurityEnabled(boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public boolean withCorsHeaders() {
        return withCorsHeaders;
    }

    public void setCorsHeaders(boolean withCorsHeaders) {
        this.withCorsHeaders = withCorsHeaders;
    }

    public void registerApp(String applicationId, DeploymentContext ctx) {
        final WebBundleDescriptorImpl descriptor = ctx.getModuleMetaData(WebBundleDescriptorImpl.class);
        final String contextRoot = descriptor.getContextRoot();
        final ReadableArchive archive = ctx.getSource();
        final ClassLoader classLoader = ctx.getClassLoader();
        documents.put(applicationId, new OpenAPISupplier(applicationId, contextRoot, archive, classLoader));
        cachedResult = null;
    }

    public void deregisterApp(String applicationId) {
        documents.remove(applicationId);
        cachedResult = null;
    }

    public void resumeApp(String applicationId) {
        documents.get(applicationId).setEnabled(true);
        cachedResult = null;
    }

    public void suspendApp(String applicationId) {
        documents.get(applicationId).setEnabled(false);
        cachedResult = null;
    }

    /**
     * @return the document If multiple application deployed then merge all the
     * documents. Creates one if it hasn't already been created.
     * @throws OpenAPIBuildException if creating the document failed.
     * @throws java.io.IOException if source archive not accessible
     */
    public synchronized OpenAPI getDocument() throws OpenAPIBuildException, IOException, CloneNotSupportedException {
        if (documents.isEmpty()) {
            return null;
        }
        if (cachedResult != null) {
            return cachedResult;
        }
        OpenAPI result = null;
        Iterator<OpenAPISupplier> iterator = documents.values().iterator();
        do {
            OpenAPI next = iterator.next().get();
            if (result == null) {
                result = ((OpenAPIImpl) next).clone();
            } else {
                OpenAPIImpl.merge(next, result, true, null);
            }
        } while (iterator.hasNext());

        this.cachedResult = result;
        return result;
    }

    public static final OpenApiService getInstance() {
        return Globals.getStaticHabitat().getService(OpenApiService.class);
    }

}
