package fish.payara.functional.micro;

import com.microsoft.playwright.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ClusterHAJSPPage {

    static public Page openNewPage(String url) {
        Playwright playwright = Playwright.create();
        // to disable the headless mode, use playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false))
        Browser browser = playwright.chromium().launch();
        BrowserContext context = browser.newContext();
        Page page = context.newPage();
        page.navigate(url);
        page.waitForLoadState();
        assertThat(page).hasTitle("Cluster - Ha JSP Sample ");
        return page;
    }

    static public void openNewUrl(Page page, String url) {
        page.navigate(url);
        page.waitForLoadState();
        assertThat(page).hasTitle("Cluster - Ha JSP Sample ");
    }

    static public void enterNameAttribute(Page page, String value) {
        Locator textInputDataName = page.locator("input[name='dataName']");
        textInputDataName.fill(value);
    }

    static public void enterValueAttribute(Page page, String value) {
        Locator textInputDataValue = page.locator("input[name='dataValue']");
        textInputDataValue.fill(value);
    }

    static public void addSessionData(Page page) {
        Locator buttonAddSessionData = page.locator("input[value='ADD SESSION DATA']");
        buttonAddSessionData.click();
    }

    static public void reloadPage(Page page) {
        Locator buttonAddSessionData = page.locator("input[value='RELOAD PAGE']");
        buttonAddSessionData.click();
        page.waitForLoadState();
    }

    static public void clearSession(Page page) {
        Locator buttonAddSessionData = page.locator("input[value='CLEAR SESSION']");
        buttonAddSessionData.click();
    }

    static public String readDataHttpSession(Page page) {
        Locator dataHttpSession = page.locator("body > ul:nth-child(12)");
        dataHttpSession.allInnerTexts();
        return dataHttpSession.allInnerTexts().toString();
    }
}
