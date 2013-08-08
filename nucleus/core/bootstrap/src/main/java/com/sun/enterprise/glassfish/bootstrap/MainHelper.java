/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.glassfish.bootstrap;

import com.sun.enterprise.module.bootstrap.ArgumentManager;
import static com.sun.enterprise.module.bootstrap.ArgumentManager.argsToMap;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.module.bootstrap.Which;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class used by bootstrap module.
 * Most of the code is moved from {@link ASMain} or {@link GlassFishMain}to this class to keep them
 * as small as possible and to improve reusability when GlassFish is launched in other modes (e.g., karaf).
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class MainHelper {
    
    private static Logger logger = LogFacade.BOOTSTRAP_LOGGER;
    
    /*protected*/

    static void checkJdkVersion() {
        int minor = getMinorJdkVersion();

        if (minor < 7) {
            logger.log(Level.SEVERE, LogFacade.BOOTSTRAP_INCORRECT_JDKVERSION, new Object[]{ 7, minor});
            System.exit(1);
        }
    }

    private static int getMinorJdkVersion() {
        // this is a subset of the code in com.sun.enterprise.util.JDK
        // this module has no dependencies on util code so it was dragged in here.

        try {
            String jv = System.getProperty("java.version");
            String[] ss = jv.split("\\.");

            if (ss == null || ss.length < 3 || !ss[0].equals("1"))
                return 1;

            return Integer.parseInt(ss[1]);
        }
        catch (Exception e) {
            return 1;
        }
    }

    static String whichPlatform() {
        String platform = Constants.Platform.Felix.toString(); // default is Felix

        // first check the system props
        String temp = System.getProperty(Constants.PLATFORM_PROPERTY_KEY);
        if (temp == null || temp.trim().length() <= 0) {
            // not in sys props -- check environment
            temp = System.getenv(Constants.PLATFORM_PROPERTY_KEY);
        }

        String trimtemp;
        if (temp != null && (trimtemp = temp.trim()).length() != 0) {
            platform = trimtemp;
        }
        return platform;
    }

    public static Properties parseAsEnv(File installRoot) {

        Properties asenvProps = new Properties();

        // let's read the asenv.conf
        File configDir = new File(installRoot, "config");
        File asenv = getAsEnvConf(configDir);

        if (!asenv.exists()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(asenv.getAbsolutePath() + " not found, ignoring");
            }
            return asenvProps;
        }
        LineNumberReader lnReader = null;
        try {
            lnReader = new LineNumberReader(new FileReader(asenv));
            String line = lnReader.readLine();
            // most of the asenv.conf values have surrounding "", remove them
            // and on Windows, they start with SET XXX=YYY
            Pattern p = Pattern.compile("[Ss]?[Ee]?[Tt]? *([^=]*)=\"?([^\"]*)\"?");
            while (line != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    File f = new File(m.group(2));
                    if (!f.isAbsolute()) {
                        f = new File(configDir, m.group(2));
                        if (f.exists()) {
                            asenvProps.put(m.group(1), f.getAbsolutePath());
                        } else {
                            asenvProps.put(m.group(1), m.group(2));
                        }
                    } else {
                        asenvProps.put(m.group(1), m.group(2));
                    }
                }
                line = lnReader.readLine();
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Error opening asenv.conf : ", ioe);
        } finally {
            try {
                if (lnReader != null)
                    lnReader.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
        return asenvProps;
    }

    void addPaths(File dir, String[] jarPrefixes, List<URL> urls) throws MalformedURLException {
        File[] jars = dir.listFiles();
        if (jars != null) {
            for (File f : jars) {
                for (String prefix : jarPrefixes) {
                    String name = f.getName();
                    if (name.startsWith(prefix) && name.endsWith(".jar"))
                        urls.add(f.toURI().toURL());
                }
            }
        }
    }

    /**
     * Figures out the asenv.conf file to load.
     */
    private static File getAsEnvConf(File configDir) {
        String osName = System.getProperty("os.name");
        if (osName.indexOf("Windows") == -1) {
            return new File(configDir, "asenv.conf");
        } else {
            return new File(configDir, "asenv.bat");
        }
    }

    /**
     * Determines the root directory of the domain that we'll start.
     */
    /*package*/ static File getDomainRoot(Properties args, Properties asEnv) {
        // first see if it is specified directly

        String domainDir = getParam(args, "domaindir");

        if (ok(domainDir))
            return new File(domainDir);

        // now see if they specified the domain name -- we will look in the
        // default domains-dir

        File defDomainsRoot = getDefaultDomainsDir(asEnv);
        String domainName = getParam(args, "domain");

        if (ok(domainName))
            return new File(defDomainsRoot, domainName);

        // OK -- they specified nothing.  Get the one-and-only domain in the
        // domains-dir
        return getDefaultDomain(defDomainsRoot);
    }

    /**
     * Verifies correctness of the root directory of the domain that we'll start and
     * sets the system property called {@link com.sun.enterprise.glassfish.bootstrap.Constants#INSTANCE_ROOT_PROP_NAME}.
     */
    /*package*/ void verifyAndSetDomainRoot(File domainRoot) {
        verifyDomainRoot(domainRoot);

        domainRoot = absolutize(domainRoot);
        System.setProperty(Constants.INSTANCE_ROOT_PROP_NAME, domainRoot.getPath());
    }

    /**
     * Verifies correctness of the root directory of the domain that we'll start.
     *
     * @param domainRoot
     */
    /*package*/
    static void verifyDomainRoot(File domainRoot) {
        String msg = null;

        if (domainRoot == null)
            msg = "Internal Error: The domain dir is null.";
        else if (!domainRoot.exists())
            msg = "the domain directory does not exist";
        else if (!domainRoot.isDirectory())
            msg = "the domain directory is not a directory.";
        else if (!domainRoot.canWrite())
            msg = "the domain directory is not writable.";
        else if (!new File(domainRoot, "config").isDirectory())
            msg = "the domain directory is corrupt - there is no config subdirectory.";

        if (msg != null)
            throw new RuntimeException(msg);
    }

    private static File getDefaultDomainsDir(Properties asEnv) {
        // note: 99% error detection!

        String dirname = asEnv.getProperty(Constants.DEFAULT_DOMAINS_DIR_PROPNAME);

        if (!ok(dirname))
            throw new RuntimeException(Constants.DEFAULT_DOMAINS_DIR_PROPNAME + " is not set.");

        File domainsDir = absolutize(new File(dirname));

        if (!domainsDir.isDirectory())
            throw new RuntimeException(Constants.DEFAULT_DOMAINS_DIR_PROPNAME +
                    "[" + dirname + "]" +
                    " is specifying a file that is NOT a directory.");

        return domainsDir;
    }


    private static File getDefaultDomain(File domainsDir) {
        File[] domains = domainsDir.listFiles(new FileFilter() {
            public boolean accept(File f) { return f.isDirectory(); }
        });

        // By default we will start an unspecified domain iff it is the only
        // domain in the default domains dir

        if (domains == null || domains.length == 0)
            throw new RuntimeException("no domain directories found under " + domainsDir);

        if (domains.length > 1)
            throw new RuntimeException("Multiple domains[" + domains.length + "] found under "
                    + domainsDir + " -- you must specify a domain name as -domain <name>");

        return domains[0];
    }


    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    private static String getParam(Properties map, String name) {
        // allow both "-" and "--"
        String val = map.getProperty("-" + name);

        if (val == null)
            val = map.getProperty("--" + name);

        return val;
    }

    private static File absolutize(File f) {
        try {
            return f.getCanonicalFile();
        }
        catch (Exception e) {
            return f.getAbsoluteFile();
        }
    }

    /**
     * CLI or any other client needs to ALWAYS pass in the instanceDir for
     * instances.
     *
     * @param args
     * @param asEnv
     * @return
     */
    static File getInstanceRoot(Properties args, Properties asEnv) {

        String instanceDir = getParam(args, "instancedir");

        if (ok(instanceDir))
            return new File(instanceDir);

        return null;
    }

    /* package */

    static File findInstallRoot() {
        File bootstrapFile = findBootstrapFile(); // glassfish/modules/glassfish.jar
        return bootstrapFile.getParentFile().getParentFile(); // glassfish/
    }

    /* package */

    static File findInstanceRoot(File installRoot, Properties args) {
        Properties asEnv = parseAsEnv(installRoot);

        // IMPORTANT - check for instance BEFORE domain.  We will always come up
        // with a default domain but there is no such thing sa a default instance

        File instanceDir = getInstanceRoot(args, asEnv);

        if (instanceDir == null) {
            // that means that this is a DAS.
            instanceDir = getDomainRoot(args, asEnv);
        }
        verifyDomainRoot(instanceDir);
        return instanceDir;
    }

    static File findInstanceRoot(File installRoot, String[] args) {
        return findInstanceRoot(installRoot, ArgumentManager.argsToMap(args));
    }

    private static File findBootstrapFile() {
        try {
            return Which.jarFile(ASMain.class);
        } catch (IOException e) {
            throw new RuntimeException("Cannot get bootstrap path from "
                    + ASMain.class + " class location, aborting");
        }
    }

    static Properties buildStartupContext(String platform, File installRoot, File instanceRoot, String[] args) {
        Properties ctx = com.sun.enterprise.module.bootstrap.ArgumentManager.argsToMap(args);
        ctx.setProperty(StartupContext.TIME_ZERO_NAME, Long.toString(System.currentTimeMillis()));

        ctx.setProperty(Constants.PLATFORM_PROPERTY_KEY, platform);

        ctx.setProperty(Constants.INSTALL_ROOT_PROP_NAME, installRoot.getAbsolutePath());
        ctx.setProperty(Constants.INSTALL_ROOT_URI_PROP_NAME, installRoot.toURI().toString());

        ctx.setProperty(Constants.INSTANCE_ROOT_PROP_NAME, instanceRoot.getAbsolutePath());
        ctx.setProperty(Constants.INSTANCE_ROOT_URI_PROP_NAME, instanceRoot.toURI().toString());

        if (ctx.getProperty(StartupContext.STARTUP_MODULE_NAME) == null) {
            ctx.setProperty(StartupContext.STARTUP_MODULE_NAME, Constants.GF_KERNEL);
        }

        // temporary hack until CLI does that for us.
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-upgrade")) {
                if (i + 1 < args.length && !args[i + 1].equals("false")) {
                    ctx.setProperty(StartupContext.STARTUP_MODULESTARTUP_NAME, "upgrade");
                }
            }
        }

        addRawStartupInfo(args, ctx);

        mergePlatformConfiguration(ctx);
        return ctx;
    }

    public static Properties buildStaticStartupContext(long timeZero, String... args) {
        checkJdkVersion();
        Properties ctx = argsToMap(args);
        ctx.setProperty(Constants.PLATFORM_PROPERTY_KEY, "Static");
        buildStartupContext(ctx);
        addRawStartupInfo(args, ctx);

        // Reset time zero with the real value. We can't do this before buildStartupContext()
        // because it has an optimization that is triggered by this key.
        ctx.setProperty(StartupContext.TIME_ZERO_NAME, Long.toString(timeZero));

        // Config variable substitution (and maybe other downstream code) relies on
        // some values in System properties, so copy them in.
        for (String key : new String[]{Constants.INSTALL_ROOT_PROP_NAME,
                                       Constants.INSTANCE_ROOT_URI_PROP_NAME,
                                       Constants.INSTALL_ROOT_PROP_NAME,
                                       Constants.INSTALL_ROOT_URI_PROP_NAME}) {
            System.setProperty(key, ctx.getProperty(key));
        }
        return ctx;
    }

    public static void buildStartupContext(Properties ctx) {
        if (ctx.getProperty(StartupContext.TIME_ZERO_NAME) == null) {
            ctx.setProperty(StartupContext.TIME_ZERO_NAME, Long.toString(System.currentTimeMillis()));
        } else {
            // Optimisation
            // Skip the rest of the code. We assume that we are called from GlassFishMain
            // which already passes a properly populated properties object.
            return;
        }

        if (ctx.getProperty(Constants.PLATFORM_PROPERTY_KEY) == null) {
            ctx.setProperty(Constants.PLATFORM_PROPERTY_KEY, Constants.Platform.Felix.name());
        }

        if (ctx.getProperty(Constants.INSTALL_ROOT_PROP_NAME) == null) {
            File installRoot = findInstallRoot();
            ctx.setProperty(Constants.INSTALL_ROOT_PROP_NAME, installRoot.getAbsolutePath());
            ctx.setProperty(Constants.INSTALL_ROOT_URI_PROP_NAME, installRoot.toURI().toString());
        }

        if (ctx.getProperty(Constants.INSTANCE_ROOT_PROP_NAME) == null) {
            File installRoot = new File(ctx.getProperty(Constants.INSTALL_ROOT_PROP_NAME));
            File instanceRoot = findInstanceRoot(installRoot, ctx);
            ctx.setProperty(Constants.INSTANCE_ROOT_PROP_NAME, instanceRoot.getAbsolutePath());
            ctx.setProperty(Constants.INSTANCE_ROOT_URI_PROP_NAME, instanceRoot.toURI().toString());
        }

        if (ctx.getProperty(StartupContext.STARTUP_MODULE_NAME) == null) {
            ctx.setProperty(StartupContext.STARTUP_MODULE_NAME, Constants.GF_KERNEL);
        }

        if (!ctx.contains(Constants.NO_FORCED_SHUTDOWN)) {
            // Since we are in non-embedded mode, we set this property to false unless user has specified it
            // When set to false, the VM will exit when server fails to startup for whatever reason.
            // See AppServerStartup.java
            ctx.setProperty(Constants.NO_FORCED_SHUTDOWN, Boolean.FALSE.toString());
        }
        mergePlatformConfiguration(ctx);
    }

    /**
     * Need the raw unprocessed args for RestartDomainCommand in case we were NOT started
     * by CLI
     *
     * @param args raw args to this main()
     * @param p    the properties to save as a system property
     */
    private static void addRawStartupInfo(final String[] args, final Properties p) {
        //package the args...
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                sb.append(Constants.ARG_SEP);

            sb.append(args[i]);
        }

        if (!wasStartedByCLI(p)) {
            // no sense doing this if we were started by CLI...
            p.put(Constants.ORIGINAL_CP, System.getProperty("java.class.path"));
            p.put(Constants.ORIGINAL_CN, ASMain.class.getName());
            p.put(Constants.ORIGINAL_ARGS, sb.toString());
        }
    }

    private static boolean wasStartedByCLI(final Properties props) {
        // if we were started by CLI there will be some special args set...

        return
                props.getProperty("-asadmin-classpath") != null &&
                        props.getProperty("-asadmin-classname") != null &&
                        props.getProperty("-asadmin-args") != null;
    }

    /**
     * This method is responsible setting up launcher class loader which is then used while calling
     * {@link org.glassfish.embeddable.GlassFishRuntime#bootstrap(org.glassfish.embeddable.BootstrapProperties, ClassLoader)}.
     *
     * This launcher class loader's delegation hierarchy looks like this:
     * launcher class loader
     *       -> OSGi framework launcher class loader
     *             -> extension class loader
     *                   -> null (bootstrap loader)
     * We first create what we call "OSGi framework launcher class loader," that has
     * classes that we want to be visible via system bundle.
     * Then we create launcher class loader which has {@link OSGiGlassFishRuntimeBuilder} and its dependencies in
     * its search path. We set the former one as the parent of this, there by sharing the same copy of
     * GlassFish API classes and also making OSGi classes visible to OSGiGlassFishRuntimeBuilder.
     *
     * We could have merged all the jars into one class loader and called it the launcher class loader, but
     * then such a loader, when set as the bundle parent loader for all OSGi classloading delegations, would make
     * more things visible than desired. Please note, glassfish.jar has a very long dependency chain. See
     * glassfish issue 13287 for the kinds of problems it can create.
     *
     * @see #createOSGiFrameworkLauncherCL(java.util.Properties, ClassLoader)
     * @param delegate: Parent class loader for the launcher class loader.
     */
    static ClassLoader createLauncherCL(Properties ctx, ClassLoader delegate) {
        try {
            ClassLoader osgiFWLauncherCL = createOSGiFrameworkLauncherCL(ctx, delegate);
            ClassLoaderBuilder clb = new ClassLoaderBuilder(ctx, osgiFWLauncherCL);
            clb.addLauncherJar(); // glassfish.jar
            return clb.build();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * This method is responsible for setting up the what we call "OSGi framework launcher class loader." It has
     * the following classes/jars in its search path:
     *  - OSGi framework classes,
     *  - GlassFish bootstrap apis (simple-glassfish-api.jar)
     *  - jdk tools.jar classpath.
     * OSGi framework classes are there because we want to launch the framework.
     * simple-glassfish-api.jar is needed, because we need those classes higher up in the class loader chain otherwise
     * {@link com.sun.enterprise.glassfish.bootstrap.GlassFishMain.Launcher} won't be able to see the same copy that's
     * used by rest of the system.
     * tools.jar is needed because its packages, which are exported via system bundle, are consumed by EJBC.
     * This class loader is configured to be the delegate for all bundle class loaders by setting
     * org.osgi.framework.bundle.parent=framework in OSGi configuration. Since this is the delegate for all bundle
     * class loaders, one should be very careful about adding stuff here, as it not only affects performance, it also
     * affects functionality as explained in GlassFish issue 13287.
     *
     * @param delegate: Parent class loader for this class loader.
     */
    private static ClassLoader createOSGiFrameworkLauncherCL(Properties ctx, ClassLoader delegate) {
        try {
            ClassLoaderBuilder clb = new ClassLoaderBuilder(ctx, delegate);
            clb.addFrameworkJars();
            clb.addBootstrapApiJar(); // simple-glassfish-api.jar
            clb.addJDKToolsJar();
            return clb.build();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Store relevant information in system properties.
     *
     * @param ctx
     */
    static void setSystemProperties(Properties ctx) {
        // Set the system property if downstream code wants to know about it
        System.setProperty(Constants.PLATFORM_PROPERTY_KEY, ctx.getProperty(Constants.PLATFORM_PROPERTY_KEY));
    }

    static void mergePlatformConfiguration(Properties ctx) {
        Properties platformConf = null;
        try {
            platformConf = PlatformHelper.getPlatformHelper(ctx).readPlatformConfiguration();
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
        }
        platformConf.putAll(ctx);
        Util.substVars(platformConf);
        // Starting with GlassFish 3.1.2, we allow user to overrride values specified in OSGi config file by
        // corresponding values as set via System propereties. There are two properties that we must always read
        // from OSGi config file. They are felix.fileinstall.dir and felix.fileinstall.log.level, as their values have
        // changed incompatibly from 3.1 to 3.1.1, but we are not able to change domain.xml in 3.1.1 for
        // compatibility reasons.
        Util.overrideBySystemProps(platformConf, Arrays.asList("felix.fileinstall.dir", "felix.fileinstall.log.level"));
        ctx.clear();
        ctx.putAll(platformConf);
    }

    static boolean isOSGiPlatform(String platform) {
        Constants.Platform p = Constants.Platform.valueOf(platform);
        switch (p) {
            case Felix:
            case Knopflerfish:
            case Equinox:
                return true;
        }
        return false;
    }

    static class ClassLoaderBuilder {

        protected ClassPathBuilder cpb;

        protected File glassfishDir;

        protected Properties ctx;

        ClassLoaderBuilder(Properties ctx, ClassLoader delegate) {
            this.ctx = ctx;
            cpb = new ClassPathBuilder(delegate);
            glassfishDir = new File(ctx.getProperty(Constants.INSTALL_ROOT_PROP_NAME));
        }

        void addFrameworkJars() throws IOException {
            PlatformHelper.getPlatformHelper(ctx).addFrameworkJars(cpb);
        }

        /**
         * Adds JDK tools.jar to classpath.
         */
        void addJDKToolsJar() {
            File jdkToolsJar = Util.getJDKToolsJar();
            try {
                cpb.addJar(jdkToolsJar);
            } catch (IOException ioe) {
                // on the mac, it happens all the time
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("JDK tools.jar does not exist at " + jdkToolsJar);
                }
            }
        }

        public ClassLoader build() {
            return cpb.create();
        }

        public void addLauncherJar() throws IOException {
            cpb.addJar(new File(glassfishDir, "modules/glassfish.jar"));
        }

        public void addBootstrapApiJar() throws IOException {
            cpb.addJar(new File(glassfishDir, "modules/simple-glassfish-api.jar"));
        }
    }

    static abstract class PlatformHelper {

        static synchronized PlatformHelper getPlatformHelper(Properties properties) {
            Constants.Platform platform =
                    Constants.Platform.valueOf(properties.getProperty(Constants.PLATFORM_PROPERTY_KEY));
            PlatformHelper platformHelper;
            switch (platform) {
                case Felix:
                    platformHelper = new FelixHelper();
                    break;
                case Knopflerfish:
                    platformHelper = new KnopflerfishHelper();
                    break;
                case Equinox:
                    platformHelper = new EquinoxHelper();
                    break;
                case Static:
                    platformHelper = new StaticHelper();
                    break;
                default:
                    throw new RuntimeException("Unsupported platform " + platform);
            }
            platformHelper.init(properties);
            return platformHelper;
        }

        protected Properties properties;
        protected File glassfishDir;
        protected File domainDir;
        protected File fwDir;

        /**
         * Location of the unified config properties file relative to the domain directory
         */
        public static final String CONFIG_PROPERTIES = "config/osgi.properties";

        /**
         * @param properties Initial properties
         */
        void init(Properties properties) {
            this.properties = properties;
            glassfishDir = StartupContextUtil.getInstallRoot(properties);
            domainDir = StartupContextUtil.getInstanceRoot(properties);
            setFwDir();
        }

        protected abstract void setFwDir();

        /**
         * Adds the jar files of the OSGi platform to the given {@link ClassPathBuilder}
         */
        protected abstract void addFrameworkJars(ClassPathBuilder cpb) throws IOException;

        /**
         * @return platform specific configuration information
         */
        protected Properties readPlatformConfiguration() throws IOException {
            Properties platformConfig = new Properties();
            final File configFile = getFrameworkConfigFile();
            if (configFile == null) return platformConfig;
            InputStream in = new FileInputStream(configFile);
            try {
                platformConfig.load(in);
            } finally {
                in.close();
            }
            return platformConfig;
        }

        protected File getFrameworkConfigFile() {
            String fileName = CONFIG_PROPERTIES;
            // First we search in domainDir. If it's not found there, we fall back on installDir
            File f = new File(domainDir, fileName);
            if (!f.exists()) {
                f = new File(glassfishDir, fileName);
            } else {
                logger.log(Level.INFO, LogFacade.BOOTSTRAP_FMWCONF, f.getAbsolutePath());
            }
            return f;
        }
    }

    static class FelixHelper extends PlatformHelper {
        private static final String FELIX_HOME = "FELIX_HOME";

        /**
         * Home of FW installation relative to Glassfish root installation.
         */
        public static final String GF_FELIX_HOME = "osgi/felix";

        /**
         * Location of the config properties file relative to the domain directory
         */
        public static final String CONFIG_PROPERTIES = "config/osgi.properties";

        @Override
        protected void setFwDir() {
            String fwPath = System.getenv(FELIX_HOME);
            if (fwPath == null) {
                // try system property, which comes from asenv.conf
                fwPath = System.getProperty(FELIX_HOME,
                        new File(glassfishDir, GF_FELIX_HOME).getAbsolutePath());
            }
            fwDir = new File(fwPath);
            if (!fwDir.exists()) {
                throw new RuntimeException("Can't locate Felix at " + fwPath);
            }
        }

        @Override
        protected void addFrameworkJars(ClassPathBuilder cpb) throws IOException {
            cpb.addJar(new File(fwDir, "bin/felix.jar"));
        }
      
        @Override
        protected Properties readPlatformConfiguration() throws IOException {
            // GlassFish filesystem layout does not recommend use of upper case char in file names.
            // So, we can't use ${GlassFish_Platform} to generically set the cache dir.
            // Hence, we set it here.
            Properties platformConfig = super.readPlatformConfiguration();
            platformConfig.setProperty(org.osgi.framework.Constants.FRAMEWORK_STORAGE, 
                                       new File(domainDir, "osgi-cache/felix/").getAbsolutePath());
            return platformConfig;
        }
            
    }

    static class EquinoxHelper extends PlatformHelper {

        /* if equinox is installed under glassfish/eclipse this would be the
         *  glassfish/eclipse/plugins dir that contains the equinox jars
         *  can be null
         * */
        private static File pluginsDir = null;

        protected void setFwDir() {
            String fwPath = System.getenv("EQUINOX_HOME");
            if (fwPath == null) {
                fwPath = new File(glassfishDir, "osgi/equinox").getAbsolutePath();
            }
            fwDir = new File(fwPath);
            if (!fwDir.exists()) {
                throw new RuntimeException("Can't locate Equinox at " + fwPath);
            }
        }

        @Override
        protected void addFrameworkJars(ClassPathBuilder cpb) throws IOException {
            // Add all the jars to classpath for the moment, since the jar name
            // is not a constant.
            if (pluginsDir != null) {
                cpb.addGlob(pluginsDir, "org.eclipse.osgi_*.jar");
            } else {
                cpb.addJarFolder(fwDir);
            }
        }

        @Override
        protected Properties readPlatformConfiguration() throws IOException {
            // GlassFish filesystem layout does not recommend use of upper case char in file names.
            // So, we can't use ${GlassFish_Platform} to generically set the cache dir.
            // Hence, we set it here.
            Properties platformConfig = super.readPlatformConfiguration();
            platformConfig.setProperty(org.osgi.framework.Constants.FRAMEWORK_STORAGE, 
                                       new File(domainDir, "osgi-cache/equinox/").getAbsolutePath());
            return platformConfig;
        }
    }

    static class KnopflerfishHelper extends PlatformHelper {

        private static final String KF_HOME = "KNOPFLERFISH_HOME";

        /**
         * Home of fw installation relative to Glassfish root installation.
         */
        public static final String GF_KF_HOME = "osgi/knopflerfish.org/osgi/";

        protected void setFwDir() {
            String fwPath = System.getenv(KF_HOME);
            if (fwPath == null) {
                fwPath = new File(glassfishDir, GF_KF_HOME).getAbsolutePath();
            }
            fwDir = new File(fwPath);
            if (!fwDir.exists()) {
                throw new RuntimeException("Can't locate KnopflerFish at " + fwPath);
            }
        }

        @Override
        protected void addFrameworkJars(ClassPathBuilder cpb) throws IOException {
            cpb.addJar(new File(fwDir, "framework.jar"));
        }

        @Override
        protected Properties readPlatformConfiguration() throws IOException {
            // GlassFish filesystem layout does not recommend use of upper case char in file names.
            // So, we can't use ${GlassFish_Platform} to generically set the cache dir.
            // Hence, we set it here.
            Properties platformConfig = super.readPlatformConfiguration();
            platformConfig.setProperty(org.osgi.framework.Constants.FRAMEWORK_STORAGE, 
                                       new File(domainDir, "osgi-cache/knopflerfish/").getAbsolutePath());
            return platformConfig;
        }
    }

    static class StaticHelper extends PlatformHelper {
        @Override
        protected void setFwDir() {
            // nothing to do
        }

        @Override
        protected void addFrameworkJars(ClassPathBuilder cpb) throws IOException {
            // nothing to do
        }

        @Override
        protected File getFrameworkConfigFile() {
            return null;  // no config file for this platform.
        }
    }
}

