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

package com.sun.enterprise.resource.naming;

import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import com.sun.logging.LogDomains;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.internal.api.Globals;

/**
 * Utility class to bootstrap connector-runtime.<br>
 * Must be used only for ObjectFactory implementations of connector, only in CLIENT mode<br>
 */
public class ConnectorNamingUtils {

    private static Logger _logger =
    LogDomains.getLogger(ConnectorNamingUtils.class, LogDomains.RSR_LOGGER);

    private volatile static ConnectorRuntime runtime;

    static {
        //making sure that connector-runtime is always initialized.
        //This solves the issue of multiple threads doing resource lookup in STANDALONE mode.
        getRuntime();
    }

    public static ConnectorRuntime getRuntime() {
        try {
            if (runtime == null) {
                synchronized(ConnectorNamingUtils.class) {
                    if(runtime == null) {
                        runtime = ConnectorRuntime.getRuntime();
                    }
                }
            }
        } catch (Exception e) {
            // Assuming that connector runtime is always available in SERVER and APPCLIENT mode and
            // hence this is CLIENT mode
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.log(Level.FINEST, "unable to get Connector Runtime due to the following exception, " +
                    "trying client mode", e);
            }
            runtime = getHabitat().getService(ConnectorRuntime.class);
        }
        return runtime;
    }

    static private ServiceLocator getHabitat() {
        ServiceLocator habitat = Globals.getStaticHabitat();
        StartupContext startupContext = new StartupContext();
        ServiceLocatorUtilities.addOneConstant(habitat, startupContext);
        ServiceLocatorUtilities.addOneConstant(habitat,
                new ProcessEnvironment(ProcessEnvironment.ProcessType.Other));

        return habitat;
    }
}
