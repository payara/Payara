/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.embeddable.web.config;

import org.glassfish.embeddable.web.WebListener;

/**
 * Class that is used for configuring WebListener instances.
 *
 * @see org.glassfish.embeddable.web.WebListener
 */
public class WebListenerConfig {

    private String id;

    private int port;

    private String protocol;

    private boolean traceEnabled;

    public WebListenerConfig(String id, int port) {
        this.id = id;
        this.port = port;
    }

    public WebListenerConfig(String id, int port, String protocol) {
        this.id = id;
        this.port = port;
        this.protocol = protocol;
    }

    /**
     * Sets the id used for configuring <tt>WebListener</tt>.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the id used for configuring <tt>WebListener</tt>.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the port number used for configuring <tt>WebListener</tt>.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the port number used for configuring <tt>WebListener</tt>.
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the protocol used for configuring <tt>WebListener</tt>.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the protocol used for configuring <tt>WebListener</tt>.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Enables or disables support for TRACE requests.
     *
     * @param traceEnabled true if support for TRACE requests is to be
     * enabled, false otherwise
     */
    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    /**
     * Checks if support for TRACE requests is enabled.
     *
     * @return true if support for TRACE requests is enabled, false otherwise
     */
    public boolean isTraceEnabled() {
        return traceEnabled;
    }

}
