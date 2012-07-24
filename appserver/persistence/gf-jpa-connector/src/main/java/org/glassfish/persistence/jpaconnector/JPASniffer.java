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


import org.glassfish.internal.deployment.GenericSniffer;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;
import javax.enterprise.deploy.shared.ModuleType;

import java.util.Enumeration;
import java.io.IOException;


/**
 * Implementation of the Sniffer for JPA.
 *
 * @author Mitesh Meswani
 */
@Service(name="jpa")
@Singleton
public class JPASniffer  extends GenericSniffer {

    private static final String[] containers = { "org.glassfish.persistence.jpa.JPAContainer" };

    public JPASniffer() {
        // We do not haGenericSniffer(String containerName, String appStigma, String urlPattern
        super("jpa", null /* appStigma */, null /* urlPattern */);
    }

    private static char SEPERATOR_CHAR = '/';
    private static final String WEB_INF                  = "WEB-INF";
    private static final String LIB                      = "lib";
    private static final String WEB_INF_LIB              = WEB_INF + SEPERATOR_CHAR + LIB;
    private static final String WEB_INF_CLASSSES         = WEB_INF + SEPERATOR_CHAR + "classes";
    private static final String META_INF_PERSISTENCE_XML = "META-INF" + SEPERATOR_CHAR + "persistence.xml";
    private static final String WEB_INF_CLASSSES_META_INF_PERSISTENCE_XML = WEB_INF_CLASSSES + SEPERATOR_CHAR + META_INF_PERSISTENCE_XML;
    private static final String JAR_SUFFIX = ".jar";
    /**
     * Returns true if the archive contains persistence.xml as defined by packaging rules of JPA
     * Tries to getResource("META-INF/persitsence.xml") on current classLoader. If it succeeds, current archive is a pu
     * root.
     * This method will be called for each bundle inside an application which would include
     * .war (the resource can be present in WEB-INF/classes or WEB-INF/lib/pu.jar),
     * ejb.jar (the resource can be present in root of the jar),
     */
    @Override
    public boolean handles(ReadableArchive location) {
            boolean isJPAArchive = false;

            // scan for persistence.xml in expected locations. If at least one is found, this is
            // a jpa archive

            //Scan for the war case
            if(isEntryPresent(location, WEB_INF)) {
                // First check for  "WEB-INF/classes/META-INF/persistence.xml"
                isJPAArchive = isEntryPresent(location, WEB_INF_CLASSSES_META_INF_PERSISTENCE_XML);
                if (!isJPAArchive) {
                    // Check in WEB-INF/lib dir
                    if (isEntryPresent(location, WEB_INF_LIB)) {
                        isJPAArchive = scanForPURootsInLibDir(location, WEB_INF_LIB);
                    } // if (isEntryPresent(location, WEB_INF_LIb))
                } // if (!isJPAArchive)
            } else {
                //Check for ejb jar case
                isJPAArchive = isEntryPresent(location, META_INF_PERSISTENCE_XML);
            }
            return isJPAArchive;
        }

    protected boolean scanForPURootsInLibDir(ReadableArchive parentArchive, String libLocation) {
        boolean puRootPresent = false;
        if (libLocation != null && !libLocation.isEmpty()) { // if an application disables lib dir by specifying <library-directory></library-directory>, do not attempt to scan lib dir
            Enumeration<String> entries = parentArchive.entries(libLocation);
            while (entries.hasMoreElements() && !puRootPresent) {
                String entryName = entries.nextElement();
                if (entryName.endsWith(JAR_SUFFIX) && // a jar in lib dir
                        entryName.indexOf(SEPERATOR_CHAR, libLocation.length() + 1 ) == -1 ) { // && not WEB-INf/lib/foo/bar.jar
                    try {
                        ReadableArchive jarInLib = parentArchive.getSubArchive(entryName);
                        puRootPresent = isEntryPresent(jarInLib, META_INF_PERSISTENCE_XML);
                        jarInLib.close();
                    } catch (IOException e) {
                        // Something went wrong while reading the jar. Do not attempt to scan it
                    } // catch
                } // if (entryName.endsWith(JAR_SUFFIX))
            } // while
        }
        return puRootPresent;
    }

    private boolean isEntryPresent(ReadableArchive location, String entry) {
            boolean entryPresent = false;
            try {
                entryPresent = location.exists(entry);
            } catch (IOException e) {
                // ignore
            }
            return entryPresent;
        }

    public String[] getContainersNames() {
        return containers;
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
        if (archiveType.toString().equals(ModuleType.WAR.toString()) ||
            archiveType.toString().equals(ModuleType.EJB.toString()) ||
            archiveType.toString().equals(ModuleType.CAR.toString())) {
            return true;
        }
        return false;
    }
}

