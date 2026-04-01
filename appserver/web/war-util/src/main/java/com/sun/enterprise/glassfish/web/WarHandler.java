/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.glassfish.web;

import static java.security.AccessController.doPrivileged;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.naming.resources.WebDirContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.web.loader.LogFacade;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.web.sniffer.WarDetector;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import com.sun.enterprise.security.permissionsxml.CommponentType;
import com.sun.enterprise.security.permissionsxml.SetPermissionsAction;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.StringUtils;

/**
 * Implementation of the ArchiveHandler for war files.
 *
 * @author Jerome Dochez, Sanjeeb Sahoo, Shing Wai Chan
 */
@Service(name= WarDetector.ARCHIVE_TYPE)
public class WarHandler extends AbstractArchiveHandler {

    private static final String GLASSFISH_WEB_XML = "WEB-INF/glassfish-web.xml";
    private static final String PAYARA_WEB_XML = "WEB-INF/payara-web.xml";
    @Deprecated
    private static final String SUN_WEB_XML = "WEB-INF/sun-web.xml";
    private static final String WAR_CONTEXT_XML = "META-INF/context.xml";
    private static final String DEFAULT_CONTEXT_XML = "config/context.xml";
    private static final Logger logger = LogFacade.getLogger();
    private static final ResourceBundle rb = logger.getResourceBundle();
    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(WarHandler.class);


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
            WebXmlParser webXmlParser = getWebXmlParser(archive, Application.createApplication());
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
    public ClassLoader getClassLoader(final ClassLoader parent, final DeploymentContext context) {
        Application applicationTemp = context.getModuleMetaData(Application.class);
        boolean hotDeploy = context.getCommandParameters(DeployCommandParameters.class).hotDeploy;
        final Application application = applicationTemp == null? Application.createApplication() : applicationTemp;
        WebappClassLoader cloader = AccessController.doPrivileged(new PrivilegedAction<WebappClassLoader>() {
            @Override
            public WebappClassLoader run() {
                return new WebappClassLoader(parent, application, hotDeploy);
            }
        });
        try {
            WebDirContext r = new WebDirContext();
            File base = new File(context.getSource().getURI());
            r.setDocBase(base.getAbsolutePath());

            cloader.setResources(r);
            File classesPath = new File(base, "WEB-INF/classes/");
            if (!classesPath.exists()) {
                // make sure the WEB-INF/classes exists, it is searched by class loader
                classesPath.mkdirs();
            }
            cloader.addRepository("WEB-INF/classes/", classesPath);
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

            WebXmlParser webXmlParser = getWebXmlParser(context.getSource(), application);
            configureLoaderAttributes(cloader, webXmlParser, base);
            configureLoaderProperties(cloader, webXmlParser, base);
            
            configureContextXmlAttribute(cloader, base, context);
            
            try {
                doPrivileged(
                        new SetPermissionsAction(
                                CommponentType.war, context, cloader));
            } catch (PrivilegedActionException e) {
                throw new SecurityException(e.getException());
            }

        } catch(XMLStreamException xse) {
            logger.log(SEVERE, xse.getMessage());
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, xse.getMessage(), xse);
            }
            xse.printStackTrace();
        } catch(IOException ioe) {
            logger.log(SEVERE, ioe.getMessage());
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, ioe.getMessage(), ioe);
            }
            ioe.printStackTrace();
        }

        cloader.start();

        return cloader;
    }

    protected WebXmlParser getWebXmlParser(ReadableArchive archive, Application application)
            throws XMLStreamException, IOException {

        WebXmlParser webXmlParser = null;
        File runtimeAltDDFile = archive.getArchiveMetaData(DeploymentProperties.RUNTIME_ALT_DD, File.class);
        if (runtimeAltDDFile != null && "glassfish-web.xml".equals(runtimeAltDDFile.getPath()) && runtimeAltDDFile.isFile()) {
            webXmlParser = new GlassFishWebXmlParser(archive, application);
            logger.warning("The glassfish-web.xml file is deprecated and support will be removed in a future release."
                + " It is recommended to use payara-web.xml instead.");
        } else if (archive.exists(PAYARA_WEB_XML)){
            webXmlParser = new PayaraWebXmlParser(archive, application);
        } else if (archive.exists(GLASSFISH_WEB_XML)) {
            webXmlParser = new GlassFishWebXmlParser(archive, application);
            logger.warning("The glassfish-web.xml file is deprecated and support will be removed in a future release."
                + " It is recommended to use payara-web.xml instead.");
        } else if (archive.exists(SUN_WEB_XML)) {
            webXmlParser = new SunWebXmlParser(archive, application);
            logger.warning("The sun-web.xml file is deprecated and support will be removed in a future release."
                + " It is recommended to use payara-web.xml instead.");
        } else { // default
            webXmlParser = new GlassFishWebXmlParser(archive, application);
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
                        String msg = rb.getString(LogFacade.CLASSPATH_ERROR);
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

        cloader.setCookieSameSiteValue(webXmlParser.getCookieSameSiteValue());
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
                        cloader.closeJARs(true);
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

        String app = dc.getAppProps().getProperty("context-root");
        if (cloader.getCookieSameSiteValue() != null && app != null && !"".equals(cloader.getCookieSameSiteValue())) {
            app = app.substring(1).toLowerCase();
            System.setProperty(app + ".sameSite", cloader.getCookieSameSiteValue());
        }

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

            List<Boolean> csrs = new ArrayList<>();
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
            logger.log(Level.WARNING, LogFacade.INCONSISTENT_CLEAR_REFERENCE_STATIC);
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
                        String msg = rb.getString(LogFacade.UNEXPECTED_XML_ELEMENT);
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
                    throw new XMLStreamException(rb.getString(LogFacade.UNEXPECTED_END_DOCUMENT));
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
        protected final Application application;

        WebXmlParser(ReadableArchive archive, Application application)
                throws XMLStreamException, IOException {

            this.application = application;
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

        public abstract String getCookieSameSiteValue();
    }

    @Deprecated
    protected class SunWebXmlParser extends WebXmlParser {
        //XXX need to compute the default delegate depending on the version of dtd
        /*
         * The DOL will *always* return a value: If 'delegate' has not been
         * configured in sun-web.xml, its default value will be returned,
         * which is FALSE in the case of sun-web-app_2_2-0.dtd and
         * sun-web-app_2_3-0.dtd, and TRUE in the case of
         * sun-web-app_2_4-0.dtd.
         */
        SunWebXmlParser(ReadableArchive archive, Application application)
                throws XMLStreamException, IOException {

            super(archive, application);
        }

        @Override
        protected String getXmlFileName() {
            return SUN_WEB_XML;
        }

        @Override
        public String getCookieSameSiteValue() {
            return "";
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
                                        logger.log(Level.WARNING, LogFacade.DYNAMIC_RELOAD_INTERVAL);
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
                                rb.getString(LogFacade.NULL_WEB_PROPERTY));
                        }

                        if ("ignoreHiddenJarFiles".equals(propName)) {
                            ignoreHiddenJarFiles = Boolean.parseBoolean(value);
                        } else {
                            Object[] params = { propName, value };
                            if (logger.isLoggable(Level.WARNING)) {
                                logger.log(Level.WARNING, LogFacade.INVALID_PROPERTY,
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
                                rb.getString(LogFacade.NULL_WEB_PROPERTY));
                        }

                        if("useMyFaces".equalsIgnoreCase(propName)) {
                            useBundledJSF = Boolean.parseBoolean(value);
                        } else if("useBundledJsf".equalsIgnoreCase(propName)) {
                            useBundledJSF = Boolean.parseBoolean(value);
                        }
                    } else if ("version-identifier".equals(name)) {
                        versionIdentifier = parser.getElementText();
                    } else if (RuntimeTagNames.PAYARA_WHITELIST_PACKAGE.equals(name)) {
                        application.addWhitelistPackage(parser.getElementText());
                    } else if (RuntimeTagNames.SESSION_CONFIG.equals(name)) {
                        readCookieConfig();
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

        protected void readCookieConfig() throws XMLStreamException {}
    }

    @Deprecated
    protected class GlassFishWebXmlParser extends SunWebXmlParser {
        GlassFishWebXmlParser(ReadableArchive archive, Application application)
                throws XMLStreamException, IOException {

            super(archive, application);
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
    
    protected class PayaraWebXmlParser extends GlassFishWebXmlParser {
        private String cookieSameSiteValue;

        PayaraWebXmlParser(ReadableArchive archive, Application application)
                throws XMLStreamException, IOException {

            super(archive, application);
        }
        
        @Override
        protected String getXmlFileName() {
            return PAYARA_WEB_XML;
        }

        @Override
        protected String getRootElementName() {
            return "payara-web-app";
        }

        @Override
        protected void readCookieConfig() throws XMLStreamException {
            while (parser.hasNext()) {
                int eventType = parser.next();
                if (eventType == XMLStreamReader.END_ELEMENT && parser.getLocalName().equals("cookie-properties")) {
                    break;
                }
                if (eventType == XMLStreamReader.START_ELEMENT && parser.getLocalName().equals("property")) {
                    String name = parser.getAttributeValue(null, "name");
                    String value = parser.getAttributeValue(null, "value");
                    if (name != null && name.equals("cookieSameSite")) {
                        this.cookieSameSiteValue = value;
                    }
                }
            }
        }

        @Override
        public String getCookieSameSiteValue() {
            return cookieSameSiteValue;
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
