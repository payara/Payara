package fish.payara.microprofile.openapi.impl.model.security;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class SecuritySchemeImpl extends ExtensibleImpl implements SecurityScheme {

    protected SecurityScheme.Type type;
    protected String description;
    protected String name;
    protected String schemeName;
    protected String ref;

    protected SecurityScheme.In in;
    protected String scheme;
    protected String bearerFormat;
    protected OAuthFlows flows;
    protected String openIdConnectUrl;

    @Override
    public SecurityScheme.Type getType() {
        return type;
    }

    @Override
    public void setType(SecurityScheme.Type type) {
        this.type = type;
    }

    @Override
    public SecurityScheme type(SecurityScheme.Type type) {
        setType(type);
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
    public SecurityScheme description(String description) {
        setDescription(description);
        return this;
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
    public SecurityScheme name(String name) {
        setName(name);
        return this;
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
    public SecurityScheme in(SecurityScheme.In in) {
        setIn(in);
        return this;
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
    public SecurityScheme scheme(String scheme) {
        setScheme(scheme);
        return this;
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
    public SecurityScheme bearerFormat(String bearerFormat) {
        setBearerFormat(bearerFormat);
        return this;
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
    public SecurityScheme flows(OAuthFlows flows) {
        setFlows(flows);
        return this;
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
    public SecurityScheme openIdConnectUrl(String openIdConnectUrl) {
        setOpenIdConnectUrl(openIdConnectUrl);
        return this;
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

    @Override
    public SecurityScheme ref(String ref) {
        setRef(ref);
        return this;
    }

    @JsonIgnore
    public String getSchemeName() {
        return schemeName;
    }

    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.security.SecurityScheme from,
            SecurityScheme to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setName(mergeProperty(to.getName(), from.apiKeyName(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setScheme(mergeProperty(to.getScheme(), from.scheme(), override));
        if (from.in() != null && from.in() != SecuritySchemeIn.DEFAULT) {
            to.setIn(mergeProperty(to.getIn(), In.valueOf(from.in().name()), override));
        }
        if (from.type() != null && from.type() != SecuritySchemeType.DEFAULT) {
            to.setType(mergeProperty(to.getType(), Type.valueOf(from.type().name()), override));
        }
        if (from.flows() != null) {
            OAuthFlows flows = new OAuthFlowsImpl();
            OAuthFlowsImpl.merge(from.flows(), flows, override);
            to.setFlows(mergeProperty(to.getFlows(), flows, override));
        }
    }

}
