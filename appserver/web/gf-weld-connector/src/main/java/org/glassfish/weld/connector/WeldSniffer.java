/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.connector;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.deployment.GenericSniffer;
import org.jvnet.hk2.annotations.Service;

import javax.enterprise.deploy.shared.ModuleType;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Implementation of the Sniffer for Weld.
 */
@Service(name="weld")
@Singleton
public class WeldSniffer extends GenericSniffer {

    private static final String[] containers = { "org.glassfish.weld.WeldContainer" };


    public WeldSniffer() {
        // We do not haGenericSniffer(String containerName, String appStigma, String urlPattern
        super("weld", null /* appStigma */, null /* urlPattern */);
    }

    /**
     * Returns true if the archive contains beans.xml as defined by packaging rules of Weld
     */
    @Override
    public boolean handles(ReadableArchive archive) {
        boolean isWeldArchive = false;
        // scan for beans.xml in expected locations. If at least one is found, this is
        // a Weld archive
        //
        if (isEntryPresent(archive, WeldUtils.WEB_INF)) {
            isWeldArchive = isEntryPresent(archive, WeldUtils.WEB_INF_BEANS_XML) ||
                            isEntryPresent(archive, WeldUtils.WEB_INF_CLASSES_META_INF_BEANS_XML);

            if (!isWeldArchive) {
                // Check jars under WEB_INF/lib
                if (isEntryPresent(archive, WeldUtils.WEB_INF_LIB)) {
                    isWeldArchive = scanLibDir(archive, WeldUtils.WEB_INF_LIB);
                }
            }
        }

        // TODO This doesn't seem to match the ReadableArchive for a stand-alone ejb-jar.
        // It might only be true for an ejb-jar within an .ear.  Revisit when officially
        // adding support for .ears
        String archiveName = archive.getName();
        if (!isWeldArchive && archiveName != null && archiveName.endsWith(WeldUtils.EXPANDED_JAR_SUFFIX)) {
            isWeldArchive = isEntryPresent(archive, WeldUtils.META_INF_BEANS_XML);
        }

        // If stand-alone ejb-jar
        if (!isWeldArchive && isEntryPresent(archive, WeldUtils.META_INF_BEANS_XML) ) {
            isWeldArchive = true;
        }

        if (!isWeldArchive && archiveName != null && archiveName.endsWith(WeldUtils.EXPANDED_RAR_SUFFIX)) {
            isWeldArchive = isEntryPresent(archive, WeldUtils.META_INF_BEANS_XML);
            if (!isWeldArchive) {
                // Check jars in root dir of rar
                isWeldArchive = scanLibDir(archive, "");
            }
        }

        return isWeldArchive;
    }

    private boolean scanLibDir(ReadableArchive archive, String libLocation) {
        boolean entryPresent = false;
        if (libLocation != null) {
            Enumeration<String> entries = archive.entries(libLocation);
            while (entries.hasMoreElements() && !entryPresent) {
                String entryName = entries.nextElement();
                // if a jar in lib dir and not WEB-INF/lib/foo/bar.jar
                if (entryName.endsWith(WeldUtils.JAR_SUFFIX) &&
                    entryName.indexOf(WeldUtils.SEPARATOR_CHAR, libLocation.length() + 1 ) == -1 ) {
                    try {
                        ReadableArchive jarInLib = archive.getSubArchive(entryName);
                        entryPresent = isEntryPresent(jarInLib, WeldUtils.META_INF_BEANS_XML);
                        jarInLib.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return entryPresent;
    }

    private boolean isEntryPresent(ReadableArchive archive, String entry) {
        boolean entryPresent = false;
        try {
            entryPresent = archive.exists(entry);
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
            archiveType.toString().equals(ModuleType.RAR.toString())) {
            return true;
        }
        return false;
    }


    @Override
    public String[] getAnnotationNames(DeploymentContext context) {
        return WeldUtils.isImplicitBeanDiscoveryEnabled(context) ? WeldUtils.getCDIEnablingAnnotations(context) : null;
    }


}

