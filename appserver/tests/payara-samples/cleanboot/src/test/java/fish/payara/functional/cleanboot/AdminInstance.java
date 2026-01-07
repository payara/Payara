package fish.payara.functional.cleanboot;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.List;

public class AdminInstance {

    static public void goToInstancePage(Page page) {
        //Click on the option Instances in the menu
        Locator instanceButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Instances").setExact(true));
        instanceButton.click();
        page.waitForSelector("input[value=' Save ']");

        //Expect the title to contain Instances
        assertThat(page).hasTitle("Payara Server Instances");
    }

    static public void waitForProcess(Page page) {
        //Wait for modal to appear and disappear
        page.waitForSelector("input[value='Processing...']");
        //page.waitForSelector("div#ajaxPanelBody");
        page.waitForSelector("input[value='Processing...']",
                new Page.WaitForSelectorOptions().setTimeout(120000).setState(WaitForSelectorState.HIDDEN));
        page.waitForSelector("div#ajaxPanelBody",
                new Page.WaitForSelectorOptions().setTimeout(120000).setState(WaitForSelectorState.HIDDEN));

    }

    static public void acceptDialog(Page page) {
        //Confirm the next dialog window
        page.onceDialog(dialog -> {
            dialog.accept();
        });
    }

    static public void dismissDialog(Page page) {
        //Confirm the next dialog window
        page.onceDialog(dialog -> {
            dialog.dismiss();
        });
    }

    static public void createInstance(Page page, String nameInstance) {

        System.out.println("Create the instance " + nameInstance);

        AdminPage.gotoHomepage(page);
        goToInstancePage(page);

        //Create new instance
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New...")).click();
        page.waitForSelector("input[id='propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText']");
	page.getByRole(AriaRole.TEXTBOX).fill(nameInstance);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("OK")).click();
        page.waitForSelector("input[value=' Save ']");

        //Check for the presence of the new instance in the table
	Locator instanceLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(nameInstance).setExact(true));
        assertThat(instanceLink).isVisible();
    }

    static public void startInstance(Page page, String nameInstance) {

        System.out.println("Start the instance " + nameInstance);

        AdminPage.gotoHomepage(page);
        goToInstancePage(page);

        //Open the page of the instance to start it
	Locator instanceLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(nameInstance).setExact(true));
        instanceLink.click();
        page.waitForSelector("div[id='propertyForm:propertyContentPage']");

        //Confirm the dialog window
	acceptDialog(page);

        Locator startButton = page.locator("input[value='Start']");
        startButton.click();

        waitForProcess(page);
    }

    static public void startInstances(Page page) {

        System.out.println("Start the instances");

        AdminPage.gotoHomepage(page);
        goToInstancePage(page);

        //Select all instances and start
        Locator selectAllButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Select All").setExact(true));
        selectAllButton.click();

        //Confirm the dialog window
        acceptDialog(page);

        Locator startButton = page.locator("input[value='Start']");
        startButton.click();

        waitForProcess(page);
    }

    static public void stopInstances(Page page) {

        System.out.println("Stop the instances");

        AdminPage.gotoHomepage(page);
        goToInstancePage(page);

        //Confirm the dialog window
        acceptDialog(page);

        //Select all instances and start
        Locator selectAllButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Select All").setExact(true));
        selectAllButton.click();
        Locator stopButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Stop"));
        stopButton.click();

        waitForProcess(page);
    }

    static public void deleteInstances(Page page) {

        System.out.println("Delete the instances");

        AdminPage.gotoHomepage(page);
        goToInstancePage(page);

        //Confirm the dialog window
        acceptDialog(page);

        //Delete all instances
        Locator selectAllButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Select All").setExact(true));
        selectAllButton.click();
        Locator deleteButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete"));
        deleteButton.click();

        waitForProcess(page);
}

    static public String collectLogs(Page page, String nameInstance, String[] logLevels) {

        System.out.println("Collect the logs from " + nameInstance);

        String logs = nameInstance + " : \n";

        AdminPage.gotoHomepage(page);
        goToInstancePage(page);

        //Open the page of the instance and open the logs
        Locator instanceLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(nameInstance).setExact(true));
        instanceLink.click();
        page.waitForSelector("div[id='propertyForm:propertyContentPage']");
        Locator viewLogButton = page.locator("input[value='View Log Files']");

        try (
                Page logPage = page.waitForPopup(() -> {
                    viewLogButton.click();
                })) {
            logPage.waitForLoadState();

            logPage.waitForSelector("div[id='propertyForm:basicTable']");

            Locator logLevelCombobox = logPage.locator("select[id='propertyForm:propertyContentPage:propertySheet:propertSectionTextField:logLevelProp:logLevel']");
            logLevelCombobox.click();
            for (String logLevel : logLevels) {
                //Change the log level to the desired value and filter logs on that level
                Locator logLevelSelector = logPage.getByLabel("Log Level:");
                logLevelSelector.selectOption(logLevel, new Locator.SelectOptionOptions().setForce(true));
                Locator searchButton = logPage.locator("input[id='propertyForm:propertyContentPage:bottomButtons:searchButtonBottom']");
                searchButton.click();
                logPage.waitForLoadState();
                //Create list of every details buttons displayed
                List<Locator> detailsButtons = logPage.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("(details)")).all();
                for (Locator detailsButton : detailsButtons) {
                    try (Page detailPageEvent = logPage.waitForPopup(() -> {
                        detailsButton.click();
                    })) {
                        detailPageEvent.waitForLoadState();
                        String logEntryLevel = detailPageEvent.locator("span[id*='logLevel']").textContent();
                        List<Locator> logEntryMessages = detailPageEvent.locator("span[id*='completeMessage']").all();
                        if (!logEntryMessages.isEmpty()) {
                            String logEntryMessage = detailPageEvent.locator("span[id*='completeMessage']").textContent();
                            logs = logs.concat(logEntryLevel + " - " + logEntryMessage + " \n");
                        }
                    }
                }
            }
        }
        logs = logs.concat(" \n");
        return logs;
    }
}
