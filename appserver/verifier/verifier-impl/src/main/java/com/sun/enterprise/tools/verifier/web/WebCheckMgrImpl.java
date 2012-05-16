/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.dd.ParseDD;
import com.sun.enterprise.tools.verifier.wsclient.WebServiceClientCheckMgrImpl;

import org.glassfish.web.deployment.io.WebDeploymentDescriptorFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Web harness
 */
public class WebCheckMgrImpl extends CheckMgr implements JarCheck {

    /**
     * name of the file containing the list of tests for the web architecture
     */
    private static final String testsListFileName = "TestNamesWeb.xml"; // NOI18N
    private static final String sunONETestsListFileName = getSunPrefix()
            .concat(testsListFileName);
    private static TagLibDescriptor[] tlds;

    public WebCheckMgrImpl(VerifierFrameworkContext verifierFrameworkContext) {
        this.verifierFrameworkContext = verifierFrameworkContext;
    }

    /**
     * Check method introduced for WebServices integration
     *
     * @param descriptor Web descriptor
     */
    public void check(Descriptor descriptor) throws Exception {
        // run persistence tests first.
        checkPersistenceUnits(WebBundleDescriptor.class.cast(descriptor));
        // a WebBundleDescriptor can have an WebServicesDescriptor
        checkWebServices(descriptor);
        // a WebBundleDescriptor can have  WebService References
        checkWebServicesClient(descriptor);

        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isWeb())
            return;

        createTaglibDescriptors(descriptor); //create document obj for all tld's defined in the war
        
        createFacesConfigDescriptor(descriptor);
        
        // run the ParseDD test
        if (getSchemaVersion(descriptor).compareTo("2.4") < 0) { // NOI18N
            WebDeploymentDescriptorFile ddf = new WebDeploymentDescriptorFile();
            File file = new File(getAbstractArchiveUri(descriptor),
                    ddf.getDeploymentDescriptorPath());
            FileInputStream is = new FileInputStream(file);
            try {
                if (is != null) {
                    Result result = new ParseDD().validateWebDescriptor(is);
                    result.setComponentName(getArchiveUri(descriptor));
                    setModuleName(result);
                    verifierFrameworkContext.getResultManager().add(result);
                    is.close();
                }
            } finally {
                try {
                    if(is!=null)
                        is.close();
                } catch(Exception e) {}
            }
        }

        super.check(descriptor);
    }

    /**
     * <p/>
     * return the configuration file name for the list of tests pertinent to the
     * web app space (jsp and servlet) </p>
     *
     * @return <code>String</code> filename containing the list of tests
     */
    protected String getTestsListFileName() {
        return testsListFileName;
    }

    /**
     * @return <code>String</code> filename containing the  SunONE tests
     */
    protected String getSunONETestsListFileName() {
        return sunONETestsListFileName;
    }

    /**
     * Create array of TagLibDescriptors for all the jsp tag lib files defined
     * in the war. Set the array in the verifier Context
     *
     * @param descriptor
     */
    protected void createTaglibDescriptors(Descriptor descriptor) {
        TagLibFactory tlf = new TagLibFactory(context, verifierFrameworkContext);
        tlds = tlf.getTagLibDescriptors((WebBundleDescriptor) descriptor);
        if (tlds != null) {
            context.setTagLibDescriptors(tlds);
            setVerifierContext(context);
        }
    }

    /**
     * Create FacesConfigDescriptor
     *
     * @param descriptor
     */
    protected void createFacesConfigDescriptor(Descriptor descriptor) {
        FacesConfigDescriptor d = new FacesConfigDescriptor(context, (WebBundleDescriptor)descriptor);
        context.setFacesConfigDescriptor(d);
    }
    
    protected void checkWebServicesClient(Descriptor descriptor)
            throws Exception {
        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isWebServicesClient())
            return;

        WebBundleDescriptor desc = (WebBundleDescriptor) descriptor;
        WebServiceClientCheckMgrImpl webServiceClientCheckMgr = new WebServiceClientCheckMgrImpl(
                verifierFrameworkContext);
        if (desc.hasWebServiceClients()) {
            Set serviceRefDescriptors = desc.getServiceReferenceDescriptors();
            Iterator it = serviceRefDescriptors.iterator();

            while (it.hasNext()) {
                webServiceClientCheckMgr.setVerifierContext(context);
                webServiceClientCheckMgr.check(
                        (ServiceReferenceDescriptor) it.next());
            }
        }
    }

    protected String getSchemaVersion(Descriptor descriptor) {
        return ((WebBundleDescriptor) descriptor).getSpecVersion();
    }

    protected void setModuleName(Result r) {
        r.setModuleName(Result.WEB);
    }

    /**
     * If the call is from deployment backend and precompilejsp option is set 
     * then there is no need to run the AllJSPsMustBeCompilable test. 
     * @return list of excluded tests
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */ 
    protected Vector<TestInformation> getTestFromExcludeList() throws ParserConfigurationException, SAXException, IOException {
        Vector<TestInformation> tests = super.getTestFromExcludeList();
        if(verifierFrameworkContext.getJspOutDir() !=null) { // pre-compile jsp flag set
            TestInformation ti = new TestInformation();
            ti.setClassName("com.sun.enterprise.tools.verifier.tests.web.AllJSPsMustBeCompilable"); // NOI18N
            tests.addElement(ti);
        }
        return tests;
    }

    protected ComponentNameConstructor getComponentNameConstructor(
            Descriptor descriptor) {
        return new ComponentNameConstructor((WebBundleDescriptor)descriptor);
    }

}
