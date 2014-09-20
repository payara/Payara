/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EventObject;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.sun.enterprise.deployment.*;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.util.VerifierConstants;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.VerifierCheck;
import com.sun.enterprise.tools.verifier.webservices.WebServiceCheckMgrImpl;
import com.sun.enterprise.tools.verifier.persistence.PersistenceUnitCheckMgrImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deploy.shared.FileArchive;

public abstract class CheckMgr {

    /* This class provides the event notification methods for the check managers
    * used by EjbCheckMgrImpl, AppCheckMgrImpl, WebCheckMgrImpl, AppClientCheckMgrImpl
    */
    static Vector<VerifierEventsListener> listenerList = new Vector<VerifierEventsListener>();

    /**
     * <p/>
     * Entry point for executing all tests pertinent to this architecture
     * </p>
     *
     * @param descriptor <code>ConnectorDescritor</code> the deployment descriptor
     */
    protected void check(Descriptor descriptor) throws Exception {
        logger.log(Level.FINE, "com.sun.enterprise.tools.verifier.CheckMgr.check",
                new Object[]{getClass().getName(), descriptor.getName()});

        setRuntimeDDPresent(getAbstractArchiveUri(descriptor));

        // Load the list of tests from the property file for this manager
        loadTestInformationFromPropsFile();

        // These temporary placeholder will keep the results of the tests

        logger.log(Level.FINE, "com.sun.enterprise.tools.verifier.CheckMgr.RunAllTests",
                new Object[]{descriptor.getName()});
        String schemaVersion = getSchemaVersion(descriptor);
        context.setSchemaVersion(schemaVersion);
        context.setJavaEEVersion(verifierFrameworkContext.getJavaEEVersion());
        context.setComponentNameConstructor(getComponentNameConstructor(descriptor));
        FileArchive moduleArchive = new FileArchive();
        moduleArchive.open(getAbstractArchiveUri(descriptor));
        context.setModuleArchive(moduleArchive);
        ResultManager resultManager = verifierFrameworkContext.getResultManager();
        for (int i = 0; i < test.size(); i++) {
            TestInformation ti = (TestInformation) test.elementAt(i);
            String minVersion = ti.getMinimumVersion();
            String maxVersion = ti.getMaximumVersion();
            // does this test apply to the schema version implemented by
            // this component's descriptor
            if (schemaVersion != null && minVersion != null &&
                    schemaVersion.compareTo(minVersion) < 0) {
                logger.log(Level.FINE, "com.sun.enterprise.tools.verifier.CheckMgr.version.NOT_APPLICABLE",
                        new Object[]{ti.getClassName()});
                continue;
            }
            if (schemaVersion != null && maxVersion != null &&
                    schemaVersion.compareTo(maxVersion) > 0) {
                logger.log(Level.FINE, "com.sun.enterprise.tools.verifier.CheckMgr.version.NOT_APPLICABLE",
                        new Object[]{ti.getClassName()});
                continue;
            }
            if(!isApplicable(ti, descriptor)) {
                logger.log(Level.FINE, "com.sun.enterprise.tools.verifier.CheckMgr.version.NOT_APPLICABLE",
                        new Object[]{ti.getClassName()});
                continue;
            }
            try {
                Class c = Class.forName(ti.getClassName());
                VerifierCheck t = (VerifierCheck) c.newInstance();
                t.setVerifierContext(context);
                Result r = t.check(descriptor);
                // no need to setComponentName as it is already set in
                // VerifierTest.getInitialisedResult(). By Sahoo
                // r.setComponentName(getArchiveUri(descriptor));
                setModuleName(r);
                resultManager.add(r);
                // notify listeners of test completion
                fireTestFinishedEvent(r);
            } catch (Throwable e) {
                LogRecord logRecord = new LogRecord(Level.SEVERE,
                        ti.getClassName());
                logRecord.setThrown(e);
                resultManager.log(logRecord);
            }
        }

        fireAllTestsFinishedEvent();
        // done adding it to hastable vector.
    }

    protected abstract ComponentNameConstructor getComponentNameConstructor(Descriptor descriptor);

    /**
     * This method sets the Context object
     */
    public void setVerifierContext(VerifierTestContext context) {
        this.context = context;
    }

    /**
     * support notification of test completion with a ChangeEvent
     * the ChangeEvent source is the corresponding Result object
     *
     * @param l change listener
     */
    public static void addVerifierEventsListener(VerifierEventsListener l) {
        listenerList.add(l);
    }

    /**
     * Remove change listener
     *
     * @param l change listener
     */
    public static void removeVerifierEventsListener(VerifierEventsListener l) {
        listenerList.remove(l);
    }

    /**
     * <p/>
     *
     * @return <code>String</code> the file name containing the list of tests
     *         to be performed by this manager on each archive file
     *         </p>
     */
    protected abstract String getTestsListFileName();

    protected abstract void setModuleName(Result r);

    protected abstract String getSchemaVersion(Descriptor descriptor);

    /**
     * Adding a new function to get the SunONE AS test lists for Ejb tests
     */
    protected abstract String getSunONETestsListFileName();

    protected String getArchiveUri(Descriptor descriptor) {
        String archiveUri = getBundleDescriptor(descriptor).getModuleDescriptor().getArchiveUri();
        return new File(archiveUri).getName();
    }
    
    /**
     * This method is overridden in EjbCheckMgrImpl. This method is used to
     * ensure that entity and mdb tests are not run for session descriptors and 
     * similarly the other way round.
     */ 
    protected boolean isApplicable(TestInformation test, Descriptor descriptor) {
        return true;
    }

    // call once per test, when r is complete
    protected void fireTestFinishedEvent(Result r) {

        Object[] listeners;
        synchronized (listenerList) {
            listeners = listenerList.toArray();
        }
        if (listeners == null)
            return;

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof VerifierEventsListener) {
                //  create the event:
                EventObject event = new EventObject(r);
                ((VerifierEventsListener) listeners[i]).testFinished(event);
            }
        }
    }

    // call once per test, when all tests for the mgr are done
    protected void fireAllTestsFinishedEvent() {

        Object[] listeners;
        synchronized (listenerList) {
            listeners = listenerList.toArray();
        }
        if (listeners == null)
            return;

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof VerifierEventsListener) {
                //  create the event:
                EventObject event = new EventObject(this);
                ((VerifierEventsListener) listeners[i]).allTestsFinished(event);
            }
        }
    }

    /**
     * <p/>
     * load all the test names from the property file. Each manager has its
     * list of test to be performed for each archive in a property file.
     * The tests list of a list of class name implementing a particular test
     * </p>
     */
    private void loadTestInformationFromPropsFile()
            throws ParserConfigurationException, SAXException, IOException {

        if(!test.isEmpty())
            return;
        logger.log(Level.FINE,
                "com.sun.enterprise.tools.verifier.CheckMgr.TestnamesPropsFile"); // NOI18N

        InputStream is = getTestsFileInputStreamFor(getTestsListFileName());

        try
        {
            // parse the xml file
            DocumentBuilder db = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document doc = db.parse(is);
            NodeList list = doc.getElementsByTagName("test"); // NOI18N
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                NodeList nl = e.getChildNodes();
                TestInformation ti = new TestInformation();
                for (int j = 0; j < nl.getLength(); j++) {
                    String nodeName = nl.item(j).getNodeName();
                    if ("test-class".equals(nodeName.trim())) { // NOI18N
                        Node el = nl.item(j);
                        ti.setClassName(el.getFirstChild().getNodeValue().trim());
                    }
                    if ("minimum-version".equals(nodeName.trim())) { // NOI18N
                        Node el = nl.item(j);
                        ti.setMinimumVersion(
                                el.getFirstChild().getNodeValue().trim());
                    }
                    if ("maximum-version".equals(nodeName.trim())) { // NOI18N
                        Node el = nl.item(j);
                        ti.setMaximumVersion(
                                el.getFirstChild().getNodeValue().trim());
                    }
                }
                test.addElement(ti);
            }

            if ((!verifierFrameworkContext.isPortabilityMode() &&
                    getRuntimeDDPresent()))
                readSunONETests(test);
            // to get the list of tests to be excluded
            Vector<TestInformation> testExcluded = getTestFromExcludeList();
            // to exclude the tests
            test = getFinalTestList(test, testExcluded);
        }
        finally
        {
            is.close();
        }
    }

    /**
     * @return <code>boolean</code> successful completion of getting exclude list
     */
    protected Vector<TestInformation> getTestFromExcludeList()
            throws ParserConfigurationException, SAXException, IOException {
        Vector<TestInformation> testExcluded = new Vector<TestInformation>();
        logger.log(Level.FINE,
                "com.sun.enterprise.tools.verifier.CheckMgr.TestnamesPropsFile"); // NOI18N
        // parse the xml file
        InputStream is = getTestsFileInputStreamFor(excludeListFileName);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(is);
            NodeList list = doc.getElementsByTagName("test"); // NOI18N
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                NodeList nl = e.getChildNodes();
                TestInformation ti = new TestInformation();
                for (int j = 0; j < nl.getLength(); j++) {
                    String nodeName = nl.item(j).getNodeName();
                    if ("test-class".equals(nodeName.trim())) { // NOI18N
                        Node el = nl.item(j);
                        ti.setClassName(el.getFirstChild().getNodeValue().trim());
                    }
                }
                testExcluded.addElement(ti);
            }
        } finally {
            is.close();
        }
        return testExcluded;
    }

    /**
     * @param orignalList
     * @param excludeList
     * @return <code>vector</code> successful completion of getting exclude list
     */
    protected Vector<TestInformation> getFinalTestList(
            Vector<TestInformation> orignalList,
            Vector<TestInformation> excludeList) {
        if (excludeList == null) return orignalList;
        if (orignalList.size() != 0 && excludeList.size() != 0) {
            for (int i = 0; i < excludeList.size(); i++) {
                for (int j = 0; j < orignalList.size(); j++) {
                    if (((TestInformation) orignalList.elementAt(j)).getClassName()
                            .equals(
                                    ((TestInformation) excludeList.elementAt(i)).getClassName())) {
                        orignalList.remove(j);
                    }
                }
            }
        }
        return orignalList;
    }

    protected void checkWebServices(Descriptor descriptor)
            throws Exception {
        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isWebServices())
            return;
        BundleDescriptor bundleDescriptor = (BundleDescriptor) descriptor;
        WebServiceCheckMgrImpl webServiceCheckMgr = new WebServiceCheckMgrImpl(
                verifierFrameworkContext);
        if (bundleDescriptor.hasWebServices()) {
            WebServicesDescriptor wdesc = bundleDescriptor.getWebServices();
            webServiceCheckMgr.setVerifierContext(context);
            webServiceCheckMgr.check(wdesc);
        }
    }

    protected void checkPersistenceUnits(RootDeploymentDescriptor descriptor)
            throws Exception {
        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isPersistenceUnits())
            return;
        CheckMgr puCheckMgr = new PersistenceUnitCheckMgrImpl(
                verifierFrameworkContext, context);
        for(PersistenceUnitsDescriptor pus :
                descriptor.getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
            for (PersistenceUnitDescriptor pu :
                    pus.getPersistenceUnitDescriptors()) {
                puCheckMgr.check(pu);
            }
        }
    }

    // end of code added for WebServices

    protected static String getSunPrefix() {
        return "sun-"; // NOI18N
    }

    protected void setRuntimeDDPresent(String uri) {
        logger.warning("setRuntimeDDPresent method not implemented");
//        InputStream is = null;
//        try {
//            AbstractArchive abstractArchive = new FileArchiveFactory().openArchive(
//                    uri);
//            Archivist archivist = ArchivistFactory.getArchivistForArchive(
//                    abstractArchive);
//            if(archivist != null) {
//                String ddFileEntryName = archivist.getRuntimeDeploymentDescriptorPath();
//                is = abstractArchive.getEntry(ddFileEntryName);
//                if (is != null) {
//                    isDDPresent = true;
//                }
//            }
//
//        } catch (IOException e) {
//            isDDPresent = false;
//        } finally {
//            try {
//                if(is != null)
//                    is.close();
//            } catch (Exception e) {
//                // nothing to do here
//            }
//        }
    }

    private boolean getRuntimeDDPresent() {
        return isDDPresent;
    }

    private void readSunONETests(Vector<TestInformation> test)
            throws ParserConfigurationException, SAXException, IOException {
        String sunonetests = getSunONETestsListFileName();
        if (sunonetests == null)
            return;
        InputStream is = getTestsFileInputStreamFor(sunonetests);
        if (is == null)
            return;
        try
        {
            DocumentBuilder db = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document doc = db.parse(is);
            NodeList list = doc.getElementsByTagName("test"); // NOI18N
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                NodeList nl = e.getChildNodes();
                TestInformation ti = new TestInformation();
                for (int j = 0; j < nl.getLength(); j++) {
                    String nodeName = nl.item(j).getNodeName();
                    if ("test-class".equals(nodeName.trim())) { // NOI18N
                        Node el = nl.item(j);
                        ti.setClassName(el.getFirstChild().getNodeValue().trim());
                    }
                    if ("minimum-version".equals(nodeName.trim())) { // NOI18N
                        Node el = nl.item(j);
                        ti.setMinimumVersion(
                                el.getFirstChild().getNodeValue().trim());
                    }
                    if ("maximum-version".equals(nodeName.trim())) { // NOI18N
                        Node el = nl.item(j);
                        ti.setMaximumVersion(
                                el.getFirstChild().getNodeValue().trim());
                    }
                }
                test.addElement(ti);
            }
        }
        finally
        {
            is.close();
        }
    }

    /**
     * Retrieve Web tests from TestNamesWeb.conf file
     *
     * @param filename file listing all web tests to run
     * @return <code>File</code> File handle for tests to run
     */
    private InputStream getTestsFileInputStreamFor(String filename) {
        // We expect all the resources to be in this pkg
        return getClass().getClassLoader().getResourceAsStream(VerifierConstants.CFG_RESOURCE_PREFIX +filename);
    }

    protected String getAbstractArchiveUri(Descriptor descriptor) {
        String archBase = context.getAbstractArchive().getURI().toString();
        if (descriptor instanceof Application)
            return archBase;
        ModuleDescriptor mdesc = getBundleDescriptor(descriptor).getModuleDescriptor();
        if(mdesc.isStandalone()) {
            return archBase;
        } else {
            return archBase + "/" +
                    FileUtils.makeFriendlyFilename(mdesc.getArchiveUri());
        }
    }
    /**
     * EjbCheckMgrImpl, WebServiceClientCheckMgrImpl and WebServiceCheckMgrImpl
     * classes override this method. For each of these areas the tests are run
     * on descriptors rather than bundle descriptors.
     */ 
    protected BundleDescriptor getBundleDescriptor(Descriptor descriptor) {
        return (BundleDescriptor) descriptor;
    }

    protected VerifierFrameworkContext verifierFrameworkContext = null;
    final protected boolean debug = Verifier.isDebug();
    protected VerifierTestContext context = null;

    /* TestExcludeList.xml Excluded tests */
    private static final String excludeListFileName = "TestExcludeList.xml"; // NOI18N
    private Logger logger = LogDomains.getLogger(
            LogDomains.AVK_VERIFIER_LOGGER);
    protected boolean isDDPresent = false;
    private Vector<TestInformation> test = new Vector<TestInformation>();
}

