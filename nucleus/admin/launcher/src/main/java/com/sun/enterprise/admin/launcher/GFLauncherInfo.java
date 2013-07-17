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

package com.sun.enterprise.admin.launcher;

import java.io.*;
import java.util.*;
import com.sun.enterprise.universal.glassfish.GFLauncherUtils;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.HostAndPort;
import org.glassfish.api.admin.RuntimeType;

/**
 * @author Byron Nevins
 */
public class GFLauncherInfo {

    /**
     * Add the string arguments in the order given.
     * @param args The string arguments
     */
    public void addArgs(String... args) {
        for (String s : args) {
            argsRaw.add(s);
        }
    }

    /**
     * Set the (optional) domain name.  This can also be sent in as a String arg
     * like so: "-domainname" "theName"
     * @param domainName
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * Set the (optional) domain parent directory.  
     * This can also be sent in as a String arg
     * like so: "-domaindir" "parentDirPath"
     * @param domainParentName The parent directory of the domain
     */
    public void setDomainParentDir(String domainParentName) {
        this.domainParentDir = new File(domainParentName);
    }

    /**
     * Starts the server in verbose mode
     * @param b 
     */
    public void setVerbose(boolean b) {
        verbose = b;
    }

    /**
     * Starts the server in watchdog mode.  This is only useful if verbose is false.
     * It does the same thing as verbose -- except without the dumping of output
     * to standard out and err streams.
     * @param b
     * @since 3.2
     */
    public void setWatchdog(boolean b) {
        watchdog = b;
    }

    /**
     * Starts the server in debug mode
     * @param b 
     */
    public void setDebug(boolean b) {
        debug = b;
    }

     /**
     * Starts the server in upgrade mode
     * @param b
     */
    public void setUpgrade(boolean b) {
        upgrade = b;
    }

    public void setDomainRootDir(File f) {
        domainRootDir = f;
    }

    public void setInstanceName(String name) {
        instanceName = name;
    }

    public void setInstanceRootDir(File f) {
        instanceRootDir = f;
    }

    public void setDropInterruptedCommands(boolean dropInterruptedCommands) {
        this.dropInterruptedCommands = dropInterruptedCommands;
    }

    public final boolean isDomain() {
        return type == RuntimeType.DAS;
    }

    public final boolean isInstance() {
        return type == RuntimeType.INSTANCE;
    }

    /**
     *
     * @return true if verbose mode is on.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     *
     * @return true if watchdog mode is on.
     */
    public boolean isWatchdog() {
        return watchdog;
    }

    /**
     * 
     * @return true if debug mode is on.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     *
     * @return true if upgrade mode is on.
     */
    public boolean isUpgrade() {
        return upgrade;
    }

    /**
     * 
     * @return The domain name
     */
    public String getDomainName() {
        return domainName;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getDomainRootDir() {
        return domainRootDir;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public List<HostAndPort> getAdminAddresses() {
        return adminAddresses;
    }
    public RuntimeType getType() {
        return type;
    }

    public File getConfigDir() {
        return SmartFile.sanitize(configDir);
    }

    void setConfigDir(File f) {
        configDir = SmartFile.sanitize(f);
    }
    
    public File getInstanceRootDir() throws GFLauncherException {
        if (!valid) {
            throw new GFLauncherException("internalError", "Call to getInstanceRootDir() on an invalid GFLauncherInfo object.");
        }
        if(instanceRootDir != null) {
            return instanceRootDir;
        }
        else if(isDomain()) {
            return domainRootDir;
        }
        else {
            throw new GFLauncherException("internalError", "Call to getInstanceRootDir() on an invalid GFLauncherInfo object.");
        }
    }

    File getDomainParentDir() {
        return domainParentDir;
    }

    public boolean isDropInterruptedCommands() {
        return dropInterruptedCommands;
    }

    /**
     *  TEMPORARY.  The guts of HK2 and V3 bootstrapping wants String[]
     * -- this will be changed soon, but it is messy to change it right now.
     * so temporarily we will humor HK2 by sending in String[]
     * @return an array of String arguments
     * @throws com.sun.enterprise.admin.launcher.GFLauncherException 
     */
    public String[] getArgsAsStringArray() throws GFLauncherException {
        List<String> list = getArgsAsList();
        String[] ss = new String[list.size()];
        return list.toArray(ss);
    }

    public List<String> getArgsAsList() throws GFLauncherException {
        Map<String, String> map = getArgs();
        Set<String> keys = map.keySet();
        List<String> argList = new ArrayList<String>();
        
        int i = 0;

        for (String key : keys) {
            argList.add(key);
            argList.add(map.get(key));
        }
        return argList;
    }

    /**
     * 
     * @return a Map<String,String> of processed and packaged args
     * @throws com.sun.enterprise.admin.launcher.GFLauncherException 
     */
    public Map<String, String> getArgs() throws GFLauncherException {
        // args processed and packaged for AppServer

        if (!valid) {
            throw new GFLauncherException("internalError", "Call to getArgs() on an invalid GFLauncherInfo object.");
        }

        Map<String, String> map = new HashMap<String, String>();

        map.put("-type", type.toString());
        
        if(isDomain()) {
            map.put("-domaindir", SmartFile.sanitize(domainRootDir.getPath()));
            map.put("-domainname", domainName);
        }
        else if(isInstance()) {
            map.put("-instancedir", SmartFile.sanitize(instanceRootDir.getPath()));
        }

        // no need for watchdog here.  It is a client-side phenomenon only!
        map.put("-verbose", Boolean.toString(verbose));
        map.put("-debug", Boolean.toString(debug));
        map.put("-instancename", instanceName);
        map.put("-upgrade", Boolean.toString(upgrade));
        map.put("-read-stdin", "true"); //always make the server read the stdin for master password, at least.

        if(respawnInfo != null) {
            respawnInfo.put(map);
        }
        return map;
    }

    public void setRespawnInfo(String classname, String classpath, String[] args) {
        respawnInfo = new RespawnInfo(classname, classpath, args);
    }

    /** Adds the given name value pair as a security token. This is what will be put on the
     *  launched process's stdin to securely pass it on. The value is accepted as a String and it may be insecure.
     *  A string formed by concatenating name, '=' and value is written to the stdin as a single
     *  line delimited by newline character. To get
     *  the value of the token, the server should parse the line knowing this. None of the parameters may be null.
     *
     * @param name  String representing name of the token
     * @param value String representing the value (should we call it a password?)
     * @throws NullPointerException if any of the parameters are null
     */
    public void addSecurityToken(String name, String value) {
        if (name == null || value == null)
            throw new NullPointerException();
        securityTokens.add(name + "=" + value);
    }
    
    GFLauncherInfo(RuntimeType type) {
        this.type = type;
    }


    void setAdminAddresses(List<HostAndPort> adminAddresses) {
        this.adminAddresses = adminAddresses;
    }
    void setup() throws GFLauncherException {
        setupFromArgs();
        finalSetup();
    }

    /**
     * IMPORTANT:  These 2 methods are designed for use only by Unit Tests so we are
     * not dependent on an installation.  Normally we figure out installDir from
     * wher we are running from. 
     */
    void setInstallDir(File f) {
        installDir = f;
    }

    File getInstallDir() {
        return installDir;
    }
            
    private void setupFromArgs() {
        argsMap = ArgumentManager.argsToMap(argsRaw);

        File f = null;
        String s = null;
        ThreeStateBoolean tsb = null;

        // pick out file props
        // annoying -- cli uses "domaindir" to represent the parent of the 
        // domain root dir.  I'm sticking with the same syntax for now...
        if ((f = getFile("domaindir")) != null) {
            domainParentDir = f;
        }

        if ((f = getFile("instanceRootDir")) != null) {
            instanceRootDir = f;
        }

        if ((f = getFile("domainroot")) != null) {
            domainRootDir = f;
        }

        // Now do the same thing with known Strings
        if ((s = getString("domain")) != null) {
            domainName = s;
        }

        // the Arg processor may have set the name "default" to the domain name
        // just like in asadmin
        if (!GFLauncherUtils.ok(domainName) && (s = getString("default")) != null) {
            domainName = s;
        }

        if ((s = getString("instancename")) != null) {
            instanceName = s;
        }

        // finally, do the booleans
        // getting ugly.  Findbugs does not like using regular Boolean object
        // a three-state boolean
        // we do NOT want to disturb the existing values of these variables if the
        // user has not explicitly overridden them.

        tsb = getBoolean("debug");

        if(tsb.isTrue())
            debug = true;
        else if(tsb.isFalse())
            debug = false;

        tsb = getBoolean("verbose");
        if(tsb.isTrue())
            verbose = true;
        else if(tsb.isFalse())
            verbose = false;

        tsb = getBoolean("watchdog");
        if(tsb.isTrue())
            watchdog = true;
        else if(tsb.isFalse())
            watchdog = false;

        tsb = getBoolean("upgrade");
        if(tsb.isTrue())
            upgrade = true;
        else if(tsb.isFalse())
            upgrade = false;
    }

    private void finalSetup() throws GFLauncherException {
        if(installDir == null)
            installDir = GFLauncherUtils.getInstallDir();

        if (!GFLauncherUtils.safeIsDirectory(installDir)) {
            throw new GFLauncherException("noInstallDir", installDir);
        }

        // check user-supplied args
        if (domainParentDir != null) {
            // if the arg was given -- then it MUST point to a real dir
            if (!GFLauncherUtils.safeIsDirectory(domainParentDir)) {
                throw new GFLauncherException("noDomainParentDir", domainParentDir);
            }
        }

        setupServerDirs();

        if (!GFLauncherUtils.safeIsDirectory(configDir)) {
            throw new GFLauncherException("noConfigDir", configDir);
        }

        configFile = new File(configDir, CONFIG_FILENAME);

        if (!GFLauncherUtils.safeExists(configFile)) {
            throw new GFLauncherException("noConfigFile", configFile);
        }

        if (instanceName == null) {
            instanceName = "server";
        }

        // if we made it here -- we're in pretty good shape!
        valid = true;
    }

    private void setupServerDirs() throws GFLauncherException {
        if(isDomain())
            setupDomainDirs();
        else if(isInstance())
            setupInstanceDirs();
    }

    private void setupDomainDirs() throws GFLauncherException {
        // if they set domainrootdir -- it takes precedence
        if (domainRootDir != null) {
            domainParentDir = domainRootDir.getParentFile();
            domainName = domainRootDir.getName();
            return;
        }

        // if they set domainParentDir -- use it.  o/w use the default dir
        if (domainParentDir == null) {
            domainParentDir = new File(installDir, DEFAULT_DOMAIN_PARENT_DIR);
        }

        // if they specified domain name -- use it.  o/w use the one and only dir
        // in the domain parent dir

        if (domainName == null) {
            domainName = getTheOneAndOnlyDomain();
        }

        domainRootDir = new File(domainParentDir, domainName);

        if (!GFLauncherUtils.safeIsDirectory(domainRootDir)) {
            throw new GFLauncherException("noDomainRootDir", domainRootDir);
        }

        configDir = new File(domainRootDir, CONFIG_DIR);
    }
    private void setupInstanceDirs() throws GFLauncherException {
        if (instanceRootDir == null) {
            throw new GFLauncherException("Missing instanceRootDir");
        }
        if (instanceName == null) {
            throw new GFLauncherException("Missing instanceName");
        }
        configDir = new File(instanceRootDir, CONFIG_DIR);
    }

    private String getTheOneAndOnlyDomain() throws GFLauncherException {
        // look for subdirs in the parent dir -- there must be one and only one

        File[] files = domainParentDir.listFiles(new FileFilter() {

            public boolean accept(File f) {
                return GFLauncherUtils.safeIsDirectory(f);
            }
        });

        if (files == null || files.length == 0) {
            throw new GFLauncherException("noDomainDirs", domainParentDir);
        }

        if (files.length > 1) {
            throw new GFLauncherException("tooManyDomainDirs", domainParentDir);
        }

        return files[0].getName();
    }

    private ThreeStateBoolean getBoolean(String key) {
        // 3 return values -- true, false, null
        String s = getValueIgnoreCommandDelimiter(key);

        if (s != null) // guaranteed true or false
            return new ThreeStateBoolean(Boolean.valueOf(s));
        else
            return new ThreeStateBoolean(null);
    }

    private File getFile(String key) {
        String s = getString(key);

        if (s == null)
            return null;
        else
            return new File(s);
    }

    private String getString(String key) {
        return getValueIgnoreCommandDelimiter(key);
    }

    private String getValueIgnoreCommandDelimiter(String key) {
        // it can be confusing trying to remember -- is it "--option"?
        // or "-option" or "option".  So look for any such match.

        if (argsMap.containsKey(key)) {
            return argsMap.get(key);
        }
        key = "-" + key;
        if (argsMap.containsKey(key)) {
            return argsMap.get(key);
        }
        key = "-" + key;
        if (argsMap.containsKey(key)) {
            return argsMap.get(key);
        }
        return null;
    }

    private RuntimeType type;
    private boolean verbose = false;
    private boolean watchdog = false;
    private boolean debug = false;
    private boolean upgrade = false;
    File installDir;
    private File domainParentDir;
    private File domainRootDir;
    private File instanceRootDir;
    //private File nodeAgentDir;
    //private File nodeAgentsDir;
    private File configDir;
    private File configFile; // domain.xml
    private String domainName;
    private String instanceName;
    private boolean dropInterruptedCommands = false;
    private boolean valid = false;
    private Map<String, String> argsMap;
    private ArrayList<String> argsRaw = new ArrayList<String>();
    private List<HostAndPort> adminAddresses;
    private RespawnInfo respawnInfo;
    // BUG TODO get the def. domains dir from asenv 3/14/2008
    private final static String DEFAULT_DOMAIN_PARENT_DIR = "domains";
    private final static String CONFIG_DIR = "config";
    private final static String CONFIG_FILENAME = "domain.xml";
    //password tokens -- could be multiple -- launcher should *just* write them onto stdin of server
    final List<String> securityTokens = new ArrayList<String>(); // note: it's package private

    boolean isVerboseOrWatchdog() {
        return verbose || watchdog;
    }

    final private static class ThreeStateBoolean {

        ThreeStateBoolean(Boolean b) {
            this.b = b;
        }
        boolean isNull() {
            return b == null;
        }
        boolean isTrue() {
            return !isNull() && b.booleanValue();
        }
        boolean isFalse() {
            return !isNull() && !b.booleanValue();
        }
        
        Boolean b;
    }

}
