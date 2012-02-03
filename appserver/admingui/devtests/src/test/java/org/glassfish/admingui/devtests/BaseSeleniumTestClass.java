/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package org.glassfish.admingui.devtests;

import com.google.common.base.Function;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;
import org.junit.*;
import org.openqa.selenium.*;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.admingui.common.util.RestUtil;
import org.glassfish.admingui.devtests.util.ElementFinder;
import org.glassfish.admingui.devtests.util.SeleniumHelper;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BaseSeleniumTestClass {
    public static final String CURRENT_WINDOW = "selenium.browserbot.getCurrentWindow()";
    public static final int TIMEOUT_CALLBACK_LOOP = 1000;
    public static final String TRIGGER_NEW_VALUES_SAVED = "New values successfully saved.";
    public static final String TRIGGER_COMMON_TASKS = "Other Tasks";
    public static final String TRIGGER_REGISTRATION_PAGE = "Receive patch information and bug updates, screencasts and tutorials, support and training offerings, and more";
    public static final String TRIGGER_ERROR_OCCURED = "An error has occurred";
    public static final boolean DEBUG = Boolean.parseBoolean(SeleniumHelper.getParameter("debug", "false"));
    @Rule
    public SpecificTestRule specificTestRule = new SpecificTestRule();
    protected static final int TIMEOUT = 90;
    protected static final int BUTTON_TIMEOUT = 750;
    protected static final Logger logger = Logger.getLogger(BaseSeleniumTestClass.class.getName());
    protected static Selenium selenium;
    protected static WebDriver driver;
    private static String currentTestClass = "";
    private boolean processingLogin = false;
    private static final String AJAX_INDICATOR = "ajaxIndicator";
    private static final Map<String, String> bundles = new HashMap<String, String>() {

        {
            put("i18n", "org.glassfish.admingui.core.Strings"); // core
            put("i18nUC", "org.glassfish.updatecenter.admingui.Strings"); // update center
            put("i18n_corba", "org.glassfish.corba.admingui.Strings");
            put("i18n_ejb", "org.glassfish.ejb.admingui.Strings");
            put("i18n_ejbLite", "org.glassfish.ejb-lite.admingui.Strings");
            put("i18n_jts", "org.glassfish.jts.admingui.Strings"); // JTS
            put("i18n_web", "org.glassfish.web.admingui.Strings"); // WEB
            put("common", "org.glassfish.common.admingui.Strings");
            put("i18nc", "org.glassfish.common.admingui.Strings"); // common -- apparently we use both in the app :|
            put("i18nce", "org.glassfish.admingui.community-theme.Strings");
            put("i18ncs", "org.glassfish.cluster.admingui.Strings"); // cluster
            put("i18njca", "org.glassfish.jca.admingui.Strings"); // JCA
            put("i18njdbc", "org.glassfish.jdbc.admingui.Strings"); // JDBC
            put("i18njmail", "org.glassfish.full.admingui.Strings");
            put("i18njms", "org.glassfish.jms.admingui.Strings"); // JMS
            put("theme", "org.glassfish.admingui.community-theme.Strings");

            // TODO: These conflict with core and should probably be changed in the pages
            //put("i18n", "org.glassfish.common.admingui.Strings");
            //put("i18n", "org.glassfish.web.admingui.Strings");
            //put("i18nc", "org.glassfish.web.admingui.Strings");
        }
    };
    private static final SeleniumHelper helper = SeleniumHelper.getInstance();
    private ElementFinder elementFinder;

    public BaseSeleniumTestClass() {
        driver = helper.getDriver();
        selenium = helper.getSeleniumInstance();
        elementFinder = helper.getElementFinder();

        if (Boolean.parseBoolean(SeleniumHelper.getParameter("slowDown", "false"))) {
            driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        if (!DEBUG) {
            RestUtil.post(helper.getBaseUrl() + "/management/domain/rotate-log", new HashMap<String, Object>());
        }
    }

    @AfterClass
    public static void captureLog() {
        try {
            helper.releaseSeleniumInstance();

            if (!currentTestClass.isEmpty() && !DEBUG) {
                URL url = new URL("http://localhost:" + SeleniumHelper.getParameter("admin.port", "4848") + "/management/domain/view-log");
                InputStream is = url.openStream();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("target/surefire-reports/" + currentTestClass + "-server.log")));
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line = in.readLine();
                while (line != null) {
                    out.write(line + System.getProperty("line.separator"));
                    line = in.readLine();
                }
                in.close();
                out.close();
            }
        } catch (FileNotFoundException fnfe) {
            //
        } catch (Exception ex) {
            Logger.getLogger(BaseSeleniumTestClass.class.getName()).log(Level.INFO, null, ex);
        }
    }

    @Before
    public void reset() {
        selenium = helper.getSeleniumInstance();
        currentTestClass = this.getClass().getName();
        openAndWait("/", TRIGGER_COMMON_TASKS);
    }

    @After
    public void afterTest() {
        if (Boolean.parseBoolean(SeleniumHelper.getParameter("releaseAfter", "false"))) {
            helper.releaseSeleniumInstance();
        }
    }

    // *************************************************************************
    // Wrappers for Selenium API
    // *************************************************************************
    /**
     * Returns the current value for the specified field
     * @see DefaultSelenium.getValue(String)
     * @param elem
     * @return
     */
    public String getFieldValue(String elem) {
        return selenium.getValue(elem);
    }
    /**
     * Types the specified text into the requested element
     * @param elem
     * @param text
     */
    public void setFieldValue(String elem, String text) {
        selenium.focus(elem);
        selenium.type(elem, text);
    }

    /**
     * Gets the text of an element.
     * @see DefaultSelenium.getText(String)
     * @param elem
     * @return
     */
    public String getText(String elem) {
        waitForElement(elem);
        return selenium.getText(elem);
    }

    /**
     * Deelects (unchecks) the specified checkbox.  After calling this method, the
     * checkbox will be unchecked regardless of its initial state.
     * @param elem
     */
    public void markCheckbox(String elem) {
        waitForElement(elem);
        selenium.check(elem);
    }

    /**
     * Selects (checks) the specified checkbox.  After calling this method, the
     * checkbox will be checked regardless of its initial state.
     * @param elem
     */
    public void clearCheckbox(String elem) {
        waitForElement(elem);
        selenium.uncheck(elem);
    }

    public void pressButton(final String button) {
        waitForElement(button);
        new ExceptionSwallowingLoop<Void>() {
            @Override
            public Void operation() {
                selenium.click(button);
                return null;
            }
        }.get();
    }

    /**
     * Return the selected value of the specified select element
     * @param elem
     * @return
     */
    public String getSelectedValue(String elem) {
        waitForElement(elem);
        return selenium.getSelectedValue(elem);
    }

    /**
     * Returns true is the specified element is present on the page
     * @param elem
     * @return
     */
    public boolean isElementPresent(String elem) {
        return selenium.isElementPresent(elem);
    }

    protected String captureScreenshot() {
        return SeleniumHelper.captureScreenshot();
    }
    
    /**
     * Select the option requested in the given select element
     * @param id
     * @param label
     */
    protected void selectDropdownOption(String id, String label) {
        try {
            label = resolveTriggerText(label);
            selenium.select(id, "label="+label);
        } catch (SeleniumException se) {
            try {
                selenium.select(id, "value=" + label);
            } catch (SeleniumException se1) {
                logger.log(Level.INFO, "An invalid option was requested.  Here are the valid options:");
                for (String option : selenium.getSelectOptions(id)) {
                    logger.log(Level.INFO, "\t{0}", option);
                }
                throw se1;
            }
        }
    }

    /**
     * Add a selection to the given select element
     * @param elem
     * @param label
     */
    protected void addSelectSelection(String elem, String label) {
        try {
            label = resolveTriggerText(label);
            selenium.addSelection(elem, "label="+label);
        } catch (SeleniumException se) {
            logger.info("An invalid option was requested.  Here are the valid options:");
            for (String option : selenium.getSelectOptions(elem)) {
                logger.log(Level.INFO, "\t{0}", option);
            }
            throw se;
        }
    }

    /**
     * Returns true if the specified checkbox is selected
     * @param elem
     * @return
     */
    protected boolean isChecked(String elem) {
        waitForElement(elem);
        return selenium.isChecked(elem);
    }

    protected void selectFile(String uploadElement, String archivePath) {
        waitForElement(uploadElement);
        selenium.attachFile(uploadElement, archivePath);
    }

    protected boolean isAlertPresent() {
        return selenium.isAlertPresent();
    }

    protected boolean isConfirmationPresent() {
        return selenium.isConfirmationPresent();
    }

    protected String getAlertText() {
        return selenium.getAlert();
    }

    protected void chooseOkOnNextConfirmation() {
        selenium.chooseOkOnNextConfirmation();
    }

    protected String getConfirmation() {
        String confirmation = null;
        if (isConfirmationPresent()) {
            confirmation = selenium.getConfirmation();
        }
        return confirmation;
    }

    protected void waitForPopUp(String windowId, String timeout) {
        selenium.waitForPopUp(windowId, timeout);
    }

    protected void selectWindow(String windowId) {
    	selenium.selectWindow(windowId);
    }
    
    protected String getSelectedLabel(String elem) {
        waitForElement(elem);
        return selenium.getSelectedLabel(elem);
    }

    protected void open(String url) {
        selenium.open(url);
    }

    protected void submitForm(String formId) {
        selenium.submit(formId);
    }

    // *************************************************************************
    // Wrappers for Selenium API
    // *************************************************************************

    protected String generateRandomString() {
        SecureRandom random = new SecureRandom();

        // prepend a letter to insure valid JSF ID, which is causing failures in some areas
        return "a" + new BigInteger(130, random).toString(16);
    }

    protected int generateRandomNumber() {
        Random r = new Random();
        return Math.abs(r.nextInt()) + 1;
    }

    protected int generateRandomNumber(int max) {
        Random r = new Random();
        return Math.abs(r.nextInt(max - 1)) + 1;
    }

    protected <T> T selectRandomItem(T... items) {
        Random r = new Random();

        return items[r.nextInt(items.length)];
    }

    protected int getTableRowCount(String id) {
        String text = getText(id);
        int count = Integer.parseInt(text.substring(text.indexOf("(") + 1, text.indexOf(")")));

        return count;
    }

    protected void openAndWait(String url, String triggerText) {
        openAndWait(url, triggerText, TIMEOUT);
    }

    public void openAndWait(String url, String triggerText, int timeout) {
        open(url);
        // wait for 2 minutes, as that should be enough time to insure that the admin console app has been deployed by the server
        waitForPageLoad(triggerText, timeout);
    }

    /**
     * Click the specified element and wait for the specified trigger text on the resulting page, timing out TIMEOUT seconds.
     *
     * @param triggerText
     */
    protected void clickAndWait(String id, String triggerText) {
        clickAndWait(id, triggerText, TIMEOUT);
    }

    protected void clickAndWait(String id, String triggerText, int seconds) {
        log ("Clicking on {0} \"{1}\"", id, triggerText);
        insureElementIsVisible(id);
        pressButton(id);
        waitForPageLoad(triggerText, seconds);
    }

    protected void clickAndWait(String id, WaitForLoadCallBack callback) {
        insureElementIsVisible(id);
        pressButton(id);
        waitForLoad(TIMEOUT, callback);
    }

    protected void clickAndWaitForElement(String clickId, final String elementId) {
        pressButton(clickId);
        waitForLoad(60, new WaitForLoadCallBack() {
            @Override
            public boolean executeTest() {
                if (isElementPresent(elementId)) {
                    return true;
                }

                return false;
            }

        });
    }

    protected void clickAndWaitForButtonEnabled(String id) {
        pressButton(id);
        waitForButtonEnabled(id);
    }

    protected void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Cause the test to wait for the page to load.  This will be used, for example, after an initial page load
     * (selenium.open) or after an Ajaxy navigation request has been made.
     *
     * @param triggerText The text that should appear on the page when it has finished loading
     * @param timeout     How long to wait (in seconds)
     */
    protected void waitForPageLoad(String triggerText, int timeout) {
        waitForLoad(timeout, new PageLoadCallBack(triggerText, false));
    }

    protected void waitForPageLoad(final String triggerText, final boolean textShouldBeMissing) {
        waitForPageLoad(triggerText, TIMEOUT, textShouldBeMissing);
    }

    protected void waitForPageLoad(final String triggerText, final int timeout, final boolean textShouldBeMissing) {
        waitForLoad(timeout, new PageLoadCallBack(triggerText, textShouldBeMissing));
    }

    protected void waitForLoad(int timeoutInSeconds, WaitForLoadCallBack callback) {
        for (int seconds = 0;; seconds++) {
            if (seconds >= (timeoutInSeconds)) {
                Assert.fail("The operation timed out waiting for the page to load.");
            }

            WebElement ajaxPanel = (WebElement) elementFinder.findElement(By.id(AJAX_INDICATOR), TIMEOUT,
                    new ExpectedCondition<Boolean>() {

                        @Override
                        public Boolean apply(WebDriver driver) {
                            try {
                                WebElement ajaxPanel = (WebElement) driver.findElement(By.id(AJAX_INDICATOR));
                                return !ajaxPanel.isDisplayed();
                            } catch (Exception e) {
                                return false;
                            }
                        }
                    });
//                if (!ajaxPanel.isDisplayed()) {
            if (callback.executeTest()) {
                break;
            }
//                }

            sleep(TIMEOUT_CALLBACK_LOOP);
        }
    }

    // The login page doesn't have the ajax indicator, so we must treat it differently
    protected void waitForLoginPageLoad(int timeoutInSeconds) {
        for (int seconds = 0;; seconds++) {
            if (seconds >= (30)) {
                Assert.fail("The operation timed out waiting for the login page to load.");
            }

            boolean loginFormIsDisplayed = false;

            try {
                loginFormIsDisplayed = isElementPresent("j_username");
            } catch (Exception ex) {
            }
            if (loginFormIsDisplayed) {
                break;
            }

            sleep(TIMEOUT_CALLBACK_LOOP);
        }
    }

    protected void handleLogin() {
        handleLogin("admin", "", TRIGGER_COMMON_TASKS);
    }

    protected void handleLogin(String userName, String password, String triggerText) {
        processingLogin = true;
        setFieldValue("j_username", userName);
        setFieldValue("j_password", password);
        clickAndWait("loginButton", triggerText);
        processingLogin = false;
    }

    protected void waitForButtonEnabled(String buttonId) {
//        waitForCondition("document.getElementById('" + buttonId + "').disabled == false", BUTTON_TIMEOUT);
        waitForLoad(BUTTON_TIMEOUT, new ButtonDisabledStateCallBack(buttonId, false));
    }

    protected void waitForButtonDisabled(String buttonId) {
        String value = selenium.getEval(CURRENT_WINDOW + ".document.getElementById('" + buttonId + "').disabled");
//        waitForCondition("document.getElementById('" + buttonId + "').disabled == true", BUTTON_TIMEOUT);
        waitForLoad(BUTTON_TIMEOUT, new ButtonDisabledStateCallBack(buttonId, true));
    }

    protected void waitForCondition(String js, int timeOutInMillis) {
        selenium.waitForCondition(CURRENT_WINDOW + "." + js, Integer.toString(timeOutInMillis));
    }

    protected void deleteRow(String buttonId, String tableId, String triggerText) {
        deleteRow(buttonId, tableId, triggerText, "col0", "col1");
    }

    protected void deleteRow(final String buttonId, final String tableId, final String triggerText, final String selectColId, final String valueColId) {
        rowActionWithConfirm(buttonId, tableId, triggerText, selectColId, valueColId);
        waitForLoad(TIMEOUT, new DeleteRowCallBack(tableId, triggerText, valueColId));
//        waitForPageLoad(triggerText, true);
    }

    protected void rowActionWithConfirm(String buttonId, String tableId, String triggerText) {
        rowActionWithConfirm(buttonId, tableId, triggerText, "col0", "col1");
    }

    protected void rowActionWithConfirm(String buttonId, String tableId, String triggerText, String selectColId, String valueColId) {
        // A defensive getConfirmation()
        getConfirmation();
        chooseOkOnNextConfirmation();
        selectTableRowByValue(tableId, triggerText, selectColId, valueColId);
        sleep(500); // argh!
        waitForButtonEnabled(buttonId);
        pressButton(buttonId);
        getConfirmation();
        sleep(500); // argh!
        waitForButtonDisabled(buttonId);
    }

    /**
     * This method will scan the all ths links for the link with the given text.  We can't rely on a link's position
     * in the table, as row order may vary (if, for example, a prior test run left data behind).  If the link is not
     * found, null is returned, so the calling code may need to check the return value prior to use.
     *
     * @param baseId
     * @param value
     * @return
     */
    protected String getLinkIdByLinkText(final String baseId, final String value) {
        final ExceptionSwallowingLoop<String> loop = new ExceptionSwallowingLoop<String>() {
            @Override
            public String operation() {
                WebElement link = elementFinder.findElement(By.linkText(value), TIMEOUT);
                return (link == null) ? null : (String) link.getAttribute("id");
            }
        };
        return loop.get();
    }

    protected boolean isTextPresent(String text) {
        return selenium.isTextPresent(resolveTriggerText(text));
    }

    protected void selectTableRowByValue(String tableId, String value) {
        selectTableRowByValue(tableId, value, "col0", "col1");
    }

    protected void selectTableRowByValue(String tableId, String value, String selectColId, String valueColId) {
        List<String> rows = getTableRowsByValue(tableId, value, valueColId);
        for (String row : rows) {
            // It seems this must be click for the JS to fire in the browser
            final String id = row + ":" + selectColId + ":select";
            selectTableRow(row, selectColId);
        }
    }

    /**
     * @See selectTableRowByValue(String tableId, String value, String selectColId, String valueColId);
     * @param baseId
     * @param value
     * @return
     */
    protected int selectTableRowsByValue(String baseId, String value) {
        return selectTableRowsByValue(baseId, value, "col0", "col1");
    }

    /**
     * For the given table, this method will select each row whose value in the specified column
     * matched the value given, returning the number of rows selected.
     */
    protected int selectTableRowsByValue(String tableId, String value, String selectColId, String valueColId) {
        List<String> rows = getTableRowsByValue(tableId, value, valueColId);
        if (!rows.isEmpty()) {
            for (String row : rows) {
                selectTableRow(row, selectColId);
            }
        }

        return rows.size();
    }

    private void selectTableRow(String rowId, String colId) {
        boolean rowHighlighted = false;

        int iterations = 0;
        this.log("Clicking on {0} in row {1} and making it sure it is highlighted", colId, rowId);
        while (!rowHighlighted && (iterations <= 50)) {
            selenium.click(rowId + ":" + colId + ":select");
            markCheckbox(rowId + ":" + colId + ":select");
            sleep(500);
            String rowClass = selenium.getAttribute("identifier="+rowId+"@class");
            rowHighlighted = ((rowClass != null) && (rowClass.contains("TblSelRow_sun4")));
            iterations++;
        }

        if (iterations >= 50) {
            Assert.fail("Timed out wait for row in " + rowId + " to be selected");
        }
    }

    protected void deleteAllTableRows(String tableId) {
        String deleteButtonId = tableId + ":topActionsGroup1:button1";
        selectAllTableRows(tableId);
        waitForButtonEnabled(deleteButtonId);
        chooseOkOnNextConfirmation();
        pressButton(deleteButtonId);
        getConfirmation();
        this.waitForButtonDisabled(deleteButtonId);
    }

    protected void selectAllTableRows(String tableId) {
        int count = getTableRowCount(tableId);
        for (int i = 0 ; i < count; i++) {
            selenium.click(tableId+":rowGroup1:" + i +":col0:select");
            markCheckbox(tableId+":rowGroup1:" + i +":col0:select");
        }
    }

    // TODO: write javadocs for this
    protected String getTableRowByValue(String tableId, String value, String valueColId) {
        try {
            int row = 0;
            while (true) { // iterate over any rows
                // Assume one row group for now and hope it doesn't bite us
                String text = getText(tableId + ":rowGroup1:" + row + ":" + valueColId);
                if (text.equals(value)) {
                    return tableId + ":rowGroup1:" + row + ":";
                }
                row++;
            }
        } catch (Exception e) {
            Assert.fail("The specified row was not found: " + value);
            return "";
        }
    }

    protected List<String> getTableRowsByValue(String tableId, String value, String valueColId) {
        List<String> rows = new ArrayList<String>();
        try {
            int row = 0;
            while (true) { // iterate over any rows
                // Assume one row group for now and hope it doesn't bite us
                String text = getText(tableId + ":rowGroup1:" + row + ":" + valueColId);
                if (text.contains(value)) {
                    rows.add(tableId + ":rowGroup1:" + row);
                }
                row++;
            }
        } catch (Exception e) {
        }

        return rows;
    }

    // TODO: write javadocs for this
    protected int getTableRowCountByValue(String tableId, String value, String valueColId, Boolean isLabel) {
        int tableCount = getTableRowCount(tableId);
        int selectedCount = 0;
        try {
            for (int i = 0; i < tableCount; i++) {
                String text = "";
                if (isLabel) {
                    text = getText(tableId + ":rowGroup1:" + i + ":" + valueColId);
                } else {
                    text = getFieldValue(tableId + ":rowGroup1:" + i + ":" + valueColId);
                }
                if (text.equals(value)) {
                    selectedCount++;
                }
            }
        } catch (Exception e) {
            Assert.fail("The specified row was not found: " + value);
            return 0;
        }
        return selectedCount;
    }

    protected int getTableRowCountByValue(String tableId, String value, String valueColId) {
        return getTableRowCountByValue(tableId, value, valueColId, true);
    }

    protected List<String> getTableColumnValues(String tableId, String columnId) {
        List<String> values = new ArrayList<String>();
        int tableCount = getTableRowCount(tableId);
        try {
            int row = 0;
            while (true) { // iterate over any rows
                // Assume one row group for now and hope it doesn't bite us
                values.add(getText(tableId + ":rowGroup1:" + row + ":" + columnId));
                row++;
            }
        } catch (Exception e) {
        }

        return values;
    }

    protected boolean tableContainsRow(String tableId, String columnId, String value) {
        return getTableRowCountByValue(tableId, value, columnId) > 0;
    }

    protected int addTableRow(String tableId, String buttonId) {
        return addTableRow(tableId, buttonId, "Additional Properties");
    }

    protected int addTableRow(String tableId, String buttonId, String countLabel) {
        int count = getTableRowCount(tableId);
        clickAndWait(buttonId, new AddTableRowCallBack(tableId, count));
        return ++count;
    }

    protected void assertTableRowCount(String tableId, int count) {
        Assert.assertEquals(count, getTableRowCount(tableId));
    }

    // Look at all those params. Maybe this isn't such a hot idea.
    /**
     * @param resourceName
     * @param tableId
     * @param enableButtonId
     * @param statusID
     * @param backToTableButtonId
     * @param tableTriggerText
     * @param editTriggerText
     */
    protected void testEnableButton(String resourceName,
            String tableId,
            String enableButtonId,
            String statusID,
            String backToTableButtonId,
            String tableTriggerText,
            String editTriggerText,
            String statusMsg) {
        testEnableDisableButton(resourceName, tableId, enableButtonId, statusID, backToTableButtonId, tableTriggerText, editTriggerText, statusMsg);
    }

    protected void testDisableButton(String resourceName,
            String tableId,
            String disableButtonId,
            String statusId,
            String backToTableButtonId,
            String tableTriggerText,
            String editTriggerText,
            String statusMsg) {
        testEnableDisableButton(resourceName, tableId, disableButtonId, statusId, backToTableButtonId, tableTriggerText, editTriggerText, statusMsg);
    }

    private void testEnableDisableButton(String resourceName,
            String tableId,
            String enableButtonId,
            String statusId,
            String backToTableButtonId,
            String tableTriggerText,
            String editTriggerText,
            String state) {
        sleep(TIMEOUT_CALLBACK_LOOP); // yuck
        selectTableRowByValue(tableId, resourceName);
        waitForButtonEnabled(enableButtonId);
        pressButton(enableButtonId);
        waitForButtonDisabled(enableButtonId);

        clickAndWait(getLinkIdByLinkText(tableId, resourceName), editTriggerText);
        // TODO: this is an ugly, ugly hack and needs to be cleaned up
        if(state.contains("Target")) {
            Assert.assertEquals(state, getText(statusId));
        } else {
            if ("on".equals(state) || "off".equals(state)) {
                Assert.assertEquals("on".equals(state), isChecked(statusId));
            } else {
                Assert.assertEquals(state, getFieldValue(statusId));
            }
        }
        clickAndWait(backToTableButtonId, tableTriggerText);
    }

    protected void testEnableOrDisableTarget(String tableSelectMutlipleId,
            String enableButtonId,
            String generalTabId,
            String targetTabId,
            String statusId,
            String generalTriggerText,
            String targetTriggerText,
            String state) {
        pressButton(tableSelectMutlipleId);
        waitForButtonEnabled(enableButtonId);
        pressButton(enableButtonId);
        waitForButtonDisabled(enableButtonId);

        clickAndWait(generalTabId, generalTriggerText);
        Assert.assertEquals(state, getText(statusId));

        clickAndWait(targetTabId, targetTriggerText);
    }

    protected void testManageTargets(String resourcesLinkId,
            String resourcesTableId,
            String enableButtonId,
            String disableButtonId,
            String enableOrDisableTextFieldId,
            String resGeneralTabId,
            String resTargetTabId,
            String resourcesTriggerText,
            String resEditTriggerText,
            String jndiName,
            String instanceName) {
        final String TRIGGER_EDIT_RESOURCE_TARGETS = "Resource Targets";
        final String enableStatus = "Enabled on 2 of 2 Target(s)";
        final String disableStatus = "Enabled on 0 of 2 Target(s)";
        final String TRIGGER_MANAGE_TARGETS = "Manage Resource Targets";
        final String DEFAULT_SERVER = "server";

        reset();
        clickAndWait(resourcesLinkId, resourcesTriggerText);
        clickAndWait(getLinkIdByLinkText(resourcesTableId, jndiName), resEditTriggerText);
        //Click on the target tab and verify whether the target is in the target table or not.
        clickAndWait(resTargetTabId, TRIGGER_EDIT_RESOURCE_TARGETS);
        Assert.assertTrue(isTextPresent(instanceName));

        //Disable all targets
        testEnableOrDisableTarget("propertyForm:targetTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image",
                disableButtonId,
                resGeneralTabId,
                resTargetTabId,
                enableOrDisableTextFieldId,
                resEditTriggerText,
                TRIGGER_EDIT_RESOURCE_TARGETS,
                disableStatus);

        //Enable all targets
        testEnableOrDisableTarget("propertyForm:targetTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image",
                enableButtonId,
                resGeneralTabId,
                resTargetTabId,
                enableOrDisableTextFieldId,
                resEditTriggerText,
                TRIGGER_EDIT_RESOURCE_TARGETS,
                enableStatus);

        //Test the manage targets : Remove the server from targets.
        clickAndWait("propertyForm:targetTable:topActionsGroup1:manageTargetButton", TRIGGER_MANAGE_TARGETS);
        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_selected", DEFAULT_SERVER);
        pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        clickAndWait("form:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        //Test the issue : 13280
        //If server instance is not one of the target, edit resource was failing. Fixed that and added a test
        clickAndWait(resourcesLinkId, resourcesTriggerText);
        clickAndWait(getLinkIdByLinkText(resourcesTableId, jndiName), resEditTriggerText);
        Assert.assertTrue(isTextPresent(jndiName));
        clickAndWait(resTargetTabId, TRIGGER_EDIT_RESOURCE_TARGETS);

        //Test the manage targets : Remove the instance and add the server.
        clickAndWait("propertyForm:targetTable:topActionsGroup1:manageTargetButton", TRIGGER_MANAGE_TARGETS);
        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_selected", instanceName);
        waitForButtonEnabled("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        waitForButtonDisabled("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_removeButton");
        selenium.removeAllSelections("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available");

        addSelectSelection("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove_available", DEFAULT_SERVER);
        waitForButtonEnabled("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        pressButton("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        waitForButtonDisabled("form:targetSection:targetSectionId:addRemoveProp:commonAddRemove:commonAddRemove_addButton");
        clickAndWait("form:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        waitForPageLoad(instanceName, false);
        Assert.assertTrue(isTextPresent(DEFAULT_SERVER));

        //Go Back to Resources Page
        clickAndWait(resourcesLinkId, resourcesTriggerText);
    }

    protected void logDebugMessage(String message) {
        if (DEBUG) {
            logger.info(message);
        }
    }

    protected String resolveTriggerText(String original) {
        String triggerText = original;
        int index = original.indexOf(".");
        if (index > -1) {
            String bundleName = original.substring(0, index);
            String key = original.substring(index + 1);
            String bundle = bundles.get(bundleName);
            if (bundle != null) {
                ResourceBundle res = ResourceBundle.getBundle(bundle);
                if (res != null) {
                    // Strip out HTML. Hopefully this will be robust enough
                    triggerText = res.getString(key).replaceAll("<.*?>", "");
                } else {
                    Logger.getLogger(BaseSeleniumTestClass.class.getName()).log(Level.WARNING, null, "An invalid resource bundle was specified: " + original);
                }
            }
        }
        return triggerText;
    }

    protected void log(String message, String... args) {
        if (DEBUG) {
            String[] temp = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                temp[i] = resolveTriggerText(args[i]);
            }
            logger.log(Level.INFO, message, temp);
        }
    }

    private void waitForElement(String elem) {
        // times out after 5 seconds
        WebDriverWait wait = new WebDriverWait(driver, 5);

        // while the following loop runs, the DOM changes - 
        // page is refreshed, or element is removed and re-added
        wait.until(presenceOfElementLocated(By.id(elem)));
        selenium.focus(elem);
    }

    private static Function<WebDriver, WebElement> presenceOfElementLocated(final By locator) {
        return new Function<WebDriver, WebElement>() {
            @Override
            public WebElement apply(WebDriver driver) {
                return driver.findElement(locator);
            }
        };
    }

    private void insureElementIsVisible (final String id) {
        if (!id.contains("treeForm:tree")) {
            return;
        }

        try {
            WebElement element = (WebElement) elementFinder.findElement(By.id(id), TIMEOUT);
            //driver.findElement(By.id(id));
            if (element.isDisplayed()) {
                return;
            }
        } catch (StaleElementReferenceException sere) {
        }

        final String parentId = id.substring(0, id.lastIndexOf(":"));
        final WebElement parentElement = (WebElement) elementFinder.findElement(By.id(parentId), TIMEOUT);
//                driver.findElement(By.id(parentId));
        if (!parentElement.isDisplayed()) {
            insureElementIsVisible(parentId);
            String grandParentId = parentId.substring(0, parentId.lastIndexOf(":"));
            String nodeId = grandParentId.substring(grandParentId.lastIndexOf(":") + 1);
            pressButton(grandParentId + ":" + nodeId + "_turner");
        }
    }

    class PageLoadCallBack implements WaitForLoadCallBack {
        boolean textShouldBeMissing;
        String triggerText;

        public PageLoadCallBack(String triggerText, boolean textShouldBeMissing) {
            this.textShouldBeMissing = textShouldBeMissing;
            this.triggerText = resolveTriggerText(triggerText);
        }


        @Override
        public boolean executeTest() {
            boolean found = false;
            try {
                if (isElementPresent("j_username") && !processingLogin) {
                    handleLogin();
                }
                if (!textShouldBeMissing) {
                    boolean visible = false;
                    final List<WebElement> elements = driver.findElements(By.xpath("//*[contains(text(), \"" + 
                            triggerText.replace("\"", "\\\"") + "\")]"));
                    if (!elements.isEmpty()) {
                        for (WebElement e : elements) {
                            if (e.isDisplayed()) {
                                visible = true;
                            }
                        }
                    } else {
                        visible = true;
                    }

                    if (isTextPresent(triggerText) && visible) {
                        found = true;
                    }
                } else if (!isTextPresent(triggerText)) {
                        found = true;

                } else {
                    if (isTextPresent("RuntimeException")) {
                        Assert.fail("Exception detected on page. Please check the logs for details");
                    }
                }
            } catch (SeleniumException se) {
                String message = se.getMessage();
                if (!"ERROR: Couldn't access document.body.  Is this HTML page fully loaded?".equals(message)) {
                    Assert.fail(message);
                }
            }

            return found;
        }
    };

    class DeleteRowCallBack implements WaitForLoadCallBack {
        private String tableId;
        private String tableRowValue;
        private String tableColId;

        public DeleteRowCallBack(String tableId, String tableRowValue, String tableColId) {
            this.tableId = tableId;
            this.tableRowValue = tableRowValue;
            this.tableColId = tableColId;
        }

        @Override
        public boolean executeTest() {
            try {
                List<String> rows = getTableRowsByValue(tableId, tableRowValue, tableColId);
                return rows.isEmpty();
            } catch (SeleniumException se) {
                return false;
            }
        }

    }

    class AddTableRowCallBack implements WaitForLoadCallBack {
        private final String tableId;
        private final int initialCount;

        public AddTableRowCallBack(String tableId, int initialCount) {
            this.tableId = tableId;
            this.initialCount = initialCount;
        }

        @Override
        public boolean executeTest() {
            try {
                int count = getTableRowCount(tableId);
                return count > initialCount;
            } catch (Exception e) {
                return false;
            }
        }
    };

    class ButtonDisabledStateCallBack implements WaitForLoadCallBack {
        private String buttonId;
        private boolean desiredState;

        public ButtonDisabledStateCallBack(String buttonId, boolean desiredState) {
            this.buttonId = buttonId;
            this.desiredState = desiredState;
        }

        @Override
        public boolean executeTest() {
//            String attr = selenium.getEval("this.browserbot.findElement('id=" + buttonId + "').disabled"); // "Classic" Selenium
            try {
                String attr =
                        elementFinder.findElement(By.id(buttonId), TIMEOUT) //                        driver.findElement(By.id(buttonId))
                        .getAttribute("disabled"); // WebDriver-backed Selenium
                return (Boolean.parseBoolean(attr) == desiredState);
            } catch (Exception ex) {
                return true;// ???
            }
        }
    }

    abstract class ExceptionSwallowingLoop<T> {
        public T get() {
            T value = null;
            boolean success = false;
            int count = 0;
            while (!success && (count < TIMEOUT /2 )) {
                try {
                    value = operation();
                    success = true;
                } catch (Exception e) {
                    logger.log(Level.FINE, "Exception caught ('{0}'). Sleeping...", e.getMessage());
                    count++;
                }
            }

            return value;
        }

        public abstract T operation();
    }
}
