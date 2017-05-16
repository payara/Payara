/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.embed.impl;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.apf.ProcessingResult;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.internal.embedded.*;
import org.glassfish.web.LogFacade;
import org.glassfish.web.deployment.archivist.WebArchivist;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;

/**
 * @author Jerome Dochez
 */
@Service @PerLookup
public class EmbeddedWebArchivist extends WebArchivist {

    private static final Logger _logger = LogFacade.getLogger();

    private final ModuleScanner scanner = new ModuleScanner() {

            final Set<Class> elements = new HashSet<Class>();

            @Override
            public void process(ReadableArchive archiveFile, Object bundleDesc, ClassLoader classLoader, Parser parser)
                    throws IOException {
                // in embedded mode, we don't scan archive, we just process all classes.
                Enumeration<String> entries = archiveFile.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement();
                    if (entry.endsWith(".class")) {
                        try {
                            elements.add(classLoader.loadClass(toClassName(entry)));
                        } catch (ClassNotFoundException e) {
                            deplLogger.log(Level.FINER,
                                           MessageFormat.format(
                                            _logger.getResourceBundle().getString(LogFacade.CANNOT_LOAD_CLASS), entry),
                                    e);
                        }
                    }
                }

            }

            private String toClassName(String entryName) {
                String name = entryName.substring("WEB-INF/classes/".length(), entryName.length()-".class".length());
                return name.replaceAll("/",".");

            }

            public void process(File archiveFile, Object bundleDesc, ClassLoader classLoader) throws IOException {

            }

            @Override
            public Set getElements() {
                return elements;
            }

    };


    static private URL defaultWebXmlLocation = null;

    protected void setDefaultWebXml(URL defaultWebXml) {
        defaultWebXmlLocation = defaultWebXml;
    }

    @Override
    protected URL getDefaultWebXML() throws IOException {
        if (defaultWebXmlLocation != null) {
            return defaultWebXmlLocation;
        } else {
            URL defaultWebXml = super.getDefaultWebXML();
            return defaultWebXml == null ? getClass().getClassLoader().getResource(
                    "org/glassfish/web/embed/default-web.xml") : defaultWebXml;
        }
    }

    @Override
    protected ProcessingResult processAnnotations(RootDeploymentDescriptor bundleDesc,
                                               ModuleScanner scanner,
                                               ReadableArchive archive)
            throws AnnotationProcessorException, IOException {

        // in embedded mode, I ignore all scanners and parse all possible classes.
        if (archive instanceof ScatteredArchive) {
            return super.processAnnotations(bundleDesc, this.scanner, archive);
        } else {
            return super.processAnnotations(bundleDesc, scanner, archive);
        }
    }
}
