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

import com.sun.enterprise.util.i18n.StringManager;

/**
 *
 * @author rebeccas
 */
public class CLIConstants {

    // class should not be instantiable
    private CLIConstants() {}
    
    private static StringManager stringManager =
        StringManager.getManager(CLIConstants.class);

    private static final StringBuilder cliInstructions = new StringBuilder();
    static {
        cliInstructions.append("\n");
        cliInstructions.append(stringManager.getString("upgrade.common.upgrade_instructions"));
        cliInstructions.append("\n");
        cliInstructions.append("\n");
        cliInstructions.append(stringManager.getString("upgrade.common.upgrade_instructions_cont"));
        cliInstructions.append("\n");       
        cliInstructions.append("\n"); 
    }
    public static final String CLI_USER_INSTRUCTIONS = cliInstructions.toString();
	
    //- CLI options
    public static final String SOURCE = "source";
    public static final String SOURCE_SHORT = "s";
    public static final String TARGET = "target";
    public static final String TARGET_SHORT = "t";
	
    public static final String CLI_OPTION_CONSOLE_SHORT = "-c";
    public static final String CLI_OPTION_CONSOLE_LONG = "--console";
    public static final String CLI_OPTION_VERSION_UC_SHORT = "-V";
    public static final String CLI_OPTION_VERSION_LC_SHORT = "-v";
    public static final String CLI_OPTION_VERSION_LONG = "--version";  
    public static final String CLI_OPTION_HELP_SHORT = "-h";
    public static final String CLI_OPTION_HELP_LONG = "--help";
    public static final String CLI_OPTION_NOPROMPT = "noprompt"; 
	
}
