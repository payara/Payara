/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fish.payara.functional.cleanboot;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

public class AdminGroup {

    static public void goToDeploymentGroupPage(Page page) {
        // Click on the option Instances in the menu
        Locator groupButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Deployment Groups").setExact(true));
        groupButton.click();
        page.waitForSelector("div[id='propertyForm:dgTable']");

        // Expect the title to contain Instances
        assertThat(page).hasTitle("Deployment Groups");
    }

    static public void waitForProcess(Page page) {
        // wait for modal to appear and disappear
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

    static public void createGroup(Page page, String nameGroup, String[] nameInstances) {

        System.out.println("Create the deployment group " + nameGroup);

        AdminPage.gotoHomepage(page);
        goToDeploymentGroupPage(page);

        // Create new instance
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New...")).click();
        page.waitForSelector("input[id='propertyForm:propertySheet:propertySectionTextField:NameTextProp:NameText']");
        page.getByRole(AriaRole.TEXTBOX).fill(nameGroup);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("OK")).click();
        page.waitForSelector("input[value='New...']");

        //Check for the presence of the new instance in the table
        Locator groupLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(nameGroup).setExact(true));
        assertThat(groupLink).isVisible();
        groupLink.click();
        page.waitForSelector("table.Tab1TblNew_sun4");

        //Create instances in the group
        for (String nameInstance : nameInstances) {
            Locator groupTabs = page.locator("table.Tab1TblNew_sun4");
            Locator groupInstanceTab = groupTabs.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Instances").setExact(true));
            groupInstanceTab.click();

            //Create new instance
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New...")).click();
            page.waitForSelector("input[id='propertyForm:propertySheet:propertSectionTextField:NameTextProp:NameText']");
            page.getByRole(AriaRole.TEXTBOX).fill(nameInstance);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("OK")).click();
            page.waitForSelector("input[value=' Save ']");

            // Check for the presence of the new instance in the table
            Locator instanceLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(nameInstance).setExact(true));
            assertThat(instanceLink).isVisible();

            Locator groupGeneralTab = groupTabs.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("General").setExact(true));
            groupGeneralTab.click();
        }

    }

    static public void startGroups(Page page) {

        System.out.println("Start the deployment groups");

        AdminPage.gotoHomepage(page);
        goToDeploymentGroupPage(page);

        acceptDialog(page);

	Locator selectAllButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Select All").setExact(true));
        selectAllButton.click();
        Locator startButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Start Deployment Group"));
        startButton.click();

        waitForProcess(page);
    }

    static public void stopGroups(Page page) {
        AdminPage.gotoHomepage(page);
        goToDeploymentGroupPage(page);

        acceptDialog(page);

        //Select all groups and Stop
        Locator selectAllButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Select All").setExact(true));
        selectAllButton.click();
        Locator stopButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Stop Deployment Group"));
        stopButton.click();

        waitForProcess(page);
    }

    static public void deleteGroups(Page page) {
        AdminPage.gotoHomepage(page);
        goToDeploymentGroupPage(page);

        acceptDialog(page);

        //Delete all groups
        Locator selectAllButton = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Select All").setExact(true));
        selectAllButton.click();
        Locator deleteButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete"));
        deleteButton.click();

        waitForProcess(page);
    }
}
