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

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.api.deployment.ApplicationContainer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.util.Result;
import java.util.concurrent.Callable;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.config.GenericGrizzlyListener;
import org.glassfish.grizzly.config.GrizzlyListener;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;
import org.glassfish.kernel.KernelLoggerInfo;

/**
 * This class is responsible for configuring Grizzly.
 *
 * @author Jerome Dochez
 * @author Jeanfrancois Arcand
 */
public class GrizzlyProxy implements NetworkProxy {
    final Logger logger;
    final NetworkListener networkListener;

    protected GrizzlyListener grizzlyListener;
    private int portNumber;

    public final static String LEADER_FOLLOWER
            = "org.glassfish.grizzly.useLeaderFollower";

    public final static String AUTO_CONFIGURE
            = "org.glassfish.grizzly.autoConfigure";

    // <http-listener> 'address' attribute
    private InetAddress address;

    private GrizzlyService grizzlyService;

    //private VirtualServer vs;


    public GrizzlyProxy(GrizzlyService service, NetworkListener listener) {
        grizzlyService = service;       
        logger = service.getLogger();
        networkListener = listener;
    }

    /**
     * Create a <code>GrizzlyServiceListener</code> based on a NetworkListener
     * configuration object.
     */
    public void initialize() throws IOException {
        String port = networkListener.getPort();
        portNumber = 8080;
        if (port == null) {
            logger.severe(KernelLoggerInfo.noPort);
            throw new RuntimeException("Cannot find port information from domain configuration");
        }
        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.badPort, port);
        }
        try {
            address = InetAddress.getByName(networkListener.getAddress());
        } catch (UnknownHostException ex) {
            LogHelper.log(logger, Level.SEVERE, KernelLoggerInfo.badAddress, ex, address);
        }

        grizzlyListener = createGrizzlyListener(networkListener);

        grizzlyListener.configure(grizzlyService.getHabitat(), networkListener);
    }

    protected GrizzlyListener createGrizzlyListener(
            final NetworkListener networkListener) {
        if (GrizzlyService.isLightWeightListener(networkListener)) {
            return createServiceInitializerListener(networkListener);
        } else {
            return createGlassfishListener(networkListener);
        }
    }

    protected GrizzlyListener createGlassfishListener(
            final NetworkListener networkListener) {
        return new GlassfishNetworkListener(grizzlyService,
                networkListener, logger);
    }

    protected GrizzlyListener createServiceInitializerListener(
            final NetworkListener networkListener) {
        return new ServiceInitializerListener(grizzlyService,
                networkListener, logger);
    }

    static ArrayList<String> toArray(String list, String token){
        return new ArrayList<String>(Arrays.asList(list.split(token)));
    }

    /**
     * Stops the Grizzly service.
     */
    @Override
    public void stop() throws IOException {
        grizzlyListener.stop();
    }

    @Override
    public void destroy() {
        grizzlyListener.destroy();
    }

    @Override
    public String toString() {
        return "GrizzlyProxy{" +
                //"virtual server=" + vs +
                "address=" + address +
                ", portNumber=" + portNumber +
                '}';
    }


    /*
    * Registers a new endpoint (adapter implementation) for a particular
    * context-root. All request coming with the context root will be dispatched
    * to the adapter instance passed in.
    * @param contextRoot for the adapter
    * @param endpointAdapter servicing requests.
    */
    @Override
    public void registerEndpoint(String contextRoot, Collection<String> vsServers,
            HttpHandler endpointService,
            ApplicationContainer container) throws EndpointRegistrationException {
        
        // e.g., there is no admin service in an instance
        if (contextRoot == null) {
            return;
        }

        if (endpointService == null) {
            throw new EndpointRegistrationException(
                "The endpoint adapter is null");
        }

        final HttpAdapter httpAdapter = grizzlyListener.getAdapter(HttpAdapter.class);
        if (httpAdapter != null) {
            httpAdapter.getMapper().register(contextRoot, vsServers, endpointService, container);
        }
    }
    
    /**
     * Removes the context-root from our list of endpoints.
     */
    @Override
    public void unregisterEndpoint(String contextRoot, ApplicationContainer app) throws EndpointRegistrationException {
        final HttpAdapter httpAdapter = grizzlyListener.getAdapter(HttpAdapter.class);
        if (httpAdapter != null) {
            httpAdapter.getMapper().unregister(contextRoot);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerEndpoint(final Endpoint endpoint) {
        final HttpAdapter httpAdapter = grizzlyListener.getAdapter(HttpAdapter.class);
        if (httpAdapter != null) {
            httpAdapter.getMapper().register(endpoint);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterEndpoint(final Endpoint endpoint) throws EndpointRegistrationException {
        unregisterEndpoint(endpoint.getContextRoot(), endpoint.getContainer());
    }

    
    @Override
    public Future<Result<Thread>> start() throws IOException {
        final FutureImpl<Result<Thread>> future =
                Futures.<Result<Thread>>createUnsafeFuture();
        
        if (!isAjpEnabled(grizzlyListener)) {
            // If this is not AJP listener - initiate startup right now
            start0();
        } else {
            // For AJP listener we have to wait until server is up and ready
            // to process incoming requests
            // Related to the GLASSFISH-18267
            grizzlyService.addServerReadyListener(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    start0();
                    return null;
                }
            });
        }
        
        future.result(new Result<Thread>(Thread.currentThread()));
        return future;
    }

    /**
     * Start internal Grizzly listener.
     * @throws IOException 
     */
    protected void start0() throws IOException {
        final long t1 = System.currentTimeMillis();

        grizzlyListener.start();

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, KernelLoggerInfo.grizzlyStarted,
                    new Object[]{Grizzly.getDotedVersion(),
                    System.currentTimeMillis() - t1,
                    grizzlyListener.getAddress() + ":" + grizzlyListener.getPort()});
        }
    }
    
    @Override
    public int getPort() {
        return portNumber;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    public GrizzlyListener getUnderlyingListener() {
        return grizzlyListener;
    }

    private static boolean isAjpEnabled(final GrizzlyListener grizzlyListener) {
        return (grizzlyListener instanceof GenericGrizzlyListener) &&
                ((GenericGrizzlyListener) grizzlyListener).isAjpEnabled();
    }
}
