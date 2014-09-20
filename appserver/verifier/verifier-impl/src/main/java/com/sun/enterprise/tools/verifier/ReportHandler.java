/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.util.VerifierConstants;
import com.sun.enterprise.util.SystemPropertyConstants;

/**
 * This class is responsible for generating the final output report file in xml and txt file.
 *
 * @author Sudipto Ghosh
 */

public class ReportHandler {

    private final String TEST = "test"; // NOI18N
    private final String TEST_NAME = "test-name"; // NOI18N
    private final String TEST_DESC = "test-description"; // NOI18N
    private final String TEST_ASSERTION = "test-assertion"; // NOI18N
    private final String STATIC_VER = "static-verification"; // NOI18N
    private final String FAILED = "failed"; // NOI18N
    private final String PASSED = "passed"; // NOI18N
    private final String NOTAPPLICABLE = "not-applicable"; // NOI18N
    private final String WARNING = "warning"; // NOI18N

    private final String FAILNUMBER = "failure-number"; // NOI18N
    private final String WARNINGNUMBER = "warning-number"; // NOI18N
    private final String ERRORNUMBER = "error-number"; // NOI18N
    private final String FAILCOUNT = "failure-count"; // NOI18N

    private final String ERROR = "error"; // NOI18N
    private final String ERROR_NAME = "error-name"; // NOI18N
    private final String ERROR_DESC = "error-description"; // NOI18N
    private final String XSL_FILE = "textFormatForVerifierSS"; // NOI18N
    private String outputFileStr = null;

    private Element rootNode = null;
    private Document document;
    private String textResult; // verification result in TEXT form.
    private ResultManager resultMgr;
    private VerifierFrameworkContext verifierFrameworkContext;
    private Logger logger = LogDomains.getLogger(LogDomains.AVK_VERIFIER_LOGGER);

    /**
     * Verifier uses this constructor to generate test report.
     * @param verifierFrameworkContext
     */
    public ReportHandler(VerifierFrameworkContext verifierFrameworkContext) {
        this.verifierFrameworkContext = verifierFrameworkContext;
        this.resultMgr = verifierFrameworkContext.getResultManager();

        String onlyJarFile = new File(verifierFrameworkContext.getJarFileName()).getName();
        String outputDirName = formatOutputDirName(verifierFrameworkContext.getOutputDirName());
        outputDirName = (outputDirName == null) ?
                "" : outputDirName + File.separator;
        if (verifierFrameworkContext.isUseTimeStamp()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(
                    "yyyyMMddhhmmss"); // NOI18N
            outputFileStr = outputDirName + onlyJarFile +
                    dateFormatter.format(new Date());
        } else
            outputFileStr = outputDirName + onlyJarFile;
    }

    /**
     * This api is called from verfier framework to generate the final report
     *
     * @throws IOException
     */
    public void generateAllReports() throws IOException {
        try {
            createResultsDocument(verifierFrameworkContext.getReportLevel());
            writeToXmlFile();
            writeToTxtFile();
            writeToConsole();
        } catch (IOException e) {
            throw  e;
        }
    }

    /**
     * writes the final output report file to the console.
     */
    private void writeToConsole() {
        if (verifierFrameworkContext.isUsingGui())
            return;
        if (verifierFrameworkContext.isBackend()) {
            logger.log(Level.SEVERE, textResult);
        } else {
            logger.log(Level.INFO, getClass().getName() + ".resultSummary",
                new Object[]{new Integer(resultMgr.getFailedCount()),
                             new Integer(resultMgr.getWarningCount()),
                             new Integer(resultMgr.getErrorCount())});
        }
        if((resultMgr.getFailedCount() + resultMgr.getWarningCount()
            + resultMgr.getErrorCount()) != 0
            || verifierFrameworkContext.getReportLevel() == VerifierConstants.ALL)
            logger.log(Level.INFO, getClass().getName() +
                ".LookInResultsTestAssertions", // NOI18N
                new Object[]{outputFileStr + ".txt"}); // NOI18N
        else
            logger.log(Level.INFO, getClass().getName() +
                ".LookInResultsTestAssertions1"); // NOI18N
    }

    /**
     * This api initializes the document object and calls generate apis to add results
     * to the document. Finally failureCount() api is called to add the error, failure
     * and warning counts to the document.
     *
     * @param reportLevel
     * @throws IOException
     */
    private void createResultsDocument(int reportLevel) throws IOException {
        createDOMTree();
        if (reportLevel != VerifierConstants.FAIL)
            addResultsToDocument(WARNING, resultMgr.getWarningResults());
        if (reportLevel == VerifierConstants.ALL) {
            addResultsToDocument(PASSED, resultMgr.getOkayResults());
            addResultsToDocument(NOTAPPLICABLE, resultMgr.getNaResults());
        }

        addResultsToDocument(FAILED, resultMgr.getFailedResults());
        Vector error = resultMgr.getError();
        if (!error.isEmpty()) {
            for (int i = 0; i < error.size(); i++) {
                LogRecord lr = (LogRecord) error.get(i);
                generateErrors(lr);
            }
        }
        failureCount();
    }

    /**
     * create the new Document tree with root node <static-verification>
     *
     * @throws IOException
     */
    private void createDOMTree() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (Exception e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        document = builder.newDocument();
        rootNode = document.createElement(STATIC_VER);
        document.appendChild(rootNode);
    }

    /**
     * This api adds each result to the document tree based on the status.
     *
     * @param status
     * @param resultVector
     */
    private void addResultsToDocument(String status, Vector resultVector) {
        for (int i = 0; i < resultVector.size(); i++) {
            Enumeration en;
            Result r = (Result) resultVector.get(i);
            String moduleName = r.getModuleName();
            if (status == FAILED) {
                en = r.getErrorDetails().elements();
            } else if (status == WARNING) {
                en = r.getWarningDetails().elements();
            } else if (status == PASSED)
                en = r.getGoodDetails().elements();
            else
                en = r.getNaDetails().elements();
            createNode(moduleName, status);
            addToDocument(moduleName, status, r, en);
        }
    }

    /**
     * This api is used to add the error logs into the document.
     *
     * @param record
     */
    private void generateErrors(LogRecord record) {
        Element errorNode = null;
        //start adding nodes to document
        //check if the node already exists. If not, add it.
        NodeList nodeList = document.getElementsByTagName(ERROR);
        if (nodeList.getLength() == 0) {
            errorNode = document.createElement(ERROR);
            rootNode.appendChild(errorNode);
        } else {
            errorNode = (Element) nodeList.item(0); //there is only 1 node with tag of errorNode value
        }
        Element excepName = getTextNode(ERROR_NAME, record.getMessage());
        errorNode.appendChild(excepName);
        if (record.getThrown() != null) {
            Element excepDescr = getTextNode(ERROR_DESC,
                    writeStackTraceToFile(record.getThrown()));
            errorNode.appendChild(excepDescr);
        }
    }

    /**
     * This method is responsible for creating nodes in the DOM tree like
     * <p>
     * <ejb>
     *   <failed></failed>
     * </ejb>
     * where moduleName is ejb and status is failed.
     *
     * @param moduleName
     * @param status
     */
    private void createNode(String moduleName, String status) {
        NodeList nodeList;
        Element moduleNode;
        nodeList = document.getElementsByTagName(moduleName);
        if (nodeList.getLength() == 0) {
            moduleNode = document.createElement(moduleName);
            rootNode.appendChild(moduleNode);
        } else {
            moduleNode = (Element) nodeList.item(0); //there is only 1 node with tag of moduleNode value
        }
        nodeList = moduleNode.getChildNodes();
        Element statusNode = null;

        if (nodeList.getLength() == 0) {
            statusNode = document.createElement(status);
            moduleNode.appendChild(statusNode);
        } else {
            for (int j = 0; j < nodeList.getLength(); j++) {
                if (((Element) nodeList.item(j)).getTagName().equals(status)) {
                    statusNode = (Element) nodeList.item(j);
                    break;
                }
            }
            if (statusNode == null) {
                statusNode = document.createElement(status);
                moduleNode.appendChild(statusNode);
            }
        }
    }

    /**
     * This method adds the result value to the appropriate location in the DOM
     * tree.
     * @param moduleName
     * @param status
     * @param r
     * @param en
     */
    private void addToDocument(String moduleName, String status, Result r,
                               Enumeration en) {
        if (r == null) return;
        NodeList nodeList;
        //this nodeList is the list of nodes below the moduleNode
        nodeList =
                document.getElementsByTagName(moduleName).item(0)
                .getChildNodes();
        Element statusNode = null;
        for (int j = 0; j < nodeList.getLength(); j++) {
            if (((Element) nodeList.item(j)).getTagName().equals(status)) {
                statusNode = (Element) nodeList.item(j);
                break;
            }
        }
        // now get the stuff and write out from result object r
        Element test = document.createElement(TEST);
        Element testName = getTextNode(TEST_NAME, r.getTestName());
        Element testAssertion = getTextNode(TEST_ASSERTION, r.getAssertion());
        // loop thru Details vector
        String string = "";
        while (en.hasMoreElements()) {
            string = string + (String) en.nextElement() + "\n"; // NOI18N
        }
        Element testDescr = getTextNode(TEST_DESC, string);
        test.appendChild(testName);
        test.appendChild(testAssertion);
        test.appendChild(testDescr);
        statusNode.appendChild(test);
    }

    /**
     * Convenience for creating a node <tag>text<tag>.
     *
     * @param tag
     * @param text
     * @return
     */
    private Element getTextNode(String tag, String text) {
        Element element = document.createElement(tag);
        element.appendChild(document.createTextNode(text));
        return element;
    }

    /**
     * wites the final result report to the output xml file
     *
     * @throws IOException
     */
    private void writeToXmlFile() throws IOException {
        FileOutputStream fos = null;
        try {
            File outputFile = extractResultsFileToTmpDir(
                    outputFileStr + ".xml"); // NOI18N
            DOMSource domSource = new DOMSource(document);
            TransformerFactory tfactory = TransformerFactory.newInstance();
            Transformer transformer = tfactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // NOI18N
            transformer.setOutputProperty(OutputKeys.METHOD, "xml"); // NOI18N
            String encoding = System.getProperty("file.encoding");
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            fos = new FileOutputStream(outputFile);
            transformer.transform(domSource, new StreamResult(fos));
        } catch (Exception e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        } finally {
            try {
                if(fos != null)
                    fos.close();
            } catch (Exception e){}
        }
    }

    /**
     * writes the final result report to output txt file
     *
     * @throws IOException
     */
    private void writeToTxtFile() throws IOException {
        InputStream xslFile = getLocalizedXSLFile();
        
        Document dynamicDocument = document;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        generateText(dynamicDocument, xslFile, output);
        textResult = output.toString("UTF-8");
        
        // dump to text file.
        File outputFile = extractResultsFileToTmpDir(outputFileStr + ".txt"); // NOI18N
        OutputStreamWriter fw = new OutputStreamWriter(
                                    new FileOutputStream(outputFile));
        fw.write(textResult);
        fw.close();
    }

    private File extractResultsFileToTmpDir(String jarFile) {
        File tmpJarFile = null;
        String fullFilename;
        tmpJarFile = new File(jarFile);
        fullFilename = tmpJarFile.getAbsolutePath();
        if (new File(fullFilename).getParent() != null) {
            (new File(new File(fullFilename).getParent())).mkdirs();
        }
        return tmpJarFile;
    }

    /**
     * Transforms the xml report to txt report.
     * @param xmlResult
     * @param stylesheet
     * @throws IOException
     */
    private void generateText(Document xmlResult, InputStream stylesheet,
                              OutputStream output)
            throws IOException {
        // Produce Output:
        FileOutputStream fos = null;
        try {
            StreamSource styleSource;
            Transformer transformer;
            TransformerFactory tFactory = TransformerFactory.newInstance();
            if (stylesheet != null) {
                styleSource = new StreamSource(stylesheet);
                transformer = tFactory.newTransformer(styleSource);
            } else {
                transformer = tFactory.newTransformer();
            }
            DOMSource source = new DOMSource(xmlResult);
            StreamResult streamResult = new StreamResult(output);
            transformer.transform(source, streamResult);

        } catch (Exception e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        } finally {
            try {
                if(fos != null)
                    fos.close();
            } catch (Exception e) {}
        }
    }

    private void failureCount() {
        int failedCount = resultMgr.getFailedCount();
        int warningCount = resultMgr.getWarningCount();
        int errorCount = resultMgr.getErrorCount();

        Element failureNode = null;
        NodeList nodeList = document.getElementsByTagName(FAILCOUNT);
        if (nodeList.getLength() == 0) {
            failureNode = document.createElement(FAILCOUNT);
            rootNode.appendChild(failureNode);
        } else {
            failureNode = (Element) nodeList.item(0);
        }

        nodeList = failureNode.getChildNodes();//document.getElementsByTagName(FAILED);
        Element failed_count = null;
        Element warning_count = null;
        Element error_count = null;

        if (nodeList.getLength() == 0) {
            failed_count =
                    getTextNode(FAILNUMBER,
                            new Integer(failedCount).toString());
            failureNode.appendChild(failed_count);

            warning_count =
                    getTextNode(WARNINGNUMBER,
                            new Integer((warningCount)).toString());
            failureNode.appendChild(warning_count);

            error_count =
                    getTextNode(ERRORNUMBER,
                            new Integer(errorCount).toString());
            failureNode.appendChild(error_count);
        } else {
            for (int j = 0; j < nodeList.getLength(); j++) {
                if (((Element) nodeList.item(j)).getTagName().equals(
                        FAILNUMBER)) {
                    failed_count = (Element) nodeList.item(j);
                    (failed_count.getFirstChild()).setNodeValue(
                            new Integer(failedCount).toString());
                }
                if (((Element) nodeList.item(j)).getTagName().equals(
                        WARNINGNUMBER)) {
                    warning_count = (Element) nodeList.item(j);
                    (warning_count.getFirstChild()).setNodeValue(
                            new Integer(warningCount).toString());
                }
                if (((Element) nodeList.item(j)).getTagName().equals(
                        ERRORNUMBER)) {
                    error_count = (Element) nodeList.item(j);
                    (error_count.getFirstChild()).setNodeValue(
                            new Integer(errorCount).toString());
                }
            }
            if (failed_count == null) {
                failed_count =
                        getTextNode(FAILNUMBER,
                                new Integer(failedCount).toString());
                failureNode.appendChild(failed_count);
            }
            if (warning_count == null) {
                warning_count =
                        getTextNode(WARNINGNUMBER,
                                new Integer(warningCount).toString());
                failureNode.appendChild(warning_count);
            }
            if (error_count == null) {
                error_count =
                        getTextNode(ERRORNUMBER,
                                new Integer(errorCount).toString());
                failureNode.appendChild(error_count);
            }
        }
    }

    /**
     * returns the error description for writing to the final report.
     * @param e
     * @return
     */
    private String writeStackTraceToFile(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    /**
     * <p>
     * @return the localized XSL file if it is present in the configuration
     * directory. If not, return the default one which is english
     * </p>
     */
    private InputStream getLocalizedXSLFile() {
        InputStream is;
        Locale locale = Locale.getDefault();
        
        // check first with the language and country
        String xslFileName = VerifierConstants.CFG_RESOURCE_PREFIX +
                XSL_FILE + "_" + locale.toString() + ".xsl"; // NOI18N
        final ClassLoader loader = getClass().getClassLoader();
        is = loader.getResourceAsStream(xslFileName);
        if (is != null) {
            return is;
        }
        // check now with the language
        xslFileName = VerifierConstants.CFG_RESOURCE_PREFIX +
                XSL_FILE + "_" + locale.getLanguage() + ".xsl"; // NOI18N
        is = loader.getResourceAsStream(xslFileName);
        if (is != null) {
            return is;
        }
        // just take the english version now...
        xslFileName = VerifierConstants.CFG_RESOURCE_PREFIX +
                XSL_FILE + ".xsl"; // NOI18N
        is = loader.getResourceAsStream(xslFileName);
        return is;
    }

    /**
     * Replaces '/' or '\' with File separator char
     * @param outputDirName
     * @return
     */
    private String formatOutputDirName(String outputDirName) {

    	if (outputDirName != null && outputDirName.trim().length() > 0 ) {
    		char[] outputDirNameArr = outputDirName.toCharArray();
    		StringBuffer formName = new StringBuffer();
    		for(int i = 0; i < outputDirNameArr.length; i++) {
    			if(outputDirNameArr[i] == '/' || outputDirNameArr[i] == '\\') {
    				formName.append(File.separatorChar);
    			} else {
    				formName.append(outputDirNameArr[i]);
    			}
    		}
    		return formName.toString();
    	}
    	return outputDirName;
    }
}
