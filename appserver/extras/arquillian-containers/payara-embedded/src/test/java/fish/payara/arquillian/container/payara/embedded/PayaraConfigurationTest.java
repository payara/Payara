// Portions Copyright [2016-2017] [Payara Foundation]
package fish.payara.arquillian.container.payara.embedded;

import org.junit.Test;

import fish.payara.arquillian.container.payara.embedded.PayaraConfiguration;

import static org.junit.Assert.assertTrue;

public class PayaraConfigurationTest {

    @Test
    public void testValidInstallRoot() throws Exception {
        String installRoot = "./src/test/resources/gfconfigs/installRoot";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstallRoot(installRoot);
        config.validate();
    }

    @Test(expected = RuntimeException.class)
    public void testInstallRootWithoutDirectories() throws Exception {
        String installRoot = "./src/test/resources/gfconfigs/";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstallRoot(installRoot);
        config.validate();
    }

    @Test
    public void testValidInstanceRoot() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/instanceRoot";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.validate();
    }

    @Test(expected = RuntimeException.class)
    public void testInstanceRootWithoutDirectories() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/emptydir";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.validate();
    }

    @Test
    public void testInstanceRootWithoutConfigXml() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/instanceRootNoConfigxml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.validate();
    }

    @Test
    public void testValidConfigXmlPath() throws Exception {
        String configXml = "./src/test/resources/gfconfigs/configxml/test-domain.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setConfigurationXml(configXml);
        config.validate();

        assertTrue(config.getConfigurationXml().startsWith("file:"));
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidConfigXmlPath() throws Exception {
        String configXml = "./src/test/resources/gfconfigs/emptydir/test-domain.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setConfigurationXml(configXml);
        config.validate();
    }

    @Test
    public void testInstanceRootAndConfigXml() throws Exception {
        String instanceRoot = "./src/test/resources/gfconfigs/instanceRoot";
        String configXml = "./src/test/resources/gfconfigs/configxml/test-domain.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setInstanceRoot(instanceRoot);
        config.setConfigurationXml(configXml);
        config.validate();

        assertTrue(config.getConfigurationXml().startsWith("file:"));
    }

    @Test
    public void testValidResourcesXmlPath() throws Exception {
        String resourcesXml = "./src/test/resources/gfconfigs/resourcesxml/glassfish-resources.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setResourcesXml(resourcesXml);
        config.validate();
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidResourcesXmlPath() throws Exception {
        String resourcesXml = "./src/test/resources/gfconfigs/emptydir/glassfish-resources.xml";

        PayaraConfiguration config = new PayaraConfiguration();
        config.setResourcesXml(resourcesXml);
        config.validate();
    }
}
