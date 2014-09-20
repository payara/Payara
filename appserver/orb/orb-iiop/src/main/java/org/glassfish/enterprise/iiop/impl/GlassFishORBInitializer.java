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

package org.glassfish.enterprise.iiop.impl;

import com.sun.logging.LogDomains;
import org.glassfish.enterprise.iiop.api.IIOPInterceptorFactory;
import org.glassfish.enterprise.iiop.util.IIOPUtils;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This file implements an initializer class for all portable interceptors
 * used in the J2EE RI (currently security and transactions).
 * It registers the IOR, client and server request interceptors.
 *
 * @author Vivek Nagar
 * @author Mahesh Kannan
 *
 */

public class GlassFishORBInitializer extends org.omg.CORBA.LocalObject
        implements ORBInitializer {
    private static final Logger _logger =
            LogDomains.getLogger(GlassFishORBInitializer.class, LogDomains.CORBA_LOGGER);

    private static void fineLog( String fmt, Object... args ) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, fmt, args ) ;
        }
    }

    public GlassFishORBInitializer() {
        /*
        //Ken feels that adding the property to orbInitProperties
        // is better than setting System properties
        try {
            System.setProperty(
                    com.sun.jts.pi.InterceptorImpl.CLIENT_POLICY_CHECKING,
                    String.valueOf(false));
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "iiop.readproperty_exception", ex);
        }
        */
    }

    /**
     * This method is called during ORB initialization.
     *
     * @param info object that provides initialization attributes
     *            and operations by which interceptors are registered.
     */
    @Override
    public void pre_init(org.omg.PortableInterceptor.ORBInitInfo info) {
    }

    /**
     * This method is called during ORB initialization.
     *
     * @param info object that provides initialization attributes
     *            and operations by which interceptors are registered.
     */
    @Override
    public void post_init(org.omg.PortableInterceptor.ORBInitInfo info) {
        Codec codec = null;

        fineLog( "J2EE Initializer post_init");
        fineLog( "Creating Codec for CDR encoding");

        CodecFactory cf = info.codec_factory();

        byte major_version = 1;
        byte minor_version = 2;
        Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value,
                major_version, minor_version);
        try {
            codec = cf.create_codec(encoding);

            IIOPUtils iiopUtils = IIOPUtils.getInstance();
            Collection<IIOPInterceptorFactory> interceptorFactories =
                    iiopUtils.getAllIIOPInterceptrFactories();

            for (IIOPInterceptorFactory factory : interceptorFactories) {
                fineLog( "Processing interceptor factory: {0}", factory);

                ClientRequestInterceptor clientReq =
                        factory.createClientRequestInterceptor(info, codec);
                ServerRequestInterceptor serverReq =
                        factory.createServerRequestInterceptor(info, codec);

                if (clientReq != null) {
                    fineLog( "Registering client interceptor: {0}", clientReq);
                    info.add_client_request_interceptor(clientReq);
                }
                if (serverReq != null) {
                    fineLog( "Registering server interceptor: {0}", serverReq);
                    info.add_server_request_interceptor(serverReq);
                }
            }

        } catch (Exception e) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, "Exception registering interceptors", e ) ;
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

