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

package org.glassfish.ejb.deployment.archivist;

import javax.inject.Inject;
import javax.inject.Provider;

import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;
import com.sun.enterprise.deployment.archivist.ExtensionsArchivist;
import com.sun.enterprise.deployment.archivist.ExtensionsArchivistFor;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.ejb.deployment.annotation.impl.EjbInWarScanner;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptor;
import org.glassfish.ejb.deployment.io.EjbInWarDeploymentDescriptorFile;
import org.glassfish.ejb.deployment.io.EjbInWarRuntimeDDFile;
import org.glassfish.ejb.deployment.io.GFEjbInWarRuntimeDDFile;
import org.glassfish.ejb.deployment.io.WLSEjbInWarRuntimeDDFile;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * @author Mahesh Kannan
 */

@Service
@Scoped(PerLookup.class)
@ExtensionsArchivistFor("ejb")
public class EjbInWarArchivist extends ExtensionsArchivist {

    @Inject
    Provider<EjbInWarScanner> scanner;

    private EjbInWarDeploymentDescriptorFile standardDD;

    private WLSEjbInWarRuntimeDDFile wlsEjbInWarRuntimeDD;

    private GFEjbInWarRuntimeDDFile gfEjbInWarRuntimeDD;

    private EjbInWarRuntimeDDFile ejbInWarRuntimeDD;

    /**
     * @return the DeploymentDescriptorFile responsible for handling
     *         standard deployment descriptor
     */
    @Override
    public DeploymentDescriptorFile getStandardDDFile(RootDeploymentDescriptor descriptor) {
        if (standardDD == null) {
            standardDD = new EjbInWarDeploymentDescriptorFile();
        }
        return standardDD;
    }

    @Override
    public DeploymentDescriptorFile getWLSConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        if (wlsEjbInWarRuntimeDD == null) {
            wlsEjbInWarRuntimeDD = new WLSEjbInWarRuntimeDDFile();
        }
        return wlsEjbInWarRuntimeDD;
    }

    @Override
    public DeploymentDescriptorFile getGFConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        if (gfEjbInWarRuntimeDD == null) {
            gfEjbInWarRuntimeDD = new GFEjbInWarRuntimeDDFile();
        }
        return gfEjbInWarRuntimeDD;
    }

    @Override
    public DeploymentDescriptorFile getSunConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        if (ejbInWarRuntimeDD == null) {
            ejbInWarRuntimeDD = new EjbInWarRuntimeDDFile();
        }
        return ejbInWarRuntimeDD;
    }

    /**
     * Returns the scanner for this archivist, usually it is the scanner registered
     * with the same module type as this archivist, but subclasses can return a
     * different version
     *
     */
    @Override
    public ModuleScanner getScanner() {
        return scanner.get();
    }

    @Override
    public boolean supportsModuleType(ArchiveType moduleType) {
        return moduleType != null && moduleType.equals(DOLUtils.warType());
    }

    @Override
    public RootDeploymentDescriptor getDefaultDescriptor() {
        return new EjbBundleDescriptor();
    }
}

