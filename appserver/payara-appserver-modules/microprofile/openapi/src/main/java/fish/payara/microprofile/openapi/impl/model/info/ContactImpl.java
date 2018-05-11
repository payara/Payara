package fish.payara.microprofile.openapi.impl.model.info;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.models.info.Contact;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class ContactImpl extends ExtensibleImpl implements Contact {

    protected String name;
    protected String url;
    protected String email;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Contact name(String name) {
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
    public Contact url(String url) {
        setUrl(url);
        return this;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public Contact email(String email) {
        setEmail(email);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.info.Contact from, Contact to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setName(mergeProperty(to.getName(), from.name(), override));
        to.setUrl(mergeProperty(to.getUrl(), from.url(), override));
        to.setEmail(mergeProperty(to.getEmail(), from.email(), override));
    }

}
