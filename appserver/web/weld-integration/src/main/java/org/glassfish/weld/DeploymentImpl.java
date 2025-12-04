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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates]

package org.glassfish.weld;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.glassfish.weld.connector.WeldUtils.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.enterprise.inject.spi.Extension;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.cdi.CDILoggerInfo;
import org.glassfish.common.util.ObjectInputStreamWithLoader;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.data.ApplicationInfo;
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
import org.jboss.weld.bootstrap.spi.helpers.MetadataImpl;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.weld.services.InjectionServicesImpl;
import org.jboss.weld.injection.spi.InjectionServices;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import org.jboss.weld.lite.extension.translator.LiteExtensionTranslator;
import java.security.PrivilegedAction;
import java.util.stream.Stream;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import jakarta.enterprise.inject.build.compatible.spi.SkipIfPortableExtensionPresent;

/*
 * Represents a deployment of a CDI (Weld) application.
 */
public class DeploymentImpl implements CDI11Deployment, Serializable {
    private static final long serialVersionUID = 1L;
    static final ThreadLocal<DeploymentImpl> currentDeployment = new ThreadLocal<>();
    static final ThreadLocal<Map<Integer, BeanDeploymentArchiveImpl>> currentBDAs = new ThreadLocal<>();
    static final ThreadLocal<DeploymentContext> currentDeploymentContext = new ThreadLocal<>();

    // Keep track of our BDAs for this deployment
    private final Set<RootBeanDeploymentArchive> rarRootBdas = new LinkedHashSet<>();
    final Set<RootBeanDeploymentArchive> ejbRootBdas = new LinkedHashSet<>();
    private final Set<RootBeanDeploymentArchive> warRootBdas = new LinkedHashSet<>();
    private final Set<RootBeanDeploymentArchive> libJarRootBdas = new LinkedHashSet<>();

    private final Set<BeanDeploymentArchive> beanDeploymentArchives = new LinkedHashSet<>();
    final transient DeploymentContext context;

    // A convenience Map to get BDA for a given BDA ID
    final Map<String, BeanDeploymentArchive> idToBeanDeploymentArchive = new HashMap<>();
    private transient SimpleServiceRegistry simpleServiceRegistry = null;

    private final transient Logger logger;

    // holds BDA's created for extensions
    private final Map<ClassLoader, BeanDeploymentArchive> extensionBDAMap = new HashMap<>();

    private final List<Metadata<Extension>> extensions = new ArrayList<>();

    private final List<Metadata<Extension>> dynamicExtensions = new ArrayList<>();

    private final transient Collection<EjbDescriptor> deployedEjbs;
    private final transient ArchiveFactory archiveFactory;

    private boolean earContextAppLibBdasProcessed = false;

    private String appName;
    private String contextId;
    final transient InjectionManager injectionManager;

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
        logger = CDILoggerInfo.getLogger();
        deployedEjbs = new LinkedList<>();
        if ( logger.isLoggable( FINE ) ) {
            logger.log(FINE, CDILoggerInfo.CREATING_DEPLOYMENT_ARCHIVE, new Object[]{ archive.getName()});
        }
        this.archiveFactory = archiveFactory;
        this.context = context;
        this.injectionManager = injectionManager;
        this.contextId = moduleName != null? moduleName : archive.getName();

        // Collect /lib Jar BDAs (if any) from the parent module.
        // If we've produced BDA(s) from any /lib jars, <code>return</code> as
        // additional BDA(s) will be produced for any subarchives (war/jar).
        libJarRootBdas.addAll(scanForLibJars(archive, ejbs, context));
        if (!libJarRootBdas.isEmpty()) {
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

        createModuleBda(archive, ejbs, context, contextId);
    }

    DeploymentImpl(DeploymentImpl deployment) {
        this.rarRootBdas.addAll(deployment.rarRootBdas);
        this.ejbRootBdas.addAll(deployment.ejbRootBdas);
        this.warRootBdas.addAll(deployment.warRootBdas);
        this.libJarRootBdas.addAll(deployment.libJarRootBdas);
        this.beanDeploymentArchives.addAll(deployment.beanDeploymentArchives);
        this.appName = deployment.appName;
        this.contextId = deployment.contextId;
        this.earContextAppLibBdasProcessed = deployment.earContextAppLibBdasProcessed;

        this.context = currentDeploymentContext.get();
        this.archiveFactory = currentDeployment.get().archiveFactory;
        getServices().addAll(currentDeployment.get().getServices().entrySet());
        this.injectionManager = currentDeployment.get().injectionManager;
        this.logger = currentDeployment.get().logger;
        this.deployedEjbs = currentDeployment.get().deployedEjbs;
    }

    DeploymentImpl filter(RootBeanDeploymentArchive rootBDA, ApplicationInfo applicationInfo) {
        DeploymentImpl filteredDeployment;
        try {
            filteredDeployment = serializeAndDeserialize(this, applicationInfo.getAppClassLoader());
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        List<String> nonRooIDs = List.of(rootBDA.getId(), rootBDA.getModuleBda().getId());
        filteredDeployment.clearAndAddAll(filteredDeployment.warRootBdas, filterBDAs(filteredDeployment.warRootBdas, nonRooIDs));
        filteredDeployment.clearAndAddAll(filteredDeployment.beanDeploymentArchives,
                filterBDAs(filteredDeployment.beanDeploymentArchives, nonRooIDs, filteredDeployment.rarRootBdas,
                        filteredDeployment.ejbRootBdas, filteredDeployment.libJarRootBdas));
        filteredDeployment.contextId = rootBDA.getId() + ".bda";
        return filteredDeployment;
    }

    private <TT extends BeanDeploymentArchive> void clearAndAddAll(Set<TT> originalBdas, Set<TT> bdas) {
        originalBdas.clear();
        originalBdas.addAll(bdas);
    }

    Set<RootBeanDeploymentArchive> getRootBDAs() {
        if (!warRootBdas.isEmpty()) {
            return warRootBdas;
        } else if (!ejbRootBdas.isEmpty()) {
            return ejbRootBdas;
        } else if (!rarRootBdas.isEmpty()) {
            return rarRootBdas;
        } else if (!libJarRootBdas.isEmpty()) {
            return libJarRootBdas;
        } else {
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    private <TT> TT serializeAndDeserialize(TT original, ClassLoader classLoader)
            throws IOException, ClassNotFoundException {
        // serialize
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try (var outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            outputStream.writeObject(original);
        }

        // deserialize
        try (var inputStream = new ObjectInputStreamWithLoader(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), classLoader)) {
            return (TT) inputStream.readObject();
        }
    }

    Object readResolve() throws ObjectStreamException {
        return new DeploymentImpl(this);
    }

    @SafeVarargs
    private <TT extends BeanDeploymentArchive> Set<TT> filterBDAs(Set<TT> bdas, List<String> bda,
                                                                   Set<RootBeanDeploymentArchive>... include) {
        if (bdas == null) {
            return null;
        }
        List<RootBeanDeploymentArchive> includeRootList = Arrays.stream(include)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toList());
        List<BeanDeploymentArchive> includeList = includeRootList.stream()
                .flatMap(list -> Stream.of(list, list.getModuleBda()))
                .collect(toList());
        return bdas.stream()
                .filter(b -> bda.stream().anyMatch(b.getId()::startsWith) || includeList.contains(b))
                .collect(toSet());
    }

    private void addBeanDeploymentArchives(RootBeanDeploymentArchive bda) {
        if (bda.getModuleBDAType() != BDAType.UNKNOWN) {
            rootBDAs(bda).add(bda);
        }
    }

    private Set<RootBeanDeploymentArchive> rootBDAs(RootBeanDeploymentArchive bda) {
        BDAType moduleBDAType = bda.getModuleBDAType();
        if (moduleBDAType.equals(BDAType.WAR)) {
            return warRootBdas;
        } else if (moduleBDAType.equals(BDAType.JAR)) {
            return ejbRootBdas;
        } else if (moduleBDAType.equals(BDAType.RAR)) {
            return rarRootBdas;
        }
        throw new IllegalArgumentException("Unknown BDAType: " + moduleBDAType);
    }

    /**
     * Produce <code>BeanDeploymentArchive</code>s for this <code>Deployment</code>
     * from information from the provided <code>ReadableArchive</code>.
     * This method is called for subsequent modules after This <code>Deployment</code> has
     * been created.
     */
    public void scanArchive(ReadableArchive archive, Collection<EjbDescriptor> ejbs, DeploymentContext context, String moduleName) {
        if (libJarRootBdas.isEmpty()) {
            libJarRootBdas.addAll(scanForLibJars(archive, ejbs, context));
            if (!libJarRootBdas.isEmpty()) {
                return;
            }
        }

        createModuleBda(archive, ejbs, context, moduleName);
    }

    /**
     * Build the accessibility relationship between <code>BeanDeploymentArchive</code>s
     * for this <code>Deployment</code>.  This method must be called after all <code>Weld</code>
     * <code>BeanDeploymentArchive</code>s have been produced for the
     * <code>Deployment</code>.
     */
    public void buildDeploymentGraph() {
        Set<BeanDeploymentArchive> ejbModuleAndRarBDAs = new HashSet<>();

        // Make jars accessible to each other - Example:
        //    /ejb1.jar <----> /ejb2.jar
        // If there are any application (/lib) jars, make them accessible

        for (RootBeanDeploymentArchive ejbRootBda : ejbRootBdas) {
            BeanDeploymentArchive ejbModuleBda = ejbRootBda.getModuleBda();
            ejbModuleAndRarBDAs.add(ejbRootBda);
            ejbModuleAndRarBDAs.add(ejbModuleBda);

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
            for (RootBeanDeploymentArchive libJarRootBda : libJarRootBdas) {
                BeanDeploymentArchive libJarModuleBda = libJarRootBda.getModuleBda();
                ejbRootBda.getBeanDeploymentArchives().add(libJarRootBda);
                ejbRootBda.getBeanDeploymentArchives().add(libJarModuleBda);
                ejbModuleBda.getBeanDeploymentArchives().add(libJarRootBda);
                ejbModuleBda.getBeanDeploymentArchives().add(libJarModuleBda);
                modifiedArchive = true;
            }

            // Make rars accessible to ejbs
            for (RootBeanDeploymentArchive rarRootBda : rarRootBdas) {
                BeanDeploymentArchive rarModuleBda = rarRootBda.getModuleBda();
                ejbModuleAndRarBDAs.add(rarRootBda);
                ejbModuleAndRarBDAs.add(rarModuleBda);
                ejbRootBda.getBeanDeploymentArchives().add(rarRootBda);
                ejbRootBda.getBeanDeploymentArchives().add(rarModuleBda);
                ejbModuleBda.getBeanDeploymentArchives().add(rarRootBda);
                ejbModuleBda.getBeanDeploymentArchives().add(rarModuleBda);
                modifiedArchive = true;
            }

            if (modifiedArchive) {
                if (getBeanDeploymentArchives().remove(ejbModuleBda)) {
                    getBeanDeploymentArchives().add(ejbModuleBda);
                }
            }
        }

        // Make jars (external to WAR modules) accessible to WAR BDAs - Example:
        //    /web.war ----> /ejb.jar
        // If there are any application (/lib) jars, make them accessible

        Iterator<RootBeanDeploymentArchive> warIter = warRootBdas.iterator();
        boolean modifiedArchive = false;
        while (warIter.hasNext()) {
            RootBeanDeploymentArchive warRootBda = warIter.next();
            BeanDeploymentArchive warModuleBda = warRootBda.getModuleBda();
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

            // Make /lib jars accessible to the war and it's sub bdas
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

            // Make rars accessible to wars and it's sub bdas
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

            if (modifiedArchive) {
                if (getBeanDeploymentArchives().remove(warModuleBda)) {
                    getBeanDeploymentArchives().add(warModuleBda);
                }
                modifiedArchive = false;
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
    public Set<BeanDeploymentArchive> getBeanDeploymentArchives() {
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
        Set<BeanDeploymentArchive> beanDeploymentArchives = getBeanDeploymentArchives();

        Iterator<BeanDeploymentArchive> lIter = beanDeploymentArchives.iterator();
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
            lIter = beanDeploymentArchives.iterator();
            while (lIter.hasNext()) {
                BeanDeploymentArchive bda = lIter.next();
                bda.getBeanDeploymentArchives().add(newBda);
            }
            
            //adding available archive to the new to solve dependencies on injection time for cdi 4.1 tck
            newBda.getBeanDeploymentArchives().addAll(beanDeploymentArchives);
            
            if ( logger.isLoggable( FINE ) ) {
                logger.log(FINE,
                           CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_RETURNING_NEWLY_CREATED_BDA,
                           new Object[]{beanClass, newBda});
            }
            beanDeploymentArchives.add(newBda);

            // Make all previously found extension BDAs visible to this extension BDA
            newBda.getBeanDeploymentArchives().addAll(extensionBDAMap.values());

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
        if (!extensions.isEmpty()) {
            return extensions;
        }

        Set<BeanDeploymentArchive> bdas = getBeanDeploymentArchives();
        ArrayList<Metadata<Extension>> extnList = new ArrayList<>();

        //registering the org.jboss.weld.lite.extension.translator.LiteExtensionTranslator
        //to be able to execute build compatible extensions
        List<Class<? extends BuildCompatibleExtension>> buildExtensions = getBuildCompatibleExtensions();
        if (!buildExtensions.isEmpty()) {
            try {
                LiteExtensionTranslator extensionTranslator = getSecurityManager() != null ?
                        doPrivileged(new PrivilegedAction<LiteExtensionTranslator>() {
                            @Override
                            public LiteExtensionTranslator run() {
                                return new LiteExtensionTranslator(buildExtensions, Thread.currentThread().getContextClassLoader());
                            }
                        }): new LiteExtensionTranslator(buildExtensions, Thread.currentThread().getContextClassLoader());
                extnList.add(new MetadataImpl<>(extensionTranslator));
            } catch(Exception e) {
                logger.log(WARNING, "Problem to register CDI Build Compatible Extensions");
                throw new RuntimeException(e);
            }
        }

        // Track classloaders to ensure we don't scan the same classloader twice
        HashSet<ClassLoader> scannedClassLoaders = new HashSet<>();

        // ensure we don't add the same extension twice
        HashMap<Class<?>,Metadata<Extension>> loadedExtensions = new HashMap<>();

        for (BeanDeploymentArchive bda : bdas) {
            if (!(bda instanceof RootBeanDeploymentArchive)) {
                ClassLoader moduleClassLoader = ((BeanDeploymentArchiveImpl)bda).getModuleClassLoaderForBDA();
                if (!scannedClassLoaders.contains(moduleClassLoader)) {
                    scannedClassLoaders.add(moduleClassLoader);
                    var extensions = context.getTransientAppMetaData(WeldDeployer.WELD_BOOTSTRAP,
                            WeldBootstrap.class).loadExtensions(moduleClassLoader);
                    for (Metadata<Extension> bdaExtn : extensions) {
                        if (loadedExtensions.get(bdaExtn.getValue().getClass()) == null) {
                            extnList.add(bdaExtn);
                            loadedExtensions.put(bdaExtn.getValue().getClass(), bdaExtn);
                        }
                    }
                }
            }
        }

        // Load sniffer extensions
        @SuppressWarnings("unchecked")
        Iterable<Supplier<Extension>> snifferExtensions = context.getTransientAppMetaData(WeldDeployer.SNIFFER_EXTENSIONS, Iterable.class);
        if (snifferExtensions != null) {
            for (Supplier<Extension> extensionCreator : snifferExtensions) {
                final Extension extension = extensionCreator.get();
                final Class<?> extensionClass = extension.getClass();
                final Metadata<Extension> extensionMetadata = new MetadataImpl<>(extension, extensionClass.getName());
                extnList.add(extensionMetadata);
            }
        }

        extnList.addAll(dynamicExtensions);
        extensions.addAll(extnList);
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
        Set<BeanDeploymentArchive> beanDeploymentArchives = getBeanDeploymentArchives();
        for(BeanDeploymentArchive bda : beanDeploymentArchives) {
            valBuff.append(bda.toString());
        }
        return valBuff.toString();
    }

    public BeanDeploymentArchive getBeanDeploymentArchiveForArchive(String archiveId) {
        return idToBeanDeploymentArchive.get(archiveId);
    }

    public void cleanup() {
        ejbRootBdas.clear();
        warRootBdas.clear();
        libJarRootBdas.clear();
        rarRootBdas.clear();
        idToBeanDeploymentArchive.clear();
    }

    private List<Class<? extends BuildCompatibleExtension>> getBuildCompatibleExtensions() {
        return
                ServiceLoader.load(BuildCompatibleExtension.class, Thread.currentThread().getContextClassLoader())
                        .stream()
                        .map(java.util.ServiceLoader.Provider::get)
                        .map(BuildCompatibleExtension::getClass)
                        .filter(e -> !e.isAnnotationPresent(SkipIfPortableExtensionPresent.class))
                        .collect(toList());
    }

    // This method creates and returns a List of BeanDeploymentArchives for each
    // Weld enabled jar under /lib of an existing Archive.
    private Set<RootBeanDeploymentArchive> scanForLibJars( ReadableArchive archive,
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
                            if (jarInLib != null && (jarInLib.exists(META_INF_BEANS_XML) || WeldUtils.isImplicitBeanArchive(context, jarInLib))) {
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
            // each appLib in context.getAppLibs is a URI of the form
            // "file:/glassfish/runtime/trunk/glassfish4/glassfish/domains/domain1/lib/applibs/mylib.jar"
            List<URI> appLibs = context.getAppLibs();

            Set<String> installedLibraries = InstalledLibrariesResolver.getInstalledLibraries(archive);
            if ( appLibs != null && !appLibs.isEmpty() && installedLibraries != null && !installedLibraries.isEmpty() ) {
                for ( URI oneAppLib : appLibs ) {
                    for ( String oneInstalledLibrary : installedLibraries ) {
                        if ( oneAppLib.getPath().endsWith( oneInstalledLibrary ) ) {
                            addLibBDA(archive, context, oneAppLib, libBdas);
                            break;
                        }
                    }
                }
            }

            if (DeploymentUtils.useWarLibraries(context)) {
                for (Path warLibrary : InstalledLibrariesResolver.getWarLibraries()) {
                    addLibBDA(archive, context, warLibrary.toUri(), libBdas);
                }
            }
        } catch (URISyntaxException | IOException e) {
            //todo: log error
        }

        for ( RootBeanDeploymentArchive oneBda : libBdas ) {
            createLibJarBda(oneBda );
        }
    }

    private void addLibBDA(ReadableArchive archive, DeploymentContext context, URI oneAppLib, List<RootBeanDeploymentArchive> libBdas) throws IOException {
        ReadableArchive libArchive = null;
        try {
            libArchive = archiveFactory.openArchive(oneAppLib);
            if ( libArchive.exists( WeldUtils.META_INF_BEANS_XML ) || WeldUtils.isImplicitBeanArchive(context, libArchive) ) {
                String bdaId = archive.getName() + "_" + libArchive.getName();
                RootBeanDeploymentArchive rootBda =
                        new RootBeanDeploymentArchive(libArchive,
                                Collections.emptyList(),
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

        for (BeanDeploymentArchive oneBda : beanDeploymentArchives) {
            BeanDeploymentArchiveImpl beanDeploymentArchiveImpl = (BeanDeploymentArchiveImpl) oneBda;
            if (beanDeploymentArchiveImpl.getKnownClasses().contains(beanClass.getName())) {
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

    private RootBeanDeploymentArchive findRootBda( ClassLoader classLoader, Set<RootBeanDeploymentArchive> rootBdas ) {
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
            if ((moduleBeansXml == null
                    || (!moduleBeansXml.getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) || forceInitialisation(context))) {
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
        return libJarRootBdas.iterator();
    }

    public Iterator<RootBeanDeploymentArchive> getRarRootBdas() {
        return rarRootBdas.iterator();
    }

    public String getAppName() {
        return appName;
    }

    public String getContextId() {
        return contextId;
    }

    /**
     * Gets the {@link Types} from the {@link DeploymentContext}'s transient metadata
     *
     * @return The {@link Types} from the {@link DeploymentContext}'s transient metadata
     */
    public Types getTypes() {
        return context.getTransientAppMetaData(Types.class.getName(), Types.class);
    }

    private boolean forceInitialisation(DeploymentContext context) {
        Boolean force = context.getTransientAppMetaData("fish.payara.faces.integration.allowFacesCdiInitialisation", Boolean.class);
        if (force != null && force) {
            logger.fine("allowFacesCdiInitialisation enabled, forcing initialisation of Weld");
            return true;
        }

        return false;
    }

}
