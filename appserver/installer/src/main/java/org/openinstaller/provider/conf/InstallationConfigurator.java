/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.openinstaller.provider.conf;

import org.openinstaller.config.PropertySheet;
import org.openinstaller.util.*;
import org.glassfish.installer.util.*;
import com.sun.pkg.bootstrap.Bootstrap;
import com.sun.pkg.client.Image;
import com.sun.pkg.client.SystemInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.Map;
import org.glassfish.installer.conf.Product;

/* A big fat chunk of Code, to be completely thrown away after MS3, to be
 * rewritten and refactored.
 */
public final class InstallationConfigurator implements Configurator, NotificationListener {

    private Product productRef;
    private String jdkHome;
    private Map<String, String> configData;
    private String productError = null;
    private static final Logger LOGGER;
    //Class wide flag to hold the overall configuration status.
    private boolean configSuccessful;

    public void setConfigData(Map<String, String> configData) {
        this.configData = configData;
    }

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }
    //OI
    private int gWaitCount;

    public InstallationConfigurator(final String productName, final String altRootDir,
            final String xcsFilePath, final String installDir) {
        productRef = new Product(productName,
                installDir,
                GlassFishUtils.getGlassfishAdminScriptPath(installDir),
                GlassFishUtils.getGlassfishConfigFilePath(installDir));
    }

    /*
     * OI hook to call individual product configurations.
     */
    public ResultReport configure(final PropertySheet propSheet, final boolean validateFlag) throws EnhancedException {

        configSuccessful = true;
        /* Storing a reference of Property Sheet to a local Hash, so that other
         * parts of this class can access the configuration data anytime needed.
         */
        setConfigData(propSheet.getAllProps());
        try {
            if (productRef.getProductName().equals("Domain")) {
                LOGGER.log(Level.INFO, Msg.get("CONFIGURING_GLASSFISH", null));
                configureGlassfish();
            }
        } catch (Exception e) {
            // Don't do anything as major error detection is handled throughout
            // this class where appropriate and fatal.
            LOGGER.log(Level.FINEST, e.getMessage());
        }

        try{
            if (productRef.getProductName().equals("UpdateTool")) {
                LOGGER.log(Level.INFO, Msg.get("CONFIGURING_UPDATETOOL", null));
                configureUpdatetool();
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINEST, e.getMessage());
            configSuccessful = false;
        }

        ResultReport.ResultStatus status =
                configSuccessful ? ResultReport.ResultStatus.SUCCESS
                : ResultReport.ResultStatus.FAIL;
        return new ResultReport(status, "http://www.oracle.com/pls/topic/lookup?ctx=821-2427&id=sjsaseeig ", "http://www.oracle.com/pls/topic/lookup?ctx=821-2427&id=sjsaseeig", null, productError);
    }

    /* Mandatory implementation of OI method?!?, not sure why? */
    public PropertySheet getCurrentConfiguration() {

        return new PropertySheet();
    }

    /*
     * OI hook to call individual product configurations.
     */
    public ResultReport unConfigure(final PropertySheet propSheet, final boolean validateFlag) {

        try {
            if (productRef.getProductName().equals("Domain")) {
                LOGGER.log(Level.INFO, Msg.get("UNCONFIGURING_GLASSFISH", null));
                unconfigureGlassfish();
            }

            if (productRef.getProductName().equals("UpdateTool")) {
                LOGGER.log(Level.INFO, Msg.get("UNCONFIGURING_UPDATETOOL", null));
                unconfigureUpdatetool();
                org.glassfish.installer.util.FileUtils.deleteDirectory(new File(productRef.getInstallLocation() + File.separator + "updatetool"));
                org.glassfish.installer.util.FileUtils.deleteDirectory(new File(productRef.getInstallLocation() + File.separator + "pkg"));
            }
            /* Delete the newly created folder, on windows. No incremental uninstallation, so delete everything.*/
            String folderName =
                    (String) TemplateProcessor.getInstance().getFromDataModel("PRODUCT_NAME");
            if (OSUtils.isWindows()) {
                WindowsShortcutManager wsShortMgr = new WindowsShortcutManager();
                wsShortMgr.deleteFolder(folderName);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINEST, e.getMessage());
        }

        return new ResultReport(ResultReport.ResultStatus.SUCCESS, "http://www.oracle.com/pls/topic/lookup?ctx=821-2427&id=sjsaseeig", "http://www.oracle.com/pls/topic/lookup?ctx=821-2427&id=sjsaseeig", null, productError);
    }

    /* Mandatory implementation of OI method?!?, not sure why? */
    public void handleNotification(final Notification aNotification,
            final Object aHandback) {
        /* We received a message from the configurator, so reset the count */
        synchronized (this) {
            gWaitCount = 0;
        }
    }

    /* Configure product glassfish */
    public void configureGlassfish() throws EnhancedException {

        // set executable permissions on most used scripts

        String installDir = productRef.getInstallLocation();
        if (!OSUtils.isWindows()) {
            LOGGER.log(Level.INFO, Msg.get("SETTING_EXECUTE_PERMISSIONS_FOR_GLASSFISH", null));
            org.glassfish.installer.util.FileUtils.setAllFilesExecutable(installDir + "/glassfish/bin");          
            org.glassfish.installer.util.FileUtils.setAllFilesExecutable(installDir + "/bin");
            if (org.glassfish.installer.util.FileUtils.isFileExist(installDir + "/mq/bin")) {
                org.glassfish.installer.util.FileUtils.setAllFilesExecutable(installDir + "/mq/bin");
            }
        }


        // Update asenv
        updateConfigFile();
        // Unpack all of *pack*ed files.
        unpackJars();
        //create domain startup/shutdown wrapper scripts
        if (OSUtils.isWindows()) {
            setupWindowsDomainScripts();
            createServerShortCuts();
        } else {
            setupUnixDomainScripts();
        }
    }

    /* Run configuration steps for update tool component. */
    public void configureUpdatetool() throws Exception {

        // set execute permissions for UC utilities
        if (!OSUtils.isWindows()) {
            LOGGER.log(Level.INFO, Msg.get("SETTING_EXECUTE_PERMISSIONS_FOR_UPDATETOOL", null));
            org.glassfish.installer.util.FileUtils.setExecutable(productRef.getInstallLocation() + "/bin/pkg");
            org.glassfish.installer.util.FileUtils.setExecutable(productRef.getInstallLocation() + "/bin/updatetool");
        }

        setupUpdateToolScripts();

        // check whether to bootstrap at all
        if (!ConfigHelper.getBooleanValue("UpdateTool.Configuration.BOOTSTRAP_UPDATETOOL")) {
            LOGGER.log(Level.INFO, Msg.get("SKIPPING_UPDATETOOL_BOOTSTRAP", null));
        } else {
            boolean allowUpdateCheck = ConfigHelper.getBooleanValue("UpdateTool.Configuration.ALLOW_UPDATE_CHECK");
            String proxyHost = configData.get("PROXY_HOST");
            String proxyPort = configData.get("PROXY_PORT");
            //populate bootstrap properties
            Properties props = new Properties();
            if (OSUtils.isWindows()) {
                props.setProperty("image.path", productRef.getInstallLocation().replace('\\', '/'));
            } else {
                props.setProperty("image.path", productRef.getInstallLocation());
            }
            props.setProperty("install.pkg", "true");
            if (!OSUtils.isAix()) {
                props.setProperty("install.updatetool", "true");
            } else {
                props.setProperty("install.updatetool", "false");
            }
            props.setProperty("optin.update.notification",
                    allowUpdateCheck ? "true" : "false");

            props.setProperty("optin.usage.reporting",
                    allowUpdateCheck ? "true" : "false");
            if ((proxyHost.length() > 0) && (proxyPort.length() > 0)) {
                props.setProperty("proxy.URL",
                        "http://" + proxyHost + ":" + proxyPort);
            }
            LOGGER.log(Level.INFO, Msg.get("BOOTSTRAPPING_UPDATETOOL", null));
            LOGGER.log(Level.FINEST, props.toString());

            // explicitly refreshing catalogs, workaround for bootstrap issue
            // proceed to bootstrap if there is an exception
            try {
                SystemInfo.initUpdateToolProps(props);
                Image img = new Image(productRef.getInstallLocation());
                img.refreshCatalogs();
            } catch (Exception e) {
                LOGGER.log(Level.FINEST, e.getMessage());
            }

            //invoke bootstrap
            Bootstrap.main(props, LOGGER);

        }
        // Create the required windows start->menu shortcuts for updatetool.
        if (OSUtils.isWindows()) {
            createUpdatetoolShortCuts();
        }
    }

    /* Undo updatetool configuration and post-installation setups.*/
    public void unconfigureUpdatetool() throws Exception {
        /* Try to shutdown the notifer. Don't do this on Mac, the notifier command
        does not work on Mac, refer to Issue #7348. */
        if (!OSUtils.isMac() && !OSUtils.isAix()) {
            try {
                String shutdownCommand;
                if (OSUtils.isWindows()) {
                    shutdownCommand = productRef.getInstallLocation() + "\\updatetool\\bin\\updatetool.exe";
                } else {
                    shutdownCommand = productRef.getInstallLocation() + "/updatetool/bin/updatetool";
                }
                String[] shutdownCommandArray = {shutdownCommand, "--notifier", "--shutdown"};
                LOGGER.log(Level.INFO, Msg.get("SHUTDOWN_NOTIFIER", null));
                ExecuteCommand shutdownExecuteCommand = new ExecuteCommand(shutdownCommandArray);
                shutdownExecuteCommand.setOutputType(ExecuteCommand.ERRORS | ExecuteCommand.NORMAL);
                shutdownExecuteCommand.setCollectOutput(true);
                LOGGER.log(Level.FINEST, shutdownExecuteCommand.expandCommand(shutdownExecuteCommand.getCommand()));
                shutdownExecuteCommand.execute();
            } catch (Exception e) {
                LOGGER.log(Level.FINEST, e.getMessage());
                // Its okay to ignore this for now.
            }
        } /* End, conditional code for Mac and Aix. */

        /* Now unregister notifer. */
        try {
            String configCommand;
            if (OSUtils.isWindows()) {
                configCommand = productRef.getInstallLocation() + "\\updatetool\\bin\\updatetoolconfig.bat";
            } else {
                configCommand = productRef.getInstallLocation() + "/updatetool/bin/updatetoolconfig";
            }
            String[] configCommandArray = {configCommand, "--unregister"};
            LOGGER.log(Level.INFO, Msg.get("UNREGISTER_NOTIFIER", null));
            ExecuteCommand configExecuteCommand = new ExecuteCommand(configCommandArray);
            configExecuteCommand.setOutputType(ExecuteCommand.ERRORS | ExecuteCommand.NORMAL);
            configExecuteCommand.setCollectOutput(true);
            LOGGER.log(Level.FINEST, configExecuteCommand.expandCommand(configExecuteCommand.getCommand()));

            configExecuteCommand.execute();
        } catch (Exception e) {
            LOGGER.log(Level.FINEST, e.getMessage());
            // Its okay to ignore this for now.

        }
    }

    /* Undo GlassFish configuration and post-installation setups.*/
    public void unconfigureGlassfish() {
        // Try to stop domain.
        stopDomain();
        LOGGER.log(Level.INFO, Msg.get("CLEANINGUP_DIRECTORIES", null));
        try {
            // Cleanup list includes both windows and non-windows files.
            // FileUtils does check for the file before deleting.
            String dirList[] = {
                productRef.getInstallLocation() + File.separator + "glassfish" + File.separator + "domains",
                productRef.getInstallLocation() + File.separator + "glassfish" + File.separator + "modules",
                productRef.getInstallLocation() + File.separator + "glassfish" + File.separator + "nodes",
                productRef.getInstallLocation() + File.separator + "glassfish" + File.separator + "lib"
            };
            for (int i = 0; i < dirList.length; i++) {
                LOGGER.log(Level.FINEST, dirList[i]);
                org.glassfish.installer.util.FileUtils.deleteDirectory(new File(dirList[i]));
            }

        } catch (Exception e) {
            LOGGER.log(Level.FINEST, e.getMessage() + "\n");
        }
    }


    /* Try to stop domain, so that uninstall can cleanup files effectively.
    Currently only tries to stop the default domain.
     */
    private void stopDomain() {
        ExecuteCommand asadminExecuteCommand = null;
        try {

            String[] asadminCommandArray = {GlassFishUtils.getGlassfishAdminScriptPath(productRef.getInstallLocation()), "stop-domain", "domain1"};
            LOGGER.log(Level.INFO, Msg.get("STOP_DEFAULT_DOMAIN", null));
            asadminExecuteCommand = new ExecuteCommand(asadminCommandArray);
            asadminExecuteCommand.setOutputType(ExecuteCommand.ERRORS | ExecuteCommand.NORMAL);
            asadminExecuteCommand.setCollectOutput(true);
            LOGGER.log(Level.FINEST, asadminExecuteCommand.expandCommand(asadminExecuteCommand.getCommand()));
            asadminExecuteCommand.execute();
        } catch (Exception e) {
        }
        LOGGER.log(Level.FINEST, asadminExecuteCommand.getAllOutput());

    }


    /* Creates shortcuts for windows. The ones created from OI will be removed due to
    mangled names. These shortcuts are in addition to the ones created by default.
    Since the descriptor for defining the short cut entry is not OS specific, we still
    need to carry on the xml entries to create items on Gnome.
     */
    private void createUpdatetoolShortCuts() {
        String folderName =
                (String) TemplateProcessor.getInstance().getFromDataModel("PRODUCT_NAME");
        LOGGER.log(Level.INFO, Msg.get("CREATE_SHORTCUT_HEADER",
                new String[]{folderName}));
        WindowsShortcutManager wsShortMgr = new WindowsShortcutManager();
        wsShortMgr.createFolder(folderName);
        String modifiedInstallDir = productRef.getInstallLocation().replace("\\", "\\\\");
        LOGGER.log(Level.FINEST, modifiedInstallDir);
        // Create short cut for starting update tool.
        wsShortMgr.createShortCut(
                folderName,
                Msg.get("START_UPDATE_TOOL", null),
                modifiedInstallDir + "\\\\bin\\\\updatetool.exe",
                Msg.get("START_UPDATE_TOOL", null),
                "\"",
                modifiedInstallDir + "\\\\updatetool\\\\vendor-packages\\\\updatetool\\\\images\\\\application-update-tool.ico",
                modifiedInstallDir + "\\\\bin",
                "2");
    }

    /* Creates shortcuts for windows. The ones created from OI will be removed due to
    manged names. These shortcuts are in addition to the ones created by default.
    Since the descriptor for defining the short cut entry is not OS specific, we still
    need to carry on the xml entries to create items on Gnome.
     */
    private void createServerShortCuts() throws EnhancedException {
        String folderName =
                (String) TemplateProcessor.getInstance().getFromDataModel("PRODUCT_NAME");
        LOGGER.log(Level.INFO, Msg.get("CREATE_SHORTCUT_HEADER",
                new String[]{folderName}));

        WindowsShortcutManager wsShortMgr = new WindowsShortcutManager();
        wsShortMgr.createFolder(folderName);
        String modifiedInstallDir = productRef.getInstallLocation().replace("\\", "\\\\");

        LOGGER.log(Level.FINEST, modifiedInstallDir);
        // Create short cut for uninstall.exe.
        wsShortMgr.createShortCut(
                folderName,
                Msg.get("UNINSTALL", null),
                modifiedInstallDir + "\\\\uninstall.exe",
                Msg.get("UNINSTALL", null),
                "-j \" & chr(34) & \"" + jdkHome.replace("\\", "\\\\") + "\" & chr(34)",
                modifiedInstallDir + "\\\\glassfish\\\\icons\\\\uninstall.ico",
                modifiedInstallDir,
                "2");

        // Create short cut for Quick Start guide.
        wsShortMgr.createShortCut(
                folderName,
                Msg.get("QSGUIDE", null),
                modifiedInstallDir + "\\\\glassfish\\\\docs\\\\quickstart.html");

        // Look for correct page deployed in the installdir before linking it.
        // this code is only w2k specific.
        String aboutFilesLocation = "\\glassfish\\docs\\";
        String aboutFiles[] = {"about_sdk.html", "about_sdk_web.html", "about.html"};
        // The default
        String aboutFile = "about.html";
        // Traverse through the list to find out which file exist first
        for (int i = 0; i < aboutFiles.length; i++) {
            File f = new File(modifiedInstallDir + aboutFilesLocation + aboutFiles[i]);
            if (f.exists()) {
                // then break
                aboutFile = aboutFiles[i];
                break;
            }
            f = null;
        }
        LOGGER.log(Level.FINEST, aboutFile);
        // Create short cut for About Page.
        wsShortMgr.createShortCut(
                folderName,
                Msg.get("ABOUT_GLASSFISH_SERVER", null),
                modifiedInstallDir + aboutFilesLocation.replace("\\", "\\\\") + aboutFile.replace("\\", "\\\\"));
    }

   
    private void updateConfigFile() throws EnhancedException {

        
	// for SDK cobundles with JDK - see if cobundled JDK exists and use that
        // checks for jdk7 directory since we only have JDK 7 cobundles 
       
        
        if (org.glassfish.installer.util.FileUtils.isFileExist(productRef.getInstallLocation() + File.separator + "jdk7")){
	   jdkHome = productRef.getInstallLocation() + File.separator + "jdk7";
           
           // on Unix, set executable permissions to jdk7/bin/* and jdk7/jre/bin/* 
           if (!OSUtils.isWindows()) {
              org.glassfish.installer.util.FileUtils.setAllFilesExecutable(productRef.getInstallLocation() + File.separator + "jdk7" 
                  + File.separator + "bin");
              org.glassfish.installer.util.FileUtils.setAllFilesExecutable(productRef.getInstallLocation() + File.separator + "jdk7" 
                  + File.separator + "jre" + File.separator + "bin");
           }
         }

         else {
 
	    // For all installation modes, fetch JAVA_HOME from panel;
	    // on MacOS and AIX use java.home property since panel is skipped
            
             try {
		 if (OSUtils.isMac() || OSUtils.isAix()) {
                    jdkHome = System.getProperty("java.home");
                 } else {
                    jdkHome = ConfigHelper.getStringValue("JDKSelection.directory.SELECTED_JDK");
		 }
             }
	     catch (Exception e) {
                 jdkHome = new File(System.getProperty("java.home")).getParent();
                 if (OSUtils.isMac() || OSUtils.isAix()) {
                    jdkHome = System.getProperty("java.home");
                 }
             }     
        }
        LOGGER.log(Level.INFO, Msg.get("UPDATE_CONFIG_HEADER", null));
        LOGGER.log(Level.INFO, Msg.get("JDK_HOME", new String[]{jdkHome}));
        //write jdkHome value to asenv.bat on Windows, asenv.conf on non-Windows platform...
        try {
            FileIOUtils configFile = new FileIOUtils();
            configFile.openFile(productRef.getConfigFilePath());
            /* Add AS_JAVA to end of buffer and file. */
            if (OSUtils.isWindows()) {
                configFile.appendLine("set AS_JAVA=" + jdkHome);
            } else {
                configFile.appendLine("AS_JAVA=" + jdkHome);
            }
            configFile.saveFile();
            configFile.closeFile();
        } catch (Exception ex) {
            LOGGER.log(Level.FINEST, ex.getMessage());
        }
    }

    /* Unpack compressed jars. */
    private void unpackJars() {
        //unpack jar files in all directories under glassfish/modules
        LOGGER.log(Level.INFO, Msg.get("UNPACK_HEADER", null));
        String dirList[] = {
            productRef.getInstallLocation() + File.separator + "glassfish" + File.separator + "modules",
            productRef.getInstallLocation() + File.separator + "glassfish" + File.separator + "modules" + File.separator + "endorsed",
            productRef.getInstallLocation() + File.separator + "glassfish" + File.separator + "modules" + File.separator + "autostart"};

        // if the jar extraction fails, then there is something really wrong.
        for (int i = 0; i < dirList.length; i++) {
            LOGGER.log(Level.FINEST, dirList[i]);
            Unpack modulesUnpack = new Unpack(new File(dirList[i]), new File(dirList[i]));
            if (!modulesUnpack.unpackJars()) {
                configSuccessful = false;
            }
        }
    }

    /* Create wrappers for asadmin start/stop on Windows.
     * This also should include copyright, that is currently taken from OSUtils.
     */
    private void setupWindowsDomainScripts() {
        LOGGER.log(Level.INFO, Msg.get("SETUP_STARTSTOP_SCRIPTS", null));
        try {
            FileIOUtils startFile = new FileIOUtils();
            startFile.openFile(productRef.getInstallLocation() + "\\glassfish\\lib\\asadmin-start-domain.bat");
            startFile.appendLine(GlassFishUtils.windowsCopyRightNoticeText);
            startFile.appendLine("setlocal");
            startFile.appendLine("call \"" + productRef.getInstallLocation() + "\\glassfish\\bin\\asadmin\" start-domain domain1\n");
            startFile.appendLine("pause");
            startFile.appendLine("endlocal");
            startFile.saveFile();
            startFile.closeFile();

            FileIOUtils stopFile = new FileIOUtils();
            stopFile.openFile(productRef.getInstallLocation() + "\\glassfish\\lib\\asadmin-stop-domain.bat");
            stopFile.appendLine(GlassFishUtils.windowsCopyRightNoticeText);
            stopFile.appendLine("setlocal");
            stopFile.appendLine("call \"" + productRef.getInstallLocation() + "\\glassfish\\bin\\asadmin\" stop-domain domain1\n");
            stopFile.appendLine("pause");
            stopFile.appendLine("endlocal");
            stopFile.saveFile();
            stopFile.closeFile();
        } catch (Exception ex) {
            LOGGER.log(Level.FINEST, ex.getMessage());
            // OK to ignore this for now.
        }

    }
    /* Create wrappers for asadmin start/stop on Solaris.
     * This also should include copyright, that is currently taken from OSUtils.
     */

    private void setupUnixDomainScripts() {
        LOGGER.log(Level.INFO, Msg.get("SETUP_STARTSTOP_SCRIPTS", null));
        try {
            FileIOUtils startFile = new FileIOUtils();
            startFile.openFile(productRef.getInstallLocation() + "/glassfish/lib/asadmin-start-domain");
            startFile.appendLine(GlassFishUtils.unixCopyRightNoticeText);
            startFile.appendLine("\"" + productRef.getInstallLocation() + "/glassfish/bin/asadmin\" start-domain domain1");
            startFile.saveFile();
            startFile.closeFile();

            FileIOUtils stopFile = new FileIOUtils();
            stopFile.openFile(productRef.getInstallLocation() + "/glassfish/lib/asadmin-stop-domain");
            stopFile.appendLine(GlassFishUtils.unixCopyRightNoticeText);
            stopFile.appendLine("\"" + productRef.getInstallLocation() + "/glassfish/bin/asadmin\" stop-domain domain1");
            stopFile.saveFile();
            stopFile.closeFile();

            org.glassfish.installer.util.FileUtils.setExecutable(new File(productRef.getInstallLocation() + "/glassfish/lib/asadmin-start-domain").getAbsolutePath());
            org.glassfish.installer.util.FileUtils.setExecutable(new File(productRef.getInstallLocation() + "/glassfish/lib/asadmin-stop-domain").getAbsolutePath());
        } catch (Exception ex) {
            LOGGER.log(Level.FINEST, ex.getMessage());
        }
    }

    /*create updatetool wrapper script used by shortcut items */
    private void setupUpdateToolScripts() {
        LOGGER.log(Level.INFO, Msg.get("SETUP_UPDATETOOL_SCRIPT", null));
        org.glassfish.installer.util.FileUtils.createDirectory(productRef.getInstallLocation()
                + File.separator
                + "updatetool"
                + File.separator
                + "lib");
        try {
            if (OSUtils.isWindows()) {
                FileIOUtils updateToolScript = new FileIOUtils();
                updateToolScript.openFile(productRef.getInstallLocation() + "\\updatetool\\lib\\updatetool-start.bat");
                updateToolScript.appendLine(GlassFishUtils.windowsCopyRightNoticeText);
                updateToolScript.appendLine("setlocal");
                updateToolScript.appendLine("cd \"" + productRef.getInstallLocation() + "\\updatetool\\bin\"");
                updateToolScript.appendLine("call updatetool.exe");
                updateToolScript.appendLine("endlocal");
                updateToolScript.saveFile();
                updateToolScript.closeFile();
            } else {
                FileIOUtils updateToolScript = new FileIOUtils();
                updateToolScript.openFile(productRef.getInstallLocation() + "/updatetool/lib/updatetool-start");
                updateToolScript.appendLine(GlassFishUtils.unixCopyRightNoticeText);
                updateToolScript.appendLine("cd \"" + productRef.getInstallLocation() + "/updatetool/bin\"");
                updateToolScript.appendLine("./updatetool");
                updateToolScript.saveFile();
                updateToolScript.closeFile();
                org.glassfish.installer.util.FileUtils.setExecutable(productRef.getInstallLocation() + "/updatetool/lib/updatetool-start");
            }

        } catch (Exception ex) {
            LOGGER.log(Level.FINEST, ex.getMessage());
        }
    }
}
