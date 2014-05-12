/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;
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
import org.glassfish.grizzly.spdy.SpdyVersion;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name="spdy")
@ContractsProvided({SpdyAddOnProvider.class, AddOn.class})
public class SpdyAddOnProvider implements AddOn, ConfigAwareElement<Spdy> {
    /**
     * The logger to use for logging messages.
     */
    private static final Logger LOGGER =
            Grizzly.logger(SpdyAddOnProvider.class);

    private GlassFishSpdyAddOn spdyAddOn;
    
    public SpdyAddOnProvider() {
    }

    @Override
    public void configure(final ServiceLocator habitat,
            final NetworkListener networkListener, final Spdy spdy) {
        
        final SpdyMode spdyMode = (spdy.getMode() == null ||
                "npn".equalsIgnoreCase(spdy.getMode())) ?
                SpdyMode.NPN :
                SpdyMode.PLAIN;

        SpdyVersion[] spdyVersions = null;
        final String strVersions = spdy.getVersions();
        if (strVersions != null) {
            final String[] strVersionsArray = strVersions.split(",");
            final List<SpdyVersion> tmpList =
                    new ArrayList<SpdyVersion>(strVersionsArray.length);
            for (String strVersion : strVersionsArray) {
                
                final String trimmedVersion = strVersion.trim();
                final SpdyVersion spdyVersion =
                        SpdyVersion.fromString(trimmedVersion);
                if (spdyVersion != null) {
                    tmpList.add(spdyVersion);
                } else {
                    LOGGER.log(Level.WARNING, "Spdy version {0} is not supported. Supported versions are: {1}",
                            new Object[]{trimmedVersion, Arrays.toString(SpdyVersion.values())});
                }
            }
            
            if (!tmpList.isEmpty()) {
                spdyVersions = tmpList.toArray(new SpdyVersion[tmpList.size()]);
            }
        }

        if (spdyVersions == null || spdyVersions.length == 0) {
            spdyVersions = GlassFishSpdyAddOn.getAllVersions();
        }
        
        spdyAddOn = new GlassFishSpdyAddOn(spdyMode, spdyVersions);
        
        spdyAddOn.setInitialWindowSize(spdy.getInitialWindowSizeInBytes());
        spdyAddOn.setMaxConcurrentStreams(spdy.getMaxConcurrentStreams());
        spdyAddOn.setMaxFrameLength(spdy.getMaxFrameLengthInBytes());
    }

    @Override
    public void setup(
            final org.glassfish.grizzly.http.server.NetworkListener networkListener,
            final FilterChainBuilder builder) {
        spdyAddOn.setup(networkListener, builder);
    }

    private static class GlassFishSpdyAddOn extends SpdyAddOn {
        private FilterChainBuilder filterChainBuilder;

        public GlassFishSpdyAddOn(final SpdyMode mode,
                final SpdyVersion... supportedSpdyVersions) {
            super(mode, supportedSpdyVersions);
        }

        @Override
        public void setup(
                final org.glassfish.grizzly.http.server.NetworkListener networkListener,
                FilterChainBuilder builder) {
            filterChainBuilder = builder;
            super.setup(networkListener, builder);
        }
        
        
        @Override
        protected TransportProbe getConfigProbe() {
            return new SpdyNpnConfigProbe(filterChainBuilder);
        }

        private static SpdyVersion[] getAllVersions() {
            return ALL_SPDY_VERSIONS;
        }
        // ---------------------------------------------------------- Nested Classes

        private final class SpdyNpnConfigProbe extends TransportProbe.Adapter {

            private final FilterChainBuilder filterChainBuilder;

            public SpdyNpnConfigProbe(FilterChainBuilder filterChainBuilder) {
                this.filterChainBuilder = filterChainBuilder;
            }

            // ----------------------------------------- Methods from TransportProbe


            @Override
            public void onBeforeStartEvent(final Transport transport) {
                final FilterChain dummyTransportFilterChain = filterChainBuilder.build();

                FilterChainBuilder builder = FilterChainBuilder.stateless();
                for (int i = 0, len = dummyTransportFilterChain.size(); i < len; i++) {
                    builder.add(dummyTransportFilterChain.get(i));
                }

                updateFilterChain(SpdyMode.NPN, builder);
                NextProtoNegSupport.getInstance().setServerSideNegotiator(transport,
                        new ProtocolNegotiator(builder.build(), supportedSpdyVersions));
            }

        } // END SpdyTransportProbe
    }
}
