/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

import com.sun.enterprise.tools.verifier.util.LogDomains;

public class Result {

    public static final int PASSED = 0;
    public static final int FAILED = 1;
    public static final int WARNING = 2;
    public static final int NOT_APPLICABLE = 3;
    public static final int NOT_RUN = 4;
    public static final int NOT_IMPLEMENTED = 5;
    private int status = NOT_RUN;

    public static final String APP = "application"; // NOI18N
    public static final String EJB = "ejb"; // NOI18N
    public static final String WEB = "web"; // NOI18N
    public static final String APPCLIENT = "appclient"; // NOI18N
    public static final String CONNECTOR = "connector"; // NOI18N
    public static final String WEBSERVICE = "webservice"; // NOI18N
    public static final String WEBSERVICE_CLIENT = "webservice_client"; // NOI18N

    private String moduleName;

    private String componentName;
    private String assertion;
    private String testName;
    private Vector<String> errorDetails = new Vector<String>();
    private Vector<String> goodDetails = new Vector<String>();
    private Vector<String> warningDetails = new Vector<String>();
    private Vector<String> naDetails = new Vector<String>();
    boolean debug = Verifier.isDebug();

    private Logger logger = LogDomains.getLogger(
            LogDomains.AVK_VERIFIER_LOGGER);
    private FaultLocation faultLocation;

    /**
     * Result Constructor
     */
    public Result() {
        faultLocation = new FaultLocation();
    }


    /**
     * Initialize the Result object
     *
     * @param c Class of the current test/assertion
     * @param compName
     */
     private static final LocalStringsImpl strings = new LocalStringsImpl(Verifier.class);
    public void init(Class c, String version, String compName) {
        setComponentName(compName);
        StringBuffer assertion = new StringBuffer(
                StringManagerHelper.getLocalStringsManager().getLocalString(
                        (c.getName() + ".assertion"), "")); // NOI18N
        String key = ".specMappingInfo_"+version; // NOI18N
        String file="server log";
        StringBuffer specMappingInfo = new StringBuffer(
                StringManagerHelper.getLocalStringsManager().getLocalString(
                        (c.getName() + key), ""));
        // if specMappingInfo_<version> is unavailable then try just specMappingInfo
        if(specMappingInfo == null || specMappingInfo.length() == 0) {
            key = c.getName() + ".specMappingInfo";
            specMappingInfo = new StringBuffer(StringManagerHelper.getLocalStringsManager().getLocalString(key, ""));
        }
         String  prefix = strings.get(
                (getClass().getName() + ".prefix"), file); // NOI18N
        String  suffix = StringManagerHelper.getLocalStringsManager().getLocalString(
                (getClass().getName() + ".suffix"), ""); // NOI18N

        if (specMappingInfo != null && specMappingInfo.length()!=0)
            setAssertion(assertion.append(" ").append(prefix+" ").append(specMappingInfo).append(" "+suffix).toString()); // NOI18N
        else
            setAssertion(assertion.toString());
        String this_package = "com.sun.enterprise.tools.verifier."; // NOI18N
        setTestName(c.getName().substring(this_package.length()));
    }

    /**
     * Store passed info
     *
     * @param detail Details of passed test
     */
    public void passed(String detail) {
        setStatus(PASSED);
        addGoodDetails(detail);
    }

    /**
     * Store warning info
     *
     * @param detail Details of warning test
     */
    public void warning(String detail) {
        setStatus(WARNING);
        addWarningDetails(detail);
    }

    /**
     * Store Not Applicable info
     *
     * @param detail Details of not applicable test
     */
    public void notApplicable(String detail) {
        setStatus(NOT_APPLICABLE);
        addNaDetails(detail);
    }

    /**
     * Store Failed info
     *
     * @param detail Details of failed test
     */
    public void failed(String detail) {
        setStatus(FAILED);
        addErrorDetails(detail);
    }

    /**
     * Retrieve Not Applicable details
     *
     * @return <code>Vector</code> not applicable details
     */
    public Vector getNaDetails() {
        if(naDetails.isEmpty()){
            Vector<String> result = new Vector<String>();
            result.add(StringManagerHelper.getLocalStringsManager()
                    .getLocalString("tests.componentNameConstructor", // NOI18N
                            "For [ {0} ]", // NOI18N
                            new Object[]{getComponentName()}));
            result.add(StringManagerHelper.getLocalStringsManager()
                    .getLocalString(getClass().getName() + ".defaultNADetails", //NOI18N
                            "Test is not applicable.")); // NOI18N
            logger.fine("Returning default NADetails."); // NOI18N
            return result;
        }
        return naDetails;
    }

    /**
     * Retrieve Warning details
     *
     * @return <code>Vector</code> warning details
     */
    public Vector getWarningDetails() {
        return warningDetails;
    }

    /**
     * Retrieve Not Applicable details
     *
     * @param s not applicable details
     */
    public void addNaDetails(String s) {
        naDetails.addElement(s);
        logger.log(Level.FINE, s);
    }

    /**
     * Retrieve Good details
     *
     * @return <code>Vector</code> good details
     */
    public Vector getGoodDetails() {
        if(goodDetails.isEmpty()){
            Vector<String> result = new Vector<String>();
            result.add(StringManagerHelper.getLocalStringsManager()
                    .getLocalString("tests.componentNameConstructor", // NOI18N
                            "For [ {0} ]", // NOI18N
                            new Object[]{getComponentName()}));
            result.add(StringManagerHelper.getLocalStringsManager()
                    .getLocalString(getClass().getName() + ".defaultGoodDetails", //NOI18N
                            "There were no errors reported.")); // NOI18N
            logger.fine("Returning default GoodDetails."); // NOI18N
            return result;
        }
        return goodDetails;
    }

    /**
     * Fill in Good details
     *
     * @param s good detail string
     */
    public void addGoodDetails(String s) {
        goodDetails.addElement(s);
        logger.log(Level.FINE, s);
    }

    /**
     * Fill in Warning details
     *
     * @param s warning detail string
     */
    public void addWarningDetails(String s) {
        warningDetails.addElement(s);
        logger.log(Level.FINE, s);
    }

    /**
     * Retrieve Error details
     *
     * @return <code>Vector</code> error details
     */
    public Vector getErrorDetails() {
        return errorDetails;
    }

    /**
     * Fill in Error details
     *
     * @param s error detail string
     */
    public void addErrorDetails(String s) {
        errorDetails.addElement(s);
        logger.log(Level.FINE, s);
    }

    /**
     * Retrieve test result status
     *
     * @return <code>int</code> test result status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Set test result status
     *
     * @param s test result status
     */
    public void setStatus(int s) {
        status = s;
    }

    /**
     * Retrieve assertion
     *
     * @return <code>String</code> assertion string
     */
    public String getAssertion() {
        return assertion;
    }

    /**
     * Set assertion
     *
     * @param s assertion string
     */
    public void setAssertion(String s) {
        assertion = s;
    }

    /**
     * Retrieve component/module name
     *
     * @return <code>String</code> component/module name
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Set component/module name
     *
     * @param s component/module name
     */
    public void setComponentName(String s) {
        componentName = s;
    }

    /**
     * Retrieve test name
     *
     * @return <code>String</code> test name
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Set test name
     *
     * @param s test name
     */
    public void setTestName(String s) {
        testName = s;
    }

    public void setModuleName(String name) {
        moduleName = name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public FaultLocation getFaultLocation() {
        return faultLocation;
    }
} // Result class
