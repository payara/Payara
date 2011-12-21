/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
import static org.glassfish.weld.connector.WeldUtils.META_INF_SERVICES_EXTENSION;
import static org.glassfish.weld.connector.WeldUtils.SEPARATOR_CHAR;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Extension;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.weld.connector.WeldUtils.BDAType;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

import com.sun.enterprise.deployment.EjbDescriptor;

/*
 * Represents a deployment of a CDI (Weld) application. 
 */
public class DeploymentImpl implements Deployment {

    // Keep track of our BDAs for this deployment
    private List<BeanDeploymentArchive> jarBDAs;
    private List<BeanDeploymentArchive> warBDAs;
    private List<BeanDeploymentArchive> libJarBDAs = null;

    private List<BeanDeploymentArchive> beanDeploymentArchives = null;
    private DeploymentContext context;

    // A convenience Map to get BDA for a given BDA ID
    private Map<String, BeanDeploymentArchive> idToBeanDeploymentArchive;
    private SimpleServiceRegistry simpleServiceRegistry = null;

    private Logger logger = Logger.getLogger(DeploymentImpl.class.getName());
    
    /**
     * Produce <code>BeanDeploymentArchive</code>s for this <code>Deployment</code>
     * from information from the provided <code>ReadableArchive</code>. 
     */
    public DeploymentImpl(ReadableArchive archive, Collection<EjbDescriptor> ejbs,
                          DeploymentContext context) {
        logger.log(FINE, "Creating deployment for archive:" + archive.getName());
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
        this.beanDeploymentArchives.add(bda);
        if (((BeanDeploymentArchiveImpl)bda).getBDAType().equals(BDAType.WAR)) {
            if (warBDAs == null) {
                warBDAs = new ArrayList<BeanDeploymentArchive>();
            }
            warBDAs.add(bda);
        } else if (((BeanDeploymentArchiveImpl)bda).getBDAType().equals(BDAType.JAR)) {
            if (jarBDAs == null) {
                jarBDAs = new ArrayList<BeanDeploymentArchive>();
            }
            jarBDAs.add(bda);
        }
        this.idToBeanDeploymentArchive.put(bda.getId(), bda);
    }

    /**
     * Produce <code>BeanDeploymentArchive</code>s for this <code>Deployment</code>
     * from information from the provided <code>ReadableArchive</code>. 
     * This method is called for subsequent modules after This <code>Deployment</code> has
     * been created.
     */
    public void scanArchive(ReadableArchive archive, Collection<EjbDescriptor> ejbs,
                            DeploymentContext context) {

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

        beanDeploymentArchives.add(bda);
        if (((BeanDeploymentArchiveImpl)bda).getBDAType().equals(BDAType.WAR)) {
            if (warBDAs == null) {
                warBDAs = new ArrayList<BeanDeploymentArchive>();
            }
            warBDAs.add(bda);
        } else if (((BeanDeploymentArchiveImpl)bda).getBDAType().equals(BDAType.JAR)) {
            if (jarBDAs == null) {
                jarBDAs = new ArrayList<BeanDeploymentArchive>();
            }
            jarBDAs.add(bda);
        }
        idToBeanDeploymentArchive.put(bda.getId(), bda);

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
            ListIterator<BeanDeploymentArchive> jarIter = jarBDAs.listIterator();
            while (jarIter.hasNext()) {
                boolean modifiedArchive = false;
                BeanDeploymentArchive jarBDA = (BeanDeploymentArchive)jarIter.next();
                ListIterator<BeanDeploymentArchive> jarIter1 = jarBDAs.listIterator();
                while (jarIter1.hasNext()) {
                    BeanDeploymentArchive jarBDA1 = (BeanDeploymentArchive)jarIter1.next();
                    if (jarBDA1.getId().equals(jarBDA.getId())) {
                        continue;
                    }
                    jarBDA.getBeanDeploymentArchives().add(jarBDA1);
                    modifiedArchive = true;
                }

                // Make /lib jars (application) accessible
                if (libJarBDAs != null) {
                    ListIterator<BeanDeploymentArchive> libJarIter = libJarBDAs.listIterator();
                    while (libJarIter.hasNext()) {
                        BeanDeploymentArchive libJarBDA = (BeanDeploymentArchive)libJarIter.next();
                        jarBDA.getBeanDeploymentArchives().add(libJarBDA);
                        modifiedArchive = true;
                    }
                }

                if (modifiedArchive) {
                    int idx = getBeanDeploymentArchives().indexOf(jarBDA);
                    if (idx >= 0) {
                        getBeanDeploymentArchives().remove(idx);
                        getBeanDeploymentArchives().add(jarBDA);
                    }
                    modifiedArchive = false;
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
                BeanDeploymentArchive warBDA = (BeanDeploymentArchive)warIter.next();
                if (jarBDAs != null) {
                    ListIterator<BeanDeploymentArchive> jarIter = jarBDAs.listIterator();
                    while (jarIter.hasNext()) {
                        BeanDeploymentArchive jarBDA = (BeanDeploymentArchive)jarIter.next();
                        warBDA.getBeanDeploymentArchives().add(jarBDA);
                        modifiedArchive = true;
                    }
                }

                // Make /lib jars (application) accessible

                if (libJarBDAs != null) {
                    ListIterator<BeanDeploymentArchive> libJarIter = libJarBDAs.listIterator();
                    while (libJarIter.hasNext()) {
                        BeanDeploymentArchive libJarBDA = (BeanDeploymentArchive)libJarIter.next();
                        warBDA.getBeanDeploymentArchives().add(libJarBDA);
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

      
    }


    @Override
    public List<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return getBeanDeploymentArchives(true);
    }
    
    public List<BeanDeploymentArchive> getBeanDeploymentArchives(boolean printDebug) {
        if (printDebug) logger.log(FINE, "DeploymentImpl::getBDAs. " +
        		"Returning \n" + beanDeploymentArchives);
        if (!beanDeploymentArchives.isEmpty()) {
            return beanDeploymentArchives;
        }
        return Collections.emptyList(); 
    }

    @Override
    public BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> beanClass) {
        logger.log(FINE, "DeploymentImpl::loadBDA:"+ beanClass);
        List<BeanDeploymentArchive> beanDeploymentArchives = getBeanDeploymentArchives(false);
        
        ListIterator<BeanDeploymentArchive> lIter = beanDeploymentArchives.listIterator(); 
        while (lIter.hasNext()) {
            BeanDeploymentArchive bda = lIter.next();
            logger.log(FINE, "checking for " + beanClass + " in root BDA" + bda.getId());
            if (((BeanDeploymentArchiveImpl)bda).getModuleBeanClasses().contains(beanClass.getName())) {
                //don't stuff this Bean Class into the BDA's beanClasses, 
                //as Weld automatically add theses classes to the BDA's bean Classes
                logger.log(FINE, "DeploymentImpl(as part of loadBDA)::An " +
                		"existing BDA has this class " + beanClass.getName() 
                		+ " and so adding this class as a bean class it to " +
                		"existing bda: " + bda);
                //((BeanDeploymentArchiveImpl)bda).addBeanClass(beanClass.getName());
                logger.log(FINE, "Deployment(as part of loadBDA): and returning " + bda);
                return bda;
            }

            //XXX: As of now, we handle one-level. Ideally, a bean deployment 
            //descriptor is a composite and we should be able to search the tree 
            //and get the right BDA for the beanClass
            if (bda.getBeanDeploymentArchives().size() > 0) {
                for(BeanDeploymentArchive subBda: bda.getBeanDeploymentArchives()){
                    Collection<String> s = ((BeanDeploymentArchiveImpl)subBda).getModuleBeanClasses();
                    logger.log(FINE, "checking for " + beanClass + " in subBDA" + subBda.getId());
                    boolean match = s.contains(beanClass.getName());
                    if (match) {
                        //don't stuff this Bean Class into the BDA's beanClasses, 
                        //as Weld automatically add theses classes to the BDA's bean Classes
                        logger.log(FINE, "DeploymentImpl(as part of loadBDA)::" +
                        		"An existing BDA has this class " 
                                + beanClass.getName() + " and so adding this " +
                                "class as a bean class to existing bda:" + subBda);
                        //((BeanDeploymentArchiveImpl)subBda).addBeanClass(beanClass.getName());
                        logger.log(FINE, "Deployment(as part of loadBDA): and returning " + subBda);
                        return subBda;
                    }
                }
            }
        }

        // If the BDA was not found for the Class, create one and add it
        logger.log(FINE, "+++++ DeploymentImpl(as part of loadBDA):: beanClass " 
                + beanClass + " not found in the BDAs of this deployment. " +
                "Hence creating a new BDA");
        List<Class<?>> beanClasses = new ArrayList<Class<?>>();
        Set<URI> beanXMLUris = new CopyOnWriteArraySet<URI>();
        Set<EjbDescriptor> ejbs = new HashSet<EjbDescriptor>();
        beanClasses.add(beanClass);
        BeanDeploymentArchive newBda = 
            new BeanDeploymentArchiveImpl(beanClass.getName(), 
                    beanClasses, beanXMLUris, ejbs, context);
        logger.log(FINE, "DeploymentImpl(as part of loadBDA):: new BDA " 
                + newBda + "created. Now adding this new BDA to " +
                "all root BDAs of this deployment");
        lIter = beanDeploymentArchives.listIterator();
        while (lIter.hasNext()) {
            BeanDeploymentArchive bda = lIter.next();
            bda.getBeanDeploymentArchives().add(newBda);
        }
        logger.log(FINE, "DeploymentImpl(as part of loadBDA):: for beanClass " 
                + beanClass + " finally returning the " +
                "newly created BDA " + newBda);
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
        List<BeanDeploymentArchive> bdas = getBeanDeploymentArchives();
        ArrayList<Metadata<Extension>> extnList = new ArrayList<Metadata<Extension>>();
        for(BeanDeploymentArchive bda:bdas){
            Iterable<Metadata<Extension>> bdaExtns = context.getTransientAppMetaData(
                    WeldDeployer.WELD_BOOTSTRAP, WeldBootstrap.class).loadExtensions(
                    ((BeanDeploymentArchiveImpl) bda).getModuleClassLoaderForBDA());
            for(Metadata<Extension> bdaExtn : bdaExtns){
                extnList.add(bdaExtn);
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
    private List<BeanDeploymentArchive> scanForLibJars(
                            ReadableArchive archive, Collection<EjbDescriptor> ejbs, 
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
                            if (jarInLib.exists(META_INF_BEANS_XML)) {
                                if (libJars == null) {
                                    libJars = new ArrayList<ReadableArchive>();
                                }
                                libJars.add(jarInLib);
                            } else if (jarInLib.exists(META_INF_SERVICES_EXTENSION)){
                                if (libJars == null) {
                                    libJars = new ArrayList<ReadableArchive>();
                                }
                                libJars.add(jarInLib);
                            }
                        } catch (IOException e) {
                            logger.log(FINE, "Exception thrown while scanning for library jars", e);
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
                BeanDeploymentArchive bda = new BeanDeploymentArchiveImpl(
                        libJarArchive, ejbs, context, 
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
                    logger.log(FINE, "DeploymentImpl::ensureEarLibJarVisibility - " + firstBDA.getId() + " being associated with " + otherBDA.getId());
                    firstBDA.getBeanDeploymentArchives().add(otherBDA);
                    modified = true;
                }
            }
            //update modified BDA
            if (modified){
                int idx = this.beanDeploymentArchives.indexOf(firstBDA);
                logger.log(FINE, "DeploymentImpl::ensureEarLibJarVisibility - updating " + firstBDA.getId() );
                if (idx >= 0) {
                    this.beanDeploymentArchives.set(idx, firstBDA);
                }
            }
        }
    }
}
