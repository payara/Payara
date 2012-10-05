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
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;
import static org.glassfish.weld.connector.WeldUtils.CLASS_SUFFIX;
import static org.glassfish.weld.connector.WeldUtils.EXPANDED_RAR_SUFFIX;
import static org.glassfish.weld.connector.WeldUtils.JAR_SUFFIX;
import static org.glassfish.weld.connector.WeldUtils.META_INF_BEANS_XML;
import static org.glassfish.weld.connector.WeldUtils.META_INF_SERVICES_EXTENSION;
import static org.glassfish.weld.connector.WeldUtils.RAR_SUFFIX;
import static org.glassfish.weld.connector.WeldUtils.SEPARATOR_CHAR;
import static org.glassfish.weld.connector.WeldUtils.WEB_INF_BEANS_XML;
import static org.glassfish.weld.connector.WeldUtils.WEB_INF_CLASSES;
import static org.glassfish.weld.connector.WeldUtils.WEB_INF_LIB;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.weld.connector.WeldUtils.BDAType;
import org.glassfish.weld.ejb.EjbDescriptorImpl;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;


/*
 * The means by which Weld Beans are discovered on the classpath. 
 */
public class BeanDeploymentArchiveImpl implements BeanDeploymentArchive {
   
    private Logger logger = Logger.getLogger(BeanDeploymentArchiveImpl.class.getName());

    private ReadableArchive archive;
    private String id;
    private List<Class<?>> moduleClasses = null; //Classes in the module
    private List<Class<?>> beanClasses = null; //Classes identified as Beans through Weld SPI
    private Set<URI> wUris = null;
    private final Collection<EjbDescriptor<?>> ejbDescImpls;
    private List<BeanDeploymentArchive> beanDeploymentArchives;

    private SimpleServiceRegistry simpleServiceRegistry = null;

    private BDAType bdaType = BDAType.UNKNOWN;

    private DeploymentContext context;
    private final Map<AnnotatedType<?>, InjectionTarget<?>> itMap 
                    = new HashMap<AnnotatedType<?>, InjectionTarget<?>>();
    
    //workaround: WELD-781
    private ClassLoader moduleClassLoaderForBDA = null;

    private String friendlyId = "";


    /**
     * Produce a <code>BeanDeploymentArchive</code> form information contained 
     * in the provided <code>ReadableArchive</code>.
     * @param context 
     */
    public BeanDeploymentArchiveImpl(ReadableArchive archive,
            Collection<com.sun.enterprise.deployment.EjbDescriptor> ejbs, DeploymentContext ctx) {
        this(archive, ejbs, ctx, null);
    }

    public BeanDeploymentArchiveImpl(ReadableArchive archive,
        Collection<com.sun.enterprise.deployment.EjbDescriptor> ejbs, DeploymentContext ctx, String bdaID) {
        this.beanClasses = new ArrayList<Class<?>>();
        this.moduleClasses = new ArrayList<Class<?>>();
        this.wUris = new CopyOnWriteArraySet<URI>();
        this.archive = archive;
        if (bdaID == null) {
            this.id = archive.getName();
        } else {
            this.id = bdaID;
        }
        
        this.friendlyId = this.id;
        this.ejbDescImpls = new HashSet<EjbDescriptor<?>>();
        this.beanDeploymentArchives = new ArrayList<BeanDeploymentArchive>();
        this.context = ctx;

        populate(ejbs);
        populateEJBsForThisBDA(ejbs);
        try {
            this.archive.close();
        } catch (Exception e) {
        }
        this.archive = null;
        
        //set to the current TCL
        this.moduleClassLoaderForBDA = Thread.currentThread().getContextClassLoader();
    }

    private void populateEJBsForThisBDA(
            Collection<com.sun.enterprise.deployment.EjbDescriptor> ejbs) {
        for(com.sun.enterprise.deployment.EjbDescriptor next : ejbs) {
            for(Class c: this.moduleClasses) {
                if (c.getName().equals(next.getEjbClassName())){
                    EjbDescriptorImpl wbEjbDesc = new EjbDescriptorImpl(next);
                    ejbDescImpls.add(wbEjbDesc);
                }
            }
        }
    }

    //These are for empty BDAs that do not model Bean classes in the current 
    //deployment unit -- for example: BDAs for portable Extensions.
    public BeanDeploymentArchiveImpl(String id, List<Class<?>> wClasses, Set<URI> wUris,
        Collection<com.sun.enterprise.deployment.EjbDescriptor> ejbs, DeploymentContext ctx) {
        this.id = id;
        this.moduleClasses = wClasses;
        this.beanClasses = new ArrayList<Class<?>>(wClasses);
        this.wUris = wUris;
        this.ejbDescImpls = new HashSet<EjbDescriptor<?>>();
        this.beanDeploymentArchives = new ArrayList<BeanDeploymentArchive>();
        this.context = ctx;
        
        populateEJBsForThisBDA(ejbs);

        //set to the current TCL
        this.moduleClassLoaderForBDA = Thread.currentThread().getContextClassLoader();
    }


    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return beanDeploymentArchives;
    }

    public Collection<String> getBeanClasses() {
        List<String> s  = new ArrayList<String>();
        for (Iterator<Class<?>> iterator = beanClasses.iterator(); iterator.hasNext();) {
            String classname = iterator.next().getName();
            s.add(classname);
        }
        //This method is called during BeanDeployment.deployBeans, so this would
        //be the right time to place the module classloader for the BDA as the TCL
        logger.log(FINER, "set TCL for " + this.id + " to " + this.moduleClassLoaderForBDA);
        Thread.currentThread().setContextClassLoader(this.moduleClassLoaderForBDA);
        //The TCL is unset at the end of deployment of CDI beans in WeldDeployer.event
        //XXX: This is a workaround for issue https://issues.jboss.org/browse/WELD-781.
        //Remove this as soon as the SPI comes in. 
        return s;
    }
    
    public Collection<Class<?>> getBeanClassObjects(){
        return beanClasses;
    }
    
    public Collection<String> getModuleBeanClasses(){
        List<String> s  = new ArrayList<String>();
        for (Iterator<Class<?>> iterator = moduleClasses.iterator(); iterator.hasNext();) {
            String classname = iterator.next().getName();
            s.add(classname);
        }
        return s;
    }

    public Collection<Class<?>> getModuleBeanClassObjects(){
        return moduleClasses;
    }

    public void addBeanClass(String beanClassName){
        boolean added = false;
        for (Iterator<Class<?>> iterator = moduleClasses.iterator(); iterator.hasNext();) {
            Class c = iterator.next();
            if (c.getName().equals(beanClassName)) {
                logger.log(FINE, "BeanDeploymentArchiveImpl::addBeanClass - adding " + c + "to " + beanClasses );
                beanClasses.add(c);
                added = true;
            }
        }
        if (!added) logger.log(FINE, "Error!!!! " + beanClassName + " not added to beanClasses");
    }

    public BeansXml getBeansXml() {
        WeldBootstrap wb =  context.getTransientAppMetaData(WeldDeployer.WELD_BOOTSTRAP, WeldBootstrap.class);
        //convert the URI Set to a URL Collection, as WeldBootstrap.parse() 
        //expects it so.
        List<URL> sl = new ArrayList<URL>();
        for(URI u: wUris) {
            try {
                sl.add(u.toURL());
            } catch (MalformedURLException mfue) {
                logger.log(Level.WARNING, "Error parsing Beans XML URL " + u, mfue);
            }
        }
        return wb.parse(sl);
    }

    /**
    * Gets a descriptor for each EJB
    *
    * @return the EJB descriptors
    */
    public Collection<EjbDescriptor<?>> getEjbs() {

       return ejbDescImpls;
    }

    public EjbDescriptor getEjbDescriptor(String ejbName) {
        EjbDescriptor match = null;

        for(EjbDescriptor next : ejbDescImpls) {
            if( next.getEjbName().equals(ejbName) ) {
                match = next;
                break;
            }
        }

        return match;
    }

    public ServiceRegistry getServices() {
        if (simpleServiceRegistry == null) {
            simpleServiceRegistry = new SimpleServiceRegistry();
        }
        return simpleServiceRegistry;
    }

    public String getId() {
        return id;
    }

    public String getFriendlyId() {
        return this.friendlyId;
    }
    
    //A graphical representation of the BDA hierarchy to aid in debugging
    //and to provide a better representation of how Weld treats the deployed
    //archive.
    public String toString() {
        String beanClassesString = ((getBeanClasses().size() > 0) ? getBeanClasses().toString() : ""); 
        String initVal = "|ID: " + getId() + ", bdaType= " + bdaType 
                        + ", accessibleBDAs #:" + getBeanDeploymentArchives().size() 
                        + ", " + formatAccessibleBDAs(this)
                        +  ", Bean Classes #: " + getBeanClasses().size() + "," 
                        + beanClassesString + ", ejbs=" + getEjbs() +"\n";
        StringBuffer valBuff = new StringBuffer(initVal);
        
        Collection<BeanDeploymentArchive> bdas = getBeanDeploymentArchives();
        Iterator<BeanDeploymentArchive> iter = bdas.iterator();
        while (iter.hasNext()) {
            BeanDeploymentArchive bda = (BeanDeploymentArchive) iter.next();
            BDAType embedBDAType = BDAType.UNKNOWN; 
            if (bda instanceof BeanDeploymentArchiveImpl) {
                embedBDAType = ((BeanDeploymentArchiveImpl)bda).getBDAType();
            }
            String embedBDABeanClasses = ((bda.getBeanClasses().size() > 0) ? bda.getBeanClasses().toString() : "");
            String val = "|---->ID: " + bda.getId() + ", bdaType= " + embedBDAType.toString() 
                    + ", accessibleBDAs #:" + bda.getBeanDeploymentArchives().size() 
                    + ", " + formatAccessibleBDAs(bda) +  ", Bean Classes #: " 
                    + bda.getBeanClasses().size() + "," + embedBDABeanClasses 
                    + ", ejbs=" + bda.getEjbs() + "\n";
            valBuff.append(val);
        }
        return valBuff.toString();
    }

    private String formatAccessibleBDAs(BeanDeploymentArchive bda) {
        StringBuffer sb = new StringBuffer("[");
        for (BeanDeploymentArchive accessibleBDA: bda.getBeanDeploymentArchives()) {
            if (accessibleBDA instanceof BeanDeploymentArchiveImpl) {
                sb.append(((BeanDeploymentArchiveImpl)accessibleBDA).getFriendlyId() + ",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public BDAType getBDAType() {
        return bdaType;
    }

    private void populate(Collection<com.sun.enterprise.deployment.EjbDescriptor> ejbs) {
        try {
            if (archive.exists(WEB_INF_BEANS_XML)) {
                logger.log(FINE, "-processing " + archive.getURI() 
                                        + " as it has WEB-INF/beans.xml");
                bdaType = BDAType.WAR;
                Enumeration<String> entries = archive.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement();
                    if (legalClassName(entry)) {
                        if (entry.contains(WEB_INF_CLASSES)) { 
                            //Workaround for incorrect WARs that bundle classes above WEB-INF/classes
                            //[See. GLASSFISH-16706]
                            entry = entry.substring(WEB_INF_CLASSES.length()+1);
                        }
                        String className = filenameToClassname(entry);
                        try {
                            beanClasses.add(getClassLoader().loadClass(className));
                            moduleClasses.add(getClassLoader().loadClass(className));
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "Error while trying to " +
                            		"load Bean Class" + className + " : " + t.toString());
                        }
                    } else if (entry.endsWith("beans.xml")) {
                        URI uri = archive.getURI();
                        File file = new File(uri.getPath() + entry);
                        URI beansXmlUri = file.toURI();
                        wUris.add(beansXmlUri);
                    }
                }
                archive.close();
            }

            // If this archive has WEB-INF/lib entry..
            // Examine all jars;  If the examined jar has a META_INF/beans.xml:
            //  collect all classes in the jar archive
            //  beans.xml in the jar archive

            if (archive.exists(WEB_INF_LIB)) {
                logger.log(FINE, "-processing WEB-INF/lib in " 
                        + archive.getURI());
                bdaType = BDAType.WAR;
                Enumeration<String> entries = archive.entries(WEB_INF_LIB);
                List<ReadableArchive> weblibJarsThatAreBeanArchives = 
                    new ArrayList<ReadableArchive>();
                while (entries.hasMoreElements()) {
                    String entry = (String)entries.nextElement();
                    //if directly under WEB-INF/lib
                    if (entry.endsWith(JAR_SUFFIX) &&
                        entry.indexOf(SEPARATOR_CHAR, WEB_INF_LIB.length() + 1 ) == -1 ) {
                        ReadableArchive weblibJarArchive = archive.getSubArchive(entry);
                        if (weblibJarArchive.exists(META_INF_BEANS_XML)) {
                            logger.log(FINE, "-WEB-INF/lib: considering " + entry 
                              + " as a bean archive and hence added another BDA for it");
                            weblibJarsThatAreBeanArchives.add(weblibJarArchive);
                        } else if (weblibJarArchive.exists(META_INF_SERVICES_EXTENSION)) {
                            logger.log(FINE, "-WEB-INF/lib: considering " + entry 
                                    + " as an extension and creating another BDA for it");
                            weblibJarsThatAreBeanArchives.add(weblibJarArchive);
                        } else {
                          logger.log(FINE, "-WEB-INF/lib: skipping " + archive.getName() 
                                          + " as it doesn't have beans.xml or an extension");
                        }
                    }
               }
                
                //process all web-inf lib JARs and create BDAs for them
                List<BeanDeploymentArchiveImpl> webLibBDAs = new ArrayList<BeanDeploymentArchiveImpl>();
                if (weblibJarsThatAreBeanArchives.size() > 0) {
                    ListIterator<ReadableArchive> libJarIterator = weblibJarsThatAreBeanArchives.listIterator();
                    while (libJarIterator.hasNext()) {
                        ReadableArchive libJarArchive = (ReadableArchive)libJarIterator.next();
                        BeanDeploymentArchiveImpl wlbda = new BeanDeploymentArchiveImpl(libJarArchive, 
                                ejbs, context, 
                                WEB_INF_LIB + SEPARATOR_CHAR + libJarArchive.getName() /* Use WEB-INF/lib/jarName as BDA Id*/);
                        this.beanDeploymentArchives.add(wlbda); //add to list of BDAs for this WAR
                        webLibBDAs.add(wlbda);
                    }
                }
                ensureWebLibJarVisibility(webLibBDAs);
            }

            //Handle RARs. RARs are packaged differently from EJB-JARs or WARs.
            //see 20.2 of Connectors 1.6 specification
            //The resource adapter classes are in a jar file within the
            //RAR archive
            if (archive.getName().endsWith(RAR_SUFFIX) || archive.getName().endsWith(EXPANDED_RAR_SUFFIX)) {
                collectRarInfo(archive);
            }
            
            if (archive.exists(META_INF_BEANS_XML)) {
                logger.log(FINE, "-JAR processing: " + archive.getURI() 
                        + " as a Bean archive jar since it has META-INF/beans.xml");
                bdaType = BDAType.JAR;
                collectJarInfo(archive, true);
            }
            
            if (archive.exists(META_INF_SERVICES_EXTENSION)){
                logger.log(FINE, "-JAR processing: " + archive.getURI() 
                        + " as an extensions jar since it has META-INF/services extension");
                bdaType = BDAType.UNKNOWN;
                collectJarInfo(archive, false);
            }

        } catch(IOException e) {
            logger.log(SEVERE, e.getLocalizedMessage(), e);
        } catch(ClassNotFoundException cne) {
            logger.log(SEVERE, cne.getLocalizedMessage(), cne);
        }
    }   

    private void ensureWebLibJarVisibility(List<BeanDeploymentArchiveImpl> webLibBDAs) {
        //ensure all web-inf/lib JAR BDAs are visible to each other
        for (int i = 0; i < webLibBDAs.size(); i++) {
            BeanDeploymentArchiveImpl firstBDA = webLibBDAs.get(i);
            boolean modified = false;
            //loop through the list once more
            for (int j = 0; j < webLibBDAs.size(); j++) {
                BeanDeploymentArchiveImpl otherBDA = webLibBDAs.get(j);
                if (!firstBDA.getId().equals(otherBDA.getId())){
                    logger.log(FINE, "BDAImpl::ensureWebLibJarVisibility - " + firstBDA.getFriendlyId() + " being associated with " + otherBDA.getFriendlyId());
                    firstBDA.getBeanDeploymentArchives().add(otherBDA);
                    modified = true;
                }
            }
            //update modified BDA
            if (modified){
                int idx = this.beanDeploymentArchives.indexOf(firstBDA);
                logger.log(FINE, "BDAImpl::ensureWebLibJarVisibility - updating " + firstBDA.getFriendlyId() );
                if (idx >= 0) {
                    this.beanDeploymentArchives.set(idx, firstBDA);
                }
            }
        }

        //Include WAR's BDA in list of accessible BDAs of WEB-INF/lib jar BDA.
        for (int i = 0; i < webLibBDAs.size(); i++) {
            BeanDeploymentArchiveImpl subBDA = webLibBDAs.get(i);
            subBDA.getBeanDeploymentArchives().add(this);
            logger.log(FINE, "BDAImpl::ensureWebLibJarVisibility - updating " 
                    + subBDA.getId() + " to include " + this.getId() );
            int idx = this.beanDeploymentArchives.indexOf(subBDA);
            if (idx >= 0) {
                this.beanDeploymentArchives.set(idx, subBDA);
            }
        }
    }

    private void collectJarInfo(ReadableArchive archive, boolean isBeanArchive) 
                        throws IOException, ClassNotFoundException {
        logger.log(FINE, "-collecting jar info for " + archive.getURI());
        Enumeration<String> entries = archive.entries();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement();
            handleEntry(entry, isBeanArchive);
        }
    }

    private void handleEntry(String entry, boolean isBeanArchive) throws ClassNotFoundException {
        if (legalClassName(entry)) {
            String className = filenameToClassname(entry);
            try {
                if (isBeanArchive) {
                    //If the jar is a bean archive, add the class as Bean class also.
                    beanClasses.add(getClassLoader().loadClass(className));
                } 
                //add the class as a module class
                moduleClasses.add(getClassLoader().loadClass(className));
            } catch (Throwable t) {
                logger.log(Level.WARNING, 
                        "Error while trying to load Bean Class " 
                        + className + " : " + t.toString());
            }
        } else if (entry.endsWith("beans.xml")) {
            try {
                URL beansXmlUrl = Thread.currentThread().getContextClassLoader().getResource(entry);
                if (beansXmlUrl != null) {  // http://java.net/jira/browse/GLASSFISH-17157
                    wUris.add(beansXmlUrl.toURI());
                }
            } catch (URISyntaxException use) {
                logger.log(Level.WARNING, "Error handling beans.xml at " + entry, use);
            }
        }
    }

    private boolean legalClassName( String className ) {
        return className.endsWith(CLASS_SUFFIX) && ! className.startsWith(WEB_INF_LIB);
    }

    private void collectRarInfo(ReadableArchive archive) throws IOException,
            ClassNotFoundException {
        logger.log(FINE, "-collecting rar info for " + archive.getURI());
        Enumeration<String> entries = archive.entries();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement();
            if (entry.endsWith(JAR_SUFFIX)){
                ReadableArchive jarArchive = archive.getSubArchive(entry);
                collectJarInfo(jarArchive, true);
            } else {
                handleEntry(entry, true);
            }
        }
    }
    
    private static String filenameToClassname(String filename) {
        String className = null;
        if (filename.indexOf(File.separatorChar) >= 0) {
            className = filename.replace(File.separatorChar, '.');
        } else {
            className = filename.replace(SEPARATOR_CHAR, '.');
        }
        className = className.substring(0, className.length()-6);
        return className;
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl;
        if (this.context.getClassLoader() != null) {
            cl = this.context.getClassLoader();
        } else if (Thread.currentThread().getContextClassLoader() != null) {
            logger.log(FINE, "Using TCL");
            cl = Thread.currentThread().getContextClassLoader();
        } else {
            logger.log(FINE, "TCL is null. Using DeploymentImpl's classloader");
            cl = BeanDeploymentArchiveImpl.class.getClassLoader();
        }
        
        //cache the moduleClassLoader for this BDA
        this.moduleClassLoaderForBDA = cl; 
        return cl;
    }

    public InjectionTarget<?> getInjectionTarget(AnnotatedType<?> annotatedType) {
        return itMap.get(annotatedType);
    }

    void putInjectionTarget(AnnotatedType<?> annotatedType, InjectionTarget<?> it) {
        itMap.put(annotatedType, it);
    }

    public ClassLoader getModuleClassLoaderForBDA() {
        return moduleClassLoaderForBDA;
    }

}
