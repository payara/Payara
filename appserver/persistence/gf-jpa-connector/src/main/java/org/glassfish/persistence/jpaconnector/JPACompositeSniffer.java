/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.jpaconnector;


import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.archivist.EARBasedPersistenceHelper;
import com.sun.enterprise.deployment.archivist.PersistenceArchivist;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.javaee.core.deployment.ApplicationHolder;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;
import javax.inject.Inject;
import javax.enterprise.deploy.shared.ModuleType;

import java.util.Enumeration;
import java.util.Set;

/**
 * Sniffer handling ears
 *
 * @author Mitesh Meswani
 */
@Service(name = "jpaCompositeSniffer")
@Singleton
public class JPACompositeSniffer extends JPASniffer {

    /**
     * Decides whether we have any pu roots at ear level
     */
    public boolean handles(DeploymentContext context) {
        ArchiveType archiveType = habitat.getService(ArchiveType.class, context.getArchiveHandler().getArchiveType());
        if (archiveType != null && !supportsArchiveType(archiveType)) {
            return false;
        }

        // Scans for pu roots in the "lib" dir of an application.
        // We do not scan for PU roots in root of .ear. JPA 2.0 spec will clarify that it is  not a portable use case.
        // It is not portable use case because JavaEE spec implies that jars in root of ears are not visible by default
        // to components (Unless an explicit Class-Path manifest entry is present) and can potentially be loaded by
        // different class loaders (corresponding to each component that refers to it) thus residing in different name
        // space. It does not make sense to make them visible at ear level (and thus in a single name space)
        boolean isJPAApplication = false;
        ApplicationHolder holder = context.getModuleMetaData(ApplicationHolder.class);
        ReadableArchive appRoot = context.getSource();
        if (holder != null && holder.app != null) {
            isJPAApplication = scanForPURootsInLibDir(appRoot, holder.app.getLibraryDirectory());

            if(!isJPAApplication) {
                if(DeploymentUtils.useV2Compatibility(context) ) {
                    //Scan for pu roots in root of ear
                    isJPAApplication = scanForPURRootsInEarRoot(context, holder.app.getModules());
                }
            }
        }
        return isJPAApplication;
    }

    /**
     *
     * This API is used to help determine if the sniffer should recognize
     * the current archive.
     * If the sniffer does not support the archive type associated with
     * the current deployment, the sniffer should not recognize the archive.
     *
     * @param archiveType the archive type to check
     * @return whether the sniffer supports the archive type
     *
     */
    public boolean supportsArchiveType(ArchiveType archiveType) {
        if (archiveType.toString().equals(ModuleType.EAR.toString())) {
            return true;
        }
        return false;
    }


    private boolean scanForPURRootsInEarRoot(DeploymentContext ctx, Set<ModuleDescriptor<BundleDescriptor>> modules) {
        boolean puPresentInEarRoot = false;
        Enumeration<String> entriesInEar = ctx.getSource().entries();
        while(entriesInEar.hasMoreElements() && !puPresentInEarRoot) {
            String entry = entriesInEar.nextElement();
            puPresentInEarRoot = PersistenceArchivist.isProbablePuRootJar(entry) && !EARBasedPersistenceHelper.isComponentJar(entry, modules);
        }
        return puPresentInEarRoot;
    }
}
