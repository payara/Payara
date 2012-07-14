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

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.ServiceReferenceDescriptor;
import com.sun.enterprise.deployment.annotation.introspection.AppClientPersistenceDependencyAnnotationScanner;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.deployment.util.AnnotationDetector;
import com.sun.enterprise.loader.ASURLClassLoader;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.xml.sax.SAXParseException;

import javax.enterprise.deploy.shared.ModuleType;
import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Represents an app client that is in a stand-alone archive, not inside an
 * enterprise app archive and not a .class file.
 * @author tjquinn
 */
@Service
public class StandAloneAppClientInfo extends AppClientInfo implements PostConstruct {

    @Inject
    protected ArchivistFactory archivistFactory;

    private ReadableArchive appClientArchive;

    private AppClientArchivist appClientArchivist = null;

    public StandAloneAppClientInfo(
            boolean isJWS, Logger logger, ReadableArchive archive,
            String mainClassFromCommandLine) 
        throws IOException, ClassNotFoundException, 
               URISyntaxException, SAXParseException {
        super(isJWS, logger, mainClassFromCommandLine);
        appClientArchive = archive;
    }

    public void postConstruct() {
        Archivist archivist = archivistFactory.getArchivist(ModuleType.CAR.toString(), getClassLoader());
        if (!(archivist instanceof AppClientArchivist)) {
            throw new IllegalArgumentException("expected an app client module but " +
                    appClientArchive.getURI().toASCIIString() +
                    " was recognized by " + archivist.getClass().getName());
        }
        appClientArchivist = (AppClientArchivist) archivist;
        setDescriptor(appClientArchivist.getDescriptor());
    }

    /**
     *Finishes initialization work.
     *<p>
     *The calling logic that instantiates this object must invoke completeInit
     *after instantiation but before using the object.
     *@throws IOException for errors opening the expanded archive
     *@throws SAXParseException for errors parsing the descriptors in a newly-opened archive
     *@throws ClassNotFoundException if the main class requested cannot be located in the archive
     *@throws URISyntaxException if preparing URIs for the class loader fails
     *
     */
    @Override
    protected void completeInit(/*URL[] persistenceURLs*/)
        throws Exception {

        //expand if needed. initialize the appClientArchive
//        appClientArchive = expand(appClientArchive);

        //Create the class loader to be used for persistence unit checking,
        //validation, and running the app client.

        // XXX The system class loader should have everything we need

//        classLoader = createClassLoader(appClientArchive, persistenceURLs);

        //Populate the deployment descriptor without validation.
        //Note that validation is done only after the persistence handling
        //has instructed the classloader created above.
        populateDescriptor(appClientArchive, appClientArchivist, getClassLoader());

         //If the selected app client depends on at least one persistence unit
         //then handle the P.U. before proceeding.
        if (appClientDependsOnPersistenceUnit(getAppClient())) {
            //@@@check to see if the descriptor is metadata-complet=true
            //if not, we would have loaded classes into the classloader
            //during annotation processing.  we need to hault and ask
            //the user to deploy the application.
            //if (!getAppClient().isFullFlag()) {
            //    throw new RuntimeException("Please deploy your application");
            //}
            handlePersistenceUnitDependency();
        }

         //Now that the persistence handling has run and instrumented the class
         //loader - if it had to - it's ok to validate.
        appClientArchivist.validate(getClassLoader());

        fixupWSDLEntries();

// XXX restore or move elsewhere
//        if (isJWS) {
//            grantRequestedPermissionsToUserCode();
//        }
    }

    /**
     *Adjusts the web services WSDL entries corresponding to where they
     *actually reside.
     */
    protected void fixupWSDLEntries()
        throws URISyntaxException, MalformedURLException, IOException,
               AnnotationProcessorException {
        ApplicationClientDescriptor ac = getAppClient();
        URI uri = (new File(getAppClientRoot(appClientArchive, ac))).toURI();
        File moduleFile = new File(uri);
        for (Iterator itr = ac.getServiceReferenceDescriptors().iterator();
                    itr.hasNext();) {
            ServiceReferenceDescriptor serviceRef =
                    (ServiceReferenceDescriptor) itr.next();
            if (serviceRef.getWsdlFileUri()!=null) {
                // In case WebServiceRef does not specify wsdlLocation, we get
                // wsdlLocation from @WebClient in wsimport generated source;
                // If wsimport was given a local WSDL file, then WsdlURI will
                // be an absolute path - in that case it should not be prefixed
                // with modileFileDir
                String wsdlURI = serviceRef.getWsdlFileUri();
                File wsdlFile = new File(wsdlURI);
                if(wsdlFile.isAbsolute()) {
                    serviceRef.setWsdlFileUrl(wsdlFile.toURI().toURL());
                } else {
                    // This is the case where WsdlFileUri is a relative path
                    // (hence relative to the root of this module or wsimport
                    // was executed with WSDL in HTTP URL form
                    serviceRef.setWsdlFileUrl(getEntryAsUrl(
                        moduleFile, serviceRef.getWsdlFileUri()));
                }
            }
        }
    }

    /**
     *Reports whether the selected app client depends on a persistence unit
     *or not.
     *@returns true if the app client depends on a persistence unit
     */
    protected boolean appClientDependsOnPersistenceUnit(
        ApplicationClientDescriptor acDescr)
            throws MalformedURLException, ClassNotFoundException,
                   IOException, URISyntaxException {
            /*
             *If the descriptor contains at least one reference to an entity
             *manager then it definitely depends on a persistence unit.
             */
            return descriptorContainsPURefcs(acDescr)
                    || mainClassContainsPURefcAnnotations(acDescr);
    }

    protected boolean mainClassContainsPURefcAnnotations(
        ApplicationClientDescriptor acDescr)
            throws MalformedURLException, ClassNotFoundException,
                   IOException, URISyntaxException {
        AnnotationDetector annoDetector =
                    new AnnotationDetector(new AppClientPersistenceDependencyAnnotationScanner());

        //e.g. FROM a.b.Foo or Foo TO a/b/Foo.class or Foo.class
        String mainClassEntryName =
                acDescr.getMainClassName().replace('.', '/') + ".class";

        return classContainsAnnotation
                (mainClassEntryName, annoDetector, appClientArchive, acDescr);
    }

    private RootDeploymentDescriptor populateDescriptor(
            ReadableArchive archive, Archivist theArchivist, ClassLoader loader)
        throws IOException, SAXParseException, Exception {

        //@@@ Optimize it later.
        //Here the application.xml is read twice for NestedAppClientInfo.
        //Once already in expand() method.

        theArchivist.setAnnotationProcessingRequested(true);

        //@@@ setting of the classloader would trigger annotation processing
        //for appclients that have only partial deployment descriptors or no
        //descriptors at all.
        //Note that the annotation processing is bypassed if the descriptors
        //are meta-complete=true", which will be the case for anything that is
        //generated by the backend, i.e. if the appclient being executed here
        //is a generated jar produced by the appserver, obtained by deploying
        //the original application client and retrieve.
        theArchivist.setClassLoader(loader);

        //open with Archivist./pen(AbstractArchive) to also ensure the
        //validation is not called
        //return archivist.open(archive);
        RootDeploymentDescriptor d = null;
        try {
            d = theArchivist.open(archive);
        } catch (Exception ex) {
            close(); //make sure there is no junk tmp director left
            throw ex;
        }

        //depend on the type of the appclient, additional work needs
        //to be done.
        massageDescriptor();

        theArchivist.setDescriptor((BundleDescriptor)d);
        return d;
    }

//    @Override
//    protected ReadableArchive expand(File file)
//        throws IOException, Exception {
//        return archiveFactory.openArchive(file);
//    }
//
//    @Override
//    protected boolean deleteAppClientDir() {
//        return false;
//    }

    @Override
    protected void massageDescriptor()
            throws IOException, AnnotationProcessorException {
        getDescriptor().getModuleDescriptor().setStandalone(true);
    }

    /**
     *Closes the instance of AppClientInfo, deleting any temporary directory
     *created and closing the archive.
     *@throws IOException in case of error closing the archive
     */
    @Override
    protected void close() throws IOException {
        try {
            // XXX Mitesh helping to update this
//            if (puAppInfo != null) {
//                new PersistenceUnitLoaderImpl().unload(puAppInfo);
//                puAppInfo = null;
//            }
            if (appClientArchive != null) {
                appClientArchive.close();
            }
            ClassLoader classLoader = getClassLoader();
            if (classLoader != null &&
                    classLoader instanceof ASURLClassLoader) {
                ((ASURLClassLoader) classLoader).done();
            }
        } finally {
            if (deleteAppClientDir()) {
                if (appClientArchive != null) {
                    appClientArchive.delete();
                }
            }
            appClientArchive = null;
        }
    }

    @Override
    protected boolean classContainsAnnotation(
            String entry, AnnotationDetector detector, 
            ReadableArchive archive, ApplicationClientDescriptor descriptor)
            throws FileNotFoundException, IOException {
//        JarFile jar = null;
        try {
            return detector.containsAnnotation(archive, entry);
        } catch (Throwable thr) {
            throw new RuntimeException(getLocalString(
                "appclient.errorCheckingAnnos",
                "Error checking for persistence unit annotations in the main class"), thr);
        } finally {
//            if (jar != null) {
//                try {
//                    jar.close();
//                } catch (IOException ioe) {
//                    throw new RuntimeException(getLocalString(
//                        "appclient.errorClosingJar",
//                        "Error closing archive {0} used in checking for persistence unit annotations",
//                        archive.getURI().toASCIIString()), ioe);
//                }
//            }
        }
    }
    @Override
    public String toString() {
        String lineSep = System.getProperty("line.separator");
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getName() + ": " + lineSep);
        result.append("  isJWS: " + isJWS);
        result.append("  archive file: " + appClientArchive.getURI().toASCIIString() + lineSep);
        result.append("  archive type: " + appClientArchive.getClass().getName() + lineSep);
        result.append("  archivist type: " + appClientArchivist.getClass().getName() + lineSep);
        result.append("  main class to be run: " + mainClassNameToRun + lineSep);
        result.append("  temporary archive directory: " + appClientArchive.getURI() + lineSep);
        result.append("  class loader type: " + getClassLoader().getClass().getName() + lineSep);

        return result.toString();
    }}
