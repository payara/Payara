/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.module;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorsClassLoaderUtil;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.connectors.connector.module.RarDetector;
import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import com.sun.enterprise.security.perms.SMGlobalPolicyUtil;
import com.sun.enterprise.security.perms.PermsArchiveDelegate;
import com.sun.logging.LogDomains;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Archive handler for resource-adapters
 *
 * @author Jagadish Ramu 
 */
@Service(name= RarDetector.ARCHIVE_TYPE)
public class RarHandler extends AbstractArchiveHandler {
    // This class should be moved to connector runtime along with ConnectorClassLoaderUtil.
    // We should also consider merging connectors-connector with connectors-internal-api

    @Inject
    private ConnectorsClassLoaderUtil loader;

    @Inject
    @Named(RarDetector.ARCHIVE_TYPE)
    private ArchiveDetector detector;

    private Logger _logger = LogDomains.getLogger(RarHandler.class, LogDomains.RSR_LOGGER);

    /**
     * {@inheritDoc}
     */
    public String getArchiveType() {
        return RarDetector.ARCHIVE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean handles(ReadableArchive archive) throws IOException {
        return detector.handles(archive);
    }

    /**
     * {@inheritDoc}
     */
    public ClassLoader getClassLoader(ClassLoader parent, DeploymentContext context) {
        try {
            String moduleDir = context.getSource().getURI().getPath();
            String moduleName = context.getSource().getName();

            List<URI> appLibs = null;
            try {
                appLibs = context.getAppLibs();
                if(_logger.isLoggable(Level.FINEST)){
                    _logger.log(Level.FINEST, "installed libraries (--applibs and EXTENSTION_LIST) for rar " +
                        "[ "+moduleName+" ] :  " + appLibs);
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            
            ClassLoader carCL;
            if (isEmbedded(context)) {
                String applicationName = ConnectorsUtil.getApplicationName(context);
                String embeddedRarName = ConnectorsUtil.getEmbeddedRarModuleName(applicationName, moduleName);
                // ear's classloader hierarchy is : module-CL -> ear-CL (contains all ejb module classpath)
                // -> embedded-RAR-CL -> ear-lib-CL.
                // parent provided here is ear-CL, we need to use
                // ear-lib-CL as parent for embedded-RAR module-CL 
                carCL = loader.createRARClassLoader(moduleDir, parent.getParent().getParent(), embeddedRarName, appLibs);
            } else {
                carCL = loader.createRARClassLoader(moduleDir, null, moduleName, appLibs);
            }
                        
            try {
                final DeploymentContext dc = context;
                final ClassLoader cl = carCL;
                
                AccessController.doPrivileged(
                        new PermsArchiveDelegate.SetPermissionsAction(
                                SMGlobalPolicyUtil.CommponentType.rar, dc, cl));
            } catch (PrivilegedActionException e) {
                throw new SecurityException(e.getException());
            }

            return carCL;
            
        } catch (ConnectorRuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * indicates whether the .rar being deployed is standalone or embedded
     * @param context deployment context
     * @return boolean indicating whether its embedded .rar
     */
    private boolean isEmbedded(DeploymentContext context) {
        ReadableArchive archive = context.getSource();
        return (archive != null && archive.getParentArchive() != null);
    }

    /**
     * Returns the classpath URIs for this archive.
     *
     * @param archive file
     * @return classpath URIs for this archive
     */
    @Override
    public List<URI> getClassPathURIs(ReadableArchive archive) {
        List<URI> uris = super.getClassPathURIs(archive);
        try {
            File archiveFile = new File(archive.getURI());
            if (archiveFile.exists() && archiveFile.isDirectory()) {
                // add top level jars
                uris.addAll(ASClassLoaderUtil.getLibDirectoryJarURIs(archiveFile));
            }
        } catch (Exception e) {
            _logger.log(Level.WARNING, e.getMessage(), e);
        }
        return uris;
    }
}
