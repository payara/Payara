/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.services.impl;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.v3.server.ContainerStarter;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.container.Sniffer;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.data.ContainerRegistry;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;

/**
 * These adapters are temporarily registered to the mapper to handle static
 * pages request that a container would like to process rather than serving
 * them statically unchanged. This is useful for things like .jsp or .php
 * files saved in the context root of the application server.
 *
 * @author Jerome Dochez
 * @author Jeanfrancois Arcand
 */
@Service
@PerLookup
public class SnifferAdapter extends HttpHandler {

    @Inject
    ContainerRegistry containerRegistry;

    @Inject
    ContainerStarter containerStarter;

    @Inject
    ModulesRegistry modulesRegistry;

    private static final Logger LOGGER = KernelLoggerInfo.getLogger();
    
    private Sniffer sniffer;
    private ContainerMapper mapper;
    private HttpHandler adapter;

    public void initialize(Sniffer sniffer, ContainerMapper mapper) {
        this.sniffer = sniffer;
        this.mapper = mapper;
    }

    // I could synchronize this method since I only start one container and do it
    // synchronously but that seems like an overkill and I would still need to handle
    // pending requests.
    @Override
    public void service(Request req, Response resp) throws Exception {

        if (adapter != null) {
            // this is not supposed to happen, however due to multiple requests coming in, I would
            // not be surprised...
            adapter.service(req, resp);
            return;
        }

        // bingo, we found a sniffer that wants to handle this requested
        // page, let's get to the container or start it.
        // start all the containers associated with sniffers.

        // need to synchronize on the registry to not end up starting the same container from
        // different threads.
        synchronized (containerRegistry) {
            if (adapter != null) {
                // I got started in the meantime
                adapter.service(req, resp);
                return;
            }

            if (containerRegistry.getContainer(sniffer.getContainersNames()[0]) != null) {
                LOGGER.fine("Container is claimed to be started...");
                containerRegistry.getContainer(sniffer.getContainersNames()[0]).getContainer();
            } else {
                final long startTime = System.currentTimeMillis();
                LOGGER.log(Level.INFO, KernelLoggerInfo.snifferAdapterStartingContainer, sniffer.getModuleType());
                try {
                    Collection<EngineInfo> containersInfo = containerStarter.startContainer(sniffer);
                    if (containersInfo != null && !containersInfo.isEmpty()) {
                        // force the start on each container
                        for (EngineInfo info : containersInfo) {
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(Level.FINE, "Got container, deployer is {0}", info.getDeployer());
                            }
                            info.getContainer();
                            LOGGER.log(Level.INFO, KernelLoggerInfo.snifferAdapterContainerStarted,
                                    new Object[]{sniffer.getModuleType(), System.currentTimeMillis() - startTime});
                        }
                    } else {
                        LOGGER.severe(KernelLoggerInfo.snifferAdapterNoContainer);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE,
                               KernelLoggerInfo.snifferAdapterExceptionStarting,
                               new Object[] { sniffer.getContainersNames()[0], e });
                }
            }

            // at this point the post construct should have been called.
            // seems like there is some possibility that the container is not synchronously started
            // preventing the calls below to succeed...
            DataChunk decodedURI = req.getRequest().getRequestURIRef().getDecodedRequestURIBC();
            try {
                // Clear the previous mapped information.
                MappingData mappingData = (MappingData) req.getNote(ContainerMapper.MAPPING_DATA);
                mappingData.recycle();

                adapter = mapper.mapUriWithSemicolon(req, decodedURI, 0, null);
                // If a SnifferAdapter doesn't do it's job, avoid recursion 
                // and throw a Runtime exception.
                if (adapter.equals(this)) {
                    adapter = null;
                    throw new RuntimeException("SnifferAdapter cannot map themself.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, KernelLoggerInfo.snifferAdapterExceptionMapping, e);
                throw e;
            }

            // pass on,,,
            if (adapter != null) {
                adapter.service(req, resp);
            } else {
                throw new RuntimeException("No Adapter found.");
            }
        }
    }
}
