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
// Portions Copyright [2016-2022] [Payara Foundation and/or its affiliates]

package org.glassfish.weld;

import static com.sun.enterprise.deployment.util.DOLUtils.getComponentEnvId;
import static com.sun.enterprise.deployment.util.DOLUtils.getCurrentBundleForContext;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.StreamSupport.stream;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION;
import static org.glassfish.cdi.CDILoggerInfo.ADDING_INJECTION_SERVICES;
import static org.glassfish.cdi.CDILoggerInfo.JMS_MESSAGElISTENER_AVAILABLE;
import static org.glassfish.cdi.CDILoggerInfo.MDB_PIT_EVENT;
import static org.glassfish.cdi.CDILoggerInfo.WELD_BOOTSTRAP_SHUTDOWN_EXCEPTION;
import static org.glassfish.internal.deployment.Deployment.APPLICATION_DISABLED;
import static org.glassfish.internal.deployment.Deployment.APPLICATION_LOADED;
import static org.glassfish.internal.deployment.Deployment.APPLICATION_STOPPED;
import static org.glassfish.internal.deployment.Deployment.APPLICATION_UNLOADED;
import static org.glassfish.weld.BeanDeploymentArchiveImpl.stripApplicationVersion;
import static org.glassfish.weld.BeanDeploymentArchiveImpl.stripMavenVersion;
import static org.glassfish.weld.util.Util.initializeWeldSingletonProvider;
import static org.jboss.weld.bootstrap.api.Environments.SERVLET;
import static org.jboss.weld.bootstrap.spi.BeanDiscoveryMode.NONE;
import static org.jboss.weld.manager.BeanManagerLookupService.lookupBeanManager;

import java.security.AccessController;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpSessionListener;
import jakarta.servlet.jsp.tagext.JspTag;

import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.SimpleDeployer;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.web.deployment.descriptor.AppListenerDescriptorImpl;
import org.glassfish.web.deployment.descriptor.ServletFilterDescriptor;
import org.glassfish.web.deployment.descriptor.ServletFilterMappingDescriptor;
import org.glassfish.weld.connector.WeldUtils;
import org.glassfish.weld.services.*;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.helpers.EEModuleDescriptorImpl;
import org.jboss.weld.bootstrap.spi.helpers.MetadataImpl;
import org.jboss.weld.configuration.spi.ExternalConfiguration;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.injection.spi.InjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.probe.ProbeExtension;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.security.NewInstanceAction;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.module.EjbSupport;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.web.ContextParameter;
import com.sun.enterprise.deployment.web.ServletFilterMapping;
import javax.naming.NamingException;
import org.jboss.weld.manager.api.ExecutorServices;

@Service
public class WeldDeployer extends SimpleDeployer<WeldContainer, WeldApplicationContainer> implements PostConstruct, EventListener {

    private Logger logger = Logger.getLogger(WeldDeployer.class.getName());

    public static final String WELD_EXTENSION = "org.glassfish.weld";
    public static final String WELD_DEPLOYMENT = "org.glassfish.weld.WeldDeployment";
    /* package */ static final String WELD_BOOTSTRAP = "org.glassfish.weld.WeldBootstrap";
    public static final String SNIFFER_EXTENSIONS = "org.glassfish.weld.sniffers";
    private static final String WELD_CONTEXT_LISTENER = "org.glassfish.weld.WeldContextListener";

    // Note...this constant is also defined in org.apache.catalina.connector.AsyncContextImpl. If it
    // changes here it must
    // change there as well. The reason it is duplicated is so that a dependency from web-core to
    // gf-weld-connector
    // is not necessary.
    private static final String WELD_LISTENER = "org.jboss.weld.module.web.servlet.WeldListener";
    static final String WELD_TERMINATION_LISTENER = "org.jboss.weld.module.web.servlet.WeldTerminalListener";
    private static final String WELD_SHUTDOWN = "weld_shutdown";

    /**
     * This constant is used to indicate if bootstrap shutdown has been called or not.
     */
    private static final String WELD_BOOTSTRAP_SHUTDOWN = "weld_bootstrap_shutdown";
    private static final String WELD_CONVERSATION_FILTER_CLASS = "org.jboss.weld.module.web.servlet.ConversationFilter";
    private static final String WELD_CONVERSATION_FILTER_NAME = "CDI Conversation Filter";

    public static final String DEV_MODE_PROPERTY = "org.jboss.weld.development";

    private static final String PROBE_FILTER_NAME = "weld-probe-filter";
    private static final String PROBE_FILTER_CLASS_NAME = "org.jboss.weld.probe.ProbeFilter";
    private static final boolean PROBE_FILTER_ASYNC_SUPPORT = true;
    private static final String PROBE_FILTER_URL_PATTERN = "/*";
    private static final String PROBE_FILTER_DISPATCHER_TYPE = "REQUEST";
    private static final String PROBE_INVOCATION_MONITOR_EXCLUDE_TYPE = ".*payara.*|.*glassfish.*";
    private static final String PROBE_EVENT_MONITOR_EXCLUDE_TYPE = "jakarta.servlet.http.*";
    private static final String PROBE_ALLOW_REMOTE_ADDRESS = "";

    private static final String JERSEY_PROCESS_ALL_CLASS_NAME = "org.glassfish.jersey.ext.cdi1x.internal.ProcessAllAnnotatedTypes";
    private static final String JERSEY_HK2_CLASS_NAME = "org.glassfish.jersey.ext.cdi1x.spi.Hk2CustomBoundTypesProvider";
    private static final String JERSEY_PROCESS_JAXRS_CLASS_NAME = "org.glassfish.jersey.ext.cdi1x.internal.ProcessJAXRSAnnotatedTypes";

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

    @Inject
    private Deployment deployment;

    private Map<Application, WeldBootstrap> appToBootstrap = new HashMap<>();

    private Map<BundleDescriptor, BeanDeploymentArchive> bundleToBeanDeploymentArchive = new HashMap<>();

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
            initializeWeldSingletonProvider();
        } catch (Throwable ignore) {
        }
    }

    @Override
    public MetaData getMetaData() {
        return new MetaData(true, null, new Class[] { Application.class });
    }

    @Override
    public void postConstruct() {
        events.register(this);
    }

    @Override
    public boolean prepare(DeploymentContext context) {
        context.addTransientAppMetaData(SNIFFER_EXTENSIONS, new HashSet<Supplier<Extension>>());

        return super.prepare(context);
    }

    /**
     * Processing in this method is performed for each module that is in the process of being loaded by
     * the container.
     *
     * <p>
     * This method will collect information from each archive (module) and produce
     * <code>BeanDeploymentArchive</code> information for each module.
     * </p>
     *
     * <p>
     * The
     * <code>BeanDeploymentArchive</code>s are stored in the <code>Deployment</code> (that will
     * eventually be handed off to <code>Weld</code>. Once this method is called for all modules (and
     * <code>BeanDeploymentArchive</code> information has been collected for all <code>Weld</code>
     * modules), a relationship structure is produced defining the accessiblity rules for the
     * <code>BeanDeploymentArchive</code>s.
     * </p>
     *
     * @param container
     * @param context
     * @return
     */
    @Override
    public WeldApplicationContainer load(WeldContainer container, DeploymentContext context) {

        DeployCommandParameters deployParams = context.getCommandParameters(DeployCommandParameters.class);
        ApplicationInfo applicationInfo = applicationRegistry.get(deployParams.name);

        ReadableArchive archive = context.getSource();

        boolean[] setTransientAppMetaData = {false};

        // See if a WeldBootsrap has already been created - only want one per app.
        WeldBootstrap bootstrap = getWeldBootstrap(context, applicationInfo, setTransientAppMetaData);

        EjbBundleDescriptor ejbBundle = getEjbBundleFromContext(context);

        EjbServices ejbServices = null;

        Set<EjbDescriptor> ejbs = new HashSet<>();
        if (ejbBundle != null) {
            ejbs.addAll(ejbBundle.getEjbs());
            ejbServices = new EjbServicesImpl(services);
        }

        // Create a deployment collecting information from the ReadableArchive (archive)
        // If the archive is a composite, or has version numbers per maven conventions, strip them out
        String archiveName = getArchiveName(context, applicationInfo, archive);

        DeploymentImpl deploymentImpl = context.getTransientAppMetaData(WELD_DEPLOYMENT, DeploymentImpl.class);
        if (deploymentImpl == null) {
            deploymentImpl = new DeploymentImpl(archive, ejbs, context, archiveFactory, archiveName, services.getService(InjectionManager.class));

            // Add services
            TransactionServices transactionServices = new TransactionServicesImpl(services);
            deploymentImpl.getServices().add(TransactionServices.class, transactionServices);

            SecurityServices securityServices = new SecurityServicesImpl();
            deploymentImpl.getServices().add(SecurityServices.class, securityServices);

            ProxyServices proxyServices = new ProxyServicesImpl(services);
            deploymentImpl.getServices().add(ProxyServices.class, proxyServices);

            try {
                ExecutorServices executorServices = new ExecutorServicesImpl();
                deploymentImpl.getServices().add(ExecutorServices.class, executorServices);
            } catch (NamingException ex) {
                throw new RuntimeException(ex);
            }

            addWeldListenerToAllWars(context);
        } else {
            deploymentImpl.scanArchive(archive, ejbs, context, archiveName);
        }
        deploymentImpl.addDeployedEjbs(ejbs);

        if (ejbBundle != null && (!deploymentImpl.getServices().contains(EjbServices.class))) {
            // EJB Services is registered as a top-level service
            deploymentImpl.getServices().add(EjbServices.class, ejbServices);
        }

        ExternalConfigurationImpl externalConfiguration = new ExternalConfigurationImpl();
        externalConfiguration.setRollingUpgradesDelimiter(System.getProperty("fish.payara.rollingUpgradesDelimiter", ":"));
        externalConfiguration.setBeanIndexOptimization(!deployParams.isAvailabilityEnabled());
        externalConfiguration.setNonPortableMode(false);
        configureConcurrentDeployment(context, externalConfiguration);

        deploymentImpl.getServices().add(ExternalConfiguration.class, externalConfiguration);

        BeanDeploymentArchive beanDeploymentArchive = deploymentImpl.getBeanDeploymentArchiveForArchive(archiveName);
        if (beanDeploymentArchive != null && !beanDeploymentArchive.getBeansXml().getBeanDiscoveryMode().equals(NONE)) {
            if (setTransientAppMetaData[0]) {
                // Do this only if we have a root BDA
                appToBootstrap.put(context.getModuleMetaData(Application.class), bootstrap);
                applicationInfo.addTransientAppMetaData(WELD_BOOTSTRAP, bootstrap);
            }

            WebBundleDescriptor webBundleDescriptor = context.getModuleMetaData(WebBundleDescriptor.class);
            boolean developmentMode = isDevelopmentMode(context);
            if (webBundleDescriptor != null) {
                webBundleDescriptor.setExtensionProperty(WELD_EXTENSION, "true");

                // Add the Weld Listener. We have to do it here too in case addWeldListenerToAllWars wasn't
                // able to do it.
                webBundleDescriptor.addAppListenerDescriptorToFirst(new AppListenerDescriptorImpl(WELD_LISTENER));

                // Add Weld Context Listener - this listener will ensure the WeldELContextListener is used
                // for JSP's..
                webBundleDescriptor.addAppListenerDescriptor(new AppListenerDescriptorImpl(WELD_CONTEXT_LISTENER));

                // Weld 2.2.1.Final. There is a tck test for this:
                // org.jboss.cdi.tck.tests.context.session.listener.SessionContextHttpSessionListenerTest
                // This WeldTerminationListener must come after all application-defined listeners
                webBundleDescriptor.addAppListenerDescriptor(new AppListenerDescriptorImpl(WeldTerminationListenerProxy.class.getName()));

                // Adding Weld ConverstationFilter if there is a filterMapping for it and it doesn't exist already.
                // However, it will be applied only if web.xml has a mapping for it.
                // Doing this here to make sure that its done only for CDI enabled web applications
                registerWeldConversationFilter(webBundleDescriptor);

                // If development mode enabled then for WAR register ProbeFilter and register ProbeExtension for
                // every deployment
                if (developmentMode) {
                    registerProbeFilter(webBundleDescriptor);
                }
            }

            if (developmentMode) {
                registerProbeExtension(externalConfiguration, deploymentImpl);
            }

            BundleDescriptor bundle = (webBundleDescriptor != null) ? webBundleDescriptor : ejbBundle;
            if (bundle != null) {

                if (!beanDeploymentArchive.getBeansXml().getBeanDiscoveryMode().equals(NONE)) {

                    // Register EE injection manager at the bean deployment archive level.
                    // We use the generic InjectionService service to handle all EE-style
                    // injection instead of the per-dependency-type InjectionPoint approach.
                    // Each InjectionServicesImpl instance knows its associated GlassFish bundle.

                    InjectionServices injectionServices = new InjectionServicesImpl(deploymentImpl.injectionManager, bundle, deploymentImpl);
                    ResourceInjectionServicesImpl resourceInjectionServices = new ResourceInjectionServicesImpl();
                    if (logger.isLoggable(FINE)) {
                        logger.log(FINE, ADDING_INJECTION_SERVICES, new Object[] { injectionServices, beanDeploymentArchive.getId() });
                    }

                    beanDeploymentArchive.getServices().add(InjectionServices.class, injectionServices);
                    beanDeploymentArchive.getServices().add(ResourceInjectionServices.class, resourceInjectionServices);
                    EEModuleDescriptor eeModuleDescriptor = getEEModuleDescriptor(beanDeploymentArchive);
                    if (eeModuleDescriptor != null) {
                        beanDeploymentArchive.getServices().add(EEModuleDescriptor.class, eeModuleDescriptor);
                    }

                    // Relevant in WAR BDA - WEB-INF/lib BDA scenarios
                    for (BeanDeploymentArchive subBda : beanDeploymentArchive.getBeanDeploymentArchives()) {
                        if (logger.isLoggable(FINE)) {
                            logger.log(FINE, ADDING_INJECTION_SERVICES, new Object[] { injectionServices, subBda.getId() });
                        }

                        subBda.getServices().add(InjectionServices.class, injectionServices);
                        eeModuleDescriptor = getEEModuleDescriptor(beanDeploymentArchive); // Should not be subBda?
                        if (eeModuleDescriptor != null) {
                            beanDeploymentArchive.getServices().add(EEModuleDescriptor.class, eeModuleDescriptor);
                        }
                    }
                }

                bundleToBeanDeploymentArchive.put(bundle, beanDeploymentArchive);
            }
        }

        context.addTransientAppMetaData(WELD_DEPLOYMENT, deploymentImpl);
        applicationInfo.addTransientAppMetaData(WELD_DEPLOYMENT, deploymentImpl);

        return new WeldApplicationContainer();
    }

    /**
     * Specific stages of the Weld bootstrapping process will execute across different stages of the
     * deployment process. Weld deployment will happen when the load phase of the deployment process is
     * complete. When all modules have been loaded, a deployment graph is produced defining the
     * accessibility relationships between <code>BeanDeploymentArchive</code>s.
     *
     * @param event
     */
    @Override
    public void event(Event<?> event) {
        if (event.is(APPLICATION_LOADED)) {
            processApplicationLoaded((ApplicationInfo) event.hook());
        } else if (isOneOf(event, APPLICATION_STOPPED, APPLICATION_UNLOADED, APPLICATION_DISABLED)) {
            processApplicationStopped((ApplicationInfo) event.hook());
        }
    }

    @Override
    protected void generateArtifacts(DeploymentContext dc) throws DeploymentException {

    }

    @Override
    protected void cleanArtifacts(DeploymentContext dc) throws DeploymentException {

    }

    @Override
    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return null;
    }

    public BeanDeploymentArchive getBeanDeploymentArchiveForBundle(BundleDescriptor bundle) {
        return bundleToBeanDeploymentArchive.get(bundle);
    }

    public boolean isCdiEnabled(BundleDescriptor bundle) {
        return bundleToBeanDeploymentArchive.containsKey(bundle);
    }

    public boolean is299Enabled(BundleDescriptor bundle) {
        return bundleToBeanDeploymentArchive.containsKey(bundle);
    }

    public WeldBootstrap getBootstrapForApp(Application app) {
        return appToBootstrap.get(app);
    }



    // ### Private methods


    private WeldBootstrap getWeldBootstrap(DeploymentContext context, ApplicationInfo appInfo, boolean[] setTransientAppMetaData) {
        // See if a WeldBootsrap has already been created - only want one per app.
        WeldBootstrap bootstrap = context.getTransientAppMetaData(WELD_BOOTSTRAP, WeldBootstrap.class);

        if (bootstrap != null && appInfo.getTransientAppMetaData(WELD_BOOTSTRAP, WeldBootstrap.class) == null) {
            // No bootstrap if no CDI BDAs exist yet
            bootstrap = null;
        }

        if (bootstrap == null) {
            bootstrap = new WeldBootstrap();
            setTransientAppMetaData[0] = true;

            // Stash the WeldBootstrap instance, so we may access the WeldManager later..
            context.addTransientAppMetaData(WELD_BOOTSTRAP, bootstrap);

            // Making sure that if WeldBootstrap is added, shutdown is set to false, as it is/would not have
            // been called.
            appInfo.addTransientAppMetaData(WELD_BOOTSTRAP_SHUTDOWN, "false");
        }

        return bootstrap;
    }

    private void processApplicationLoaded(ApplicationInfo applicationInfo) {
        WeldBootstrap bootstrap = applicationInfo.getTransientAppMetaData(WELD_BOOTSTRAP, WeldBootstrap.class);

        if (bootstrap != null) {
            DeploymentImpl deploymentImpl = applicationInfo.getTransientAppMetaData(WELD_DEPLOYMENT, DeploymentImpl.class);
            deploymentImpl.buildDeploymentGraph();

            List<BeanDeploymentArchive> archives = deploymentImpl.getBeanDeploymentArchives();

            addResourceLoaders(archives);
            addCdiServicesToNonModuleBdas(deploymentImpl.getLibJarRootBdas(), services.getService(InjectionManager.class));
            addCdiServicesToNonModuleBdas(deploymentImpl.getRarRootBdas(), services.getService(InjectionManager.class));

            // Get current TCL
            ClassLoader oldTCL = Thread.currentThread().getContextClassLoader();

            invocationManager.pushAppEnvironment(applicationInfo::getName);

            ComponentInvocation componentInvocation = createComponentInvocation(applicationInfo);

            try {
                invocationManager.preInvoke(componentInvocation);
                bootstrap.startExtensions(postProcessExtensions(deploymentImpl.getExtensions(), archives));
                bootstrap.startContainer(deploymentImpl.getContextId() + ".bda", SERVLET, deploymentImpl);

                //This changes added to pass the following test
                // CreateBeanAttributesTest#testBeanAttributesForSessionBean
                if(!deploymentImpl.getBeanDeploymentArchives().isEmpty()) {
                    BeanDeploymentArchive rootArchive = deploymentImpl.getBeanDeploymentArchives().get(0);
                    ServiceRegistry rootServices = bootstrap.getManager(rootArchive).getServices();
                    EjbSupport originalEjbSupport = rootServices.get(EjbSupport.class);
                    if (originalEjbSupport != null) {
                        // We need to create a proxy instead of a simple wrapper
                        EjbSupport proxyEjbSupport = (EjbSupport) java.lang.reflect.Proxy.newProxyInstance(EjbSupport.class.getClassLoader(),
                                new Class[]{EjbSupport.class}, new java.lang.reflect.InvocationHandler() {
                                    @Override
                                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                                        if (method.getName().equals("isEjb")) {

                                            EjbSupport targetEjbSupport = getTargetEjbSupport((Class<?>) args[0]);

                                            if (targetEjbSupport != null) {
                                                return method.invoke(targetEjbSupport, args);
                                            }

                                        } else if (method.getName().equals("createSessionBeanAttributes")) {
                                            Object enhancedAnnotated = args[0];

                                            Class<?> beanClass = (Class<?>)
                                                    enhancedAnnotated.getClass()
                                                            .getMethod("getJavaClass")
                                                            .invoke(enhancedAnnotated);

                                            EjbSupport targetEjbSupport = getTargetEjbSupport(beanClass);
                                            if (targetEjbSupport != null) {
                                                return method.invoke(targetEjbSupport, args);
                                            }
                                        }

                                        return method.invoke(originalEjbSupport, args);
                                    }

                                    private EjbSupport getTargetEjbSupport(Class<?> beanClass) {
                                        BeanDeploymentArchive ejbArchive = deploymentImpl.getBeanDeploymentArchive(beanClass);
                                        if (ejbArchive == null) {
                                            return null;
                                        }

                                        BeanManagerImpl ejbBeanManager = lookupBeanManager(beanClass, bootstrap.getManager(ejbArchive));

                                        return ejbBeanManager.getServices().get(EjbSupport.class);
                                    }
                                });
                        rootServices.add(EjbSupport.class, proxyEjbSupport);
                    }
                }
                bootstrap.startInitialization();
                fireProcessInjectionTargetEvents(bootstrap, applicationInfo, deploymentImpl);
                bootstrap.deployBeans();
                bootstrap.validateBeans();
                bootstrap.endInitialization();
            } catch (Throwable t) {
                doBootstrapShutdown(applicationInfo);

                throw new DeploymentException(getDeploymentErrorMsgPrefix(t) + t.getMessage(), t);
            } finally {
                try {
                    invocationManager.postInvoke(componentInvocation);
                    invocationManager.popAppEnvironment();
                    deploymentComplete(deploymentImpl);
                } catch (Throwable t) {
                    logger.log(SEVERE, "Exception dispatching post deploy event", t);
                }

                // The TCL is originally the EAR classloader and is reset during Bean deployment to the
                // corresponding module classloader in BeanDeploymentArchiveImpl.getBeans
                // for Bean classloading to succeed.
                // The TCL is reset to its old value here.
                Thread.currentThread().setContextClassLoader(oldTCL);
            }
        }
    }

    private void processApplicationStopped(ApplicationInfo applicationInfo) {

        Application application = applicationInfo.getMetaData(Application.class);
        if (application != null) {
            removeBundleDescriptors(application);
            appToBootstrap.remove(application);
        }

        String shutdown = applicationInfo.getTransientAppMetaData(WELD_SHUTDOWN, String.class);
        if (Boolean.valueOf(shutdown).equals(TRUE)) {
            return;
        }

        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(applicationInfo.getAppClassLoader());

        try {
            WeldBootstrap bootstrap = applicationInfo.getTransientAppMetaData(WELD_BOOTSTRAP, WeldBootstrap.class);
            if (bootstrap != null) {
                invocationManager.pushAppEnvironment(applicationInfo::getName);

                try {
                    doBootstrapShutdown(applicationInfo);
                } catch (Exception e) {
                    logger.log(WARNING, WELD_BOOTSTRAP_SHUTDOWN_EXCEPTION, new Object[] { e });
                } finally {
                    invocationManager.popAppEnvironment();
                }

                applicationInfo.addTransientAppMetaData(WELD_SHUTDOWN, "true");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }

        DeploymentImpl deploymentImpl = applicationInfo.getTransientAppMetaData(WELD_DEPLOYMENT, DeploymentImpl.class);
        if (deploymentImpl != null) {
            deploymentImpl.cleanup();
        }

    }

    private void addResourceLoaders(List<BeanDeploymentArchive> archives) {
        for (BeanDeploymentArchive archive : archives) {
            archive.getServices().add(
                ResourceLoader.class,
                new ResourceLoaderImpl(((BeanDeploymentArchiveImpl) archive).getModuleClassLoaderForBDA()));
        }
    }

    private ComponentInvocation createComponentInvocation(ApplicationInfo applicationInfo) {
        BundleDescriptor bundleDescriptor = getCurrentBundleForContext(deployment.getCurrentDeploymentContext());

        ComponentInvocation componentInvocation = new ComponentInvocation(
                getComponentEnvId((JndiNameEnvironment) bundleDescriptor),
                SERVLET_INVOCATION,
                applicationInfo,
                applicationInfo.getName(),
                applicationInfo.getName(),
                applicationInfo.getName()
        );

        componentInvocation.setJNDIEnvironment(bundleDescriptor);

        return componentInvocation;
    }

    private Iterable<Metadata<Extension>> postProcessExtensions(Iterable<Metadata<Extension>> extensions, List<BeanDeploymentArchive> archives) {

        // See if the Jersey extension that scans all classes in the bean archives is present.
        // Normally this should always be the case as this extension is statically included with Jersey,
        // but who knows whether we'll once have distributions without Jersey
        Optional<Metadata<Extension>> optionaProcessAllAnnotatedTypes = findJerseyProcessAll(extensions);

        if (optionaProcessAllAnnotatedTypes.isPresent()) {

            // Get the Metadata instance containing the above mentioned extension
            Metadata<Extension> processAllMeta = optionaProcessAllAnnotatedTypes.get();

            // Get the class loader used by the JAX_RS (Jersey) archive, so we can use this to load
            // other classes from that same archive
            ClassLoader jaxRsClassLoader = processAllMeta.getValue().getClass().getClassLoader();

            try {
                // The reason Jersey scans ALL classes is that a Hk2CustomBoundTypesProvider may be present. But if
                // this
                // is not there Jersey only has to scan the known JAX-RS classes (@Path etc)
                if (!hasJerseyHk2Provider(archives, jaxRsClassLoader)) {
                    // Hk2CustomBoundTypesProvider not found, replace the extension that scans everything with
                    // the one that only scans JAX-RS classes
                    return replaceWith(extensions, processAllMeta, newProcessJaxRsMeta(jaxRsClassLoader));
                }
            } catch (Exception e) {
                // Just log, the net result is only that the extension does a little extra scanning and logs a
                // warning
                // about that
                logger.log(FINE, "Exception trying to replace JaxRs scan all extension", e);
            }
        }

        // If the scan-all extension shouldn't or couldn't be replaced, just return the existing unmodified
        // list
        return extensions;
    }

    private Optional<Metadata<Extension>> findJerseyProcessAll(Iterable<Metadata<Extension>> extensions) {
        return stream(extensions.spliterator(), false)
                .filter(e -> e.getValue().getClass().getName().equals(JERSEY_PROCESS_ALL_CLASS_NAME)).findAny();
    }

    private boolean hasJerseyHk2Provider(List<BeanDeploymentArchive> archives, ClassLoader jaxRsClassLoader)
            throws ClassNotFoundException {
        Class<?> hk2Provider = Class.forName(JERSEY_HK2_CLASS_NAME, false, jaxRsClassLoader);

        for (BeanDeploymentArchive archive : archives) {
            archive.getBeanClasses(); // implicitly sets thread class loader
            if (ServiceLoader.load(hk2Provider).iterator().hasNext()) {
                return true;
            }
        }

        return false;
    }

    private Iterable<Metadata<Extension>> replaceWith(Iterable<Metadata<Extension>> extensions, Metadata<Extension> processAllMeta, Metadata<Extension> processJaxRsMeta) {
        return stream(extensions.spliterator(), false).map(e -> e.equals(processAllMeta) ? processJaxRsMeta : e)
                .collect(Collectors.toList());
    }

    private Metadata<Extension> newProcessJaxRsMeta(ClassLoader jaxRsClassLoader) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Extension processJAXRSAnnotatedTypes = (Extension) Class.forName(JERSEY_PROCESS_JAXRS_CLASS_NAME, false, jaxRsClassLoader)
                .newInstance();

        return new MetadataImpl<>(processJAXRSAnnotatedTypes);
    }

    @SafeVarargs
    private static final boolean isOneOf(Event<?> event, EventTypes<ApplicationInfo>... eventTypes) {
        return stream(eventTypes).anyMatch(e -> event.is(e));
    }

    private void removeBundleDescriptors(Application application) {
        for (BundleDescriptor bundleDescriptor : application.getBundleDescriptors()) {
            if (bundleDescriptor instanceof EjbBundleDescriptor || bundleDescriptor instanceof WebBundleDescriptor) {
                bundleToBeanDeploymentArchive.remove(bundleDescriptor);
            }
        }
    }

    private void deploymentComplete(DeploymentImpl deploymentImpl) {
        for (BeanDeploymentArchive beanDeploymentArchive : deploymentImpl.getBeanDeploymentArchives()) {
            ((BeanDeploymentArchiveImpl) beanDeploymentArchive).setDeploymentComplete(true);
        }
    }

    private void doBootstrapShutdown(ApplicationInfo applicationInfo) {
        WeldBootstrap bootstrap = applicationInfo.getTransientAppMetaData(WELD_BOOTSTRAP, WeldBootstrap.class);
        String bootstrapShutdown = applicationInfo.getTransientAppMetaData(WELD_BOOTSTRAP_SHUTDOWN, String.class);

        if (bootstrapShutdown == null || Boolean.valueOf(bootstrapShutdown).equals(FALSE)) {
            bootstrap.shutdown();
            applicationInfo.addTransientAppMetaData(WELD_BOOTSTRAP_SHUTDOWN, "true");
        }
    }

    private String getArchiveName(DeploymentContext context, ApplicationInfo applicationInfo, ReadableArchive archive) {
        // Create a Deployment Collecting Information From The ReadableArchive (archive)
        // if archive is a composite, or has version numbers per maven conventions, strip it out
        boolean isSubArchive = archive.getParentArchive() != null;

        String archiveName = !isSubArchive ? applicationInfo.getName() : archive.getName();
        if (isSubArchive) {
            archiveName = stripMavenVersion(archiveName);
        }

        if (!context.getArchiveHandler().getArchiveType().isEmpty()) {
            archiveName = String.format("%s.%s",
                            stripApplicationVersion(archiveName),
                            context.getArchiveHandler().getArchiveType());
        }

        return archiveName;
    }

    private String getDeploymentErrorMsgPrefix(Throwable t) {
        if (t instanceof jakarta.enterprise.inject.spi.DefinitionException) {
            return "CDI definition failure:";
        } else if (t instanceof jakarta.enterprise.inject.spi.DeploymentException) {
            return "CDI deployment failure:";
        } else {
            Throwable cause = t.getCause();
            if (cause == t || cause == null) {
                return "CDI deployment failure:";
            } else {
                return getDeploymentErrorMsgPrefix(cause);
            }
        }
    }

    /*
     * We are only firing ProcessInjectionTarget<X> for non-contextual EE components and not using the
     * InjectionTarget<X> from the event during instance creation in JCDIServiceImpl.java TODO weld
     * would provide a better way to do this, otherwise we may need TODO to store InjectionTarget<X> to
     * be used in instance creation
     */
    private void fireProcessInjectionTargetEvents(WeldBootstrap bootstrap, ApplicationInfo applicationInfo,
            DeploymentImpl deploymentImpl) {
        List<BeanDeploymentArchive> beanDeploymentArchives = deploymentImpl.getBeanDeploymentArchives();

        Class<?> messageListenerClass = getMessageListenerClass();

        // Web-Profile and other lighter distributions would not ship the JMS
        // API and implementations.
        // So, the weld-integration layer cannot have a direct dependency on the JMS API
        boolean isFullProfile = messageListenerClass != null;

        for (BeanDeploymentArchive beanDeploymentArchive : beanDeploymentArchives) {
            Collection<Class<?>> beanClassObjects = ((BeanDeploymentArchiveImpl) beanDeploymentArchive).getBeanClassObjects();
            for (Class<?> bdaClazz : beanClassObjects) {
                for (Class<?> nonClazz : NON_CONTEXT_CLASSES) {
                    if (nonClazz.isAssignableFrom(bdaClazz)) {
                        firePITEvent(bootstrap, beanDeploymentArchive, bdaClazz);
                    }
                }

                // For distributions that have the JMS API, an MDB is a valid non-contextual EE component to which we have to
                // fire <code>ProcessInjectionTarget</code> events (see GLASSFISH-16730)
                if (isFullProfile && messageListenerClass.isAssignableFrom(bdaClazz)) {
                    if (logger.isLoggable(FINE)) {
                        logger.log(FINE, MDB_PIT_EVENT, new Object[] { bdaClazz });
                    }

                    firePITEvent(bootstrap, beanDeploymentArchive, bdaClazz);
                }
            }

            // Fix for CDI TCK test: ContainerEventTest#testProcessInjectionTargetEventFiredForServlet
            // Check for Servlets which have not yet been loaded and haven't been identified as Beans
            // From the spec: "The container must also fire an event for every Jakarta EE component class supporting
            // injection that may be instantiated by the container at runtime". Stress on the "may".
            Collection<String> injectionTargetClassNames = WeldUtils.getInjectionTargetClassNames(
                    deploymentImpl.getTypes(), beanDeploymentArchive.getKnownClasses());
            for (String injectionTargetClassName : injectionTargetClassNames) {
                // Don't fire twice
                if (beanDeploymentArchive.getBeanClasses().contains(injectionTargetClassName)) {
                    continue;
                }

                Class<?> injectionTargetClass = null;
                try {
                    injectionTargetClass = applicationInfo.getAppClassLoader().loadClass(injectionTargetClassName);
                } catch (ClassNotFoundException appClassLoaderClassNotFoundException) {
                    try {
                        logger.log(Level.FINE, "Caught exception loading class using application class loader, " +
                                "trying again with module class loader");
                        injectionTargetClass = applicationInfo.getModuleClassLoader().loadClass(injectionTargetClassName);
                    } catch (ClassNotFoundException moduleClassLoaderClassNotFoundException) {
                        logger.log(Level.FINE, "Caught exception loading class using module class loader, " +
                                "ProcessInjectionTarget event may not get fired for " + injectionTargetClassName);
                    }
                }

                if (injectionTargetClass != null) {
                    for (Class<?> nonClazz : NON_CONTEXT_CLASSES) {
                        if (nonClazz.isAssignableFrom(injectionTargetClass)) {
                            firePITEvent(bootstrap, beanDeploymentArchive, injectionTargetClass);
                        }
                    }

                    if(isFullProfile && messageListenerClass.isAssignableFrom(injectionTargetClass)) {
                        firePITEvent(bootstrap, beanDeploymentArchive, injectionTargetClass);
                    }
                }
            }
        }
    }

    private Class<?> getMessageListenerClass() {
        try {
            Class<?> messageListenerClass = Thread.currentThread().getContextClassLoader().loadClass("jakarta.jms.MessageListener");
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, JMS_MESSAGElISTENER_AVAILABLE);
            }

            return messageListenerClass;
        } catch (ClassNotFoundException cnfe) {
            // ignore cnfe
        }

        return null;
    }

    private void firePITEvent(WeldBootstrap bootstrap, BeanDeploymentArchive beanDeploymentArchive, Class<?> bdaClazz) {
        // Fix for issue GLASSFISH-17464
        // The PIT event should not be fired for interfaces
        if (bdaClazz.isInterface()) {
            return;
        }

        AnnotatedType<?> annotatedType = bootstrap.getManager(beanDeploymentArchive).createAnnotatedType(bdaClazz);
        ((BeanDeploymentArchiveImpl) beanDeploymentArchive).
            putInjectionTarget(
                annotatedType,
                bootstrap.getManager(beanDeploymentArchive).fireProcessInjectionTarget(annotatedType));
    }

    private EEModuleDescriptor getEEModuleDescriptor(BeanDeploymentArchive beanDeploymentArchive) {
        EEModuleDescriptor eeModuleDescriptor = null;
        if (beanDeploymentArchive instanceof BeanDeploymentArchiveImpl) {
            WeldUtils.BDAType bdaType = ((BeanDeploymentArchiveImpl) beanDeploymentArchive).getBDAType();
            if (bdaType.equals(WeldUtils.BDAType.JAR)) {
                eeModuleDescriptor = new EEModuleDescriptorImpl(beanDeploymentArchive.getId(),
                        EEModuleDescriptor.ModuleType.EJB_JAR);
            } else if (bdaType.equals(WeldUtils.BDAType.WAR)) {
                eeModuleDescriptor = new EEModuleDescriptorImpl(beanDeploymentArchive.getId(), EEModuleDescriptor.ModuleType.WEB);
            } else if (bdaType.equals(WeldUtils.BDAType.RAR)) {
                eeModuleDescriptor = new EEModuleDescriptorImpl(beanDeploymentArchive.getId(),
                        EEModuleDescriptor.ModuleType.CONNECTOR);
            }
        }

        return eeModuleDescriptor;
    }

    private boolean isDevelopmentMode(DeploymentContext context) {
        boolean devMode = WeldUtils.isCDIDevModeEnabled(context) || Boolean.getBoolean(DEV_MODE_PROPERTY);
        WebBundleDescriptor wDesc = context.getModuleMetaData(WebBundleDescriptor.class);
        if (!devMode && wDesc != null) {
            Enumeration<ContextParameter> cpEnumeration = wDesc.getContextParameters();
            while (cpEnumeration.hasMoreElements()) {
                ContextParameter param = cpEnumeration.nextElement();
                if (DEV_MODE_PROPERTY.equals(param.getName()) && Boolean.valueOf(param.getValue())) {
                    devMode = true;
                    WeldUtils.setCDIDevMode(context, devMode);
                    break;
                }
            }
        }
        return devMode;
    }

    private void registerWeldConversationFilter(WebBundleDescriptor webBundleDescriptor) {
        for (ServletFilterMapping filterMapping : webBundleDescriptor.getServletFilterMappings()) {
            if (WELD_CONVERSATION_FILTER_NAME.equals(((ServletFilterMappingDescriptor) filterMapping).getDisplayName())) {

                ServletFilterDescriptor servletFilter = new ServletFilterDescriptor();
                servletFilter.setClassName(WELD_CONVERSATION_FILTER_CLASS);
                servletFilter.setName(WELD_CONVERSATION_FILTER_NAME);
                webBundleDescriptor.addServletFilter(servletFilter);

                break;
            }
        }
    }

    private void registerProbeFilter(WebBundleDescriptor webBundleDescriptor) {
        ServletFilterDescriptor servletFilter = new ServletFilterDescriptor();
        servletFilter.setClassName(PROBE_FILTER_CLASS_NAME);
        servletFilter.setName(PROBE_FILTER_NAME);
        servletFilter.setAsyncSupported(PROBE_FILTER_ASYNC_SUPPORT);
        webBundleDescriptor.addServletFilter(servletFilter);

        ServletFilterMappingDescriptor servletFilterMapping = new ServletFilterMappingDescriptor();
        servletFilterMapping.setName(PROBE_FILTER_NAME);
        servletFilterMapping.addURLPattern(PROBE_FILTER_URL_PATTERN);
        servletFilterMapping.addDispatcher(PROBE_FILTER_DISPATCHER_TYPE);
        webBundleDescriptor.addServletFilterMapping(servletFilterMapping);
    }

    private void registerProbeExtension(ExternalConfigurationImpl externalConfiguration, DeploymentImpl deploymentImpl) {
        externalConfiguration.setProbeEventMonitorExcludeType(PROBE_EVENT_MONITOR_EXCLUDE_TYPE);
        externalConfiguration.setProbeInvocationMonitorExcludeType(PROBE_INVOCATION_MONITOR_EXCLUDE_TYPE);
        externalConfiguration.setProbeAllowRemoteAddress(PROBE_ALLOW_REMOTE_ADDRESS);
        deploymentImpl.addDynamicExtension(createProbeExtension());
    }

    private void configureConcurrentDeployment(DeploymentContext context, ExternalConfigurationImpl configuration) {
        configuration.setConcurrentDeployment(WeldUtils.isConcurrentDeploymentEnabled());
        configuration.setPreLoaderThreadPoolSize(WeldUtils.getPreLoaderThreads());
    }

    private Metadata<Extension> createProbeExtension() {
        ProbeExtension probeExtension;
        Class<ProbeExtension> probeExtensionClass = ProbeExtension.class;
        try {
            if (System.getSecurityManager() != null) {
                probeExtension = AccessController.doPrivileged(NewInstanceAction.of(probeExtensionClass));
            } else {
                probeExtension = probeExtensionClass.newInstance();
            }
        } catch (Exception e) {
            throw new WeldException(e.getCause());
        }

        return new MetadataImpl<Extension>(probeExtension, "N/A");
    }

    private void addWeldListenerToAllWars(DeploymentContext context) {
        // if there's at least 1 ejb jar then add the listener to all wars
        ApplicationHolder applicationHolder = context.getModuleMetaData(ApplicationHolder.class);
        if (applicationHolder != null) {
            if (applicationHolder.app.getBundleDescriptors(EjbBundleDescriptor.class).size() > 0) {
                Set<WebBundleDescriptor> webBundleDescriptors = applicationHolder.app
                        .getBundleDescriptors(WebBundleDescriptor.class);
                for (WebBundleDescriptor oneWebBundleDescriptor : webBundleDescriptors) {
                    // Add the Weld Listener if it does not already exist..
                    // we have to do this regardless because the war may not be cdi-enabled but an ejb is.
                    oneWebBundleDescriptor.addAppListenerDescriptorToFirst(new AppListenerDescriptorImpl(WELD_LISTENER));
                    oneWebBundleDescriptor.addAppListenerDescriptor(
                            new AppListenerDescriptorImpl(WeldTerminationListenerProxy.class.getName()));
                }
            }
        }
    }

    private EjbBundleDescriptor getEjbBundleFromContext(DeploymentContext context) {

        EjbBundleDescriptor ejbBundle = context.getModuleMetaData(EjbBundleDescriptor.class);

        if (ejbBundle == null) {

            WebBundleDescriptor wDesc = context.getModuleMetaData(WebBundleDescriptor.class);
            if (wDesc != null) {
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
        if (injectionMgr != null && rootBdas != null) {
            while (rootBdas.hasNext()) {
                RootBeanDeploymentArchive oneRootBda = rootBdas.next();
                addCdiServicesToBda(injectionMgr, oneRootBda);
                addCdiServicesToBda(injectionMgr, oneRootBda.getModuleBda());
            }
        }
    }

    private void addCdiServicesToBda(InjectionManager injectionMgr, BeanDeploymentArchive bda) {
        InjectionServices injectionServices = new NonModuleInjectionServices(injectionMgr);
        bda.getServices().add(InjectionServices.class, injectionServices);
    }
}
