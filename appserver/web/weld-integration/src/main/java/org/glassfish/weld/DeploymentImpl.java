/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import static java.util.logging.Level.FINE;
import static org.glassfish.weld.connector.WeldUtils.JAR_SUFFIX;
import static org.glassfish.weld.connector.WeldUtils.META_INF_BEANS_XML;
import static org.glassfish.weld.connector.WeldUtils.SEPARATOR_CHAR;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Extension;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.cdi.CDILoggerInfo;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.weld.connector.WeldUtils;
import org.glassfish.weld.connector.WeldUtils.BDAType;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

import com.sun.enterprise.deployment.EjbDescriptor;

/*
 * Represents a deployment of a CDI (Weld) application.
 */
public class DeploymentImpl implements CDI11Deployment {

    // Keep track of our BDAs for this deployment
    private List<BeanDeploymentArchive> rarBDAs;
    private List<BeanDeploymentArchive> jarBDAs;
    private List<BeanDeploymentArchive> warBDAs;
    private List<BeanDeploymentArchive> libJarBDAs = null;

    private List<BeanDeploymentArchive> beanDeploymentArchives = null;
    private DeploymentContext context;

    // A convenience Map to get BDA for a given BDA ID
    private Map<String, BeanDeploymentArchive> idToBeanDeploymentArchive;
    private SimpleServiceRegistry simpleServiceRegistry = null;

    private Logger logger = CDILoggerInfo.getLogger();

    // holds BDA's created for extensions
    private Map<ClassLoader, BeanDeploymentArchive> extensionBDAMap = new HashMap<>();

    private Iterable<Metadata<Extension>> extensions;

    private Collection<EjbDescriptor> deployedEjbs = new LinkedList<>();

    /**
     * Produce <code>BeanDeploymentArchive</code>s for this <code>Deployment</code>
     * from information from the provided <code>ReadableArchive</code>.
     */
    public DeploymentImpl(ReadableArchive archive, Collection<EjbDescriptor> ejbs, DeploymentContext context) {
        if ( logger.isLoggable( FINE ) ) {
            logger.log(FINE, CDILoggerInfo.CREATING_DEPLOYMENT_ARCHIVE, new Object[]{ archive.getName()});
        }
        this.beanDeploymentArchives = new ArrayList<BeanDeploymentArchive>();
        this.context = context;
        this.idToBeanDeploymentArchive = new HashMap<String, BeanDeploymentArchive>();

        // Collect /lib Jar BDAs (if any) from the parent module.
        // If we've produced BDA(s) from any /lib jars, <code>return</code> as
        // additional BDA(s) will be produced for any subarchives (war/jar).
        libJarBDAs = scanForLibJars(archive, ejbs, context);
        if ((libJarBDAs != null) && libJarBDAs.size() > 0) {
            return;
        }

        BeanDeploymentArchive bda = new BeanDeploymentArchiveImpl(archive, ejbs, context);
        addBeanDeploymentArchives(bda);
    }

    // Refactored constructor and scanArchive to use this method as it was duplicate code.
    private void addBeanDeploymentArchives(BeanDeploymentArchive bda) {
        beanDeploymentArchives.add(bda);
        if (((BeanDeploymentArchiveImpl)bda).getBDAType().equals(BDAType.WAR)) {
            if (warBDAs == null) {
                warBDAs = new ArrayList<>();
            }
            warBDAs.add(bda);
        } else if (((BeanDeploymentArchiveImpl)bda).getBDAType().equals(BDAType.JAR)) {
            if (jarBDAs == null) {
                jarBDAs = new ArrayList<>();
            }
            jarBDAs.add(bda);
        } else if (((BeanDeploymentArchiveImpl)bda).getBDAType().equals(BDAType.RAR)) {
            if (rarBDAs == null) {
                rarBDAs = new ArrayList<>();
            }
            rarBDAs.add(bda);
        }
        idToBeanDeploymentArchive.put(bda.getId(), bda);
    }

    /**
     * Produce <code>BeanDeploymentArchive</code>s for this <code>Deployment</code>
     * from information from the provided <code>ReadableArchive</code>.
     * This method is called for subsequent modules after This <code>Deployment</code> has
     * been created.
     */
    public void scanArchive(ReadableArchive archive, Collection<EjbDescriptor> ejbs, DeploymentContext context) {
        if (libJarBDAs == null) {
            libJarBDAs = scanForLibJars(archive, ejbs, context);
            if ((libJarBDAs != null) && libJarBDAs.size() > 0) {
                return;
            }
        }

        BeanDeploymentArchive bda = new BeanDeploymentArchiveImpl(archive, ejbs, context);

        this.context = context;

        if (idToBeanDeploymentArchive == null) {
            idToBeanDeploymentArchive = new HashMap<String, BeanDeploymentArchive>();
        }

        addBeanDeploymentArchives(bda);
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

        if (jarBDAs != null) {
            for (BeanDeploymentArchive oneJarBDA : jarBDAs) {
                boolean modifiedArchive = false;
                for (BeanDeploymentArchive otherJarBDA : jarBDAs) {
                    if (otherJarBDA.getId().equals(oneJarBDA.getId())) {
                        continue;
                    }
                    oneJarBDA.getBeanDeploymentArchives().add(otherJarBDA);
                    modifiedArchive = true;
                }

                // Make /lib jars (application) accessible
                if (libJarBDAs != null) {
                    for (BeanDeploymentArchive libJarBDA : libJarBDAs) {
                        oneJarBDA.getBeanDeploymentArchives().add(libJarBDA);
                        modifiedArchive = true;
                    }
                }

                // Make rars accessible to ejbs
                if (rarBDAs != null) {
                    for (BeanDeploymentArchive oneRarBda : rarBDAs) {
                        oneJarBDA.getBeanDeploymentArchives().add(oneRarBda);
                        modifiedArchive = true;
                    }
                }

                if (modifiedArchive) {
                    int idx = getBeanDeploymentArchives().indexOf(oneJarBDA);
                    if (idx >= 0) {
                        getBeanDeploymentArchives().remove(idx);
                        getBeanDeploymentArchives().add(oneJarBDA);
                    }
                }
            }
        }

        // Make jars (external to WAR modules) accessible to WAR BDAs - Example:
        //    /web.war ----> /ejb.jar
        // If there are any application (/lib) jars, make them accessible

        if (warBDAs != null) {
            ListIterator<BeanDeploymentArchive> warIter = warBDAs.listIterator();
            boolean modifiedArchive = false;
            while (warIter.hasNext()) {
                BeanDeploymentArchive warBDA = warIter.next();
                if (jarBDAs != null) {
                    for (BeanDeploymentArchive jarBDA : jarBDAs) {
                        warBDA.getBeanDeploymentArchives().add(jarBDA);
                        modifiedArchive = true;
                    }
                }

                // Make /lib jars (application) accessible
                if (libJarBDAs != null) {
                    for (BeanDeploymentArchive libJarBDA : libJarBDAs) {
                        warBDA.getBeanDeploymentArchives().add(libJarBDA);
                        modifiedArchive = true;
                    }
                }

                // Make rars accessible to wars
                if (rarBDAs != null) {
                    for (BeanDeploymentArchive oneRarBda : rarBDAs) {
                        warBDA.getBeanDeploymentArchives().add(oneRarBda);
                        modifiedArchive = true;
                    }
                }

                if (modifiedArchive) {
                    int idx = getBeanDeploymentArchives().indexOf(warBDA);
                    if (idx >= 0) {
                        getBeanDeploymentArchives().remove(idx);
                        getBeanDeploymentArchives().add(warBDA);
                    }
                    modifiedArchive = false;
                }
            }
        }
        addDependentBdas();
    }

    private void addDependentBdas() {
        Set<BeanDeploymentArchive> additionalBdas = new HashSet<BeanDeploymentArchive>();
        for ( BeanDeploymentArchive oneBda : beanDeploymentArchives ) {
            BeanDeploymentArchiveImpl beanDeploymentArchiveImpl = ( BeanDeploymentArchiveImpl ) oneBda;
            Collection<BeanDeploymentArchive> subBdas = beanDeploymentArchiveImpl.getBeanDeploymentArchives();
            for (BeanDeploymentArchive subBda : subBdas) {
                if ( subBda.getBeanClasses().size() > 0 ) {
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
        if (!beanDeploymentArchives.isEmpty()) {
            return beanDeploymentArchives;
        }
        return Collections.emptyList();
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
            if (((BeanDeploymentArchiveImpl)bda).getModuleBeanClassObjects().contains(beanClass)) {
                //don't stuff this Bean Class into the BDA's beanClasses,
                //as Weld automatically add theses classes to the BDA's bean Classes
                if ( logger.isLoggable( FINE ) ) {
                    logger.log(FINE,
                               CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_ADD_TO_EXISTING,
                               new Object[] {beanClass.getName(), bda });
                    //((BeanDeploymentArchiveImpl)bda).addBeanClass(beanClass.getName());
                }
                return bda;
            }

            //XXX: As of now, we handle one-level. Ideally, a bean deployment
            //descriptor is a composite and we should be able to search the tree
            //and get the right BDA for the beanClass
            if (bda.getBeanDeploymentArchives().size() > 0) {
                for(BeanDeploymentArchive subBda: bda.getBeanDeploymentArchives()){
                    Collection<Class<?>> moduleBeanClasses = ((BeanDeploymentArchiveImpl)subBda).getModuleBeanClassObjects();
                    if ( logger.isLoggable( FINE ) ) {
                        logger.log(FINE,
                                   CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_CHECKING_SUBBDA,
                                   new Object[] {beanClass, subBda.getId()});
                    }
                    boolean match = moduleBeanClasses.contains(beanClass);
                    if (match) {
                        //don't stuff this Bean Class into the BDA's beanClasses,
                        //as Weld automatically add theses classes to the BDA's bean Classes
                        if ( logger.isLoggable( FINE ) ) {
                            logger.log(FINE,
                                       CDILoggerInfo.LOAD_BEAN_DEPLOYMENT_ARCHIVE_ADD_TO_EXISTING,
                                       new Object[]{ beanClass.getName(), subBda});
                        }
                        //((BeanDeploymentArchiveImpl)subBda).addBeanClass(beanClass.getName());
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
        List<Class<?>> beanClasses = new ArrayList<Class<?>>();
        List<URL> beanXMLUrls = new CopyOnWriteArrayList<URL>();
        Set<EjbDescriptor> ejbs = new HashSet<EjbDescriptor>();
        beanClasses.add(beanClass);
        BeanDeploymentArchive newBda =
            new BeanDeploymentArchiveImpl(beanClass.getName(),
                                          beanClasses, beanXMLUrls, ejbs, context);
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
        addBeanDeploymentArchives(newBda);
        extensionBDAMap.put( beanClass.getClassLoader(), newBda);
        return newBda;
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
        if ( extensions != null ) {
            return extensions;
        }

        List<BeanDeploymentArchive> bdas = getBeanDeploymentArchives();
        ArrayList<Metadata<Extension>> extnList = new ArrayList<Metadata<Extension>>();
        for ( BeanDeploymentArchive bda : bdas ) {
            ClassLoader moduleClassLoader = ( ( BeanDeploymentArchiveImpl ) bda ).getModuleClassLoaderForBDA();
            extensions = context.getTransientAppMetaData( WeldDeployer.WELD_BOOTSTRAP,
                                                          WeldBootstrap.class).loadExtensions( moduleClassLoader );
            if ( extensions != null ) {
                for ( Metadata<Extension> bdaExtn : extensions ) {
                    extnList.add(bdaExtn);
                }
            }
        }
        return extnList;
    }

    @Override
    public String toString() {
        StringBuffer valBuff = new StringBuffer();
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
        if (jarBDAs != null) {
            jarBDAs.clear();
        }
        if (warBDAs != null) {
            warBDAs.clear();
        }
        if (libJarBDAs!= null) {
            libJarBDAs.clear();
        }
        if (idToBeanDeploymentArchive != null) {
            idToBeanDeploymentArchive.clear();
        }
    }


    // This method creates and returns a List of BeanDeploymentArchives for each
    // Weld enabled jar under /lib of an existing Archive.
    private List<BeanDeploymentArchive> scanForLibJars( ReadableArchive archive,
                                                        Collection<EjbDescriptor> ejbs,
                                                        DeploymentContext context) {
        List<ReadableArchive> libJars = null;
        ApplicationHolder holder = context.getModuleMetaData(ApplicationHolder.class);
        if ((holder != null) && (holder.app != null)) {
            String libDir = holder.app.getLibraryDirectory();
            if (libDir != null && !libDir.isEmpty()) {
                Enumeration<String> entries = archive.entries(libDir);
                while (entries.hasMoreElements()) {
                    String entryName = entries.nextElement();
                    // if a jar is directly in lib dir and not WEB-INF/lib/foo/bar.jar
                    if (entryName.endsWith(JAR_SUFFIX) &&
                        entryName.indexOf(SEPARATOR_CHAR, libDir.length() + 1 ) == -1 ) {
                        try {
                            ReadableArchive jarInLib = archive.getSubArchive(entryName);
                            if (jarInLib.exists(META_INF_BEANS_XML) || WeldUtils.hasCDIEnablingAnnotations(context, jarInLib.getURI())) {
                                if (libJars == null) {
                                    libJars = new ArrayList<ReadableArchive>();
                                }
                                libJars.add(jarInLib);
                                // This is causing tck failures, specifically
                                // MultiModuleProcessingTest.testProcessedModulesCount
                                // creating a bda for an extionsion that does not include a beans.xml is handled later
                                // when annotated types are created by that extension.  This is done in
                                // loadBeanDeploymentArchive(Class<?> beanClass)
//                            } else if (jarInLib.exists(META_INF_SERVICES_EXTENSION)){
//                                if (libJars == null) {
//                                    libJars = new ArrayList<ReadableArchive>();
//                                }
//                                libJars.add(jarInLib);
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
            ListIterator<ReadableArchive> libJarIterator = libJars.listIterator();
            while (libJarIterator.hasNext()) {
                ReadableArchive libJarArchive = (ReadableArchive)libJarIterator.next();
                BeanDeploymentArchive bda = new BeanDeploymentArchiveImpl(libJarArchive, ejbs, context,
                    /* use lib/jarname as BDA ID */ libDir + SEPARATOR_CHAR
                    + libJarArchive.getName());
                this.beanDeploymentArchives.add(bda);
                if (libJarBDAs  == null) {
                    libJarBDAs = new ArrayList<BeanDeploymentArchive>();
                }
                libJarBDAs.add(bda);
                this.idToBeanDeploymentArchive.put(bda.getId(), bda);
            }
            //Ensure each library jar in EAR/lib is visible to each other.
            ensureEarLibJarVisibility(libJarBDAs);
        }

        return libJarBDAs;
    }

    private void ensureEarLibJarVisibility(List<BeanDeploymentArchive> earLibBDAs) {
        //ensure all ear/lib JAR BDAs are visible to each other
        for (int i = 0; i < earLibBDAs.size(); i++) {
            BeanDeploymentArchive firstBDA = earLibBDAs.get(i);
            boolean modified = false;
            //loop through the list once more
            for (int j = 0; j < earLibBDAs.size(); j++) {
                BeanDeploymentArchive otherBDA = earLibBDAs.get(j);
                if (!firstBDA.getId().equals(otherBDA.getId())){
                    if ( logger.isLoggable( FINE ) ) {
                        logger.log(FINE, "DeploymentImpl::ensureEarLibJarVisibility - " + firstBDA.getId() + " being associated with " + otherBDA.getId());
                    }
                    firstBDA.getBeanDeploymentArchives().add(otherBDA);
                    modified = true;
                }
            }
            //update modified BDA
            if (modified){
                int idx = this.beanDeploymentArchives.indexOf(firstBDA);
                if ( logger.isLoggable( FINE ) ) {
                    logger.log(FINE, "DeploymentImpl::ensureEarLibJarVisibility - updating " + firstBDA.getId() );
                }
                if (idx >= 0) {
                    this.beanDeploymentArchives.set(idx, firstBDA);
                }
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

    public BeanDeploymentArchive getBeanDeploymentArchive(Class<?> beanClass) {
        for ( BeanDeploymentArchive oneBda : beanDeploymentArchives ) {
            BeanDeploymentArchiveImpl beanDeploymentArchiveImpl = ( BeanDeploymentArchiveImpl ) oneBda;
            if ( beanDeploymentArchiveImpl.getBeanClassObjects().contains( beanClass ) ) {
                return oneBda;
            }
        }
        return null;
    }

}
