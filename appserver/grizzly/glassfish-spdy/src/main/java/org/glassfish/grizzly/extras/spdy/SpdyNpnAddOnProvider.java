/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.grizzly.extras.spdy;

import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.TransportProbe;
import org.glassfish.grizzly.config.ConfigAwareElement;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Spdy;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.spdy.NextProtoNegSupport;
import org.glassfish.grizzly.spdy.SpdyAddOn;
import org.glassfish.grizzly.spdy.SpdyMode;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name="spdy-npn")
@ContractsProvided({SpdyNpnAddOnProvider.class, AddOn.class})
public class SpdyNpnAddOnProvider extends SpdyAddOn implements ConfigAwareElement<Spdy> {

    private FilterChainBuilder filterChainBuilder;
    
    public SpdyNpnAddOnProvider() {
        super(SpdyMode.NPN);
    }

    @Override
    public void configure(final ServiceLocator habitat,
            final NetworkListener networkListener, final Spdy spdy) {
        setInitialWindowSize(spdy.getInitialWindowSizeInBytes());
        setMaxConcurrentStreams(spdy.getMaxConcurrentStreams());
        setMaxFrameLength(spdy.getMaxFrameLengthInBytes());
    }

    @Override
    public void setup(
            final org.glassfish.grizzly.http.server.NetworkListener networkListener,
            final FilterChainBuilder builder) {
        this.filterChainBuilder = builder;
        super.setup(networkListener, builder);
    }


    @Override
    protected TransportProbe getConfigProbe() {
        return new SpdyNpnConfigProbe(filterChainBuilder);
    }
    
    // ---------------------------------------------------------- Nested Classes

    private final class SpdyNpnConfigProbe extends TransportProbe.Adapter {

        private final FilterChainBuilder filterChainBuilder;

        public SpdyNpnConfigProbe(FilterChainBuilder filterChainBuilder) {
            this.filterChainBuilder = filterChainBuilder;
        }
        
        // ----------------------------------------- Methods from TransportProbe


        @Override
        public void onBeforeStartEvent(Transport transport) {
            final FilterChain dummyTransportFilterChain = filterChainBuilder.build();
            
            FilterChainBuilder builder = FilterChainBuilder.stateless();
            for (int i = 0, len = dummyTransportFilterChain.size(); i < len; i++) {
                builder.add(dummyTransportFilterChain.get(i));
            }
            
            updateFilterChain(SpdyMode.NPN, builder);
            NextProtoNegSupport.getInstance().setServerSideNegotiator(transport,
                    new ProtocolNegotiator(builder.build()));
        }

    } // END SpdyTransportProbe    
}
