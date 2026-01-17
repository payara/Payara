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
package fish.payara.microprofile.openapi.impl.model;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.info.InfoImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import fish.payara.microprofile.openapi.impl.model.tags.TagImpl;
import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createList;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.*;

public class OpenAPIImpl extends ExtensibleImpl<OpenAPI> implements OpenAPI, Cloneable {

    protected String openapi;
    protected Info info;
    protected ExternalDocumentation externalDocs;
    protected List<Server> servers = createList();
    protected List<SecurityRequirement> security = createList();
    protected List<Tag> tags = createList();
    protected Paths paths = new PathsImpl();
    protected Map<String, Set<String>> endpoints = createOrderedMap();
    protected Components components = new ComponentsImpl();

    private ApiContext context;

    public static OpenAPI createInstance(AnnotationModel annotation, ApiContext context) {
        OpenAPIImpl from = new OpenAPIImpl();
        from.context = context;
        AnnotationModel info = annotation.getValue("info", AnnotationModel.class);
        if (info != null) {
            from.setInfo(InfoImpl.createInstance(info));
        }
        AnnotationModel externalDocs = annotation.getValue("externalDocs", AnnotationModel.class);
        if (externalDocs != null) {
            from.setExternalDocs(ExternalDocumentationImpl.createInstance(externalDocs));
        }
        extractAnnotations(annotation, context, "security", SecurityRequirementImpl::createInstance, from::addSecurityRequirement);
        extractAnnotations(annotation, context, "securitySets", SecurityRequirementImpl::createInstances, from::addSecurityRequirement);
        extractAnnotations(annotation, context, "servers", ServerImpl::createInstance, from::addServer);
        extractAnnotations(annotation, context, "tags", TagImpl::createInstance, from::addTag);
        AnnotationModel components = annotation.getValue("components", AnnotationModel.class);
        if (components != null) {
            from.setComponents(ComponentsImpl.createInstance(components, context));
        }
        from.setExtensions(parseExtensions(annotation));
        return from;
    }

    public final ApiContext getContext() {
        return context;
    }

    @Override
    public String getOpenapi() {
        return openapi;
    }

    @Override
    public void setOpenapi(String openapi) {
        this.openapi = openapi;
    }

    @Override
    public Info getInfo() {
        return info;
    }

    @Override
    public void setInfo(Info info) {
        this.info = info;
    }

    @Override
    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    @Override
    public void setExternalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
    }

    @Override
    public List<Server> getServers() {
        return readOnlyView(servers);
    }

    @Override
    public void setServers(List<Server> servers) {
        this.servers = createList(servers);
    }

    @Override
    public OpenAPI addServer(Server server) {
        if (server == null) {
            return this;
        }

        final String serverUrl = server.getUrl();

        if (servers == null) {
            servers = createList();
        }

        for (Server existingServer : getServers()) {
            // If a server with the same URL is found, merge them.
            // Consider two servers without url as different in order to pass TCK.
            if (serverUrl != null && serverUrl.equals(existingServer.getUrl())) {
                ModelUtils.merge(server, existingServer, true);
                return this;
            }
        }

        // If a server with the same URL doesn't exist, create it
        servers.add(server);

        return this;
    }

    @Override
    public void removeServer(Server server) {
        if (servers != null) {
            servers.remove(server);
        }
    }

    @Override
    public List<SecurityRequirement> getSecurity() {
        return readOnlyView(security);
    }

    @Override
    public void setSecurity(List<SecurityRequirement> security) {
        this.security = createList(security);
    }

    @Override
    public OpenAPI addSecurityRequirement(SecurityRequirement securityRequirement) {
        if (securityRequirement != null) {
            if (security == null) {
                security = createList();
            }
            security.add(securityRequirement);
        }
        return this;
    }

    @Override
    public void removeSecurityRequirement(SecurityRequirement securityRequirement) {
        if (security != null) {
            security.remove(securityRequirement);
        }
    }

    @Override
    public List<Tag> getTags() {
        return readOnlyView(tags);
    }

    @Override
    public void setTags(List<Tag> tags) {
        this.tags = createList(tags);
    }

    @Override
    public OpenAPI addTag(Tag tag) {
        if (tags == null) {
            tags = createList();
        }
        tags.add(tag);
        return this;
    }

    @Override
    public void removeTag(Tag tag) {
        if (tags != null) {
            tags.remove(tag);
        }
    }

    @Override
    public Paths getPaths() {
        return paths;
    }

    @Override
    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    @Override
    public Components getComponents() {
        return components;
    }

    @Override
    public void setComponents(Components components) {
        this.components = components;
    }

    public static OpenAPI merge(OpenAPI parent, List<OpenAPI> children, boolean override) {
        for (OpenAPI child : children) {
            OpenAPIImpl.merge(child, parent, override, null);
        }
        return parent;
    }

    public static void merge(OpenAPI from, OpenAPI to, boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        to.setOpenapi(mergeProperty(to.getOpenapi(), from.getOpenapi(), override));
        // Handle @Info
        if (from.getInfo() != null) {
            if (to.getInfo() == null) {
                to.setInfo(new InfoImpl());
            }
            InfoImpl.merge(from.getInfo(), to.getInfo(), override);
        }
        // Handle @Servers
        if (from.getServers()!= null) {
            for (Server server : from.getServers()) {
                if (server != null) {
                    Server newServer = new ServerImpl();
                    ServerImpl.merge(server, newServer, true);
                    if (!to.getServers().contains(newServer)) {
                        to.addServer(newServer);
                    }
                }
            }
        }
        // Handle @ExternalDocumentation
        if (from.getExternalDocs() != null) {
            if (to.getExternalDocs() == null) {
                to.setExternalDocs(new ExternalDocumentationImpl());
            }
            ExternalDocumentationImpl.merge(from.getExternalDocs(), to.getExternalDocs(), override);
        }
        ExtensibleImpl.merge(from, to, override);
        // Handle @SecurityRequirement
        if (from.getSecurity() != null) {
            for (SecurityRequirement requirement : from.getSecurity()) {
                if (requirement != null) {
                    SecurityRequirement newRequirement = new SecurityRequirementImpl();
                    SecurityRequirementImpl.merge(requirement, newRequirement);
                    if (!to.getSecurity().contains(newRequirement)) {
                        to.addSecurityRequirement(newRequirement);
                    }
                }
            }
        }
        // Handle @Tags
        if (from.getTags()!= null) {
            for (Tag tag : from.getTags()) {
                if (tag != null) {
                    if (to.getTags() == null) {
                        to.setTags(createList());
                    }
                    Tag newTag = new TagImpl();
                    TagImpl.merge(tag, newTag, override);
                    to.addTag(newTag);
                }
            }
        }
        // Handle @Components
        ComponentsImpl.merge(from.getComponents(), to.getComponents(), override, context);
        PathsImpl.merge(from.getPaths(), to.getPaths(), override);
        //Handle Endpoints
        Map<String, Set<String>> endpoints = ((OpenAPIImpl) from).getEndpoints();
        if (!endpoints.isEmpty()) {
            OpenAPIImpl toImpl = (OpenAPIImpl) to;
            for (String root : endpoints.keySet()) {
                Set<String> paths = endpoints.get(root);
                toImpl.setEndpoints(ModelUtils.buildEndpoints(toImpl.getEndpoints(), root, paths));
            }
        }
    }

    @Override
    public OpenAPI clone()
            throws CloneNotSupportedException {
        OpenAPI clonedObj = new OpenAPIImpl();
        clonedObj.setOpenapi(this.openapi);
        clonedObj.setInfo(this.info);
        clonedObj.setExternalDocs(this.externalDocs);
        clonedObj.setServers(new ArrayList<>(this.servers));
        clonedObj.setSecurity(new ArrayList<>(this.security));
        clonedObj.setTags(new ArrayList<>(this.tags));
        clonedObj.setPaths(new PathsImpl(this.paths.getPathItems()));
        clonedObj.setComponents(this.components);
        clonedObj.setExtensions(this.extensions);
        ((OpenAPIImpl) clonedObj).setEndpoints(new TreeMap<>(this.getEndpoints()));
        return clonedObj;
    }

    public Map<String, Set<String>> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, Set<String>> endpoints) {
        this.endpoints = endpoints;
    }

}