/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.ejb.deployment;

import static java.security.AccessController.doPrivileged;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.glassfish.deployment.common.DeploymentProperties.COMPATIBILITY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.ejb.LogFacade;
import org.glassfish.ejb.deployment.archive.EjbJarDetector;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.loader.ASURLClassLoader;
import com.sun.enterprise.security.permissionsxml.CommponentType;
import com.sun.enterprise.security.permissionsxml.SetPermissionsAction;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * @author sanjeeb.sahoo@oracle.com
 */
@Service(name = EjbJarDetector.ARCHIVE_TYPE)
public class EjbJarHandler extends AbstractArchiveHandler {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(EjbJarHandler.class);

    private static final Logger _logger = LogFacade.getLogger();

    @Inject
    @Named(EjbJarDetector.ARCHIVE_TYPE)
    private ArchiveDetector detector;

    @Override
    public String getArchiveType() {
        return EjbJarDetector.ARCHIVE_TYPE;
    }

    @Override
    public boolean handles(ReadableArchive archive) throws IOException {
        return detector.handles(archive);
    }

    @Override
    public String getVersionIdentifier(ReadableArchive archive) {
        try {
            return new GFEjbJarXMLParser(archive).extractVersionIdentifierValue(archive);
        } catch (XMLStreamException | IOException e) {
            _logger.severe(e.getMessage());
        }

        return null;
    }

    public ClassLoader getClassLoader(final ClassLoader parent, DeploymentContext context) {
        ASURLClassLoader cloader = AccessController.doPrivileged(new PrivilegedAction<ASURLClassLoader>() {
            @Override
            public ASURLClassLoader run() {
                return new ASURLClassLoader(parent);
            }
        });

        try {
            String compatProp = context.getAppProps().getProperty(COMPATIBILITY);

            // If user does not specify the compatibility property
            // let's see if it's defined in glassfish-ejb-jar.xml
            if (compatProp == null) {
                compatProp = new GFEjbJarXMLParser(context.getSource()).getCompatibilityValue();
                if (compatProp != null) {
                    context.getAppProps().put(COMPATIBILITY, compatProp);
                }
            }

            // If user does not specify the compatibility property
            // let's see if it's defined in sun-ejb-jar.xml
            if (compatProp == null) {
                compatProp = new SunEjbJarXMLParser(context.getSourceDir()).getCompatibilityValue();
                if (compatProp != null) {
                    context.getAppProps().put(DeploymentProperties.COMPATIBILITY, compatProp);
                }
            }

            // If the compatibility property is set to "v2", we should add
            // all the jars under the ejb module root to maintain backward
            // compatibility of v2 jar visibility
            if (compatProp != null && compatProp.equals("v2")) {
                List<URL> moduleRootLibraries = ASClassLoaderUtil.getURLsAsList(null, new File[] { context.getSourceDir() }, true);
                for (URL url : moduleRootLibraries) {
                    cloader.addURL(url);
                }
            }

            cloader.addURL(context.getSource().getURI().toURL());
            cloader.addURL(context.getScratchDir("ejb").toURI().toURL());
            
            // Add libraries referenced from manifest
            for (URL url : getManifestLibraries(context)) {
                cloader.addURL(url);
            }

            try {
                doPrivileged(new SetPermissionsAction(CommponentType.ejb, context, cloader));
            } catch (PrivilegedActionException e) {
                throw new SecurityException(e.getException());
            }

        } catch (Exception e) {
            _logger.severe(e.getMessage());
            throw new RuntimeException(e);
        }

        return cloader;
    }

    private static class GFEjbJarXMLParser {
        private XMLStreamReader parser;
        private String compatValue;

        GFEjbJarXMLParser(ReadableArchive archive) throws XMLStreamException, FileNotFoundException, IOException {
            InputStream input = null;
            File runtimeAltDDFile = archive.getArchiveMetaData(DeploymentProperties.RUNTIME_ALT_DD, File.class);
            if (runtimeAltDDFile != null && runtimeAltDDFile.getPath().indexOf(DescriptorConstants.GF_PREFIX) != -1 && runtimeAltDDFile.exists()
                    && runtimeAltDDFile.isFile()) {
                DOLUtils.validateRuntimeAltDDPath(runtimeAltDDFile.getPath());
                input = new FileInputStream(runtimeAltDDFile);
            } else {
                input = archive.getEntry("META-INF/glassfish-ejb-jar.xml");
            }

            if (input != null) {
                try {
                    read(input);
                } catch (Throwable t) {
                    String msg = localStrings.getLocalString("ejb.deployment.exception_parsing_glassfishejbjarxml",
                            "Error in parsing glassfish-ejb-jar.xml for archive [{0}]: {1}", archive.getURI(), t.getMessage());
                    throw new RuntimeException(msg);
                } finally {
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                    try {
                        input.close();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
        }

        protected String extractVersionIdentifierValue(ReadableArchive archive) throws XMLStreamException, IOException {
            InputStream input = null;
            String versionIdentifierValue = null;
            String rootElement = null;

            try {
                File runtimeAltDDFile = archive.getArchiveMetaData(DeploymentProperties.RUNTIME_ALT_DD, File.class);
                if (runtimeAltDDFile != null && runtimeAltDDFile.getPath().indexOf(DescriptorConstants.GF_PREFIX) != -1 && runtimeAltDDFile.exists()
                        && runtimeAltDDFile.isFile()) {
                    DOLUtils.validateRuntimeAltDDPath(runtimeAltDDFile.getPath());
                    input = new FileInputStream(runtimeAltDDFile);
                } else {
                    input = archive.getEntry("META-INF/glassfish-ejb-jar.xml");
                }

                rootElement = "glassfish-ejb-jar";
                if (input != null) {

                    // Parse elements only from glassfish-ejb
                    parser = getXMLInputFactory().createXMLStreamReader(input);

                    int event = 0;
                    skipRoot(rootElement);

                    while (parser.hasNext() && (event = parser.next()) != END_DOCUMENT) {
                        if (event == START_ELEMENT) {
                            String name = parser.getLocalName();
                            if ("version-identifier".equals(name)) {
                                versionIdentifierValue = parser.getElementText();
                            } else {
                                skipSubTree(name);
                            }
                        }
                    }
                }
            } finally {
                if (parser != null) {
                    try {
                        parser.close();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                if (input != null) {
                    try {
                        input.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            return versionIdentifierValue;
        }

        private void read(InputStream input) throws XMLStreamException {
            parser = getXMLInputFactory().createXMLStreamReader(input);

            int event = 0;
            boolean done = false;
            skipRoot("glassfish-ejb-jar");

            while (!done && (event = parser.next()) != END_DOCUMENT) {

                if (event == START_ELEMENT) {
                    String name = parser.getLocalName();
                    if (COMPATIBILITY.equals(name)) {
                        compatValue = parser.getElementText();
                        done = true;
                    } else {
                        skipSubTree(name);
                    }
                }
            }
        }

        private void skipRoot(String name) throws XMLStreamException {
            while (true) {
                int event = parser.next();
                if (event == START_ELEMENT) {
                    if (!name.equals(parser.getLocalName())) {
                        throw new XMLStreamException();
                    }
                    return;
                }
            }
        }

        private void skipSubTree(String name) throws XMLStreamException {
            while (true) {
                int event = parser.next();
                if (event == END_DOCUMENT) {
                    throw new XMLStreamException();
                } else if (event == END_ELEMENT && name.equals(parser.getLocalName())) {
                    return;
                }
            }
        }

        String getCompatibilityValue() {
            return compatValue;
        }
    }

    private static class SunEjbJarXMLParser {
        private XMLStreamReader parser = null;
        private String compatValue = null;

        SunEjbJarXMLParser(File baseDir) throws XMLStreamException, FileNotFoundException {
            InputStream input = null;
            File f = new File(baseDir, "META-INF/sun-ejb-jar.xml");
            if (f.exists()) {
                input = new FileInputStream(f);
                try {
                    read(input);
                } catch (Throwable t) {
                    String msg = localStrings.getLocalString("ejb.deployment.exception_parsing_sunejbjarxml",
                            "Error in parsing sun-ejb-jar.xml for archive [{0}]: {1}", baseDir, t.getMessage());
                    throw new RuntimeException(msg);
                } finally {
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                    if (input != null) {
                        try {
                            input.close();
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                }
            }
        }

        private void read(InputStream input) throws XMLStreamException {
            parser = getXMLInputFactory().createXMLStreamReader(input);

            int event = 0;
            boolean done = false;
            skipRoot("sun-ejb-jar");

            while (!done && (event = parser.next()) != END_DOCUMENT) {

                if (event == START_ELEMENT) {
                    String name = parser.getLocalName();
                    if (DeploymentProperties.COMPATIBILITY.equals(name)) {
                        compatValue = parser.getElementText();
                        done = true;
                    } else {
                        skipSubTree(name);
                    }
                }
            }
        }

        private void skipRoot(String name) throws XMLStreamException {
            while (true) {
                int event = parser.next();
                if (event == START_ELEMENT) {
                    if (!name.equals(parser.getLocalName())) {
                        throw new XMLStreamException();
                    }
                    return;
                }
            }
        }

        private void skipSubTree(String name) throws XMLStreamException {
            while (true) {
                int event = parser.next();
                if (event == END_DOCUMENT) {
                    throw new XMLStreamException();
                } else if (event == END_ELEMENT && name.equals(parser.getLocalName())) {
                    return;
                }
            }
        }

        String getCompatibilityValue() {
            return compatValue;
        }
    }

}
