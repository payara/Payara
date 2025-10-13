/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.appclient.server.core;

import static com.sun.enterprise.security.permissionsxml.CommponentType.car;
import static java.security.AccessController.doPrivileged;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.appclient.server.connector.CarDetector;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import com.sun.enterprise.loader.ASURLClassLoader;
import com.sun.enterprise.security.permissionsxml.CommponentType;
import com.sun.enterprise.security.permissionsxml.SetPermissionsAction;

/**
 * @author sanjeeb.sahoo@oracle.com
 */
@Service(name = CarDetector.ARCHIVE_TYPE)
public class CarHandler extends AbstractArchiveHandler {

    @Inject @Named(CarDetector.ARCHIVE_TYPE)
    private ArchiveDetector detector;

    private static final Logger _logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER, 
                JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);

    @Override
    public String getArchiveType() {
        return detector.getArchiveType().toString();
    }

    @Override
    public String getVersionIdentifier(ReadableArchive archive) {
        String versionIdentifier = null;
        try {
            GFCarXMLParser gfXMLParser = new GFCarXMLParser();
            versionIdentifier = gfXMLParser.extractVersionIdentifierValue(archive);
        } catch (IOException e) {
            _logger.log(Level.SEVERE, e.getMessage());
        } catch (XMLStreamException e) {
            _logger.log(Level.SEVERE, e.getMessage());
        }
        return versionIdentifier;

    }

    @Override
    public boolean handles(ReadableArchive archive) throws IOException {
        return detector.handles(archive);
    }

    @Override
    public ClassLoader getClassLoader(final ClassLoader parent, DeploymentContext context) {
        ASURLClassLoader cloader = AccessController.doPrivileged(new PrivilegedAction<ASURLClassLoader>() {
            @Override
            public ASURLClassLoader run() {
                return new ASURLClassLoader(parent);
            }
        });
        try {
            cloader.addURL(context.getSource().getURI().toURL());
            
            // Add libraries referenced from manifest
            for (URL url : getManifestLibraries(context)) {
                cloader.addURL(url);
            }
                       
            try {
                doPrivileged(new SetPermissionsAction(car, context, cloader));
            } catch (PrivilegedActionException e) {
                throw new SecurityException(e.getException());
            }


        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        
        return cloader;
    }

    private static class GFCarXMLParser {
        private XMLStreamReader parser = null;

        protected String extractVersionIdentifierValue(ReadableArchive archive) throws XMLStreamException, IOException {

            InputStream input = null;
            String versionIdentifierValue = null;
            String rootElement = null;

            try {
                rootElement = "glassfish-application-client";
                input = archive.getEntry("META-INF/glassfish-application-client.xml");
                if (input != null) {
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
                    parser.close();
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

    }


}
