package fish.payara.functional.cleanboot;

import com.microsoft.playwright.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.NotMicroCompatible;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import java.sql.Timestamp;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class CleanBootIT {

    static private Page page;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = PayaraTestShrinkWrap.getWebArchive();
        return archive;
    }

    @BeforeClass
    static public void openPage() {
        //Load the Admin Console
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        page = browser.newPage();
        page.navigate("http://localhost:4848/");
        page.waitForSelector("table[role='presentation']", new Page.WaitForSelectorOptions().setTimeout(120000));
    }

    @AfterClass
    static public void closePage() {
        page.close();
    }

    @Test
    @RunAsClient
    public void openAdminConsole() {
        AdminPage.gotoHomepage(page);
        assertThat(page).hasTitle("Payara Server Console - Common Tasks");
    }

    @Test
    @RunAsClient
    public void createInstanceTest() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String logs = timestamp.toString() + " \n";

        AdminInstance.createInstance(page, "testInstance1");
        AdminInstance.startInstance(page, "testInstance1");

        String[] logLevels = {"WARNING", "SEVERE", "ALERT", "EMERGENCY"};
        logs = logs.concat(AdminInstance.collectLogs(page, "testInstance1", logLevels));

        AdminInstance.stopInstances(page);
        AdminInstance.deleteInstances(page);

        timestamp = new Timestamp(System.currentTimeMillis());
        logs = logs.concat(timestamp.toString()) + " \n";
        System.out.println(logs);
        boolean containsError = logs.contains("SEVERE") || logs.contains("ALERT") || logs.contains("EMERGENCY");
        if (containsError) {
            Assert.fail("Logs contain SEVERE or ALERT or EMERGENCY");
        }
    }

    @Test
    @RunAsClient
    public void createDeploymentGroupTest() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String logs = timestamp.toString() + " \n";
        String[] instances = {"groupInstance1", "groupInstance2"};

        AdminGroup.createGroup(page, "group1", instances);
        AdminGroup.startGroups(page);

        String[] logLevels = {"WARNING", "SEVERE", "ALERT", "EMERGENCY"};
        logs = logs.concat(AdminInstance.collectLogs(page, "groupInstance1", logLevels));
        logs = logs.concat(AdminInstance.collectLogs(page, "groupInstance2", logLevels));

        AdminGroup.stopGroups(page);
        AdminGroup.deleteGroups(page);
        AdminInstance.deleteInstances(page);

        timestamp = new Timestamp(System.currentTimeMillis());
        logs = logs.concat(timestamp.toString()) + " \n";
        System.out.println(logs);
        boolean containsError = logs.contains("SEVERE") || logs.contains("ALERT") || logs.contains("EMERGENCY");
        if (containsError) {
            Assert.fail("Logs contain SEVERE or ALERT or EMERGENCY");
        }
    }
}
