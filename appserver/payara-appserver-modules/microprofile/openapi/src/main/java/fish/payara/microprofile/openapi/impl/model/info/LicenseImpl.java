package fish.payara.microprofile.openapi.impl.model.info;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.models.info.License;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class LicenseImpl extends ExtensibleImpl implements License {

    protected String name;
    protected String url;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public License name(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public License url(String url) {
        setUrl(url);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.info.License from, License to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setName(mergeProperty(to.getName(), from.name(), override));
        to.setUrl(mergeProperty(to.getUrl(), from.url(), override));
    }

}
