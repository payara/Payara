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
package fish.payara.microprofile.openapi.impl.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toSet;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.glassfish.hk2.classmodel.reflect.Type;

public class OpenApiConfiguration {

    private static final Logger LOGGER = Logger.getLogger(OpenApiConfiguration.class.getName());

    private static final String MODEL_READER_KEY = "mp.openapi.model.reader";
    private static final String FILTER_KEY = "mp.openapi.filter";
    private static final String SCAN_DISABLE_KEY = "mp.openapi.scan.disable";
    private static final String SCAN_PACKAGES_KEY = "mp.openapi.scan.packages";
    private static final String SCAN_CLASSES_KEY = "mp.openapi.scan.classes";
    private static final String SCAN_EXCLUDE_PACKAGES_KEY = "mp.openapi.scan.exclude.packages";
    private static final String SCAN_EXCLUDE_CLASSES_KEY = "mp.openapi.scan.exclude.classes";
    private static final String SERVERS_KEY = "mp.openapi.servers";
    private static final String PATH_PREFIX_KEY = "mp.openapi.servers.path.";
    private static final String OPERATION_PREFIX_KEY = "mp.openapi.servers.operation.";

    private final Class<? extends OASModelReader> modelReader;
    private final Class<? extends OASFilter> filter;
    private final boolean scanDisable;
    private List<String> scanPackages = new ArrayList<>();
    private List<String> scanClasses = new ArrayList<>();
    private List<String> excludePackages = new ArrayList<>();
    private List<String> excludeClasses = new ArrayList<>();
    private List<String> servers = new ArrayList<>();
    private Map<String, Set<String>> pathServerMap = new HashMap<>();
    private Map<String, Set<String>> operationServerMap = new HashMap<>();

    public OpenApiConfiguration(ClassLoader applicationClassLoader) {
        // Find the correct configuration instance
        if (applicationClassLoader == null) {
            applicationClassLoader = getClass().getClassLoader();
        }
        Config config = ConfigProvider.getConfig(applicationClassLoader);

        // Find each variable
        this.modelReader = findModelReaderFromConfig(config, applicationClassLoader);
        this.filter = findFilterFromConfig(config, applicationClassLoader);
        this.scanDisable = findScanDisableFromConfig(config);
        this.scanPackages = findScanPackagesFromConfig(config);
        this.scanClasses = findScanClassesFromConfig(config);
        this.excludePackages = findExcludePackages(config);
        this.excludeClasses = findExcludeClasses(config);
        this.servers = findServers(config);
        this.pathServerMap = findPathServerMap(config);
        this.operationServerMap = findOperationServerMap(config);
    }

    /**
     * @return the {@link OASModelReader} class provided by the application.
     */
    public Class<? extends OASModelReader> getModelReader() {
        return modelReader;
    }

    /**
     * @return the {@link OASFilter} class provided by the application.
     */
    public Class<? extends OASFilter> getFilter() {
        return filter;
    }

    /**
     * @return whether to disable application scanning.
     */
    public boolean getScanDisable() {
        return scanDisable;
    }

    /**
     * @return a whitelist of packages to scan in the application.
     */
    public List<String> getScanPackages() {
        return scanPackages;
    }

    /**
     * @return a whitelist of classes to scan in the application.
     */
    public List<String> getScanClasses() {
        return scanClasses;
    }

    /**
     * @return a blacklist of packages to not scan in the application.
     */
    public List<String> getExcludePackages() {
        return excludePackages;
    }

    /**
     * @return a blacklist of classes to not scan in the application.
     */
    public List<String> getExcludeClasses() {
        return excludeClasses;
    }

    /**
     * @return a list of servers to add to the root document.
     */
    public List<String> getServers() {
        return servers;
    }

    /**
     * @return a map of paths to the servers it contains.
     */
    public Map<String, Set<String>> getPathServerMap() {
        return pathServerMap;
    }

    /**
     * @return a map of operation ids to the servers it contains.
     */
    public Map<String, Set<String>> getOperationServerMap() {
        return operationServerMap;
    }

    /**
     * @param types the list of classes to filter.
     * @return a filtered list of classes, using {@link #getScanClasses()},
     *         {@link #getExcludeClasses()}, {@link #getScanPackages()} and
     *         {@link #getExcludePackages()}.
     */
    public Set<Type> getValidClasses(Collection<Type> types) {
        return types.stream()
                // If scan classes are specified, check that the class is in the list
                .filter(type -> scanClasses.isEmpty() || scanClasses.contains(type.getName()))
                // If exclude classes are specified, check that the class is not the list
                .filter(type -> excludeClasses.isEmpty() || !excludeClasses.contains(type.getName()))
                // If scan packages are specified, check that the class package starts with one
                // in the list
                .filter(type -> scanPackages.isEmpty()
                        || scanPackages.stream().anyMatch(pkg -> type.getName().startsWith(pkg)))
                // If exclude packages are specified, check that the class package doesn't start
                // with any in the list
                .filter(clazz -> excludePackages.isEmpty()
                        || excludePackages.stream().noneMatch(pkg -> clazz.getName().startsWith(pkg)))
                .collect(toSet());
    }

    @SuppressWarnings("unchecked")
    private Class<? extends OASModelReader> findModelReaderFromConfig(Config config, ClassLoader classLoader) {
        try {
            String modelReaderClassName = config.getValue(MODEL_READER_KEY, String.class);
            Class<?> modelReaderClass = getClassFromName(modelReaderClassName, classLoader);
            if (modelReaderClass != null && OASModelReader.class.isAssignableFrom(modelReaderClass)) {
                return (Class<? extends OASModelReader>) modelReaderClass;
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends OASFilter> findFilterFromConfig(Config config, ClassLoader classLoader) {
        try {
            String filterClassName = config.getValue(FILTER_KEY, String.class);
            Class<?> filterClass = getClassFromName(filterClassName, classLoader);
            if (filterClass != null && OASFilter.class.isAssignableFrom(filterClass)) {
                return (Class<? extends OASFilter>) filterClass;
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return null;
    }

    private static boolean findScanDisableFromConfig(Config config) {
        try {
            return config.getValue(SCAN_DISABLE_KEY, Boolean.class);
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return false;
    }

    private static List<String> findScanPackagesFromConfig(Config config) {
        List<String> packages = new ArrayList<>();
        try {
            packages.addAll(Arrays.asList(config.getValue(SCAN_PACKAGES_KEY, String[].class)));
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return packages;
    }

    private List<String> findScanClassesFromConfig(Config config) {
        List<String> classes = new ArrayList<>();
        try {
            List<String> classNames = Arrays.asList(config.getValue(SCAN_CLASSES_KEY, String[].class));
            for (String className : classNames) {
                classes.add(className);
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return classes;
    }

    private static List<String> findExcludePackages(Config config) {
        List<String> packages = new ArrayList<>();
        try {
            packages.addAll(Arrays.asList(config.getValue(SCAN_EXCLUDE_PACKAGES_KEY, String[].class)));
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return packages;
    }

    private List<String> findExcludeClasses(Config config) {
        List<String> classes = new ArrayList<>();
        try {
            List<String> classNames = Arrays.asList(config.getValue(SCAN_EXCLUDE_CLASSES_KEY, String[].class));
            for (String className : classNames) {
                    classes.add(className);
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return classes;
    }

    private static List<String> findServers(Config config) {
        List<String> serverList = new ArrayList<>();
        try {
            serverList.addAll(Arrays.asList(config.getValue(SERVERS_KEY, String[].class)));
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return serverList;
    }

    private static Map<String, Set<String>> findPathServerMap(Config config) {
        Map<String, Set<String>> map = new HashMap<>();
        try {
            for (String propertyName : config.getPropertyNames()) {
                if (propertyName.startsWith(PATH_PREFIX_KEY)) {
                    map.put(propertyName.replaceFirst(PATH_PREFIX_KEY, ""),
                            new HashSet<>(Arrays.asList(config.getValue(propertyName, String[].class))));
                }
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return map;
    }

    private static Map<String, Set<String>> findOperationServerMap(Config config) {
        Map<String, Set<String>> map = new HashMap<>();
        try {
            for (String propertyName : config.getPropertyNames()) {
                if (propertyName.startsWith(OPERATION_PREFIX_KEY)) {
                    map.put(propertyName.replaceFirst(OPERATION_PREFIX_KEY, ""),
                            new HashSet<>(Arrays.asList(config.getValue(propertyName, String[].class))));
                }
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return map;
    }

    private Class<?> getClassFromName(String className, ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        if (className == null) {
            return null;
        }

        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            LOGGER.log(WARNING, "Unable to find class.", ex);
        }
        return null;
    }

}
