/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.glassfish.api.container.Container;
import org.glassfish.api.container.Sniffer;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ContainerRegistry;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.module.Module;

/**
 * This class is responsible for starting containers.
 *
 * @author Jerome Dochez, Sanjeeb Sahoo
 */
@Service
public class ContainerStarter {

	@Inject
	ServiceLocator serviceLocator;
	
    @Inject
    ServiceLocator habitat;

    @Inject
    Logger logger;

    @Inject
    ServerEnvironmentImpl env;

    @Inject ContainerRegistry registry;

    public Collection<EngineInfo> startContainer(Sniffer sniffer) {

        assert sniffer!=null;
        String containerName = sniffer.getModuleType();
        assert containerName!=null;
        
        // I do the container setup first so the code has a chance to set up
        // repositories which would allow access to the container module.
        try {

            Module[] modules = sniffer.setup(null, logger);
            logger.logp(Level.FINE, "ContainerStarter", "startContainer", "Sniffer {0} set up following modules: {1}",
                    new Object[]{sniffer, modules != null ? Arrays.toString(modules): ""});
        } catch(FileNotFoundException fnf) {
            logger.log(Level.SEVERE, fnf.getMessage());
            return null;
        } catch(IOException ioe) {
            logger.log(Level.SEVERE, ioe.getMessage(), ioe);
            return null;

        }

        // first the right container from that module.
        Map<String, EngineInfo> containers = new HashMap<String, EngineInfo>();
        for (String name : sniffer.getContainersNames()) {
            ServiceHandle<Container> provider = serviceLocator.getServiceHandle(Container.class, name);
            if (provider == null) {
                logger.severe("Cannot find Container named " + name + ", so unable to start " + sniffer.getModuleType() + " container");
                return null;
            }
            EngineInfo info = new EngineInfo(provider, sniffer, null /* never used */);
            containers.put(name, info);
        }
        // Now that we have successfully created all containers, let's register them as well.
        for (Map.Entry<String, EngineInfo> entry : containers.entrySet()) {
            registry.addContainer(entry.getKey(), entry.getValue());
        }
        return containers.values();
    }


}
