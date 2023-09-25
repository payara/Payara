package fish.payara.functional.micro;

import com.microsoft.playwright.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import org.jboss.arquillian.container.test.api.Deployment;

import fish.payara.samples.PayaraArquillianTestRunner;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PayaraArquillianTestRunner.class)
public class DualSessionsIT {

    public static WebArchive createClusterJsp() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "clusterjsp.war")
                .addAsWebInfResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/WEB-INF/sun-web.xml"))
                .addAsWebInfResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/WEB-INF/web.xml"))
                .addAsWebResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/HaJsp.jsp"))
                .addAsWebResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/ClearSession.jsp"))
                .addAsManifestResource(new File("src/test/resources/clusterjsp/clusterjsp-war/web/MANIFEST.MF"));
        return archive;
    }

    // Deployment with testable = false as otherwise, Arquillian would add its own libraries to the war
    // including the library arquillian-jakarta-servlet-protocol.jar, preventing clusterjsp to work as intended
    @Deployment(name = "microInstance1", order = 1, testable = false)
    @TargetsContainer("micro1")
    public static WebArchive microInstance1() {
        return createClusterJsp();
    }

    @ArquillianResource
    @OperateOnDeployment("microInstance1")
    private URL deployment1URL;

    @Deployment(name = "microInstance2", order = 2, testable = false)
    @TargetsContainer("micro2")
    public static WebArchive microInstance2() {
        return createClusterJsp();
    }

    @ArquillianResource
    @OperateOnDeployment("microInstance2")
    private URL deployment2URL;


    @Test
    @RunAsClient
    public void addAndReadAttributes() throws MalformedURLException, InterruptedException {
        try (Page page = ClusterHAJSPPage.openNewPage(deployment1URL.toString())) {
            ClusterHAJSPPage.enterNameAttribute(page, "attribute1");
            ClusterHAJSPPage.enterValueAttribute(page, "value1");
            ClusterHAJSPPage.addSessionData(page);
            String results = ClusterHAJSPPage.readDataHttpSession(page);
            assertThat(results, containsString("attribute1 = value1"));
            System.out.println("attribute1 found in instance1");

            ClusterHAJSPPage.openNewUrl(page, deployment2URL.toString());
            ClusterHAJSPPage.enterNameAttribute(page, "attribute2");
            ClusterHAJSPPage.enterValueAttribute(page, "value2");
            ClusterHAJSPPage.addSessionData(page);
            String results2 = ClusterHAJSPPage.readDataHttpSession(page);
            assertThat(results2, containsString("attribute1 = value1"));
            System.out.println("attribute1 found in instance2");

            ClusterHAJSPPage.openNewUrl(page, deployment1URL.toString());
            ClusterHAJSPPage.reloadPage(page);
            String results3 = ClusterHAJSPPage.readDataHttpSession(page);
            assertThat(results3, containsString("attribute1 = value1"));
            System.out.println("attribute1 found in instance1");
            assertThat(results3, containsString("attribute2 = value2"));
            System.out.println("attribute2 found in instance1");
        }
    }

}
