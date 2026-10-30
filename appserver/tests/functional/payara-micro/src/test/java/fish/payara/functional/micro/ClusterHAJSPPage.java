/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
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
