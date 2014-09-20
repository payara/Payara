/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.archivist;

import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EarType;
import com.sun.enterprise.deployment.annotation.introspection.EjbComponentAnnotationScanner;
import com.sun.enterprise.deployment.io.ApplicationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.runtime.ApplicationRuntimeDDFile;
import com.sun.enterprise.deployment.io.runtime.GFApplicationRuntimeDDFile;
import com.sun.enterprise.deployment.io.runtime.WLSApplicationRuntimeDDFile;
import com.sun.enterprise.deployment.util.AnnotationDetector;
import com.sun.enterprise.deployment.util.ApplicationValidator;
import com.sun.enterprise.deployment.util.ApplicationVisitor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.shared.ArchivistUtils;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.deployment.common.RootDeploymentDescriptor;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.xml.sax.SAXParseException;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 * This class is responsible for handling application archive files
 *
 * @author  Jerome Dochez
 */
@Service
@PerLookup
@ArchivistFor(EarType.ARCHIVE_TYPE)
public class ApplicationArchivist extends Archivist<Application> {
    @Inject
    Provider<ArchivistFactory> archivistFactory;

    /** resources... */
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(ApplicationArchivist.class);        
    
    /** Creates new ApplicationArchivist */
    public ApplicationArchivist() {
        handleRuntimeInfo = true;
    }
    
    /**
     * @return the  module type handled by this archivist
     * as defined in the application DTD
     *
     */
    @Override
    public ArchiveType getModuleType() {
        return DOLUtils.earType();
    }
    
            
    /**
     * writes the content of an archive to a JarFile
     *
     * @param in the descriptors to use for writing
     * @param out the output stream to write to
     */
    @Override
    protected void writeContents(ReadableArchive in, WritableArchive out) throws IOException {
        
        Vector filesToSkip = new Vector();
        
        if(DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {	
	    DOLUtils.getDefaultLogger().fine("Write " + out.getURI() + " with " + this);
	}
         
        // any files already written to the output should never be rewritten
        for (Enumeration alreadyWritten = out.entries(); alreadyWritten.hasMoreElements();) {
            String elementName = (String) alreadyWritten.nextElement();
            filesToSkip.add(elementName);
        }
                
        // write this application .ear file contents...
        for (ModuleDescriptor aModule : descriptor.getModules()) {
            Archivist subArchivist = archivistFactory.get().getArchivist(aModule.getModuleType());
            subArchivist.initializeContext(this);
            subArchivist.setModuleDescriptor(aModule);
            if(DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().info("Write " + aModule.getArchiveUri() + " with " + subArchivist);
            }
            
            // Create a new jar file inside the application .ear
            WritableArchive internalJar = out.createSubArchive(aModule.getArchiveUri());
              
            // we need to copy the old archive to a temp file so
            // the save method can copy its original contents from
            InputStream is = in.getEntry(aModule.getArchiveUri());
            File tmpFile=null;
            try {
                if (in instanceof WritableArchive) {
                    subArchivist.setArchiveUri(internalJar.getURI().getSchemeSpecificPart());
                } else {
                    tmpFile = getTempFile(path);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
                    ArchivistUtils.copy(is, bos);

                    // configure archivist
                    subArchivist.setArchiveUri(tmpFile.getAbsolutePath());
                }
                subArchivist.writeContents(internalJar);
                out.closeEntry(internalJar);
            } finally {
                if (tmpFile!=null) {
                    boolean ok = tmpFile.delete();
                    if (! ok) {
                      logger.log(Level.WARNING, localStrings.getLocalString("enterprise.deployment.cantDelete", "Error deleting file {0}", new Object[]{tmpFile.getAbsolutePath()}));
                    }
                }
            }
                
            // no need to copy the bundle from the original jar file
            filesToSkip.add(aModule.getArchiveUri());
        }
        
        // now write the old contents and new descriptors
        super.writeContents(in, out, filesToSkip);
    }
    
    /**

    /**
     * @return a default BundleDescriptor for this archivist
     */
    @Override
    public Application getDefaultBundleDescriptor() {
        return Application.createApplication();
    }
    
    /**
     * open a new application archive file, read all the deployment descriptors
     *
     * @param appArchive the file path for the J2EE Application archive
     */
    @Override
    public Application open(ReadableArchive appArchive)
        throws IOException, SAXParseException { 
        
        setManifest(appArchive.getManifest());
        
        // read the standard deployment descriptors
        Application appDesc = readStandardDeploymentDescriptor(appArchive);
        return openWith(appDesc, appArchive);
    }

    public Application openWith(Application application, ReadableArchive archive)
        throws IOException, SAXParseException {         
        setManifest(archive.getManifest());

        setDescriptor(application);

        Map<ExtensionsArchivist, RootDeploymentDescriptor> extensions = new HashMap<ExtensionsArchivist, RootDeploymentDescriptor>();

        if (extensionsArchivists!=null) {
            for (ExtensionsArchivist extension : extensionsArchivists) {
                if (extension.supportsModuleType(getModuleType())) {
                    Object o = extension.open(this, archive, descriptor);
                    if (o instanceof RootDeploymentDescriptor) {
                        if (o != descriptor) {
                            extension.addExtension(descriptor, (RootDeploymentDescriptor) o);
                        }
                        extensions.put(extension, (RootDeploymentDescriptor) o);
                    }
                }
            }
        }
 
        // save the handleRuntimeInfo value first
        boolean origHandleRuntimeInfo = handleRuntimeInfo;

        // read the modules standard deployment descriptors
        handleRuntimeInfo = false;
        if (!readModulesDescriptors(application, archive))
            return null;

        // now read the runtime deployment descriptors
        handleRuntimeInfo = origHandleRuntimeInfo;
        
        if (handleRuntimeInfo) {
            readRuntimeDeploymentDescriptor(archive, application);

            // read extensions runtime deployment descriptors if any
            for (Map.Entry<ExtensionsArchivist, RootDeploymentDescriptor> extension : extensions.entrySet()) {
                // after standard DD and annotations are processed, we should
                // an extension descriptor now
                if (extension.getValue() != null) {
                    extension.getKey().readRuntimeDeploymentDescriptor(this, archive, extension.getValue());
                }
            }
            // validate...
            if (classLoader!=null) {
                validate(null);
            }
        }
        return application;
    }

    /**
     * This method creates a top level Application object for an ear.
     * @param archive the archive for the application
     * @param directory whether the application is packaged as a directory
     */
    public Application createApplication(ReadableArchive archive,
        boolean directory) throws IOException, SAXParseException {
        if (hasStandardDeploymentDescriptor(archive) ) {
            return readStandardDeploymentDescriptor(archive);
        } else {
            return getApplicationFromIntrospection(archive, directory);
        }
    }

    /**
     * This method introspect an ear file and populate the Application object.
     * We follow the Java EE platform specification, Section EE.8.4.2
     * to determine the type of the modules included in this application.
     *
     * @param archive   the archive representing the application root
     * @param directory whether this is a directory deployment
     */
    private Application getApplicationFromIntrospection(
            ReadableArchive archive, boolean directory) {
        String appRoot = archive.getURI().getSchemeSpecificPart(); //archive is a directory
        if (appRoot.endsWith(File.separator)) {
            appRoot = appRoot.substring(0, appRoot.length() - 1);
        }

        Application app = Application.createApplication();
        app.setLoadedFromApplicationXml(false);
        app.setVirtual(false);

        //name of the file without its extension
        String appName = appRoot.substring(
                appRoot.lastIndexOf(File.separatorChar) + 1);
        app.setName(appName);

        List<ReadableArchive> unknowns = new ArrayList<ReadableArchive>();
        File[] files = getEligibleEntries(new File(appRoot), directory);
        for (File subModule : files) {
            ReadableArchive subArchive = null;
            try {
                try {
                    subArchive = archiveFactory.openArchive(subModule);
                } catch (IOException ex) {
                    logger.log(Level.WARNING, ex.getMessage());
                }

                //for archive deployment, we check the sub archives by its
                //file extension; for directory deployment, we check the sub
                //directories by its name. We are now supporting directory
                //names with both "_suffix" and ".suffix".

                //Section EE.8.4.2.1.a
                String name = subModule.getName();
                String uri = deriveArchiveUri(appRoot, subModule, directory);
                if ((!directory && name.endsWith(".war"))
                        || (directory &&
                        (name.endsWith("_war") ||
                                name.endsWith(".war")))) {
                    ModuleDescriptor<BundleDescriptor> md = new ModuleDescriptor<BundleDescriptor>();
                    md.setArchiveUri(uri);
                    md.setModuleType(DOLUtils.warType());
                    // the context root will be set later after 
                    // we process the sub modules
                    app.addModule(md);
                }
                //Section EE.8.4.2.1.b
                else if ((!directory && name.endsWith(".rar"))
                        || (directory &&
                        (name.endsWith("_rar") ||
                                name.endsWith(".rar")))) {
                    ModuleDescriptor<BundleDescriptor> md = new ModuleDescriptor<BundleDescriptor>();
                    md.setArchiveUri(uri);
                    md.setModuleType(DOLUtils.rarType());
                    app.addModule(md);
                } else if ((!directory && name.endsWith(".jar"))
                        || (directory &&
                        (name.endsWith("_jar") ||
                                name.endsWith(".jar")))) {
                    try {
                        //Section EE.8.4.2.1.d.i
                        AppClientArchivist acArchivist = new AppClientArchivist();
                        if (acArchivist.hasStandardDeploymentDescriptor(subArchive)
                                || acArchivist.hasRuntimeDeploymentDescriptor(subArchive)
                                || acArchivist.getMainClassName(subArchive.getManifest()) != null) {

                            ModuleDescriptor<BundleDescriptor> md = new ModuleDescriptor<BundleDescriptor>();
                            md.setArchiveUri(uri);
                            md.setModuleType(DOLUtils.carType());
                            md.setManifest(subArchive.getManifest());
                            app.addModule(md);
                            continue;
                        }

                        //Section EE.8.4.2.1.d.ii
                        Archivist ejbArchivist = archivistFactory.get().getArchivist(
                                DOLUtils.ejbType());
                        if (ejbArchivist.hasStandardDeploymentDescriptor(subArchive)
                                || ejbArchivist.hasRuntimeDeploymentDescriptor(subArchive)) {

                            ModuleDescriptor<BundleDescriptor> md = new ModuleDescriptor<BundleDescriptor>();
                            md.setArchiveUri(uri);
                            md.setModuleType(DOLUtils.ejbType());
                            app.addModule(md);
                            continue;
                        }
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, ex.getMessage());
                    }

                    //Still could not decide between an ejb and a library
                    unknowns.add(subArchive);
                    
                    // Prevent this unknown archive from being closed in the
                    // finally block, because the same object will be used in
                    // the block below where unknowns are checked one more time.
                    subArchive = null;
                } else {
                    //ignored
                }
            } finally {
                if (subArchive != null) {
                    try {
                        subArchive.close();
                    } catch (IOException ioe) {
                        logger.log(Level.WARNING, localStrings.getLocalString("enterprise.deployment.errorClosingSubArch", "Error closing subarchive {0}", new Object[]{subModule.getAbsolutePath()}), ioe);
                    }
                }
            }
        }

        if (unknowns.size() > 0) {
            AnnotationDetector detector =
                    new AnnotationDetector(new EjbComponentAnnotationScanner());
            for (int i = 0; i < unknowns.size(); i++) {
                File jarFile = new File(unknowns.get(i).getURI().getSchemeSpecificPart());
                try {
                    if (detector.hasAnnotationInArchive(unknowns.get(i))) {
                        String uri = deriveArchiveUri(appRoot, jarFile, directory);
                        //Section EE.8.4.2.1.d.ii, alas EJB
                        ModuleDescriptor<BundleDescriptor> md = new ModuleDescriptor<BundleDescriptor>();
                        md.setArchiveUri(uri);
                        md.setModuleType(DOLUtils.ejbType());
                        app.addModule(md);
                    }
                    /*
                     * The subarchive was opened by the anno detector.  Close it.
                     */
                    unknowns.get(i).close();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, ex.getMessage());
                }
            }
        }

        return app;
    }

    private static String deriveArchiveUri(
            String appRoot, File subModule, boolean deploydir) {

        //if deploydir, revert the name of the directory to
        //the format of foo/bar/voodoo.ext (where ext is war/rar/jar)
        if (deploydir) {
            return FileUtils.revertFriendlyFilename(subModule.getName());
        }

        // convert appRoot to canonical path so it would work on windows platform
        String aRoot = null;
        try {
            aRoot = (new File(appRoot)).getCanonicalPath();
        } catch (IOException ex) {
            aRoot = appRoot;
        } 

        //if archive deploy, need to make sure all of the directory
        //structure is correctly included
        String uri = null;
        try {
            uri = subModule.getCanonicalPath().substring(aRoot.length() + 1);
        } catch (IOException ex) {
            uri = subModule.getAbsolutePath().substring(aRoot.length() + 1);
        } 
        return uri.replace(File.separatorChar, '/');
    }

    private static File[] getEligibleEntries(File appRoot, boolean deploydir) {

        //For deploydir, all modules are exploded at the top of application root
        if (deploydir) {
            return appRoot.listFiles(new DirectoryIntrospectionFilter());
        }

        //For archive deploy, recursively search the entire package
        Vector<File> files = new Vector<File>();
        getListOfFiles(appRoot, files,
                new ArchiveIntrospectionFilter(appRoot.getAbsolutePath()));
        return files.toArray(new File[files.size()]);
    }

    private static void getListOfFiles(
            File directory, Vector<File> files, FilenameFilter filter) {

        File[] list = directory.listFiles(filter);
        for (int i = 0; i < list.length; i++) {
            if (!list[i].isDirectory()) {
                files.add(list[i]);
            } else {
                getListOfFiles(list[i], files, filter);
            }
        }
    }

    private static class ArchiveIntrospectionFilter implements FilenameFilter {
        private String libDir;
        private final FileArchive.StaleFileManager sfm;

        ArchiveIntrospectionFilter(String root) {
            try {
                sfm = FileArchive.StaleFileManager.Util.getInstance(
                    new File(root));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            libDir = root + File.separator + "lib" + File.separator;
        }

        public boolean accept(File dir, String name) {

            File currentFile = new File(dir, name);
            if (currentFile.isDirectory()) {
                return sfm.isEntryValid(currentFile, true);
            }

            //For ".war" and ".rar", check all files in the archive
            if (name.endsWith(".war") || name.endsWith(".rar")) {
                return sfm.isEntryValid(currentFile, true);
            }

            String path = currentFile.getAbsolutePath();
            if (!path.startsWith(libDir) && path.endsWith(".jar")) {
                return sfm.isEntryValid(currentFile, true);
            }

            return false;
        }
    }

    private static class DirectoryIntrospectionFilter implements FilenameFilter {

        DirectoryIntrospectionFilter() {
        }

        public boolean accept(File dir, String name) {

            File currentFile = new File(dir, name);
            if (!currentFile.isDirectory()) {
                return false;
            }

            // now we are supporting directory names with
            // ".suffix" and "_suffix"
            if (resemblesTopLevelSubmodule(name)) {
                return true;
            }

            return false;
        }
    }    
    
    
    /**
     * read the modules deployment descriptor from this application object using
     * the passed archive
     * @param app application containing the list of modules.
     * @param appArchive containing the sub modules files.
     * @return true if everything went fine
     */
    public boolean readModulesDescriptors(Application app, ReadableArchive appArchive)
        throws IOException, SAXParseException { 
        
        List<ModuleDescriptor> nonexistentModules = 
            new ArrayList<ModuleDescriptor>();
        
        List<ModuleDescriptor> sortedModules = sortModules(app);

        for (ModuleDescriptor aModule : sortedModules) {
            if (aModule.getArchiveUri().indexOf(" ") != -1) { 
                throw new IllegalArgumentException(localStrings.getLocalString("enterprise.deployment.unsupporturi", "Unsupported module URI {0}, it contains space(s)", new Object[]{aModule.getArchiveUri()}));
            }
            if(DOLUtils.getDefaultLogger().isLoggable(Level.FINE)) {
                DOLUtils.getDefaultLogger().fine("Opening sub-module " + aModule);
            }
            BundleDescriptor descriptor = null;
            Archivist newArchivist = archivistFactory.get().getArchivist(aModule.getModuleType());
            newArchivist.initializeContext(this);
            newArchivist.setRuntimeXMLValidation(this.getRuntimeXMLValidation());
            newArchivist.setRuntimeXMLValidationLevel(
                this.getRuntimeXMLValidationLevel());
            newArchivist.setAnnotationProcessingRequested(
                annotationProcessingRequested);

            ReadableArchive embeddedArchive = appArchive.getSubArchive(aModule.getArchiveUri());
            if (embeddedArchive == null) {
                throw new IllegalArgumentException(localStrings.getLocalString("enterprise.deployment.nosuchmodule", "Could not find sub module [{0}] as defined in application.xml", new Object[]{aModule.getArchiveUri()}));
            }
            embeddedArchive.setParentArchive(appArchive);
            DOLUtils.setExtensionArchivistForSubArchivist(habitat, embeddedArchive, aModule, app, newArchivist);

            if (aModule.getAlternateDescriptor()!=null) {
                // the module use alternate deployement descriptor, ignore the
                // DDs in the archive.
                InputStream is = appArchive.getEntry(aModule.getAlternateDescriptor());
                DeploymentDescriptorFile ddFile = newArchivist.getStandardDDFile();
                ddFile.setXMLValidation(newArchivist.getXMLValidation());
                ddFile.setXMLValidationLevel(newArchivist.getXMLValidationLevel());
                if (appArchive.getURI()!=null) {
                    ddFile.setErrorReportingString(appArchive.getURI().getSchemeSpecificPart());
                }

                descriptor = (BundleDescriptor) ddFile.read(is);
                descriptor.setApplication(app);
                is.close();

                // TODO : JD need to be revisited for EAR files with Alternative descriptors, what does
                // it mean for sub components.
                Map<ExtensionsArchivist, RootDeploymentDescriptor> extensions =
                    new HashMap<ExtensionsArchivist, RootDeploymentDescriptor>();
                List<ExtensionsArchivist> extensionsArchivists = newArchivist.getExtensionArchivists();
                if (extensionsArchivists != null) {
                    for (ExtensionsArchivist extension : extensionsArchivists) {
                            Object rdd = extension.open(newArchivist, embeddedArchive, descriptor);
                            if (rdd instanceof RootDeploymentDescriptor) {
                                extensions.put(extension, (RootDeploymentDescriptor) rdd);
                            }
                    }
                }
                newArchivist.postStandardDDsRead(descriptor, embeddedArchive, extensions);
                newArchivist.readAnnotations(embeddedArchive, descriptor, extensions);
                newArchivist.postAnnotationProcess(descriptor, embeddedArchive);
                newArchivist.postOpen(descriptor, embeddedArchive);
                // now reads the runtime deployment descriptor...
                if (isHandlingRuntimeInfo()) {
                    DOLUtils.readAlternativeRuntimeDescriptor(appArchive, embeddedArchive, newArchivist, descriptor, aModule.getAlternateDescriptor());
                    // read extensions runtime deployment descriptors if any
                    for (Map.Entry<ExtensionsArchivist, RootDeploymentDescriptor> extension : extensions.entrySet()) {
                        // after standard DD and annotations are processed
                        // we should have an extension descriptor now
                        if (extension.getValue() != null) {
                            extension.getKey().readRuntimeDeploymentDescriptor(newArchivist, embeddedArchive, extension.getValue());
                        }
                    }
                }
            } else {
                // open the subarchive to get the deployment descriptor...
                descriptor = newArchivist.open(embeddedArchive, app);
            }
            embeddedArchive.close();
            if (descriptor != null) {
                descriptor.getModuleDescriptor().setArchiveUri(
                    aModule.getArchiveUri());
                aModule.setModuleName(
                    descriptor.getModuleDescriptor().getModuleName());
                aModule.setDescriptor(descriptor);
                descriptor.setApplication(app);
                aModule.setManifest(newArchivist.getManifest());
                // for optional application.xml case, set the 
                // context root as module name for web modules
                if (!appArchive.exists("META-INF/application.xml")) {
                    if (aModule.getModuleType().equals(DOLUtils.warType())) {
                        aModule.setContextRoot(aModule.getModuleName());
                    }
                }
            } else {
                // display a message only if we had a handle on the sub archive
                return false;
            }
        }        
        // now remove all the non-existent modules from app so these modules
        // don't get processed further
        for (ModuleDescriptor nonexistentModule : nonexistentModules) {
            app.removeModule(nonexistentModule);
        }
        return true;
    }

    private List<ModuleDescriptor> sortModules(Application app) {
        List<ModuleDescriptor> sortedModules = 
            new ArrayList<ModuleDescriptor>();
        sortedModules.addAll(app.getModuleDescriptorsByType(DOLUtils.rarType()));
        sortedModules.addAll(app.getModuleDescriptorsByType(DOLUtils.ejbType()));
        sortedModules.addAll(app.getModuleDescriptorsByType(DOLUtils.warType()));
        sortedModules.addAll(app.getModuleDescriptorsByType(DOLUtils.carType()));
        return sortedModules;
    }
  
    
    /**
     * Read the runtime deployment descriptors (can contained in one or 
     * many file) set the corresponding information in the passed descriptor.
     * By default, the runtime deployment descriptors are all contained in 
     * the xml file characterized with the path returned by 
     *
     * @param archive the input archive
     * @param descriptor the initialized deployment descriptor
     */
    @Override
    public void readRuntimeDeploymentDescriptor(ReadableArchive archive, Application descriptor)
        throws IOException, SAXParseException {    
        
        if (descriptor != null) {

            // each modules first...
            for (ModuleDescriptor md : descriptor.getModules()) { 
                Archivist archivist = archivistFactory.get().getArchivist(md.getModuleType());
                archivist.initializeContext(this);
                archivist.setRuntimeXMLValidation(
                    this.getRuntimeXMLValidation());
                archivist.setRuntimeXMLValidationLevel(
                    this.getRuntimeXMLValidationLevel());
                ReadableArchive subArchive = archive.getSubArchive(md.getArchiveUri());
                if (md.getAlternateDescriptor()!=null) {
                    DOLUtils.readAlternativeRuntimeDescriptor(archive, subArchive, archivist, (BundleDescriptor)md.getDescriptor(), md.getAlternateDescriptor());
                } else {
                    archivist.readRuntimeDeploymentDescriptor(subArchive, (BundleDescriptor)md.getDescriptor());
                }
            }
        }
        // for the application
        super.readRuntimeDeploymentDescriptor(archive,  descriptor);
    }

    /**
     * validates the DOL Objects associated with this archivist, usually
     * it requires that a class loader being set on this archivist or passed
     * as a parameter
     */
    @Override
    public void validate(ClassLoader aClassLoader) {
        ClassLoader cl = aClassLoader;
        if (cl==null) {
            cl = classLoader;
        }
        if (cl==null) {
            return;
        }
	    descriptor.setClassLoader(cl);
        descriptor.visit((ApplicationVisitor) new ApplicationValidator());
        
    }    
    
    /**
     * @return the DeploymentDescriptorFile responsible for handling
     * standard deployment descriptor
     */
    @Override
    public DeploymentDescriptorFile getStandardDDFile() {
        if (standardDD == null) {
            standardDD = new ApplicationDeploymentDescriptorFile();   
        }
        return standardDD;
    }   
    

    /**
     * @return the list of the DeploymentDescriptorFile responsible for
     *         handling the configuration deployment descriptors
     */
    public List<ConfigurationDeploymentDescriptorFile> getConfigurationDDFiles() {
        if (confDDFiles == null) {
            confDDFiles = DOLUtils.getConfigurationDeploymentDescriptorFiles(habitat, EarType.ARCHIVE_TYPE);
        }
        return confDDFiles;
    }

    /**
     * Perform Optional packages dependencies checking on an archive 
     */
    @Override
    public boolean performOptionalPkgDependenciesCheck(ReadableArchive archive) throws IOException {
        
        if (!super.performOptionalPkgDependenciesCheck(archive))
            return false;
        
        // now check sub modules
        if (descriptor==null) {
            throw new IOException("Application object not set on archivist");
        }

        boolean returnValue = true;
        for (ModuleDescriptor md : descriptor.getModules()) {
            ReadableArchive sub = archive.getSubArchive(md.getArchiveUri());
            if (sub!=null) {
                Archivist subArchivist = archivistFactory.get().getArchivist(md.getModuleType());
                if (!subArchivist.performOptionalPkgDependenciesCheck(sub))
                    returnValue = false;
            }
        }
        return returnValue;
    }
        
    /**
     * Copy this archivist to a new abstract archive
     * @param source the archive to copy from
     * @param target the new archive to use to copy our contents into
     */
    public void copyInto(ReadableArchive source, WritableArchive target) throws IOException {
        try {
            Application a = readStandardDeploymentDescriptor(source);
            copyInto(a, source, target);
        } catch(SAXParseException spe) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.fileCopyFailure", spe);
        }
    }
    
    /**
     * Copy this archivist to a new abstract archive
     * @param a the deployment descriptor for an application
     * @param source the source archive
     * @param target the target archive
     */
    public void copyInto(Application a, ReadableArchive source, WritableArchive target) throws IOException {
        copyInto(a, source, target, true);
    }
    
    /**
     * Copy this archivist to a new abstract archive
     * @param a the deployment descriptor for an application
     * @param source the source archive
     * @param target the target archive
     * @param overwriteManifest if true, the manifest in source archive overwrites the one in target
     */
    public void copyInto(Application a, ReadableArchive source,
                         WritableArchive target, boolean overwriteManifest)
        throws IOException {
        Vector entriesAdded = new Vector();
        for (ModuleDescriptor aModule : a.getModules()) {
            entriesAdded.add(aModule.getArchiveUri());
            ReadableArchive subSource = source.getSubArchive(aModule.getArchiveUri());
            WritableArchive subTarget = target.createSubArchive(aModule.getArchiveUri());
            Archivist newArchivist = archivistFactory.get().getArchivist(aModule.getModuleType());
            ReadableArchive subArchive = archiveFactory.openArchive(subTarget.getURI());
            subSource.setParentArchive(subArchive);
            newArchivist.copyInto(subSource, subTarget, overwriteManifest);
            target.closeEntry(subTarget);
            String subModulePath = subSource.getURI().getSchemeSpecificPart();
            String parentPath = source.getURI().getSchemeSpecificPart();
            if (subModulePath.startsWith(parentPath)) {
                subModulePath = subModulePath.substring(parentPath.length()+File.separator.length());
                for (Enumeration subEntries = subSource.entries();subEntries.hasMoreElements();) {
                    String anEntry = (String) subEntries.nextElement();
                    entriesAdded.add(subModulePath + "/" + anEntry);
                }
            }
            subSource.close();
            subArchive.close();
        }
        super.copyInto(source, target, entriesAdded, overwriteManifest);
    }
    

    /**
     * This method will be invoked if and only if the following is true:
     * 1. directory deployment with neither standard nor runtime DD
     * 2. JSR88 DeploymentManager.distribute using InputStream with neither
     *    standard nor runtime DD
     * <p>
     * Note that we will only venture a guess for case 1.  JSR88 deployment
     * of an application (ear) using InputStream without any deployment 
     * descriptor will NOT be supported at this time.
     */
    @Override
    protected boolean postHandles(ReadableArchive abstractArchive)
            throws IOException {
        // if we come here and archive is not a directory, it could not be ear
        if (!(abstractArchive instanceof FileArchive)) {
            return false;
        }

        // Only try to make a guess if the archive is a directory

        // We will try to conclude if a directory represents an application
        // by looking at if it contains any Java EE modules.
        // We are supporting directory names with both "_suffix" and ".suffix".
        File file = new File(abstractArchive.getURI());
        if (file.isDirectory()) {
            for (String dirName : abstractArchive.getDirectories()) {
                if (resemblesTopLevelSubmodule(dirName)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected String getArchiveExtension() {
        return APPLICATION_EXTENSION;
    }

    /**
     * Returns whether the entry name appears to be that of a submodule at
     * the top level of an enclosing application.
     * <p>
     * Judge an entry to be a top-level submodule if it ends with _war, _jar,
     * _rar, or .war, .jar, or .rar (MyEclipse uses latter pattern.)
     *
     * @param entryName entryName
     * @return true | false
     */
    private static boolean resemblesTopLevelSubmodule(final String entryName) {
        return (entryName.endsWith("_war")
                || entryName.endsWith("_jar")
                || entryName.endsWith("_rar")
                || entryName.endsWith(".war")
                || entryName.endsWith(".jar")
                || entryName.endsWith(".rar"));
    }
}
