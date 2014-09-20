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

package org.glassfish.web.deployment.archivist;

import com.sun.enterprise.deployment.Application;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;
import org.glassfish.web.deployment.annotation.impl.WarScanner;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFor;
import com.sun.enterprise.deployment.archivist.ExtensionsArchivist;
import com.sun.enterprise.deployment.io.DeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.util.*;
import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.WarType;
import org.glassfish.web.deployment.descriptor.*;
import org.glassfish.web.deployment.io.WebDeploymentDescriptorFile;
import org.glassfish.web.deployment.util.*;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This module is responsible for reading and write web applications
 * archive files (war).
 *
 * @author  Jerome Dochez
 * @version
 */
@Service @PerLookup
@ArchivistFor(WarType.ARCHIVE_TYPE)
public class WebArchivist extends Archivist<WebBundleDescriptorImpl> {

    private static final Logger logger = com.sun.enterprise.web.WebContainer.logger;

    @LogMessageInfo(
            message = "Error in parsing default-web.xml",
            level = "WARNING")
    private static final String ERROR_PARSING = "AS-WEB-GLUE-00276";


    private static final String DEFAULT_WEB_XML = "default-web.xml";

    @Inject
    private ServerEnvironment env;

    private WebBundleDescriptorImpl defaultWebXmlBundleDescriptor = null;

    /**
     * @return the  module type handled by this archivist
     * as defined in the application DTD
     *
     */
    @Override
    public ArchiveType getModuleType() {
        return DOLUtils.warType();
    }

    /**
     * Archivist read XML deployment descriptors and keep the
     * parsed result in the DOL descriptor instances. Sets the descriptor
     * for a particular Archivst type
     */
    public void setDescriptor(Application descriptor) {
        java.util.Set webBundles = descriptor.getBundleDescriptors(WebBundleDescriptorImpl.class);
        if (webBundles.size()>0) {
            this.descriptor = (WebBundleDescriptorImpl) webBundles.iterator().next();
            if (this.descriptor.getModuleDescriptor().isStandalone())
                return;
            else
                this.descriptor=null;
        }
    }

    /**
     * @return the DeploymentDescriptorFile responsible for handling
     * standard deployment descriptor
     */
    @Override
    public DeploymentDescriptorFile<WebBundleDescriptorImpl> getStandardDDFile() {
        if (standardDD == null) {
            standardDD = new WebDeploymentDescriptorFile();
        }
        return standardDD;
    }

    /**
     * @return the list of the DeploymentDescriptorFile responsible for
     *         handling the configuration deployment descriptors
     */
    @Override
    public List<ConfigurationDeploymentDescriptorFile> getConfigurationDDFiles() {
        if (confDDFiles == null) {
            confDDFiles = DOLUtils.getConfigurationDeploymentDescriptorFiles(habitat, WarType.ARCHIVE_TYPE);
        }
        return confDDFiles;
    }

    /**
     * @return a default BundleDescriptor for this archivist
     */
    @Override
    public WebBundleDescriptorImpl getDefaultBundleDescriptor() {
        return new WebBundleDescriptorImpl();
    }

    /**
     * @return a validated WebBundleDescriptor corresponding to default-web.xml
     *         that can be used in webtier.
     */
    public synchronized WebBundleDescriptorImpl getDefaultWebXmlBundleDescriptor() {
        if (defaultWebXmlBundleDescriptor == null) {
            defaultWebXmlBundleDescriptor = getPlainDefaultWebXmlBundleDescriptor();
            WebBundleValidator validator = new WebBundleValidator();
            validator.accept(defaultWebXmlBundleDescriptor );
        }
        return defaultWebXmlBundleDescriptor ;
    }

    /**
     * After reading all the standard deployment descriptors, merge any
     * resource descriptors from EJB descriptors into the WebBundleDescriptor.
     *
     * @param descriptor the deployment descriptor for the module
     * @param archive the module archive
     * @param extensions map of extension archivists
     */
    @Override
    protected void postStandardDDsRead(WebBundleDescriptorImpl descriptor,
                ReadableArchive archive,
                Map<ExtensionsArchivist, RootDeploymentDescriptor> extensions)
                throws IOException {
        for (RootDeploymentDescriptor rd : extensions.values()) {
            if (rd instanceof EjbBundleDescriptor) {
                EjbBundleDescriptor eb = (EjbBundleDescriptor)rd;
                descriptor.addJndiNameEnvironment(eb);
                for (EjbDescriptor ejb : eb.getEjbs()) {
                    ejb.notifyNewModule(descriptor);
                }
            }
        }
    }

    /**
     * @return a non-validated WebBundleDescriptor corresponding to default-web.xml
     */
    private WebBundleDescriptorImpl getPlainDefaultWebXmlBundleDescriptor() {
        WebBundleDescriptorImpl defaultWebBundleDesc = new WebBundleDescriptorImpl();
        InputStream fis = null;

        try {
            // parse default-web.xml contents
            URL defaultWebXml = getDefaultWebXML();
            if (defaultWebXml!=null)  {
                fis = defaultWebXml.openStream();
                WebDeploymentDescriptorFile wddf =
                    new WebDeploymentDescriptorFile();
                wddf.setXMLValidation(false);
                defaultWebBundleDesc.addWebBundleDescriptor(wddf.read(fis));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, ERROR_PARSING);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ioe) {
                // do nothing
            }
        }
        return defaultWebBundleDesc;
    }

    /**
     * Obtains the location of <tt>default-web.xml</tt>.
     * This allows subclasses to load the file from elsewhere.
     *
     * @return
     *      null if not found, in which case the default web.xml will not be read
     *      and <tt>web.xml</tt> in the applications need to have everything.
     */
    protected URL getDefaultWebXML() throws IOException {
        File file = new File(env.getConfigDirPath(),DEFAULT_WEB_XML);
        if (file.exists())
            return file.toURI().toURL();
        else
            return null;
    }


    /**
     * perform any post deployment descriptor reading action
     *
     * @param descriptor the deployment descriptor for the module
     * @param archive the module archive
     */
    @Override
    protected void postOpen(WebBundleDescriptorImpl descriptor, ReadableArchive archive)
        throws IOException
    {
        super.postOpen(descriptor, archive);
        postValidate(descriptor, archive);
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
        descriptor.visit(new WebBundleValidator());
    }

    /**
     * In the case of web archive, the super handles() method should be able
     * to make a unique identification.  If not, then the archive is definitely
     * not a war.
     */
    @Override
    protected boolean postHandles(ReadableArchive abstractArchive)
            throws IOException {
        return DeploymentUtils.isArchiveOfType(abstractArchive, getModuleType(), locator);
    }

    @Override
    protected String getArchiveExtension() {
        return WEB_EXTENSION;
    }

    /**
     * @return a list of libraries included in the archivist
     */
    public Vector<String> getLibraries(Archive archive) {

        Enumeration<String> entries = archive.entries();
        if (entries==null)
            return null;

        Vector<String> libs = new Vector<String>();
        while (entries.hasMoreElements()) {

            String entryName = entries.nextElement();
            if (!entryName.startsWith("WEB-INF/lib")) {
                continue; // not in WEB-INF...
            }
            if (entryName.endsWith(".jar")) {
                libs.add(entryName);
            }
        }
        return libs;
    }

    @Override
    protected void postAnnotationProcess(WebBundleDescriptorImpl descriptor,
            ReadableArchive archive) throws IOException {
        super.postAnnotationProcess(descriptor, archive);

        // read web-fragment.xml
        List<WebFragmentDescriptor> wfList = readStandardFragments(descriptor, archive);

        // process annotations in web-fragment
        // extension annotation processing will be done in top level
        if (isProcessAnnotation(descriptor)) {
            Map<ExtensionsArchivist, RootDeploymentDescriptor> localExtensions =
                    new HashMap<ExtensionsArchivist, RootDeploymentDescriptor>();
            for (WebFragmentDescriptor wfDesc : wfList) {
                super.readAnnotations(archive, wfDesc, localExtensions);
            }

            // scan manifest classpath
            ModuleScanner scanner = getScanner();
            if (scanner instanceof WarScanner) {
                ((WarScanner)scanner).setScanOtherLibraries(true);
                readAnnotations(archive, descriptor, localExtensions, scanner);
            }
        }

        WebFragmentDescriptor mergedWebFragment = null;
        for (WebFragmentDescriptor wf : wfList) {
            if (mergedWebFragment == null) {
                mergedWebFragment = wf;
            } else {
                mergedWebFragment.addWebBundleDescriptor(wf);
            }
        }

        if (mergedWebFragment != null) {
            descriptor.addWebBundleDescriptor(mergedWebFragment);
        }

        // apply default from default-web.xml to web.xml
        WebBundleDescriptorImpl defaultWebBundleDescriptor = getPlainDefaultWebXmlBundleDescriptor();
        descriptor.addDefaultWebBundleDescriptor(defaultWebBundleDescriptor);
    }

    /**
     * This method will return the list of web fragment in the desired order.
     */
    private List<WebFragmentDescriptor> readStandardFragments(WebBundleDescriptorImpl descriptor,
            ReadableArchive archive) throws IOException {

        List<WebFragmentDescriptor> wfList = new ArrayList<WebFragmentDescriptor>();
        Vector libs = getLibraries(archive);
        if (libs != null && libs.size() > 0) {

            for (int i = 0; i < libs.size(); i++) {
                String lib = (String)libs.get(i);
                Archivist wfArchivist = new WebFragmentArchivist(this, habitat);
                wfArchivist.setRuntimeXMLValidation(this.getRuntimeXMLValidation());
                wfArchivist.setRuntimeXMLValidationLevel(
                        this.getRuntimeXMLValidationLevel());
                wfArchivist.setAnnotationProcessingRequested(false);

                WebFragmentDescriptor wfDesc = null;
                ReadableArchive embeddedArchive = archive.getSubArchive(lib);
                try {
                    if (embeddedArchive != null &&
                            wfArchivist.hasStandardDeploymentDescriptor(embeddedArchive)) {
                        try {
                            wfDesc = (WebFragmentDescriptor)wfArchivist.open(embeddedArchive);
                        } catch(SAXParseException ex) {
                            IOException ioex = new IOException();
                            ioex.initCause(ex);
                            throw ioex;
                        }
                    } else {   
                        wfDesc = new WebFragmentDescriptor();
                    }
                } finally {
                    if (embeddedArchive != null) {
                        embeddedArchive.close();
                    }
                }
                wfDesc.setJarName(lib.substring(lib.lastIndexOf('/') + 1));    
                wfList.add(wfDesc);

                descriptor.putJarNameWebFragmentNamePair(wfDesc.getJarName(), wfDesc.getName());

            }

            if (((WebBundleDescriptorImpl)descriptor).getAbsoluteOrderingDescriptor() != null) {
                wfList = ((WebBundleDescriptorImpl)descriptor).getAbsoluteOrderingDescriptor().order(wfList);
            } else {
                OrderingDescriptor.sort(wfList);
            }

            for (WebFragmentDescriptor wf : wfList) {
                descriptor.addOrderedLib(wf.getJarName());
            }
        }

        return wfList;
    }
}
