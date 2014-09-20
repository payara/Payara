/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import com.sun.enterprise.security.perms.SMGlobalPolicyUtil;
import com.sun.enterprise.security.perms.PermsArchiveDelegate;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.apache.naming.resources.WebDirContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.web.sniffer.WarDetector;
import org.glassfish.loader.util.ASClassLoaderUtil;
import javax.inject.Inject;
import javax.inject.Named;
import org.jvnet.hk2.config.types.Property;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * Implementation of the ArchiveHandler for war files.
 *
 * @author Jerome Dochez, Sanjeeb Sahoo, Shing Wai Chan
 */
@Service(name= WarDetector.ARCHIVE_TYPE)
public class WarHandler extends AbstractArchiveHandler {

    private static final String GLASSFISH_WEB_XML = "WEB-INF/glassfish-web.xml";
    private static final String SUN_WEB_XML = "WEB-INF/sun-web.xml";
    private static final String WEBLOGIC_XML = "WEB-INF/weblogic.xml";
    private static final String WAR_CONTEXT_XML = "META-INF/context.xml";
    private static final String DEFAULT_CONTEXT_XML = "config/context.xml";
    private static final Logger logger = WebappClassLoader.logger;
    private static final ResourceBundle rb = logger.getResourceBundle();
    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(WarHandler.class);


    @LogMessageInfo(
            message = "extra-class-path component [{0}] is not a valid pathname",
            level = "SEVERE",
            cause = "A naming exception is encountered",
            action = "Check the list of resources")
    public static final String CLASSPATH_ERROR = "AS-WEB-UTIL-00027";

    @LogMessageInfo(
            message = "The clearReferencesStatic is not consistent in context.xml for virtual servers",
            level = "WARNING")
    public static final String INCONSISTENT_CLEAR_REFERENCE_STATIC = "AS-WEB-UTIL-00028";

    @LogMessageInfo(
            message = "class-loader attribute dynamic-reload-interval in sun-web.xml not supported",
            level = "WARNING")
    public static final String DYNAMIC_RELOAD_INTERVAL = "AS-WEB-UTIL-00029";

    @LogMessageInfo(
            message = "Property element in sun-web.xml has null 'name' or 'value'",
            level = "WARNING")
    public static final String NULL_WEB_PROPERTY = "AS-WEB-UTIL-00030";

    @LogMessageInfo(
            message = "Ignoring invalid property [{0}] = [{1}]",
            level = "WARNING")
    public static final String INVALID_PROPERTY = "AS-WEB-UTIL-00031";

    @LogMessageInfo(
            message = "The xml element should be [{0}] rather than [{1}]",
            level = "INFO")
    public static final String UNEXPECTED_XML_ELEMENT = "AS-WEB-UTIL-00032";

    @LogMessageInfo(
            message = "This is an unexpected end of document",
            level = "WARNING")
    public static final String UNEXPECTED_END_DOCUMENT = "AS-WEB-UTIL-00033";

    //the following two system properties need to be in sync with DOLUtils
    private static final boolean gfDDOverWLSDD = Boolean.valueOf(System.getProperty("gfdd.over.wlsdd"));
    private static final boolean ignoreWLSDD = Boolean.valueOf(System.getProperty("ignore.wlsdd"));

    @Inject @Named(WarDetector.ARCHIVE_TYPE)
    private ArchiveDetector detector;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Override
    public String getArchiveType() {
        return WarDetector.ARCHIVE_TYPE;
    }

    @Override
    public String getVersionIdentifier(ReadableArchive archive) {
        String versionIdentifierValue = null;
        try {
            WebXmlParser webXmlParser = getWebXmlParser(archive);
            versionIdentifierValue = webXmlParser.getVersionIdentifier();
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
            WebDirContext r = new WebDirContext();
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

            WebXmlParser webXmlParser = getWebXmlParser(context.getSource());
            configureLoaderAttributes(cloader, webXmlParser, base);
            configureLoaderProperties(cloader, webXmlParser, base);
            
            configureContextXmlAttribute(cloader, base, context);
            
            try {
                final DeploymentContext dc = context;
                final ClassLoader cl = cloader;
                
                AccessController.doPrivileged(
                        new PermsArchiveDelegate.SetPermissionsAction(
                                SMGlobalPolicyUtil.CommponentType.war, dc, cl));
            } catch (PrivilegedActionException e) {
                throw new SecurityException(e.getException());
            }

        } catch(XMLStreamException xse) {
            logger.log(Level.SEVERE, xse.getMessage());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, xse.getMessage(), xse);
            }
            xse.printStackTrace();
        } catch(IOException ioe) {
            logger.log(Level.SEVERE, ioe.getMessage());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, ioe.getMessage(), ioe);
            }
            ioe.printStackTrace();
        }

        cloader.start();

        return cloader;
    }

    protected WebXmlParser getWebXmlParser(ReadableArchive archive)
            throws XMLStreamException, IOException {

        WebXmlParser webXmlParser = null;
        boolean hasWSLDD = archive.exists(WEBLOGIC_XML);
        File runtimeAltDDFile = archive.getArchiveMetaData(
                    DeploymentProperties.RUNTIME_ALT_DD, File.class);
        if (runtimeAltDDFile != null &&
                "glassfish-web.xml".equals(runtimeAltDDFile.getPath()) &&
                runtimeAltDDFile.isFile()) {
            webXmlParser = new GlassFishWebXmlParser(archive);
        } else if (!gfDDOverWLSDD && !ignoreWLSDD && hasWSLDD) {
            webXmlParser = new WeblogicXmlParser(archive);
        } else if (archive.exists(GLASSFISH_WEB_XML)) {
            webXmlParser = new GlassFishWebXmlParser(archive);
        } else if (archive.exists(SUN_WEB_XML)) {
            webXmlParser = new SunWebXmlParser(archive);
        } else if (gfDDOverWLSDD && !ignoreWLSDD && hasWSLDD) {
            webXmlParser = new WeblogicXmlParser(archive);
        } else { // default
            if (gfDDOverWLSDD || ignoreWLSDD) {
                webXmlParser = new GlassFishWebXmlParser(archive);
            } else {
                webXmlParser = new WeblogicXmlParser(archive);
            }
        }
        return webXmlParser;
    }

    protected void configureLoaderAttributes(WebappClassLoader cloader,
            WebXmlParser webXmlParser, File base) {

        boolean delegate = webXmlParser.isDelegate();
        cloader.setDelegate(delegate);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("WebModule[" + base +
                        "]: Setting delegate to " + delegate);
        }

        String extraClassPath = webXmlParser.getExtraClassPath();
        if (extraClassPath != null) {
            // Parse the extra classpath into its ':' and ';' separated
            // components. Ignore ':' as a separator if it is preceded by
            // '\'
            String[] pathElements = extraClassPath.split(";|((?<!\\\\):)");
            for (String path : pathElements) {
                path = path.replace("\\:", ":");
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("WarHandler[" + base +
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
                        String msg = rb.getString(CLASSPATH_ERROR);
                        Object[] params = { path };
                        msg = MessageFormat.format(msg, params);
                        logger.log(Level.SEVERE, msg, mue2);
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

    protected void configureContextXmlAttribute(WebappClassLoader cloader,
            File base, DeploymentContext dc) throws XMLStreamException, IOException {

        boolean consistent = true;
        Boolean value = null;

        File warContextXml = new File(base.getAbsolutePath(), WAR_CONTEXT_XML);
        if (warContextXml.exists()) {
            ContextXmlParser parser = new ContextXmlParser(warContextXml);
            value = parser.getClearReferencesStatic();
        }

        if (value == null) {
            Boolean domainCRS = null;
            File defaultContextXml = new File(serverEnvironment.getInstanceRoot(), DEFAULT_CONTEXT_XML);
            if (defaultContextXml.exists()) {
                ContextXmlParser parser = new ContextXmlParser(defaultContextXml);
                domainCRS = parser.getClearReferencesStatic();
            }

            List<Boolean> csrs = new ArrayList<Boolean>();
            HttpService httpService = serverConfig.getHttpService();
            DeployCommandParameters params = dc.getCommandParameters(DeployCommandParameters.class);
            String vsIDs = params.virtualservers;
            List<String> vsList = StringUtils.parseStringList(vsIDs, " ,");
            if (httpService != null && vsList != null && !vsList.isEmpty()) {
                for (VirtualServer vsBean : httpService.getVirtualServer()) {
                    if (vsList.contains(vsBean.getId())) {
                        Boolean csr = null;
                        Property prop = vsBean.getProperty("contextXmlDefault");
                        if (prop != null) {
                            File contextXml = new File(serverEnvironment.getInstanceRoot(),
                                    prop.getValue());
                            if (contextXml.exists()) {  // vs context.xml
                                ContextXmlParser parser = new ContextXmlParser(contextXml);
                                csr = parser.getClearReferencesStatic();
                            }
                        }

                        if (csr == null) {
                            csr = domainCRS;
                        }
                        csrs.add(csr);
                    }
                }

                // check that it is consistent
                for (Boolean b : csrs) {
                    if (b != null) {
                        if (value != null && !b.equals(value)) {
                            consistent = false;
                            break;
                        }
                        value = b;
                    }
                }

            }
        }

        if (consistent) {
            if (value != null) {
                cloader.setClearReferencesStatic(value);
            }
        } else if (logger.isLoggable(Level.WARNING)) {
            logger.log(Level.WARNING, INCONSISTENT_CLEAR_REFERENCE_STATIC);
        }
    }
    
    // ---- inner class ----
    protected abstract class BaseXmlParser {
        protected XMLStreamReader parser = null;

        /**
         * This method will parse the input stream and set the XMLStreamReader
         * object for latter use.
         *
         * @param input InputStream
         * @exception XMLStreamException;
         */
        protected abstract void read(InputStream input) throws XMLStreamException;

        protected void init(InputStream input)     
                throws XMLStreamException {

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
            }
        }

        protected void skipRoot(String name) throws XMLStreamException {
            while (true) {
                int event = parser.next();
                if (event == START_ELEMENT) {
                    String localName = parser.getLocalName();
                    if (!name.equals(localName)) {
                        String msg = rb.getString(UNEXPECTED_XML_ELEMENT);
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
                    throw new XMLStreamException(rb.getString(UNEXPECTED_END_DOCUMENT));
                } else if (event == END_ELEMENT && name.equals(parser.getLocalName())) {
                    return;
                }
            }
        }

    }

    protected abstract class WebXmlParser extends BaseXmlParser {
        protected boolean delegate = true;

        protected boolean ignoreHiddenJarFiles = false;
        protected boolean useBundledJSF = false;
        protected String extraClassPath = null;

        protected String versionIdentifier = null;

        WebXmlParser(ReadableArchive archive) 
                throws XMLStreamException, IOException {

            if (archive.exists(getXmlFileName())) {
                try (InputStream is = archive.getEntry(getXmlFileName())) {
                    init(is);
                } catch (Throwable t) {
                    String msg = localStrings.getLocalString("web.deployment.exception_parsing_webxml", "Error in parsing {0} for archive [{1}]: {2}", getXmlFileName(), archive.getURI(), t.getMessage());
                    throw new RuntimeException(msg);
                }
            }
        }

        protected abstract String getXmlFileName();

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

        String getVersionIdentifier() {
            return versionIdentifier;
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

        SunWebXmlParser(ReadableArchive archive)
                throws XMLStreamException, IOException {

            super(archive);
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
                                        logger.log(Level.WARNING, DYNAMIC_RELOAD_INTERVAL);
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
                                rb.getString(NULL_WEB_PROPERTY));
                        }

                        if ("ignoreHiddenJarFiles".equals(propName)) {
                            ignoreHiddenJarFiles = Boolean.valueOf(value);
                        } else {
                            Object[] params = { propName, value };
                            if (logger.isLoggable(Level.WARNING)) {
                                logger.log(Level.WARNING, INVALID_PROPERTY,
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
                                rb.getString(NULL_WEB_PROPERTY));
                        }

                        if("useMyFaces".equalsIgnoreCase(propName)) {
                            useBundledJSF = Boolean.valueOf(value);
                        } else if("useBundledJsf".equalsIgnoreCase(propName)) {
                            useBundledJSF = Boolean.valueOf(value);
                        }
                    } else if ("version-identifier".equals(name)) {
                        versionIdentifier = parser.getElementText();
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

        GlassFishWebXmlParser(ReadableArchive archive)
                throws XMLStreamException, IOException {

            super(archive);
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
        WeblogicXmlParser(ReadableArchive archive)
                throws XMLStreamException, IOException {

            super(archive);
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

    protected class ContextXmlParser extends BaseXmlParser {
        protected Boolean clearReferencesStatic = null;

        ContextXmlParser(File contextXmlFile)
                throws XMLStreamException, IOException {

            if (contextXmlFile.exists()) {
                try (InputStream is = new FileInputStream(contextXmlFile)) {
                    init(is);
                }
            }
        }

        /**
         * This method will parse the input stream and set the XMLStreamReader
         * object for latter use.
         *
         * @param input InputStream
         * @exception XMLStreamException;
         */
        @Override
        protected void read(InputStream input) throws XMLStreamException {
            parser = getXMLInputFactory().createXMLStreamReader(input);

            int event = 0;
            while (parser.hasNext() && (event = parser.next()) != END_DOCUMENT) {
                if (event == START_ELEMENT) {
                    String name = parser.getLocalName();
                    if ("Context".equals(name)) {
                        String path = null;
                        Boolean crs = null;
                        int count = parser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            String attrName = parser.getAttributeName(i).getLocalPart();
                            if ("clearReferencesStatic".equals(attrName)) {
                                crs = Boolean.valueOf(parser.getAttributeValue(i));
                            } else if ("path".equals(attrName)) {
                                path = parser.getAttributeValue(i);
                            }
                        }
                        if (path == null) {  // make sure no path associated to it
                            clearReferencesStatic = crs;
                            break;
                        }
                    }  else {
                        skipSubTree(name);
                    }
                }
            }
        }

        Boolean getClearReferencesStatic() {
            return clearReferencesStatic;
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
