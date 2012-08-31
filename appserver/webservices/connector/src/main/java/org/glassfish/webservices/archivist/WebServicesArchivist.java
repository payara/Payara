/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.archivist;

import com.sun.enterprise.deployment.archivist.ExtensionsArchivist;
import com.sun.enterprise.deployment.archivist.ExtensionsArchivistFor;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.WebServicesDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.io.runtime.WLSWebServicesDeploymentDescriptorFile;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.webservices.io.WebServicesDeploymentDescriptorFile;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.glassfish.hk2.api.PerLookup;

import org.xml.sax.SAXParseException;

/**
 * Extension Archivist for webservices.
 */
@Service
@PerLookup
@ExtensionsArchivistFor("webservices")
public class WebServicesArchivist extends ExtensionsArchivist {

    public DeploymentDescriptorFile getStandardDDFile(RootDeploymentDescriptor descriptor) {
        if (standardDD == null) {
            standardDD = new WebServicesDeploymentDescriptorFile(descriptor);
        } 
        return standardDD;
    }

    /**
     * @return the list of the DeploymentDescriptorFile responsible for
     *         handling the configuration deployment descriptors
     */
    public List<ConfigurationDeploymentDescriptorFile> getConfigurationDDFiles(RootDeploymentDescriptor descriptor) {
        if (confDDFiles == null) {
            confDDFiles = new ArrayList<ConfigurationDeploymentDescriptorFile>();
            confDDFiles.add(new WLSWebServicesDeploymentDescriptorFile(descriptor));
        }
        return confDDFiles;
    }

    public boolean supportsModuleType(ArchiveType moduleType) {
        return (DOLUtils.warType().equals(moduleType) || DOLUtils.ejbType().equals(moduleType));
    }

    @Override
    public Object open(Archivist main, ReadableArchive archive, RootDeploymentDescriptor descriptor) throws IOException, SAXParseException {
        BundleDescriptor bundleDescriptor =
            BundleDescriptor.class.cast(super.open(main, archive, descriptor));

        if (bundleDescriptor != null) {
            return bundleDescriptor.getWebServices();
        } else if (descriptor instanceof BundleDescriptor) {
            return BundleDescriptor.class.cast(descriptor).getWebServices();
        } else throw new IllegalArgumentException("" + descriptor + " is not instance of BundleDescriptor");
    }

    public RootDeploymentDescriptor getDefaultDescriptor() {
        return new WebServicesDescriptor();
    }

    /**
     * writes the deployment descriptors (standard and runtime)
     * to a JarFile using the right deployment descriptor path
     *
     * @param in the input archive
     * @param out the abstract archive file to write to
     */
    @Override
    public void writeDeploymentDescriptors(Archivist main, BundleDescriptor descriptor, ReadableArchive in, WritableArchive out) throws IOException {
        if (descriptor.hasWebServices()) {
            super.writeDeploymentDescriptors(main, descriptor, in, out);
        }
    }
}
