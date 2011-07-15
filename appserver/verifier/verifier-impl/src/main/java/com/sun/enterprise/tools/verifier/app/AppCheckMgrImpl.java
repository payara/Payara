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

package com.sun.enterprise.tools.verifier.app;

import com.sun.enterprise.deployment.Application;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.tools.verifier.CheckMgr;
import com.sun.enterprise.tools.verifier.VerifierFrameworkContext;
import com.sun.enterprise.tools.verifier.JarCheck;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

/**
 * Application harness
 */

public class AppCheckMgrImpl extends CheckMgr implements JarCheck {

    /**
     * name of the file containing the list of tests for the Application
     * architecture
     */
    private static final String testsListFileName = "TestNamesApp.xml"; // NOI18N
    private static final String sunONETestsListFileName = getSunPrefix()
            .concat(testsListFileName);

    public AppCheckMgrImpl(VerifierFrameworkContext verifierFrameworkContext) {
        this.verifierFrameworkContext = verifierFrameworkContext;
    }

    /**
     * Check application for spec. conformance
     *
     * @param descriptor Application descriptor
     */
    public void check(Descriptor descriptor) throws Exception {
        // run persistence tests first.
        checkPersistenceUnits(Application.class.cast(descriptor));

        if (verifierFrameworkContext.isPartition() &&
                !verifierFrameworkContext.isApp())
            return;
        // all tests for embedding application
        super.check(descriptor);
    }

    /**
     * return the configuration file name for the list of tests pertinent to the
     * web app space (jsp and servlet)
     *
     * @return <code>String</code> filename containing the list of tests
     */
    protected String getTestsListFileName() {
        return testsListFileName;
    }

    /**
     * return the configuration file name for the list of tests pertinent to the
     * application scope (SunONE)
     *
     * @return <code>String</code> filename containing the list of tests
     */
    protected String getSunONETestsListFileName() {
        return sunONETestsListFileName;
    }

    /**
     *
     * @param descriptor
     * @return the name of the archive
     */
    protected String getArchiveUri(Descriptor descriptor) {
        return ((Application) descriptor).getName();
    }

    /**
     *
     * @param descriptor
     * @return version of spec the archive corresponds to.
     */
    protected String getSchemaVersion(Descriptor descriptor) {
        return ((RootDeploymentDescriptor) descriptor).getSpecVersion();
    }

    /**
     * Sets the module name in Result object.
     *
     * @param r result object
     */
    protected void setModuleName(Result r) {
        r.setModuleName(Result.APP);
    }

    protected ComponentNameConstructor getComponentNameConstructor(
            Descriptor descriptor) {
        return new ComponentNameConstructor((Application)descriptor);
    }

}
