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

package org.glassfish.embeddable.web;

import org.glassfish.embeddable.web.config.WebListenerConfig;
import org.glassfish.embeddable.GlassFishException;

/**
 * Base implementation of the <b>WebListener</b> interface
 *
 * @author Amy Roh
 */

public class WebListenerBase implements WebListener  {

    private WebListenerConfig config;

    private String id;

    private int port;

    private String protocol;

    private WebContainer webContainer;

    public WebListenerBase() {
    }

    public WebListenerBase(String id, int port) {
        this.id = id;
        this.port = port;
    }

    /**
     * Sets the id for this <tt>WebListener</tt>.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the id of this <tt>WebListener</tt>.
     */
    public String getId() {
        return id;
    }

    /**
     * Reconfigures this <tt>WebListener</tt> with the given
     * configuration.
     */
    public void setConfig(WebListenerConfig config)
            throws ConfigException, GlassFishException {
        this.config = config;
        setId(config.getId());
        setPort(config.getPort());
        setProtocol(config.getProtocol());
        if (webContainer != null) {
            webContainer.removeWebListener(this);
            webContainer.addWebListener(this);
        }
    }

    /**
     * Gets the current configuration of this <tt>WebListener</tt>.
     */
    public WebListenerConfig getConfig() {
        return config;
    }

    /**
     * Sets the port number for this <tt>WebListener</tt>.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the port number of this <tt>WebListener</tt>.
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the protocol which will be used by this <tt>WebListener</tt>.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the protocol used by this <tt>WebListener</tt>.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the <tt>WebContainer</tt> which will be used by this <tt>WebListener</tt>.
     */
    public void setWebContainer(WebContainer webContainer) {
        this.webContainer = webContainer;
    }

    /**
     * Gets the <tt>WebContainer</tt> used by this <tt>WebListener</tt>.
     */
    public WebContainer getWebContainer() {
        return webContainer;
    }


}


