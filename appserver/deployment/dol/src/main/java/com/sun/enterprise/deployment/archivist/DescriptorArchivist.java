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

package com.sun.enterprise.deployment.archivist;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * This class is responsible for writing deployment descriptors 
 * after a deployment action has occured to a abstract archive instance.
 *
 * @author  Jerome Dochez
 */
@Service
public class DescriptorArchivist {
    @Inject
    protected ArchivistFactory archivistFactory;

    @Inject
    private  Provider<ApplicationArchivist> archivistProvider;

    /**
     * writes an application deployment descriptors
     * @param the application object
     * @param the abstract archive
     */
    public void write(Application application, ReadableArchive in,
        WritableArchive out) throws IOException {
        if (application.isVirtual()) {
            ModuleDescriptor aModule = (ModuleDescriptor) application.getModules().iterator().next();
            Archivist moduleArchivist = archivistFactory.getArchivist(aModule.getModuleType());
            write((BundleDescriptor)aModule.getDescriptor(), moduleArchivist, in, out);
        } else {
            // this is a real application.
            // let's start by writing out all submodules deployment descriptors
            for (ModuleDescriptor aModule : application.getModules()) {
                Archivist moduleArchivist = archivistFactory.getArchivist(aModule.getModuleType());
                WritableArchive moduleArchive = out.createSubArchive(aModule.getArchiveUri());
                ReadableArchive moduleArchive2 = in.getSubArchive(aModule.getArchiveUri());
                write((BundleDescriptor)aModule.getDescriptor(),  moduleArchivist, moduleArchive2, moduleArchive);
            }
            
            // now let's write the application descriptor
            ApplicationArchivist archivist = archivistProvider.get();
            archivist.setDescriptor(application);
            archivist.writeDeploymentDescriptors(in, out); 
        }
    }
    
    /**
     * writes a bundle descriptor
     * @param the bundle descriptor
     * @param the archivist responsible for writing such bundle type
     * @param the archive to write to
     */
    protected void write(BundleDescriptor bundle, Archivist archivist, ReadableArchive in, WritableArchive out)
        throws IOException
    {
        archivist.setDescriptor(bundle);
        archivist.writeDeploymentDescriptors(in, out);
    }
}
