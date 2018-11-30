/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package org.glassfish.weld;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import static java.util.logging.Level.FINE;
import static org.glassfish.weld.connector.WeldUtils.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Extension;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.cdi.CDILoggerInfo;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.weld.connector.WeldUtils;
import org.glassfish.weld.connector.WeldUtils.BDAType;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.weld.services.InjectionServicesImpl;
import org.jboss.weld.injection.spi.InjectionServices;

/*
 * Represents a deployment of a CDI (Weld) application.
 */
public class DeploymentImpl implements CDI11Deployment {

    // Keep track of our BDAs for this deployment
    private List<RootBeanDeploymentArchive> rarRootBdas;
    private List<RootBeanDeploymentArchive> ejbRootBdas;
    private List<RootBeanDeploymentArchive> warRootBdas;
    private List<RootBeanDeploymentArchive> libJarRootBdas = null;

    private List<BeanDeploymentArchive> beanDeploymentArchives = new ArrayList<>();
    private DeploymentContext context;

    // A convenience Map to get BDA for a given BDA ID
    private Map<String, BeanDeploymentArchive> idToBeanDeploymentArchive = new HashMap<>();
    private SimpleServiceRegistry simpleServiceRegistry = null;

    private Logger logger = CDILoggerInfo.getLogger();

    // holds BDA's created for extensions
    private Map<ClassLoader, BeanDeploymentArchive> extensionBDAMap = new HashMap<>();

    private Iterable<Metadata<Extension>> extensions;

    private List<Metadata<Extension>> dynamicExtensions = new ArrayList<>();

    private Collection<EjbDescriptor> deployedEjbs = new LinkedList<>();
    private ArchiveFactory archiveFactory;

    private boolean earContextAppLibBdasProcessed = false;

    private String appName;
    final InjectionManager injectionManager;

    /**
     * Produce <code>BeanDeploymentArchive</code>s for this <code>Deployment</code>
     * from information from the provided <code>ReadableArchive</code>.
     */
    public DeploymentImpl(ReadableArchive archive,
                          Collection<EjbDescriptor> ejbs,
                          DeploymentContext context,
                          ArchiveFactory archiveFactory,
                          String moduleName,
                          InjectionManager injectionManager) {
        if ( logger.isLoggable( FINE ) ) {
            logger.log(FINE, CDILoggerInfo.CREATING_DEPLOYMENT_ARCHIVE, new Object[]{ archive.getName()});
        }
        this.archiveFactory = archiveFactory;
        this.context = context;
        this.injectionManager = injectionManager;

        // Collect /lib Jar BDAs (if any) from the parent module.
        // If we've produced BDA(s) from any /lib jars, <code>return</code> as
        // additional BDA(s) will be produced for any subarchives (war/jar).
        libJarRootBdas = scanForLibJars(archive, ejbs, context);
        if ((libJarRootBdas != null) && !libJarRootBdas.isEmpty()) {
            return;
        }

        ApplicationHolder holder = context.getModuleMetaData(ApplicationHolder.class);
        if ((holder != null) && (holder.app != null)) {
            this.appName = holder.app.getAppName();
        } else if(moduleName != null) {
            this.appName = moduleName;
        } else {
            this.appName = "CDIApp";
        }

        createModuleBda(archive, ejbs, context, moduleName);
    }

    private void addBeanDeploymentArchives(RootBeanDeploymentArchive bda) {
        BDAType moduleBDAType = bda.getModuleBDAType();
        if (moduleBDAType.equals(BDAType.WAR)) {
            if (warRootBdas == null) {
                warRootBdas = new ArrayList<>();
            }
            warRootBdas.add(bda);
        } else if (moduleBDAType.equals(BDAType.JAR)) {
            if (ejbRootBdas == null) {
                ejbRootBdas = new ArrayList<>();
            }
            ejbRootBdas.add(bda);
        } else if (moduleBDAType.equals(BDAType.RAR)) {
            if (rarRootBdas == null) {
                rarRootBdas = new ArrayList<>();
            }
            rarRootBdas.add(bda);
        }
    }

    /**
     * Produce <code>BeanDeploymentArchive</code>s for this <code>Deployment</code>
     * from information from the provided <code>ReadableArchive</code>.
     * This method is called for subsequent modules after This <code>Deployment</code> has
     * been created.
     */
    public void scanArchive(ReadableArchive archive, Collection<EjbDescriptor> ejbs, DeploymentContext context, String moduleName) {
        if (libJarRootBdas == null) {
            libJarRootBdas = scanForLibJars(archive, ejbs, context);
            if ((libJarRootBdas != null) && !libJarRootBdas.isEmpty()) {
                return;
            }
        }

        this.context = context;
        createModuleBda(archive, ejbs, context, moduleName);
    }

    /**
     * Build the accessibility relationship between <code>BeanDeploymentArchive</code>s
     * for this <code>Deployment</code>.  This method must be called after all <code>Weld</code>
     * <code>BeanDeploymentArchive</code>s have been produced for the
     * <code>Deployment</code>.
     */
    public void buildDeploymentGraph() {
        // Make jars accessible to each other - Example:
        //    /ejb1.jar <----> /ejb2.jar
        // If there are any application (/lib) jars, make them accessible

        if (ejbRootBdas != null) {
            for (RootBeanDeploymentArchive ejbRootBda : ejbRootBdas) {
                BeanDeploymentArchive ejbModuleBda = ejbRootBda.getModuleBda();

                boolean modifiedArchive = false;
                for (RootBeanDeploymentArchive otherEjbRootBda : ejbRootBdas) {
                    BeanDeploymentArchive otherEjbModuleBda = otherEjbRootBda.getModuleBda();
                    if (otherEjbModuleBda.getId().equals(ejbModuleBda.getId())) {
                        continue;
                    }
                    ejbRootBda.getBeanDeploymentArchives().add(otherEjbRootBda);
                    ejbRootBda.getBeanDeploymentArchives().add(otherEjbModuleBda);
                    ejbModuleBda.getBeanDeploymentArchives().add(otherEjbModuleBda);
                    modifiedArchive = true;
                }

                // Make /lib jars accessible to the ejbs.
                if (libJarRootBdas != null) {
                    for (RootBeanDeploymentArchive libJarRootBda : libJarRootBdas) {
                        BeanDeploymentArchive libJarModuleBda = libJarRootBda.getModuleBda();
                        ejbRootBda.getBeanDeploymentArchives().add(libJarRootBda);
                        ejbRootBda.getBeanDeploymentArchives().add(libJarModuleBda);
                        ejbModuleBda.getBeanDeploymentArchives().add(libJarRootBda);
                        ejbModuleBda.getBeanDeploymentArchives().add(libJarModuleBda);
                        modifiedArchive = true;
                    }
                }

                // Make rars accessible to ejbs
                if (rarRootBdas != null) {
                    for (RootBeanDeploymentArchive rarRootBda : rarRootBdas) {
                        BeanDeploymentArchive rarModuleBda = rarRootBda.getModuleBda();
                        ejbRootBda.getBeanDeploymentArchives().add(rarRootBda);
                        ejbRootBda.getBeanDeploymentArchives().add(rarModuleBda);
                        ejbModuleBda.getBeanDeploymentArchives().add(rarRootBda);
                        ejbModuleBda.getBeanDeploymentArchives().add(rarModuleBda);
                        modifiedArchive = true;
                    }
                }

                if (modifiedArchive) {
                    int idx = getBeanDeploymentArchives().indexOf(ejbModuleBda);
                    if (idx >= 0) {
                        getBeanDeploymentArchives().remove(idx);
                        getBeanDeploymentArchives().add(ejbModuleBda);
                    }
                }
            }
        }

        // Make jars (external to WAR modules) accessible to WAR BDAs - Example:
        //    /web.war ----> /ejb.jar
        // If there are any application (/lib) jars, make them accessible

        if (warRootBdas != null) {
            ListIterator<RootBeanDeploymentArchive> warIter = warRootBdas.listIterator();
            boolean modifiedArchive = false;
            while (warIter.hasNext()) {
                RootBeanDeploymentArchive warRootBda = warIter.next();
                BeanDeploymentArchive warModuleBda = warRootBda.getModuleBda();
                if (ejbRootBdas != null) {
                    for (RootBeanDeploymentArchive ejbRootBda : ejbRootBdas) {
                        BeanDeploymentArchive ejbModuleBda = ejbRootBda.getModuleBda();
                        warRootBda.getBeanDeploymentArchives().add(ejbRootBda);
                        warRootBda.getBeanDeploymentArchives().add(ejbModuleBda);
                        warModuleBda.getBeanDeploymentArchives().add(ejbRootBda);
                        warModuleBda.getBeanDeploymentArchives().add(ejbModuleBda);

                       for ( BeanDeploymentArchive oneBda : warModuleBda.getBeanDeploymentArchives() ) {
                         oneBda.getBeanDeploymentArchives().add( ejbRootBda );
                         oneBda.getBeanDeploymentArchives().add( ejbModuleBda );
                       }

                      modifiedArchive = true;
                    }
                }

                // Make /lib jars accessible to the war and it's sub bdas
                if (libJarRootBdas != null) {
                    for (RootBeanDeploymentArchive libJarRootBda : libJarRootBdas) {
                        BeanDeploymentArchive libJarModuleBda = libJarRootBda.getModuleBda();
                        warRootBda.getBeanDeploymentArchives().add(libJarRootBda);
                        warRootBda.getBeanDeploymentArchives().add(libJarModuleBda);
                        warModuleBda.getBeanDeploymentArchives().add(libJarRootBda);
                        warModuleBda.getBeanDeploymentArchives().add(libJarModuleBda);

                        for ( BeanDeploymentArchive oneBda : warModuleBda.getBeanDeploymentArchives() ) {
                          oneBda.getBeanDeploymentArchives().add( libJarRootBda );
                          oneBda.getBeanDeploymentArchives().add( libJarModuleBda );
                        }

                        modifiedArchive = true;
                    }
                }

                // Make rars accessible to wars and it's sub bdas
                if (rarRootBdas != null) {
                    for (RootBeanDeploymentArchive rarRootBda : rarRootBdas) {
                        BeanDeploymentArchive rarModuleBda = rarRootBda.getModuleBda();
                        warRootBda.getBeanDeploymentArchives().add(rarRootBda);
                        warRootBda.getBeanDeploymentArchives().add(rarModuleBda);
                        warModuleBda.getBeanDeploymentArchives().add(rarRootBda);
                        warModuleBda.getBeanDeploymentArchives().add(rarModuleBda);

                      for ( BeanDeploymentArchive oneBda : warModuleBda.getBeanDeploymentArchives() ) {
                        oneBda.getBeanDeploymentArchives().add( rarRootBda );
                        oneBda.getBeanDeploymentArchives().add( rarModuleBda );
                      }

                      modifiedArchive = true;
                    }
                }

                if (modifiedArchive) {
                    int idx = getBeanDeploymentArchives().indexOf(warModuleBda);
                    if (idx >= 0) {
                        getBeanDeploymentArchives().remove(idx);
                        getBeanDeploymentArchives().add(warModuleBda);
                    }
                    modifiedArchive = false;
                }
            }
        }

        addDependentBdas();
    }

    private void addDependentBdas() {
        Set<BeanDeploymentArchive> additionalBdas = new HashSet<>();
        for ( BeanDeploymentArchive oneBda : beanDeploymentArchives ) {
            BeanDeploymentArchiveImpl beanDeploymentArchiveImpl = ( BeanDeploymentArchiveImpl ) oneBda;
            Collection<BeanDeploymentArchive> subBdas = beanDeploymentArchiveImpl.getBeanDeploymentArchives();
            for (BeanDeploymentArchive subBda : subBdas) {
                if ( !subBda.getBeanClasses().isEmpty() ) {
                    // only add it if it's cdi-enabled (contains at least one bean that is managed by cdi)
                    additionalBdas.add(subBda);
                }
            }
        }

        for ( BeanDeploymentArchive oneBda : additionalBdas ) {
            if ( ! beanDeploymentArchives.contains( oneBda ) ) {
                beanDeploymentArchives.add( oneBda );
            }
        }
    }

    @Override
    public List<BeanDeploymentArchive> getBeanDeploymentArchives() {
        if ( logger.isLoggable( FINE ) ) {
            logger.log(FINE, CDILoggerInfo.GET_BEAN_DEPLOYMENT_ARCHIVES, new Object[] {beanDeploymentArchives});
        }
        return beanDeploymentArchives;
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> beanClass) {
        if ( logger.isLoggable( FINE ) ) {
            logger.log(FINE, CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE, new Object[] { beanClass });
        }
        List<BeanDeploymentArchive> beanDeploymentArchives = getBeanDeploymentArchives();

        ListIterator<BeanDeploymentArchive> lIter = beanDeploymentArchives.listIterator();
        while (lIter.hasNext()) {
            BeanDeploymentArchive bda = lIter.next();
            if ( logger.isLoggable( FINE ) ) {
                logger.log(FINE,
                           CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_CHECKING,
                           new Object[] { beanClass, bda.getId() });
            }
            if (((BeanDeploymentArchiveImpl)bda).getModuleBeanClasses().contains(beanClass.getName())) {
                //don't stuff this Bean Class into the BDA's beanClasses,
                //as Weld automatically add theses classes to the BDA's bean Classes
                if ( logger.isLoggable( FINE ) ) {
                    logger.log(FINE,
                               CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_ADD_TO_EXISTING,
                               new Object[] {beanClass.getName(), bda });
                }
                return bda;
            }

            //XXX: As of now, we handle one-level. Ideally, a bean deployment
            //descriptor is a composite and we should be able to search the tree
            //and get the right BDA for the beanClass
            if (!bda.getBeanDeploymentArchives().isEmpty()) {
                for(BeanDeploymentArchive subBda: bda.getBeanDeploymentArchives()){
                    Collection<String> moduleBeanClassNames = ((BeanDeploymentArchiveImpl)subBda).getModuleBeanClasses();
                    if ( logger.isLoggable( FINE ) ) {
                        logger.log(FINE,
                                   CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_CHECKING_SUBBDA,
                                   new Object[] {beanClass, subBda.getId()});
                    }
                    boolean match = moduleBeanClassNames.contains(beanClass.getName());
                    if (match) {
                        //don't stuff this Bean Class into the BDA's beanClasses,
                        //as Weld automatically add theses classes to the BDA's bean Classes
                        if ( logger.isLoggable( FINE ) ) {
                            logger.log(FINE,
                                       CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_ADD_TO_EXISTING,
                                       new Object[]{ beanClass.getName(), subBda});
                        }
                        return subBda;
                    }
                }
            }
        }

        BeanDeploymentArchive extensionBDA = extensionBDAMap.get(beanClass.getClassLoader());
        if ( extensionBDA != null ) {
            return extensionBDA;
        }

        // If the BDA was not found for the Class, create one and add it
        if ( logger.isLoggable( FINE ) ) {
            logger.log(FINE, CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_CREATE_NEW_BDA, new Object []{beanClass});
        }
        List<Class<?>> beanClasses = new ArrayList<>();
        List<URL> beanXMLUrls = new CopyOnWriteArrayList<>();
        Set<EjbDescriptor> ejbs = new HashSet<>();
        beanClasses.add(beanClass);
        BeanDeploymentArchive newBda =
            new BeanDeploymentArchiveImpl(beanClass.getName(),
                                          beanClasses, beanXMLUrls, ejbs, context);
        // have to create new InjectionServicesImpl for each new BDA so injection context is propagated for the correct bundle
        newBda.getServices().add(InjectionServices.class, new InjectionServicesImpl(injectionManager, DOLUtils.getCurrentBundleForContext(context), this));
        BeansXml beansXml = newBda.getBeansXml();
        if (beansXml == null || !beansXml.getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
            if ( logger.isLoggable( FINE ) ) {
                logger.log(FINE,
                           CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_ADD_NEW_BDA_TO_ROOTS,
                           new Object[] {} );
            }
            lIter = beanDeploymentArchives.listIterator();
            while (lIter.hasNext()) {
                BeanDeploymentArchive bda = lIter.next();
                bda.getBeanDeploymentArchives().add(newBda);
            }
            if ( logger.isLoggable( FINE ) ) {
                logger.log(FINE,
                           CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_RETURNING_NEWLY_CREATED_BDA,
                           new Object[]{beanClass, newBda});
            }
            beanDeploymentArchives.add(newBda);
            idToBeanDeploymentArchive.put(newBda.getId(), newBda);
            extensionBDAMap.put( beanClass.getClassLoader(), newBda);
            return newBda;
        }

        return null;
    }

    @Override
    public ServiceRegistry getServices() {
        if (simpleServiceRegistry == null) {
            simpleServiceRegistry = new SimpleServiceRegistry();
        }
        return simpleServiceRegistry;
    }

    @Override
    public Iterable<Metadata<Extension>> getExtensions() {
        if (extensions != null) {
            return extensions;
        }

        List<BeanDeploymentArchive> bdas = getBeanDeploymentArchives();
        ArrayList<Metadata<Extension>> extnList = new ArrayList<>();
        
        // Track classloaders to ensure we don't scan the same classloader twice
        HashSet<ClassLoader> scannedClassLoaders = new HashSet<>();
        
        // ensure we don't add the same extension twice
        HashMap<Class,Metadata<Extension>> loadedExtensions = new HashMap<>();
        
        for (BeanDeploymentArchive bda : bdas) {
            if (!(bda instanceof RootBeanDeploymentArchive)) {
                ClassLoader moduleClassLoader = ((BeanDeploymentArchiveImpl)bda).getModuleClassLoaderForBDA();
                if (!scannedClassLoaders.contains(moduleClassLoader)) {
                    scannedClassLoaders.add(moduleClassLoader);
                    extensions = context.getTransientAppMetaData(WeldDeployer.WELD_BOOTSTRAP, 
                                                                 WeldBootstrap.class).loadExtensions(moduleClassLoader);
                    if (extensions != null) {
                        for (Metadata<Extension> bdaExtn : extensions) {
                            if (loadedExtensions.get(bdaExtn.getValue().getClass()) == null) {
                                extnList.add(bdaExtn);
                                loadedExtensions.put(bdaExtn.getValue().getClass(), bdaExtn);
                            }
                        }
                    }
                }
            }
        }
        extnList.addAll(dynamicExtensions);
        extensions = extnList;
        return extnList;
    }

    public boolean addDynamicExtension(Metadata<Extension> extension) {
        return dynamicExtensions.add(extension);
    }

    public boolean removeDynamicExtension(Metadata<Extension> extension) {
        return dynamicExtensions.remove(extension);
    }

    public void clearDynamicExtensions() {
        dynamicExtensions.clear();
    }

    @Override
    public String toString() {
        StringBuilder valBuff = new StringBuilder();
        List<BeanDeploymentArchive> beanDeploymentArchives = getBeanDeploymentArchives();
        ListIterator<BeanDeploymentArchive> lIter = beanDeploymentArchives.listIterator();
        while (lIter.hasNext()) {
            BeanDeploymentArchive bda = lIter.next();
            valBuff.append(bda.toString());
        }
        return valBuff.toString();
    }

    public BeanDeploymentArchive getBeanDeploymentArchiveForArchive(String archiveId) {
        return idToBeanDeploymentArchive.get(archiveId);
    }

    public void cleanup() {
        if (ejbRootBdas != null) {
            ejbRootBdas.clear();
        }
        if (warRootBdas != null) {
            warRootBdas.clear();
        }
        if (libJarRootBdas != null) {
            libJarRootBdas.clear();
        }

        if ( rarRootBdas != null ) {
            rarRootBdas.clear();
        }

        if (idToBeanDeploymentArchive != null) {
            idToBeanDeploymentArchive.clear();
        }
    }


    // This method creates and returns a List of BeanDeploymentArchives for each
    // Weld enabled jar under /lib of an existing Archive.
    private List<RootBeanDeploymentArchive> scanForLibJars( ReadableArchive archive,
                                                            Collection<EjbDescriptor> ejbs,
                                                            DeploymentContext context) {
        List<ReadableArchive> libJars = null;
        ApplicationHolder holder = context.getModuleMetaData(ApplicationHolder.class);
        if ((holder != null) && (holder.app != null)) {
            String libDir = holder.app.getLibraryDirectory();
            if (libDir != null && !libDir.isEmpty()) {
                Enumeration<String> entries = archive.entries(libDir);
                while (entries.hasMoreElements()) {
                    final String entryName = entries.nextElement();

                    // if a jar is directly in lib dir and not WEB-INF/lib/foo/bar.jar
                    if (DOLUtils.isScanningAllowed(holder.app, entryName) && entryName.endsWith(JAR_SUFFIX) &&
                        entryName.indexOf(SEPARATOR_CHAR, libDir.length() + 1 ) == -1 ) {
                        try {
                            ReadableArchive jarInLib = archive.getSubArchive(entryName);
                            if (jarInLib.exists(META_INF_BEANS_XML) || WeldUtils.isImplicitBeanArchive(context, jarInLib)) {
                                if (libJars == null) {
                                    libJars = new ArrayList<>();
                                }
                                libJars.add(jarInLib);
                            }
                        } catch (IOException e) {
                            logger.log(FINE, CDILoggerInfo.EXCEPTION_SCANNING_JARS, new Object[]{e});
                        }
                    }
                }
            }
        }

        if (libJars != null) {
            String libDir = holder.app.getLibraryDirectory();
            for (ReadableArchive libJarArchive : libJars) {
                createLibJarBda(libJarArchive, ejbs, libDir);
            }
        }

        return libJarRootBdas;
    }

    private void createLibJarBda(ReadableArchive libJarArchive, Collection<EjbDescriptor> ejbs, String libDir ) {
        RootBeanDeploymentArchive rootBda =
            new RootBeanDeploymentArchive(libJarArchive,
                                          ejbs,
                                          context,
                                          libDir + SEPARATOR_CHAR + libJarArchive.getName());
        createLibJarBda(rootBda);
    }

    private void createLibJarBda(RootBeanDeploymentArchive rootLibBda) {
        BeanDeploymentArchive libModuleBda = rootLibBda.getModuleBda();
        BeansXml moduleBeansXml = libModuleBda.getBeansXml();
        if (moduleBeansXml == null || !moduleBeansXml.getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
            addBdaToDeploymentBdas(rootLibBda);
            addBdaToDeploymentBdas(libModuleBda);
            if (libJarRootBdas == null) {
                libJarRootBdas = new ArrayList<>();
            }

            for ( RootBeanDeploymentArchive existingLibJarRootBda : libJarRootBdas) {
                rootLibBda.getBeanDeploymentArchives().add( existingLibJarRootBda );
                rootLibBda.getBeanDeploymentArchives().add( existingLibJarRootBda.getModuleBda() );
                rootLibBda.getModuleBda().getBeanDeploymentArchives().add( existingLibJarRootBda );
                rootLibBda.getModuleBda().getBeanDeploymentArchives().add( existingLibJarRootBda.getModuleBda() );

                existingLibJarRootBda.getBeanDeploymentArchives().add( rootLibBda );
                existingLibJarRootBda.getBeanDeploymentArchives().add( rootLibBda.getModuleBda() );
                existingLibJarRootBda.getModuleBda().getBeanDeploymentArchives().add( rootLibBda );
                existingLibJarRootBda.getModuleBda().getBeanDeploymentArchives().add( rootLibBda.getModuleBda() );
            }

            libJarRootBdas.add(rootLibBda);
        }
    }

    private void addBdaToDeploymentBdas( BeanDeploymentArchive bda ) {
        if ( ! beanDeploymentArchives.contains( bda ) ) {
            beanDeploymentArchives.add(bda);
            idToBeanDeploymentArchive.put(bda.getId(), bda);
        }
    }

    // These are application libraries that reside outside of the ear.  They are usually specified by entries
    // in the manifest.
    // to test this put a jar in domains/domain1/lib/applibs and in its manifest make sure it has something like:
    //                           Extension-Name: com.acme.extlib
    // In a war's manifest put in something like:
    //                           Extension-List: MyExtLib
    //                           MyExtLib-Extension-Name: com.acme.extlib
    private void processBdasForAppLibs( ReadableArchive archive, DeploymentContext context ) {
        List<RootBeanDeploymentArchive> libBdas = new ArrayList<>();
        try {
            // each appLib in context.getAppLibs is a URI of the form "file:/glassfish/runtime/trunk/glassfish4/glassfish/domains/domain1/lib/applibs/mylib.jar"
            List<URI> appLibs = context.getAppLibs();

            Set<String> installedLibraries = InstalledLibrariesResolver.getInstalledLibraries(archive);
            if ( appLibs != null && !appLibs.isEmpty() && installedLibraries != null && !installedLibraries.isEmpty() ) {
                for ( URI oneAppLib : appLibs ) {
                    for ( String oneInstalledLibrary : installedLibraries ) {
                        if ( oneAppLib.getPath().endsWith( oneInstalledLibrary ) ) {
                            ReadableArchive libArchive = null;
                            try {
                                libArchive = archiveFactory.openArchive(oneAppLib);
                                if ( libArchive.exists( WeldUtils.META_INF_BEANS_XML ) ) {
                                    String bdaId = archive.getName() + "_" + libArchive.getName();
                                    RootBeanDeploymentArchive rootBda =
                                        new RootBeanDeploymentArchive(libArchive,
                                                                      Collections.<EjbDescriptor>emptyList(),
                                                                      context,
                                                                      bdaId );
                                    libBdas.add(rootBda);
                                }
                            } finally {
                                if ( libArchive != null ) {
                                    try {
                                        libArchive.close();
                                    } catch ( Exception ignore ) {}
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (URISyntaxException | IOException e) {
            //todo: log error
        }

        for ( RootBeanDeploymentArchive oneBda : libBdas ) {
            createLibJarBda(oneBda );
        }
    }

    protected void addDeployedEjbs( Collection<EjbDescriptor> ejbs ) {
        if ( ejbs != null ) {
            deployedEjbs.addAll( ejbs );
        }
    }

    public Collection<EjbDescriptor> getDeployedEjbs() {
        return deployedEjbs;
    }

    /**
     * Get a bda for the specified beanClass
     *
     * @param beanClass The beanClass to get the bda for.
     *
     * @return If the beanClass is in the archive represented by the bda
     * then return that bda.  Otherwise if the class loader of the beanClass matches the module class loader
     * of any of the root bdas then return that root bda.  Otherwise return null.
     */
    public BeanDeploymentArchive getBeanDeploymentArchive(Class<?> beanClass) {
        if ( beanClass == null ) {
            return null;
        }

        for ( BeanDeploymentArchive oneBda : beanDeploymentArchives ) {
            BeanDeploymentArchiveImpl beanDeploymentArchiveImpl = ( BeanDeploymentArchiveImpl ) oneBda;
            if ( beanDeploymentArchiveImpl.getBeanClassObjects().contains( beanClass ) ) {
                return oneBda;
            }
        }

        // find a root bda
        ClassLoader classLoader = beanClass.getClassLoader();

        RootBeanDeploymentArchive rootBda = findRootBda( classLoader, ejbRootBdas );
        if ( rootBda == null ) {
            rootBda = findRootBda( classLoader, warRootBdas );
            if ( rootBda == null ) {
                rootBda = findRootBda( classLoader, libJarRootBdas );
                if ( rootBda == null ) {
                    rootBda = findRootBda( classLoader, rarRootBdas );
                }
            }
        }

        return rootBda;
    }

    private RootBeanDeploymentArchive findRootBda( ClassLoader classLoader, List<RootBeanDeploymentArchive> rootBdas ) {
        if ( rootBdas == null || classLoader == null ) {
            return null;
        }

        for ( RootBeanDeploymentArchive oneRootBda : rootBdas ) {
            if ( classLoader.equals( oneRootBda.getModuleClassLoaderForBDA() ) ) {
                return oneRootBda;
            }
        }

        return null;
    }

    private void createModuleBda( ReadableArchive archive,
                                  Collection<EjbDescriptor> ejbs,
                                  DeploymentContext context,
                                  String moduleName) {
        RootBeanDeploymentArchive rootBda = new RootBeanDeploymentArchive(archive, ejbs, context, moduleName);

            BeanDeploymentArchive moduleBda = rootBda.getModuleBda();
            BeansXml moduleBeansXml = moduleBda.getBeansXml();
            if (moduleBeansXml == null || !moduleBeansXml.getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
                addBdaToDeploymentBdas(rootBda);
                addBdaToDeploymentBdas(moduleBda);
                addBeanDeploymentArchives(rootBda);
            }

            // first check if the parent is an ear and if so see if there are app libs defined there.
            if ( ! earContextAppLibBdasProcessed && context instanceof DeploymentContextImpl ) {
                DeploymentContextImpl deploymentContext = ( DeploymentContextImpl ) context;
                DeploymentContext parentContext = deploymentContext.getParentContext();
                if ( parentContext != null ) {
                    processBdasForAppLibs( parentContext.getSource(), parentContext );
                    parentContext.getSource();
                    earContextAppLibBdasProcessed = true;
                }
            }

            // then check the module
            processBdasForAppLibs(archive, context);
    }



    public Iterator<RootBeanDeploymentArchive> getLibJarRootBdas() {
        if ( libJarRootBdas == null ) {
            return null;
        }
        return libJarRootBdas.iterator();
    }

    public Iterator<RootBeanDeploymentArchive> getRarRootBdas() {
        if ( rarRootBdas == null ) {
            return null;
        }
        return rarRootBdas.iterator();
    }

    public String getAppName() {
        return appName;
    }
}
