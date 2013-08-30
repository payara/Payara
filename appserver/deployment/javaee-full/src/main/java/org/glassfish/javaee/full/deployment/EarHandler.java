/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.javaee.full.deployment;

import com.sun.enterprise.connectors.connector.module.RarDetector;
import com.sun.enterprise.deployment.deploy.shared.InputJarArchive;
import java.net.URI;

import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.archive.*;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.appclient.server.connector.CarDetector;
import org.glassfish.deployment.common.*;
import org.glassfish.ejb.deployment.archive.EjbJarDetector;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.api.DelegatingClassLoader;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.web.sniffer.WarDetector;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PreDestroy;
import org.xml.sax.SAXParseException;
import com.sun.enterprise.deploy.shared.AbstractArchiveHandler;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.deploy.shared.Util;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deployment.archivist.ApplicationArchivist;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.deploy.shared.JarArchive;
import com.sun.enterprise.security.integration.DDPermissionsLoader;
import com.sun.enterprise.security.perms.EarEEPermissionsProcessor;
import com.sun.enterprise.security.perms.SMGlobalPolicyUtil;
import com.sun.enterprise.security.perms.PermsArchiveDelegate;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.io.*;
import java.util.logging.*;
import java.util.Map;
import java.net.URLClassLoader;
import java.net.URL;
import java.text.MessageFormat;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.Permission;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedAction;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.inject.Inject;
import javax.inject.Named;
import static javax.xml.stream.XMLStreamConstants.*;

@Service(name=EarDetector.ARCHIVE_TYPE)
public class EarHandler extends AbstractArchiveHandler implements CompositeHandler {

    @Inject
    Deployment deployment;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject
    ServerEnvironment env;

    @Inject
    DasConfig dasConfig;

    @Inject @Named(EarDetector.ARCHIVE_TYPE)
    ArchiveDetector detector;

    private static final String EAR_LIB = "ear_lib";
    private static final String EMBEDDED_RAR = "embedded_rar";

    private static LocalStringsImpl strings = new LocalStringsImpl(EarHandler.class);
    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(EarHandler.class);

    private static final Logger _logger = EarDeployer.deplLogger;

    //declaredPermission
    protected PermissionCollection earDeclaredPC;

    //ee permissions for all types
    private Map<SMGlobalPolicyUtil.CommponentType, PermissionCollection> eeGarntsMap;
    
    public String getArchiveType() {
        return EarDetector.ARCHIVE_TYPE;
    }

    public String getVersionIdentifier(ReadableArchive archive) {
        String versionIdentifier = null;
        try {
            GFApplicationXmlParser gfApplicationXMLParser = new GFApplicationXmlParser(archive);
            versionIdentifier = gfApplicationXMLParser.extractVersionIdentifierValue(archive);
        } catch (XMLStreamException e) {
            _logger.log(Level.SEVERE, e.getMessage());
        } catch (IOException e) {
            _logger.log(Level.SEVERE, e.getMessage());
        }
        return versionIdentifier;
    }

    public boolean handles(ReadableArchive archive) throws IOException {
        return detector.handles(archive);
    }

    @Override
    public void expand(ReadableArchive source, WritableArchive target, DeploymentContext context) throws IOException {
        // expand the top level first so we could read application.xml
        super.expand(source, target, context);

        ReadableArchive source2 = null;
        try {
            /*
             * We know that the expansion is into a directory, so we should know
             * that target is a FileArchive which is also readable as-is.
             */
            source2 = (FileArchive) target;

            ApplicationHolder holder =
                getApplicationHolder(source2, context, false);

            // now start to expand the sub modules 
            for (ModuleDescriptor md : holder.app.getModules()) {
                String moduleUri = md.getArchiveUri();
                ReadableArchive subArchive = null;
                WritableArchive subTarget = null;
                ReadableArchive subArchiveToExpand = null;
                try {
                    subArchive = source2.getSubArchive(moduleUri);
                    if (subArchive == null) {
                        _logger.log(Level.WARNING,
                            "Exception while locating sub archive: " +
                            moduleUri);
                        continue;
                    }
                    // optimize performance by retrieving the archive handler
                    // based on module type first
                    ArchiveHandler subHandler = getArchiveHandlerFromModuleType(md.getModuleType());
                    if (subHandler == null) {
                        subHandler = deployment.getArchiveHandler(subArchive);
                    }
                    context.getModuleArchiveHandlers().put(
                        moduleUri, subHandler);
                    if (subHandler!=null) {
                        subTarget = target.createSubArchive(
                            FileUtils.makeFriendlyFilenameExtension(moduleUri));
                        /*
                         * A subarchive might be packaged as a subdirectory
                         * (instead of a nested JAR) in an EAR.  If so and if it
                         * has the same name as the directory into which we'll
                         * expand the submodule, make sure it is also of the
                         * correct archive type (i.e., directory and not JAR)
                         * in which case we don't need to expand it because the developer
                         * already did so before packaging.
                         */
                        subArchiveToExpand = chooseSubArchiveToExpand(moduleUri, subTarget, subArchive, source2);
                        if (subArchiveToExpand != null) {
                            subHandler.expand(subArchiveToExpand, subTarget, context);
                        } else {
                            /*
                             * The target for expansion is the same URI as the
                             * subarchive.  Make sure they are the same type;
                             * if so, we just skip the expansion.  Otherwise,
                             * we would leave a JAR where the rest of
                             * deployment expects a subdirectory so throw an
                             * exception in that case.
                             */
                            if ( ! areSameStorageType(subTarget, subArchive)) {
                                final String msg = MessageFormat.format(
                                        _logger.getResourceBundle().getString("enterprise.deployment.backend.badSubModPackaging"),
                                        subArchive.getURI().toASCIIString(),
                                        subArchive.getClass().getName());
                                throw new RuntimeException(msg);
                            }
                        }
// Keep the original submodule file because the app client deployer needs it.
/*
                        // delete the original module file
                        File origSubArchiveFile = new File(
                            target.getURI().getSchemeSpecificPart(), moduleUri);
                        origSubArchiveFile.delete();
*/
                    }
                } catch(IOException ioe) {
                    _logger.log(Level.FINE, "Exception while processing " +
                        moduleUri, ioe);
                } finally {
                    try {
                        if (subArchive != null) {
                            subArchive.close();
                        }
                        if (subTarget != null) {
                            subTarget.close();
                        }
                        if (subArchiveToExpand != null) {
                            subArchiveToExpand.close();
                        }
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
        } finally {
            if (source2 != null) {
                source2.close();
            }
        }
    }

    private ReadableArchive chooseSubArchiveToExpand(
            final String moduleURI,
            final WritableArchive subTarget,
            final ReadableArchive subArchive,
            final ReadableArchive expandedOriginalArchive) throws IOException {
        /*
         * The subArchive will normally be xxx.jar (or .rar, etc.)
         * In this case, its URI differs from the URI of the target (which
         * will be xxx_jar) and we should expand subArchive into subTarget.
         * But the developer might have pre-expanded the archive in which case
         * subArchive and subTarget will both be xxx_jar.  In such a case
         * we do not want to expand the directory onto itself.
         *
         * Yet, on Windows, it is possible that the xxx_jar directory is left
         * over from a previous expansion from xxx.jar to xxx_jar, in which case
         * we DO want to expand xxx.jar into xxx_jar.  
         */
        if (! subTarget.getURI().equals(subArchive.getURI())) {
            /*
             * The URIs are not the same, so the subArchive is probably xxx.jar
             * and the target is probably xxx_jar.
             */
            return subArchive;
        }

        /*
         * Try to find the xxx.jar entry in the file archive that is the expanded
         * version of the original archive.  If that entry exists, then the
         * xxx_jar entry in the already-expanded directory is probably
         * a left-over from a previous deployment and we should expand the
         * original subarchive into it.  If, on the other hand, the xxx.jar
         * entry does not exist in the expansion, then the developer probably
         * packaged the EAR with a pre-expanded module directory instead of
         * the module JAR; in that case there is no need to expand the
         * pre-expanded directory into itself.
         */
        if (expandedOriginalArchive.exists(moduleURI)) {
            final URI unexpandedSubArchiveURI = expandedOriginalArchive.getURI().resolve(moduleURI);
            return archiveFactory.openArchive(unexpandedSubArchiveURI);
        }
        return null;
    }

    private static boolean areSameStorageType(final Archive arch1, final Archive arch2) {
        return (   (arch1 instanceof FileArchive && arch2 instanceof FileArchive)
                || (arch1 instanceof JarArchive && arch2 instanceof JarArchive));
    }

    public ClassLoader getClassLoader(final ClassLoader parent, DeploymentContext context) {
        final ReadableArchive archive  = context.getSource();
        
        ApplicationHolder holder =
            getApplicationHolder(archive, context, true);

        // the ear classloader hierachy will be 
        // ear lib classloader <- embedded rar classloader <- 
        // ear classloader <- various module classloaders
        final DelegatingClassLoader embeddedConnCl;
        final EarClassLoader cl;
        // add the libraries packaged in the application library directory
        try {
            String compatProp = context.getAppProps().getProperty(
                DeploymentProperties.COMPATIBILITY);
            // if user does not specify the compatibility property
            // let's see if it's defined in glassfish-application.xml
            if (compatProp == null) {
                GFApplicationXmlParser gfApplicationXmlParser =
                    new GFApplicationXmlParser(context.getSource());
                compatProp = gfApplicationXmlParser.getCompatibilityValue();
                if (compatProp != null) {
                    context.getAppProps().put(
                        DeploymentProperties.COMPATIBILITY, compatProp);
                }
            }
            // if user does not specify the compatibility property
            // let's see if it's defined in sun-application.xml
            if (compatProp == null) {
                SunApplicationXmlParser sunApplicationXmlParser =
                    new SunApplicationXmlParser(context.getSourceDir());
                compatProp = sunApplicationXmlParser.getCompatibilityValue();
                if (compatProp != null) {
                    context.getAppProps().put(
                        DeploymentProperties.COMPATIBILITY, compatProp);
                }
            }
            
            if (System.getSecurityManager() != null) {
                //procee declared permissions
                earDeclaredPC = 
                    PermsArchiveDelegate.getDeclaredPermissions(
                            SMGlobalPolicyUtil.CommponentType.ear, context);
                
                //process ee permissions
                processEEPermissions(context);
            }
            
            final URL[] earLibURLs = ASClassLoaderUtil.getAppLibDirLibraries(context.getSourceDir(), holder.app.getLibraryDirectory(), compatProp);
            final EarLibClassLoader earLibCl = AccessController.doPrivileged(new PrivilegedAction<EarLibClassLoader>() {
                @Override
                public EarLibClassLoader run() {
                    return new EarLibClassLoader(earLibURLs, parent);
                }
            });

            if (System.getSecurityManager() != null) {
                addEEOrDeclaredPermissions(earLibCl, earDeclaredPC, false);   
                if(_logger.isLoggable(Level.FINE))
                    _logger.fine("added declaredPermissions to earlib: " + earDeclaredPC );
                addEEOrDeclaredPermissions(earLibCl, 
                        eeGarntsMap.get(SMGlobalPolicyUtil.CommponentType.ear),
                                true);
                if(_logger.isLoggable(Level.FINE))
                    _logger.fine("added all ee permissions to earlib: " + 
                        eeGarntsMap.get(SMGlobalPolicyUtil.CommponentType.ear) );
            }

            embeddedConnCl =  AccessController.doPrivileged(new PrivilegedAction<DelegatingClassLoader>() {
                @Override
                public DelegatingClassLoader run() {
                    return new DelegatingClassLoader(earLibCl);
                }
            });

            cl =  AccessController.doPrivileged(new PrivilegedAction<EarClassLoader>() {
                @Override
                public EarClassLoader run() {
                    return new EarClassLoader(embeddedConnCl);
                }
            });

            // add ear lib to module classloader list so we can 
            // clean it up later
            cl.addModuleClassLoader(EAR_LIB, earLibCl);
            
            if(System.getSecurityManager() != null) {
                //push declared permissions to ear classloader
                addEEOrDeclaredPermissions(cl, earDeclaredPC, false);
                if(_logger.isLoggable(Level.FINE))
                    _logger.fine("declaredPermissions added: " + earDeclaredPC );
                //push ejb permissions to ear classloader
                addEEOrDeclaredPermissions(cl, 
                        eeGarntsMap.get(SMGlobalPolicyUtil.CommponentType.ejb),
                                true);
                if(_logger.isLoggable(Level.FINE))
                    _logger.fine("ee permissions added: " + eeGarntsMap.get(SMGlobalPolicyUtil.CommponentType.ejb) );
            }

        } catch (Exception e) {
            _logger.log(Level.SEVERE, strings.get("errAddLibs"), e);
            throw new RuntimeException(e);
        }



        for (ModuleDescriptor md : holder.app.getModules()) {
            ReadableArchive sub = null;
            String moduleUri = md.getArchiveUri();
            try {
                sub = archive.getSubArchive(moduleUri);
                if (sub instanceof InputJarArchive) {
                    throw new IllegalArgumentException(strings.get("wrongArchType", moduleUri));
                }
            } catch (IOException e) {
                _logger.log(Level.FINE, "Sub archive " + moduleUri + " seems unreadable" ,e);
            }
            if (sub!=null) {
                try {
                    ArchiveHandler handler =
                        context.getModuleArchiveHandlers().get(moduleUri);
                    if (handler == null) {
                        handler = getArchiveHandlerFromModuleType(md.getModuleType());
                        if (handler == null) {
                            handler = deployment.getArchiveHandler(sub);
                        }
                        context.getModuleArchiveHandlers().put(
                            moduleUri, handler);
                    }

                    if (handler!=null) {
                        ActionReport subReport =
                            context.getActionReport().addSubActionsReport();
                        // todo : this is a hack, once again, 
                        // the handler is assuming a file:// url
                        ExtendedDeploymentContext subContext =
                            new DeploymentContextImpl(subReport,
                            sub,
                            context.getCommandParameters(
                                DeployCommandParameters.class), env) {

                            @Override
                            public File getScratchDir(String subDirName) {
                                String modulePortion = Util.getURIName(
                                    getSource().getURI());
                                return (new File(super.getScratchDir(
                                    subDirName), modulePortion));
                            }
                        };

                        // sub context will store the root archive handler also 
                        // so we can figure out the enclosing archive type
                        subContext.setArchiveHandler
                            (context.getArchiveHandler());

                        subContext.setParentContext((ExtendedDeploymentContext)context);

                        sub.setParentArchive(context.getSource());

                        ClassLoader subCl = handler.getClassLoader(cl, subContext);

                        if ((System.getSecurityManager() != null) && (subCl instanceof DDPermissionsLoader)) {                           
                           addEEOrDeclaredPermissions(subCl, earDeclaredPC, false);
                           if(_logger.isLoggable(Level.FINE))
                               _logger.fine("added declared permissions to sub module of " + subCl );
                        }
                        
                        if (md.getModuleType().equals(DOLUtils.ejbType())) {
                            // for ejb module, we just add the ejb urls 
                            // to EarClassLoader and use that to load 
                            // ejb module
                            URL[] moduleURLs =
                                ((URLClassLoader)subCl).getURLs();
                            for (URL moduleURL : moduleURLs) {
                                cl.addURL(moduleURL);
                            }
                            cl.addModuleClassLoader(moduleUri, cl);
                            PreDestroy.class.cast(subCl).preDestroy();
                        } else if (md.getModuleType().equals(DOLUtils.rarType())) {
                            embeddedConnCl.addDelegate(
                                (DelegatingClassLoader.ClassFinder)subCl);
                            cl.addModuleClassLoader(moduleUri, subCl);
                        } else {
                            Boolean isTempClassLoader = context.getTransientAppMetaData(ExtendedDeploymentContext.IS_TEMP_CLASSLOADER, Boolean.class);
                            if (subCl instanceof URLClassLoader &&
                                (isTempClassLoader != null) &&
                                isTempClassLoader) {
                                // for temp classloader, we add all the module
                                // urls to the top level EarClassLoader
                                URL[] moduleURLs =
                                    ((URLClassLoader)subCl).getURLs();
                                for (URL moduleURL : moduleURLs) {
                                    cl.addURL(moduleURL);
                                }
                            }
                            cl.addModuleClassLoader(moduleUri, subCl);
                        }
                    }
                } catch (IOException e) {
                    _logger.log(Level.SEVERE, strings.get("noClassLoader", moduleUri), e);
                }
            }
        }
        return cl;
    }
    
    
    protected void processEEPermissions(DeploymentContext dc) {
        
        EarEEPermissionsProcessor eePp = 
            new EarEEPermissionsProcessor(dc);
        
        eeGarntsMap = eePp.getAllAdjustedEEPermission();
        
    }

    //set ee or declared permissions
    private void addEEOrDeclaredPermissions(ClassLoader cloader, final PermissionCollection pc, final boolean isEEPermission) {
        
        if(!(cloader instanceof DDPermissionsLoader))
            return;
        
        final DDPermissionsLoader ddpl = (DDPermissionsLoader)cloader;
        try {
            AccessController.doPrivileged(
                  new PrivilegedExceptionAction<Object>() {
                    public Object run() throws SecurityException {
                        if (isEEPermission)
                            ddpl.addEEPermissions(pc);
                        else
                            ddpl.addDeclaredPermissions(pc);
                        
                        return null;
                    }
                });
            } catch (PrivilegedActionException e) {
                throw new SecurityException(e.getException());
            }        
    }
    
    
    public boolean accept(ReadableArchive source, String entryName) {
        // I am hiding everything but the metadata.
        return entryName.startsWith("META-INF");

    }

    // do any necessary meta data initialization for composite handler
    public void initCompositeMetaData(DeploymentContext context) {
        // populate ear level metadata
        getApplicationHolder(context.getSource(), context, true);
    }

    private ApplicationHolder getApplicationHolder(ReadableArchive source,
        DeploymentContext context, boolean isDirectory) {
        ApplicationHolder holder = context.getModuleMetaData(ApplicationHolder.class);
        if (holder==null || holder.app==null) {
            try {
                DeployCommandParameters params = context.getCommandParameters(DeployCommandParameters.class); 
                if (params != null && params.altdd != null) {
                    source.addArchiveMetaData(DeploymentProperties.ALT_DD, 
                        params.altdd);
                }
                long start = System.currentTimeMillis();
                ApplicationArchivist archivist = habitat.getService(ApplicationArchivist.class);
                archivist.setAnnotationProcessingRequested(true);

                String xmlValidationLevel = dasConfig.getDeployXmlValidation();
                archivist.setXMLValidationLevel(xmlValidationLevel);
                if (xmlValidationLevel.equals("none")) {
                    archivist.setXMLValidation(false);
                }

                holder = new ApplicationHolder(archivist.createApplication(
                    source, isDirectory));
                _logger.fine("time to read application.xml " + (System.currentTimeMillis() - start));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (SAXParseException e) {
                throw new RuntimeException(e);
            }
            context.addModuleMetaData(holder);
        }

        if (holder.app==null) {
            throw new RuntimeException(strings.get("errReadMetadata"));
        }
        return holder;
    }

    // get archive handler from module type
    // performance optimization so we don't need to retrieve archive handler
    // the normal way which might involve annotation scanning
    private ArchiveHandler getArchiveHandlerFromModuleType(ArchiveType type) {
        if (type.equals(DOLUtils.warType())) {
            return habitat.getService(ArchiveHandler.class, WarDetector.ARCHIVE_TYPE);
        } else if (type.equals(DOLUtils.rarType())) {
            return habitat.getService(ArchiveHandler.class, RarDetector.ARCHIVE_TYPE);
        } else if (type.equals(DOLUtils.ejbType())) {
            return habitat.getService(ArchiveHandler.class, EjbJarDetector.ARCHIVE_TYPE);
        } else if (type.equals(DOLUtils.carType())) {
            return habitat.getService(ArchiveHandler.class, CarDetector.ARCHIVE_TYPE);
        } else {
            return null;
        }
    }

    private static class GFApplicationXmlParser {
        private XMLStreamReader parser = null;
        private String compatValue = null;

        GFApplicationXmlParser(ReadableArchive archive) throws XMLStreamException, FileNotFoundException, IOException {
            InputStream input = null;
            File runtimeAltDDFile = archive.getArchiveMetaData(
                DeploymentProperties.RUNTIME_ALT_DD, File.class);
            if (runtimeAltDDFile != null && runtimeAltDDFile.getPath().indexOf(DescriptorConstants.GF_PREFIX) != -1 && runtimeAltDDFile.exists() && runtimeAltDDFile.isFile()) {
                DOLUtils.validateRuntimeAltDDPath(runtimeAltDDFile.getPath());
                input = new FileInputStream(runtimeAltDDFile);
            } else {
                input = archive.getEntry("META-INF/glassfish-application.xml");
            }

            if (input != null) {
                try {
                    read(input);
                } catch (Throwable t) {
                    String msg = localStrings.getLocalString("exception_parsing_glassfishapplicationxml", "Error in parsing sun-application.xml for archive [{0}]: {1}", archive.getURI(), t.getMessage());
                    throw new RuntimeException(msg);
                } finally {
                    if (parser != null) {
                        try {
                            parser.close();
                        } catch(Exception ex) {
                            // ignore
                        }
                    }
                    try {
                        input.close();
                    } catch(Exception ex) {
                            // ignore
                    }
                }
            }
        }

        protected String extractVersionIdentifierValue(ReadableArchive archive) throws XMLStreamException, IOException{

            InputStream input = null;
            String versionIdentifierValue = null;

            try
            {
                File runtimeAltDDFile = archive.getArchiveMetaData(
                    DeploymentProperties.RUNTIME_ALT_DD, File.class);
                if (runtimeAltDDFile != null && runtimeAltDDFile.getPath().indexOf(DescriptorConstants.GF_PREFIX) != -1 && runtimeAltDDFile.exists() && runtimeAltDDFile.isFile()) {
                    DOLUtils.validateRuntimeAltDDPath(runtimeAltDDFile.getPath());
                    input = new FileInputStream(runtimeAltDDFile);
                } else {
                    input = archive.getEntry("META-INF/glassfish-application.xml");
                }

                if (input != null) {

                    // parse elements only from glassfish-web
                    parser = getXMLInputFactory().createXMLStreamReader(input);

                    int event = 0;
                    skipRoot("glassfish-application");

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

        private void read(InputStream input) throws XMLStreamException {
            parser = getXMLInputFactory().createXMLStreamReader(input);

            int event = 0;
            boolean done = false;
            skipRoot("glassfish-application");

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

    private static class SunApplicationXmlParser {
        private XMLStreamReader parser = null;
        private String compatValue = null;

        SunApplicationXmlParser(File baseDir) throws XMLStreamException, FileNotFoundException {
            InputStream input = null;
            File f = new File(baseDir, "META-INF/sun-application.xml");
            if (f.exists()) {
                input = new FileInputStream(f);
                try {
                    read(input);
                } catch (Throwable t) {
                    String msg = localStrings.getLocalString("exception_parsing_sunapplicationxml", "Error in parsing glassfish-application.xml for archive [{0}]: {1}", baseDir.getPath(), t.getMessage());
                    throw new RuntimeException(msg);
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

        private void read(InputStream input) throws XMLStreamException {
            parser = getXMLInputFactory().createXMLStreamReader(input);

            int event = 0;
            boolean done = false;
            skipRoot("sun-application");

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
