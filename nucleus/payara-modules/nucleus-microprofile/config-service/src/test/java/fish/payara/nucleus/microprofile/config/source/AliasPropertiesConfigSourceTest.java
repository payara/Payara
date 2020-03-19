package fish.payara.nucleus.microprofile.config.source;

import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class AliasPropertiesConfigSourceTest {

    @Test
    public void testGetAliasedSystemProperty() {
        Properties properties = new Properties();
        properties.put("fish.payara.alias.system", "${java.home}");
        AliasPropertiesConfigSource config = new AliasPropertiesConfigSource(true, properties);

        Assert.assertEquals(System.getProperty("java.home"), config.getValue("fish.payara.alias.system"));
    }

    @Test
    public void testGetAliasedEnvironmentProperty() {
        Properties properties = new Properties();
        properties.put("fish.payara.alias.env", "${ENV=PATH}");
        AliasPropertiesConfigSource config = new AliasPropertiesConfigSource(true, properties);

        Assert.assertEquals(System.getenv().get("PATH"), config.getValue("fish.payara.alias.env"));
    }

    @Test
    public void testGetUndefinedProperty() {
        AliasPropertiesConfigSource config = new AliasPropertiesConfigSource(true, new Properties());

        Assert.assertNull(config.getValue("${java.home}"));
        Assert.assertNull(config.getValue("java.home"));
        Assert.assertNull(config.getValue("wibble"));
    }

    /**
     * Test to check
     */
    @Test
    public void testGetIncorrectProperty() {
        Properties properties = new Properties();
        properties.put("fish.payara.alias.incorrect", "java.home");
        properties.put("fish.payara.alias.incorrect.system", "{wibbly.wobbly.timey.wimey}");
        properties.put("fish.payara.alias.incorrect.env", "${ENV=wibbly.wobbly.timey.wimey}");
        AliasPropertiesConfigSource config = new AliasPropertiesConfigSource(true, properties);

        Assert.assertNull(config.getValue("fish.payara.alias.incorrect"));
        Assert.assertNull(config.getValue("fish.payara.alias.incorrect.system"));
        Assert.assertNull(config.getValue("fish.payara.alias.incorrect.env"));
    }

}
