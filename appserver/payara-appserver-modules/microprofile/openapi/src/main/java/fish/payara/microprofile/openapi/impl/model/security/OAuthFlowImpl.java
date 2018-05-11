package fish.payara.microprofile.openapi.impl.model.security;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.Scopes;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class OAuthFlowImpl extends ExtensibleImpl implements OAuthFlow {

    protected String authorizationUrl;
    protected String tokenUrl;
    protected String refreshUrl;
    protected Scopes scopes;

    @Override
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    @Override
    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public OAuthFlow authorizationUrl(String authorizationUrl) {
        setAuthorizationUrl(authorizationUrl);
        return this;
    }

    @Override
    public String getTokenUrl() {
        return tokenUrl;
    }

    @Override
    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    @Override
    public OAuthFlow tokenUrl(String tokenUrl) {
        setTokenUrl(tokenUrl);
        return this;
    }

    @Override
    public String getRefreshUrl() {
        return refreshUrl;
    }

    @Override
    public void setRefreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
    }

    @Override
    public OAuthFlow refreshUrl(String refreshUrl) {
        setRefreshUrl(refreshUrl);
        return this;
    }

    @Override
    public Scopes getScopes() {
        return scopes;
    }

    @Override
    public void setScopes(Scopes scopes) {
        this.scopes = scopes;
    }

    @Override
    public OAuthFlow scopes(Scopes scopes) {
        setScopes(scopes);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.security.OAuthFlow from, OAuthFlow to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setTokenUrl(mergeProperty(to.getTokenUrl(), from.tokenUrl(), override));
        if (from.scopes() != null) {
            Scopes scopes = new ScopesImpl();
            for (OAuthScope scope : from.scopes()) {
                scopes.addScope(scope.name(), scope.description());
            }
            to.setScopes(mergeProperty(to.getScopes(), scopes, override));
        }
        to.setRefreshUrl(mergeProperty(to.getRefreshUrl(), from.refreshUrl(), override));
        to.setAuthorizationUrl(mergeProperty(to.getAuthorizationUrl(), from.authorizationUrl(), override));
    }

}
