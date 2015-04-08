/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ApplicationEnvironment;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.cdi.CDILoggerInfo;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.SimpleDeployer;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.web.deployment.descriptor.AppListenerDescriptorImpl;
import org.glassfish.weld.services.*;
import org.glassfish.weld.util.Util;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BootstrapConfiguration;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.InjectionServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import javax.inject.Inject;
import javax.servlet.jsp.tagext.JspTag;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PostConstruct;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.web.ServletFilterMapping;
import org.glassfish.web.deployment.descriptor.ServletFilterDescriptor;
import org.glassfish.web.deployment.descriptor.ServletFilterMappingDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;

@Service
public class WeldDeployer extends SimpleDeployer<WeldContainer, WeldApplicationContainer>
    implements PostConstruct, EventListener {

    private Logger logger = Logger.getLogger(WeldDeployer.class.getName());

    public static final String WELD_EXTENSION = "org.glassfish.weld";

    public static final String WELD_DEPLOYMENT = "org.glassfish.weld.WeldDeployment";

    /* package */ static final String WELD_BOOTSTRAP = "org.glassfish.weld.WeldBootstrap";

    private static final String WELD_CONTEXT_LISTENER = "org.glassfish.weld.WeldContextListener";

    // Note...this constant is also defined in org.apache.catalina.connector.AsyncContextImpl.  If it changes here it must
    // change there as well.  The reason it is duplicated is so that a dependency from web-core to gf-weld-connector
    // is not necessary.
    private static final String WELD_LISTENER = "org.jboss.weld.servlet.WeldListener";

    private static final String WELD_TERMINATION_LISTENER = "org.jboss.weld.servlet.WeldTerminalListener";

    private static final String WELD_SHUTDOWN = "weld_shutdown";

    //This constant is used to indicate if bootstrap shutdown has been called or not.
    private static final String WELD_BOOTSTRAP_SHUTDOWN = "weld_bootstrap_shutdown";

    private static final String WELD_CONVERSATION_FILTER_CLASS = "org.jboss.weld.servlet.ConversationFilter";
    private static final String WELD_CONVERSATION_FILTER_NAME = "CDI Conversation Filter";
    @Inject
    private Events events;

    @Inject
    private ServiceLocator services;

    @Inject
    private ApplicationRegistry applicationRegistry;

    @Inject
    private InvocationManager invocationManager;

    @Inject
    ArchiveFactory archiveFactory;

    private Map<Application, WeldBootstrap> appToBootstrap =
            new HashMap<Application, WeldBootstrap>();

    private Map<BundleDescriptor, BeanDeploymentArchive> bundleToBeanDeploymentArchive =
            new HashMap<BundleDescriptor, BeanDeploymentArchive>();

    private static final Class<?>[] NON_CONTEXT_CLASSES = {
          Servlet.class,
          ServletContextListener.class,
          Filter.class,
          HttpSessionListener.class,
          ServletRequestListener.class,
          JspTag.class
            // TODO need to add more classes
    };

    static {
      try {
        Util.initializeWeldSingletonProvider();
      } catch ( Throwable ignore ) {}
    }

    @Override
    public MetaData getMetaData() {
        return new MetaData(true, null, new Class[] {Application.class});
    }

    public void postConstruct() {
        events.register(this);
    }

    /**
     * Specific stages of the Weld bootstrapping process will execute across different stages
     * of the deployment process.  Weld deployment will happen when the load phase of the
     * deployment process is complete.  When all modules have been loaded, a deployment
     * graph is produced defining the accessibility relationships between
     * <code>BeanDeploymentArchive</code>s.
     */
    public void event(Event event) {
        if ( event.is(org.glassfish.internal.deployment.Deployment.APPLICATION_LOADED) ) {
            ApplicationInfo appInfo = (ApplicationInfo)event.hook();
            WeldBootstrap bootstrap = appInfo.getTransientAppMetaData(WELD_BOOTSTRAP,
                WeldBootstrap.class);
            if( bootstrap != null ) {
                DeploymentImpl deploymentImpl = appInfo.getTransientAppMetaData(
                        WELD_DEPLOYMENT, DeploymentImpl.class);

                deploymentImpl.buildDeploymentGraph();

                List<BeanDeploymentArchive> archives = deploymentImpl.getBeanDeploymentArchives();
                for (BeanDeploymentArchive archive : archives) {
                    ResourceLoaderImpl loader = new ResourceLoaderImpl(
                      ((BeanDeploymentArchiveImpl) archive).getModuleClassLoaderForBDA());
                    archive.getServices().add(ResourceLoader.class, loader);
                }

                addCdiServicesToNonModuleBdas(deploymentImpl.getLibJarRootBdas(),
                                              services.getService(InjectionManager.class));
                addCdiServicesToNonModuleBdas(deploymentImpl.getRarRootBdas(),
                                              services.getService(InjectionManager.class));

                //get Current TCL
                ClassLoader oldTCL = Thread.currentThread().getContextClassLoader();

                final String fAppName = appInfo.getName();
                invocationManager.pushAppEnvironment(new ApplicationEnvironment() {

                    @Override
                    public String getName() {
                        return fAppName;
                    }

                });
                try {
                    bootstrap.startExtensions(deploymentImpl.getExtensions());
                    bootstrap.startContainer(fAppName, Environments.SERVLET, deploymentImpl/*, new ConcurrentHashMapBeanStore()*/);
                    bootstrap.startInitialization();
                    fireProcessInjectionTargetEvents(bootstrap, deploymentImpl);
                    bootstrap.deployBeans();


                    bootstrap.validateBeans();
                    bootstrap.endInitialization();
                } catch (Throwable t) {
                    try {
                        doBootstrapShutdown(appInfo);
                    } finally {
                        // ignore.
                    }
                    String msgPrefix = getDeploymentErrorMsgPrefix( t );
                    DeploymentException de = new DeploymentException(msgPrefix + t.getMessage());
                    de.initCause(t);
                    throw(de);
                } finally {
                    invocationManager.popAppEnvironment();

                    //The TCL is originally the EAR classloader
                    //and is reset during Bean deployment to the
                    //corresponding module classloader in BeanDeploymentArchiveImpl.getBeans
                    //for Bean classloading to succeed. The TCL is reset
                    //to its old value here.
                    Thread.currentThread().setContextClassLoader(oldTCL);
                    deploymentComplete( deploymentImpl );
                }
            }
        } else if ( event.is(org.glassfish.internal.deployment.Deployment.APPLICATION_STOPPED) ||
                    event.is(org.glassfish.internal.deployment.Deployment.APPLICATION_UNLOADED) ||
                    event.is(org.glassfish.internal.deployment.Deployment.APPLICATION_DISABLED)) {
                ApplicationInfo appInfo = (ApplicationInfo)event.hook();

            Application app = appInfo.getMetaData(Application.class);

            if( app != null ) {

                for(BundleDescriptor next : app.getBundleDescriptors()) {
                    if( next instanceof EjbBundleDescriptor || next instanceof WebBundleDescriptor ) {
                        bundleToBeanDeploymentArchive.remove(next);
                    }
                }

                appToBootstrap.remove(app);
            }

            String shutdown = appInfo.getTransientAppMetaData(WELD_SHUTDOWN, String.class);
            if (Boolean.valueOf(shutdown).equals(Boolean.TRUE)) {
                return;
            }

            ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(appInfo.getAppClassLoader());
            try {
                WeldBootstrap bootstrap = appInfo.getTransientAppMetaData(WELD_BOOTSTRAP, WeldBootstrap.class);
                if (bootstrap != null) {
                    final String fAppName = appInfo.getName();
                    invocationManager.pushAppEnvironment(new ApplicationEnvironment() {
                        @Override
                        public String getName() {
                            return fAppName;
                        }
                    });
                    try {
                        doBootstrapShutdown(appInfo);
                    } catch(Exception e) {
                        logger.log(Level.WARNING,
                                   CDILoggerInfo.WELD_BOOTSTRAP_SHUTDOWN_EXCEPTION,
                                   new Object [] {e});
                    }
                    finally {
                        invocationManager.popAppEnvironment();
                    }
                    appInfo.addTransientAppMetaData(WELD_SHUTDOWN, "true");
                }
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            }
            DeploymentImpl deploymentImpl = appInfo.getTransientAppMetaData( WELD_DEPLOYMENT, DeploymentImpl.class);
            if (deploymentImpl != null) {
                deploymentImpl.cleanup();
            }
        }
    }

    private void deploymentComplete(DeploymentImpl deploymentImpl) {
        for ( BeanDeploymentArchive oneBda : deploymentImpl.getBeanDeploymentArchives()) {
            (( BeanDeploymentArchiveImpl) oneBda ).setDeploymentComplete( true );
        }
    }

    private void doBootstrapShutdown(ApplicationInfo appInfo){
        WeldBootstrap bootstrap = appInfo.getTransientAppMetaData(WELD_BOOTSTRAP,
                WeldBootstrap.class);
       String bootstrapShutdown = appInfo.getTransientAppMetaData(WELD_BOOTSTRAP_SHUTDOWN,
               String.class);
       if (bootstrapShutdown == null || Boolean.valueOf(bootstrapShutdown).equals(Boolean.FALSE)) {
            bootstrap.shutdown();
            appInfo.addTransientAppMetaData(WELD_BOOTSTRAP_SHUTDOWN, "true");
       }
    }
    private String getDeploymentErrorMsgPrefix( Throwable t ) {
        if ( t instanceof javax.enterprise.inject.spi.DefinitionException ) {
            return "CDI definition failure:";
        } else if ( t instanceof javax.enterprise.inject.spi.DeploymentException ) {
            return "CDI deployment failure:";
        } else {
            Throwable cause = t.getCause();
            if ( cause == t || cause == null ) {
                return "CDI deployment failure:";
            } else {
                return getDeploymentErrorMsgPrefix( cause );
            }
        }
    }

    /*
     * We are only firing ProcessInjectionTarget<X> for non-contextual EE
     * components and not using the InjectionTarget<X> from the event during
     * instance creation in JCDIServiceImpl.java
     * TODO weld would provide a better way to do this, otherwise we may need
     * TODO to store InjectionTarget<X> to be used in instance creation
     */
    private void fireProcessInjectionTargetEvents(WeldBootstrap bootstrap, DeploymentImpl impl) {
        List<BeanDeploymentArchive> bdaList = impl.getBeanDeploymentArchives();
        boolean isFullProfile = false;
        Class<?> messageListenerClass = null;

        //Web-Profile and other lighter distributions would not ship the JMS
        //API and implementations. So, the weld-integration layer cannot
        //have a direct dependency on the JMS API
        try {
            messageListenerClass = Thread.currentThread().getContextClassLoader().
                                            loadClass("javax.jms.MessageListener");
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, CDILoggerInfo.JMS_MESSAGElISTENER_AVAILABLE);
            }
            isFullProfile = true;
        } catch (ClassNotFoundException cnfe){
            //ignore cnfe
            isFullProfile = false;
        }

        for(BeanDeploymentArchive bda : bdaList) {
            Collection<Class<?>> bdaClasses = ((BeanDeploymentArchiveImpl)bda).getBeanClassObjects();
            for(Class<?> bdaClazz: bdaClasses) {
                for(Class<?> nonClazz : NON_CONTEXT_CLASSES) {
                    if (nonClazz.isAssignableFrom(bdaClazz)) {
                        firePITEvent(bootstrap, bda, bdaClazz);
                    }
                }

                //For distributions that have the JMS API, an MDB is a valid
                //non-contextual EE component to which we have to
                //fire <code>ProcessInjectionTarget</code>
                //events (see GLASSFISH-16730)
                if (isFullProfile) {
                    if (messageListenerClass.isAssignableFrom(bdaClazz)) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE,
                                       CDILoggerInfo.MDB_PIT_EVENT,
                                       new Object[]{ bdaClazz });
                        }
                        firePITEvent(bootstrap, bda, bdaClazz);
                    }
                }
            }
        }
    }

    private void firePITEvent(WeldBootstrap bootstrap,
            BeanDeploymentArchive bda, Class<?> bdaClazz) {
        //Fix for issue GLASSFISH-17464
        //The PIT event should not be fired for interfaces
        if(bdaClazz.isInterface()){
            return;
        }
        AnnotatedType<?> at = bootstrap.getManager(bda).createAnnotatedType(bdaClazz);
        InjectionTarget<?> it = bootstrap.getManager(bda).fireProcessInjectionTarget(at);
        ((BeanDeploymentArchiveImpl)bda).putInjectionTarget(at, it);
    }

    public BeanDeploymentArchive getBeanDeploymentArchiveForBundle(BundleDescriptor bundle) {
        return bundleToBeanDeploymentArchive.get(bundle);
    }

    public boolean is299Enabled(BundleDescriptor bundle) {
        return bundleToBeanDeploymentArchive.containsKey(bundle);
    }

    public WeldBootstrap getBootstrapForApp(Application app) {
        return appToBootstrap.get(app);
    }

    protected void generateArtifacts(DeploymentContext dc) throws DeploymentException {

    }

    protected void cleanArtifacts(DeploymentContext dc) throws DeploymentException {

    }

    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return null;
    }

    /**
     * Processing in this method is performed for each module that is in the process of being
     * loaded by the container.  This method will collect information from each archive (module)
     * and produce  <code>BeanDeploymentArchive</code> information for each module.
     * The <code>BeanDeploymentArchive</code>s are stored in the <code>Deployment</code>
     * (that will eventually be handed off to <code>Weld</code>.  Once this method is called
     * for all modules (and <code>BeanDeploymentArchive</code> information has been collected
     * for all <code>Weld</code> modules), a relationship structure is produced defining the
     * accessiblity rules for the <code>BeanDeploymentArchive</code>s.
     */
    @Override
    public WeldApplicationContainer load(WeldContainer container, DeploymentContext context) {

        DeployCommandParameters deployParams = context.getCommandParameters(DeployCommandParameters.class);
        ApplicationInfo appInfo = applicationRegistry.get(deployParams.name);

        ReadableArchive archive = context.getSource();

        // See if a WeldBootsrap has already been created - only want one per app.

        WeldBootstrap bootstrap = context.getTransientAppMetaData(WELD_BOOTSTRAP,
                WeldBootstrap.class);
        if ( bootstrap == null) {
            bootstrap = new WeldBootstrap();
            Application app = context.getModuleMetaData(Application.class);
            appToBootstrap.put(app, bootstrap);
            // Stash the WeldBootstrap instance, so we may access the WeldManager later..
            context.addTransientAppMetaData(WELD_BOOTSTRAP, bootstrap);
            appInfo.addTransientAppMetaData(WELD_BOOTSTRAP, bootstrap);
            // Making sure that if WeldBootstrap is added, shutdown is set to false, as it is/would not have been called.
            appInfo.addTransientAppMetaData(WELD_BOOTSTRAP_SHUTDOWN, "false");
        }

        EjbBundleDescriptor ejbBundle = getEjbBundleFromContext(context);

        EjbServices ejbServices = null;

        Set<EjbDescriptor> ejbs = new HashSet<EjbDescriptor>();
        if( ejbBundle != null ) {
            ejbs.addAll(ejbBundle.getEjbs());
            ejbServices = new EjbServicesImpl(services);
        }

        // Create a Deployment Collecting Information From The ReadableArchive (archive)
        DeploymentImpl deploymentImpl = context.getTransientAppMetaData(WELD_DEPLOYMENT, DeploymentImpl.class);
        if (deploymentImpl == null) {
            deploymentImpl = new DeploymentImpl(archive, ejbs, context, archiveFactory);

            // Add services
            TransactionServices transactionServices = new TransactionServicesImpl(services);
            deploymentImpl.getServices().add(TransactionServices.class, transactionServices);

            SecurityServices securityServices = new SecurityServicesImpl();
            deploymentImpl.getServices().add(SecurityServices.class, securityServices);

            ProxyServices proxyServices = new ProxyServicesImpl(services);
            deploymentImpl.getServices().add(ProxyServices.class, proxyServices);

            BootstrapConfigurationImpl bootstrapConfiguration = new BootstrapConfigurationImpl();
            deploymentImpl.getServices().add(BootstrapConfiguration.class, bootstrapConfiguration);

            addWeldListenerToAllWars(context);
        } else {
            deploymentImpl.scanArchive(archive, ejbs, context);
        }
        deploymentImpl.addDeployedEjbs(ejbs);

        if( ejbBundle != null && (!deploymentImpl.getServices().contains(EjbServices.class))) {
            // EJB Services is registered as a top-level service
            deploymentImpl.getServices().add(EjbServices.class, ejbServices);
        }


        BeanDeploymentArchive bda = deploymentImpl.getBeanDeploymentArchiveForArchive(archive.getName());
        if (bda != null && !bda.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {

            WebBundleDescriptor wDesc = context.getModuleMetaData(WebBundleDescriptor.class);
            if( wDesc != null) {
                wDesc.setExtensionProperty(WELD_EXTENSION, "true");

                // Add the Weld Listener.  We have to do it here too in case addWeldListenerToAllWars wasn't
                // able to do it.
                wDesc.addAppListenerDescriptorToFirst(new AppListenerDescriptorImpl(WELD_LISTENER));
                // Add Weld Context Listener - this listener will ensure the WeldELContextListener is used
                // for JSP's..
                wDesc.addAppListenerDescriptor(new AppListenerDescriptorImpl(WELD_CONTEXT_LISTENER));

                // Weld 2.2.1.Final.  There is a tck test for this: org.jboss.cdi.tck.tests.context.session.listener.SessionContextHttpSessionListenerTest
                // This WeldTerminationListener must come after all application-defined listeners
                wDesc.addAppListenerDescriptor(new AppListenerDescriptorImpl(WELD_TERMINATION_LISTENER));

                // Adding Weld ConverstationFilter if there is filterMapping for it and it doesn't exist already.
                // However, it will be applied only if web.xml has mapping for it.
                // Doing this here to make sure that its done only for CDI enabled web application
                for (ServletFilterMapping sfMapping : wDesc.getServletFilterMappings()) {
                      String displayName = ((ServletFilterMappingDescriptor)sfMapping).getDisplayName();
                      if (WELD_CONVERSATION_FILTER_NAME.equals(displayName)) {
                            ServletFilterDescriptor ref = new ServletFilterDescriptor();
                            ref.setClassName(WELD_CONVERSATION_FILTER_CLASS);
                            ref.setName(WELD_CONVERSATION_FILTER_NAME);
                            wDesc.addServletFilter(ref);
                            break;
                      }
                }
            }

            BundleDescriptor bundle = (wDesc != null) ? wDesc : ejbBundle;
            if( bundle != null) {

//                if (bda.getBeanDeploymentArchives().size() > 0 && !bda.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
                if (!bda.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
                    // Register EE injection manager at the bean deployment archive level.
                    // We use the generic InjectionService service to handle all EE-style
                    // injection instead of the per-dependency-type InjectionPoint approach.
                    // Each InjectionServicesImpl instance knows its associated GlassFish bundle.

                    InjectionManager injectionMgr = services.getService(InjectionManager.class);
                    InjectionServices injectionServices = new InjectionServicesImpl(injectionMgr, bundle, deploymentImpl);

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,
                                   CDILoggerInfo.ADDING_INJECTION_SERVICES,
                                   new Object [] {injectionServices, bda.getId()});
                    }
                    bda.getServices().add(InjectionServices.class, injectionServices);

                    //Relevant in WAR BDA - WEB-INF/lib BDA scenarios
                    for(BeanDeploymentArchive subBda: bda.getBeanDeploymentArchives()){
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE,
                                       CDILoggerInfo.ADDING_INJECTION_SERVICES,
                                       new Object [] {injectionServices, subBda.getId()});
                        }
                        subBda.getServices().add(InjectionServices.class, injectionServices);
                    }
                }

                bundleToBeanDeploymentArchive.put(bundle, bda);
            }
        }

        WeldApplicationContainer wbApp = new WeldApplicationContainer();

        // Stash the WeldBootstrap instance, so we may access the WeldManager later..
        //context.addTransientAppMetaData(WELD_BOOTSTRAP, bootstrap);

        context.addTransientAppMetaData(WELD_DEPLOYMENT, deploymentImpl);
        appInfo.addTransientAppMetaData(WELD_DEPLOYMENT, deploymentImpl);

        return wbApp;
    }

    private void addWeldListenerToAllWars(DeploymentContext context) {
        // if there's at least 1 ejb jar then add the listener to all wars
        ApplicationHolder applicationHolder = context.getModuleMetaData(ApplicationHolder.class);
        if ( applicationHolder != null ) {
            if ( applicationHolder.app.getBundleDescriptors(EjbBundleDescriptor.class).size() > 0 ) {
                Set<WebBundleDescriptor> webBundleDescriptors = applicationHolder.app.getBundleDescriptors(WebBundleDescriptor.class);
                for ( WebBundleDescriptor oneWebBundleDescriptor : webBundleDescriptors ) {
                    // Add the Weld Listener if it does not already exist..
                    // we have to do this regardless because the war may not be cdi-enabled but an ejb is.
                    oneWebBundleDescriptor.addAppListenerDescriptorToFirst(new AppListenerDescriptorImpl(WELD_LISTENER));
                    oneWebBundleDescriptor.addAppListenerDescriptor(new AppListenerDescriptorImpl(WELD_TERMINATION_LISTENER));
                }
            }
        }
    }

    private EjbBundleDescriptor getEjbBundleFromContext(DeploymentContext context) {

        EjbBundleDescriptor ejbBundle = context.getModuleMetaData(EjbBundleDescriptor.class);

        if( ejbBundle == null ) {

            WebBundleDescriptor wDesc = context.getModuleMetaData(WebBundleDescriptor.class);
            if( wDesc != null ) {
                Collection<EjbBundleDescriptor> ejbBundles = wDesc.getExtensionsDescriptors(EjbBundleDescriptor.class);
                if (ejbBundles.iterator().hasNext()) {
                    ejbBundle = ejbBundles.iterator().next();
                }
            }
        }
        return ejbBundle;
    }

    /**
     * Add the cdi services to a non-module bda (library or rar)
     */
    private void addCdiServicesToNonModuleBdas(Iterator<RootBeanDeploymentArchive> rootBdas, InjectionManager injectionMgr) {
        if ( injectionMgr != null && rootBdas != null ) {
            while( rootBdas.hasNext() ) {
                RootBeanDeploymentArchive oneRootBda = rootBdas.next();
                addCdiServicesToBda( injectionMgr, oneRootBda );
                addCdiServicesToBda(injectionMgr, oneRootBda.getModuleBda());
            }
        }
    }

    private void addCdiServicesToBda( InjectionManager injectionMgr, BeanDeploymentArchive bda ) {
        InjectionServices injectionServices = new NonModuleInjectionServices(injectionMgr);
        bda.getServices().add(InjectionServices.class, injectionServices);
    }

}
