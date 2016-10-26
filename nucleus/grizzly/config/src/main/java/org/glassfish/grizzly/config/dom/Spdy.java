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
// Portions Copyright [2016] [Payara Foundation] 
package org.glassfish.grizzly.config.dom;

import javax.validation.constraints.Pattern;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.types.PropertyBag;

@Configured
public interface Spdy extends ConfigBeanProxy, PropertyBag {

    final int MAX_CONCURRENT_STREAMS = 50;
    final int INITIAL_WINDOW_SIZE_IN_BYTES = 64 * 1024;
    int MAX_FRAME_LENGTH_IN_BYTES = 1 << 24;
    final boolean ENABLED = true;
    final String MODE_PATTERN = "(npn|plain)";
    
    // the order defines the versions priority to be chosen during NPN handshake
    final String VERSIONS = "spdy/3.1, spdy/3";

    /**
     * Enables SPDY support.
     */
    @Attribute(defaultValue = "" + ENABLED, dataType = Boolean.class)
    String getEnabled();

    void setEnabled(String enabled);

    /**
     * SPDY mode.
     */
    @Attribute(dataType = String.class)
    @Pattern(regexp = MODE_PATTERN)
    String getMode();

    void setMode(String mode);
    
    /**
     * Configures the number of concurrent streams allowed per SPDY connection.
     * The default is 50.
     */
    @Attribute(defaultValue = "" + MAX_CONCURRENT_STREAMS, dataType = Integer.class)
    String getMaxConcurrentStreams();

    void setMaxConcurrentStreams(String maxConcurrentStreams);

    /**
     * Configures the initial window size in bytes.  The default is 64K.
     */
    @Attribute(defaultValue = "" + INITIAL_WINDOW_SIZE_IN_BYTES, dataType = Integer.class)
    String getInitialWindowSizeInBytes();

    void setInitialWindowSizeInBytes(String initialWindowSizeInBytes);

    /**
     * Configures the maximum length of SPDY frame to be accepted.  The default is 2^24.
     */
    @Attribute(defaultValue = "" + MAX_FRAME_LENGTH_IN_BYTES, dataType = Integer.class)
    String getMaxFrameLengthInBytes();

    void setMaxFrameLengthInBytes(String maxFrameLengthInBytes);
    
    /**
     * Enables SPDY support.
     */
    @Attribute(defaultValue = VERSIONS, dataType = String.class)
    String getVersions();

    void setVersions(String versions);
}
