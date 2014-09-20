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


import java.io.File;
import java.net.URL;

/**
 * Class that is used for configuring WebContainer instances.
 *
 * <p/> Usage example:
 * <pre>
 *      // Create and start Glassfish
 *      GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish();
 *      glassfish.start();
 *
 *      // Access WebContainer
 *      WebContainer container = glassfish.getService(WebContainer.class);
 *
 *      WebContainerConfig config = new WebContainerConfig();
 *      config.setListings(true);
 *      config.setPort(9090);
 *      config.setHostNames("localhost");
 *      container.setConfiguration(config);
 * </pre>
 *
 * @see org.glassfish.embeddable.web.WebContainer
 *
 */
public class WebContainerConfig {


    private URL defaultWebXml;
    private File docRoot;
    private String hostNames = "${com.sun.aas.hostName}";
    private String  listenerName = "embedded-listener";
    private boolean listings = false;
    private int port = 8080;    
    private String virtualServerId = "server";

    /**
     * Sets the default web xml
     *
     * @param url the url of the default web xml
     */
    public void setDefaultWebXml(URL url) {
        defaultWebXml = url;
    }

    /**
     * Gets the default web xml
     * (default: <i>org/glassfish/web/embed/default-web.xml</i>).
     */
    public URL getDefaultWebXml() {
        if (defaultWebXml == null) {
            defaultWebXml = getClass().getClassLoader().getResource("org/glassfish/web/embed/default-web.xml");
        }
        return defaultWebXml;
    }

    /**
     * Sets the docroot directory
     *
     * @param f the docroot directory
     */
    public void setDocRootDir(File f) {
        docRoot = f;
    }

    /**
     * Gets the docroot directory
     */
    public File getDocRootDir() {
        // TODO: Need to get the docroot from the top level API somehow
        return docRoot;
    }

    /**
     * Sets the host names of the default <tt>VirtualServer</tt>
     * (default: <i>localhost</i>).
     *
     * @param hostNames the host names of the default <tt>VirtualServer</tt> seprated by commas.
     */
    public void setHostNames(String hostNames) {
        this.hostNames = hostNames;
    }

    /**
     * Gets the host names of the default <tt>VirtualServer</tt>
     * (default: <i>localhost</i>).
     *
     * @return the host names of the default <tt>VirtualServer</tt>
     */
    public String getHostNames() {
        return hostNames;
    }

    /**
     * Sets the default listener name
     *
     * @param name the name of the default listener
     */
    public void setListenerName(String name) {
        listenerName = name;
    }

    /**
     * Gets the default listener name
     */
    public String getListenerName() {
        return listenerName;
    }

    /**
     * Enables or disables directory listings
     *
     * @param directoryListing true if directory listings are to be
     * enabled, false otherwise
     */
    public void setListings(boolean directoryListing) {
        listings = directoryListing;
    }

    /**
     * Return if directory listings is enabled
     */
    public boolean getListings() {
        return listings;
    }

    /**
     * Sets the port of the default <tt>WebListener</tt> (default: 8080).
     *
     * @param port the port of the default <tt>WebListener</tt>
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the port of the default <tt>WebListener</tt> (default: 8080).
     *
     * @return the port of the default <tt>WebListener</tt>
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the id of the default <tt>VirtualServer</tt>
     * (default: <i>server</i>).
     *
     * @param virtualServerId the id of the default <tt>VirtualServer</tt>
     */
    public void setVirtualServerId(String virtualServerId) {
        this.virtualServerId = virtualServerId;
    }

    /**
     * Gets the id of the default <tt>VirtualServer</tt>
     * (default: <i>server</i>).
     *
     * @return the id of the default <tt>VirtualServer</tt>
     */
    public String getVirtualServerId() {
        return virtualServerId;
    }

}
