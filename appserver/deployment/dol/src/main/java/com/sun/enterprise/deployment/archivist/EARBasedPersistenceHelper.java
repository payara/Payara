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

package com.sun.enterprise.deployment.archivist;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.archivist.PersistenceArchivist.SubArchivePURootScanner;
import org.glassfish.deployment.common.ModuleDescriptor;
import java.util.Map;
import java.util.Set;
import org.glassfish.api.deployment.archive.ReadableArchive;

/**
 * Common logic supporting persistence archivists that deal with EARs.
 *
 * @author tjquinn
 */
public class EARBasedPersistenceHelper {

    /**
     * @return true if the jarName corresponds to component jar (like a war or ejb.jar) in an .ear false otherwise
     */
    public static boolean isComponentJar(String jarName, Set<ModuleDescriptor<BundleDescriptor>> moduleDescriptors) {
        boolean isComponentJar = false;
        for (ModuleDescriptor md : moduleDescriptors) {
            String archiveUri = md.getArchiveUri();
            if (jarName.equals(archiveUri)) {
                isComponentJar = true;
                break;
            }
        }
        return isComponentJar;
    }

    /**
     * Adds candidate persistence archives from the EAR's library directory
     * and, if selected, from the top-level.
     * @param earArchive ReadableArchive for the EAR
     * @param app application's descriptor
     * @param includeTopLevel whether or not to include top-level JARs for scanning
     * @param probablePersistentArchives map to which new candidates will be added
     */
    protected static void addLibraryAndTopLevelCandidates(final ReadableArchive earArchive,
            final Application app,
            final boolean includeTopLevel,
            final Map<String,ReadableArchive> probablePersistentArchives) {
        //Get probable archives from root of the ear
        if (includeTopLevel) {
            SubArchivePURootScanner earRootScanner = new EARTopLevelJARPURootScanner(app);
            probablePersistentArchives.putAll(
                    PersistenceArchivist.getProbablePersistenceRoots(earArchive, earRootScanner));
        }

        //Geather all jars in lib of ear
        SubArchivePURootScanner libPURootScannerScanner = new EARLibraryPURootScanner(app);
        probablePersistentArchives.putAll(
                PersistenceArchivist.getProbablePersistenceRoots(earArchive, libPURootScannerScanner));

    }


    /**
     * Allows scanning of library JARs in an EAR.
     * <p>
     * This implementation correctly handles the semantics of the
     * <library-directory> element (and its absence) from the descriptor.
     * That is, if the element is missing use the default value "/lib."  If
     * the element is present and non-null, use it.  If the element is
     * present and empty then no library directory exists in the application.
     */
    static class EARLibraryPURootScanner extends SubArchivePURootScanner {

        private final Application app;

        /**
         * Creates a new instance of the scanner, using the specified
         * Application descriptor.
         *
         * @param app descriptor for the application
         */
        protected EARLibraryPURootScanner(
                final Application app) {
            this.app = app;
        }

        @Override
        String getPathOfSubArchiveToScan() {
            /*
             * Take advantage of the fact that the app's getLibraryDirectory
             * method handles all the semantics of the <library-directory>
             * element.
             */
            return app.getLibraryDirectory();
        }

    }

    /**
     * Allows scanning of the top-level JARs of an EAR.
     */
    static class EARTopLevelJARPURootScanner extends SubArchivePURootScanner {

        private final Application app;

        protected EARTopLevelJARPURootScanner(final Application app) {
            this.app = app;
        }

        @Override
        public String getPathOfSubArchiveToScan() {
            // We are scanning root of ear.
            return "";
        }

        @Override
        public boolean isProbablePuRootJar(String jarName) {
            return super.isProbablePuRootJar(jarName) &&
                    // component roots are not scanned while scanning ear. They will be handled
                    // while scanning the component.
                    !isComponentJar(jarName,(app.getModules()));
        }
    }
}
