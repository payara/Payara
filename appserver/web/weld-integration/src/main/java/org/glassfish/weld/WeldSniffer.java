/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import static org.glassfish.weld.WeldUtils.EXPANDED_JAR_SUFFIX;
import static org.glassfish.weld.WeldUtils.EXPANDED_RAR_SUFFIX;
import static org.glassfish.weld.WeldUtils.JAR_SUFFIX;
import static org.glassfish.weld.WeldUtils.META_INF_BEANS_XML;
import static org.glassfish.weld.WeldUtils.SEPARATOR_CHAR;
import static org.glassfish.weld.WeldUtils.WEB_INF;
import static org.glassfish.weld.WeldUtils.WEB_INF_BEANS_XML;
import static org.glassfish.weld.WeldUtils.WEB_INF_LIB;

import java.io.IOException;
import java.util.Enumeration;

import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.deployment.GenericSniffer;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

/**
 * Implementation of the Sniffer for Weld.
 */
@Service(name="weld")
@Scoped(Singleton.class)
public class WeldSniffer extends GenericSniffer implements Sniffer {

    private static final String[] containers = { "org.glassfish.weld.WeldContainer" };


    public WeldSniffer() {
        // We do not haGenericSniffer(String containerName, String appStigma, String urlPattern
        super("weld", null /* appStigma */, null /* urlPattern */);
    }

    /**
     * Returns true if the archive contains beans.xml as defined by packaging rules of Weld 
     */
    @Override
    public boolean handles(ReadableArchive archive, ClassLoader loader) {
        boolean isWeldArchive = false;
        // scan for beans.xml in expected locations. If at least one is found, this is
        // a Weld archive
        //
        if (isEntryPresent(archive, WEB_INF)) {
            isWeldArchive = isEntryPresent(archive, WEB_INF_BEANS_XML);

            if (!isWeldArchive) {
                // Check jars under WEB_INF/lib
                if (isEntryPresent(archive, WEB_INF_LIB)) {
                    isWeldArchive = scanLibDir(archive, WEB_INF_LIB); 
                } 
            } 
        } 

        // TODO This doesn't seem to match the ReadableArchive for a stand-alone ejb-jar.
        // It might only be true for an ejb-jar wihtin an .ear.  Revisit when officially
        // adding support for .ears
        String archiveName = archive.getName();
        if (!isWeldArchive && archiveName != null && archiveName.endsWith(EXPANDED_JAR_SUFFIX)) {
            isWeldArchive = isEntryPresent(archive, META_INF_BEANS_XML);
        }

        // If stand-alone ejb-jar
        if (!isWeldArchive && isEntryPresent(archive, META_INF_BEANS_XML) ) {
            isWeldArchive = true;
        }     

        if (!isWeldArchive && archiveName != null && archiveName.endsWith(EXPANDED_RAR_SUFFIX)) {
            isWeldArchive = isEntryPresent(archive, META_INF_BEANS_XML);
        }
        
        return isWeldArchive;
    }

    private boolean scanLibDir(ReadableArchive archive, String libLocation) {
        boolean entryPresent = false;
        if (libLocation != null && !libLocation.isEmpty()) { 
            Enumeration<String> entries = archive.entries(libLocation);
            while (entries.hasMoreElements() && !entryPresent) {
                String entryName = entries.nextElement();
                // if a jar in lib dir and not WEB-INF/lib/foo/bar.jar
                if (entryName.endsWith(JAR_SUFFIX) && 
                    entryName.indexOf(SEPARATOR_CHAR, libLocation.length() + 1 ) == -1 ) {
                    try {
                        ReadableArchive jarInLib = archive.getSubArchive(entryName);
                        entryPresent = isEntryPresent(jarInLib, META_INF_BEANS_XML);
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
}

