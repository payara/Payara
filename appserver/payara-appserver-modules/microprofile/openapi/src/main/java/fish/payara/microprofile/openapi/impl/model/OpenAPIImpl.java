package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import fish.payara.microprofile.openapi.impl.model.info.InfoImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import fish.payara.microprofile.openapi.impl.model.tags.TagImpl;

public class OpenAPIImpl extends ExtensibleImpl implements OpenAPI {

    protected String openapi;
    protected Info info;
    protected ExternalDocumentation externalDocs;
    protected List<Server> servers = new ArrayList<>();
    protected List<SecurityRequirement> security = new ArrayList<>();
    protected List<Tag> tags = new ArrayList<>();
    protected Paths paths = new PathsImpl();
    protected Components components = new ComponentsImpl();

    @Override
    public String getOpenapi() {
        return openapi;
    }

    @Override
    public void setOpenapi(String openapi) {
        this.openapi = openapi;
    }

    @Override
    public OpenAPI openapi(String openapi) {
        setOpenapi(openapi);
        return this;
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
    public OpenAPI info(Info info) {
        setInfo(info);
        return this;
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
    public OpenAPI externalDocs(ExternalDocumentation externalDocs) {
        setExternalDocs(externalDocs);
        return this;
    }

    @Override
    public List<Server> getServers() {
        return servers;
    }

    @Override
    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    @Override
    public OpenAPI servers(List<Server> servers) {
        setServers(servers);
        return this;
    }

    @Override
    public OpenAPI addServer(Server server) {
        servers.add(server);
        return this;
    }

    @Override
    public List<SecurityRequirement> getSecurity() {
        return security;
    }

    @Override
    public void setSecurity(List<SecurityRequirement> security) {
        this.security = security;
    }

    @Override
    public OpenAPI security(List<SecurityRequirement> security) {
        setSecurity(security);
        return this;
    }

    @Override
    public OpenAPI addSecurityRequirement(SecurityRequirement securityRequirement) {
        security.add(securityRequirement);
        return this;
    }

    @Override
    public List<Tag> getTags() {
        return tags;
    }

    @Override
    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public OpenAPI tags(List<Tag> tags) {
        setTags(tags);
        return this;
    }

    @Override
    public OpenAPI addTag(Tag tag) {
        tags.add(tag);
        return this;
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
    public OpenAPI paths(Paths paths) {
        setPaths(paths);
        return this;
    }

    @Override
    public OpenAPI path(String name, PathItem path) {
        paths.addPathItem(name, path);
        return this;
    }

    @Override
    public Components getComponents() {
        return components;
    }

    @Override
    public void setComponents(Components components) {
        this.components = components;
    }

    @Override
    public OpenAPI components(Components components) {
        setComponents(components);
        return this;
    }

    public static void merge(OpenAPIDefinition from, OpenAPI to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        // Handle @Info
        if (!isAnnotationNull(from.info())) {
            if (to.getInfo() == null) {
                to.setInfo(new InfoImpl());
            }
            InfoImpl.merge(from.info(), to.getInfo(), override);
        }
        // Handle @Servers
        if (from.servers() != null) {
            for (org.eclipse.microprofile.openapi.annotations.servers.Server server : from.servers()) {
                if (!isAnnotationNull(server)) {
                    Server newServer = new ServerImpl();
                    ServerImpl.merge(server, newServer, true);
                    if (!to.getServers().contains(newServer)) {
                        to.addServer(newServer);
                    }
                }
            }
        }
        // Handle @ExternalDocumentation
        if (!isAnnotationNull(from.externalDocs())) {
            if (to.getExternalDocs() == null) {
                to.setExternalDocs(new ExternalDocumentationImpl());
            }
            ExternalDocumentationImpl.merge(from.externalDocs(), to.getExternalDocs(), override);
        }
        // Handle @SecurityRequirement
        if (from.security() != null) {
            for (org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement requirement : from
                    .security()) {
                if (!isAnnotationNull(requirement)) {
                    SecurityRequirement newRequirement = new SecurityRequirementImpl();
                    SecurityRequirementImpl.merge(requirement, newRequirement, override);
                    if (!to.getSecurity().contains(newRequirement)) {
                        to.addSecurityRequirement(newRequirement);
                    }
                }
            }
        }
        // Handle @Tags
        if (from.tags() != null) {
            for (org.eclipse.microprofile.openapi.annotations.tags.Tag tag : from.tags()) {
                if (!isAnnotationNull(tag)) {
                    if (to.getTags() == null) {
                        to.setTags(new ArrayList<>());
                    }
                    Tag newTag = new TagImpl();
                    TagImpl.merge(tag, newTag, override);
                    to.addTag(newTag);
                }
            }
        }
        // Handle @Components
        ComponentsImpl.merge(from.components(), to.getComponents(), override, null);
    }

}