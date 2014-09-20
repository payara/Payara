/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Logger;
import org.glassfish.grizzly.config.dom.NetworkListener;

import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transport;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.threadpool.GrizzlyExecutorService;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * This class extends Grizzly's GrizzlyServiceListener class to customize it for GlassFish and enable a single listener
 * do both lazy service initialization as well as init of HTTP and admin listeners
 *
 * @author Vijay Ramachandran
 * @author Alexey Stashok
 */
public class ServiceInitializerListener extends org.glassfish.grizzly.config.GenericGrizzlyListener {
    private final Logger logger;
    private final GrizzlyService grizzlyService;
    private final NetworkListener networkListener;

    public ServiceInitializerListener(final GrizzlyService grizzlyService,
            final NetworkListener networkListener,
            final Logger logger) {
        this.grizzlyService = grizzlyService;
        this.networkListener = networkListener;
        this.logger = logger;
    }

    public NetworkListener getNetworkListener() {
        return networkListener;
    }
    
    @Override
    protected void configureTransport(final NetworkListener networkListener,
                                      final Transport transportConfig,
                                      final FilterChainBuilder filterChainBuilder) {
        
        transport = configureDefaultThreadPoolConfigs(
                TCPNIOTransportBuilder.newInstance().build());

        final int acceptorThreads = transportConfig != null
                ? Integer.parseInt(transportConfig.getAcceptorThreads())
                : Transport.ACCEPTOR_THREADS;
        
        transport.setSelectorRunnersCount(acceptorThreads);
        transport.getKernelThreadPoolConfig().setPoolName(networkListener.getName() + "-kernel");
        
        if (acceptorThreads > 0) {
            transport.getKernelThreadPoolConfig()
                    .setCorePoolSize(acceptorThreads)
                    .setMaxPoolSize(acceptorThreads);
        }
        
        rootFilterChain = FilterChainBuilder.stateless().build();

        transport.setProcessor(rootFilterChain);
        transport.setIOStrategy(SameThreadIOStrategy.getInstance());
    }


    @Override
    protected void configureProtocol(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final Protocol protocol, final FilterChainBuilder filterChainBuilder) {
        filterChainBuilder.add(new ServiceInitializerFilter(this,
                grizzlyService.getHabitat(), logger));
    }

    @Override
    protected void configureThreadPool(final ServiceLocator habitat,
            final NetworkListener networkListener,
            final ThreadPool threadPool) {
        
        // we don't need worker thread pool
        transport.setWorkerThreadPoolConfig(null);
    }
}
