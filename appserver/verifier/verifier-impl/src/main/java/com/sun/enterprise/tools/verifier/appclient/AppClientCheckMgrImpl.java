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

package com.sun.enterprise.tools.verifier.appclient;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Set;

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.io.AppClientDeploymentDescriptorFile;
import com.sun.enterprise.tools.verifier.CheckMgr;
import com.sun.enterprise.tools.verifier.VerifierFrameworkContext;
import com.sun.enterprise.tools.verifier.JarCheck;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.dd.ParseDD;
import com.sun.enterprise.tools.verifier.wsclient.WebServiceClientCheckMgrImpl;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

/**
 * Application Client harness
 */
public class AppClientCheckMgrImpl extends CheckMgr implements JarCheck {

    /**
     * name of the file containing the list of tests for the application client
     * architecture
     */
    private static final String testsListFileName = "TestNamesAppClient.xml"; // NOI18N
    private static final String sunONETestsListFileName = getSunPrefix()
            .concat(testsListFileName);

    public AppClientCheckMgrImpl(VerifierFrameworkContext verifierFrameworkContext) {
        this.verifierFrameworkContext = verifierFrameworkContext;
    }

    /**
     * Check method introduced for WebServices integration
     *
     * @param descriptor appclient descriptor
     */
    public void check(Descriptor descriptor) throws Exception {
        // run persistence tests first.
        checkPersistenceUnits(ApplicationClientDescriptor.class.cast(descriptor));
        //An ApplicationClient can have WebService References
        checkWebServicesClient(descriptor);

        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isAppClient())
            return;
        // run the ParseDD test
        if (getSchemaVersion(descriptor).compareTo("1.4") < 0) { // NOI18N
            AppClientDeploymentDescriptorFile ddf = new AppClientDeploymentDescriptorFile();
            File file = new File(getAbstractArchiveUri(descriptor),
                    ddf.getDeploymentDescriptorPath());
            FileInputStream is = new FileInputStream(file);
            try {
                if (is != null) {
                    Result result = new ParseDD().validateAppClientDescriptor(is);
                    result.setComponentName(getArchiveUri(descriptor));
                    setModuleName(result);
                    verifierFrameworkContext.getResultManager().add(result);
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
     * return the configuration file name for the list of tests pertinent to the
     * connector architecture
     *
     * @return <code>String</code> filename containing the list of tests
     */
    protected String getTestsListFileName() {
        return testsListFileName;
    }

    /**
     * return the configuration file name for the list of tests pertinent to the
     * application client architecture
     *
     * @return <code>String</code> filename containing the list of tests
     */
    protected String getSunONETestsListFileName() {
        return sunONETestsListFileName;
    }

    protected String getSchemaVersion(Descriptor descriptor) {
        return ((RootDeploymentDescriptor) descriptor).getSpecVersion();
    }

    protected void setModuleName(Result r) {
        r.setModuleName(Result.APPCLIENT);
    }

    protected void checkWebServicesClient(Descriptor descriptor)
            throws Exception {
        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isWebServicesClient())
            return;
        WebServiceClientCheckMgrImpl webServiceClientCheckMgr = 
                                new WebServiceClientCheckMgrImpl(verifierFrameworkContext);
        ApplicationClientDescriptor desc = (ApplicationClientDescriptor) descriptor;
        if (desc.hasWebServiceClients()) {
            Set serviceRefDescriptors = desc.getServiceReferenceDescriptors();
            Iterator it = serviceRefDescriptors.iterator();
            while (it.hasNext()) {
                webServiceClientCheckMgr.setVerifierContext(context);
                webServiceClientCheckMgr.check(
                        (ServiceReferenceDescriptor) it.next());
            }
        } else // set not applicable for all tests in WebServices for this Appclient Bundle 
            webServiceClientCheckMgr.setVerifierContext(context);
    }

    protected ComponentNameConstructor getComponentNameConstructor(
            Descriptor descriptor) {
        return new ComponentNameConstructor((ApplicationClientDescriptor)descriptor);
    }

}
