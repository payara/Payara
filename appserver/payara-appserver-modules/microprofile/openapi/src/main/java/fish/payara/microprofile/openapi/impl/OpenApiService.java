/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.openapi.impl;

import com.sun.enterprise.v3.server.ApplicationLifecycle;
import com.sun.enterprise.v3.services.impl.GrizzlyService;
import fish.payara.microprofile.openapi.api.OpenAPIBuildException;
import fish.payara.microprofile.openapi.impl.admin.OpenApiServiceConfiguration;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor;
import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;
import fish.payara.microprofile.openapi.impl.processor.FileProcessor;
import fish.payara.microprofile.openapi.impl.processor.FilterProcessor;
import fish.payara.microprofile.openapi.impl.processor.ModelReaderProcessor;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.UnprocessedChangeEvents;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toSet;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;

@Service(name = "microprofile-openapi-service")
@RunLevel(StartupRunLevel.VAL)
public class OpenApiService implements PostConstruct, PreDestroy, EventListener, ConfigListener {

    private static final Logger LOGGER = Logger.getLogger(OpenApiService.class.getName());

    private OpenAPI allDocuments;

    private Deque<OpenApiMapping> mappings;

    @Inject
    private Events events;

    @Inject
    private OpenApiServiceConfiguration config;

    @Inject
    private ServerEnvironment environment;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private ApplicationLifecycle applicationLifecycle;

    @Override
    public void postConstruct() {
        mappings = new ConcurrentLinkedDeque<>();
        events.register(this);
    }

    @Override
    public void preDestroy() {
        events.unregister(this);
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(config.getEnabled());
    }
    
    public boolean isSecurityEnabled() {
        return Boolean.parseBoolean(config.getSecurityEnabled());
    }

    public boolean withCorsHeaders() {
        return Boolean.parseBoolean(config.getCorsHeaders());
    }

    /**
     * Listen for OpenAPI config changes.
     */
    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] event) {
        return ConfigSupport.sortAndDispatch(event, new Changed() {
            @Override
            public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> tClass, T t) {
                if (tClass == OpenApiServiceConfiguration.class) {
                    if (type == TYPE.CHANGE) {
                        if (isEnabled()) {
                            LOGGER.info("OpenAPIService enabled.");
                        } else {
                            LOGGER.info("OpenAPIService disabled.");
                        }
                    }
                }
                return null;
            }
        }, LOGGER);
    }

    /**
     * Listen for application deployment events.
     */
    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.APPLICATION_STARTED)) {
            // Get the application information
            ApplicationInfo appInfo = (ApplicationInfo) event.hook();

            // Create all the relevant resources
            if (isValidApp(appInfo)) {
                // Store the application mapping in the list
                OpenApiMapping mapping = new OpenApiMapping(appInfo);
                mappings.add(mapping);
                allDocuments = null;
            }
        } else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo appInfo = (ApplicationInfo) event.hook();
            for (OpenApiMapping mapping : mappings) {
                if (mapping.getAppInfo().equals(appInfo)) {
                    mappings.remove(mapping);
                    allDocuments = null;
                    break;
                }
            }
        }
    }

    /**
     * @return the document If multiple application deployed then merge all the
     * documents. Creates one if it hasn't already been created.
     * @throws OpenAPIBuildException if creating the document failed.
     * @throws java.io.IOException if source archive not accessible
     */
    public OpenAPI getDocument() throws OpenAPIBuildException, IOException {
        if (mappings.isEmpty() || !isEnabled()) {
            return null;
        }
        if (mappings.size() == 1) {
            OpenAPI document = mappings.peekLast().getDocument();
            if (document == null) {
                document = mappings.peekLast().buildDocument();
            }
            return document;
        }
        List<OpenAPI> docs = new ArrayList<>();
        for (OpenApiMapping mapping : mappings) {
            if (mapping.getDocument() == null) {
                allDocuments = null;
                mapping.buildDocument();
            }
            docs.add(mapping.getDocument());
        }
        if (allDocuments == null) {
            allDocuments = new OpenAPIImpl();
            OpenAPIImpl.merge(allDocuments, docs, true);
        }
        return allDocuments;
    }

    /**
     * @return an instance of this service from HK2.
     */
    public static OpenApiService getInstance() {
        return Globals.getStaticBaseServiceLocator().getService(OpenApiService.class);
    }

    /**
     * @param appInfo the application descriptor.
     * @return boolean if the app is a valid target for an OpenAPI document.
     */
    private static boolean isValidApp(ApplicationInfo appInfo) {
        return appInfo.getMetaData(WebBundleDescriptorImpl.class) != null
                && !appInfo.getSource().getURI().getPath().contains("glassfish/lib/install")
                && !appInfo.getSource().getURI().getPath().contains("h2db/bin")
                && !appInfo.getSource().getURI().getPath().contains("mq/lib");
    }

    /**
     * @param appInfo the application descriptor.
     * @return boolean the context root of the application.
     */
    private static String getContextRoot(ApplicationInfo appInfo) {
        return appInfo.getMetaData(WebBundleDescriptorImpl.class).getContextRoot();
    }

    private class OpenApiMapping {

        private final ApplicationInfo appInfo;
        private final OpenApiConfiguration appConfig;
        private volatile OpenAPI document;

        OpenApiMapping(ApplicationInfo appInfo) {
            this.appInfo = appInfo;
            this.appConfig = new OpenApiConfiguration(appInfo.getAppClassLoader());
        }

        ApplicationInfo getAppInfo() {
            return appInfo;
        }

        private OpenAPI getDocument() throws OpenAPIBuildException {
            return document;
        }

        private synchronized OpenAPI buildDocument() throws OpenAPIBuildException, IOException {
            if (this.document != null) {
                return this.document;
            }

            Parser parser = applicationLifecycle.getDeployableParser(
                    appInfo.getSource(),
                    true,
                    true,
                    StructuredDeploymentTracing.create(appInfo.getName()),
                    LOGGER
            );
            Types types = parser.getContext().getTypes();

            OpenAPI doc = new OpenAPIImpl();
            try {
                String contextRoot = getContextRoot(appInfo);
                List<URL> baseURLs = getServerURL(contextRoot);
                doc = new ModelReaderProcessor().process(doc, appConfig);
                doc = new FileProcessor(appInfo.getAppClassLoader()).process(doc, appConfig);
                doc = new ApplicationProcessor(
                        types,
                        filterTypes(appInfo, appConfig, types),
                        appInfo.getAppClassLoader()
                ).process(doc, appConfig);
                doc = new BaseProcessor(baseURLs).process(doc, appConfig);
                doc = new FilterProcessor().process(doc, appConfig);
            } catch (Throwable t) {
                throw new OpenAPIBuildException(t);
            } finally {
                this.document = doc;
            }

            LOGGER.info("OpenAPI document created.");
            return this.document;
        }

    }

    /**
     * @return a list of all classes in the archive.
     */
    private Set<Type> filterTypes(ApplicationInfo appInfo, OpenApiConfiguration config, Types hk2Types) {
        ReadableArchive archive = appInfo.getSource();
        Set<Type> types = new HashSet<>(filterLibTypes(config, hk2Types, archive));
        types.addAll(
                Collections.list(archive.entries()).stream()
                        // Only use the classes
                        .filter(clazz -> clazz.endsWith(".class"))
                        // Remove the WEB-INF/classes and return the proper class name format
                        .map(clazz -> clazz.replaceAll("WEB-INF/classes/", "").replace("/", ".").replace(".class", ""))
                        // Fetch class type
                        .map(clazz -> hk2Types.getBy(clazz))
                        // Don't return null classes
                        .filter(Objects::nonNull)
                        .collect(toSet())
        );
        return config == null ? types : config.getValidClasses(types);
    }

    private Set<Type> filterLibTypes(
            OpenApiConfiguration config,
            Types hk2Types,
            ReadableArchive archive) {
        Set<Type> types = new HashSet<>();
        if (config != null && config.getScanLib()) {
            Enumeration<String> subArchiveItr = archive.entries();
            while (subArchiveItr.hasMoreElements()) {
                String subArchiveName = subArchiveItr.nextElement();
                if (subArchiveName.startsWith("WEB-INF/lib/") && subArchiveName.endsWith(".jar")) {
                    try {
                        ReadableArchive subArchive = archive.getSubArchive(subArchiveName);
                        types.addAll(
                                Collections.list(subArchive.entries())
                                        .stream()
                                        // Only use the classes
                                        .filter(clazz -> clazz.endsWith(".class"))
                                        // return the proper class name format
                                        .map(clazz -> clazz.replace("/", ".").replace(".class", ""))
                                        // Fetch class type
                                        .map(clazz -> hk2Types.getBy(clazz))
                                        // Don't return null classes
                                        .filter(Objects::nonNull)
                                        .collect(toSet())
                        );
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
        }
        return types;
    }

    private List<URL> getServerURL(String contextRoot) {
        List<URL> result = new ArrayList<>();
        ServerContext context = Globals.get(ServerContext.class);

        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException ex) {
            hostName = "localhost";
        }

        String instanceType = environment.getRuntimeType().toString();
        List<Integer> httpPorts = new ArrayList<>();
        List<Integer> httpsPorts = new ArrayList<>();
        List<NetworkListener> networkListeners = context.getConfigBean().getConfig().getNetworkConfig().getNetworkListeners().getNetworkListener();
        String adminListener = context.getConfigBean().getConfig().getAdminListener().getName();
        networkListeners
                .stream()
                .filter(networkListener -> Boolean.parseBoolean(networkListener.getEnabled()))
                .forEach(networkListener -> {

                    int port;
                    try {
                        // get the dynamic config port
                        port = habitat.getService(GrizzlyService.class).getRealPort(networkListener);
                    } catch (MultiException ex) {
                        LOGGER.log(WARNING, "Failed to get running Grizzly listener.", ex);
                        // get the port in the domain xml
                        port = Integer.parseInt(networkListener.getPort());
                    }

                    // Check if this listener is using HTTP or HTTPS
                    boolean securityEnabled = Boolean.parseBoolean(networkListener.findProtocol().getSecurityEnabled());
                    List<Integer> ports = securityEnabled ? httpsPorts : httpPorts;

                    // If this listener isn't the admin listener, it must be an HTTP/HTTPS listener
                    if (!networkListener.getName().equals(adminListener)) {
                        ports.add(port);
                    } else if (instanceType.equals("MICRO")) {
                        // micro instances can use the admin listener as both an admin and HTTP/HTTPS port
                        ports.add(port);
                    }
                });

        for (Integer httpPort : httpPorts) {
            try {
                result.add(new URL("http", hostName, httpPort, contextRoot));
            } catch (MalformedURLException ex) {
                // ignore
            }
        }
        for (Integer httpsPort : httpsPorts) {
            try {
                result.add(new URL("https", hostName, httpsPort, contextRoot));
            } catch (MalformedURLException ex) {
                // ignore
            }
        }
        return result;
    }

}
