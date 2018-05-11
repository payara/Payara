package fish.payara.microprofile.openapi.impl.config;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASModelReader;

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
    private static final String PATH_PREFIX_KEY = "mp.openapi.path.";
    private static final String OPERATION_PREFIX_KEY = "mp.openapi.operation.";

    private Class<? extends OASModelReader> modelReader;
    private Class<? extends OASFilter> filter;
    private boolean scanDisable;
    private List<String> scanPackages = new ArrayList<>();
    private List<Class<?>> scanClasses = new ArrayList<>();
    private List<String> excludePackages = new ArrayList<>();
    private List<Class<?>> excludeClasses = new ArrayList<>();
    private List<String> servers = new ArrayList<>();
    private Map<String, String> pathServerMap = new HashMap<>();
    private Map<String, String> operationServerMap = new HashMap<>();

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
        this.scanClasses = findScanClassesFromConfig(config, applicationClassLoader);
        this.excludePackages = findExcludePackages(config);
        this.excludeClasses = findExcludeClasses(config, applicationClassLoader);
        this.servers = findServers(config);
        this.pathServerMap = findPathServerMap(config);
        this.operationServerMap = findOperationServerMap(config);
    }

    public Class<? extends OASModelReader> getModelReader() {
        return modelReader;
    }

    public Class<? extends OASFilter> getFilter() {
        return filter;
    }

    public boolean getScanDisable() {
        return scanDisable;
    }

    public List<String> getScanPackages() {
        return scanPackages;
    }

    public List<Class<?>> getScanClasses() {
        return scanClasses;
    }

    public List<String> getExcludePackages() {
        return excludePackages;
    }

    public List<Class<?>> getExcludeClasses() {
        return excludeClasses;
    }

    public List<String> getServers() {
        return servers;
    }

    public Map<String, String> getPathServerMap() {
        return pathServerMap;
    }

    public Map<String, String> getOperationServerMap() {
        return operationServerMap;
    }

    public Set<Class<?>> getValidClasses(Set<Class<?>> classes) {
        return classes.stream()
                // If scan classes are specified, check that the class is in the list
                .filter(clazz -> scanClasses.isEmpty() || scanClasses.contains(clazz))
                // If exclude classes are specified, check that the class is not the list
                .filter(clazz -> excludeClasses.isEmpty() || !excludeClasses.contains(clazz))
                // If scan packages are specified, check that the class package starts with one in the list
                .filter(clazz -> scanPackages.isEmpty() || scanPackages.stream().anyMatch(pkg -> clazz.getPackage().getName().startsWith(pkg)))
                // If exclude packages are specified, check that the class package doesn't start with any in the list
                .filter(clazz -> excludePackages.isEmpty() || !excludePackages.stream().anyMatch(pkg -> clazz.getPackage().getName().startsWith(pkg)))
                .collect(toSet());
    }

    private Map<String, String> findOperationServerMap(Config config) {
        Map<String, String> map = new HashMap<>();
        try {
            for (String propertyName : config.getPropertyNames()) {
                if (propertyName.startsWith(OPERATION_PREFIX_KEY)) {
                    operationServerMap.put(propertyName, config.getValue(propertyName, String.class));
                }
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return map;
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

    private boolean findScanDisableFromConfig(Config config) {
        try {
            return config.getValue(SCAN_DISABLE_KEY, Boolean.class);
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> findScanPackagesFromConfig(Config config) {
        List<String> packages = new ArrayList<>();
        try {
            packages.addAll(config.getValue(SCAN_PACKAGES_KEY, List.class));
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return packages;
    }

    @SuppressWarnings("unchecked")
    private List<Class<?>> findScanClassesFromConfig(Config config, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            List<String> classNames = (List<String>) config.getValue(SCAN_CLASSES_KEY, List.class);
            for (String className : classNames) {
                Class<?> clazz = getClassFromName(className, classLoader);
                if (clazz != null) {
                    classes.add(clazz);
                }
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    private List<String> findExcludePackages(Config config) {
        List<String> packages = new ArrayList<>();
        try {
            packages.addAll(config.getValue(SCAN_EXCLUDE_PACKAGES_KEY, List.class));
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return packages;
    }

    @SuppressWarnings("unchecked")
    private List<Class<?>> findExcludeClasses(Config config, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            List<String> classNames = config.getValue(SCAN_EXCLUDE_CLASSES_KEY, List.class);
            for (String className : classNames) {
                Class<?> clazz = getClassFromName(className, classLoader);
                if (clazz != null) {
                    classes.add(clazz);
                }
            }
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    private List<String> findServers(Config config) {
        List<String> serverList = new ArrayList<>();
        try {
            serverList.addAll(config.getValue(SERVERS_KEY, List.class));
        } catch (NoSuchElementException ex) {
            // Ignore
        }
        return serverList;
    }

    private Map<String, String> findPathServerMap(Config config) {
        Map<String, String> map = new HashMap<>();
        try {
            for (String propertyName : config.getPropertyNames()) {
                if (propertyName.startsWith(PATH_PREFIX_KEY)) {
                    map.put(propertyName, config.getValue(propertyName, String.class));
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
            LOGGER.log(Level.WARNING, "Unable to find class.", ex);
        }
        return null;
    }

}