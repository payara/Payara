/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2024 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.sun.enterprise.v3.server.ApplicationLifecycle;
import com.sun.enterprise.v3.services.impl.GrizzlyService;

import fish.payara.microprofile.openapi.impl.model.util.ModelUtils;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.Type;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;

import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.processor.ApplicationProcessor;
import fish.payara.microprofile.openapi.impl.processor.BaseProcessor;
import fish.payara.microprofile.openapi.impl.processor.ConfigPropertyProcessor;
import fish.payara.microprofile.openapi.impl.processor.FileProcessor;
import fish.payara.microprofile.openapi.impl.processor.FilterProcessor;
import fish.payara.microprofile.openapi.impl.processor.ModelReaderProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class OpenAPISupplier implements Supplier<OpenAPI> {

    private static final Logger logger = Logger.getLogger(OpenAPISupplier.class.getName());

    private final OpenApiConfiguration config;
    private final String applicationId;
    private final String contextRoot;
    private final ReadableArchive archive;
    private final ClassLoader classLoader;

    private volatile OpenAPI document;

    private boolean enabled;

    public OpenAPISupplier(String applicationId, String contextRoot,
            ReadableArchive archive, ClassLoader classLoader) {
        this.config = new OpenApiConfiguration(classLoader);
        this.applicationId = applicationId;
        this.contextRoot = contextRoot;
        this.archive = archive;
        this.classLoader = classLoader;
        this.enabled = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public synchronized OpenAPI get() {
        if (this.document != null) {
            return this.document;
        }
        if (!enabled) {
            return null;
        }

        try {
            // collect types from this WAR project, including WEB-INF/lib jars
            Map<String, Type> types = collectTypesFromArchive(archive, applicationId);

            // if configured to scan libs, scan libs of the parent EAR (if available)
            if (config != null && config.getScanLib()) {
                Map<String, Type> earLibTypes = collectEarLibTypes(archive.getParentArchive());
                types.putAll(earLibTypes);
            }

            OpenAPI doc = new OpenAPIImpl();
            try {
                final List<URL> baseURLs = getServerURL(contextRoot);
                doc = new ConfigPropertyProcessor().process(doc, config);
                doc = new ModelReaderProcessor().process(doc, config);
                doc = new FileProcessor(classLoader).process(doc, config);
                doc = new ApplicationProcessor(
                        types,
                        filterTypes(archive, types, config != null && config.getScanLib()),
                        filterTypes(archive, types, false),
                        classLoader
                ).process(doc, config);
                if (doc.getPaths() != null && !doc.getPaths().getPathItems().isEmpty()) {
                    ((OpenAPIImpl) doc).setEndpoints(ModelUtils.buildEndpoints(null, contextRoot,
                            doc.getPaths().getPathItems().keySet()));
                }
                doc = new BaseProcessor(baseURLs).process(doc, config);
                doc = new FilterProcessor().process(doc, config);
            } finally {
                this.document = doc;
            }

            return this.document;
        } catch (Exception ex) {
            throw new RuntimeException("An error occurred while creating the OpenAPI document.", ex);
        }
    }

    private Map<String, Type> collectEarLibTypes(ReadableArchive parentArchive) {
        Map<String, Type> earLibTypes = new HashMap<>();
        if (parentArchive != null) {
            Enumeration<String> entries = parentArchive.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement();
                // scan only lib/*.jar libraries
                if (entry.startsWith("lib/") && entry.endsWith(".jar")) {
                    try {
                        Map<String, Type> libTypes = collectTypesFromArchive(parentArchive.getSubArchive(entry),
                                applicationId + "-parent-" + entry);
                        earLibTypes.putAll(libTypes);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Unable to parse EAR archive '" + entry + "': " + ex.getMessage(), ex);
                    }
                }
            }
        }
        return earLibTypes;
    }

    private Map<String, Type> collectTypesFromArchive(ReadableArchive archive, String entryId) throws IOException {
        Map<String, Type> types = new HashMap<>();
        Parser earLibParser;
        earLibParser = Globals.get(ApplicationLifecycle.class).getDeployableParser(archive,
                true,
                true,
                StructuredDeploymentTracing.create(entryId),
                logger
        );
        types.putAll(typesToMap(earLibParser.getContext().getTypes(), archive.getURI()));
        return types;
    }

    public static Map<String, Type> typesToMap(Types types, URI archive) {
        return types.getAllTypes().stream()
                // We only care about classes defined by the application. With the switch to OSGi R8 and allowing Felix
                // to import/export JDK classes we need to filter out said JDK classes as (currently) HK2 does not do
                // so for us. Collecting to a map based on name without filtering would lead to a conflict between
                // the ClassModel and the 'xType' e.g. the ClassModel for the 'Enum' class and the 'EnumType'
                // used for modelling things of type 'Enum' would both have the name 'java.lang.Enum'
                .filter(type -> type.getDefiningURIs().stream().anyMatch(
                        definingUri -> definingUri.getPath().contains(archive.getPath())))
                .collect(Collectors.toMap((t) -> t.getName(), Function.identity(), (first, second) -> {
                    logger.log(Level.FINE, "Duplicate type {0} detected while performing OpenAPI scanning, will use the first.",
                            first.getName());
                    logger.log(Level.FINER, "First duplicate type: {0}\n\nSecond duplicate type: {1}",
                            new Object[]{first.toString(), second.toString()});
                    return first;
                }));
    }

    /**
     * @return a list of all classes in the archive.
     */
    private Set<Type> filterTypes(ReadableArchive archive, Map<String, Type> hk2Types, boolean scanLibs) {
        Set<Type> types = new HashSet<>();
        types.addAll(filterLibTypes(hk2Types, archive, scanLibs));
        types.addAll(
                Collections.list(archive.entries()).stream()
                        // Only use the classes
                        .filter(clazz -> clazz.endsWith(".class"))
                        // Remove the WEB-INF/classes and return the proper class name format
                        .map(clazz -> clazz.replaceAll("WEB-INF/classes/", "").replace("/", ".").replace(".class", ""))
                        // Fetch class type
                        .map(clazz -> hk2Types.get(clazz))
                        // Don't return null classes
                        .filter(Objects::nonNull)
                        .collect(toSet())
        );
        return config == null ? types : config.getValidClasses(types);
    }

    private Set<Type> filterLibTypes(
            Map<String, Type> hk2Types,
            ReadableArchive archive,
            boolean scanLibs
    ) {
        Set<Type> types = new HashSet<>();
        if (scanLibs) {
            // add libraries from WAR's /WEB-INF/lib/*.jar
            addFoundClasses(archive, hk2Types, "WEB-INF/lib/", types);

            // add here also EAR's /lib/*.jar
            addFoundClasses(archive.getParentArchive(), hk2Types, "lib/", types);
        }
        return types;
    }

    private void addFoundClasses(ReadableArchive archiveToScan, Map<String, Type> hk2Types, String prefix, Set<Type> types) {
        if (archiveToScan != null) {
            Enumeration<String> subArchiveItr = archiveToScan.entries();
            while (subArchiveItr.hasMoreElements()) {
                String subArchiveName = subArchiveItr.nextElement();
                if (subArchiveName.startsWith(prefix) && subArchiveName.endsWith(".jar")) {
                    try {
                        ReadableArchive subArchive = archiveToScan.getSubArchive(subArchiveName);
                        types.addAll(
                                Collections.list(subArchive.entries())
                                        .stream()
                                        // Only use the classes
                                        .filter(clazz -> clazz.endsWith(".class"))
                                        // return the proper class name format
                                        .map(clazz -> clazz.replace("/", ".").replace(".class", ""))
                                        // Fetch class type
                                        .map(clazz -> hk2Types.get(clazz))
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

        String instanceType = Globals.get(ServerEnvironment.class).getRuntimeType().toString();
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
                        port = Globals.get(GrizzlyService.class).getRealPort(networkListener);
                    } catch (MultiException ex) {
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
