package fish.payara.microprofile.openapi.impl.model.info;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class InfoImpl extends ExtensibleImpl implements Info {

    protected String title;
    protected String description;
    protected String termsOfService;
    protected Contact contact;
    protected License license;
    protected String version;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Info title(String title) {
        setTitle(title);
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
    public Info description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public String getTermsOfService() {
        return termsOfService;
    }

    @Override
    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    @Override
    public Info termsOfService(String termsOfService) {
        setTermsOfService(termsOfService);
        return this;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    public Info contact(Contact contact) {
        setContact(contact);
        return this;
    }

    @Override
    public License getLicense() {
        return license;
    }

    @Override
    public void setLicense(License license) {
        this.license = license;
    }

    @Override
    public Info license(License license) {
        setLicense(license);
        return this;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Info version(String version) {
        setVersion(version);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.info.Info from, Info to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setTitle(mergeProperty(to.getTitle(), from.title(), override));
        to.setVersion(mergeProperty(to.getVersion(), from.version(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setTermsOfService(mergeProperty(to.getTermsOfService(), from.termsOfService(), override));
        if (!isAnnotationNull(from.license())) {
            if (to.getLicense() == null) {
                to.setLicense(new LicenseImpl());
            }
            LicenseImpl.merge(from.license(), to.getLicense(), override);
        }
        if (!isAnnotationNull(from.contact())) {
            if (to.getContact() == null) {
                to.setContact(new ContactImpl());
            }
            ContactImpl.merge(from.contact(), to.getContact(), override);
        }
    }

}
