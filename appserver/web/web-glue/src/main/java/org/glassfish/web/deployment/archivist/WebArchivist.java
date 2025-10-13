/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2014-2024] [Payara Foundation and/or its affiliates]

package org.glassfish.web.deployment.archivist;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.EarType;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.deployment.Deployment;
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
import org.glassfish.web.LogFacade;
import org.glassfish.web.sniffer.WarType;
import org.glassfish.web.deployment.descriptor.*;
import org.glassfish.web.deployment.io.WebDeploymentDescriptorFile;
import org.glassfish.web.deployment.util.*;
import org.jvnet.hk2.annotations.Service;
import jakarta.inject.Inject;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


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

    private static final Logger logger = LogFacade.getLogger();


    private static final String DEFAULT_WEB_XML = "default-web.xml";

    @Inject
    private ServerEnvironment env;

    private WebBundleDescriptorImpl defaultWebXmlBundleDescriptor = null;
    @Inject
    private ArchiveFactory archiveFactory;
    @Inject
    private Deployment deployment;

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
            if (!this.descriptor.getModuleDescriptor().isStandalone())
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
            logger.log(Level.WARNING, LogFacade.ERROR_PARSING);
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
    public Set<String> getLibraries(ReadableArchive archive) throws IOException {
        Set<String> libraries = new LinkedHashSet<>();
        // WAR libraries
        extractLibraries(archive, true, libraries);
        ReadableArchive parentArchive = archive.getParentArchive();
        if (parentArchive != null && parentArchive.getExtraData(ArchiveType.class).toString().equals(EarType.ARCHIVE_TYPE)) {
            // EAR shared libraries
            extractLibraries(parentArchive.getSubArchive("lib"), false, libraries);
        }
        // Webapp shared libraries
        if(DeploymentUtils.useWarLibraries(deployment.getCurrentDeploymentContext())) {
            InstalledLibrariesResolver.getWarLibraries().forEach(warLibrary -> libraries.add(warLibrary.toString()));
        }
        return libraries;
    }

    private static void extractLibraries(Archive archive, boolean hasWebInfPrefix, Set<String> libs) {
        Enumeration<String> entries = archive != null ? archive.entries() : null;
        if (entries==null)
            return;

        while (entries.hasMoreElements()) {

            String entryName = entries.nextElement();
            if (hasWebInfPrefix && !entryName.startsWith("WEB-INF/lib")) {
                continue; // not in prefix (i.e. WEB-INF)...
            }
            if (entryName.endsWith(".jar")) {
                libs.add(entryName);
            }
        }
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
            Map<ExtensionsArchivist, RootDeploymentDescriptor> localExtensions = new HashMap<>();
            for (WebFragmentDescriptor wfDesc : wfList) {
                // if web.xml specifies metadata-complete=true,
                // all web fragment metadata-complete
                // should be overridden and be true also
                if (descriptor.isFullAttribute()) {
                    wfDesc.setFullAttribute(
                            String.valueOf(descriptor.isFullAttribute()));
                }
                if (wfDesc.isWarLibrary()) {
                    if (!DeploymentUtils.getWarLibraryCache().containsKey(wfDesc.getWarLibraryPath())) {
                        ReadableArchive warArchive = null;
                        try {
                            warArchive = archiveFactory.openArchive(new File(wfDesc.getWarLibraryPath()));
                            warArchive.setExtraData(Parser.class, archive.getExtraData(Parser.class));
                            super.readAnnotations(warArchive, wfDesc, localExtensions);
                        } finally {
                            if (warArchive != null) {
                                warArchive.close();
                            }
                        }
                        DeploymentUtils.getWarLibraryCache().putIfAbsent(wfDesc.getWarLibraryPath(),
                                new DeploymentUtils.WarLibraryDescriptor(wfDesc, filterTypesByWarLibrary(wfDesc)));
                    }
                } else {
                    super.readAnnotations(archive, wfDesc, localExtensions);
                }
            }

            // scan manifest classpath
            ModuleScanner scanner = getScanner();
            if (scanner instanceof WarScanner) {
                ((WarScanner)scanner).setScanOtherLibraries(true);
                readAnnotations(archive, descriptor, localExtensions, scanner);
            }
        }

        WebFragmentDescriptor mergedWebFragment = new WebFragmentDescriptor();
        mergedWebFragment.setExists(false);
        for (WebFragmentDescriptor wf : wfList) {
            // we have the first fragment that's contains the web-fragment.xml file
            if(!mergedWebFragment.isExists() && wf.isExists()) {
                mergedWebFragment.setExists(true);
                mergedWebFragment.setDistributable(wf.isDistributable());
            }
            mergedWebFragment.addWebBundleDescriptor(wf);
        }

        if (!wfList.isEmpty()) {
            descriptor.addWebBundleDescriptor(mergedWebFragment);

            // if there any mapping stubs left, there is something invalid referenced from web.xml
            for(WebComponentDescriptor desc : descriptor.getWebComponentDescriptors()) {
                if(desc instanceof WebComponentDescriptorStub) {
                    throw new RuntimeException(String.format("There is no web component by the name of %s here.", desc.getName()));
                }
            }
        }

        // apply default from default-web.xml to web.xml
        WebBundleDescriptorImpl defaultWebBundleDescriptor = getPlainDefaultWebXmlBundleDescriptor();
        descriptor.addDefaultWebBundleDescriptor(defaultWebBundleDescriptor);
    }

    private List<Type> filterTypesByWarLibrary(WebFragmentDescriptor wfDesc) {
        Types types = deployment.getCurrentDeploymentContext().getTransientAppMetaData(Types.class.getName(), Types.class);
        if (types == null) {
            return List.of();
        }
        return types.getAllTypes().stream().filter(key -> key.wasDefinedIn(
                List.of(Path.of(wfDesc.getWarLibraryPath()).toUri()))).collect(Collectors.toList());
    }

    /**
     * This method will return the list of web fragment in the desired order.
     */
    private List<WebFragmentDescriptor> readStandardFragments(WebBundleDescriptorImpl descriptor,
            ReadableArchive archive) throws IOException {

        List<WebFragmentDescriptor> wfList = new ArrayList<WebFragmentDescriptor>();
        for (String lib : getLibraries(archive)) {
            Archivist wfArchivist = new WebFragmentArchivist(this, habitat);
            wfArchivist.setRuntimeXMLValidation(this.getRuntimeXMLValidation());
            wfArchivist.setRuntimeXMLValidationLevel(
                    this.getRuntimeXMLValidationLevel());
            wfArchivist.setAnnotationProcessingRequested(false);

            WebFragmentDescriptor wfDesc = null;
            ReadableArchive embeddedArchive = null;
            boolean isWarLibrary = false;
            if (lib.startsWith("WEB-INF")) {
                embeddedArchive = archive.getSubArchive(lib);
            } else if (archive.getParentArchive() != null) {
                embeddedArchive = archive.getParentArchive().getSubArchive("lib").getSubArchive(lib);
            } else if (!DeploymentUtils.getWarLibraryCache().containsKey(lib) && lib.startsWith("/")
                    && lib.contains(DeploymentUtils.WAR_LIBRARIES)) {
                embeddedArchive = archiveFactory.openArchive(new File(lib));
                isWarLibrary = true;
            }
            try {
                if (embeddedArchive != null &&
                        wfArchivist.hasStandardDeploymentDescriptor(embeddedArchive)) {
                    try {
                        wfDesc = (WebFragmentDescriptor) wfArchivist.open(embeddedArchive);
                    } catch (SAXParseException ex) {
                        IOException ioex = new IOException();
                        ioex.initCause(ex);
                        throw ioex;
                    }
                } else if (DeploymentUtils.getWarLibraryCache().containsKey(lib)) {
                    wfDesc = (WebFragmentDescriptor) DeploymentUtils.getWarLibraryCache().get(lib).getDescriptor();
                } else {
                    wfDesc = new WebFragmentDescriptor();
                    wfDesc.setExists(false);
                }
            } finally {
                if (embeddedArchive != null) {
                    embeddedArchive.close();
                }
            }
            wfDesc.setJarName(lib.substring(lib.lastIndexOf('/') + 1));
            if (isWarLibrary) {
                if (wfDesc.getClassLoader() != null) {
                    wfDesc.setClassLoader(wfDesc.getClassLoader().getParent());
                }
                wfDesc.setWarLibraryPath(lib);
            }
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

        return wfList;
    }
}
