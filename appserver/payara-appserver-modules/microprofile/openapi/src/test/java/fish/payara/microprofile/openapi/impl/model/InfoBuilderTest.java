package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createContact;
import static org.eclipse.microprofile.openapi.OASFactory.createLicense;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.info.InfoImpl},
 * {@link fish.payara.microprofile.openapi.impl.model.info.ContactImpl} and
 * {@link fish.payara.microprofile.openapi.impl.model.info.LicenseImpl}.
 */
public class InfoBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getInfo() // info created by base class
            .description("description")
            .termsOfService("termsOfService")
            .addExtension("x-ext", "ext-value");
        document.getInfo().contact(createContact()
            .name("name")
            .email("email")
            .url("url")
            .addExtension("x-ext", "ext-value"));
        document.getInfo().license(createLicense()
            .name("name")
            .url("url")
            .addExtension("x-ext", "ext-value"));
    }

    @Test
    public void infoHasExpectedFields() {
        JsonNode info = getOpenAPIJson().get("info");
        assertNotNull(info);
        assertEquals(7, info.size());
        assertEquals("title", info.get("title").textValue());
        assertEquals("version", info.get("version").textValue());
        assertEquals("termsOfService", info.get("termsOfService").textValue());
        assertEquals("description", info.get("description").textValue());
        assertEquals("ext-value", info.get("x-ext").textValue());
    }

    @Test
    public void contactHasExpectedFields() {
        JsonNode contact = path(getOpenAPIJson(), "info.contact");
        assertNotNull(contact);
        assertEquals(4, contact.size());
        assertEquals("name", contact.get("name").textValue());
        assertEquals("url", contact.get("url").textValue());
        assertEquals("email", contact.get("email").textValue());
        assertEquals("ext-value", contact.get("x-ext").textValue());
    }

    @Test
    public void licenseHasExpectedFields() {
        JsonNode license = path(getOpenAPIJson(), "info.license");
        assertNotNull(license);
        assertEquals(3, license.size());
        assertEquals("name", license.get("name").textValue());
        assertEquals("url", license.get("url").textValue());
        assertEquals("ext-value", license.get("x-ext").textValue());
    }
}
