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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.annotation.introspection.EjbComponentAnnotationScanner;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFor;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.util.AnnotationDetector;
import com.sun.enterprise.deployment.util.DOLUtils;

import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.ejb.deployment.archive.EjbType;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.io.EjbDeploymentDescriptorFile;
import org.glassfish.ejb.deployment.util.EjbBundleValidator;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * This class is responsible for handling J2EE EJB Bundlearchive files.
 *
 * @author  Jerome Dochez
 */
@Service
@PerLookup
@ArchivistFor(EjbType.ARCHIVE_TYPE)
public class EjbArchivist extends Archivist<EjbBundleDescriptorImpl> {

    @Inject
    private EjbType ejbType;

    /**
     * @return the  module type handled by this archivist
     * as defined in the application DTD
     *
     */
    @Override
    public ArchiveType getModuleType() {
        return ejbType;
    }

    /**
     * Set the DOL descriptor  for this Archivist, used by super classes
     */
    public void setDescriptor(Application descriptor) {
        // this is acceptable if the application actually represents
        // a standalone module
        Set<EjbBundleDescriptorImpl> ejbBundles = descriptor.getBundleDescriptors(EjbBundleDescriptorImpl.class);
        if (ejbBundles.size()>0) {
            this.descriptor = ejbBundles.iterator().next();
            if (this.descriptor.getModuleDescriptor().isStandalone())
                return;
            else
                this.descriptor=null;
        }
    }

    /**
     * @return the DeploymentDescriptorFile responsible for handling
     * standard deployment descriptor
     */
    @Override
    public DeploymentDescriptorFile getStandardDDFile() {
        if (standardDD == null) {
            standardDD = new EjbDeploymentDescriptorFile();
        }
        return standardDD;
    }

    /**
     * @return the list of the DeploymentDescriptorFile responsible for
     *         handling the configuration deployment descriptors
     */
    public List<ConfigurationDeploymentDescriptorFile> getConfigurationDDFiles() {
        if (confDDFiles == null) {
            confDDFiles = DOLUtils.getConfigurationDeploymentDescriptorFiles(habitat, EjbContainerUtil.EJB_CONTAINER_NAME);
        }
        return confDDFiles;
    }

    /**
     * @return a default BundleDescriptor for this archivist
     */
    @Override
    public EjbBundleDescriptorImpl getDefaultBundleDescriptor() {
        return new EjbBundleDescriptorImpl();
    }

    /**
     * perform any post deployment descriptor reading action
     *
     * @param descriptor deployment descriptor for the module
     * @param archive the module archive
     */
    @Override
    protected void postOpen(EjbBundleDescriptorImpl descriptor, ReadableArchive archive)
        throws IOException
    {
        super.postOpen(descriptor, archive);
        postValidate(descriptor, archive);
    }

    /**
     * validates the DOL Objects associated with this archivist, usually
     * it requires that a class loader being set on this archivist or passed
     * as a parameter
     */
    @Override
    public void validate(ClassLoader aClassLoader) {
        ClassLoader cl = aClassLoader;
        if (cl==null) {
            cl = classLoader;
        }
        if (cl==null) {
            return;
        }
        descriptor.setClassLoader(cl);
        descriptor.visit(new EjbBundleValidator());
    }

    @Override
    protected String getArchiveExtension() {
        return EJB_EXTENSION;
    }

    @Override
    protected boolean postHandles(ReadableArchive abstractArchive)
            throws IOException {
        AnnotationDetector detector =
                    new AnnotationDetector(new EjbComponentAnnotationScanner());
        return (!DeploymentUtils.isArchiveOfType(abstractArchive, DOLUtils.warType(), locator)) &&
                detector.hasAnnotationInArchiveWithNoScanning(abstractArchive);
    }
}
