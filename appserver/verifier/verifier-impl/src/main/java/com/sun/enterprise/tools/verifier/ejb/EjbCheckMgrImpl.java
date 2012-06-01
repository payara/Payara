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

package com.sun.enterprise.tools.verifier.ejb;

import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.tools.verifier.CheckMgr;
import com.sun.enterprise.tools.verifier.JarCheck;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.TestInformation;
import com.sun.enterprise.tools.verifier.VerifierFrameworkContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.dd.ParseDD;
import com.sun.enterprise.tools.verifier.wsclient.WebServiceClientCheckMgrImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.jdo.spi.persistence.support.ejb.ejbc.JDOCodeGenerator;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.io.EjbDeploymentDescriptorFile;
import org.glassfish.ejb.deployment.util.EjbBundleValidator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Ejb harness
 */
public class EjbCheckMgrImpl extends CheckMgr implements JarCheck {

    /**
     * name of the file containing the list of tests for the ejb architecture
     */
    private static final String testsListFileName = "TestNamesEjb.xml"; // NOI18N
    private static final String sunONETestsListFileName = getSunPrefix()
            .concat(testsListFileName);
    // the JDO Code generator needs to be initialized once per BundleDescriptor
    private JDOCodeGenerator jdc = new JDOCodeGenerator();

    public EjbCheckMgrImpl(VerifierFrameworkContext verifierFrameworkContext) {
        this.verifierFrameworkContext = verifierFrameworkContext;
    }

    /**
     * Check Ejb for spec. conformance
     *
     * @param descriptor Ejb descriptor
     */
    public void check(Descriptor descriptor) throws Exception {
        // run persistence tests first.
        checkPersistenceUnits(EjbBundleDescriptorImpl.class.cast(descriptor));
        // an EjbBundleDescriptor can have an WebServicesDescriptor
        checkWebServices(descriptor);
        // an EjbBundleDescriptor can have  WebService References
        checkWebServicesClient(descriptor);

        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isEjb())
            return;

        EjbBundleDescriptorImpl bundleDescriptor = (EjbBundleDescriptorImpl) descriptor;
        setDescClassLoader(bundleDescriptor);
        // DOL (jerome): is asking us to call this in some cases, like when
        // an ejb-ref is unresolved etc.
        try {
            EjbBundleValidator validator = new EjbBundleValidator();
            validator.accept(bundleDescriptor);
        } catch (Exception e) {
        } // nothing can be done
        
        // initialize JDOC if bundle has CMP's
        if (bundleDescriptor.containsCMPEntity()) {
            try {
                // See bug #6274161. We now pass an additional boolean
                // to indicate whether we are in portable or AS mode.
                jdc.init(bundleDescriptor, context.getClassLoader(),
                        getAbstractArchiveUri(bundleDescriptor),
                        verifierFrameworkContext.isPortabilityMode());
            } catch (Throwable ex) {
                context.setJDOException(ex);
            }
        }
        // set the JDO Codegenerator into the context
        context.setJDOCodeGenerator(jdc);
        
        // run the ParseDD test
        if (bundleDescriptor.getSpecVersion().compareTo("2.1") < 0) { // NOI18N
            EjbDeploymentDescriptorFile ddf = new EjbDeploymentDescriptorFile();
            File file = new File(getAbstractArchiveUri(bundleDescriptor),
                    ddf.getDeploymentDescriptorPath());
            FileInputStream is = new FileInputStream(file);
            try {
                if (is != null) {
                    Result result = new ParseDD().validateEJBDescriptor(is);
                    result.setComponentName(new File(bundleDescriptor.getModuleDescriptor().
                            getArchiveUri()).getName());
                    setModuleName(result);
                    verifierFrameworkContext.getResultManager().add(result);
                }
            } finally {
                try {
                    if(is != null) {
                        is.close();
                    }
                } catch (Exception e) {}
            }
        }

        for (Iterator itr = bundleDescriptor.getEjbs().iterator();
             itr.hasNext();) {
            EjbDescriptor ejbDescriptor = (EjbDescriptor) itr.next();
            super.check(ejbDescriptor);
        }

        if (bundleDescriptor.containsCMPEntity() &&
                context.getJDOException() == null) {
            jdc.cleanup();
             context.setJDOCodeGenerator(null);
        }
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
     * @return <code>String</code> filename containing sunone tests
     */
    protected String getSunONETestsListFileName() {
        return sunONETestsListFileName;
    }

    protected void checkWebServicesClient(Descriptor descriptor)
            throws Exception {
        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isWebServicesClient())
            return;
        EjbBundleDescriptorImpl desc = (EjbBundleDescriptorImpl) descriptor;
        WebServiceClientCheckMgrImpl webServiceClientCheckMgr = new WebServiceClientCheckMgrImpl(
                verifierFrameworkContext);
        if (desc.hasWebServiceClients()) {
            Set ejbdescs = desc.getEjbs();
            Iterator ejbIt = ejbdescs.iterator();

            while (ejbIt.hasNext()) {
                EjbDescriptor ejbDesc = (EjbDescriptor) ejbIt.next();
                context.setEjbDescriptorForServiceRef(ejbDesc);
                Set serviceRefDescriptors = ejbDesc.getServiceReferenceDescriptors();
                Iterator it = serviceRefDescriptors.iterator();
                while (it.hasNext()) {
                    webServiceClientCheckMgr.setVerifierContext(context);
                    webServiceClientCheckMgr.check(
                            (ServiceReferenceDescriptor) it.next());
                }
            }
            context.setEjbDescriptorForServiceRef(null);
        }
    }

    protected String getSchemaVersion(Descriptor descriptor) {
        return getBundleDescriptor(descriptor).getSpecVersion();
    }

    protected void setModuleName(Result r) {
        r.setModuleName(Result.EJB);
    }

    protected EjbBundleDescriptorImpl getBundleDescriptor(Descriptor descriptor) {
        return ((EjbDescriptor) descriptor).getEjbBundleDescriptor();
    }

    /**
     * entity and mdb assertions should not be run for session descriptors and 
     * similarly the other way round.
     */ 
    protected boolean isApplicable(TestInformation test, Descriptor descriptor) {
        String testName = test.getClassName();
        if(descriptor instanceof EjbSessionDescriptor &&
                (testName.indexOf("tests.ejb.entity")>=0 || // NOI18N
                testName.indexOf("tests.ejb.messagebean")>=0)) // NOI18N
            return false;
        if(descriptor instanceof EjbEntityDescriptor &&
                (testName.indexOf("tests.ejb.session")>=0 || // NOI18N
                testName.indexOf("tests.ejb.messagebean")>=0)) // NOI18N
            return false;
        if(descriptor instanceof EjbMessageBeanDescriptor &&
                (testName.indexOf("tests.ejb.session")>=0 || // NOI18N
                testName.indexOf("tests.ejb.entity")>=0)) // NOI18N
            return false;
        return true;
    }

    private String getAbstractArchiveUri(EjbBundleDescriptorImpl desc) {
        String archBase = context.getAbstractArchive().getURI().toString();
        ModuleDescriptor mdesc = desc.getModuleDescriptor();
        if(mdesc.isStandalone()) {
            return archBase;
        } else {
            return archBase + "/" +
                    FileUtils.makeFriendlyFilename(mdesc.getArchiveUri());
        }
    }

    private void setDescClassLoader(EjbBundleDescriptorImpl bundleDescriptor) {
        Iterator bundleItr = bundleDescriptor.getEjbs().iterator();
        while (bundleItr.hasNext()) {
            EjbDescriptor descriptor = (EjbDescriptor) bundleItr.next();
            if (descriptor instanceof IASEjbCMPEntityDescriptor) {
                ((IASEjbCMPEntityDescriptor) (descriptor)).setClassLoader(
                        context.getClassLoader());
            }
        }
    }

    protected ComponentNameConstructor getComponentNameConstructor(
            Descriptor descriptor) {
        return new ComponentNameConstructor((EjbDescriptor)descriptor);
    }

}
