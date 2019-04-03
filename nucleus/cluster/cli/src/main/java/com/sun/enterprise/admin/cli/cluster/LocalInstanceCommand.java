/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portios Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.io.InstanceDirs;
import java.io.*;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;

import jline.console.ConsoleReader;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;

import com.sun.enterprise.admin.servermgmt.cli.LocalServerCommand;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.io.ServerDirs;

/**
 * A base class for local commands that manage a local server instance.
 * This base class is used by a LOT of other classes.  There is a big comment before
 * each section of the file -- sorted by visibility.
 * If you specifically want a method to be overridden -- make it protected but
 * not final.
 * final protected means -- "call me but don't override me".  This convention is
 * to make things less confusing.
 * If you add a method or change whether it is final -- move it to the right section.
 *
 * Default instance file structure.
 * <pre>
 * ||---- &lt;GlassFish Install Root&gt;
 *          ||---- nodes (nodeDirRoot, --nodedir)
 *                  ||---- &lt;node-1&gt; (nodeDirChild, --node)
 *                          || ---- agent
 *                                  || ---- config
 *                                          | ---- das.properties
 *                          || ---- &lt;instance-1&gt; (instanceDir)
 *                                  ||---- config
 *                                  ||---- applications
 *                                  ||---- java-web-start
 *                                  ||---- generated
 *                                  ||---- lib
 *                                  ||---- docroot
 *                          || ---- &lt;instance-2&gt; (instanceDir)
 *                  ||---- &lt;node-2&gt; (nodeDirChild)
 * </pre>
 * @author Byron Nevins
 */
// -----------------------------------------------------------------------
// ----------------   public methods here   --------------- --------------
// -----------------------------------------------------------------------
public abstract class LocalInstanceCommand extends LocalServerCommand {
    @Param(name = "nodedir", optional = true, alias = "agentdir")
    protected String nodeDir;           // nodeDirRoot
    @Param(name = "node", optional = true, alias = "nodeagent")
    protected String node;
    // subclasses decide whether it's optional, required, or not allowed
    //@Param(name = "instance_name", primary = true, optional = true)
    protected String instanceName;
    protected File nodeDirRoot;         // the parent dir of all node(s)
    protected File nodeDirChild;        // the specific node dir
    protected File instanceDir;         // the specific instance dir
    protected String domainName;
    protected boolean isCreateInstanceFilesystem = false;
    private InstanceDirs instanceDirs;

    // This is especially used for change-master-password command for a node.
    // We iterate through all the instances and so it should relax this requirement,
    // that there is only 1 instance in a node.
    protected boolean checkOneAndOnly = true;

    @Override
    protected void validate() throws CommandException, CommandValidationException {
        initInstance();
    }

// -----------------------------------------------------------------------
// -------- protected methods where overriding is allowed here -----------
// -----------------------------------------------------------------------
    /**
     * Override this method if your class does NOT want to create directories
     * @param f the directory to create
     * @return true iff the directory was created
     */
    protected boolean mkdirs(File f) {
        return f.mkdirs();
    }

    /**
     * Override this method if your class does NOT want CommandException thrown
     * if directory does not exist.
     * @param f directory to check
     */
    protected boolean isDirectory(File f) {
        return f.isDirectory();
    }

    /**
     * Override this method if your class does NOT want to set ServerDirs
     */
    protected boolean setServerDirs() {
        return true;
    }

    protected void initInstance() throws CommandException {
        /* node dir - parent directory of all node(s) */
        String nodeDirRootPath = null;
        if (ok(nodeDir))
            nodeDirRootPath = nodeDir;
        else // node dir = <install-root>/nodes
            nodeDirRootPath = getNodeDirRootDefault();

        nodeDirRoot = new File(nodeDirRootPath);
        mkdirs(nodeDirRoot);

        if (ok(nodeDir)) {
            // Ensure later uses of nodeDir get an absolute path
            // See bug 15014. Don't use getCanonicalPath(). See bug
            // 15889
            nodeDir = nodeDirRoot.getAbsolutePath();
        }

        if (!isDirectory(nodeDirRoot)) {
            throw new CommandException(
                    Strings.get("Instance.badNodeDir", nodeDirRoot));
        }

        /* <node_dir>/<node> */
        if (node != null) {
            nodeDirChild = new File(nodeDirRoot, node);
        } else {
            nodeDirChild = getTheOneAndOnlyNode(nodeDirRoot);
        }

        /* <node_dir>/<node>/<instance name> */
        if (getInstanceName() != null) {
            instanceDir = new File(nodeDirChild, getInstanceName());
            mkdirs(instanceDir);
        } else {
            instanceDir = getTheOneAndOnlyInstance(nodeDirChild);
            setInstanceName(instanceDir.getName());
        }

        if (!isDirectory(instanceDir)) {
            throw new CommandException(
                    Strings.get("Instance.badInstanceDir", instanceDir));
        }
        nodeDirChild = SmartFile.sanitize(nodeDirChild);
        instanceDir = SmartFile.sanitize(instanceDir);

        try {
            if (setServerDirs()) {
                instanceDirs = new InstanceDirs(instanceDir);
                setServerDirs(instanceDirs.getServerDirs());
            }
        } catch (IOException e) {
            throw new CommandException(e);
        }

        logger.log(Level.FINER, "nodeDirChild: {0}", nodeDirChild);
        logger.log(Level.FINER, "instanceDir: {0}", instanceDir);
    }

    protected final InstanceDirs getInstanceDirs() {
        return instanceDirs;
    }

// -----------------------------------------------------------------------
// -------- protected methods where overriding is NOT allowed here -----------
// -----------------------------------------------------------------------
    /**
     * Set the programOpts based on the das.properties file.
     */
    protected final void setDasDefaults(File propfile) throws CommandException {
        Properties dasprops = getDasProperties(propfile);

        // read properties and set them in programOpts
        // properties are:
        // agent.das.port
        // agent.das.host
        // agent.das.isSecure
        // agent.das.user           XXX - not in v2?
        String p;
        p = dasprops.getProperty("agent.das.host");
        if (p != null) {
            programOpts.setHost(p);
        }
        p = dasprops.getProperty("agent.das.port");
        int port = -1;
        if (p != null) {
            port = Integer.parseInt(p);
        }
        p = dasprops.getProperty("agent.das.protocol");
        if (p != null && p.equals("rmi_jrmp")) {
            programOpts.setPort(updateDasPort(dasprops, port, propfile));
        } else if (p == null || p.equals("http")) {
            programOpts.setPort(port);
        } else {
            throw new CommandException(Strings.get("Instance.badProtocol",
                    propfile.toString(), p));
        }
        p = dasprops.getProperty("agent.das.isSecure");
        if (p != null) {
            programOpts.setSecure(Boolean.parseBoolean(p));
        }
        p = dasprops.getProperty("agent.das.user");
        if (p != null) {
            programOpts.setUser(p);
        }
        // XXX - what about the DAS admin password?
    }

    /**
     * Checks if programOpts values match das.properties file.
     */
    protected final void validateDasOptions(String hostOption, String portOption,
            String isSecureOption, File propfile) throws CommandException {
        if (propfile != null) {
            Properties dasprops = getDasProperties(propfile);
            if (!dasprops.isEmpty()) {
                String errorMsg = "";
                String nodeName = nodeDirChild != null ? nodeDirChild.getName() : "";
                String hostProp = dasprops.getProperty("agent.das.host");
                String portProp = dasprops.getProperty("agent.das.port");
                String secureProp = dasprops.getProperty("agent.das.isSecure");
                if (!matchingHostnames(hostProp, hostOption)) {
                    errorMsg = errorMsg + Strings.get("Instance.DasHostInvalid", hostOption, nodeName) + "\n";
                }
                if (portProp != null && !portProp.equals(portOption)) {
                    errorMsg = errorMsg + Strings.get("Instance.DasPortInvalid", portOption, nodeName) + "\n";
                }
                if (secureProp != null && !secureProp.equals(isSecureOption)) {
                    errorMsg = errorMsg + Strings.get("Instance.DasIsSecureInvalid", isSecureOption, nodeName) + "\n";
                }
                if (!errorMsg.isEmpty()) {
                    errorMsg = errorMsg + Strings.get("Instance.DasConfig", nodeName, hostProp, portProp, secureProp);
                    throw new CommandException(errorMsg);
                }
            }
        }
    }

   /**
     * Check if two hostnames refer to the same host. We start with a cheap
     * string comparison. If that fails we see if the hostnames refer to the
     * same host.
     *
     * @param host1
     * @param host2
     * @return
     */
    private boolean matchingHostnames(String host1, String host2) {

        if (!StringUtils.ok(host1) || !StringUtils.ok(host2)) {
            if (!StringUtils.ok(host1) && !StringUtils.ok(host2)) {
                // Both empty/null strings. Consider it a match.
                return true;
            } else {
                // Only one string null/empty. No match.
                return false;
            }
        }

        if (host1.equalsIgnoreCase(host2)) {
            // Hostnames exactly match. Same host.
            return true;
        }

        if (NetUtils.isEqual(host1, host2)) {
            // Hostnames don't exactly match, but refer to same IP. Match.
            return true;
        }

        if (NetUtils.isThisHostLocal(host1) && NetUtils.isThisHostLocal(host2)) {
            // Hostnames both refer to the local host. Match.
            return true;
        }

        // Don't match.
        return false;
    }

    protected final Properties getDasProperties(File propfile) throws CommandException {
        Properties dasprops = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propfile);
            dasprops.load(fis);
            fis.close();
            fis = null;
        } catch (IOException ioex) {
            throw new CommandException(Strings.get("Instance.cantReadDasProperties", propfile.getPath()));
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException cex) {
                    // ignore it
                }
            }
        }
        return dasprops;
    }

    protected final void whackFilesystem() throws CommandException {
        ServerDirs dirs = getServerDirs();
        File whackee = dirs.getServerDir();
        File parent = dirs.getServerParentDir();
        File grandParent = dirs.getServerGrandParentDir();

        if (whackee == null || !whackee.isDirectory()) {
            throw new CommandException(Strings.get("DeleteInstance.noWhack", whackee));
        }

        // IT 12680 -- perhaps a new empty dir was created.  Get rid of it and
        // make this an error.
        File[] files = whackee.listFiles();

        if (files == null || files.length <= 0) {
            // empty dir
            if (files != null) {
                FileUtils.whack(whackee);
            }

            throw new CommandException(Strings.get("DeleteInstance.noWhack", whackee));
        }

        // Rename the instance directory to a temporary name to ensure that
        // the directory tree can be deleted on Windows.
        // The FileUtils.renameFile method has a retry built in.
        try {
            File tmpwhackee = File.createTempFile("oldinst", null, parent);
            if (!tmpwhackee.delete()) {
                throw new IOException(Strings.get("cantdelete", tmpwhackee));
            }
            FileUtils.renameFile(whackee, tmpwhackee);
            FileUtils.whack(tmpwhackee);
        } catch (IOException ioe) {
            throw new CommandException(Strings.get("DeleteInstance.badWhackWithException", whackee, ioe, StringUtils.getStackTrace(ioe)));
        }
        if (whackee.isDirectory()) {
            StringBuilder sb = new StringBuilder();
            sb.append("whackee=").append(whackee.toString());
            sb.append(", files in parent:");
            files = parent.listFiles();
            for (File f : files) {
                sb.append(f.toString()).append(", ");
            }
            File f1 = new File(whackee.toString());
            sb.append(", new wackee.exists=").append(f1.exists());
            throw new CommandException(Strings.get("DeleteInstance.badWhack", whackee) + ", " + sb.toString());
        }

        // now see if the parent dir is empty.  If so wipe it out.
        // Don't be too picky with throwin errors here...
        try {
            files = parent.listFiles();

            if (noInstancesRemain(files)) {
                File tmpwhackee = File.createTempFile("oldnode", null, grandParent);
                if (!tmpwhackee.delete()) {
                    throw new IOException(Strings.get("cantdelete", tmpwhackee));
                }
                FileUtils.renameFile(parent, tmpwhackee);
                FileUtils.whack(tmpwhackee);
            }
        } catch (IOException ioe) {
            // we tried!!!
        }
    }

    private boolean noInstancesRemain(File[] files) {
        if (files == null || files.length <= 0) {
            return true;
        }

        return files.length == 1
                && files[0].isDirectory()
                && files[0].getName().equals("agent");
    }

    /**
     * Gets the GlassFish installation root (using property com.sun.aas.installRoot),
     * first from asenv.conf.  If that's not available, then from java.lang.System.
     *
     * @return path of GlassFish install root
     * @throws CommandException if the GlassFish install root is not found
     */
    protected String getInstallRootPath() throws CommandException {
        String installRootPath = getSystemProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY);

        if (!StringUtils.ok(installRootPath)) {
            installRootPath = System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
        }

        if (!StringUtils.ok(installRootPath)) {
            throw new CommandException("noInstallDirPath");
        }
        return installRootPath;
    }

    /**
     * Gets the GlassFish product installation root (using property
     * com.sun.aas.productRoot), first from asenv.conf. If that's not
     * available, then from java.lang.System.
     *
     * This will typically be the parent of the glassfish install root
     *
     * @return path of GlassFish product install root
     * @throws CommandException if the GlassFish install root is not found
     */
    protected String getProductRootPath() throws CommandException {
        String productRootPath = getSystemProperty(SystemPropertyConstants.PRODUCT_ROOT_PROPERTY);

        if (!StringUtils.ok(productRootPath))
            productRootPath = System.getProperty(SystemPropertyConstants.PRODUCT_ROOT_PROPERTY);

        if (!StringUtils.ok(productRootPath)) {
            // Product install root is parent of glassfish install root
            File installRoot = new File(getInstallRootPath());
            return installRoot.getParent();
        }
        return productRootPath;
    }

    protected String getNodeInstallDir() throws CommandException {
        String installDir = null;
        try {
            RemoteCLICommand rc = new RemoteCLICommand("get", this.programOpts, this.env);
            String s = rc.executeAndReturnOutput("get", "nodes.node." + node + ".install-dir");
            if (s != null) {
                installDir = s.substring(s.indexOf('=') + 1);
            }
        } catch (CommandException ce) {
            // ignore
        }
        return installDir;
    }

// -----------------------------------------------------------------------
// -------- everything below here is private    --------------------------
// -----------------------------------------------------------------------
    /**
     * Update DAS port from an old V2 das.properties file.
     * If the old port is the standard jrmp port, just use the new
     * standard http port.  Otherwise, prompt for the new port number
     * if possible.  In any event, try to rewrite the das.properties
     * file with the new values.
     */
    private int updateDasPort(Properties dasprops, int port, File propfile) {
        if (port == 8686) {     // the old JRMP port
            logger.info(Strings.get("Instance.oldDasProperties",
                    propfile.toString(), Integer.toString(port),
                    Integer.toString(programOpts.getPort())));
            port = programOpts.getPort();
        } else {
            try (ConsoleReader console = new ConsoleReader(System.in, System.out, null)) {
                if (console != null) {
                    String line = console.readLine(Strings.get("Instance.oldDasPropertiesPrompt",
                            propfile.toString(), Integer.toString(port),
                            Integer.toString(programOpts.getPort())));
                    while (line != null && line.length() > 0) {
                        try {
                            port = Integer.parseInt(line);
                            if (port > 0 && port <= 65535)
                                break;
                        } catch (NumberFormatException nfex) {
                            //try again
                        }
                        line = console.readLine(Strings.get("Instance.reenterPort", Integer.toString(programOpts.getPort())));
                    }
                } else {
                    logger.info(Strings.get("Instance.oldDasPropertiesWrong",
                            propfile.toString(), Integer.toString(port),
                            Integer.toString(programOpts.getPort())));
                    port = programOpts.getPort();
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error reading input", ioe);
                logger.info(Strings.get("Instance.oldDasPropertiesWrong",
                        propfile.toString(), Integer.toString(port),
                        Integer.toString(programOpts.getPort())));
                port = programOpts.getPort();
            }
        }

        dasprops.setProperty("agent.das.protocol", "http");
        dasprops.setProperty("agent.das.port", Integer.toString(port));
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(propfile));
            dasprops.store(bos, "Domain Administration Server Connection Properties");
            bos.close();
            bos = null;
        } catch (IOException ex2) {
            logger.info(Strings.get("Instance.dasPropertiesUpdateFailed"));
        }
        finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException cex) {
                    // ignore it
                }
            }
        }
        // whether we were able to update the file or not, keep going
        logger.log(Level.FINER, "New DAS port number: {0}", port);
        return port;
    }

    private File getTheOneAndOnlyNode(File parent) throws CommandException {
        // look for subdirs in the parent dir -- there must be one and only one
        // or there can be zero in which case we create one-and-only

        File[] files = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return isDirectory(f);
            }
        });

        // ERROR:  more than one node dir child
        if (files != null && files.length > 1) {
            throw new CommandException(Strings.get("tooManyNodes", parent));
        }

        // the usual case -- one node dir child
        if (files != null && files.length == 1)
            return files[0];

        /*
         * If there is no existing node dir child -- create one!
         * If the instance is on the same machine as DAS, use "localhost" as the node dir child
         * Only for _create-instance-filesystem
         */
        if (isCreateInstanceFilesystem) {
            try {
                String dashost = null;
                if (programOpts != null) {
                    dashost = programOpts.getHost();
                }
                String hostname = InetAddress.getLocalHost().getHostName();
                if (hostname.equals(dashost) || NetUtils.isThisHostLocal(dashost)) {
                    hostname = "localhost" + "-" + domainName;
                }
                File f = new File(parent, hostname);

                if (!(mkdirs(f) || isDirectory(f))) // for instance there is a regular file with that name
                {
                    throw new CommandException(Strings.get("cantCreateNodeDirChild", f));
                }

                return f;
            } catch (UnknownHostException ex) {
                throw new CommandException(Strings.get("cantGetHostName", ex));
            }
        } else {
            throw new CommandException(Strings.get("DeleteInstance.noInstance"));
        }
    }

    private File getTheOneAndOnlyInstance(File parent) throws CommandException {
        // look for subdirs in the parent dir -- there must be one and only one

        File[] files = parent.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return isDirectory(f);
            }
        });

        if (files == null || files.length == 0) {
            throw new CommandException(Strings.get("Instance.noInstanceDirs", parent));
        }

        // expect two - the "agent" directory and the instance directory
        if (files.length > 2 && checkOneAndOnly) {
            throw new CommandException(Strings.get("Instance.tooManyInstanceDirs", parent));
        }

        for (File f : files) {
            if (!f.getName().equals("agent")) {
                return f;
            }
        }
        throw new CommandException(Strings.get("Instance.noInstanceDirs", parent));
    }

    /**
     * Return the default value for nodeDirRoot, first checking if com.sun.aas.agentRoot
     * was specified in asenv.conf and returning this value. If not specified,
     * then the defaut value is the {GlassFish_Install_Root}/nodes.
     * nodeDirRoot is the parent directory of the node(s).
     *
     * @return String default nodeDirRoot - parent directory of node(s)
     * @throws CommandException if the GlassFish install root is not found
     */
    private String getNodeDirRootDefault() throws CommandException {
        String nodeDirDefault = getSystemProperty(SystemPropertyConstants.AGENT_ROOT_PROPERTY);

        if (StringUtils.ok(nodeDirDefault)) {
            return nodeDirDefault;
        }

        String installRootPath = getInstallRootPath();
        return installRootPath + "/" + "nodes";
    }

    @Override
    protected File getMasterPasswordFile() {

        if (nodeDirChild == null)
            return null;

        File mp = new File(new File(nodeDirChild,"agent"), "master-password");
        if (!mp.canRead()) {
            return null;
        }

        return mp;
    }

    /**
     * @return the instanceName
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * @param instanceName the instanceName to set
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
