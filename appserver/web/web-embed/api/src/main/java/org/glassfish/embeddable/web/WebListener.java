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

import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.web.config.WebListenerConfig;

/**
 * Representation of a network listener for web requests.
 *
 * <p/>See {@link WebContainer} for usage example.
 *
 * @author Rajiv Mordani
 * @author Amy Roh
 */

public interface WebListener  {

    /**
     * Sets the id for this <tt>WebListener</tt>.
     *
     * @param id for this <tt>WebListener</tt>
     */
    public void setId(String id);

    /**
     * Gets the id of this <tt>WebListener</tt>.
     *
     * @return id of this <tt>WebListener</tt>
     */
    public String getId();   

    /**
     * Sets the port number for this <tt>WebListener</tt>.
     *
     * @param port the port number for this <tt>WebListener</tt>
     */
    public void setPort(int port);

    /**
     * Gets the port number of this <tt>WebListener</tt>.
     *
     * @return the port number of this <tt>WebListener</tt>
     */
    public int getPort();

    /**
     * Sets the protocol for this <tt>WebListener</tt>.
     *
     * @param protocol the protocol for this <tt>WebListener</tt>
     */
    public void setProtocol(String protocol);

    /**
     * Gets the protocol of this <tt>WebListener</tt>.
     *
     * @return the protocol of this <tt>WebListener</tt>
     */
    public String getProtocol();

    /**
     * Reconfigures this <tt>WebListener</tt> with the given
     * configuration.
     *
     * <p>In order for the given configuration to take effect, this
     * <tt>WebListener</tt> will be stopped and restarted.
     *
     * @param config the configuration to be applied
     *
     * @throws ConfigException if the configuration requires a restart,
     * and this <tt>WebListener</tt> fails to be restarted
     * @throws GlassFishException if an error occurs,
     * and this <tt>WebListener</tt> fails to be restarted
     */
    public void setConfig(WebListenerConfig config)
            throws ConfigException, GlassFishException;

    /**
     * Gets the current configuration of this <tt>WebListener</tt>.
     *
     * @return the current configuration of this <tt>WebListener</tt>,
     * or <tt>null</tt> if no special configuration was ever applied to this
     * <tt>WebListener</tt>
     */
    public WebListenerConfig getConfig();

    /**
     * Sets the <tt>WebContainer</tt> which will be used by this <tt>WebListener</tt>.
     */
    public void setWebContainer(WebContainer webContainer);

    /**
     * Gets the <tt>WebContainer</tt> used by this <tt>WebListener</tt>.
     */
    public WebContainer getWebContainer();

}
