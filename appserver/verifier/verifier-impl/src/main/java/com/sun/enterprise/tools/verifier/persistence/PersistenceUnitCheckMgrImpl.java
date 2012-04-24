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

package com.sun.enterprise.tools.verifier.persistence;

import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

/**
 * This class is responsible for checking a PU represented by a {@link
 * PersistenceUnitDescriptor}
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class PersistenceUnitCheckMgrImpl extends CheckMgr {

    // module for which this check mgr is running the test.
    // This string is one of the types defined in Result class.
    private String moduleName;
    private LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();
    

    public PersistenceUnitCheckMgrImpl(
            VerifierFrameworkContext verifierFrameworkContext, VerifierTestContext context) {
        this.verifierFrameworkContext = verifierFrameworkContext;
        this.context = context;
    }

    @Override protected void check(Descriptor descriptor) throws Exception {
        PersistenceUnitDescriptor pu =
                PersistenceUnitDescriptor.class.cast(descriptor);
        RootDeploymentDescriptor rootDD = pu.getParent().getParent();
        if(rootDD.isApplication()) {
            moduleName = Result.APP;
        } else {
            ModuleDescriptor mdesc =
                    BundleDescriptor.class.cast(rootDD).getModuleDescriptor();
            final ArchiveType moduleType = mdesc.getModuleType();
            if(moduleType != null && moduleType.equals(DOLUtils.ejbType())) {
                moduleName = Result.EJB;
            } else if (moduleType != null && moduleType.equals(DOLUtils.warType())) {
                moduleName = Result.WEB;
            } else if (moduleType != null && moduleType.equals(DOLUtils.carType())) {
                moduleName = Result.APPCLIENT;
            } else {
                throw new RuntimeException(
                        smh.getLocalString(getClass().getName()+".exception", // NOI18N
                                "Unknown module type : {0}", // NOI18N
                                new Object[] {moduleType}));
            }
        }
        super.check(descriptor);
    }

    /**
     * We override here because there is nothing like sun-persistence.xml.
     * @param uri
     */
    @Override protected void setRuntimeDDPresent(String uri) {
        isDDPresent = false;
    }

    protected ComponentNameConstructor getComponentNameConstructor(
            Descriptor descriptor) {
        return new ComponentNameConstructor(
                PersistenceUnitDescriptor.class.cast(descriptor));
    }

    protected String getTestsListFileName() {
        return "TestNamesPersistence.xml"; // NOI18N
    }

    protected void setModuleName(Result r) {
        r.setModuleName(moduleName);
    }

    protected String getSchemaVersion(Descriptor descriptor) {
        // A PU inherits its schema version from its parent.
        return PersistenceUnitDescriptor.class.cast(descriptor).getParent().
                getSpecVersion();
    }

    protected String getSunONETestsListFileName() {
        return null;
    }

    /**
     * This method returns the path to the module.
     * @param descriptor is a PersistenceUnitDescriptor
     * @return the path to the module
     */
    protected String getAbstractArchiveUri(Descriptor descriptor) {
        String archBase = context.getAbstractArchive().getURI().toString();
        RootDeploymentDescriptor rootDD =
                PersistenceUnitDescriptor.class.cast(descriptor).getParent().getParent();
        if(rootDD.isApplication()) {
            return archBase;
        } else {
            ModuleDescriptor mdesc =
                    BundleDescriptor.class.cast(rootDD).getModuleDescriptor();
            if(mdesc.isStandalone()) {
                return archBase;
            } else {
                return archBase + "/" +
                        FileUtils.makeFriendlyFilename(mdesc.getArchiveUri());
            }
        }
    }
}
