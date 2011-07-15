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

package com.sun.enterprise.tools.upgrade.common;

import com.sun.enterprise.tools.upgrade.common.arguments.ARG_source;
import com.sun.enterprise.tools.upgrade.common.arguments.ARG_target;
import com.sun.enterprise.tools.upgrade.common.arguments.ArgumentHandler;
import com.sun.enterprise.tools.upgrade.logging.LogService;
import com.sun.enterprise.util.i18n.StringManager;
import java.io.Console;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Utility to evaluate the CLI input arguments and prompt
 * the user for missing data or data determined to be invalid.
 */
public class InteractiveInputImpl implements DirectoryMover, InteractiveInput {

    private static final Logger logger = LogService.getLogger();
    private static final StringManager sm =
        StringManager.getManager(InteractiveInputImpl.class);
    private static final CommonInfoModel commonInfoModel =
        CommonInfoModel.getInstance();

    private static final Console console = System.console();
    
    private Map<String, ArgumentHandler> inputMap;

	@Override
    public void processArguments(ArrayList<ArgumentHandler> aList) {
        // this should only be called in the interactive case, but to be safe
        if (console == null) {
            throw new RuntimeException("CLI cannot be used with null console");
        }
        int cnt = aList.size();
        this.inputMap = new HashMap<String, ArgumentHandler>();
        for (int i = 0; i < cnt; i++) {
            ArgumentHandler tmpAh = aList.get(i);
            inputMap.put(tmpAh.getCmd(), tmpAh);
        }

        // in command line case, we want info to go to console as well
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(LogService.createFormatter());
        logger.addHandler(handler);
        
        sourcePrompt();
        targetPrompt();
        if (!CommonInfoModel.getInstance().isUpgradeSupported()) {
            System.exit(1);
        }
        masterPasswordPrompt();
    }

    private void sourcePrompt() {
        ArgumentHandler tmpA = inputMap.get(CLIConstants.SOURCE_SHORT);
        if (tmpA == null) {
            tmpA = inputMap.get(CLIConstants.SOURCE);
        }
        if (tmpA == null) {
            String source = console.readLine(
                sm.getString("enterprise.tools.upgrade.cli.Source_input"));
            tmpA = new ARG_source();
            tmpA.setRawParameters(source);
            inputMap.put(CLIConstants.SOURCE, tmpA);
        }
        //Check if input is a valid source directory input
        if (tmpA.isValidParameter()) {
            tmpA.exec();
        } else {
            System.err.println(sm.getString(
                "enterprise.tools.upgrade.cli.not_valid_source_install"));
            inputMap.remove(CLIConstants.SOURCE_SHORT);
            inputMap.remove(CLIConstants.SOURCE);
            sourcePrompt();
        }
    }
	
    private void targetPrompt() {
        ArgumentHandler tmpA = inputMap.get(CLIConstants.TARGET_SHORT);
        if (tmpA == null) {
            tmpA = inputMap.get(CLIConstants.TARGET);
        }
        if (tmpA == null) {
            String target = console.readLine(
                sm.getString("enterprise.tools.upgrade.cli.Target_input"));
            tmpA = new ARG_target();
            tmpA.setRawParameters(target);
            inputMap.put(CLIConstants.TARGET, tmpA);
        }

        // in the interactive CLI case, we'll allow users to fix name clashes
        tmpA.getCommonInfo().getTarget().setDirectoryMover(this);
        if (tmpA.isValidParameter()) {
            tmpA.exec();
        } else {
            System.err.println(sm.getString(
                "enterprise.tools.upgrade.cli.not_valid_target_install"));
            inputMap.remove(CLIConstants.TARGET_SHORT);
            inputMap.remove(CLIConstants.TARGET);
            targetPrompt();
        }
    }
	
    private void masterPasswordPrompt() {
        if (commonInfoModel.getSource().getMasterPassword() != null) {
            return;
        }
        char[] passwordChars = console.readPassword(
            sm.getString("enterprise.tools.upgrade.cli.MasterPW_input"));
        if (passwordChars.length > 0) {
            commonInfoModel.getSource().setMasterPassword(passwordChars);
        }
    }

    /**
     * Ask the user whether or not to move the
     * conflicting directory.
     */
    @Override
    public boolean moveDirectory(File dir) {
        final String no = sm.getString("enterprise.tools.upgrade.cli.no_option");
        final String yes = sm.getString("enterprise.tools.upgrade.cli.yes_option");
        System.out.print(sm.getString(
            "enterprise.tools.upgrade.cli.move_dir",
            dir.getName(), yes, no));
        String response = console.readLine();
        while (!yes.equals(response) && !no.equals(response)) {
            System.err.print(sm.getString(
                "enterprise.tools.upgrade.cli.move_dir_responses",
                yes, no));
            response = console.readLine();
        }
        boolean move = yes.equalsIgnoreCase(response);
        if (move) {
            UpgradeUtils.getUpgradeUtils(commonInfoModel).rename(dir);
        }
        return move;
    }
}
