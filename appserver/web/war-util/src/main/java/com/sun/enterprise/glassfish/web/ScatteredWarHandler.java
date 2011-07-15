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

package com.sun.enterprise.glassfish.web;

import com.sun.logging.LogDomains;
import org.apache.naming.resources.FileDirContext;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.embedded.ScatteredArchive;
import org.glassfish.web.loader.WebappClassLoader;
import org.jvnet.hk2.annotations.Service;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Dochez
 */
@Service
public class ScatteredWarHandler extends WarHandler {

    private static final Logger logger = LogDomains.getLogger(ScatteredWarHandler.class, LogDomains.DPL_LOGGER);
    
    @Override
    public String getArchiveType() {
        return "scattered-archive";
    }

    @Override
    public boolean handles(ReadableArchive archive) {
        return (archive instanceof ScatteredArchive &&
                ((ScatteredArchive) archive).type()==ScatteredArchive.Builder.type.war);
    }

    @Override
    public ClassLoader getClassLoader(final ClassLoader parent, DeploymentContext context) {
        ScatteredArchive archive = (ScatteredArchive) context.getSource();
        WebappClassLoader cloader = AccessController.doPrivileged(new PrivilegedAction<WebappClassLoader>() {
            @Override
            public WebappClassLoader run() {
                return new WebappClassLoader(parent);
            }
        });
        try {
            FileDirContext r = new FileDirContext();
            File base = archive.getResourcesDir();
            r.setDocBase(base.getAbsolutePath());
            File sunWeb = archive.getFile("WEB-INF/sun-web.xml");
            SunWebXmlParser sunWebXmlParser = null;
            if (sunWeb!=null && sunWeb.exists()) {
                sunWebXmlParser = new SunWebXmlParser(sunWeb.getParentFile().getParent());
            }

            cloader.setResources(r);
            for (URL url : archive.getClassPath()) {
                cloader.addRepository(url.toExternalForm());
            }
            if (context.getScratchDir("jsp") != null) {
                cloader.setWorkDir(context.getScratchDir("jsp"));
            }

            if (sunWebXmlParser!=null) {
                configureLoaderAttributes(cloader, sunWebXmlParser, base);
                configureLoaderProperties(cloader, sunWebXmlParser, base);
            }
        } catch(XMLStreamException xse) {
            logger.log(Level.SEVERE, xse.getMessage());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, xse.getMessage(), xse);
            }
        } catch(FileNotFoundException fnfe) {
            logger.log(Level.SEVERE, fnfe.getMessage());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, fnfe.getMessage(), fnfe);
            }
        }
        try {
            cloader.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cloader;
    }
}
