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
