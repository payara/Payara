/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.glassfish.bootstrap.osgi;

import com.sun.enterprise.glassfish.bootstrap.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Goes through a list of URIs and installs bundles from those locations.
 * It installs the bundles in the same order as they appear in the configuration.
 * <p/>
 * This class is also responsible for updating or uninstalling bundles during
 * subsequent restart if jars have been updated or deleted.
 * <p/>
 * It can also be passed a list of URIs of bundles to be started automatically.
 * The list of bundles to be started must be a subset of list of bundles to be
 * installed. The autostart bundles can also be configured with start level.
 * It can be configured to start bundles persistently or transiently.
 * <p/>
 * This being a provisioning service itself can't expect too many other services to be available.
 * So, it relies on core framework services only.
 * <p/>
 * Several operations of this class can be customized via a {@link Customizer} object. Please refer to
 * {@link DefaultCustomizer} for the default policy.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
public class BundleProvisioner {
    /*
     * Implementation Note: Since bundle installation order can affect OSGi packager resolver, this class
     * honors the order specified by user.
     */

    /**
     * This interface allows us to customize various aspects of this class.
     * e.g., what should be used as location string while installing bundles,
     * what should be installed from a given directory, etc.
     */
    interface Customizer {
        /**
         * @param jar jar to be installed as bundle
         * @return Location that should be used while installing this jar as a bundle
         */
        String makeLocation(Jar jar);

        /**
         * Is this URI a directory?
         *
         * @param uri URI to be checked
         * @return true if this URI represents a directory, else false.
         */
        boolean isDirectory(URI uri);

        /**
         * Is this jar managed by us?
         *
         * @param jar
         * @return
         */
        boolean isManaged(Jar jar);

        /**
         * Return list of locations from where bundles are installed.
         *
         * @return
         */
        List<URI> getAutoInstallLocations();

        /**
         * Return list of locations from where bundles are started. This must be a subset of what is returned by
         * {@link #getAutoInstallLocations()}
         *
         * @return
         */
        List<URI> getAutoStartLocations();

        /**
         * Options used in Bundle.start().
         *
         * @return
         */
        int getStartOptions();

        /**
         * @param jar
         * @return start level of this bundle. -1 if not known
         */
        Integer getStartLevel(Jar jar);
    }

    private static final String FILE_SCHEME = "file";

    private static Logger logger = Logger.getLogger(BundleProvisioner.class.getPackage().getName());

    private BundleContext bundleContext;
    private boolean systemBundleUpdationRequired;
    private Map<URI, Jar> currentManagedBundles = new HashMap<URI, Jar>();
    private int noOfUninstalledBundles;
    private int noOfUpdatedBundles;
    private int noOfInstalledBundles;
    private Customizer customizer;

    private StartLevel sl;
    private PackageAdmin pa;

    public BundleProvisioner(BundleContext bundleContext, Properties config) {
        this(bundleContext, new DefaultCustomizer(config));
    }

    public BundleProvisioner(BundleContext bundleContext, Customizer customizer) {
        this.bundleContext = bundleContext;
        this.customizer = customizer;
        final ServiceReference reference =
                getBundleContext().getServiceReference(StartLevel.class.getName());
        sl = StartLevel.class.cast(
                getBundleContext().getService(reference));
    }

    private BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * This method goes collects list of bundles that have been installed
     * from the watched directory in previous run of the program,
     * compares them with the current set of jar files,
     * uninstalls old bundles, updates modified bundles, installs new bundles.
     * It returns list of bundle ids provisioned by this provisoner.
     */
    public List<Long> installBundles() {
        initCurrentManagedBundles();
        final Collection<Jar> current = getBundleJars();
        List<Jar> discovered = discoverJars();

        // Find out all the new, deleted and common bundles.
        // new = discovered - current
        List<Jar> newBundles = new ArrayList<Jar>(discovered);
        newBundles.removeAll(current);

        // deleted = current - discovered
        List<Jar> deletedBundles = new ArrayList<Jar>(current);
        deletedBundles.removeAll(discovered);

        // existing = intersection of current & discovered
        List<Jar> existingBundles = new ArrayList<Jar>(discovered);
        // We remove discovered ones from current, so that we are left
        // with a collection of Jars made from files so that we can compare
        // them with bundles.
        existingBundles.retainAll(current);

        // We do the operations in the following order:
        // uninstall, update, install, refresh & start.
        uninstall(deletedBundles);
        update(existingBundles);
        install(newBundles);
        List<Long> ids = new ArrayList<Long>();
        for (Jar j : currentManagedBundles.values()) {
            ids.add(j.getBundleId());
        }
        return ids;
    }

    /**
     * Go through the list of auto start bundles and start them.
     */
    public void startBundles() {
        for (URI uri : getAutoStartLocations()) {
            Bundle bundle = getBundle(new Jar(uri));
            if (bundle == null) {
                logger.logp(Level.WARNING, "BundleProvisioner", "startBundles",
                        "Can not start bundle {0} because it is not contained in the list of installed bundles",
                        new Object[]{uri});
                continue;
            } else {
                startBundle(bundle);
            }
        }
    }

    /**
     * Start a bundle using given policy
     *
     * @param bundle
     */
    private void startBundle(Bundle bundle) {
        if (!isFragment(bundle)) {
            try {
                bundle.start(customizer.getStartOptions());
                logger.logp(Level.FINE, "BundleProvisioner", "startBundle", "Started bundle = {0}",
                        new Object[]{bundle});
            } catch (BundleException e) {
                logger.logp(Level.WARNING, "BundleProvisioner", "startBundle",
                        "Failed to start " + bundle, e);
            }
        }
    }

    /**
     * This method goes through all the currently installed bundles
     * and returns information about those bundles whose location
     * refers to locations as we have been configured to manage.
     */
    private void initCurrentManagedBundles() {
        Bundle[] bundles = getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            try {
                final long id = bundle.getBundleId();
                if (id == 0) {
                    // We can't manage system bundle
                    continue;
                }
                Jar jar = new Jar(bundle);
                if (customizer.isManaged(jar)) {
                    addBundle(jar);
                }
            }
            catch (URISyntaxException e) {
                // Ignore and continue.
                // This can never happen for bundles that have been installed
                // by FileInstall, as we always use proper filepath as location.
            }
        }
    }

    /**
     * @return list of URIs configured to be installed everytime
     */
    private List<URI> getAutoInstallLocations() {
        return customizer.getAutoInstallLocations();
    }

    /**
     * @return list of URIs configured to be started everytime
     */
    private List<URI> getAutoStartLocations() {
        return customizer.getAutoStartLocations();
    }

    private Integer getStartLevel(Jar jar) {
        return customizer.getStartLevel(jar);
    }

    /**
     * Goes through the list of URIs configured via the config properties and converst them into
     * bundle Jar objects. It delegates to the customizer to discover the bundle jars.
     *
     * @return
     */
    private List<Jar> discoverJars() {
        List<Jar> jars = new ArrayList<Jar>();
        for (URI uri : getAutoInstallLocations()) {
            jars.add(new Jar(uri));
        }
        return jars;
    }

    private int uninstall(Collection<Jar> bundles) {
        for (Jar jar : bundles) {
            Bundle bundle = getBundle(jar);
            if (bundle == null) {
                // this is highly unlikely, but can't be ruled out.
                logger.logp(Level.WARNING, "BundleProvisioner", "uninstall",
                        "Can't uninstall bundle = {0} as it's already uninstalled", new Object[]{bundle});
                continue;
            }
            try {
                bundle.uninstall();
                noOfUninstalledBundles++;
                removeBundle(jar);
                logger.logp(Level.INFO, "BundleProvisioner", "uninstall",
                        "Uninstalled bundle {0} installed from {1} ",
                        new Object[]{bundle.getBundleId(), jar.getPath()});
            } catch (Exception e) {
                logger.logp(Level.WARNING, "BundleProvisioner", "uninstall",
                        "Failed to uninstall bundle " + jar.getPath(),
                        e);
            }
        }
        return noOfUninstalledBundles;
    }

    private int update(Collection<Jar> jars) {
        for (Jar jar : jars) {
            final Jar existingJar = getBundleJar(jar);
            if (jar.isNewer(existingJar)) {
                Bundle bundle = getBundle(existingJar);
                if (bundle == null) {
                    // this is highly unlikely, but can't be ruled out.
                    logger.logp(Level.WARNING, "BundleProvisioner", "update",
                            "Can't update bundle = {0} as it's already uninstalled", new Object[]{bundle});
                    continue;
                }
                try {
                    if (isFrameworkExtensionBundle(bundle)) {
                        systemBundleUpdationRequired = true;
                    }
                    bundle.update();
                    noOfUpdatedBundles++;
                    logger.logp(Level.INFO, "BundleProvisioner", "update",
                            "Updated bundle {0} from {1} ",
                            new Object[]{bundle.getBundleId(), jar.getPath()});
                } catch (Exception e) {
                    logger.logp(Level.WARNING, "BundleProvisioner", "update",
                            "Failed to update " + jar.getPath(),
                            e);
                }
            }
        }
        return noOfUpdatedBundles;
    }

    private Collection<Jar> getBundleJars() {
        return currentManagedBundles.values();
    }

    private Jar getBundleJar(Jar jar) {
        return currentManagedBundles.get(jar.getURI());
    }

    private void addBundle(Jar jar) throws URISyntaxException {
        currentManagedBundles.put(jar.getURI(), jar);
    }

    private void removeBundle(Jar jar) throws URISyntaxException {
        currentManagedBundles.remove(jar.getURI());
    }

    /**
     * Return a bundle corresponding to this jar object.
     * It first searches using BundleContext as opposed to {@link #currentManagedBundles} so that it can give
     * more accurate results if bundles have been uninstalled without our knowledge.
     *
     * @param jar
     * @return
     */
    private Bundle getBundle(Jar jar) {
        long bundleId = jar.getBundleId();
        if (bundleId < 0) {
            final Jar jar1 = currentManagedBundles.get(jar.getURI());
            if (jar1 != null) {
                bundleId = jar1.getBundleId();
            }
        }
        return getBundleContext().getBundle(bundleId);
    }

    /**
     * Is the supplied bundle a framework extension bundle?
     *
     * @param bundle
     * @return
     */
    private boolean isFrameworkExtensionBundle(Bundle bundle) {
        if (isFragment(bundle)) {
            // Since Fragment-Host can use a framework specific symbolic name of the system bundle, we can't
            // assume that user has used "system.bundle." So, we check for the directive "extension:=framework"
            final String fragmentHost = (String) bundle.getHeaders().get(org.osgi.framework.Constants.FRAGMENT_HOST);
            final String separator = ";";
            for (String s : fragmentHost.split(separator)) {
                int idx = s.indexOf(":=");
                if (idx != -1) {
                    String directiveName = s.substring(0, idx).trim();
                    if (directiveName.equals("extension") && s.substring(idx + 2).trim().equals("framework")) {
                        logger.logp(Level.FINE, "BundleProvisioner", "isSystemBundleFragment",
                                "{0} is a framework extension bundle", new Object[]{bundle});
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Is this a fragment bundle?
     *
     * @param bundle
     * @return
     */
    private boolean isFragment(Bundle bundle) {
        final ServiceReference reference =
                getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = PackageAdmin.class.cast(
                getBundleContext().getService(reference));
        return (pa.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT);
    }

    private int install(Collection<Jar> jars) {
        for (Jar jar : jars) {
            try {
                final InputStream is = jar.getURI().toURL().openStream();
                try {
                    Bundle b = getBundleContext().installBundle(makeLocation(jar), is);
                    Integer startLevel = getStartLevel(jar);
                    if (startLevel != null) { // if specified, set it
                        getStartLevelService().setBundleStartLevel(b, startLevel);
                    }
                    noOfInstalledBundles++;
                    addBundle(new Jar(b));
                    logger.logp(Level.FINE, "BundleProvisioner", "install",
                            "Installed bundle {0} from {1} ",
                            new Object[]{b.getBundleId(), jar.getURI()});
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            } catch (Exception e) {
                logger.logp(Level.WARNING, "BundleProvisioner", "install",
                        "Failed to install " + jar.getURI(),
                        e);
            }
        }
        return noOfInstalledBundles;
    }

    private StartLevel getStartLevelService() {
        return sl;
    }

    private String makeLocation(Jar jar) {
        return customizer.makeLocation(jar);
    }

    /**
     * Refresh packages
     */
    public void refresh() {
        final ServiceReference reference =
                getBundleContext().getServiceReference(PackageAdmin.class.getName());
        PackageAdmin pa = PackageAdmin.class.cast(
                getBundleContext().getService(reference));
        pa.refreshPackages(null); // null to refresh any bundle that's obsolete
        getBundleContext().ungetService(reference);
    }

    /**
     * @return true if anything changed since last time framework was initialized
     */
    public boolean hasAnyThingChanged() {
        return getNoOfInstalledBundles() + getNoOfUninstalledBundles() + getNoOfUpdatedBundles() > 0;
    }

    /**
     * @return true if system bundle needs to be updated because of bundles getting updated or deleted or installed.
     */
    public boolean isSystemBundleUpdationRequired() {
        return systemBundleUpdationRequired;
    }

    /**
     * @return no of bundles uninstalled
     */
    public int getNoOfUninstalledBundles() {
        return noOfUninstalledBundles;
    }

    /**
     * @return no of bundles updated
     */
    public int getNoOfUpdatedBundles() {
        return noOfUpdatedBundles;
    }

    /**
     * @return no of bundles installed
     */
    public int getNoOfInstalledBundles() {
        return noOfInstalledBundles;
    }

    /**
     * This is default implementation of {@link Customizer} which uses the URI of the Jar as location.
     * The default customizer uses following configuration properties:
     * <p/>
     * glassfish.osgi.auto.install:
     * <p/>
     * This contains white space delimited list of absolute URIs to be installed.
     * If a URI represents a directory location, then it will be treated as if all the jar files from that
     * directory location (non-recursive) were specified instead.
     * Bundles will be installed in the same order they are specified.
     * <p/>
     * <p/>
     * glassfish.osgi.auto.start:
     * This contains white space delimited list of absolute URIs to be started.
     * If a URI represents a directory location, then it will be treated as if all the jar files from that
     * directory location (non-recursive) were specified instead. Although bundles will be started in the order
     * they are specified in this list, their eventual activation order depends on start level and/or activation policy.
     * <p/>
     * <p/>
     * glassfish.osgi.auto.start.level.N, where N is > 0
     * This contains list of bundle URIs to be started at start level N.
     * <p/>
     * <p/>
     * glassfish.osgi.auto.start.option:
     * This specifies the options used to start bundles. It is an integer and must confirm to the format accepted by
     * {@link Bundle#start(int)} method. The default is to use {@link Bundle#START_ACTIVATION_POLICY}. It also means
     * bundles will be started persistently by default. To start transiently, one has to specify explicitly.
     *
     * @author sanjeeb.sahoo@oracle.com
     */
    public static class DefaultCustomizer implements Customizer {

        private Properties config;

        /**
         * Maps URI to start level
         */
        private final Map<URI, Integer> startLevels = new HashMap<URI, Integer>();
        private List<URI> autoInstallLocations;
        private List<URI> autoStartLocations;
        private List<URI> configuredAutoInstallLocations;

        public DefaultCustomizer(Properties config) {
            this.config = config;
            // for optimization reasons, process these properties once and store their values
            processAutoInstallLocations();
            processAutoStartLocations();
            processStartLevels();
        }

        private void processAutoInstallLocations() {
            String list = config.getProperty(Constants.AUTO_INSTALL_PROP);
            configuredAutoInstallLocations = getLocations(list, false);
            autoInstallLocations = getLocations(list);
        }

        private void processAutoStartLocations() {
            String list = config.getProperty(Constants.AUTO_START_PROP);
            autoStartLocations = getLocations(list);
        }

        private void processStartLevels() {
            for (String key : config.stringPropertyNames()) {
                if (key.startsWith(Constants.AUTO_START_LEVEL_PROP)) {
                    try {
                        Integer startLevel = Integer.parseInt(key.substring(key.lastIndexOf('.') + 1));
                        String list = config.getProperty(key);
                        for (URI uri : getLocations(list)) {
                            if (startLevels.containsKey(uri)) {
                                logger.logp(Level.WARNING, "BundleProvisioner$DefaultCustomizer",
                                        "processStartLevels",
                                        "Can not set the start level for {0} to {2} as it is already set to {1}.",
                                        new Object[]{uri, startLevels.get(uri), startLevel});
                            } else {
                                startLevels.put(uri, startLevel);
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        System.err.println("Invalid property: " + key);
                    }

                }
            }
        }

        @Override
        public List<URI> getAutoInstallLocations() {
            return autoInstallLocations;
        }

        @Override
        public List<URI> getAutoStartLocations() {
            return autoStartLocations;
        }

        private List<URI> getLocations(String list) {
            return getLocations(list, true);
        }

        private List<URI> getLocations(String list, boolean expand) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            List<URI> uris = new ArrayList<URI>();
            for (String s : list.split("\\s")) {
                try {
                    URI uri = new URI(s);
                    if (!uri.isAbsolute()) {
                        logger.logp(Level.WARNING, "BundleProvisioner", "getLocations",
                                "Skipping entry {0} because it is not an absolute URI ", new Object[]{uri});
                        continue;
                    }
                    if (expand && isDirectory(uri)) {
                        uris.addAll(listJarFiles(uri));
                    } else {
                        uris.add(uri);
                    }
                } catch (URISyntaxException e) {
                    logger.logp(Level.WARNING, "BundleProvisioner", "getLocations",
                            "Skipping entry {0} due to " + e, new Object[]{s});
                }
            }
            return uris;
        }

        @Override
        public int getStartOptions() {
            String s = config.getProperty(Constants.AUTO_START_OPTIONS_PROP);
            if (s != null) {
                return Integer.valueOf(s);
            } else {
                return Bundle.START_ACTIVATION_POLICY;
            }
        }

        @Override
        public String makeLocation(Jar jar) {
            return jar.getURI().toString();
        }

        @Override
        public boolean isDirectory(URI uri) {
            try {
                return new File(uri).isDirectory();
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * @param aDirectoryURI uri corresponding to a directory location which will be searched.
         * @return URIs corresponding to jar files in a given directory location.
         */
        protected Collection<? extends URI> listJarFiles(URI aDirectoryURI) {
            // currently we only support file type directory URI. In future, we should be able to handle
            // directories inside jar files as well.
            assert (FILE_SCHEME.equalsIgnoreCase(aDirectoryURI.getScheme()));
            final List<URI> jarURIs = new ArrayList<URI>();
//            File dir = new File(aDirectoryURI);
            new File(aDirectoryURI).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.getName().endsWith(".jar") && !pathname.isDirectory()) {
                        jarURIs.add(pathname.toURI());
                        return true;
                    }
                    return false;
                }
            });
            return jarURIs;
        }

        @Override
        public boolean isManaged(Jar jar) {
            URI uri = jar.getURI();
            if (uri == null) {
                // jar.getURI is null means we could not parse the location
                // as a meaningful URI. We can't do any meaningful processing for this bundle.
                return false;
            } else {
                for (URI configuredLocation : getConfiguredAutoInstallLocations()) {
                    final String otherLocationAsString = configuredLocation.toString();
                    if (uri.toString().regionMatches(0, otherLocationAsString, 0, otherLocationAsString.length())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Integer getStartLevel(Jar jar) {
            return startLevels.get(jar.getURI());
        }

        protected List<URI> getConfiguredAutoInstallLocations() {
            return configuredAutoInstallLocations;
        }
    }

    /**
     * A simple main method to test this class
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        logger.logp(Level.INFO, "BundleProvisioner", "main", "Starting");
        Properties props = new Properties();
        props.load(new FileInputStream(args[0]));
        Util.substVars(props);
        PrintStream out = new PrintStream(new FileOutputStream(args[1], true));
        long t0 = System.currentTimeMillis();
        Framework f = null;
        for (FrameworkFactory ff : ServiceLoader.load(FrameworkFactory.class)) {
            f = ff.newFramework(props);
            System.out.println("framework = " + f);
            break;
        }
        if (f == null) {
            throw new RuntimeException("no OSGi framework in classpath");
        }
        long t1 = System.currentTimeMillis();
        logger.logp(Level.INFO, "BundleProvisioner", "main", "timeTaken to locate OSGi framework = {0} ms",
                new Object[]{t1 - t0});
        f.init();
        long t2 = System.currentTimeMillis();
        logger.logp(Level.INFO, "BundleProvisioner", "main", "timeTaken to initialize OSGi framework = {0} ms",
                new Object[]{t2 - t1});
        BundleProvisioner bundleProvisioner = new BundleProvisioner(f.getBundleContext(), props);
        bundleProvisioner.installBundles();
        long t3 = System.currentTimeMillis();
        logger.logp(Level.INFO, "BundleProvisioner", "main", "timeTaken to finish installation of bundles = {0} ms",
                new Object[]{t3 - t2});
        int installed = bundleProvisioner.getNoOfInstalledBundles();
        int updated = bundleProvisioner.getNoOfUpdatedBundles();
        int uninstalled = bundleProvisioner.getNoOfUninstalledBundles();
        System.out.printf("installed %d, updated = %d, uninstalled = %d\n", installed, updated, uninstalled);
        if (bundleProvisioner.hasAnyThingChanged()) {
            System.out.println("Refreshing framework");
            bundleProvisioner.refresh();
        }
        bundleProvisioner.startBundles();
        f.start();
        long t4 = System.currentTimeMillis();
        logger.logp(Level.INFO, "BundleProvisioner", "main", "time taken to finish starting bundles = {0} ms",
                new Object[]{t4 - t3});
        logger.logp(Level.INFO, "BundleProvisioner", "main", "total time taken to start = {0}", new Object[]{t4-t0});
        if (args.length == 3 && args[2].equalsIgnoreCase("wait-before-stopping")) {
            System.out.println("Hit enter to continue");
            System.in.read(); //
        }
        f.stop();
        f.waitForStop(0);
        long t5 = System.currentTimeMillis();
        logger.logp(Level.INFO, "BundleProvisioner", "main", "time taken to stop = {0} ms", new Object[]{t5 - t4});
        logger.logp(Level.INFO, "BundleProvisioner", "main", "Total time taken = {0}", new Object[]{t5-t0});
        out.printf("%d,%d,%d,%d,%d,%d,%d\n", t1-t0, t2-t1, t3-t2, t4-t3, t4-t0, t5-t4, t5-t0);
    }
}
