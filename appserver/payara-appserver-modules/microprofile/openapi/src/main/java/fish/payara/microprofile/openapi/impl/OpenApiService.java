/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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

import static java.util.stream.Collectors.toSet;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
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

import fish.payara.microprofile.openapi.impl.admin.OpenApiServiceConfiguration;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor;
import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;
import fish.payara.microprofile.openapi.impl.processor.FileProcessor;
import fish.payara.microprofile.openapi.impl.processor.FilterProcessor;
import fish.payara.microprofile.openapi.impl.processor.ModelReaderProcessor;

@Service(name = "microprofile-openapi-service")
@RunLevel(StartupRunLevel.VAL)
public class OpenApiService implements PostConstruct, PreDestroy, EventListener, ConfigListener {

    private static final Logger LOGGER = Logger.getLogger(OpenApiService.class.getName());

    private Deque<Map<ApplicationInfo, OpenAPI>> models;

    @Inject
    private Events events;

    @Inject
    private OpenApiServiceConfiguration config;

    @Override
    public void postConstruct() {
        models = new ConcurrentLinkedDeque<>();
        events.register(this);
    }

    @Override
    public void preDestroy() {
        events.unregister(this);
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(config.getEnabled());
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
            if (isEnabled() && isValidApp(appInfo)) {
                // Create the OpenAPI config
                OpenApiConfiguration appConfig = new OpenApiConfiguration(appInfo.getAppClassLoader());

                // Map the application info to a new openapi document, and store it in the list
                Map<ApplicationInfo, OpenAPI> map = Collections.singletonMap(appInfo,
                        createOpenApiDocument(appInfo, appConfig));
                models.add(map);

                LOGGER.info("OpenAPI document created.");
            }
        } else if (event.is(Deployment.APPLICATION_UNLOADED)) {
            ApplicationInfo appInfo = (ApplicationInfo) event.hook();
            for (Map<ApplicationInfo, OpenAPI> map : models) {
                if (map.keySet().toArray()[0].equals(appInfo)) {
                    models.remove(map);
                    break;
                }
            }
        }
    }

    /**
     * Gets the document for the most recently deployed application.
     */
    public OpenAPI getDocument() {
        if (models.isEmpty()) {
            return null;
        }
        OpenAPI lastDocument = null;
        for (Map<ApplicationInfo, OpenAPI> model : models)
            lastDocument = (OpenAPI) model.values().toArray()[0];
        return lastDocument;
    }

    private OpenAPI createOpenApiDocument(ApplicationInfo appInfo, OpenApiConfiguration config) {
        OpenAPI document = new OpenAPIImpl();

        String contextRoot = getContextRoot(appInfo);
        ReadableArchive archive = appInfo.getSource();
        Set<Class<?>> classes = getClassesFromArchive(archive, appInfo.getAppClassLoader());

        document = new ModelReaderProcessor().process(document, config);
        document = new FileProcessor(appInfo.getAppClassLoader()).process(document, config);
        document = new ApplicationProcessor(classes).process(document, config);
        document = new BaseProcessor(contextRoot).process(document, config);
        document = new FilterProcessor().process(document, config);
        return document;
    }

    /**
     * Retrieves an instance of this service from HK2.
     */
    public static OpenApiService getInstance() {
        return Globals.getStaticBaseServiceLocator().getService(OpenApiService.class);
    }

    /**
     * @param appInfo the application descriptor.
     * @return boolean if the app is a valid target for an OpenAPI document.
     */
    private static boolean isValidApp(ApplicationInfo appInfo) {
        return appInfo.getMetaData(WebBundleDescriptorImpl.class) != null;
    }

    /**
     * @param appInfo the application descriptor.
     * @return boolean the context root of the application.
     */
    private static String getContextRoot(ApplicationInfo appInfo) {
        return appInfo.getMetaData(WebBundleDescriptorImpl.class).getContextRoot();
    }

    /**
     * @param archive the archive to read from.
     * @param appClassLoader the classloader to use to load the classes.
     * @return a list of all loadable classes in the archive.
     */
    private static Set<Class<?>> getClassesFromArchive(ReadableArchive archive, ClassLoader appClassLoader) {
        return Collections.list((Enumeration<String>) archive.entries()).stream()
                // Only use the classes
                .filter(x -> x.endsWith(".class"))
                // Remove the WEB-INF/classes and return the proper class name format
                .map(x -> x.replaceAll("WEB-INF/classes/", "").replace("/", ".").replace(".class", ""))
                // Attempt to load the classes
                .map(x -> {
                    Class<?> loadedClass = null;
                    // Attempt to load the class, ignoring any errors
					try {
						loadedClass = appClassLoader.loadClass(x);
					} catch (Throwable t) {
                    }
					try {
						loadedClass = Class.forName(x);
					} catch (Throwable t) {
                    }
                    return loadedClass;
                })
                // Don't return null classes
                .filter(x -> x != null)
                .collect(toSet());
    }

}