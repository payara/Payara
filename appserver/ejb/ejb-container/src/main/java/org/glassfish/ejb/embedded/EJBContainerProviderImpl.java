/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.embedded;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.ejb.spi.EJBContainerProvider;

import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;

import org.glassfish.deployment.common.ModuleDescriptor;
import org.glassfish.embeddable.*;

import org.glassfish.deployment.common.GenericAnnotationDetector;
import org.glassfish.deployment.common.DeploymentUtils;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import org.glassfish.ejb.deployment.io.EjbDeploymentDescriptorFile;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import com.sun.enterprise.security.EmbeddedSecurity;

import com.sun.enterprise.module.bootstrap.Which;

/**
 * GlassFish implementation of the EJBContainerProvider.
 *
 * @author Marina Vatkina
 */
public class EJBContainerProviderImpl implements EJBContainerProvider {

    private static final String GF_PROVIDER_NAME = EJBContainerProviderImpl.class.getName();

    private static final String GF_EJB_EMBEDDED_PROPERTY_START = "org.glassfish.ejb.embedded.";

    protected static final String KEEP_TEMPORARY_FILES = GF_EJB_EMBEDDED_PROPERTY_START + "keep-temporary-files";

    private static final String SKIP_CLIENT_MODULES = GF_EJB_EMBEDDED_PROPERTY_START + "skip-client-modules";
    private static final String GF_INSTALLATION_ROOT = GF_EJB_EMBEDDED_PROPERTY_START + "glassfish.installation.root";
    private static final String GF_INSTANCE_ROOT = GF_EJB_EMBEDDED_PROPERTY_START + "glassfish.instance.root";
    private static final String GF_DOMAIN_FILE = GF_EJB_EMBEDDED_PROPERTY_START + "glassfish.configuration.file";
    private static final String GF_INSTANCE_REUSE = GF_EJB_EMBEDDED_PROPERTY_START + "glassfish.instance.reuse";
    private static final String GF_WEB_HTTP_PORT = GF_EJB_EMBEDDED_PROPERTY_START + "glassfish.web.http.port";

    private static final String WEAVING = "org.glassfish.persistence.embedded.weaving.enabled";

    private static final Attributes.Name ATTRIBUTE_NAME_SKIP = new Attributes.Name("Bundle-SymbolicName");
    private static final String[] KNOWN_PACKAGES = 
            {"org.glassfish.", "com.sun.enterprise.", "org.eclipse.", "org.jboss.weld."};
    private static final String[] ATTRIBUTE_VALUES_OK = {"sample", "test"};


    // Use Bundle from another package
    private static final Logger _logger = 
            LogDomains.getLogger(EJBContainerProviderImpl.class, LogDomains.EJB_LOGGER);
    private static final StringManager localStrings = 
            StringManager.getManager(EJBContainerProviderImpl.class);

    private static final Object lock = new Object();

    private static EJBContainerImpl container;
    private static GlassFishRuntime runtime;
    private static ArchiveFactory archiveFactory;
    private static Class[] ejbAnnotations = null;

    public EJBContainerProviderImpl() {}

    public EJBContainer createEJBContainer(Map<?, ?> properties) throws EJBException {
        if (properties == null || properties.get(EJBContainer.PROVIDER) == null || 
                properties.get(EJBContainer.PROVIDER).equals(GF_PROVIDER_NAME)) {

            if (container != null && container.isOpen()) {
                throw new EJBException(localStrings.getString(
                        "ejb.embedded.exception_exists_container"));
            }

            boolean ok = false;
            Locations l = getLocations(properties);
            try {
                createContainer(properties, l);
                Set<DeploymentElement> modules = addModules(properties, l);
                if (!DeploymentElement.hasEJBModule(modules)) {
                    _logger.log(Level.SEVERE, "ejb.embedded.no_modules_found");
                } else {
                    container.deploy(properties, modules);
                }
                ok = true;
                return container;
            } catch (EJBException e) {
                throw e;
            } catch (Throwable t) {
                _logger.log(Level.SEVERE, "ejb.embedded.exception_instantiating", t);
                throw new EJBException(t.getMessage());
            } finally {
                if (!ok && container != null) {
                    try {
                        _logger.info("[EJBContainerProviderImpl] Cleaning up on failure ...");
                        container.close();
                    } catch (Throwable t1) {
                        _logger.info("[EJBContainerProviderImpl] Error cleaning up..." + t1);
                    }
                    container = null;
                }
            }
        }

        return null; // not this provider
    }

    private Locations createContainer(Map<?, ?> properties, Locations l) throws EJBException {
        synchronized(lock) {
            // if (container == null || !container.isOpen()) {
            try {
                if (runtime != null) {
                    runtime.shutdown(); // dispose of the old one
                }

                BootstrapProperties bootstrapProperties = new BootstrapProperties();

                // Propagate non EJB embeddable container properties into GlassFishProperties
                Properties newProps = new Properties();
                if (properties != null) {
                    copyUserProperties(properties, newProps);
                }

                // Disable weaving if it is not spesified
                if (newProps.getProperty(WEAVING) == null) {
                    newProps.setProperty(WEAVING, "false");
                }

                GlassFishProperties glassFishProperties = new GlassFishProperties(newProps);
                if (Boolean.getBoolean(KEEP_TEMPORARY_FILES)) {
                    glassFishProperties.setProperty("org.glassfish.embeddable.autoDelete", "false"); // set autodelete to false.
                    glassFishProperties.setConfigFileReadOnly(false); // make sure the domain.xml is written back. 
                }

                if (l.installed_root != null && l.instance_root != null) {
                    // Real install
                    _logger.info("[EJBContainerProviderImpl] Using installation location " + l.installed_root.getCanonicalPath());
                    bootstrapProperties.setInstallRoot(l.installed_root.getCanonicalPath());
                }
                if (l.instance_root != null && l.reuse_instance_location) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("[EJBContainerProviderImpl] Reusing instance location at: " + l.instance_root);
                    }
                    _logger.info("[EJBContainerProviderImpl] Using instance location: " + l.instance_root.getCanonicalPath());
                    glassFishProperties.setInstanceRoot(l.instance_root.getCanonicalPath());
                } else if (l.domain_file != null) {
                    _logger.info("[EJBContainerProviderImpl] Using config file location: " + l.domain_file.toURI().toString());
                    glassFishProperties.setConfigFileURI(l.domain_file.toURI().toString());
                }
                addWebContainerIfRequested(properties, glassFishProperties);

                runtime = GlassFishRuntime.bootstrap(bootstrapProperties);
                _logger.info("[EJBContainerProviderImpl] Using runtime class: " + runtime.getClass());
                GlassFish server = runtime.newGlassFish(glassFishProperties);
                if (l.instance_root != null && !l.reuse_instance_location) {
                    // XXX Start the server to get the services
                    server.start();
                    EmbeddedSecurity es = server.getService(EmbeddedSecurity.class);
                    ServiceLocator habitat = server.getService(ServiceLocator.class);

                    server.stop();

                    // If we are running from an existing install, copy over security files to the temp instance
                    if (es != null) {
                        es.copyConfigFiles(habitat, l.instance_root, l.domain_file);
                    }
                }

                // server is started in EJBContainerImpl constructor
                container = new EJBContainerImpl(server);

                validateInstanceDirectory();

                archiveFactory = server.getService(ArchiveFactory.class);

                Sniffer sniffer = server.getService(Sniffer.class, "Ejb");
                ejbAnnotations = sniffer.getAnnotationTypes();
            } catch (Exception e) {
                try {
                    if (container != null) {
                        container.stop();
                    }
                } catch (Exception e0) {
                    _logger.log(Level.SEVERE, e0.getMessage(), e0);
                }
                container = null;
                throw new EJBException(e);
            }
            // }
        }

        return l;
    }

    /**
     * Adds EJB modules for the property in the properties Map or if such property
     * is not specified, from the System classpath. Also adds library references.
     */
    private Set<DeploymentElement> addModules(Map<?, ?> properties, Locations l) {
        Set<DeploymentElement> modules = new HashSet<DeploymentElement>();
        Object obj = (properties == null)? null : properties.get(EJBContainer.MODULES);
        boolean skip_module_with_main_class = getBooleanProperty(properties, SKIP_CLIENT_MODULES);
        Map<String, Boolean> moduleNames = new HashMap<String, Boolean>();

        // Check EJBContainer.MODULES setting first - it can have an explicit set of files
        if (obj != null) {
            // Check module names first
            if (obj instanceof String) {
                moduleNames.put((String)obj, false);
            } else if (obj instanceof String[]) {
                String[] arr = (String[])obj;
                for (String s : arr) {
                    moduleNames.put(s, false);
                }
            } else if (obj instanceof File) {
                addModule(l, modules, moduleNames, (File)obj);
            } else if (obj instanceof File[]) {
                File[] arr = (File[])obj;
                for (File f : arr) {
                    addModule(l, modules, moduleNames, f);
                }
            } 
        } 

        if (modules.isEmpty()) {
            // No file is specified - load from the classpath
            String path = System.getProperty("java.class.path");
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("[EJBContainerProviderImpl] Looking for EJB modules in classpath: " + path);
            }
            String[] entries = path.split(File.pathSeparator);
            for (String s0 : entries) {
                addModule(l, modules, moduleNames, new File(s0), skip_module_with_main_class);
            }

            if (!moduleNames.isEmpty()) {
                StringBuffer sb = new StringBuffer();
                for (Map.Entry<String, Boolean> entry : moduleNames.entrySet()) {
                    if (!entry.getValue()) {
                        sb.append(entry.getKey()).append(", ");
                    }
                }
                int ln = sb.length();
                if (ln > 0) {
                    // Errors found. Trim the constructed string
                    throw new EJBException("Modules: [" + sb.substring(0, ln-2) + "] do not match an entry in the classpath");
                }
            }
        }

        return modules;
    }

    /**
     * @returns DeploymentElement if this file represents an EJB module or a library.
     * Returns null if it's an EJB module which name is not present in the list of requested
     * module names.
     */
    private DeploymentElement getRequestedEJBModuleOrLibrary(File file, Map<String, Boolean> moduleNames) 
            throws Exception {
        DeploymentElement result = null;
        String fileName = file.getName();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("... Testing ... " + fileName);
        }
        ReadableArchive archive = null;
        InputStream is = null;
        try {
            boolean isEJBModule = false;
            String moduleName = DeploymentUtils.getDefaultEEName(fileName);
            archive = archiveFactory.openArchive(file);
            is = getDeploymentDescriptor(archive);
            if (is != null) {
                isEJBModule = true;
                EjbDeploymentDescriptorFile eddf =
                        new EjbDeploymentDescriptorFile();
                eddf.setXMLValidation(false);
                EjbBundleDescriptor bundleDesc =  (EjbBundleDescriptor) eddf.read(is);
                ModuleDescriptor moduleDesc = bundleDesc.getModuleDescriptor();
                moduleDesc.setArchiveUri(fileName);
                moduleName = moduleDesc.getModuleName();
            } else {
                GenericAnnotationDetector detector =
                    new GenericAnnotationDetector(ejbAnnotations);
                isEJBModule = detector.hasAnnotationInArchive(archive);
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("... is EJB module: " + isEJBModule);
                if (isEJBModule) {
                    _logger.fine("... is Requested EJB module [" + moduleName + "]: " 
                            + (moduleNames.isEmpty() || moduleNames.containsKey(moduleName)));
                }
            }

            if (!isEJBModule || moduleNames.isEmpty()) {
                result = new DeploymentElement(file, isEJBModule, moduleName);
            } else if (moduleNames.containsKey(moduleName) && !moduleNames.get(moduleName)) {
                // Is a requested EJB module and was not found already
                result = new DeploymentElement(file, isEJBModule, moduleName);
                moduleNames.put(moduleName, true);
            }

            return result;
        } finally {
            if (archive != null) 
                archive.close();
            if (is != null) 
                is.close();
        }
    }

    /**
     * Adds an a DeploymentElement to the Set of modules if it represents an EJB module or a library.
     */
    private void addModule(Locations l, Set<DeploymentElement> modules, Map<String, Boolean> moduleNames, File f) {
        addModule(l, modules, moduleNames, f, false);
    }

    /**
     * Adds an a DeploymentElement to the Set of modules if it represents an EJB module or a library.
     * If skip_module_with_main_class is true, ignore the module that contains Main-Class attribute
     * in its manifest file
     */
    private void addModule(Locations l, Set<DeploymentElement> modules, Map<String, Boolean> moduleNames, File f,
            boolean skip_module_with_main_class) {
        try {
            if (f.exists() && !skipJar(f, l, skip_module_with_main_class)) {
                
                DeploymentElement de = getRequestedEJBModuleOrLibrary(f, moduleNames);
                if (de != null) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.fine("... Added " + ((de.isEJBModule())? "EJB Module" : "library") + 
                                " .... " + de.getElement().getName());
                    }
                    modules.add(de);
                }
            } 
        } catch (Exception ioe) {
            _logger.log(Level.FINE, "ejb.embedded.io_exception", ioe);
            // skip it
        }
    }

    /**
     * @returns true if this jar is either a GlassFish module jar or one
     * of the other known implementation modules.
     */
    private boolean skipJar(File file, Locations l, boolean skip_module_with_main_class) throws Exception {
        if (file.isDirectory() ) {
            if (!skip_module_with_main_class) {
                // Nothing to check
                return false;
            }
            File m_file = new File(file, "META-INF/MANIFEST.MF");
            if (!m_file.exists()) {
                return false;
            }
            InputStream is = null;
            try {
                is = new FileInputStream(m_file);
                if(containsMainClass(new Manifest(is))) {
                    // Ignore dirs with a Manifest file with a Main-Class attribute
                    _logger.info("... skipping entry with a Manifest file with a Main-Class attribute: " + file.getName());
                    return true;
                } else {
                    return false;
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        _logger.log(Level.FINE, "Exception while closing Manifest file under "
                                + file + ": ", ex);
                    }
                }
            }
        }

        // Skip jars in the install modules directory
        if (l.modules_dir != null && 
                l.modules_dir.equals(file.getAbsoluteFile().getParentFile().getAbsolutePath())) {
            _logger.info("... skipping module: " + file.getName());
            return true;
        }

        JarFile jf = null;
        try {
            jf = new JarFile(file);
            Manifest m = jf.getManifest();
            if (m != null) {
                if (skip_module_with_main_class && containsMainClass(m)) {
                    // Ignore jars with a Main-Class attribute
                    _logger.info("... skipping entry with a Manifest file with a Main-Class attribute: " + file.getName());
                    return true;
                }

                java.util.jar.Attributes attributes = m.getMainAttributes();
                String value = attributes.getValue(ATTRIBUTE_NAME_SKIP);
                if (value != null) {
                    for (String skipValue : KNOWN_PACKAGES) {
                        if (value.startsWith(skipValue)) {
                            for (String okValue : ATTRIBUTE_VALUES_OK) {
                                // value starts with one of the KNOWN_PACKAGES but contains an okValue further down
                                if (value.indexOf(okValue) > 0) {
                                    // Still OK
                                    return false;
                                }
                            }
                            // Not OK - skip it
                            _logger.info("... skipping entry with a Manifest file with a special attribute: " + file.getName());
                            return true;
                        }
                    }
                }
            }
        } finally {
            if (jf != null) {
                try {
                    jf.close();
                } catch (IOException ex) {
                    _logger.log(Level.FINE, "Exception while closing JarFile "
                            + jf.getName() + ": ", ex);
                }
            }
        }

        return false;
    }

    /**
     * Create a File object from the location and report an error
     * if such file does not exist.
     * Returns null if such file does not exist.
     */
    private File getValidFile(String location, String msg_key) {
        File f = new File(location);
        if (!f.exists()) {
            _logger.log(Level.WARNING, msg_key, location);
            f = null;
        }
        return f;
    }

    /**
     * Returns true if the Manifest file contains Main-Class attribute
     */
    private boolean containsMainClass(Manifest m) {
        if (m != null) {
            java.util.jar.Attributes attributes = m.getMainAttributes();
            String value = attributes.getValue(Attributes.Name.MAIN_CLASS);
            return (value != null && value.length() > 0);
        } 

        return false;
    }


    /**
     * Create File objects corresponding to instance root and domain.xml location.
     */
    private Locations getLocations(Map<?, ?> properties) throws EJBException {
        String installed_root_location = null;
        String instance_root_location = null;
        String domain_file_location = null;
        File installed_root = null;
        File instance_root = null;
        File domain_file = null;
        boolean reuse_instance_location = false;

        if (properties != null) {
            // Check if anything is set
            installed_root_location = (String) properties.get(GF_INSTALLATION_ROOT);
            instance_root_location = (String) properties.get(GF_INSTANCE_ROOT);
            domain_file_location = (String) properties.get(GF_DOMAIN_FILE);
            reuse_instance_location = getBooleanProperty(properties, GF_INSTANCE_REUSE);
        }

        if (installed_root_location == null) {
            // Try to calculate installation location relative to 
            // the jar that contains this class
            try {
                installed_root_location = Which.jarFile(getClass()).
                        getParentFile().getParentFile().getAbsolutePath();
            } catch (Exception e) {
                _logger.log(Level.SEVERE, "ejb.embedded.cannot_determine_installation_location");
                _logger.log(Level.FINE, e.getMessage(), e);
            }
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("[EJBContainerProviderImpl] installed_root_location : " + installed_root_location);
        }
        if (installed_root_location != null) {
            installed_root = getValidFile(installed_root_location, "ejb.embedded.installation_location_not_exists");
            if (installed_root != null) {
                if (instance_root_location == null) {
                    // Calculate location for the domain relative to GF install
                    instance_root_location = installed_root_location 
                            + File.separatorChar + "domains" 
                            + File.separatorChar + "domain1";
                }

                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("[EJBContainerProviderImpl] instance_root_location: " + instance_root_location);
                }
                instance_root = getValidFile(instance_root_location, "ejb.embedded.instance_location_not_exists");
            }
        }
        if (instance_root != null && domain_file_location == null) {
            // Calculate location for the domain.xml relative to GF instance
            domain_file_location = instance_root_location
                    + File.separatorChar + "config" 
                    + File.separatorChar + "domain.xml";
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("[EJBContainerProviderImpl] domain_file_location : " + domain_file_location);
        }

        if (domain_file_location != null) {
            domain_file = getValidFile(domain_file_location, "ejb.embedded.configuration_file_location_not_exists");
            if (domain_file != null) {
                if (!reuse_instance_location) {
                    File temp_domain_file = null;
                    try {
                        DomainXmlTransformer dxf = new DomainXmlTransformer(domain_file, _logger);
                        boolean keep_ports = (properties == null)? false : ((properties.get(GF_WEB_HTTP_PORT) == null)? false : true);
                        temp_domain_file = dxf.transform(keep_ports);
                    } catch (Exception e) {
                        throw new EJBException(localStrings.getString(
                                "ejb.embedded.exception_creating_temporary_domain_xml_file"), e);
                    }

                    if (temp_domain_file != null) {
                        domain_file = temp_domain_file;
                    } else {
                        throw new EJBException(localStrings.getString(
                                "ejb.embedded.failed_create_temporary_domain_xml_file"));
                    }
                }
            }
        }
        return new Locations(installed_root, instance_root, domain_file, reuse_instance_location);
    }

    private InputStream getDeploymentDescriptor(ReadableArchive archive) throws IOException {
        InputStream dd = archive.getEntry("META-INF/ejb-jar.xml");
        if (dd == null) {
            // Try EJB in a .war file as well
            dd = archive.getEntry("WEB-INF/ejb-jar.xml");
        }
        return dd;
    }

    private void addWebContainerIfRequested(Map<?, ?> properties, GlassFishProperties props) throws EJBException {
        String http_port = (properties == null)? null : (String)properties.get(GF_WEB_HTTP_PORT);
        if (http_port != null) {

            int port = 8080;
            try {
                port = Integer.parseInt(http_port);
            } catch (NumberFormatException e) {
                System.err.println("Using port 8080");
            }
            props.setPort("http-listener-1", port);
        }
    }

    /**
     * Copy user specified properties into a Proprties object that will be used
     * to create GlassFishProperties.
     */
    private void copyUserProperties(Map<?, ?> properties, Properties props) {
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                String sk = (String) key;
                if (!sk.startsWith(GF_EJB_EMBEDDED_PROPERTY_START)) {
                    for (String prefix : KNOWN_PACKAGES) {
                        if (sk.startsWith(prefix)) {
                            Object value = entry.getValue();
                            if (value instanceof String) {
                                props.setProperty(sk, (String)value);
                            } else {
                                props.setProperty(sk, value.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns boolean value whether the original type is String or Boolean
     */
    private boolean getBooleanProperty(Map<?, ?> properties, String key) {
        boolean result = false;
        if (properties != null) {
            Object value = properties.get(key);
            if (value != null) {
                if (value instanceof String) {
                    result = Boolean.valueOf((String)value);
                } else {
                    try {
                        result = (Boolean) value;
                    } catch (Exception e) {}
                }
            }
        }

        return result;
    }

    /**
     * Verifies that instance directory exists and not empty
     */
    private void validateInstanceDirectory() {
        // Verify that the instance was created properly
        File instance_directory = new File(System.getProperty("com.sun.aas.instanceRoot"));
        if (!instance_directory.exists()) {
            throw new IllegalStateException("Unexpected ERROR: Instance directory " + instance_directory + " does not exist");
        } else if (!instance_directory.isDirectory()) {
            throw new IllegalStateException("Unexpected ERROR: Instance directory " + instance_directory + " is not a directory");
        }

        File[] files = instance_directory.listFiles();
        if (files == null || files.length == 0) {
            throw new IllegalStateException("Unexpected ERROR: Instance directory " + instance_directory + " is empty");
        }
    }

    private static class Locations {
        final File installed_root;
        final File instance_root;
        final File domain_file;
        final String modules_dir;
        final boolean reuse_instance_location;

        Locations (File installed_root, File instance_root, File domain_file, boolean reuse_instance_location) {
            this.installed_root  = installed_root;
            this.instance_root  = instance_root;
            this.domain_file  = domain_file;
            if (installed_root != null) {
                modules_dir = (new File(installed_root, "modules")).getAbsolutePath();
            } else {
                modules_dir = null;
            }
            this.reuse_instance_location  = reuse_instance_location;
        }
    }
}
