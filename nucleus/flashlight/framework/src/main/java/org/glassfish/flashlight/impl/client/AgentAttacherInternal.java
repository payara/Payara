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
package org.glassfish.flashlight.impl.client;

import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.util.logging.*;
import org.glassfish.flashlight.FlashlightLoggerInfo;
import static org.glassfish.flashlight.FlashlightLoggerInfo.*;
import static com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY;

/**
 * created May 26, 2011
 * @author Byron Nevins
 */
final class AgentAttacherInternal {
    static boolean isAttached() {
        return isAttached;
    }

    static boolean attachAgent() {
        return attachAgent(-1, "");
    }

    static boolean attachAgent(int pid, String options) {
        try {
            if (isAttached)
                return true;

            if(pid < 0)
                pid = ProcessUtils.getPid();

            if (pid < 0) {
                logger.log(Level.WARNING, INVALID_PID);
                return false;
            }

            VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
            String ir = System.getProperty(INSTALL_ROOT_PROPERTY);
            File dir = new File(ir, "lib" + File.separator + "monitor");

            if (!dir.isDirectory()) {
                logger.log(Level.WARNING, MISSING_AGENT_JAR_DIR, dir);
                return false;
            }

            File agentJar = new File(dir, "flashlight-agent.jar");

            if (!agentJar.isFile()) {
                logger.log(Level.WARNING, MISSING_AGENT_JAR, dir);
                return false;
            }

            vm.loadAgent(SmartFile.sanitize(agentJar.getPath()), options);
            isAttached = true;
        }
        catch (Throwable t) {
            logger.log(Level.WARNING, ATTACH_AGENT_EXCEPTION, t.getMessage());
            isAttached = false;
        }
        
        return isAttached;
    }
    private static final Object syncOnMe = new Object();
    private static final Logger logger = FlashlightLoggerInfo.getLogger();
    private static boolean isAttached = false;
}
