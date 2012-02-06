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

package com.sun.enterprise.deployment.archivist;

import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;
import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.xml.sax.SAXParseException;
import org.jvnet.hk2.annotations.Contract;

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An extension archivist is processing extensions deployment descriptors like
 * web services, persistence or even EJB information within a war file.
 *
 * They do not represent a top level archivist, as it is not capable of loading
 * BundleDescriptors directly but require a top level archivist to do so before
 * they can process their own metadata
 *
 * @author Jerome Dochez
 */
@Contract
public abstract class ExtensionsArchivist  {

    protected final Logger logger = LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);

    public abstract DeploymentDescriptorFile getStandardDDFile(RootDeploymentDescriptor descriptor);

    public abstract DeploymentDescriptorFile getConfigurationDDFile(RootDeploymentDescriptor descriptor);

    public abstract boolean supportsModuleType(ArchiveType moduleType);

    public abstract <T extends RootDeploymentDescriptor> T getDefaultDescriptor();

    public ModuleScanner getScanner() {
        return null;
    }

    public DeploymentDescriptorFile getSunConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        return null;
    }

    public DeploymentDescriptorFile getGFCounterPartConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        return null;
    }

    public DeploymentDescriptorFile getSunCounterPartConfigurationDDFile(RootDeploymentDescriptor descriptor) {
        return null;
    }

    public <T extends RootDeploymentDescriptor> void addExtension(RootDeploymentDescriptor root, RootDeploymentDescriptor extension) {
        root.addExtensionDescriptor(extension.getClass(), extension, null);
        extension.setModuleDescriptor(root.getModuleDescriptor());
    }

    public Object open(Archivist main, ReadableArchive archive, RootDeploymentDescriptor descriptor)
            throws IOException, SAXParseException {


        DeploymentDescriptorFile confDD = getStandardDDFile(descriptor);
         if (archive.getURI() != null) {
             confDD.setErrorReportingString(archive.getURI().getSchemeSpecificPart());
         }
         InputStream is = null;
         try {
             is = archive.getEntry(confDD.getDeploymentDescriptorPath());
             if (is == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Deployment descriptor: " +
                                    confDD.getDeploymentDescriptorPath(),
                                    " does not exist in archive: " + 
                                    archive.getURI().getSchemeSpecificPart());
                }

             } else {
                 confDD.setXMLValidation(main.getXMLValidation());
                 confDD.setXMLValidationLevel(main.getXMLValidationLevel());
                 return confDD.read(descriptor, is);
             }
         } finally {
             if (is != null) {
                 is.close();
             }
         }
         return null;
     }

     public Object readRuntimeDeploymentDescriptor(Archivist main, ReadableArchive archive, RootDeploymentDescriptor descriptor)
            throws IOException, SAXParseException {

        DeploymentDescriptorFile confDD = getConfigurationDDFile(descriptor);

        // if this extension archivist has no runtime DD, just return the 
        // original descriptor
        if (confDD == null) {
            return descriptor;
        }

        if (archive.getURI() != null) {
            confDD.setErrorReportingString(archive.getURI().getSchemeSpecificPart());
        }
        InputStream is = null;
        InputStream is2 = null;
        InputStream is3 = null;
        try {
            String confDDPath = confDD.getDeploymentDescriptorPath();
            is = archive.getEntry(confDDPath);
            if (is == null) {
                confDD = getSunConfigurationDDFile(descriptor);
                if (confDD != null) {
                    confDDPath = confDD.getDeploymentDescriptorPath();
                    is = archive.getEntry(confDDPath);
                }
            }

            if (is != null) {
                DeploymentDescriptorFile gfConfDD = 
                    getGFCounterPartConfigurationDDFile(descriptor);
                if (gfConfDD != null) {
                   is2 = archive.getEntry(
                       gfConfDD.getDeploymentDescriptorPath()); 
                   // when Glassfish counterpart configuration file is present
                   // we should ignore this extension configuration file
                   if (is2 != null) {
                       logger.log(Level.WARNING, 
                           "gf.counterpart.configdd.exists",
                           new Object[] {
                               confDDPath,      
                               archive.getURI().getSchemeSpecificPart(), 
                               gfConfDD.getDeploymentDescriptorPath()});
                       return null;
                   }
                }

                DeploymentDescriptorFile sunConfDD =
                    getSunCounterPartConfigurationDDFile(descriptor);
                if (sunConfDD != null) {
                   is3 = archive.getEntry(
                       sunConfDD.getDeploymentDescriptorPath());
                   // when Sun counterpart configuration file is present
                   // we should ignore this extension configuration file
                   if (is3 != null) {
                       logger.log(Level.WARNING, 
                           "sun.counterpart.configdd.exists",
                           new Object[] {
                               confDDPath,
                               archive.getURI().getSchemeSpecificPart(),
                               sunConfDD.getDeploymentDescriptorPath()});
                       return null;
                   }
                }

                confDD.setXMLValidation(main.getRuntimeXMLValidation());
                confDD.setXMLValidationLevel(main.getRuntimeXMLValidationLevel());
                return confDD.read(descriptor, is);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (is2 != null) {
                is2.close();
            }
            if (is3 != null) {
                is3.close();
            }
        }
        return null;
    }

}
