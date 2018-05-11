package fish.payara.microprofile.openapi.impl.model.servers;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.annotations.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class ServerImpl extends ExtensibleImpl implements Server {

    protected String url;
    protected String description;
    protected ServerVariables variables;

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public Server url(String url) {
        setUrl(url);
        return this;
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
    public Server description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public ServerVariables getVariables() {
        return variables;
    }

    @Override
    public void setVariables(ServerVariables variables) {
        this.variables = variables;
    }

    @Override
    public Server variables(ServerVariables variables) {
        setVariables(variables);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.servers.Server from, Server to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setUrl(mergeProperty(to.getUrl(), from.url(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        if (from.variables() != null) {
            if (to.getVariables() == null) {
                to.setVariables(new ServerVariablesImpl());
            }
            for (ServerVariable variable : from.variables()) {
                ServerVariablesImpl.merge(variable, to.getVariables(), override);
            }
        }
    }

}
