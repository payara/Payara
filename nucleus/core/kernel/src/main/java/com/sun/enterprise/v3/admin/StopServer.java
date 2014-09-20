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
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.kernel.KernelLoggerInfo;

/**
 * A class to house identical code for stopping instances and DAS
 * @author Byron Nevins
 */
public class StopServer {

    /**
     * Shutdown of the server :
     *
     * All running services are stopped.
     * LookupManager is flushed.
     */
    protected final void doExecute(ServiceLocator habitat, ServerEnvironment env, boolean force) {
        try {
            KernelLoggerInfo.getLogger().info(KernelLoggerInfo.serverShutdownInit);
            // Don't shutdown GlassFishRuntime, as that can bring the OSGi framework down which is wrong
            // when we are embedded inside an existing runtime. So, just stop the glassfish instance that
            // we are supposed to stop. Leave any cleanup to some other code.

            // get the GlassFish object - we have to wait in case startup is still in progress
            // This is a temporary work-around until HK2 supports waiting for the service to
            // show up in the ServiceLocator.
            GlassFish gfKernel = habitat.getService(GlassFish.class);
            while (gfKernel == null) {
                Thread.sleep(1000);
                gfKernel = habitat.getService(GlassFish.class);
            }
            // gfKernel is absolutely positively for-sure not null.
            gfKernel.stop();
        }
        catch (Throwable t) {
            // ignore
        }


        if (force) {
            System.exit(0);
        }
        else {
            deletePidFile(env);
        }
    }

    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(StopServer.class);

    /**
     * It is **Essential** to delete this file!  Other code will assume the server
     * is running if it exists.
     * Any old App is currently (10/10/10) allowed to add a shutdownhook with a System.exit()
     * which is GUARANTEED to prevent the shutdown hook for deleting the pidfile to run.
     * So -- we always do it BEFORE trying to exit.
     */
    private void deletePidFile(ServerEnvironment env) {
        File pidFile = new File(env.getConfigDirPath(), "pid");

        if (pidFile.isFile()) {
            FileUtils.deleteFileNowOrLater(pidFile);
        }
    }
}
