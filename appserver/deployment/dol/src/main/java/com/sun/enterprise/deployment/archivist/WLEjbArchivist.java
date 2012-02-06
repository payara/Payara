/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.archivist;

import com.sun.enterprise.deployment.EjbBundleDescriptor;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.runtime.EjbRuntimeDDFile;
import com.sun.enterprise.deployment.io.runtime.GFEjbRuntimeDDFile;
import com.sun.enterprise.deployment.io.runtime.WLEjbRuntimeDDFile;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.xml.sax.SAXParseException;

import java.io.IOException;

@Service
@Scoped(PerLookup.class)
public class WLEjbArchivist extends ExtensionsArchivist {
    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(WLEjbArchivist.class);

    @Override                                                  
    public DeploymentDescriptorFile getStandardDDFile(RootDeploymentDescriptor descriptor) {
        return null;
    }

    @Override
    public DeploymentDescriptorFile getConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        return new WLEjbRuntimeDDFile();
    }

    @Override
    public boolean supportsModuleType(ArchiveType moduleType) {
        return moduleType != null && moduleType.equals(org.glassfish.deployment.common.DeploymentUtils.ejbType());
    }

    @Override
    public Object open(Archivist main, ReadableArchive archive, RootDeploymentDescriptor descriptor) throws IOException, SAXParseException {
        return descriptor;
    }

    @Override
    public RootDeploymentDescriptor getDefaultDescriptor() {
        return new EjbBundleDescriptor();
    }

    @Override
    public DeploymentDescriptorFile getGFCounterPartConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        return new GFEjbRuntimeDDFile();
    }

    @Override
    public DeploymentDescriptorFile getSunCounterPartConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        return new EjbRuntimeDDFile();
    }

    @Override
    public Object readRuntimeDeploymentDescriptor(Archivist main, ReadableArchive archive, RootDeploymentDescriptor descriptor)
            throws IOException, SAXParseException {
        DeploymentDescriptorFile configDD = getConfigurationDDFile(descriptor);
        String configDDPath = configDD.getDeploymentDescriptorPath();
        if (archive.exists(configDDPath)) {
            DOLUtils.getDefaultLogger().warning(
                    localStrings.getLocalString("enterprise.deployment.archivist.DDNotSupported",
                    "Ignore {0} as it is not supported in this release.", new Object[]{configDDPath}));
        }
        return descriptor;
    }
}

