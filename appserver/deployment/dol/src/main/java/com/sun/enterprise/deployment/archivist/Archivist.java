/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.annotation.factory.AnnotatedElementHandlerFactory;
import com.sun.enterprise.deployment.annotation.factory.SJSASFactory;
import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import static com.sun.enterprise.deployment.io.DescriptorConstants.PERSISTENCE_DD_ENTRY;
import static com.sun.enterprise.deployment.io.DescriptorConstants.WEB_WEBSERVICES_JAR_ENTRY;
import static com.sun.enterprise.deployment.io.DescriptorConstants.EJB_WEBSERVICES_JAR_ENTRY;
import com.sun.enterprise.deployment.util.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.shared.ArchivistUtils;
import org.glassfish.apf.*;
import org.glassfish.apf.Scanner;
import org.glassfish.apf.impl.DefaultErrorHandler;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.deployment.common.*;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.classmodel.reflect.*;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Optional;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This abstract class contains all common behaviour for Achivisits. Archivists
 * classes are responsible for reading and writing correct J2EE Archives
 *
 * @author Jerome Dochez
 */
@Contract
public abstract class Archivist<T extends BundleDescriptor> {

    protected static final Logger logger =
            DOLUtils.getDefaultLogger();

    public static final String MANIFEST_VERSION_VALUE = "1.0";

    // the path for the underlying archive file
    protected String path;

    // should we read or save the runtime info.
    protected boolean handleRuntimeInfo = true;

    // default should be false in production
    protected boolean annotationProcessingRequested = false;

    // attributes of this archive
    protected Manifest manifest;
    
    // standard DD file associated with this archivist
    protected DeploymentDescriptorFile<T> standardDD;

    // configuration DD files associated with this archivist
    protected List<ConfigurationDeploymentDescriptorFile> confDDFiles;

    // the sorted configuration DD files with precedence from 
    // high to low
    private List<ConfigurationDeploymentDescriptorFile> sortedConfDDFiles;

    // configuration DD file that will be used
    private ConfigurationDeploymentDescriptorFile confDD;

    // resources...
    private static final LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(Archivist.class);    
    
    // class loader to use when validating the DOL
    protected ClassLoader classLoader = null;

    // boolean for XML validation
    private boolean isValidatingXML = true;

    // boolean for runtime XML validation
    private boolean isValidatingRuntimeXML = true;

    // xml validation error level reporting/recovering
    private String validationLevel = "parsing";

    // runtime xml validation error level reporting/recovering
    private String runtimeValidationLevel = "parsing";

    // error handler for annotation processing
    private ErrorHandler annotationErrorHandler = null;

    private static final String WSDL = ".wsdl";
    private static final String XML = ".xml";
    private static final String XSD = ".xsd";


    protected static final String APPLICATION_EXTENSION = ".ear";
    protected static final String APPCLIENT_EXTENSION = ".jar";
    protected static final String WEB_EXTENSION = ".war";
    protected static final String WEB_FRAGMENT_EXTENSION = ".jar";
    protected static final String EJB_EXTENSION = ".jar";
    protected static final String CONNECTOR_EXTENSION = ".rar";
    //Used to detect the uploaded files which always end in ".tmp"
    protected static final String UPLOAD_EXTENSION = ".tmp";

    private static final String PROCESS_ANNOTATION_FOR_OLD_DD =
            "process.annotation.for.old.dd";

    private static final boolean processAnnotationForOldDD =
            Boolean.getBoolean(PROCESS_ANNOTATION_FOR_OLD_DD); 
    
    protected T descriptor;

    @Inject
    protected ServiceLocator habitat;

    @Inject
    protected ServiceLocator locator;

    @Inject
    SJSASFactory annotationFactory;

    @Inject
    ArchiveFactory archiveFactory;

    protected List<ExtensionsArchivist> extensionsArchivists;

    /**
     * Creates new Archivist
     */
    public Archivist() {
        annotationErrorHandler = new DefaultErrorHandler();
    }

    /**
     * initializes this instance from another archivist, this is used
     * to transfer contextual information between archivists, for
     * example whether we should handle runtime information and such
     */
    protected void initializeContext(Archivist other) {
        handleRuntimeInfo = other.isHandlingRuntimeInfo();
        annotationProcessingRequested = other.isAnnotationProcessingRequested();
        isValidatingXML = other.isValidatingXML;
        validationLevel = other.validationLevel;
        classLoader = other.classLoader;
        annotationErrorHandler = other.annotationErrorHandler;
    }

    
    /**
     * Set the applicable extension archivists for this archivist
     *
     * @param descriptor for this archivist instnace
     */
    public void setExtensionArchivists(List<ExtensionsArchivist> archivists) {
        extensionsArchivists = archivists;
    }

    public List<ExtensionsArchivist> getExtensionArchivists() {
        return extensionsArchivists;
    }

    /**
     * Archivist read XML deployment descriptors and keep the
     * parsed result in the DOL descriptor instances. Sets the descriptor
     * for a particular Archivist type
     *
     * @param descriptor for this archivist instnace
     */
    public void setDescriptor(T descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * @return the Descriptor for this archvist
     */
    public T getDescriptor() {
        return descriptor;
    }

    /**
     * Open a new archive file, read the deployment descriptors and annotations      *  and set the constructed DOL descriptor instance
     *
     * @param archive the archive file path
     * @return the deployment descriptor for this archive
     */
    public T open(ReadableArchive archive)
            throws IOException, SAXParseException {
        return open(archive, (Application) null);
    }

    public T open(final ReadableArchive descriptorArchive,
            final ReadableArchive contentArchive) throws IOException, SAXParseException {
        return open(descriptorArchive, contentArchive, null);
    }
    /**
     * Creates the DOL object graph for an app for which the descriptor(s)
     * reside in one archive and the content resides in another.
     * <p>
     * This allows the app client container to use both the generated JAR
     * which contains the descriptors that are filled in during deployment and
     * also the developer's original JAR which contains the classes that
     * might be subject to annotation processing.
     *
     * @param descriptorArchive archive containing the descriptor(s)
     * @param contentArchive archive containing the classes, etc.
     * @param app owning DOL application (if any)
     * @return DOL object graph for the application
     * 
     * @throws IOException
     * @throws SAXParseException
     */
    public T open(final ReadableArchive descriptorArchive,
            final ReadableArchive contentArchive,
            final Application app)
            throws IOException, SAXParseException {
        setManifest(contentArchive.getManifest());

        T descriptor = readDeploymentDescriptors(descriptorArchive, contentArchive, app);
        if (descriptor != null) {
            postOpen(descriptor, contentArchive);
        }
        return descriptor;
    }

    public T open(ReadableArchive archive,
            Application app) throws IOException, SAXParseException {
        return open(archive, archive, app);
    }

    // fill in the rest of the application with an application object
    // populated from previus reading of the standard deployment descriptor
    public Application openWith(Application app, ReadableArchive archive)
            throws IOException, SAXParseException {

        setManifest(archive.getManifest());

        // application archivist will override this method
        if (app.isVirtual()) {
            T descriptor = readRestDeploymentDescriptors((T)app.getStandaloneBundleDescriptor(), archive, archive, app);
            if (descriptor != null) {
                postOpen(descriptor, archive);
                descriptor.setApplication(app);
            }
        }
        return app;
    }


    /**
     * Open a new archive file, read the XML descriptor and set the  constructed
     * DOL descriptor instance
     *
     * @param path the archive file path
     * @return the deployment descriptor for this archive
     */
    public T open(String path)
            throws IOException, SAXParseException {

        this.path = path;
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
        return open(file);
    }

    /**
     * open a new archive file using a file descriptor
     *
     * @param file the archive to open
     */
    public T open(File file) throws IOException, SAXParseException {

        path = file.getAbsolutePath();
        ReadableArchive archive = archiveFactory.openArchive(file);
        T descriptor = open(archive);

        archive.close();

        // attempt validation
        validate(null);

        return descriptor;    
    }

    /**
     * perform any action after all standard DDs is read
     *
     * @param descriptor the deployment descriptor for the module
     * @param archive the module archive
     * @param extensions map of extension archivists
     */
    protected void postStandardDDsRead(T descriptor, ReadableArchive archive,
                Map<ExtensionsArchivist, RootDeploymentDescriptor> extensions)
                throws IOException {
    }

    /**
     * perform any action after annotation processed
     *
     * @param descriptor the deployment descriptor for the module
     * @param archive the module archive
     */
    protected void postAnnotationProcess(T descriptor,
                                         ReadableArchive archive) throws IOException {
    }

    /**
     * perform any action after all runtime DDs read
     *
     * @param descriptor the deployment descriptor for the module
     * @param archive the module archive
     */
    public void postRuntimeDDsRead(T descriptor,
                                      ReadableArchive archive) throws IOException {
    }

    /**
     * perform any post deployment descriptor reading action
     *
     * @param descriptor the deployment descriptor for the module
     * @param archive the module archive
     */
    protected void postOpen(T descriptor, ReadableArchive archive)
            throws IOException {
    }

    /**
     * Read all Java EE deployment descriptors and annotations
     *
     * @return the initialized descriptor
     */
    private T readDeploymentDescriptors(ReadableArchive descriptorArchive,
            ReadableArchive contentArchive,
            Application app) throws IOException, SAXParseException {

        // read the standard deployment descriptors
        T descriptor = readStandardDeploymentDescriptor(descriptorArchive);
        descriptor.setApplication(app);

        ModuleDescriptor newModule = createModuleDescriptor(descriptor);
        newModule.setArchiveUri(contentArchive.getURI().getSchemeSpecificPart());
        return readRestDeploymentDescriptors(descriptor, descriptorArchive, contentArchive, app);
    }

    private T readRestDeploymentDescriptors (T descriptor, 
        ReadableArchive descriptorArchive, ReadableArchive contentArchive, 
        Application app) throws IOException, SAXParseException {
        Map<ExtensionsArchivist, RootDeploymentDescriptor> extensions = new HashMap<ExtensionsArchivist, RootDeploymentDescriptor>();
        if (extensionsArchivists!=null) {
            for (ExtensionsArchivist extension : extensionsArchivists) {
                    Object o = extension.open(this, descriptorArchive, descriptor);
                    if (o instanceof RootDeploymentDescriptor) {
                        if (o != descriptor) {
                            extension.addExtension(descriptor, (RootDeploymentDescriptor) o);
                        }
                        extensions.put(extension, (RootDeploymentDescriptor) o);
                    } else {
                        extensions.put(extension, null); // maybe annotation processing will yield results
                    }
            }
        }

        postStandardDDsRead(descriptor, contentArchive, extensions);

        readAnnotations(contentArchive, descriptor, extensions);
        postStandardDDsRead(descriptor, contentArchive, extensions);
        postAnnotationProcess(descriptor, contentArchive);

        // now read the runtime deployment descriptors
        readRuntimeDeploymentDescriptor(descriptorArchive, descriptor);

        // read extensions runtime deployment descriptors if any
        for (Map.Entry<ExtensionsArchivist, RootDeploymentDescriptor> extension : extensions.entrySet()) {
            // after standard DD and annotations are processed, we should
            // an extension descriptor now
            if (extension.getValue() != null) {
                extension.getKey().readRuntimeDeploymentDescriptor(this, descriptorArchive, extension.getValue());
            }
        }

        postRuntimeDDsRead(descriptor, contentArchive);

        return descriptor;
    }

    /**
     * Read all Java EE annotations
     */
    protected void readAnnotations(ReadableArchive archive, T descriptor,
                                 Map<ExtensionsArchivist, RootDeploymentDescriptor> extensions)
            throws IOException {

        readAnnotations(archive, descriptor, extensions, null);
    }

    protected void readAnnotations(ReadableArchive archive, T descriptor,
                                 Map<ExtensionsArchivist, RootDeploymentDescriptor> extensions,
                                 ModuleScanner scanner)
            throws IOException {
        
        try {
            boolean processAnnotationForMainDescriptor = isProcessAnnotation(descriptor);
            ProcessingResult result = null; 

            if (processAnnotationForMainDescriptor) {
                if (scanner == null) {
                    scanner = getScanner();
                }
                result = processAnnotations(descriptor, scanner, archive);
            }

            // process extensions annotations if any
            for (Map.Entry<ExtensionsArchivist, RootDeploymentDescriptor> extension : extensions.entrySet()) {
                try {
                    if (extension.getValue() == null) {
                        // extension descriptor is not present
                        // use main descriptor information to decide
                        // whether to process annotations
                        if (processAnnotationForMainDescriptor) {
                            RootDeploymentDescriptor o = extension.getKey().getDefaultDescriptor();
                            if( o != null ) {
                                o.setModuleDescriptor(descriptor.getModuleDescriptor());
                            }
                            processAnnotations(o, extension.getKey().getScanner(), archive);
                            if (o!=null && !o.isEmpty()) {
                                extension.getKey().addExtension(descriptor, o);
                                extensions.put(extension.getKey(), (RootDeploymentDescriptor) o);
                            }
                        }
                    } else{
                        // extension deployment descriptor is present
                        // use the extension descriptor information to decide
                        // whether to process annotations
                        BundleDescriptor extBundle;
                        if (extension.getValue() instanceof BundleDescriptor) {
                            extBundle = (BundleDescriptor)extension.getValue();
                            if (isProcessAnnotation(extBundle)) {
                                processAnnotations(extBundle, extension.getKey().getScanner(), archive);
                            }
                        }
                    }
                 } catch (AnnotationProcessorException ex) {
                    DOLUtils.getDefaultLogger().severe(ex.getMessage());
                    DOLUtils.getDefaultLogger().log(Level.FINE, ex.getMessage(), ex);
                    throw new IllegalStateException(ex);
                }
            }

            if (result != null &&
                    ResultType.FAILED.equals(result.getOverallResult())) {
                DOLUtils.getDefaultLogger().severe(localStrings.getLocalString(
                        "enterprise.deployment.archivist.annotationprocessingfailed",
                        "Annotations processing failed for {0}",
                        new Object[]{archive.getURI()}));
            }
            //XXX for backward compatible in case of having cci impl in EJB
        } catch (NoClassDefFoundError err) {
            if (DOLUtils.getDefaultLogger().isLoggable(Level.WARNING)) {
                DOLUtils.getDefaultLogger().warning(
                        "Error in annotation processing: " + err);
            }
        } catch (AnnotationProcessorException ex) {
            DOLUtils.getDefaultLogger().severe(ex.getMessage());
            DOLUtils.getDefaultLogger().log(Level.FINE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns the scanner for this archivist, usually it is the scanner regitered
     * with the same module type as this archivist, but subclasses can return a
     * different version
     *
     */
    public ModuleScanner getScanner() {
        
        Scanner scanner = null;
        try {
            scanner = habitat.getService(Scanner.class, getModuleType().toString());
            if (scanner==null || !(scanner instanceof ModuleScanner)) {
                logger.log(Level.SEVERE, "Cannot find module scanner for " + this.getManifest());
            }
        } catch (MultiException e) {
            // XXX To do
            logger.log(Level.SEVERE, "Cannot find scanner for " + this.getModuleType(), e);
        }
        return (ModuleScanner)scanner;
    }

    /**
     * Process annotations in a bundle descriptor, the annoation processing
     * is dependent on the type of descriptor being passed.
     */
    public ProcessingResult processAnnotations(T bundleDesc,
                                               ReadableArchive archive)
            throws AnnotationProcessorException, IOException {
        
        return processAnnotations(bundleDesc, getScanner(), archive);

    }
    
    /**
     * Process annotations in a bundle descriptor, the annoation processing
     * is dependent on the type of descriptor being passed.
     */
    protected ProcessingResult processAnnotations(RootDeploymentDescriptor bundleDesc,
                                               ModuleScanner scanner,
                                               ReadableArchive archive)
            throws AnnotationProcessorException, IOException {

        if (scanner == null) {
            return null;
        }

        AnnotatedElementHandler aeHandler =
                AnnotatedElementHandlerFactory.createAnnotatedElementHandler(
                        bundleDesc);

        if (aeHandler == null) {
            return null;
        }

        Parser parser = null;
        if (archive.getParentArchive() != null) {
            parser = archive.getParentArchive().getExtraData(Parser.class);
        } else {
            parser = archive.getExtraData(Parser.class);
        }

        scanner.process(archive, bundleDesc, classLoader, parser);

        if (!scanner.getElements().isEmpty()) {
            if (((BundleDescriptor)bundleDesc).isDDWithNoAnnotationAllowed()) {
                // if we come into this block, it means an old version
                // of deployment descriptor has annotation which is not correct
                // throw exception in this case
                String ddName =
                        getStandardDDFile().getDeploymentDescriptorPath();
                String explodedArchiveName =
                        new File(archive.getURI()).getName();
                String archiveName = FileUtils.revertFriendlyFilenameExtension(
                        explodedArchiveName);
                throw new AnnotationProcessorException(
                        localStrings.getLocalString(
                                "enterprise.deployment.oldDDwithAnnotation",
                                "{0} in archive {1} is of version {2}, which cannot support annotations in an application.  Please upgrade the deployment descriptor to be a version supported by Java EE 5.0 (or later).",
                                new Object[]{ddName, archiveName, bundleDesc.getSpecVersion()}));
            }
            AnnotationProcessor ap = annotationFactory.getAnnotationProcessor();
            ProcessingContext ctx = ap.createContext();
            ctx.setArchive(archive);
            if (annotationErrorHandler != null) {
                ctx.setErrorHandler(annotationErrorHandler);
            }
            ctx.setProcessingInput(scanner);
            ctx.pushHandler(aeHandler);

            // Make sure there is a classloader available on the descriptor
            // during annotation processing.
            ClassLoader originalBundleClassLoader = null;
            try {
                originalBundleClassLoader = bundleDesc.getClassLoader();
            } catch (Exception e) {
                // getClassLoader can throw exception if not available
            }

            // Only set classloader if it's not already set.
            if (originalBundleClassLoader == null) {
                bundleDesc.setClassLoader(classLoader);
            }

            try {
                return ap.process(ctx);
            } finally {
                if (originalBundleClassLoader == null) {
                    bundleDesc.setClassLoader(null);
                }
            }
        }
        return null;
    }

    /**
     * Read the standard deployment descriptors (can contained in one or
     * many file) and return the corresponding initialized descriptor instance.
     * By default, the standard deployment descriptors are all contained in
     * the xml file characterized with the path returned by
     *
     * @return the initialized descriptor
     * @link getDeploymentDescriptorPath
     */
    public T readStandardDeploymentDescriptor(ReadableArchive archive)
            throws IOException, SAXParseException {

        InputStream is = null;

        try {
            getStandardDDFile().setArchiveType(getModuleType());
            File altDDFile = archive.getArchiveMetaData(
                DeploymentProperties.ALT_DD, File.class);
            if (altDDFile != null && altDDFile.exists() && altDDFile.isFile()) {
                is = new FileInputStream(altDDFile); 
            } else {
                is = archive.getEntry(standardDD.getDeploymentDescriptorPath());
            }
            if (is != null) {
                standardDD.setXMLValidation(getXMLValidation());
                standardDD.setXMLValidationLevel(validationLevel);
                if (archive.getURI() != null) {
                    standardDD.setErrorReportingString(archive.getURI().getSchemeSpecificPart());
                }
                T result = standardDD.read(is);
                ((RootDeploymentDescriptor)result).setClassLoader(classLoader);
                return result;
            } else {
                /*
                 *Always return at least the default, because the info is needed 
                 *when an app is loaded during a server restart and there might not
                 *be a physical descriptor file.
                 */
                return getDefaultBundleDescriptor();
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Read the runtime deployment descriptors (can contained in one or
     * many file) set the corresponding information in the passed descriptor.
     * By default, the runtime deployment descriptors are all contained in
     * the xml file characterized with the path returned by
     *
     * @param archive the archive
     * @param descriptor the initialized deployment descriptor
     * @link getRuntimeDeploymentDescriptorPath
     */
    public void readRuntimeDeploymentDescriptor(ReadableArchive archive, T descriptor)
            throws IOException, SAXParseException {
        readRuntimeDeploymentDescriptor(archive, descriptor, true);
    }

    /**
     * Read the runtime deployment descriptors (can contained in one or
     * many file) set the corresponding information in the passed descriptor.
     * By default, the runtime deployment descriptors are all contained in
     * the xml file characterized with the path returned by
     *
     * @param archive the archive
     * @param descriptor the initialized deployment descriptor
     * @param warnIfMultipleDDs whether to log warnings if both the GlassFish and the legacy Sun descriptors are present
     * @link getRuntimeDeploymentDescriptorPath
     */
    public void readRuntimeDeploymentDescriptor(ReadableArchive archive, T descriptor,
            final boolean warnIfMultipleDDs)
            throws IOException, SAXParseException {

        String ddFileEntryName = getRuntimeDeploymentDescriptorPath(archive);
        // if we are not supposed to handle runtime info, just pass
        if (!isHandlingRuntimeInfo() || ddFileEntryName == null) {
            return;
        }

        DOLUtils.readRuntimeDeploymentDescriptor(getSortedConfigurationDDFiles(archive), archive, descriptor, this, warnIfMultipleDDs);
    }

    /*
     * write the J2EE module represented by this instance to a new 
     * J2EE archive file
     * 
     */
    public void write() throws IOException {
        write(path);
    }

    /**
     * saves the archive
     *
     * @param outPath the file to use
     */
    public void write(String outPath) throws IOException {
        ReadableArchive in = archiveFactory.openArchive(new File(path));
        write(in, outPath);
        in.close();
    }

    /**
     * save the archive
     *
     * @param in archive to copy old elements from
     * @param outPath the file to use
     */
    public void write(ReadableArchive in, String outPath) throws IOException {

        ReadableArchive oldArchive = null;
        try {
            oldArchive = archiveFactory.openArchive(new File(outPath));
        } catch (IOException ioe) {
            // there could be many reasons why we cannot open this archive, 
            // we should continue
        }
        WritableArchive out = null;
        BufferedOutputStream bos = null;
        try {
            File outputFile=null;
            if (oldArchive != null && oldArchive.exists() &&
                    !(oldArchive instanceof WritableArchive)) {
                // this is a rewrite, get a temp file name...
                // I am creating a tmp file just to get a name
                outputFile = getTempFile(outPath);
                outputFile.delete();
                out = archiveFactory.createArchive(outputFile);
                oldArchive.close();
            } else {
                out = archiveFactory.createArchive(new File(outPath));
            }

            // write archivist content
            writeContents(in, out);
            out.close();
            in.close();

            // if we were using a temp file, time to rewrite the original
            if (outputFile != null) {
                ReadableArchive finalArchive = archiveFactory.openArchive(new File(outPath));
                finalArchive.delete();
                ReadableArchive tmpArchive = archiveFactory.openArchive(outputFile);
                tmpArchive.renameTo(outPath);
            }
        } catch (IOException ioe) {
            // cleanup
            if (out != null) {
                try {
                    out.close();
                    //out.delete(); <-- OutputJarArchive.delete isn't supported.
                } catch (IOException outIoe) {
                    // ignore exceptions here, otherwise this will end up masking the real
                    // IOException in 'ioe'.
                }
            }
            // propagate exception
            throw ioe;
        }
    }

    public void write(ReadableArchive in, WritableArchive out) throws IOException {
        writeContents(in, out);
    }

    /**
     * writes the content of an archive to a JarFile
     *
     * @param out jar output stream to write to
     */
    protected void writeContents(WritableArchive out) throws IOException {
        ReadableArchive in = archiveFactory.openArchive(new File(path));
        writeContents(in, out);
        in.close();
    }


    /**
     * writes the content of an archive to another archive
     *
     * @param in input archive
     * @param out output archive
     */
    protected void writeContents(ReadableArchive in, WritableArchive out) throws IOException {

        writeContents(in, out, null);
    }


    /**
     * writes the content of an archive to a JarFile
     *
     * @param in input  archive
     * @param out archive output stream to write to
     * @param entriesToSkip files to not write from the original archive
     */
    protected void writeContents(ReadableArchive in, WritableArchive out, Vector entriesToSkip)
            throws IOException {

        // Copy original jarFile elements
        if (in != null && in.exists()) {
            if (entriesToSkip == null) {
                entriesToSkip = getListOfFilesToSkip(in);
            } else {
                entriesToSkip.addAll(getListOfFilesToSkip(in));
            }
            copyJarElements(in, out, entriesToSkip);
        }

        // now the deployment descriptors
        writeDeploymentDescriptors(in, out);

        // manifest file
        if (manifest != null) {
            OutputStream os = out.putNextEntry(JarFile.MANIFEST_NAME);
            manifest.write(new DataOutputStream(os));
            out.closeEntry();
        }
    }

    /**
     * writes the deployment descriptors (standard and runtime)
     * to a JarFile using the right deployment descriptor path
     *
     * @param in the input archive
     * @param out the abstract archive file to write to
     */
    public void writeDeploymentDescriptors(ReadableArchive in, WritableArchive out) throws IOException {

        // Standard DDs
        writeStandardDeploymentDescriptors(out);

        // Runtime DDs
        writeRuntimeDeploymentDescriptors(in, out);

        // Extension DDs
        writeExtensionDeploymentDescriptors(in, out);
    }

    /**
     * writes the standard deployment descriptors to an abstract archive
     *
     * @param out archive to write to
     */
    public void writeStandardDeploymentDescriptors(WritableArchive out) throws IOException {

        getStandardDDFile().setArchiveType(getModuleType());
        OutputStream os = out.putNextEntry(getDeploymentDescriptorPath());
        standardDD.write(getDescriptor(), os);
        out.closeEntry();
    }

    /**
     * writes the runtime deployment descriptors to an abstract archive
     *
     * @param in the input archive
     * @param out output archive
     */
    public void writeRuntimeDeploymentDescriptors(ReadableArchive in, WritableArchive out) throws IOException {

        T desc = getDescriptor();

        // when source archive contains runtime deployment descriptor 
        // files, write those out
        // otherwise write all possible runtime deployment descriptor 
        // files out (revisit this to see what is the desired behavior 
        // here, write out all, or write out the highest precedence one, 
        // or not write out)
        List<ConfigurationDeploymentDescriptorFile> confDDFilesToWrite = getSortedConfigurationDDFiles(in); 
        if (confDDFilesToWrite.isEmpty()) {
            confDDFilesToWrite = getConfigurationDDFiles();
        }
        for (ConfigurationDeploymentDescriptorFile ddFile : confDDFilesToWrite) {
            ddFile.setArchiveType(getModuleType());
            OutputStream os = out.putNextEntry(
                ddFile.getDeploymentDescriptorPath());
            ddFile.write(desc, os);
            out.closeEntry();
        }
    }

    /**
     * Write extension descriptors
     * @param in the input archive
     * @param out the output archive
     */
    public void writeExtensionDeploymentDescriptors(ReadableArchive in, WritableArchive out) throws IOException {
        // we need to re-initialize extension archivists, but we don't have 
        // applicable sniffers information here, so we will get all extension 
        // archivists with matched type. This is ok as it's just for writing
        // out deployment descriptors which will not be invoked in normal 
        // code path
        Collection<ExtensionsArchivist> extArchivists = habitat.getAllServices(ExtensionsArchivist.class);

        for (ExtensionsArchivist extension : extArchivists) {
            if (extension.supportsModuleType(getModuleType())) {
                extension.writeDeploymentDescriptors(this, getDescriptor(), in, out);
            }
        }
    }

    private List<ConfigurationDeploymentDescriptorFile> getSortedConfigurationDDFiles(ReadableArchive archive) throws IOException {
        if (sortedConfDDFiles == null) {
            sortedConfDDFiles = DOLUtils.processConfigurationDDFiles(getConfigurationDDFiles(), archive, getModuleType());
        }
        return sortedConfDDFiles;
    }

    /**
     * @return the location of the DeploymentDescriptor file for a
     *         particular type of J2EE Archive
     */
    public String getDeploymentDescriptorPath() {
        return getStandardDDFile().getDeploymentDescriptorPath();
    }

    /**
     * @return the location of the runtime deployment descriptor file
     *         for a particular type of J2EE Archive
     */
    public String getRuntimeDeploymentDescriptorPath(ReadableArchive archive) throws IOException {
        DeploymentDescriptorFile ddFile = getConfigurationDDFile(archive);
        if (ddFile != null) {
            return ddFile.getDeploymentDescriptorPath();
        } else {
            return null;
        }
    }

    /**
     * Archivists can be associated with a module descriptor once the
     * XML deployment descriptors have been read and the DOL tree
     * is initialized.
     */
    public void setModuleDescriptor(ModuleDescriptor<T> module) {
        setDescriptor(module.getDescriptor());
        setManifest(module.getManifest());
    }

    /**
     * Perform Optional packages dependencies checking on an archive
     */
    public boolean performOptionalPkgDependenciesCheck(ReadableArchive archive) throws IOException {

        boolean dependenciesSatisfied = true;
        Manifest m = archive.getManifest();
        if (m != null) {
            dependenciesSatisfied = InstalledLibrariesResolver.resolveDependencies(m, archive.getURI().getSchemeSpecificPart());
        }
        // now check my libraries.
        Vector<String> libs = getLibraries(archive);
        if (libs != null) {
            for (String libUri : libs) {
                JarInputStream jis = null;
                try {
                    jis = new JarInputStream(archive.getEntry(libUri));
                    m = jis.getManifest();
                    if (m != null) {
                        if (!InstalledLibrariesResolver.resolveDependencies(m, libUri)) {
                            dependenciesSatisfied = false;
                        }
                    }
                } finally {
                    if (jis != null)
                        jis.close();
                }
            }
        }
        return dependenciesSatisfied;
    }

    /**
     * @return the  module type handled by this archivist
     *         as defined in the application DTD/Schema
     */              
    public abstract ArchiveType getModuleType();

    /**
     * @return the DeploymentDescriptorFile responsible for handling
     *         standard deployment descriptor
     */
    public abstract DeploymentDescriptorFile<T> getStandardDDFile();

    /**
     * @return the list of the DeploymentDescriptorFile responsible for
     *         handling the configuration deployment descriptors
     */
    public abstract List<ConfigurationDeploymentDescriptorFile> getConfigurationDDFiles();

    /**
     * @return if exists the DeploymentDescriptorFile responsible for
     *         handling the configuration deployment descriptors
     */
    private ConfigurationDeploymentDescriptorFile getConfigurationDDFile(ReadableArchive archive) throws IOException {
        if (confDD == null) {
            getSortedConfigurationDDFiles(archive);
            if (sortedConfDDFiles != null && !sortedConfDDFiles.isEmpty()) {
               confDD = sortedConfDDFiles.get(0);
            }
        }
        return confDD;
    }

    /**
     * @return a default BundleDescriptor for this archivist
     */
    public abstract T getDefaultBundleDescriptor();

    /**
     * @return The archive extension handled by a specific archivist
     */
    protected abstract String getArchiveExtension();

    /**
     * Returns true if this archivist is capable of handling the archive type
     *  Here we check for the existence of the deployment descriptors
     * @return true if the archivist is handling the provided archive
     */
    protected abstract boolean postHandles(ReadableArchive archive) throws IOException;

    public boolean hasStandardDeploymentDescriptor(ReadableArchive archive)
            throws IOException {
        InputStream stIs = archive.getEntry(getDeploymentDescriptorPath());
        if (stIs != null) {
            stIs.close();
            return true;
        }
        return false;
    }

    public boolean hasRuntimeDeploymentDescriptor(ReadableArchive archive)
            throws IOException {

        //check null: since .par archive does not have runtime dds
        getConfigurationDDFile(archive); 

        if (confDD != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this archivist is capable of handling the archive type
     * Here we check for the existence of the deployment descriptors
     *
     * @param archive the input archive
     * @return true if this archivist can handle this archive
     */
    public boolean handles(ReadableArchive archive) {
        try {
            //first, check the existence of any deployment descriptors
            if (hasStandardDeploymentDescriptor(archive) ||
                    hasRuntimeDeploymentDescriptor(archive)) {
                return true;
            }

            // Java EE 5 Specification: Section EE.8.4.2.1

            //second, check file extension if any, excluding .jar as it needs
            //additional processing
            String uri = archive.getURI().toString();
            File file = new File(archive.getURI());
            if (!file.isDirectory() && !uri.endsWith(Archivist.EJB_EXTENSION)) {
                if (uri.endsWith(getArchiveExtension())) {
                    return true;
                }
            }

            //finally, still not returned here, call for additional processing
            if (postHandles(archive)) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /**
     * creates a new module descriptor for this archivist
     *
     * @return the new module descriptor
     */
    public ModuleDescriptor createModuleDescriptor(T descriptor) {
        ModuleDescriptor newModule = descriptor.getModuleDescriptor();
        setDescriptor(descriptor);
        return newModule;
    }


    /**
     * print the current descriptor associated with this archivist
     */
    public void printDescriptor() {
        DescriptorVisitor tracerVisitor = getDescriptor().getTracerVisitor();
        if (tracerVisitor == null) {
            tracerVisitor = new TracerVisitor();
        }
        getDescriptor().visit(tracerVisitor);
    }

    /**
     * sets if this archivist saves the runtime info
     *
     * @param handleRuntimeInfo to true to save the runtime info
     */
    public void setHandleRuntimeInfo(boolean handleRuntimeInfo) {
        this.handleRuntimeInfo = handleRuntimeInfo;
    }

    /**
     * @return true if this archivist will save the runtime info
     */
    public boolean isHandlingRuntimeInfo() {
        return handleRuntimeInfo;
    }

    /**
     * sets if this archivist process annotation
     *
     * @param annotationProcessingRequested to true to process annotation
     */
    public void setAnnotationProcessingRequested(
            boolean annotationProcessingRequested) {
        this.annotationProcessingRequested = annotationProcessingRequested;
    }

    /**
     * @return true if this archivist will process annotation
     */
    public boolean isAnnotationProcessingRequested() {
        return annotationProcessingRequested;
    }

    /**
     * sets annotation ErrorHandler for this archivist
     *
     * @param annotationErrorHandler
     */
    public void setAnnotationErrorHandler(ErrorHandler annotationErrorHandler) {
        this.annotationErrorHandler = annotationErrorHandler;
    }

    /**
     * @return annotation ErrorHandler of this archivist
     */
    public ErrorHandler getAnnotationErrorHandler() {
        return annotationErrorHandler;
    }

    /**
     * sets the manifest file for this archive
     *
     * @param m manifest to use at saving time
     */
    public void setManifest(Manifest m) {
        manifest = m;
    }

    /**
     * @return the manifest file for this archive
     */
    public Manifest getManifest() {
        return manifest;
    }

    /**
     * Sets the class-path for this archive
     *
     * @param newClassPath the new class-path
     */
    public void setClassPath(String newClassPath) {

        if (manifest == null) {
            manifest = new Manifest();
        }

        Attributes atts = manifest.getMainAttributes();
        atts.putValue(Attributes.Name.CLASS_PATH.toString(), newClassPath);
    }

    /**
     * @return the class-path as set in the manifest associated
     *         with the archive
     */
    public String getClassPath() {

        if (manifest == null) {
            return null;
        }

        Attributes atts = manifest.getMainAttributes();
        return atts.getValue(Attributes.Name.CLASS_PATH);
    }

    /**
     * @return a list of libraries included in the archivist
     */
    public Vector getLibraries(Archive archive) {

        Enumeration<String> entries = archive.entries();
        if (entries == null)
            return null;

        Vector libs = new Vector();
        while (entries.hasMoreElements()) {

            String entryName = entries.nextElement();
            if (entryName.indexOf('/') != -1) {
                continue; // not on the top level
            }
            if (entryName.endsWith(".jar")) {
                libs.add(entryName);
            }
        }
        return libs;
    }

    /**
     * @returns an entry name unique amongst names in this archive based on the triel name.
     */
    protected String getUniqueEntryFilenameFor(Archive archive, String trialName) throws IOException {
        Vector entriesNames = new Vector();
        Enumeration e = archive.entries();
        while (e != null && e.hasMoreElements()) {
            entriesNames.add(e.nextElement());
        }
        return Descriptor.createUniqueFilenameAmongst(trialName, entriesNames);
    }

    /**
     * utility method to get a tmp file in the current user directory of the provided
     * directory
     *
     * @param fileOrDirPath path to a file or directory to use as temp location (use parent directory
     *             if a file is provided)
     */
    protected static File getTempFile(String fileOrDirPath) throws IOException {
        if (fileOrDirPath != null) {
            return getTempFile(new File(fileOrDirPath));
        } else {
            return getTempFile((File) null);
        }
    }

    /**
     * @return the list of files that should not be copied from the old archive
     *         when a save is performed.
     */
    public Vector getListOfFilesToSkip(ReadableArchive archive) throws IOException {

        Vector filesToSkip = new Vector();
        filesToSkip.add(getDeploymentDescriptorPath());
        if (manifest != null) {
            filesToSkip.add(JarFile.MANIFEST_NAME);
        }
        if (getRuntimeDeploymentDescriptorPath(archive) != null) {
            filesToSkip.add(getRuntimeDeploymentDescriptorPath(archive));
        }

        // Can't depend on having a descriptor, so skip all possible
        // web service deployment descriptor paths.
        filesToSkip.addAll(
                getAllWebservicesDeploymentDescriptorPaths());

        return filesToSkip;
    }

    /**
     * utility method to get a tmp file in the current user directory of the provided
     * directory
     *
     * @param fileOrDir file or directory to use as temp location (use parent directory
     *             if a file is provided)
     */
    protected static File getTempFile(File fileOrDir) throws IOException {

        File dir = null;
        if (fileOrDir == null) {
            dir = new File(System.getProperty("user.dir"));
        } else {
            if (!fileOrDir.isDirectory()) {
                dir = fileOrDir.getParentFile();
                if (dir == null) {
                    dir = new File(System.getProperty("user.dir"));
                }
            } else {
                dir = fileOrDir;
            }
        }
        return File.createTempFile("tmp", ".jar", dir);
    }

    /**
     * add a file to an output abstract archive
     *
     * @param archive abstraction to use when adding the file
     * @param filePath to the file to add
     * @param entryName the entry name in the archive
     */
    protected static void addFileToArchive(WritableArchive archive, String filePath, String entryName)
            throws IOException {

        FileInputStream is = null;
        try {
            is =  new FileInputStream(new File(filePath));
            OutputStream os = archive.putNextEntry(entryName);
            ArchivistUtils.copyWithoutClose(is, os);
        } finally {
            try {
                if (is!=null)
                    is.close();
            } catch(IOException e) {
                archive.closeEntry();
                throw e;
            }
            archive.closeEntry();
        }
    }

    /**
     * copy all contents of a jar file to a new jar file except
     * for all the deployment descriptors files
     *
     * @param in  jar file
     * @param out jar file
     * @param ignoreList vector of entry name to not copy from to source jar file
     */
    protected void copyJarElements(ReadableArchive in, WritableArchive out, Vector ignoreList)
            throws IOException {

        Enumeration entries = in.entries();
        if (entries != null) {
            for (; entries.hasMoreElements();) {
                String anEntry = (String) entries.nextElement();
                if (ignoreList == null || !ignoreList.contains(anEntry)) {
                    InputStream is = in.getEntry(anEntry);
                    if (is != null) {
                      OutputStream os = out.putNextEntry(anEntry);
                      ArchivistUtils.copyWithoutClose(is, os);
                      is.close();
                    }
                    out.closeEntry();
                }
            }
        }
    }

    /**
     * rename a tmp file
     *
     * @param from name
     * @param to name
     */
    protected boolean renameTmp(String from, String to) throws IOException {

        ReadableArchive finalArchive = archiveFactory.openArchive(new File(to));
        finalArchive.delete();
        ReadableArchive tmpArchive = archiveFactory.openArchive(new File(from));
        boolean success = tmpArchive.renameTo(to);
        if (!success) {
            throw new IOException("Error renaming JAR");
        }
        return success;
    }

    /**
     * Sets the path for this archivist's archive file
     */
    public void setArchiveUri(String path) {
        this.path = path;
    }

    /**
     * @return the path for this archivist's archive file
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the classloader for this archivist
     *
     * @param classLoader  classLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Gets the classloader for this archivist
     *
     * @return classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Turn on or off the XML Validation for all standard deployment
     * descriptors loading
     *
     * @param validate set to true to turn on XML validation
     */
    public void setXMLValidation(boolean validate) {
        isValidatingXML = validate;
    }

    /**
     * @return true if the Deployment Descriptors XML will be validated
     *         while reading.
     */
    public boolean getXMLValidation() {
        return isValidatingXML;
    }

    /**
     * Turn on or off the XML Validation for runtime deployment
     * descriptors loading
     *
     * @param validate set to true to turn on XML validation
     */
    public void setRuntimeXMLValidation(boolean validate) {
        isValidatingRuntimeXML = validate;
    }

    /**
     * @return true if the runtime XML will be validated
     *         while reading.
     */
    public boolean getRuntimeXMLValidation() {
        return isValidatingRuntimeXML;
    }

    /**
     * Sets the xml validation error reporting/recovering level.
     * The reporting level is active only when xml validation is
     * turned on @see setXMLValidation.
     * so far, two values can be passed, medium which reports the
     * xml validation and continue and full which reports the
     * xml validation and stop the xml parsing.
     */
    public void setXMLValidationLevel(String level) {
        validationLevel = level;
    }

    /**
     * @return the xml validation reporting level
     */
    public String getXMLValidationLevel() {
        return validationLevel;
    }

    /**
     * Sets the runtime xml validation error reporting/recovering level.
     * The reporting level is active only when xml validation is
     * turned on @see setXMLValidation.
     * so far, two values can be passed, medium which reports the
     * xml validation and continue and full which reports the
     * xml validation and stop the xml parsing.
     */
    public void setRuntimeXMLValidationLevel(String level) {
        runtimeValidationLevel = level;
    }

    /**
     * @return the runtime xml validation reporting level
     */
    public String getRuntimeXMLValidationLevel() {
        return runtimeValidationLevel;
    }

    /**
     * validates the DOL Objects associated with this archivist, usually
     * it requires that a class loader being set on this archivist or passed
     * as a parameter
     */
    public void validate(ClassLoader aClassLoader) {
    }

    protected void postValidate(BundleDescriptor bundleDesc, ReadableArchive archive) {
        ComponentPostVisitor postVisitor = habitat.getService(ComponentPostVisitor.class);
        postVisitor.setArchive(archive);
        bundleDesc.visit(postVisitor);
    }


    /**
     * Copy this archivist to a new abstract archive
     *
     * @param target the new archive to use to copy our contents into
     */
    public void copyInto(WritableArchive target) throws IOException {
        ReadableArchive source = archiveFactory.openArchive(new File(path));
        copyInto(source, target);
    }

    /**
     * Copy source archivist to a target abstract archive.  By default,
     * every entry in source archive will be copied to the target archive,
     * including the manifest of the source archive.
     *
     * @param source        the source archive to copy from
     * @param target        the target archive to copy to
     */
    public void copyInto(ReadableArchive source, WritableArchive target) throws IOException {
        copyInto(source, target, null, true);
    }

    /**
     * Copy source archivist to a target abstract archive.  By default,
     * every entry in source archive will be copied to the target archive.
     *
     * @param source            the source archive to copy from
     * @param target            the target archive to copy to
     * @param overwriteManifest if true, the manifest in source archive
     *                          overwrites the one in target archive
     */
    public void copyInto(ReadableArchive source, WritableArchive target, boolean overwriteManifest) throws IOException {
        copyInto(source, target, null, overwriteManifest);
    }

    /**
     * Copy source archivist to a target abstract archive.  By default, the manifest
     * in source archive overwrites the one in target archive.
     *
     * @param source        the source archive to copy from
     * @param target        the target archive to copy to
     * @param entriesToSkip the entries that will be skipped by target archive
     */
    public void copyInto(ReadableArchive source, WritableArchive target, Vector entriesToSkip)
            throws IOException {
        copyInto(source, target, entriesToSkip, true);
    }

    /**
     * Copy this archivist to a new abstract archive
     *
     * @param source            the source archive to copy from
     * @param target            the target archive to copy to
     * @param entriesToSkip     the entries that will be skipped by target archive
     * @param overwriteManifest if true, the manifest in source archive
     *                          overwrites the one in target archive
     */
    public void copyInto(ReadableArchive source, WritableArchive target,
                         Vector entriesToSkip, boolean overwriteManifest)
            throws IOException {

        copyJarElements(source, target, entriesToSkip);
        if (overwriteManifest) {
            Manifest m = source.getManifest();
            if (m != null) {
                OutputStream os = target.putNextEntry(JarFile.MANIFEST_NAME);
                m.write(os);
                target.closeEntry();
            }
        }
    }

    /**
     * extract a entry of this archive to a file
     *
     * @param entryName entry name
     * @param out file to copy the entry into
     */
    public void extractEntry(String entryName, File out) throws IOException {
        ReadableArchive archive = archiveFactory.openArchive(new File(path));
        InputStream is = archive.getEntry(entryName);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(out));
        ArchivistUtils.copy(new BufferedInputStream(is), os);
        archive.close();
    }

    // only copy the entry if the destination archive does not have this entry
    public void copyAnEntry(ReadableArchive in,
                                   WritableArchive out, String entryName) 
        throws IOException {
        InputStream is = null;
        InputStream is2 = null;
        ReadableArchive in2 = archiveFactory.openArchive(out.getURI());
        try {
            is = in.getEntry(entryName);
            is2 = in2.getEntry(entryName);
            if (is != null && is2 == null) {
                OutputStream os = out.putNextEntry(entryName);
                ArchivistUtils.copyWithoutClose(is, os);
            }
        } finally {
            /*
             *Close any streams that were opened.
             */
            in2.close();
            if (is != null) {
                is.close();
            }
            if (is2 != null) {
                is2.close();
            }
            out.closeEntry();
        }
    }

    public void copyStandardDeploymentDescriptors(ReadableArchive in,
                                                  WritableArchive out) throws IOException {
        String entryName = getDeploymentDescriptorPath();
        copyAnEntry(in, out, entryName);
    }

    // copy wsdl and mapping files etc 
    public void copyExtraElements(ReadableArchive in,
                                         WritableArchive out) throws IOException {
        Enumeration entries = in.entries();
        if (entries != null) {
            for (; entries.hasMoreElements();) {
                String anEntry = (String) entries.nextElement();
                if (anEntry.endsWith(PERSISTENCE_DD_ENTRY)) {
                    // Don't copy persistence.xml file because they are some times
                    // bundled inside war/WEB-INF/lib/*.jar and hence we always
                    // read them from exploded directory.
                    // see Integration Notice #80587
                    continue;
                }
                if (anEntry.indexOf(WSDL) != -1 ||
                        anEntry.indexOf(XML) != -1 ||
                        anEntry.indexOf(XSD) != -1) {
                    copyAnEntry(in, out, anEntry);
                }
            }
        }
    }

    // for backward compat, we are not implementing those methods directly
    public Object readMetaInfo(ReadableArchive archive) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected boolean isProcessAnnotation(BundleDescriptor descriptor) {
        // if the system property is set to process annotation for pre-JavaEE5
        // DD, the semantics of isFull flag is: full attribute is set to 
        // true in DD. Otherwise the semantics is full attribute set to 
        // true or it is pre-JavaEE5 DD.
        boolean isFull = false;
        if (processAnnotationForOldDD) {
            isFull = descriptor.isFullAttribute();
        } else {
            isFull = descriptor.isFullFlag();
        }

        // only process annotation when these two requirements satisfied:
        // 1. It is not a full deployment descriptor
        // 2. It is called through dynamic deployment
        return (!isFull && annotationProcessingRequested && classLoader != null);
    }

    public Vector getAllWebservicesDeploymentDescriptorPaths() {
        Vector allDescPaths = new Vector();
        allDescPaths.add(WEB_WEBSERVICES_JAR_ENTRY);
        allDescPaths.add(EJB_WEBSERVICES_JAR_ENTRY);

        return allDescPaths;
    }
}
