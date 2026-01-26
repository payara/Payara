/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2025] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package com.sun.enterprise.v3.bootstrap;

import com.sun.enterprise.module.bootstrap.StartupContext;
import fish.payara.internal.api.PostBootRunLevel;
import fish.payara.boot.runtime.BootCommands;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

@Service
@RunLevel(value = PostBootRunLevel.VAL)
public class BootCommandService implements PostConstruct {

    private static final Logger LOGGER = Logger.getLogger(BootCommandService.class.getName());

    @Inject
    StartupContext startupContext;

    @Inject
    CommandRunner commandRunner;

    @Inject
    ServerEnvironment env;

    /**
     * Runs a series of commands from a file
     * @param file
     */
    public void doBootCommands(String file, boolean expandValues) {
        if (file == null) {
            return;
        }
        try {
            BootCommands bootCommands = new BootCommands();
            System.out.println("Reading in commands from " + file);
            bootCommands.parseCommandScript(new File(file), expandValues);
            bootCommands.executeCommands(commandRunner);
        } catch (IOException ex) {
            LOGGER.log(SEVERE, "Error reading from file");
        } catch (Throwable ex) {
            LOGGER.log(SEVERE, null, ex);
        }
    }

    @Override
    public void postConstruct() {
        try{
            LOGGER.fine("Satisfying Optional Packages dependencies...");
            InstalledLibrariesResolver.initializeInstalledLibRegistry(env.getLibPath().getAbsolutePath());
        }catch(Exception e){
            LOGGER.log(Level.WARNING, KernelLoggerInfo.exceptionOptionalDepend, e);
        }

        doBootCommands(startupContext.getArguments().getProperty("-postbootcommandfile"), true);
    }
}
