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

package com.sun.enterprise.glassfish.web;

import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import com.sun.logging.LogDomains;
import org.apache.naming.resources.FileDirContext;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.web.sniffer.WarDetector;
import org.glassfish.loader.util.ASClassLoaderUtil;
import javax.inject.Inject;
import javax.inject.Named;
import org.jvnet.hk2.annotations.Service;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * Implementation of the ArchiveHandler for war files.
 *
 * @author Jerome Dochez, Sanjeeb Sahoo
 */
@Service(name= WarDetector.ARCHIVE_TYPE)
public class WarHandler extends AbstractArchiveHandler {
    @Inject @Named(WarDetector.ARCHIVE_TYPE)
    ArchiveDetector detector;
    private static final String GLASSFISH_WEB_XML = "WEB-INF/glassfish-web.xml";
    private static final String SUN_WEB_XML = "WEB-INF/sun-web.xml";
    private static final String WEBLOGIC_XML = "WEB-INF/weblogic.xml";
    private static final Logger logger = LogDomains.getLogger(WarHandler.class, LogDomains.WEB_LOGGER);
    private static final ResourceBundle rb = logger.getResourceBundle();

    @Override
    public String getArchiveType() {
        return WarDetector.ARCHIVE_TYPE;
    }

    @Override
    public String getVersionIdentifier(ReadableArchive archive) {
        String versionIdentifierValue = null;
        try {
            GlassFishWebXmlParser gfWebXMLParser = new GlassFishWebXmlParser(null);
            versionIdentifierValue = gfWebXMLParser.extractVersionIdentifierValue(archive);
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.getMessage());
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
        return versionIdentifierValue;
    }

    @Override
    public boolean handles(ReadableArchive archive) throws IOException {
        return detector.handles(archive);
    }

    @Override
    public ClassLoader getClassLoader(final ClassLoader parent, DeploymentContext context) {
        WebappClassLoader cloader = AccessController.doPrivileged(new PrivilegedAction<WebappClassLoader>() {
            @Override
            public WebappClassLoader run() {
                return new WebappClassLoader(parent);
            }
        });
        try {
            FileDirContext r = new FileDirContext();
            File base = new File(context.getSource().getURI());
            r.setDocBase(base.getAbsolutePath());

            cloader.setResources(r);
            cloader.addRepository("WEB-INF/classes/", new File(base, "WEB-INF/classes/"));
            if (context.getScratchDir("ejb") != null) {
                cloader.addRepository(context.getScratchDir("ejb").toURI().toURL().toString().concat("/"));
            }
            if (context.getScratchDir("jsp") != null) {
                cloader.setWorkDir(context.getScratchDir("jsp"));
            }

             // add libraries referenced from manifest
            for (URL url : getManifestLibraries(context)) {
                cloader.addRepository(url.toString());
            }

            WebXmlParser webXmlParser = null;
            if ((new File(base, GLASSFISH_WEB_XML)).exists()) {
                webXmlParser = new GlassFishWebXmlParser(base.getAbsolutePath());
            } else if ((new File(base, SUN_WEB_XML)).exists()) {
                webXmlParser = new SunWebXmlParser(base.getAbsolutePath());
            } else if ((new File(base, WEBLOGIC_XML)).exists()) {
                webXmlParser = new WeblogicXmlParser(base.getAbsolutePath());
            } else {
                webXmlParser = new GlassFishWebXmlParser(base.getAbsolutePath());
            }

            configureLoaderAttributes(cloader, webXmlParser, base);
            configureLoaderProperties(cloader, webXmlParser, base);
            
        } catch(MalformedURLException malex) {
            logger.log(Level.SEVERE, malex.getMessage());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, malex.getMessage(), malex);            
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

        cloader.start();

        return cloader;
    }

    protected void configureLoaderAttributes(WebappClassLoader cloader,
            WebXmlParser webXmlParser, File base) {

        boolean delegate = webXmlParser.isDelegate();
        cloader.setDelegate(delegate);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("WebModule[" + webXmlParser.getBase() +
                        "]: Setting delegate to " + delegate);
        }

        String extraClassPath = webXmlParser.getExtraClassPath();
        if (extraClassPath != null) {
            // Parse the extra classpath into its ':' and ';' separated
            // components. Ignore ':' as a separator if it is preceded by
            // '\'
            String[] pathElements = extraClassPath.split(";|((?<!\\\\):)");
            if (pathElements != null) {
                for (String path : pathElements) {
                    path = path.replace("\\:", ":");
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("WarHandler[" + webXmlParser.getBase() +
                                    "]: Adding " + path +
                                    " to the classpath");
                    }

                    try {
                        URL url = new URL(path);
                        cloader.addRepository(path);
                    } catch (MalformedURLException mue1) {
                        // Not a URL, interpret as file
                        File file = new File(path);
                        // START GlassFish 904
                        if (!file.isAbsolute()) {
                            // Resolve relative extra class path to the
                            // context's docroot
                            file = new File(base.getPath(), path);
                        }
                        // END GlassFish 904

                        try {
                            URL url = file.toURI().toURL();
                            cloader.addRepository(url.toString());
                        } catch (MalformedURLException mue2) {
                            String msg = rb.getString(
                                "webcontainer.classpathError");
                            Object[] params = { path };
                            msg = MessageFormat.format(msg, params);
                            logger.log(Level.SEVERE, msg, mue2);
                        }
                    }
                }
            }
        }
    }

    protected void configureLoaderProperties(WebappClassLoader cloader,
            WebXmlParser webXmlParser, File base) {

        cloader.setUseMyFaces(webXmlParser.isUseBundledJSF());

        File libDir = new File(base, "WEB-INF/lib");
        if (libDir.exists()) {
            int baseFileLen = base.getPath().length();
            final boolean ignoreHiddenJarFiles = webXmlParser.isIgnoreHiddenJarFiles();

            for (File file : libDir.listFiles(
                    new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            String fileName = pathname.getName();
                            return ((fileName.endsWith(".jar") ||
                                        fileName.endsWith(".zip")) &&
                                    (!ignoreHiddenJarFiles ||
                                    !fileName.startsWith(".")));
                        }
                    }))
            {
                try {
                    if (file.isDirectory()) {
                        // support exploded jar file
                        cloader.addRepository("WEB-INF/lib/" + file.getName() + "/", file);
                    } else {
                        cloader.addJar(file.getPath().substring(baseFileLen),
                                new JarFile(file), file);
                    }
                } catch (Exception e) {
                    // Catch and ignore any exception in case the JAR file
                    // is empty.
                }
            }
        }
    }


    // ---- inner class ----
    protected abstract class WebXmlParser {
        protected String baseStr = null;
        protected XMLStreamReader parser = null;

        protected boolean delegate = true;

        protected boolean ignoreHiddenJarFiles = false;
        protected boolean useBundledJSF = false;
        protected String extraClassPath = null;

        WebXmlParser(String baseStr) 
                throws XMLStreamException, FileNotFoundException {

            this.baseStr = baseStr;
            InputStream input = null;
            File f = new File(baseStr, getXmlFileName());
            if (f.exists()) {
                input = new FileInputStream(f);
                try {
                    read(input);
                } finally {
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch(Exception ex) {
                            // ignore
                        }
                    }
                    if (input != null) {
                        try {
                            input.close();
                        } catch(Exception ex) {
                            // ignore
                        }
                    }
                }
            }
        }

        protected abstract String getXmlFileName();

        /**
         * This method will parse the input stream and set the XMLStreamReader
         * object for latter use.
         *
         * @param input InputStream
         * @exception XMLStreamException;
         */
        protected abstract void read(InputStream input) throws XMLStreamException;

        protected void skipRoot(String name) throws XMLStreamException {
            while (true) {
                int event = parser.next();
                if (event == START_ELEMENT) {
                    String localName = parser.getLocalName();
                    if (!name.equals(localName)) {
                        String msg = rb.getString("webcontainer.unexpectedXmlElement");
                        msg = MessageFormat.format(msg, new Object[] { name, localName }); 
                        throw new XMLStreamException(msg);
                    }
                    return;
                }
            }
        }

        protected void skipSubTree(String name) throws XMLStreamException {
            while (true) {
                int event = parser.next();
                if (event == END_DOCUMENT) {
                    throw new XMLStreamException(rb.getString("webcontainer.unexpectedEndDocument"));
                } else if (event == END_ELEMENT && name.equals(parser.getLocalName())) {
                    return;
                }
            }
        }

        String getBase() {
            return baseStr;
        }

        boolean isDelegate() {
            return delegate;
        }

        boolean isIgnoreHiddenJarFiles() {
            return ignoreHiddenJarFiles;
        }

        String getExtraClassPath() {
            return extraClassPath;
        }

        boolean isUseBundledJSF() {
            return useBundledJSF;
        }
    }

    protected class SunWebXmlParser extends WebXmlParser {
        //XXX need to compute the default delegate depending on the version of dtd
        /*
         * The DOL will *always* return a value: If 'delegate' has not been
         * configured in sun-web.xml, its default value will be returned,
         * which is FALSE in the case of sun-web-app_2_2-0.dtd and
         * sun-web-app_2_3-0.dtd, and TRUE in the case of
         * sun-web-app_2_4-0.dtd.
         */

        SunWebXmlParser(String baseStr) throws XMLStreamException, FileNotFoundException {
            super(baseStr);
        }

        @Override
        protected String getXmlFileName() {
            return SUN_WEB_XML;
        }

        protected String getRootElementName() {
            return "sun-web-app";
        }

        @Override
        protected void read(InputStream input) throws XMLStreamException {
            parser = getXMLInputFactory().createXMLStreamReader(input);

            int event = 0;
            boolean inClassLoader = false;
            skipRoot(getRootElementName());

            while (parser.hasNext() && (event = parser.next()) != END_DOCUMENT) {
                if (event == START_ELEMENT) {
                    String name = parser.getLocalName();
                    if ("class-loader".equals(name)) {
                        int count = parser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            String attrName = parser.getAttributeName(i).getLocalPart();
                            if ("delegate".equals(attrName)) {
                                delegate = Boolean.valueOf(parser.getAttributeValue(i));
                            } else if ("extra-class-path".equals(attrName)) {
                                extraClassPath = parser.getAttributeValue(i);
                            } else if ("dynamic-reload-interval".equals(attrName)) {
                                if (parser.getAttributeValue(i) != null) {
                                    // Log warning if dynamic-reload-interval is specified
                                    // in sun-web.xml since it is not supported
                                    if (logger.isLoggable(Level.WARNING)) {
                                        logger.log(Level.WARNING, "webcontainer.dynamicReloadInterval");
                                    }
                                }
                            }
                        }
                        inClassLoader = true;
                    } else if (inClassLoader && "property".equals(name)) {
                        int count = parser.getAttributeCount();
                        String propName = null;
                        String value = null;
                        for (int i = 0; i < count; i++) {
                            String attrName = parser.getAttributeName(i).getLocalPart();
                            if ("name".equals(attrName)) {
                                propName = parser.getAttributeValue(i);
                            } else if ("value".equals(attrName)) {
                                value = parser.getAttributeValue(i);
                            }
                        }

                        if (propName == null || value == null) {
                            throw new IllegalArgumentException(
                                rb.getString("webcontainer.nullWebProperty"));
                        }

                        if ("ignoreHiddenJarFiles".equals(propName)) {
                            ignoreHiddenJarFiles = Boolean.valueOf(value);
                        } else {
                            Object[] params = { propName, value };
                            if (logger.isLoggable(Level.WARNING)) {
                                logger.log(Level.WARNING, "webcontainer.invalidProperty",
                                           params);
                            }
                        }
                    } else if ("property".equals(name)) {
                        int count = parser.getAttributeCount();
                        String propName = null;
                        String value = null;
                        for (int i = 0; i < count; i++) {
                            String attrName = parser.getAttributeName(i).getLocalPart();
                            if ("name".equals(attrName)) {
                                propName = parser.getAttributeValue(i);
                            } else if ("value".equals(attrName)) {
                                value = parser.getAttributeValue(i);
                            }
                        }

                        if (propName == null || value == null) {
                            throw new IllegalArgumentException(
                                rb.getString("webcontainer.nullWebProperty"));
                        }

                        if("useMyFaces".equalsIgnoreCase(propName)) {
                            useBundledJSF = Boolean.valueOf(value);
                        } else if("useBundledJsf".equalsIgnoreCase(propName)) {
                            useBundledJSF = Boolean.valueOf(value);
                        }
                    } else {
                        skipSubTree(name);
                    }
                } else if (inClassLoader && event == END_ELEMENT) {
                    if ("class-loader".equals(parser.getLocalName())) {
                        inClassLoader = false;
                    }
                }
            }
        }
    }

    protected class GlassFishWebXmlParser extends SunWebXmlParser {

        GlassFishWebXmlParser(String baseStr) throws XMLStreamException, FileNotFoundException {
            super(baseStr);
        }

        protected String extractVersionIdentifierValue(ReadableArchive archive) throws XMLStreamException, IOException{

            InputStream input = null;
            String versionIdentifierValue = null;

            try
            {
                input = archive.getEntry( getXmlFileName() );

                if (input != null) {

                    // parse elements only from glassfish-web
                    parser = getXMLInputFactory().createXMLStreamReader(input);

                    int event = 0;
                    skipRoot(getRootElementName());

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
            }
            finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch(Exception e) {
                        // ignore
                    }
                }
            }

            return  versionIdentifierValue;
        }

        @Override
        protected String getXmlFileName() {
            return GLASSFISH_WEB_XML;
        }

        @Override
        protected String getRootElementName() {
            return "glassfish-web-app";
        }
    }

    protected class WeblogicXmlParser extends WebXmlParser {
        WeblogicXmlParser(String baseStr) throws XMLStreamException, FileNotFoundException {
            super(baseStr);
        }

        @Override
        protected String getXmlFileName() {
            return WEBLOGIC_XML;
        }

        @Override
        protected void read(InputStream input) throws XMLStreamException {
            parser = getXMLInputFactory().createXMLStreamReader(input);

            skipRoot("weblogic-web-app");

            int event = 0;
            while (parser.hasNext() && (event = parser.next()) != END_DOCUMENT) {
                if (event == START_ELEMENT) {
                    String name = parser.getLocalName();
                    if ("prefer-web-inf-classes".equals(name)) {
                        //weblogic DD has default "false" for perfer-web-inf-classes
                        delegate = !Boolean.parseBoolean(parser.getElementText());
                        break;
                    }  else if (!"container-descriptor".equals(name)) {
                        skipSubTree(name);
                    }
                }
            }
        }
    }

    /**
     * Returns the classpath URIs for this archive.
     *
     * @param archive file
     * @return classpath URIs for this archive
     */
    @Override
    public List<URI> getClassPathURIs(ReadableArchive archive) {
        List<URI> uris = super.getClassPathURIs(archive);
        try {
            File archiveFile = new File(archive.getURI());
            if (archiveFile.exists() && archiveFile.isDirectory()) {
                uris.add(new URI(archive.getURI().toString()+"WEB-INF/classes/"));
                File webInf = new File(archiveFile, "WEB-INF");
                File webInfLib = new File(webInf, "lib");
                if (webInfLib.exists()) {
                    uris.addAll(ASClassLoaderUtil.getLibDirectoryJarURIs(webInfLib));
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return uris;
    }
}
