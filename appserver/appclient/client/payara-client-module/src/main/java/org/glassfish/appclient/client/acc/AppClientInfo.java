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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.util.AnnotationDetector;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.api.deployment.InstrumentableClassLoader;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.appclient.common.Util;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.xml.sax.SAXParseException;

import javax.persistence.EntityManagerFactory;

/**
 *Represents information about the app client, regardless of what type of
 *archive (jar or directory) it is stored in or what type of module 
 *(app client or nested within an ear) that archive holds.
 *
 *@author tjquinn
 */
@Service
public abstract class AppClientInfo {

    public static final String USER_CODE_IS_SIGNED_PROPERTYNAME = "com.sun.aas.user.code.signed";
    
    private static final String SIGNED_USER_CODE_PERMISSION_TEMPLATE_NAME = "jwsclientSigned.policy";
    
    private static final String UNSIGNED_USER_CODE_PERMISSION_TEMPLATE_NAME = "jwsclientUnsigned.policy";
    
    private static final String CODE_BASE_PLACEHOLDER_NAME = "com.sun.aas.jws.client.codeBase";
    
    /** logger */
    protected Logger _logger;
    
//    /** abstract representation of the storage location */
//    private ReadableArchive appClientArchive = null;

//    /** original appclient (file or directory) */
//    private File appClientArchive = null;
//    private ReadableArchive appClient;

    /** abstract archivist able to operate on the module in the location 
      * (specified by archive */
    private Archivist archivist = null;
    
    /** main class name as the user specified it on the command line */
    protected String mainClassFromCommandLine;
    
    /**
     *main class to be used - could come from the command line or from the 
     *manifest of the selected app client archive 
     */
    protected String mainClassNameToRun = null;

    /** class loader - cached */
    private ClassLoader classLoader = null;
    
    /** indicates if the app client has been launched using Java Web Start */
    protected boolean isJWS = false;

    /**
     * descriptor from the app client module or a default one for the
     * .class file case and the regular java launch with main class case
     */
    private ApplicationClientDescriptor acDesc = null;
    
    /** access to the localizable strings */
    private static final LocalStringManager localStrings =
            new LocalStringManagerImpl(AppClientInfo.class);

// XXX Mitesh helping to update this
//    private PersistenceUnitLoader.ApplicationInfo puAppInfo;

//    /**
//     *Creates a new instance of AppClientInfo.  Always invoked from subclasse
//     *because AppClientInfo is abstract.
//     *<p>
//     *Note that any code instantiationg one of the concrete subclasses MUST
//     *invoke completeInit after this super.constructor has returned.
//     *
//     *@param isJWS indicates if ACC has been launched using Java Web Start
//     *@param logger the logger to use for messages
//     *@param archive the AbstractArchive for the app client module or
//     *               directory being launched
//     *@param archivist the Archivist corresponding to the type of module
//     *                 in the archive
//     *@param mainClassFromCommandLine the main class name specified on the
//     *       command line (null if not specified)
//     */
//    public AppClientInfo(boolean isJWS, Logger logger, ReadableArchive appClientarchive,
//                         Archivist archivist, String mainClassFromCommandLine) {
//        this.isJWS = isJWS;
//        _logger = logger;
//        this.appClientArchive = appClientarchive;
//        this.archivist = archivist;
//        this.mainClassFromCommandLine = mainClassFromCommandLine;
//    }

    /**
     * Creates a new AppClientInfo for a main class file.
     * 
     * @param isJWS
     * @param logger
     * @param mainClassFromCommandLine
     */
    public AppClientInfo(boolean isJWS, Logger logger, String mainClassFromCommandLine) {
        this.isJWS = isJWS;
        _logger = logger;
        this.mainClassFromCommandLine = mainClassFromCommandLine;
    }

    protected void setDescriptor(ApplicationClientDescriptor acDesc) {
        this.acDesc = acDesc;
    }

    protected ApplicationClientDescriptor getDescriptor() {
        return acDesc;
    }

    protected void completeInit() throws Exception {
        
    }
//    /**
//     *Finishes initialization work.
//     *<p>
//     *The calling logic that instantiates this object must invoke completeInit
//     *after instantiation but before using the object.
//     *@throws IOException for errors opening the expanded archive
//     *@throws SAXParseException for errors parsing the descriptors in a newly-opened archive
//     *@throws ClassNotFoundException if the main class requested cannot be located in the archive
//     *@throws URISyntaxException if preparing URIs for the class loader fails
//     *
//     */
//    protected void completeInit(URL[] persistenceURLs)
//        throws IOException, SAXParseException, ClassNotFoundException,
//               URISyntaxException, AnnotationProcessorException, Exception {
//
//        //expand if needed. initialize the appClientArchive
////        appClientArchive = expand(appClientArchive);
//
//        //Create the class loader to be used for persistence unit checking,
//        //validation, and running the app client.
//        classLoader = createClassLoader(appClientArchive, persistenceURLs);
//
//        //Populate the deployment descriptor without validation.
//        //Note that validation is done only after the persistence handling
//        //has instructed the classloader created above.
//        populateDescriptor(appClientArchive, archivist, classLoader);
//
//         //If the selected app client depends on at least one persistence unit
//         //then handle the P.U. before proceeding.
//        if (appClientDependsOnPersistenceUnit(getAppClient())) {
//            //@@@check to see if the descriptor is metadata-complet=true
//            //if not, we would have loaded classes into the classloader
//            //during annotation processing.  we need to hault and ask
//            //the user to deploy the application.
//            //if (!getAppClient().isFullFlag()) {
//            //    throw new RuntimeException("Please deploy your application");
//            //}
//            handlePersistenceUnitDependency();
//        }
//
//         //Now that the persistence handling has run and instrumented the class
//         //loader - if it had to - it's ok to validate.
//        archivist.validate(classLoader);
//
//        fixupWSDLEntries();
//
//        if (isJWS) {
//            grantRequestedPermissionsToUserCode();
//        }
//    }
    
    /**
     *Returns the app client descriptor to be run.
     *@return the descriptor for the selected app client
     */
    protected ApplicationClientDescriptor getAppClient() {
        return getAppClient(archivist);
    }

    protected ClassLoader getClassLoader() {
        return classLoader;
    }

    protected void close() throws IOException {

    }
    protected boolean deleteAppClientDir() {
        return !_keepExplodedDir;
    }
    
    protected String getLocalString(final String key, final String defaultMessage,
            final Object... args) {
        String result = localStrings.getLocalString(this.getClass(),
                key, defaultMessage, args);
        return result;
    }

    /**
     *Processes persistence unit handling for the ACC.
     */
    protected void handlePersistenceUnitDependency()
            throws URISyntaxException, MalformedURLException {
        // XXX Mitesh helping to update this
//        this.puAppInfo = new ApplicationInfoImpl(this);
//        new PersistenceUnitLoaderImpl().load(puAppInfo);
    }

    /**
     * implementation of
     * {@link com.sun.enterprise.server.PersistenceUnitLoader.ApplicationInfo}.
     */
    private static class ApplicationInfoImpl
            // XXX Mitesh helping to update this
//            implements PersistenceUnitLoader.ApplicationInfo
    {

        private AppClientInfo outer; // the outer object we are associated with

        private ApplicationClientDescriptor appClient;

        public ApplicationInfoImpl(AppClientInfo outer) {
            this.outer = outer;
            appClient = outer.getAppClient();
        }

        //TODO: This method does not appear to be used -- can it be deleted?
        public Application getApplication(ServiceLocator habitat) {
        	
            Application application = appClient.getApplication();
            if (application == null) {
                application = Application.createVirtualApplication(
                        appClient.getModuleID(),
                        appClient.getModuleDescriptor());
            }
            return application;
        }

        public InstrumentableClassLoader getClassLoader() {
            return (InstrumentableClassLoader) outer.getClassLoader();
        }

//        public String getApplicationLocation() {
//            return outer.appClientArchive.getURI().toASCIIString();
//        }

        /**
         * @return list of PU that are actually referenced by the
         *         appclient.
         */
        public Collection<? extends PersistenceUnitDescriptor>
                getReferencedPUs() {
            return appClient.findReferencedPUs();
        }

        /**
         * @return list of EMFs that have been loaded for this appclient.
         */
        public Collection<? extends EntityManagerFactory> getEntityManagerFactories() {
            Collection<EntityManagerFactory> emfs =
                    new HashSet<EntityManagerFactory>();

            if (appClient.getApplication() != null) {
                emfs.addAll(appClient.getApplication()
                        .getEntityManagerFactories());
            }
            emfs.addAll(appClient.getEntityManagerFactories());
            return emfs;
        }
    } // end of class ApplicationInfoImpl

    /**
     *Reports whether the app client's descriptor shows a dependence on a
     *persistence unit.
     *@param descr the descriptor for the app client in question
     *@returns true if the descriptor shows such a dependency
     */
    protected  boolean descriptorContainsPURefcs(
        ApplicationClientDescriptor descr) {
        return ! descr.getEntityManagerFactoryReferenceDescriptors().isEmpty();
    }
    
    /**
     *Reports whether the main class in the archive contains annotations that 
     *refer to persistence units.
     *@return boolean if the main class contains annotations that refer to a pers. unit
     */

    protected static URL getEntryAsUrl(File moduleLocation, String uri)
        throws MalformedURLException, IOException {
        URL url = null;
        try {
            url = new URL(uri);
        } catch(java.net.MalformedURLException e) {
            // ignore
            url = null;
        }
        if (url!=null) {
            return url;
        }
        if( moduleLocation != null ) {
            if( moduleLocation.isFile() ) {
                url = createJarUrl(moduleLocation, uri);
            } else {
                String path = uri.replace('/', File.separatorChar);
                url = new File(moduleLocation, path).toURI().toURL();
            }
        }
        return url;
    }

    private static URL createJarUrl(File jarFile, String entry)
        throws MalformedURLException, IOException {
        return new URL("jar:" + jarFile.toURI().toURL() + "!/" + entry);
    }


// XXX restore or move elsewhere -- grants permissions to jars expanded from v2 generated JAR
//    /**
//     *Granting the appropriate level of permissions to user code, emulating
//     *the Java Web Start behavior as required in the JNLP spec.
//     *<p>
//     *Classes from the user's app client jar are loaded using the ASURLClassLoader
//     *rather than the Java Web Start class loader.  As a result, Java Web Start
//     *cannot grant the appropriate level of permissions to the user code since
//     *it is the JWS class loader that does that.  So we need to grant the user
//     *code the appropriate level of permissions, based on whether the user's
//     *app client jar was signed or not.
//     *@param retainTempFiles indicates whether to keep the generated policy file
//     */
//    protected void grantRequestedPermissionsToUserCode() throws IOException, URISyntaxException {
//        /*
//         *Create a temporary file containing permissions grants.  We will use
//         *this temp file to refresh the Policy objects's settings.  The temp
//         *file will contain one segment for each element in the class path
//         *for the user code (from the expanded generated app client jar).
//         *The permissions granted will be the same for all class path elements,
//         *and the specific settings will either be the sandbox permissions
//         *as described in the JNLP spec or full permissions.
//         */
//        boolean userJarIsSigned = Boolean.getBoolean(USER_CODE_IS_SIGNED_PROPERTYNAME);
//
//        boolean retainTempFiles = Boolean.getBoolean(AppClientContainer.APPCLIENT_RETAIN_TEMP_FILES_PROPERTYNAME);
//
//        /*
//         *Use a template to construct each part of the policy file, choosing the
//         *template based on whether the user code is signed or not.
//         */
//        String templateName = (userJarIsSigned ? SIGNED_USER_CODE_PERMISSION_TEMPLATE_NAME : UNSIGNED_USER_CODE_PERMISSION_TEMPLATE_NAME);
//        String template = Util.loadResource(JWSACCMain.class, templateName);
//
//        /*
//         *Create the temporary policy file to write to.
//         */
//        File policyFile = File.createTempFile("accjws-user", ".policy");
//        if ( ! retainTempFiles) {
//            policyFile.deleteOnExit();
//        }
//
//        PrintStream ps = null;
//
//        try {
//            ps = new PrintStream(policyFile);
//
//            Properties p = new Properties();
//
//            /*
//             *Use the list of class paths already set up on the ASURLClassLoader.
//             *Then for each element in the class path, write a part of the policy
//             *file granting the privs specified in the selected template to the code path.
//             */
//            ASURLClassLoader loader = (ASURLClassLoader) getClassLoader();
//            for (URL classPathElement : loader.getURLs()) {
//                /*
//                 *Convert the URL into a proper codebase expression suitable for
//                 *a grant clause in the policy file.
//                 */
//                String codeBase = Util.URLtoCodeBase(classPathElement);
//                if (codeBase != null) {
//                    p.setProperty(CODE_BASE_PLACEHOLDER_NAME, codeBase);
//                    String policyPart = Util.replaceTokens(template, p);
//
//                    ps.println(policyPart);
//                }
//            }
//
//            /*
//             *Close the temp file and use it to refresh the current Policy object.
//             */
//            ps.close();
//
//            JWSACCMain.refreshPolicy(policyFile);
//
//            if ( ! retainTempFiles) {
//                policyFile.delete();
//            }
//        } finally {
//            if (ps != null) {
//                ps.close();
//            }
//        }
//    }
    
    /////////////////////////////////////////////////////////////////
    //  The following protected methods are overridden by at least //
    //  one of the sub classes.                                    //
    /////////////////////////////////////////////////////////////////

//    /**
//     *Expands the contents of the source archive into a temporary
//     *directory, using the same format as backend server expansions.
//     *@param file an archive file to be expanded
//     *@return an opened archive for the expanded directory archive
//     *@exception IOException in case of errors during the expansion
//     */
//    protected abstract ReadableArchive expand(File file)
//        throws IOException, Exception;

    protected ApplicationClientDescriptor getAppClient(
        Archivist archivist) {
        return ApplicationClientDescriptor.class.cast(
                    archivist.getDescriptor());
    }

    protected String getAppClientRoot(
        ReadableArchive archive, ApplicationClientDescriptor descriptor) {
        return archive.getURI().toASCIIString();
    }                                        

    protected void massageDescriptor()
            throws IOException, AnnotationProcessorException {
        //default behavor: no op
    }

    protected List<String> getClassPaths(ReadableArchive archive) {
        List<String> paths = new ArrayList();
        paths.add(archive.getURI().toASCIIString());
        return paths;
    }

    /**
     *Returns the main class that should be executed.
     *@return the name of the main class to execute when the client starts
     */
    protected String getMainClassNameToRun(ApplicationClientDescriptor acDescr) {
         if (mainClassNameToRun == null) {
             if (mainClassFromCommandLine != null) {
                 mainClassNameToRun = mainClassFromCommandLine;
                 _logger.fine("Main class is " + mainClassNameToRun + " from command line");
             } else {
                 /*
                  *Find out which class to execute from the descriptor.
                  */
                 mainClassNameToRun = getAppClient().getMainClassName();
                 _logger.fine("Main class is " + mainClassNameToRun + " from descriptor");
             }
         }
         return mainClassNameToRun;
    }

    protected boolean classContainsAnnotation(
            String entry, AnnotationDetector detector, 
            ReadableArchive archive, ApplicationClientDescriptor descriptor)
            throws FileNotFoundException, IOException {
        return detector.containsAnnotation(archive, entry);
//        String acRoot = getAppClientRoot(archive, descriptor);
//        String entryLocation = acRoot + File.separator + entry;
//        File entryFile = new File(entryLocation);
//        return detector.containsAnnotation(archive, entry)
//        return detector.containsAnnotation(entryFile);
    }

    @Override
    public String toString() {
        String lineSep = System.getProperty("line.separator");
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getName() + ": " + lineSep);
        result.append("  isJWS: " + isJWS);
//        result.append("  archive file: " + appClientArchive.getURI().toASCIIString() + lineSep);
//        result.append("  archive type: " + appClientArchive.getClass().getName() + lineSep);
        result.append("  archivist type: " + archivist.getClass().getName() + lineSep);
        result.append("  main class to be run: " + mainClassNameToRun + lineSep);
//        result.append("  temporary archive directory: " + appClientArchive.getURI() + lineSep);
        result.append("  class loader type: " + classLoader.getClass().getName() + lineSep);

        return result.toString();

    }


    //for debug purpose
    protected static final boolean _keepExplodedDir = 
            Boolean.getBoolean("appclient.keep.exploded.dir");
}
