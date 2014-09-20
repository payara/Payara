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
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.PersistenceDeploymentDescriptorFile;
import org.glassfish.api.deployment.archive.ArchiveType;
import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Enumeration;

import org.glassfish.logging.annotation.LogMessageInfo;

public abstract class PersistenceArchivist extends ExtensionsArchivist {
    protected static final String JAR_EXT = ".jar";
    protected static final char SEPERATOR_CHAR = '/';

    public static final Logger deplLogger = com.sun.enterprise.deployment.util.DOLUtils.deplLogger;

  @LogMessageInfo(message = "Exception caught:  {0} for the subarchve indicated by the path:  {1}.", cause="An exception was caught when the subarchive was opened because the subarchive was not present.", action="Correct the archive so that the subarchive is present.", level="SEVERE")
      private static final String EXCEPTION_CAUGHT = "AS-DEPLOYMENT-00004";

  public PersistenceArchivist() {
  }

    public DeploymentDescriptorFile getStandardDDFile(RootDeploymentDescriptor descriptor) {
        if (standardDD == null) {
             standardDD = new PersistenceDeploymentDescriptorFile();
        }
        return standardDD;
    }

    /**
     * @return the list of the DeploymentDescriptorFile responsible for
     *         handling the configuration deployment descriptors
     */
    public List<ConfigurationDeploymentDescriptorFile> getConfigurationDDFiles(RootDeploymentDescriptor descriptor) {
        return Collections.emptyList();
    }

    @Override
    // Need to be overridden in derived class to define module type it supports
    public abstract boolean supportsModuleType(ArchiveType moduleType);

    @Override
    public Object open(Archivist main, ReadableArchive archive, RootDeploymentDescriptor descriptor)
            throws IOException, SAXParseException {
        String puRoot = getPuRoot(archive);
        readPersistenceDeploymentDescriptor(main, archive, puRoot, descriptor);
        return null;  // return null so that the descritor does not get added twice to extensions
    }

    /** @return true of given path is probable pu root */
    public static boolean isProbablePuRootJar(String path) {
        return isJarEntry(path) && checkIsInRootOfArchive(path);
    }

    /** @return true if given path is in root of archive */
    private static boolean checkIsInRootOfArchive(String path) {
        return path.indexOf('/') == -1;
    }

    /** @return true of give path corresponds to a jar entry */
    private static boolean isJarEntry(String path) {
        return path.endsWith(JAR_EXT);
    }


    protected PersistenceUnitsDescriptor readPersistenceDeploymentDescriptor(Archivist main,
            ReadableArchive subArchive, String puRoot, RootDeploymentDescriptor descriptor)
            throws IOException, SAXParseException {

        final String subArchiveURI = subArchive.getURI().getSchemeSpecificPart();
        if (deplLogger.isLoggable(Level.FINE)) {
            deplLogger.logp(Level.FINE, "Archivist",
                    "readPersistenceDeploymentDescriptor",
                    "PURoot = [{0}] subArchive = {1}",
                    new Object[]{puRoot, subArchiveURI});
        }
        if (descriptor.getExtensionsDescriptors(PersistenceUnitsDescriptor.class, puRoot) != null) {
            if (deplLogger.isLoggable(Level.FINE)) {
                deplLogger.logp(Level.FINE, "Archivist",
                        "readPersistenceDeploymentDescriptor",
                        "PU has been already read for = {0}",
                        subArchiveURI);
            }
            return null;
        }
        PersistenceUnitsDescriptor persistenceUnitsDescriptor =
                    PersistenceUnitsDescriptor.class.cast(super.open(main, subArchive, descriptor));

        if (persistenceUnitsDescriptor!=null) {

            persistenceUnitsDescriptor.setParent(descriptor);
            persistenceUnitsDescriptor.setPuRoot(puRoot);            
            descriptor.addExtensionDescriptor(PersistenceUnitsDescriptor.class,persistenceUnitsDescriptor, puRoot);
        }

        return persistenceUnitsDescriptor;
    }

    public <T extends RootDeploymentDescriptor> T getDefaultDescriptor() {
        return null;
    }

    /**
     * Gets probable persitence roots from given parentArchive using given subArchiveRootScanner
     * @param parentArchive the parentArchive within which probable persitence roots need to be scanned
     * @param subArchivePURootScanner the scanner instance used for the scan
     * @see com.sun.enterprise.deployment.archivist.EarPersistenceArchivist.SubArchivePURootScanner
     * @return Map of puroot path to probable puroot archive.
     */
    protected static Map<String, ReadableArchive> getProbablePersistenceRoots(ReadableArchive parentArchive, SubArchivePURootScanner subArchivePURootScanner) {
        Map<String, ReadableArchive> probablePersitenceArchives = new HashMap<String, ReadableArchive>();
        ReadableArchive  archiveToScan = subArchivePURootScanner.getSubArchiveToScan(parentArchive);
        if(archiveToScan != null) { // The subarchive exists
            Enumeration<String> entries = archiveToScan.entries();
            String puRootPrefix = subArchivePURootScanner.getPurRootPrefix();
            while(entries.hasMoreElements()) {
                String entry = entries.nextElement();
                if(subArchivePURootScanner.isProbablePuRootJar(entry)) {
                    ReadableArchive puRootArchive = getSubArchive(archiveToScan, entry, false /* expect entry to be present */);
                    if(puRootArchive != null) {
                        String puRoot = puRootPrefix + entry;
                        probablePersitenceArchives.put(puRoot, puRootArchive);
                    }
                }
            }

        }
        return probablePersitenceArchives;
    }

    private static ReadableArchive getSubArchive(ReadableArchive parentArchive, String path, boolean expectAbscenceOfSubArchive) {
        ReadableArchive returnedArchive = null;
        try {
            returnedArchive = parentArchive.getSubArchive(path);
        } catch (IOException ioe) {
            // if there is any problem in opening the subarchive, and the subarchive is expected to be present, log the exception
            if(!expectAbscenceOfSubArchive) {
              LogRecord lr = new LogRecord(Level.SEVERE, EXCEPTION_CAUGHT);
              Object args[] = { ioe.getMessage(), path };
              lr.setParameters(args);
              lr.setThrown(ioe);
              deplLogger.log(lr);
            }
        }
        return returnedArchive;
    }

    protected String getPuRoot(ReadableArchive archive) {
        // getPuRoot() is called from Open() of this class to define pu root. This is default implementation to keep compiler happy.
        // It is assumed that subclasses not overriding open() (which are currently ACCPersitenceArchivist and ServerSidePersitenceArchivist)
        // would override this method.
        assert false;
        return null;
    }

    protected static abstract class SubArchivePURootScanner {
        abstract String getPathOfSubArchiveToScan();

        ReadableArchive getSubArchiveToScan(ReadableArchive parentArchive) {
            String pathOfSubArchiveToScan = getPathOfSubArchiveToScan();
            return (pathOfSubArchiveToScan == null || pathOfSubArchiveToScan.isEmpty()) ? parentArchive :
                    getSubArchive(parentArchive, pathOfSubArchiveToScan, true /*It is possible that lib does not exist for a given ear */);
        }

        String getPurRootPrefix() {
            String pathOfSubArchiveToScan = getPathOfSubArchiveToScan();
            return (pathOfSubArchiveToScan == null || pathOfSubArchiveToScan.isEmpty()) ? pathOfSubArchiveToScan : pathOfSubArchiveToScan + SEPERATOR_CHAR;
        }

        boolean isProbablePuRootJar(String jarName) {
            // all jars in root of subarchive are probable pu roots
            boolean probablePuRootJar = PersistenceArchivist.isProbablePuRootJar(jarName);
            if(!probablePuRootJar && isJarEntry(jarName) ) {
                // A jar that is not in root of archive. Log that it will not be scanned
                if (deplLogger.isLoggable(Level.FINE)) {
                    deplLogger.logp(Level.FINE, "PersistenceArchivist",
                            "readPersistenceDeploymentDescriptors",
                            "skipping {0} as it exists inside a directory in {1}.",
                            new Object[]{jarName, getPathOfSubArchiveToScan()});
                }
            }
            return probablePuRootJar;
        }
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
        // we do not write out any persistence deployment descriptors 
        // for now
    }
}
