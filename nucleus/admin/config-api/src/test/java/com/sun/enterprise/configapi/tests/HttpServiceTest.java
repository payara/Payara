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

package com.sun.enterprise.configapi.tests;

import org.glassfish.grizzly.config.dom.FileCache;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Protocol;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * HttpService related tests
 *
 * @author Jerome Dochez
 */
public class HttpServiceTest extends ConfigApiTest {
    public String getFileName() {
        return "DomainTest";
    }

    NetworkListener listener = null;

    @Before
    public void setup() {
        listener = getHabitat().<NetworkConfig>getService(NetworkConfig.class).getNetworkListener("admin-listener");
        assertTrue(listener != null);
    }

    @Test
    public void connectionTest() {
        final String max = listener.findHttpProtocol().getHttp().getMaxConnections();
        logger.fine("Max connections = " + max);
        assertEquals("Should only allow 250 connections.  The default is 256, however.", "250", max);
    }

    @Test
    public void validTransaction() throws TransactionFailure {
        final String max = listener.findHttpProtocol().getHttp().getMaxConnections();

        ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {
            public Object run(NetworkListener okToChange) throws TransactionFailure {
                final Http http = okToChange.createChild(Http.class);
                http.setMaxConnections("100");
                http.setTimeoutSeconds("65");
                http.setFileCache(http.createChild(FileCache.class));
                ConfigSupport.apply(new SingleConfigCode<Protocol>() {
                    @Override
                    public Object run(Protocol param) {
                        param.setHttp(http);
                        return null;
                    }
                }, okToChange.findHttpProtocol());
                return http;
            }
        }, listener);

        ConfigSupport.apply(new SingleConfigCode<Http>() {
            @Override
            public Object run(Http param) {
                param.setMaxConnections(max);
                return null;
            }
        }, listener.findHttpProtocol().getHttp());
        try {
            ConfigSupport.apply(new SingleConfigCode<Http>() {
                public Object run(Http param) throws TransactionFailure {
                    param.setMaxConnections("7");
                    throw new TransactionFailure("Sorry, changed my mind", null);
                }
            }, listener.findHttpProtocol().getHttp());
        } catch (TransactionFailure e) {
            logger.fine("good, got my exception about changing my mind");
        }
    }
}
