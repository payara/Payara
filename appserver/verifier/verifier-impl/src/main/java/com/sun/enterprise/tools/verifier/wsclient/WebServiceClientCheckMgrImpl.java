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

package com.sun.enterprise.tools.verifier.wsclient;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.api.deployment.archive.ArchiveType;

/**
 * WebServices harness
 */
public class WebServiceClientCheckMgrImpl extends CheckMgr implements JarCheck {

    /**
     * name of the file containing the list of tests for the webservice client
     * architecture
     */
    private static final String testsListFileName = "TestNamesWebServicesClient.xml"; // NOI18N
    private static final String sunONETestsListFileName = getSunPrefix()
            .concat(testsListFileName);
    private String moduleName;

    public WebServiceClientCheckMgrImpl(VerifierFrameworkContext verifierFrameworkContext) {
        this.verifierFrameworkContext = verifierFrameworkContext;
    }

    /**
     * Check Ejb for spec. conformance
     *
     * @param descriptor WebServices descriptor
     */
    public void check(Descriptor descriptor) throws Exception {
        ServiceReferenceDescriptor rootDescriptor = (ServiceReferenceDescriptor) descriptor;
        ArchiveType moduleType = rootDescriptor.getBundleDescriptor()
                .getModuleType();
        if (moduleType != null && moduleType.equals(DOLUtils.ejbType()))
            moduleName = Result.EJB;
        else if (moduleType != null && moduleType.equals(DOLUtils.warType()))
            moduleName = Result.WEB;
        else if (moduleType != null && moduleType.equals(DOLUtils.carType()))
            moduleName = Result.APPCLIENT;
        super.check(rootDescriptor);
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

    /**
     * A webservices client can be an application client or an ejb or a web component
     * For a j2ee1.4 specific webservices client the version of client descriptor
     * is 1.1. For jee 5.0 this version is 1.0
     * @param descriptor
     * @return webservices client descriptor version
     */
    protected String getSchemaVersion(Descriptor descriptor) {
        String wsclientVersion = null;
        String version = ((ServiceReferenceDescriptor) descriptor).getBundleDescriptor()
                .getSpecVersion();
        if(moduleName.equals(Result.EJB)){
            if("2.1".equals(version)) wsclientVersion = "1.1"; // NOI18N
            else if("3.0".equals(version)) wsclientVersion = "1.2"; // NOI18N
        } else if(moduleName.equals(Result.WEB)){
            if("2.4".equals(version)) wsclientVersion = "1.1"; // NOI18N
            else if("2.5".equals(version)) wsclientVersion = "1.2"; // NOI18N
        } else if(moduleName.equals(Result.APPCLIENT)){
            if("1.4".equals(version)) wsclientVersion = "1.1"; // NOI18N
            else if("5".equals(version)) wsclientVersion = "1.2"; // NOI18N
        }
        if(wsclientVersion==null) {
            wsclientVersion = ""; // should we not throw exception?
        }
        return wsclientVersion;
    }

    protected void setModuleName(Result r) {
        r.setModuleName(moduleName);
    }
    
    protected BundleDescriptor getBundleDescriptor(Descriptor descriptor) {
        return ((ServiceReferenceDescriptor)descriptor).getBundleDescriptor();
    }
    protected ComponentNameConstructor getComponentNameConstructor(
            Descriptor descriptor) {
        return new ComponentNameConstructor((ServiceReferenceDescriptor)descriptor);
    }

}
