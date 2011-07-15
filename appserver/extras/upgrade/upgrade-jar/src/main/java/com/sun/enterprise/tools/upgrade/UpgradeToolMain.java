/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.upgrade;

import com.sun.enterprise.tools.upgrade.common.*;
import com.sun.enterprise.tools.upgrade.gui.MainFrame;
import com.sun.enterprise.tools.upgrade.logging.*;
import com.sun.enterprise.tools.upgrade.common.arguments.*;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.util.i18n.StringManager;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpgradeToolMain {

    private static final Logger logger = LogService.getLogger();

    static {
        String domainRoot = System.getProperty(UpgradeConstants.AS_DOMAIN_ROOT);
        if (domainRoot == null) {
            System.err.println("Configuration Error: AS_DEFS_DOMAINS_PATH is not set.");
            System.exit(1);
        }
    }    

    private static final StringManager sm =
        StringManager.getManager(UpgradeToolMain.class);
    private CommonInfoModel commonInfo = CommonInfoModel.getInstance();
 
    public UpgradeToolMain() {
        logger.log(Level.INFO,
            sm.getString("enterprise.tools.upgrade.start_upgrade_tool"));

        //- Have GF sets asenv.conf properties to system properties
        new ASenvPropertyReader();

        //- Default location of all traget server domains
        String rawTargetDomainRoot =
            System.getProperty(UpgradeConstants.AS_DOMAIN_ROOT);
        if (rawTargetDomainRoot == null) {
            rawTargetDomainRoot = "";
        }
        String targetDomainRoot = null;
        try {
            targetDomainRoot = new File(rawTargetDomainRoot).getCanonicalPath();
        } catch (IOException ioe) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format(
                    "Will not create canonical path for target: %s",
                    ioe.getLocalizedMessage()));
            }
            targetDomainRoot = new File(rawTargetDomainRoot).getAbsolutePath();
        }
        commonInfo.getTarget().setInstallDir(targetDomainRoot);
    }
    
    public void startGUI(String[] args) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, sm.getString(
                "enterprise.tools.upgrade.start_upgrade_tool_gui"));
        }
        if (args.length > 0) {
            //- set all vaild options user provided on cmd-line
            GUICmdLineInput guiIn = new GUICmdLineInput();
            guiIn.processArguments(guiIn.parse(args));
        }

        final UpgradeToolMain thisToolMain = this;
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame(thisToolMain).setVisible(true);
            }
        });
    }
    
    public void startCLI(String [] args){
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, sm.getString(
                "enterprise.tools.upgrade.start_upgrade_tool_cli"));
        }
        try{
			cliParse(args);
        }catch(Exception e){
            logger.log(Level.INFO, sm.getString(
                "enterprise.tools.upgrade.unexpected_parsing",
                e.toString()));
            System.exit(1);
        }
        this.upgrade();
    }
    
    private void cliParse(String[] args) {
        ArgsParser ap = new ArgsParser();
        ArrayList<ArgumentHandler> aList = ap.parse(args);

        InteractiveInput tmpI = new InteractiveInputImpl();
        tmpI.processArguments(aList);
        
        if (logger.isLoggable(Level.FINE)) {
            printArgs(aList);
        }

    }

    private void printArgs(ArrayList<ArgumentHandler> aList) {
        StringBuilder sb = new StringBuilder();
        for (ArgumentHandler tmpAh : aList) {
            if (tmpAh instanceof ARG_c || tmpAh instanceof ARG_console ||
                tmpAh instanceof ARG_h || tmpAh instanceof ARG_help ||
                tmpAh instanceof ARG_V || tmpAh instanceof ARG_version) {
                sb.append("-").append(tmpAh.getCmd());
            } else {
                sb.append("-").append(tmpAh.getCmd());
                sb.append(" ").append(tmpAh.getRawParameter());
            }
            sb.append(" ");
        }
        logger.fine(UpgradeConstants.ASUPGRADE + " " + sb.toString());
    }
	    
    private void upgrade() {
        try {
            commonInfo.setupTasks();

            try {
                DomainsProcessor dProcessor = new DomainsProcessor(commonInfo);
                TargetAppSrvObj _target = commonInfo.getTarget();
                int exitValue = dProcessor.startDomain(_target.getDomainName());
                logger.info(sm.getString("enterprise.tools.end.asadmin.out"));

                if (exitValue != 0) {
                    logger.warning(sm.getString(
                        "enterprise.tools.upgrade.processExitValue", exitValue));
                } else {
                    logger.info(sm.getString("enterprise.tools.upgrade.done"));
                }

            } catch (HarnessException he) {
                logger.log(Level.SEVERE, sm.getString(
                    "enterprise.tools.upgrade.generalException", he));
            }

        } catch (Exception e) {
            logger.log(Level.INFO, e.getMessage());
        }
    }

    public static void main(String[] args) {
        UpgradeToolMain main = new UpgradeToolMain();
        boolean isCLIcmd = false;
        for (int i = 0; i < args.length; i++) {
            //- -c/--console option is not position dependent
            if (args[i].equals(CLIConstants.CLI_OPTION_CONSOLE_SHORT) ||
                args[i].equals(CLIConstants.CLI_OPTION_CONSOLE_LONG)) {
                isCLIcmd = true;
            }

            if (args[i].equals(CLIConstants.CLI_OPTION_HELP_SHORT) ||
                args[i].equals(CLIConstants.CLI_OPTION_HELP_LONG)) {
                ARG_help tmpH = new ARG_help();
                tmpH.exec();
                System.exit(0);
            }
        }

        if (isCLIcmd) {
            main.startCLI(args);
            System.exit(0);
        } else {
            main.startGUI(args);
        }

    }

    /*
     * Called from the GUI worker thread to perform the upgrade.
     */
    public void performUpgrade() {
        upgrade();
    }
}
