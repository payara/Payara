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

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.deploy.shared.MultiReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.xml.sax.SAXParseException;

/**
 * PersistenceArchivist for app clients that knows how to scan for PUs in
 * the app client itself as well as in library JARs (or top-level JARs from
 * the containing EAR) that might accompany the app client.
 *
 */
@Service
@ExtensionsArchivistFor("jpa")
public class ACCPersistenceArchivist extends PersistenceArchivist {

    
    
    @Inject
    private ProcessEnvironment env;
    
    @Inject
    private ArchiveFactory archiveFactory;

    @Override
    public boolean supportsModuleType(ArchiveType moduleType) {
        return (moduleType != null && moduleType.equals(DOLUtils.carType())) && (env.getProcessType() == ProcessType.ACC) ;
    }

    @Override
    public Object open(Archivist main, ReadableArchive archive, RootDeploymentDescriptor descriptor) throws IOException, SAXParseException {
        if(deplLogger.isLoggable(Level.FINE)) {
            deplLogger.logp(Level.FINE, "ACCPersistencerArchivist",
                    "readPersistenceDeploymentDescriptors", "archive = {0}",
                    archive.getURI());
        }
        
        final Map<String,ReadableArchive> candidatePersistenceArchives =
                new HashMap<String,ReadableArchive>();
        
        /*
         * The descriptor had better be an ApplicationClientDescriptor!
         */
        if ( ! (descriptor instanceof ApplicationClientDescriptor)) {
            return null;
        }
        
        final ApplicationClientDescriptor acDescr = ApplicationClientDescriptor.class.cast(descriptor);
        
        try {
            final Manifest mf = archive.getManifest();
            final Attributes mainAttrs = mf.getMainAttributes();
            /*
             * We must scan the app client archive itself.  
             */
            URI clientURI = clientURI(archive, acDescr);
            candidatePersistenceArchives.put(clientURI.toASCIIString(), archive);

            /*
             * If this app client 
             * was deployed as part of an EAR then scan any library JARs and, if the
             * client was also deployed or launched in v2-compatibility mode, any
             * top-level JARs in the EAR.
             * 
             * Exactly how we do this depends on whether this is a deployed client
             * (which will reside in a client download directory) or a non-deployed
             * one (which will reside either as a stand-alone client or within an
             * EAR).
             */
            if (isDeployed(mainAttrs)) {
                if ( ! isDeployedClientAlsoStandAlone(mainAttrs)) {
                    addOtherDeployedScanTargets(archive, mainAttrs, candidatePersistenceArchives);
                }
            } else if ( ! isStandAlone(acDescr)) {
                addOtherNondeployedScanTargets(archive, acDescr, candidatePersistenceArchives);
            }

            for (Map.Entry<String, ReadableArchive> pathToArchiveEntry : candidatePersistenceArchives.entrySet()) {
                readPersistenceDeploymentDescriptor(main, 
                        pathToArchiveEntry.getValue(), 
                        pathToArchiveEntry.getKey(), 
                        descriptor);
            }
        } finally {
            for (Map.Entry<String, ReadableArchive> pathToArchiveEntry : candidatePersistenceArchives.entrySet()) {
                //pathToArchiveEntry.getValue().close();
            }
        }
        return null;
    }


    private boolean isDeployedClientAlsoStandAlone(final Attributes mainAttrs) {
        final String relativePathToGroupFacade = mainAttrs.getValue(AppClientArchivist.GLASSFISH_GROUP_FACADE);
        return relativePathToGroupFacade == null;
    }

    private URI clientURI(final ReadableArchive archive,
            final ApplicationClientDescriptor acDesc) throws IOException {
        if (archive instanceof MultiReadableArchive) {
            /*
             * Getting the manifest from a MultiReadableArchive returns the
             * manifest from the facade.
             */
            final Manifest facadeMF = archive.getManifest();
            final Attributes facadeMainAttrs = facadeMF.getMainAttributes();
            final URI clientRelativeURI = URI.create(
                facadeMainAttrs.getValue(AppClientArchivist.GLASSFISH_APPCLIENT));
            if (isDeployedClientAlsoStandAlone(facadeMainAttrs)) {
                return clientRelativeURI;
            }
            /*
             * We need the relative URI to the developer's client JAR within
             * the download directory.
             */
            final URI absURIToClient = ((MultiReadableArchive) archive).getURI(1);
            final String relativeURIPathToAnchorDir =
                    facadeMainAttrs.getValue(AppClientArchivist.GLASSFISH_ANCHOR_DIR);
            final URI absURIToAnchorDir = archive.getURI().resolve(relativeURIPathToAnchorDir);
            return absURIToAnchorDir.relativize(absURIToClient);
        }

        return archive.getURI();
    }
    
    private boolean isStandAlone(final ApplicationClientDescriptor ac) {
        /*
         * For a non-deployed app (this case), the descriptor for a stand-alone
         * app client has a null application value.
         */
        return (ac.getApplication() == null || ac.isStandalone());
    }

    private boolean isDeployed(final Attributes mainAttrs) throws IOException {
        final String gfClient = mainAttrs.getValue(AppClientArchivist.GLASSFISH_APPCLIENT);
        return gfClient != null;
    }
    
    private void addOtherDeployedScanTargets(
            final ReadableArchive archive,
            final Attributes mainAttrs,
            Map<String,ReadableArchive> candidates) throws IOException {
        
        final String otherPUScanTargets = mainAttrs.getValue(
                AppClientArchivist.GLASSFISH_CLIENT_PU_SCAN_TARGETS_NAME);
        
        /*
         * Include library JARs - listed in the facade's Class-Path - and
         * any additional (typically top-level) JARs to be scanned.
         */
        
        addScanTargetsFromURIList(archive, otherPUScanTargets, candidates);
    }
    
    private void addOtherNondeployedScanTargets(final ReadableArchive clientArchive,
            final ApplicationClientDescriptor acDescr,
            final Map<String,ReadableArchive> candidates) {
        
        /*
         * The archive is a non-deployed one.  We know from an earlier check
         * that this is not a stand-alone app client, so we can use the
         * app client archive's parent archive to get to the containing EAR for
         * use in a subarchive scanner.
         */
        final ReadableArchive earArchive = clientArchive.getParentArchive();

        EARBasedPersistenceHelper.addLibraryAndTopLevelCandidates(earArchive,
                acDescr.getApplication(),
                true,
                candidates);
        
        
        
    }
    
    private void addScanTargetsFromURIList(final ReadableArchive archive,
            final String relativeURIList,
            final Map<String,ReadableArchive> candidates) throws IOException {
        if (relativeURIList == null || relativeURIList.isEmpty()) {
            return;
        }
        final String[] relativeURIs = relativeURIList.split(" ");
        for (String uriText : relativeURIs) {
            final URI scanTargetURI = archive.getURI().resolve(uriText);
            candidates.put(uriText, archiveFactory.openArchive(scanTargetURI));
        }
    }
    
    private class AppClientPURootScanner extends SubArchivePURootScanner {

        private final ReadableArchive clientArchive;
        
        private AppClientPURootScanner(final ReadableArchive clientArchive) {
            this.clientArchive = clientArchive;
        }
        
        @Override
        ReadableArchive getSubArchiveToScan(ReadableArchive parentArchive) {
            return clientArchive;
        }

        /**
         * The superclass requires this implementation, but it is never used
         * because we also override getSubArchiveToScan.
         * 
         * @return
         */
        @Override
        String getPathOfSubArchiveToScan() {
            return "";
        }
    }
}
