/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.nio.SelectorHandler;
import org.glassfish.grizzly.nio.SelectorRunner;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.grizzly.LazyServiceInitializer;

/**
 * The {@link org.glassfish.grizzly.filterchain.Filter} implementation,
 * which lazily initializes custom service on the first accepted connection
 * and passes connection there.
 *
 * @author Vijay Ramachandran
 */
public class ServiceInitializerFilter extends BaseFilter {
    private final ServiceLocator locator;
    private volatile LazyServiceInitializer targetInitializer = null;
    private final List<ActiveDescriptor<?>> initializerImplList;
    
    protected final Logger logger;

    private final ServiceInitializerListener listener;

    private final Object LOCK_OBJ = new Object();
//    private long timeout = 60000;

    public ServiceInitializerFilter(final ServiceInitializerListener listener,
            final ServiceLocator habitat, final Logger logger) {
        this.locator = habitat;
        
        initializerImplList =
                habitat.getDescriptors(BuilderHelper.createContractFilter(LazyServiceInitializer.class.getName()));

        if (initializerImplList.isEmpty()) {
            throw new IllegalStateException("NO Lazy Initializer was found for port = " +
                    listener.getPort());
        }

        this.logger = logger;
        this.listener = listener;
    }

    @Override
    public NextAction handleAccept(final FilterChainContext ctx) throws IOException {
        final NIOConnection nioConnection = (NIOConnection) ctx.getConnection();
        final SelectableChannel channel = nioConnection.getChannel();
        
        // The LazyServiceInitializer's name we're looking for should be equal
        // to either listener or protocol name
        final String listenerName = listener.getName();
        final String protocolName = listener.getNetworkListener().getProtocol();
        
        if (targetInitializer == null) {
            synchronized (LOCK_OBJ) {
                if (targetInitializer == null) {
                    LazyServiceInitializer targetInitializerLocal = null;
                    for (final ActiveDescriptor<?> initializer : initializerImplList) {
                        String serviceName = initializer.getName();
                        
                        
                        if (serviceName != null &&
                                (listenerName.equalsIgnoreCase(serviceName) ||
                                protocolName.equalsIgnoreCase(serviceName))) {
                            targetInitializerLocal = (LazyServiceInitializer) locator.getServiceHandle(initializer).getService();
                            break;
                        }
                    }

                    if (targetInitializerLocal == null) {
                        logger.log(Level.SEVERE, "NO Lazy Initialiser implementation was found for port = {0}",
                                String.valueOf(listener.getPort()));
                        nioConnection.close();

                        return ctx.getStopAction();
                    }
                    if (!targetInitializerLocal.initializeService()) {
                        logger.log(Level.SEVERE, "Lazy Service initialization failed for port = {0}",
                                String.valueOf(listener.getPort()));

                        nioConnection.close();

                        return ctx.getStopAction();
                    }
                    
                    targetInitializer = targetInitializerLocal;
                }
            }
        }

        final NextAction nextAction = ctx.getSuspendAction();
        ctx.completeAndRecycle();

        // Deregister channel
        final SelectorRunner runner = nioConnection.getSelectorRunner();
        final SelectorHandler selectorHandler =
                ((NIOTransport) nioConnection.getTransport()).getSelectorHandler();

        selectorHandler.deregisterChannel(runner, channel);

        // Underlying service rely the channel is blocking
        channel.configureBlocking(true);
        targetInitializer.handleRequest(channel);

        return nextAction;
    }
}
