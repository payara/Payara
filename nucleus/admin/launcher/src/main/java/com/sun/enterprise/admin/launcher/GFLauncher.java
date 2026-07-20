/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2019 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package com.sun.enterprise.admin.launcher;

import com.sun.enterprise.universal.collections.CollectionUtils;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.universal.glassfish.GFLauncherUtils;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.ProcessStreamDrainer;
import com.sun.enterprise.universal.xml.MiniXmlParser;
import com.sun.enterprise.universal.xml.MiniXmlParserException;
import com.sun.enterprise.util.JDK;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.admin.launcher.PayaraDefaultJvmOptions;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sun.enterprise.admin.launcher.GFLauncherConstants.*;
import static com.sun.enterprise.util.SystemPropertyConstants.*;

/**
 * This is the main Launcher class designed for external and internal usage.
 * Each of the 3 kinds of server -- domain, node-agent and instance -- need to
 * subclass this class.
 *
 * @author bnevins
 */
public abstract class GFLauncher {

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(".* version \"([^\"\\-]+)(-.*)?\".*");
    private final List<String> commandLine = new ArrayList<String>();
    private final GFLauncherInfo info;
    private Map<String, String> asenvProps;
    private JavaConfig javaConfig;
    private JvmOptions jvmOptions;
    private Map<String, String> sysPropsFromXml;
    private String javaExe;
    private String classpath;
    private String adminFileRealmKeyFile;
    private boolean secureAdminEnabled;
    private List<String> debugOptions;
    private long startTime;
    private String logFilename;
    private LaunchType mode = LaunchType.normal;
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(GFLauncher.class);
    private boolean setupCalledByClients = false; //handle with care
    private int exitValue = -1;
    private ProcessWhacker processWhacker;
    private Process process;
    private ProcessStreamDrainer psd;
    private boolean logFilenameWasFixed = false;
    private int debugPort = -1;
    private boolean debugSuspend = false;
    
    
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
    @SuppressWarnings("UseSpecificCatch")
    public final void launch() throws GFLauncherException {
        try {
            startTime = System.currentTimeMillis();
            if (!setupCalledByClients) {
                setup();
            }
            internalLaunch();
        } catch (GFLauncherException gfe) {
            throw gfe;
        } catch (Throwable t) {
            // hk2 might throw a java.lang.Error
            throw new GFLauncherException(STRINGS.get("unknownError", t.getMessage()), t);
        } finally {
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

    public void setup() throws GFLauncherException, MiniXmlParserException {
        ASenvPropertyReader pr;
        if (isFakeLaunch()) {
            pr = new ASenvPropertyReader(info.getInstallDir());
        } else {
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
        // Set the config java-home value as the Java home for the environment,
        // unless it is empty or it is already refering to a substitution of
        // the environment variable.
        String jhome = javaConfig.getJavaHome();
        if (GFLauncherUtils.ok(jhome) && !jhome.trim().equals("${" + JAVA_ROOT_PROPERTY + "}")) {
            asenvProps.put(JAVA_ROOT_PROPERTY, jhome);
        }
        setJavaExecutable();
        setupJvmOptions(parser);

        Map<String, String> realmprops = parser.getAdminRealmProperties();
        if (realmprops != null) {
            String classname = realmprops.get("classname");
            String keyfile = realmprops.get("file");
            if ("com.sun.enterprise.security.auth.realm.file.FileRealm".equals(classname) && keyfile != null) {
                adminFileRealmKeyFile = keyfile;
            }
        }

        secureAdminEnabled = parser.getSecureAdminEnabled();

        setupMonitoring(parser);
        sysPropsFromXml = parser.getSystemProperties();
        asenvProps.put(INSTANCE_ROOT_PROPERTY, getInfo().getInstanceRootDir().getPath());

        debugOptions = getDebug();
        parseDebug();
        parser.setupConfigDir(getInfo().getConfigDir());
        setLogFilename(parser);
        resolveAllTokens();
        fixLogFilename();
        GFLauncherLogger.addLogFileHandler(logFilename);
        setClasspath();
        setCommandLine();
        logCommandLine();
        checkJDKVersion();
        setupCalledByClients = true;
    }
    
    public Map<String, String> getSysPropsFromXml() {
        return sysPropsFromXml;
    }

    /**
     * Returns the admin realm key file for the server, if the admin realm is a
     * FileRealm. Otherwise return null. This value can be used to create a
     * FileRealm for the server.
     * @return 
     */
    public String getAdminRealmKeyFile() {
        return adminFileRealmKeyFile;
    }

    /**
     * Returns true if secure admin is enabled
     * @return 
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
     * either get a valid Process object or an Exception will be thrown. You are
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
            throw new GFLauncherException(STRINGS.get("internalError") + " call to getLogFilename() before it has been initialized");

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


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    //////     ALL private and package-private below   ////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * Internal launcher which in turn calls {@link #launchInstance()}
     * @throws GFLauncherException 
     */
    abstract void internalLaunch() throws GFLauncherException;

    private void parseDebug() {
        // look for an option of this form:
        // -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9009
        // or
        // -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9009
        // and extract the suspend and port values
        for (String opt : debugOptions) {
            if (isJdwpOption(opt)) {
              debugPort = extractDebugPort(opt);
              debugSuspend = extractDebugSuspend(opt);
            }
        }
    }

    static boolean isJdwpOption(String option) {
        return option.startsWith("-Xrunjdwp:") || option.startsWith("-agentlib:jdwp");
    }

    private static final String DEBUG_ADDRESS_PORT_GROUP = "port";
    private static final Pattern DEBUG_ADDRESS_PATTERN = Pattern.compile(".*address=(?<hostWithColon>(?<host>.+):)?(?<port>\\d*).*");

    static int extractDebugPort(String option) {
        Matcher m = DEBUG_ADDRESS_PATTERN.matcher(option);
        if (!m.matches()) {
            return -1;
        }
        try {
            String portGroup = m.group(DEBUG_ADDRESS_PORT_GROUP);
            return Integer.parseInt(portGroup);
        } catch (NumberFormatException nfex) {
            return -1;
        }
    }

    static boolean extractDebugSuspend(String option) {
        Pattern suspendRegex = Pattern.compile(".*suspend=[yY](?:,.*|$)");
        Matcher m = suspendRegex.matcher(option);
        return m.matches();
    }

    private void setLogFilename(MiniXmlParser parser) {
        
        // Check if launching an instance to get appropriate logFilename
        if (info.isInstance()) {
            logFilename = parser.getInstanceLogFilename();

        } else /*  Assume DAS if not an instance */{
            logFilename = parser.getLogFilename();
        }

        if (logFilename == null) {
            logFilename = DEFAULT_LOGFILE;
        }
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
    }

    void setMode(LaunchType mode) {
        this.mode = mode;
    }

    LaunchType getMode() {
        return mode;
    }

    boolean isFakeLaunch() {
        return mode == LaunchType.fake;
    }

    /**
     * Returns a list of all the files to be loaded as part of the main classpath.
     * This includes all module jars.
     * @return
     * @throws GFLauncherException if Payara is running in embedded mode
     */
    abstract List<File> getMainClasspath() throws GFLauncherException;

    /**
     * Returns the class of Payara containg the {@code main()} function
     * @return
     * @throws GFLauncherException 
     */
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

    /**
     * Launches the instance
     * @throws GFLauncherException if there was an error with the process of starting the instance
     * @throws MiniXmlParserException if there was an error reading the domain.xml
     */
    void launchInstance() throws GFLauncherException, MiniXmlParserException {
        if (isFakeLaunch()) {
            return;
        }

        List<String> cmds;
	// Use launchctl bsexec on MacOS versions before 10.10
	// otherwise use regular startup.
 	// (No longer using StartupItemContext).
	// See GLASSFISH-21343
        if (OS.isDarwin() && useLaunchCtl(System.getProperty("os.version")) && 
	    (!getInfo().isVerboseOrWatchdog())) {
            // On MacOS we need to start long running process with
            // StartupItemContext. See IT 12942
            cmds = new ArrayList<String>();
            // In MacOS 10.10 they removed StartupItemContext
            // so call launchctl directly doing what StartupItemContext did
            // See GLASSFISH-21113
            // remove bsexec as from 10.10.3 bsexec requires sudo
	    cmds.add("nohup");
            cmds.addAll(getCommandLine());            
        } else {
            cmds = getCommandLine();
        }

        ProcessBuilder pb = new ProcessBuilder(cmds);

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

            // Under SSH on Windows, the child JVM inherits the SSH session's Job Object and
            // is killed when the exec channel closes. Use a detached launch to escape it.
            // SSH_CLIENT / SSH_CONNECTION are set by sshd for every exec channel and are
            // absent for local subprocess launches (e.g. admin console start-instance),
            // making them a reliable discriminator over System.console() == null alone.
            if (OS.isWindows() && !info.isVerboseOrWatchdog() && isRunningUnderSsh()) {
                process = launchDetachedOnWindows(cmds, pb);
            } else {
                process = pb.start();
                writeSecurityTokens(process);
            }

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
            osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
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
            return STRINGS.get("server_process_died", ev, output);
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

        GFLauncherNativeHelper nativeHelper = new GFLauncherNativeHelper(info, javaConfig, jvmOptions);
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

    /**
     * Returns the Java Virtual Machine options (for testing)
     * @return 
     */
    List<String> getJvmOptions() {
        return jvmOptions.toStringArray();
    }

    /**
     * Adds a string to a list if it is not null
     * @param list
     * @param s 
     */
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
            final String msg = STRINGS.get("serverStopped", info.getType());
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
        all.put(SystemPropertyConstants.SERVER_NAME, getInfo().getInstanceName());
        all.putAll(sysPropsFromXml);
        all.putAll(jvmOptions.getCombinedMap());
        TokenResolver resolver = new TokenResolver(all);
        resolver.resolve(jvmOptions.xProps);
        resolver.resolve(jvmOptions.xxProps);
        resolver.resolve(jvmOptions.plainProps);
        resolver.resolve(jvmOptions.sysProps);
        resolver.resolve(javaConfig.getMap());
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
        List<File> extCP = Collections.singletonList(
                new File(info.getInstanceRootDir(), "lib/ext/*")
        );

        // create a list of all the classpath pieces in the right order
        List<File> all = new ArrayList<File>();
        all.addAll(extCP);
        all.addAll(prefixCP);
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

    private void setupJvmOptions(MiniXmlParser parser) throws MiniXmlParserException, GFLauncherException {
        // JVM options


        Optional<JDK.Version> jdkVersion = getConfiguredJdkVersion(javaExe);
        List<String> rawJvmOptions = parser.getJvmOptions()
                .stream()
                .filter(fullOption -> JDK.isCorrectJDK(jdkVersion, fullOption.vendorOrVM, fullOption.minVersion, fullOption.maxVersion))
                .map(option -> option.option)
                .collect(Collectors.toList());
        rawJvmOptions.addAll(getSpecialSystemProperties());
        jvmOptions = new JvmOptions(rawJvmOptions);
        if (info.isDropInterruptedCommands()) {
            jvmOptions.sysProps.put(SystemPropertyConstants.DROP_INTERRUPTED_COMMANDS, Boolean.TRUE.toString());
        }
        
        // PAYARA-1681 - Add default Payara JVM options if an override isn't in place
        addDefaultJvmOptions();
    }

    /**
     * Get the Java version from the given path to a Java executable.
     *
     * @param javaExePath The full path to the executable java command.
     * @return The Java version as a JDK.Version object, if successful.
     * @throws GFLauncherException
     */
    private Optional<JDK.Version> getConfiguredJdkVersion(String javaExePath) throws GFLauncherException {
        try {
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(javaExePath + " -version");
            p.waitFor();
            try (BufferedReader b = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = b.readLine()) != null) {
                    Matcher m = JAVA_VERSION_PATTERN.matcher(line);
                    if (m.matches()) {
                        return Optional.ofNullable(JDK.getVersion(m.group(1)));
                    }
                }
            }
            return Optional.empty();
        } catch (IOException | InterruptedException ex) {
            throw new GFLauncherException("nojvm");
        }
    }

    private void addDefaultJvmOptions() {
        if (!jvmOptions.getCombinedMap().containsKey(PayaraDefaultJvmOptions.GRIZZLY_DEFAULT_MEMORY_MANAGER_PROPERTY)) {
            jvmOptions.sysProps.put(PayaraDefaultJvmOptions.GRIZZLY_DEFAULT_MEMORY_MANAGER_PROPERTY, 
                    PayaraDefaultJvmOptions.GRIZZLY_DEFAULT_MEMORY_MANAGER_VALUE);
            
            // Log that we've made the change
            GFLauncherLogger.fine(GFLauncherLogger.DEFAULT_JVM_OPTION, 
                    PayaraDefaultJvmOptions.GRIZZLY_DEFAULT_MEMORY_MANAGER_PROPERTY,
                    PayaraDefaultJvmOptions.GRIZZLY_DEFAULT_MEMORY_MANAGER_VALUE);
        }
    }

    private void setupMonitoring(MiniXmlParser parser) throws GFLauncherException {
        // As usual we have to be very careful.

        // If it is NOT enabled -- we are out of here!!!
        if (!parser.isMonitoringEnabled()) {
            return;
        }

        // if the user has a hard-coded "-javaagent" jvm-option that uses OUR jar
        // then we do NOT want to add our own.
        Set<String> plainKeys = jvmOptions.plainProps.keySet();
        for (String key : plainKeys) {
            if (key.startsWith("javaagent:")) {
                // complications -- of course!!  They may have mix&match forward and back slashes
                key = key.replace('\\', '/');
                if (key.contains(FLASHLIGHT_AGENT_NAME))
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
            String msg = STRINGS.get("no_flashlight_agent", flashlightJarFile);
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

    private void checkJDKVersion() {
        if(!JDK.isRunningLTSJDK()) {
            GFLauncherLogger.warning("You are running the product on an unsupported JDK version and might see unexpected results or exceptions.");
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

    // Launches ASMain via Windows Task Scheduler so it survives SSH channel close.
    // Win32-OpenSSH kills every exec-channel process when the channel closes (Job Object with
    // KILL_ON_JOB_CLOSE); the Task Scheduler service creates the JVM outside that Job Object.
    // PowerShell ScheduledTask cmdlets (New-ScheduledTask*) use CIM/WMI internally; WMI
    // treats SSH network-logon (NTLM Type-3) sessions as remote, which non-admin users are
    // denied on ROOT\Microsoft\Windows\TaskScheduler. The COM Schedule.Service API also denies
    // RegisterTaskDefinition for non-admin SSH sessions. schtasks.exe uses a legacy RPC path
    // to the service that non-admin users can access. The task is interactive (/rl limited,
    // no /ru), so it runs in the user's logged-on session. wscript.exe (GUI subsystem) runs
    // cmd.exe hidden (SW_HIDE, bWaitOnReturn=False) so no console appears. The batch sets the
    // working directory, writes its cmd.exe PID via WMI before starting Java; a sentinel polls
    // that PID so waitForServer() detects failures without timing out.
    private Process launchDetachedOnWindows(List<String> cmds, ProcessBuilder pb) throws IOException, GFLauncherException {

        File tokenFile = File.createTempFile("payara-tokens-", ".tmp");
        // Temp dir can be shared; remove inherited ACEs so only the owner can read the tokens.
        restrictToOwner(tokenFile);
        try (BufferedWriter writer = Files.newBufferedWriter(tokenFile.toPath(), StandardCharsets.UTF_8)) {
            for (String token : info.securityTokens) {
                writer.write(token);
                writer.newLine();
            }
        }

        // @argfile avoids Windows 32 767-char CreateProcess limit
        File argFile = File.createTempFile("payara-args-", ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(argFile.toPath(), StandardCharsets.UTF_8)) {
            for (String arg : cmds.subList(1, cmds.size())) {
                if (arg.contains(" ") || arg.contains("\t")) {
                    writer.write("\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
                } else {
                    writer.write(arg);
                }
                writer.newLine();
            }
        }

        // Batch: sets working directory, writes cmd.exe PID via WMI before starting Java,
        // and redirects stdin from tokenFile with '<'. cd /d is needed because schtasks.exe
        // does not expose a working-directory field for the task action.
        File pidFile = File.createTempFile("payara-pid-", ".tmp");
        File batchFile = File.createTempFile("payara-start-", ".bat");
        try (BufferedWriter writer = Files.newBufferedWriter(batchFile.toPath(), StandardCharsets.UTF_8)) {
            String pidFilePs = pidFile.getAbsolutePath().replace("'", "''");
            String javaQ = cmds.getFirst().replace("\"", "\"\"");
            String argQ = argFile.getAbsolutePath().replace("\"", "\"\"");
            String tokQ = tokenFile.getAbsolutePath().replace("\"", "\"\"");
            String workDirQ = (pb.directory() != null ? pb.directory() : new File(".")).getAbsolutePath().replace("\"", "\"\"");
            writer.write("@echo off\r\n");
            writer.write("cd /d \"" + workDirQ + "\"\r\n");
            writer.write("powershell -NoProfile -NonInteractive -Command "
                    + "\"(Get-WmiObject Win32_Process -Filter ('ProcessId='+$PID)).ParentProcessId"
                    + " | Out-File -FilePath '" + pidFilePs + "' -Encoding ascii -NoNewline\"\r\n");
            writer.write("\"" + javaQ + "\" \"@" + argQ + "\" < \"" + tokQ + "\"\r\n");
        }

        // wscript.exe (GUI subsystem, no console) runs cmd.exe hidden via SW_HIDE; bWaitOnReturn=False
        // so wscript exits immediately and cmd.exe runs independently.
        File vbsFile = File.createTempFile("payara-launch-", ".vbs");
        try (BufferedWriter writer = Files.newBufferedWriter(vbsFile.toPath(), StandardCharsets.UTF_8)) {
            writer.write("CreateObject(\"WScript.Shell\").Run \"cmd.exe /c \"\"\" & WScript.Arguments(0) & \"\"\"\", 0, False\r\n");
        }

        // schtasks.exe works for non-admin SSH users where both the PowerShell CIM-based
        // cmdlets and the COM Schedule.Service RegisterTaskDefinition API are denied.
        // The task XML explicitly sets DisallowStartIfOnBatteries=false and
        // StopIfGoingOnBatteries=false; schtasks command-line flags do not expose these
        // settings, which default to true and silently block execution on battery power.
        String taskName = "PayaraStart-" + UUID.randomUUID().toString().replace("-", "");

        File xmlFile = File.createTempFile("payara-task-", ".xml");
        writeTaskXml(xmlFile, vbsFile.getAbsolutePath(), batchFile.getAbsolutePath());

        File stderrFile = File.createTempFile("payara-schtasks-stderr-", ".log");
        List<File> toClean = new ArrayList<>(Arrays.asList(tokenFile, argFile, batchFile, vbsFile, pidFile, xmlFile, stderrFile));

        ProcessBuilder createPb = new ProcessBuilder(
                "schtasks", "/create",
                "/xml", xmlFile.getAbsolutePath(),
                "/tn", taskName,
                "/f");
        createPb.redirectInput(new File("NUL"));
        createPb.redirectOutput(new File("NUL"));
        createPb.redirectError(stderrFile);

        Process createPs = createPb.start();
        try {
            boolean finished = createPs.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                createPs.destroyForcibly();
                String err = readSilently(stderrFile);
                toClean.forEach(File::delete);
                throw new GFLauncherException("schtasks /create timed out after 30 s"
                        + (err.isEmpty() ? "" : ": " + err));
            }
            if (createPs.exitValue() != 0) {
                String err = readSilently(stderrFile);
                toClean.forEach(File::delete);
                throw new GFLauncherException("schtasks /create failed (exit=" + createPs.exitValue() + ")"
                        + (err.isEmpty() ? "" : ": " + err));
            }
        } catch (InterruptedException ie) {
            createPs.destroyForcibly();
            toClean.forEach(File::delete);
            Thread.currentThread().interrupt();
            throw new GFLauncherException("schtasks /create interrupted");
        }

        File runStderrFile = File.createTempFile("payara-schtasks-run-stderr-", ".log");
        toClean.add(runStderrFile);
        Process runPs;
        try {
            runPs = new ProcessBuilder("schtasks", "/run", "/tn", taskName)
                    .redirectInput(new File("NUL"))
                    .redirectOutput(new File("NUL"))
                    .redirectError(runStderrFile)
                    .start();
        } catch (IOException ioe) {
            deleteTaskSilently(taskName);
            toClean.forEach(File::delete);
            throw ioe;
        }
        try {
            boolean runFinished = runPs.waitFor(30, TimeUnit.SECONDS);
            if (!runFinished) {
                runPs.destroyForcibly();
                String err = readSilently(runStderrFile);
                deleteTaskSilently(taskName);
                toClean.forEach(File::delete);
                throw new GFLauncherException("schtasks /run timed out after 30 s"
                        + (err.isEmpty() ? "" : ": " + err));
            }
            if (runPs.exitValue() != 0) {
                String err = readSilently(runStderrFile);
                deleteTaskSilently(taskName);
                toClean.forEach(File::delete);
                throw new GFLauncherException("schtasks /run failed (exit=" + runPs.exitValue() + ")"
                        + (err.isEmpty() ? "" : ": " + err));
            }
        } catch (InterruptedException ie) {
            runPs.destroyForcibly();
            deleteTaskSilently(taskName);
            toClean.forEach(File::delete);
            Thread.currentThread().interrupt();
            throw new GFLauncherException("schtasks /run interrupted");
        }

        int cmdPid = -1;
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                String raw = new String(Files.readAllBytes(pidFile.toPath()), StandardCharsets.UTF_8).trim();
                if (!raw.isEmpty()) {
                    cmdPid = Integer.parseInt(raw);
                    break;
                }
            } catch (IOException | NumberFormatException ignored) {
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                toClean.forEach(File::delete);
                throw new GFLauncherException("Interrupted waiting for Payara process PID");
            }
        }

        // Unregister the task; the already-running process is unaffected.
        new ProcessBuilder("schtasks", "/delete", "/tn", taskName, "/f")
                .redirectInput(new File("NUL"))
                .redirectOutput(new File("NUL"))
                .redirectError(new File("NUL"))
                .start();

        if (cmdPid < 0) {
            toClean.forEach(File::delete);
            throw new GFLauncherException("Timed out waiting for Payara process PID file");
        }

        GFLauncherLogger.fine("launchDetachedOnWindows", "Task launched via schtasks. pid=" + cmdPid + " batchFile=" + batchFile.getAbsolutePath());

        Thread cleanup = new Thread(() -> {
            try {
                Thread.sleep(120_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            toClean.forEach(File::delete);
        }, "payara-launch-cleanup");
        cleanup.setDaemon(true);
        cleanup.start();

        // Sentinel polls cmd.exe (blocks until java.exe exits); exits with code 1 when GF dies.
        Process sentinel = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command",
                "$p=" + cmdPid + ";"
                + "while(Get-Process -Id $p -ErrorAction SilentlyContinue){Start-Sleep -Seconds 1};"
                + "exit 1")
                .redirectInput(new File("NUL"))
                .redirectOutput(new File("NUL"))
                .redirectError(new File("NUL"))
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(sentinel::destroyForcibly, "payara-sentinel-killer"));
        return sentinel;
    }

    // Replaces the file's ACL with a single owner-only ALLOW entry so that other
    // local accounts cannot read the file even if java.io.tmpdir is a shared directory.
    // Best-effort: if the platform or file system does not support ACLs, we proceed
    // with whatever permissions the OS assigned at creation time.
    private static boolean isRunningUnderSsh() {
        return System.getenv("SSH_CLIENT") != null || System.getenv("SSH_CONNECTION") != null;
    }

    private static String readSilently(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void deleteTaskSilently(String taskName) {
        try {
            new ProcessBuilder("schtasks", "/delete", "/tn", taskName, "/f")
                    .redirectInput(new File("NUL"))
                    .redirectOutput(new File("NUL"))
                    .redirectError(new File("NUL"))
                    .start();
        } catch (IOException ignored) {
        }
    }

    private static void writeTaskXml(File xmlFile, String vbsPath, String batchPath) throws IOException {
        String vbs = escapeXml(vbsPath);
        String bat = escapeXml(batchPath);
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\r\n"
                + "<Task version=\"1.2\" xmlns=\"http://schemas.microsoft.com/windows/2004/02/mit/task\">\r\n"
                + "  <RegistrationInfo/>\r\n"
                + "  <Triggers><TimeTrigger><StartBoundary>2000-01-01T00:00:00</StartBoundary>"
                + "<Enabled>true</Enabled></TimeTrigger></Triggers>\r\n"
                + "  <Principals><Principal id=\"Author\">"
                + "<LogonType>InteractiveToken</LogonType>"
                + "<RunLevel>LeastPrivilege</RunLevel>"
                + "</Principal></Principals>\r\n"
                + "  <Settings>\r\n"
                + "    <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>\r\n"
                + "    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>\r\n"
                + "    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>\r\n"
                + "    <AllowHardTerminate>true</AllowHardTerminate>\r\n"
                + "    <StartWhenAvailable>false</StartWhenAvailable>\r\n"
                + "    <RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>\r\n"
                + "    <IdleSettings/>\r\n"
                + "    <AllowStartOnDemand>true</AllowStartOnDemand>\r\n"
                + "    <Enabled>true</Enabled>\r\n"
                + "    <Hidden>false</Hidden>\r\n"
                + "    <RunOnlyIfIdle>false</RunOnlyIfIdle>\r\n"
                + "    <WakeToRun>false</WakeToRun>\r\n"
                + "    <ExecutionTimeLimit>PT0S</ExecutionTimeLimit>\r\n"
                + "    <Priority>7</Priority>\r\n"
                + "  </Settings>\r\n"
                + "  <Actions Context=\"Author\"><Exec>"
                + "<Command>wscript.exe</Command>"
                + "<Arguments>//nologo &quot;" + vbs + "&quot; &quot;" + bat + "&quot;</Arguments>"
                + "</Exec></Actions>\r\n"
                + "</Task>\r\n";
        byte[] bom = {(byte) 0xFF, (byte) 0xFE};
        byte[] content = xml.getBytes(StandardCharsets.UTF_16LE);
        byte[] withBom = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(content, 0, withBom, bom.length, content.length);
        Files.write(xmlFile.toPath(), withBom);
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static void restrictToOwner(File file) {
        try {
            AclFileAttributeView aclView = Files.getFileAttributeView(file.toPath(), AclFileAttributeView.class);
            if (aclView == null) {
                return;
            }
            UserPrincipal owner = Files.getOwner(file.toPath());
            AclEntry ownerOnly = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner).setPermissions(AclEntryPermission.values()).build();
            aclView.setAcl(List.of(ownerOnly));
        } catch (IOException ignored) {
        }
    }

    private void setupLogLevels() {
        if (info.isVerbose())
            GFLauncherLogger.setConsoleLevel(Level.INFO);
        else
            GFLauncherLogger.setConsoleLevel(Level.WARNING);
    }

    @SuppressWarnings("UseSpecificCatch")
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

            if (info.isDomain()) {
                sname = info.getDomainName();
            } else {
                sname = info.getInstanceName();
            }
            
            System.out.println(STRINGS.get("ssh", sname));
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
    
    ///////////////////////////////////////////////////////////////////////////
    private static class ProcessWhacker implements Runnable {
        
        private final String message;
        private Process process;
        
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
    }
}
