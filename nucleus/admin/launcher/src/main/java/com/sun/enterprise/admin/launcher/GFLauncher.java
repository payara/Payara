/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2015 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.launcher;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import com.sun.enterprise.universal.collections.CollectionUtils;
import com.sun.enterprise.universal.glassfish.GFLauncherUtils;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.ProcessStreamDrainer;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import static com.sun.enterprise.util.SystemPropertyConstants.*;
import static com.sun.enterprise.admin.launcher.GFLauncherConstants.*;

/**
 * This is the main Launcher class designed for external and internal usage.
 * Each of the 3 kinds of server -- domain, node-agent and instance -- need to
 * subclass this class.
 *
 * @author bnevins
 */
public abstract class GFLauncher {
    ///////////////////////////////////////////////////////////////////////////
    //////     PUBLIC api area starts here             ////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     *
     * @return The info object that contains startup info
     */
    public final GFLauncherInfo getInfo() {
        return info;
    }

    /**
     * Launches the server. Any fatal error results in a GFLauncherException No
     * unchecked Throwables of any kind will be thrown.
     *
     * @throws com.sun.enterprise.admin.launcher.GFLauncherException
     */
    public final void launch() throws GFLauncherException {
        try {
            startTime = System.currentTimeMillis();
            if (!setupCalledByClients)
                setup();
            internalLaunch();
        }
        catch (GFLauncherException gfe) {
            throw gfe;
        }
        catch (Throwable t) {
            // hk2 might throw a java.lang.Error
            throw new GFLauncherException(strings.get("unknownError", t.getMessage()), t);
        }
        finally {
            GFLauncherLogger.removeLogFileHandler();
        }
    }

    /**
     * Launches the server - but forces the setup() to go through again.
     *
     * @throws com.sun.enterprise.admin.launcher.GFLauncherException
     */
    public final void relaunch() throws GFLauncherException {
        setupCalledByClients = false;
        launch();
    }

    public final void launchJVM(List<String> cmdsIn) throws GFLauncherException {
        try {
            setup();    // we only use one thing -- the java executable
            List<String> commands = new LinkedList<String>();
            commands.add(javaExe);

            for (String cmd : cmdsIn) {
                commands.add(cmd);
            }

            ProcessBuilder pb = new ProcessBuilder(commands);
            Process p = pb.start();
            ProcessStreamDrainer.drain("launchJVM", p); // just to be safe
        }
        catch (GFLauncherException gfe) {
            throw gfe;
        }
        catch (Throwable t) {
            // hk2 might throw a java.lang.Error
            throw new GFLauncherException(strings.get("unknownError", t.getMessage()), t);
        }
        finally {
            GFLauncherLogger.removeLogFileHandler();
        }
    }

    public void setup() throws GFLauncherException, MiniXmlParserException {
        ASenvPropertyReader pr;
        if (isFakeLaunch()) {
            pr = new ASenvPropertyReader(info.getInstallDir());
        }
        else {
            pr = new ASenvPropertyReader();
        }

        asenvProps = pr.getProps();
        info.setup();
        setupLogLevels();
        MiniXmlParser parser = new MiniXmlParser(getInfo().getConfigFile(), getInfo().getInstanceName());
        String domainName = parser.getDomainName();
        if (GFLauncherUtils.ok(domainName)) {
            info.setDomainName(domainName);
        }
        info.setAdminAddresses(parser.getAdminAddresses());
        javaConfig = new JavaConfig(parser.getJavaConfig());
        setupProfilerAndJvmOptions(parser);
        setupUpgradeSecurity();

        Map<String, String> realmprops = parser.getAdminRealmProperties();
        if (realmprops != null) {
            String classname = realmprops.get("classname");
            String keyfile = realmprops.get("file");
            if ("com.sun.enterprise.security.auth.realm.file.FileRealm".equals(classname)
                    && keyfile != null) {
                adminFileRealmKeyFile = keyfile;
            }
        }

        secureAdminEnabled = parser.getSecureAdminEnabled();

        renameOsgiCache();
        setupMonitoring(parser);
        sysPropsFromXml = parser.getSystemProperties();
        asenvProps.put(INSTANCE_ROOT_PROPERTY, getInfo().getInstanceRootDir().getPath());

        // Set the config java-home value as the Java home for the environment,
        // unless it is empty or it is already refering to a substitution of
        // the environment variable.
        String jhome = javaConfig.getJavaHome();
        if (GFLauncherUtils.ok(jhome) && !jhome.trim().equals("${" + JAVA_ROOT_PROPERTY + "}")) {
            asenvProps.put(JAVA_ROOT_PROPERTY, jhome);
        }
        debugOptions = getDebug();
        parseDebug();
        parser.setupConfigDir(getInfo().getConfigDir(), getInfo().getInstallDir());
        setLogFilename(parser);
        resolveAllTokens();
        fixLogFilename();
        GFLauncherLogger.addLogFileHandler(logFilename, info);
        setJavaExecutable();
        setClasspath();
        setCommandLine();
        setJvmOptions();
        logCommandLine();
        // if no <network-config> element, we need to upgrade this domain
        needsAutoUpgrade = !parser.hasNetworkConfig();
        needsManualUpgrade = !parser.hasDefaultConfig();
        setupCalledByClients = true;
    }

    /**
     * Returns the admin realm key file for the server, if the admin realm is a
     * FileRealm. Otherwise return null. This value can be used to create a
     * FileRealm for the server.
     */
    public String getAdminRealmKeyFile() {
        return adminFileRealmKeyFile;
    }

    /**
     * Returns true if secure admin is enabled
     */
    public boolean isSecureAdminEnabled() {
        return secureAdminEnabled;
    }

    /**
     * Returns the exit value of the process. This only makes sense when we ran
     * in verbose mode and waited for the process to exit in the wait() method.
     * Caveat Emptor!
     *
     * @return the process' exit value if it completed and we waited. Otherwise
     * it returns -1
     */
    public final int getExitValue() {
        return exitValue;
    }

    /**
     * You don't want to call this before calling launch because it would not
     * make sense.
     *
     * @return The Process object of the launched Server process. you will
     * either get a valid Process object or an Exceptio will be thrown. You are
     * guaranteed not to get a null.
     * @throws GFLauncherException if the Process has not been created yet -
     * call launch() before calling this method.
     */
    public final Process getProcess() throws GFLauncherException {
        if (process == null)
            throw new GFLauncherException("invalid_process");

        return process;
    }

    /**
     * A ProcessStreamDrainer is always attached to every Process created here.
     * It is handy for getting the stdin and stdout as a nice String.
     *
     * @return A valid ProcessStreamDrainer. You are guaranteed to never get a
     * null.
     * @throws GFLauncherException if the process has not launched yet
     * @see com.sun.enterprise.universal.process.ProcessStreamDrainer
     */
    public final ProcessStreamDrainer getProcessStreamDrainer() throws GFLauncherException {
        if (psd == null)
            throw new GFLauncherException("invalid_psd");

        return psd;
    }

    /**
     * Get the location of the server logfile
     *
     * @return The full path of the logfile
     * @throws GFLauncherException if you call this method too early
     */
    public String getLogFilename() throws GFLauncherException {
        if (!logFilenameWasFixed)
            throw new GFLauncherException(strings.get("internalError") + " call to getLogFilename() before it has been initialized");

        return logFilename;
    }

    /**
     * Return the port number of the debug port, or -1 if debugging is not
     * enabled.
     *
     * @return the debug port, or -1 if not debugging
     */
    public final int getDebugPort() {
        return debugPort;
    }

    /**
     * Return true if suspend=y AND debugging is on. otherwise return false.
     *
     * @return the debug port, or -1 if not debugging
     */
    public final boolean isDebugSuspend() {
        return debugPort >= 0 && debugSuspend;
    }

    /**
     * Does this domain need to be automatically upgraded before it can be
     * started?
     *
     * @return true if the domain needs to be upgraded first
     */
    public final boolean needsAutoUpgrade() {
        return needsAutoUpgrade;
    }

    /**
     * Does this domain need to be manually upgraded before it can be started?
     *
     * @return true if the domain needs to be upgraded first
     */
    public final boolean needsManualUpgrade() {
        return needsManualUpgrade;
    }

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    //////     ALL private and package-private below   ////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    abstract void internalLaunch() throws GFLauncherException;

    private void parseDebug() {
        // look for an option of this form:
        // -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009
        // and extract the suspend and port values
        for (String opt : debugOptions) {
            if (!opt.startsWith("-Xrunjdwp:"))
                continue;
            String[] attrs = opt.substring(10).split(",");
            for (String attr : attrs) {
                if (attr.startsWith("address=")) {
                    try {
                        debugPort = Integer.parseInt(attr.substring(8));
                    }
                    catch (NumberFormatException ex) {
                        debugPort = -1;
                    }
                }
                if (attr.startsWith("suspend=")) {
                    try {
                        debugSuspend = attr.substring(8).toLowerCase(Locale.getDefault()).equals("y");
                    }
                    catch (Exception ex) {
                        debugSuspend = false;
                    }
                }
            }
        }
    }

    private void setLogFilename(MiniXmlParser parser) throws GFLauncherException {
        logFilename = parser.getLogFilename();

        if (logFilename == null)
            logFilename = DEFAULT_LOGFILE;
    }

    private void fixLogFilename() throws GFLauncherException {
        if (!GFLauncherUtils.ok(logFilename))
            logFilename = DEFAULT_LOGFILE;

        File f = new File(logFilename);

        if (!f.isAbsolute()) {
            // this is quite normal.  Logging Service will by default return
            // a relative path!
            f = new File(info.getInstanceRootDir(), logFilename);
        }

        // Get rid of garbage like "c:/gf/./././../gf"
        f = SmartFile.sanitize(f);

        // if the file doesn't exist -- make sure the parent dir exists
        // this is common in unit tests AND the first time the instance is
        // started....

        if (!f.exists()) {
            File parent = f.getParentFile();
            if (!parent.isDirectory()) {
                boolean wasCreated = parent.mkdirs();
                if (!wasCreated) {
                    f = null; // give up!!
                }
            }
        }

        if (f == null)
            logFilename = null;
        else
            logFilename = f.getPath();

        logFilenameWasFixed = true;
    }

    // unit tests will want 'fake' so that the process is not really started.
    enum LaunchType {
        normal, debug, trace, fake
    };

    void setMode(LaunchType mode) {
        this.mode = mode;
    }

    LaunchType getMode() {
        return mode;
    }

    boolean isFakeLaunch() {
        return mode == LaunchType.fake;
    }

    abstract List<File> getMainClasspath() throws GFLauncherException;

    abstract String getMainClass() throws GFLauncherException;

    GFLauncher(GFLauncherInfo info) {
        this.info = info;
    }

    final Map<String, String> getEnvProps() {
        return asenvProps;
    }

    public final List<String> getCommandLine() {
        return commandLine;
    }

    final long getStartTime() {
        return startTime;
    }

    void launchInstance() throws GFLauncherException, MiniXmlParserException {
        if (isFakeLaunch()) {
            return;
        }

        List<String> cmds = null;
	// Use launchctl bsexec on MacOS versions before 10.10
	// otherwise use regular startup.
 	// (No longer using StartupItemContext).
	// See GLASSFISH-21343
        if (OS.isDarwin() && useLaunchCtl(System.getProperty("os.version")) && 
	    (!getInfo().isVerboseOrWatchdog())) {
            // On MacOS we need to start long running process with
            // StartupItemContext. See IT 12942
            cmds = new ArrayList<String>();
            //cmds.add("/usr/libexec/StartupItemContext");
            // In MacOS 10.10 they removed StartupItemContext
            // so call launchctl directly doing what StartupItemContext did
            // See GLASSFISH-21113
            cmds.add("launchctl");
            cmds.add("bsexec");
            cmds.add("/");
            cmds.addAll(getCommandLine());
        }
        else {
            cmds = getCommandLine();
        }

        ProcessBuilder pb = new ProcessBuilder(cmds);

        //pb.directory(getInfo().getConfigDir());


        // change the directory if there is one specified, o/w stick with the
        // default.
        try {
            File newDir = getInfo().getConfigDir();
            pb.directory(newDir);
        }
        catch (Exception e) {
        }

        //run the process and attach Stream Drainers
        try {
            closeStandardStreamsMaybe();
            process = pb.start();
            final String name = getInfo().getDomainName();

            // verbose trumps watchdog.
            if (getInfo().isVerbose()) {
                psd = ProcessStreamDrainer.redirect(name, process);
            }
            else if (getInfo().isWatchdog()) {
                psd = ProcessStreamDrainer.dispose(name, process);
            }
            else {
                psd = ProcessStreamDrainer.save(name, process);
            }
            writeSecurityTokens(process);
        }
        catch (Exception e) {
            throw new GFLauncherException("jvmfailure", e, e);
        }

        //if verbose, hang around until the domain stops
        if (getInfo().isVerboseOrWatchdog())
            wait(process);
    }

    /**
     * Checks whether to use launchctl for start up by checking if mac os version < 10.10
     *
     * @return  True if osversion < 10.10 
     */
    private static boolean useLaunchCtl(String osversion) {

        int major = 0;
        int minor = 0;

	if (osversion == null || osversion.isEmpty())
	    return false;

        String[] split = osversion.split("[\\._\\-]+");

	try {
            if (split.length > 0  && split[0].length() > 0) {
                major = Integer.parseInt(split[0]);
            }
            if (split.length > 1 && split[1].length() > 0) {
                minor = Integer.parseInt(split[1]);
            }
	    return ((major <= 9) || (major <= 10 && minor < 10));
	}
	catch (NumberFormatException e) {
	    // Assume version is 10.10 or later.
	    return false;
	}
    }

    private void writeSecurityTokens(Process sp) throws GFLauncherException, IOException {
        handleDeadProcess();
        OutputStream os = sp.getOutputStream();
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        try {
            osw = new OutputStreamWriter(os);
            bw = new BufferedWriter(osw);
            for (String token : info.securityTokens) {
                bw.write(token);
                bw.newLine();
                bw.flush();      //flushing once is ok too
            }
        }
        catch (IOException e) {
            handleDeadProcess();
            throw e;   //process is not dead, but got some other exception, rethrow it
        }
        finally {
            if (bw != null) {
                bw.close();
            }
            if (osw != null) {
                osw.close();
            }
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException ioe) {
                    // nothing to do
                }
            }
            if (bw != null) {
                handleDeadProcess();
            }
        }
    }

    private void handleDeadProcess() throws GFLauncherException {
        String trace = getDeadProcessTrace(process);
        if (trace != null)
            throw new GFLauncherException(trace);
    }

    private String getDeadProcessTrace(Process sp) throws GFLauncherException {
        //returns null in case the process is NOT dead
        try {
            int ev = sp.exitValue();
            ProcessStreamDrainer psd1 = getProcessStreamDrainer();
            String output = psd1.getOutErrString();
            String trace = strings.get("server_process_died", ev, output);
            return trace;
        }
        catch (IllegalThreadStateException e) {
            //the process is still running and we are ok
            return null;
        }
    }

    void setCommandLine() throws GFLauncherException {
        List<String> cmdLine = getCommandLine();
        cmdLine.clear();
        addIgnoreNull(cmdLine, javaExe);
        addIgnoreNull(cmdLine, "-cp");
        addIgnoreNull(cmdLine, getClasspath());
        addIgnoreNull(cmdLine, debugOptions);

        String CLIStartTime = System.getProperty("WALL_CLOCK_START");

        if (CLIStartTime != null && CLIStartTime.length() > 0) {
            cmdLine.add("-DWALL_CLOCK_START=" + CLIStartTime);
        }

        if (jvmOptions != null)
            addIgnoreNull(cmdLine, jvmOptions.toStringArray());

        GFLauncherNativeHelper nativeHelper = new GFLauncherNativeHelper(info, javaConfig, jvmOptions, profiler);
        addIgnoreNull(cmdLine, nativeHelper.getCommands());
        addIgnoreNull(cmdLine, getMainClass());

        try {
            addIgnoreNull(cmdLine, getInfo().getArgsAsList());
        }
        catch (GFLauncherException gfle) {
            throw gfle;
        }
        catch (Exception e) {
            //harmless
        }
    }

    void setJvmOptions() throws GFLauncherException {
        List<String> jvmOpts = getJvmOptions();
        jvmOpts.clear();

        if (jvmOptions != null)
            addIgnoreNull(jvmOpts, jvmOptions.toStringArray());

    }

    public final List<String> getJvmOptions() {
        return jvmOptionsList;
    }

    private void addIgnoreNull(List<String> list, String s) {
        if (GFLauncherUtils.ok(s))
            list.add(s);
    }

    private void addIgnoreNull(List<String> list, Collection<String> ss) {
        if (ss != null && !ss.isEmpty())
            list.addAll(ss);
    }

    private void wait(final Process p) throws GFLauncherException {
        try {
            setShutdownHook(p);
            p.waitFor();
            exitValue = p.exitValue();
        }
        catch (InterruptedException ex) {
            throw new GFLauncherException("verboseInterruption", ex, ex);
        }
    }

    private void setShutdownHook(final Process p) {
        // ON UNIX a ^C on the console will also kill DAS
        // On Windows a ^C on the console will not kill DAS
        // We want UNIX behavior on Windows
        // note that the hook thread will run in both cases:
        // 1. the server died on its own, e.g. with a stop-domain
        // 2. a ^C (or equivalent signal) was received by the console
        // note that exitValue is still set to -1

        // if we are restarting we may get many many processes.
        // Each time this method is called we reset the Process reference inside
        // the processWhacker

        if (processWhacker == null) {
            Runtime runtime = Runtime.getRuntime();
            final String msg = strings.get("serverStopped", info.getType());
            processWhacker = new ProcessWhacker(p, msg);
            runtime.addShutdownHook(new Thread(processWhacker));
        }
        else
            processWhacker.setProcess(p);
    }

    private void resolveAllTokens() {
        // resolve jvm-options against:
        // 1. itself
        // 2. <system-property>'s from domain.xml
        // 3. system properties -- essential there is, e.g. "${path.separator}" in domain.xml
        // 4. asenvProps
        // 5. env variables
        // i.e. add in reverse order to get the precedence right

        Map<String, String> all = new HashMap<String, String>();
        Map<String, String> envProps = System.getenv();
        Map<String, String> sysProps =
                CollectionUtils.propertiesToStringMap(System.getProperties());

        // TODO: Uncomment when admin password processing & aliasing is sorted out.

        // Map<String, String> passwordAliases = new HashMap<String, String>();
        // try {
        //     String masterPassword = "changeit";
        //     if (IdentityManager.getMasterPassword() != null)
        //         masterPassword = IdentityManager.getMasterPassword();
        //     PasswordAdapter pa = new PasswordAdapter(masterPassword.toCharArray());
        //     Enumeration e = pa.getAliases();
        //     if (e.hasMoreElements()) {
        //         String alias = (String) e.nextElement();
        //         passwordAliases.put(alias, pa.getPasswordForAlias(alias));
        //     }
        // } catch (Exception e) {
        //     // TODO: ignore now. Defaults to not resolving password aliases
        // }
        // all.putAll(passwordAliases);

        all.putAll(envProps);
        all.putAll(asenvProps);
        all.putAll(sysProps);
        all.putAll(sysPropsFromXml);
        all.putAll(jvmOptions.getCombinedMap());
        all.putAll(profiler.getConfig());
        TokenResolver resolver = new TokenResolver(all);
        resolver.resolve(jvmOptions.xProps);
        resolver.resolve(jvmOptions.xxProps);
        resolver.resolve(jvmOptions.plainProps);
        resolver.resolve(jvmOptions.sysProps);
        resolver.resolve(javaConfig.getMap());
        resolver.resolve(profiler.getConfig());
        resolver.resolve(debugOptions);
        //resolver.resolve(sysPropsFromXml);
        logFilename = resolver.resolve(logFilename);
        adminFileRealmKeyFile = resolver.resolve(adminFileRealmKeyFile);

        // TODO ?? Resolve sysPropsFromXml ???
    }

    private void setJavaExecutable() throws GFLauncherException {
        // first choice is from domain.xml
        if (setJavaExecutableIfValid(javaConfig.getJavaHome()))
            return;

        // second choice is from asenv
        if (!setJavaExecutableIfValid(asenvProps.get(JAVA_ROOT_PROPERTY)))
            throw new GFLauncherException("nojvm");

    }

    void setClasspath() throws GFLauncherException {
        List<File> mainCP = getMainClasspath(); // subclass provides this
        List<File> envCP = javaConfig.getEnvClasspath();
        List<File> sysCP = javaConfig.getSystemClasspath();
        List<File> prefixCP = javaConfig.getPrefixClasspath();
        List<File> suffixCP = javaConfig.getSuffixClasspath();
        List<File> profilerCP = profiler.getClasspath();

        // create a list of all the classpath pieces in the right order
        List<File> all = new ArrayList<File>();
        all.addAll(prefixCP);
        all.addAll(profilerCP);
        all.addAll(mainCP);
        all.addAll(sysCP);
        all.addAll(envCP);
        all.addAll(suffixCP);
        setClasspath(GFLauncherUtils.fileListToPathString(all));
    }

    boolean setJavaExecutableIfValid(String filename) {
        if (!GFLauncherUtils.ok(filename)) {
            return false;
        }

        File f = new File(filename);

        if (!f.isDirectory()) {
            return false;
        }

        if (GFLauncherUtils.isWindows()) {
            f = new File(f, "bin/java.exe");
        }
        else {
            f = new File(f, "bin/java");
        }

        if (f.exists()) {
            javaExe = SmartFile.sanitize(f).getPath();
            return true;
        }
        return false;
    }

    private List<String> getDebug() {
        if (info.isDebug() || javaConfig.isDebugEnabled()) {
            return javaConfig.getDebugOptions();
        }
        return Collections.emptyList();
    }

    private void setupProfilerAndJvmOptions(MiniXmlParser parser) throws MiniXmlParserException, GFLauncherException {
        // add JVM options from Profiler *last* so they override config's
        // JVM options

        profiler = new Profiler(
                parser.getProfilerConfig(),
                parser.getProfilerJvmOptions(),
                parser.getProfilerSystemProperties());

        List<String> rawJvmOptions = parser.getJvmOptions();
        rawJvmOptions.addAll(getSpecialSystemProperties());
        if (profiler.isEnabled()) {
            rawJvmOptions.addAll(profiler.getJvmOptions());
        }
        jvmOptions = new JvmOptions(rawJvmOptions);
        if (info.isDropInterruptedCommands()) {
            jvmOptions.sysProps.put(SystemPropertyConstants.DROP_INTERRUPTED_COMMANDS, Boolean.TRUE.toString());
        }
    }

    private void setupUpgradeSecurity() throws GFLauncherException {
        // If this is an upgrade and the security manager is on,
        // copy the current server.policy file to the domain
        // before the upgrade.
        if (info.isUpgrade()
                && jvmOptions.sysProps.containsKey("java.security.manager")) {

            GFLauncherLogger.info(GFLauncherLogger.copy_server_policy);

            File source = new File(new File(new File(info.installDir, "lib"),
                    "templates"), "server.policy");
            File target = new File(info.getConfigDir(), "server.policy");

            try {
                FileUtils.copyFile(source, target);
            }
            catch (IOException ioe) {
                // the actual error is wrapped differently depending on
                // whether the problem was with the source or target
                Throwable cause = ioe.getCause() == null ? ioe : ioe.getCause();
                throw new GFLauncherException(strings.get(
                        "copy_server_policy_error", cause.getMessage()));
            }
        }
    }

    /**
     * Because of some issues in GlassFish OSGi launcher, a server updated from
     * version 3.0.x to 3.1 won't start if a OSGi cache directory populated with
     * 3.0.x modules is used. So, as a work around, we rename the cache
     * directory when upgrade path is used. See GLASSFISH-15772 for more
     * details.
     *
     * @throws GFLauncherException if it fails to rename the cache directory
     */
    private void renameOsgiCache() throws GFLauncherException {
        if (info.isUpgrade()) {
            File osgiCacheDir = new File(info.getDomainRootDir(),
                    "osgi-cache");
            File backupOsgiCacheDir = new File(info.getDomainRootDir(),
                    "osgi-cache-" + System.currentTimeMillis());
            if (osgiCacheDir.exists() && !backupOsgiCacheDir.exists()) {
                if (!FileUtils.renameFile(osgiCacheDir, backupOsgiCacheDir)) {
                    throw new GFLauncherException(strings.get("rename_osgi_cache_failed", osgiCacheDir, backupOsgiCacheDir));
                }
                else {
                    GFLauncherLogger.fine("rename_osgi_cache_succeeded", osgiCacheDir, backupOsgiCacheDir);
                }
            }
        }
    }

    private void setupMonitoring(MiniXmlParser parser) throws GFLauncherException {
        // As usual we have to be very careful.

        // If it is NOT enabled -- we are out of here!!!
        if (parser.isMonitoringEnabled() == false)
            return;

        // if the user has a hard-coded "-javaagent" jvm-option that uses OUR jar
        // then we do NOT want to add our own.
        Set<String> plainKeys = jvmOptions.plainProps.keySet();
        for (String key : plainKeys) {
            if (key.startsWith("javaagent:")) {
                // complications -- of course!!  They may have mix&match forward and back slashes
                key = key.replace('\\', '/');
                if (key.indexOf(FLASHLIGHT_AGENT_NAME) >= 0)
                    return; // Done!!!!
            }
        }

        // It is not already specified AND monitoring is enabled.
        try {
            jvmOptions.plainProps.put(getMonitoringAgentJvmOptionString(), null);
        }
        catch (GFLauncherException gfe) {
            // This has been defined as a non-fatal error.
            // Silently ignore it -- but do NOT add it as an option
        }
    }

    private String getMonitoringAgentJvmOptionString() throws GFLauncherException {
        File libMonDir = new File(getInfo().getInstallDir(), LIBMON_NAME);
        File flashlightJarFile = new File(libMonDir, FLASHLIGHT_AGENT_NAME);

        if (flashlightJarFile.isFile())
            return "javaagent:" + getCleanPath(flashlightJarFile);
        // No agent jar...
        else {
            String msg = strings.get("no_flashlight_agent", flashlightJarFile);
            GFLauncherLogger.warning(GFLauncherLogger.NO_FLASHLIGHT_AGENT, flashlightJarFile);
            throw new GFLauncherException(msg);
        }
    }

    private static String getCleanPath(File f) {
        return SmartFile.sanitize(f).getPath().replace('\\', '/');
    }

    private List<String> getSpecialSystemProperties() throws GFLauncherException {
        Map<String, String> props = new HashMap<String, String>();
        props.put(INSTALL_ROOT_PROPERTY, getInfo().getInstallDir().getAbsolutePath());
        props.put(INSTANCE_ROOT_PROPERTY, getInfo().getInstanceRootDir().getAbsolutePath());
        return (this.propsToJvmOptions(props));
    }

    void logCommandLine() {
        StringBuilder sb = new StringBuilder();

        if (!isFakeLaunch()) {
            Iterable<String> cmdLine = getCommandLine();

            for (String s : cmdLine) {
                sb.append(NEWLINE);
                sb.append(s);
            }
            GFLauncherLogger.info(GFLauncherLogger.COMMAND_LINE, sb.toString());
        }
    }

    String getClasspath() {
        return classpath;
    }

    void setClasspath(String s) {
        classpath = s;
    }

    private List<String> propsToJvmOptions(Map<String, String> map) {
        List<String> ss = new ArrayList<String>();
        Set<Map.Entry<String, String>> entries = map.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String value = entry.getValue();
            String jvm = "-D" + name;

            if (value != null) {
                jvm += "=" + value;
            }

            ss.add(jvm);
        }

        return ss;
    }

    private void setupLogLevels() {
        if (info.isVerbose())
            GFLauncherLogger.setConsoleLevel(Level.INFO);
        else
            GFLauncherLogger.setConsoleLevel(Level.WARNING);
    }

    private void closeStandardStreamsMaybe() {
        // see issue 12832
        // Windows bug/feature -->
        // Say process A (ssh) creates Process B (asadmin start-instance )
        // which then fires up Process C (the instance).
        // Process B exits but Process A does NOT.  Process A is waiting for
        // Process C to exit.
        // The solution is to close down the standard streams BEFORE creating
        // Process C.  Then Process A becomes convinced that the process it created
        // has finished.
        // If there is a console that means the user is sitting at the terminal
        // directly and we don't have to worry about it.
        // Note that the issue is inside SSH -- not inside GF code per se.  I.e.
        // Process B absolutely positively does exit whether or not this code runs...
        // don't run this unless we have to because our "..." messages disappear.

        if (System.console() == null && OS.isWindows() && !(info.isVerboseOrWatchdog())) {
            String sname;

            if (info.isDomain())
                sname = info.getDomainName();
            else
                sname = info.getInstanceName();

            System.out.println(strings.get("ssh", sname));
            try {
                System.in.close();
            }
            catch (Exception e) { // ignore
            }
            try {
                System.err.close();
            }
            catch (Exception e) { // ignore
            }
            try {
                System.out.close();
            }
            catch (Exception e) { // ignore
            }
        }
    }
    private List<String> commandLine = new ArrayList<String>();
    private List<String> jvmOptionsList = new ArrayList<String>();
    private GFLauncherInfo info;
    private Map<String, String> asenvProps;
    private JavaConfig javaConfig;
    private JvmOptions jvmOptions;
    private Profiler profiler;
    private Map<String, String> sysPropsFromXml;
    private String javaExe;
    private String classpath;
    private String adminFileRealmKeyFile;
    private boolean secureAdminEnabled;
    private List<String> debugOptions;
    private long startTime;
    private String logFilename;
    private LaunchType mode = LaunchType.normal;
    private final static LocalStringsImpl strings = new LocalStringsImpl(GFLauncher.class);
    private boolean setupCalledByClients = false; //handle with care
    private int exitValue = -1;
    private ProcessWhacker processWhacker;
    private Process process;
    private ProcessStreamDrainer psd;
    private boolean logFilenameWasFixed = false;
    private boolean needsAutoUpgrade = false;
    private boolean needsManualUpgrade = false;
    private int debugPort = -1;
    private boolean debugSuspend = false;

    ///////////////////////////////////////////////////////////////////////////
    private static class ProcessWhacker implements Runnable {
        ProcessWhacker(Process p, String msg) {
            message = msg;
            process = p;
        }

        void setProcess(Process p) {
            process = p;
        }

        @Override
        public void run() {
            // we are in a shutdown hook -- most of the JVM is gone.
            // logger won't work anymore...
            System.out.println(message);
            process.destroy();
        }
        private String message;
        private Process process;
    }
}
