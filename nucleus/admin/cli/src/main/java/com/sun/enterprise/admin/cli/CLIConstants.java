/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli;

/**
 * Constants for use in this package and "sub" packages
 * @author bnevins
 */
public class CLIConstants {
    ////////////////////////////////////////////////////////////////////////////
    ///////       public                   /////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    public static final int     DEFAULT_ADMIN_PORT              = 4848;
    public static final String  DEFAULT_HOSTNAME                = "localhost";
    public static final String  EOL                             = System.getProperty("line.separator");

    public static final long    WAIT_FOR_DAS_TIME_MS            = 10 * 60 * 1000; // 10 minutes
    public static final int     RESTART_NORMAL                  = 10;
    public static final int     RESTART_DEBUG_ON                = 11;
    public static final int     RESTART_DEBUG_OFF               = 12;
    public static final String  WALL_CLOCK_START_PROP           = "WALL_CLOCK_START";
    public static final String  MASTER_PASSWORD                 = "AS_ADMIN_MASTERPASSWORD";
    public static final int     SUCCESS                         = 0;
    public static final int     ERROR                           = 1;
    public static final int     WARNING                         = 4;
    public static final long     DEATH_TIMEOUT_MS                = 1 * 60 * 1000;

    public static final String K_ADMIN_PORT = "agent.adminPort";
    public static final String K_ADMIN_HOST = "agent.adminHost";
    public static final String K_AGENT_PROTOCOL = "agent.protocol";
    public static final String K_CLIENT_HOST = "agent.client.host";
    public static final String K_DAS_HOST = "agent.das.host";
    public static final String K_DAS_PROTOCOL = "agent.das.protocol";
    public static final String K_DAS_PORT = "agent.das.port";
    public static final String K_DAS_IS_SECURE = "agent.das.isSecure";

    public static final String K_MASTER_PASSWORD = "agent.masterpassword";
    public static final String K_SAVE_MASTER_PASSWORD = "agent.saveMasterPassword";

    public static final String AGENT_LISTEN_ADDRESS_NAME="listenaddress";
    public static final String REMOTE_CLIENT_ADDRESS_NAME="remoteclientaddress";
    public static final String AGENT_JMX_PROTOCOL_NAME="agentjmxprotocol";
    public static final String DAS_JMX_PROTOCOL_NAME="dasjmxprotocol";
    public static final String AGENT_DAS_IS_SECURE="isDASSecure";

    public static final String NODEAGENT_DEFAULT_DAS_IS_SECURE = "false";
    public static final String NODEAGENT_DEFAULT_DAS_PORT = String.valueOf(CLIConstants.DEFAULT_ADMIN_PORT);
    public static final String NODEAGENT_DEFAULT_HOST_ADDRESS = "0.0.0.0";
    public static final String NODEAGENT_JMX_DEFAULT_PROTOCOL = "rmi_jrmp";
    public static final String HOST_NAME_PROPERTY = "com.sun.aas.hostName";
    public static final int RESTART_CHECK_INTERVAL_MSEC = 300;

    ////////////////////////////////////////////////////////////////////////////
    ///////       private                   ////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////


    private CLIConstants() {
       // no instances allowed!
    }
}
