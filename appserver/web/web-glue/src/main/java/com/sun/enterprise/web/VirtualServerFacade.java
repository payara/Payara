/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.web;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;

import com.sun.enterprise.config.serverbeans.Applications;
import org.apache.catalina.core.*;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.web.ConfigException;
import org.glassfish.embeddable.web.Context;
import org.glassfish.embeddable.web.WebListener;
import org.glassfish.embeddable.web.config.SecurityConfig;
import org.glassfish.embeddable.web.config.VirtualServerConfig;
import org.glassfish.internal.api.Globals;

/**
 * Facade object which masks the internal <code>VirtualServer</code>
 * object from the web application.
 *
 * @author Amy Roh
 */
public class VirtualServerFacade implements org.glassfish.embeddable.web.VirtualServer {

        
    // ----------------------------------------------------------- Constructors


    public VirtualServerFacade(String id, File docRoot, WebListener...  webListeners) {
        this.id = id;
        this.docRoot = docRoot;
        if (webListeners != null) {
            this.webListeners = Arrays.asList(webListeners);
        }
    }


    // ----------------------------------------------------- Instance Variables


    private VirtualServerConfig config;

    private File docRoot;

    private String id;

    /**
     * Wrapped web module.
     */
    private VirtualServer vs = null;

    private List<WebListener> webListeners = null;

    // ----------------------------------------------------- embedded methods


    /**
     * Sets the docroot of this <tt>VirtualServer</tt>.
     *
     * @param docRoot the docroot of this <tt>VirtualServer</tt>.
     */
    public void setDocRoot(File docRoot) {
        this.docRoot = docRoot;
        if (vs != null) {
            vs.setDocRoot(docRoot);
        }
    }

    /**
     * Gets the docroot of this <tt>VirtualServer</tt>.
     */
    public File getDocRoot() {
        return docRoot;
    }

    /**
     * Return the virtual server identifier.
     */
    public String getID() {
        return id;
    }

    /**
     * Set the virtual server identifier string.
     *
     * @param id New identifier for this virtual server
     */
    public void setID(String id) {
        this.id = id;
        if (vs != null) {
            vs.setID(id);
        }
    }

    /**
     * Sets the collection of <tt>WebListener</tt> instances from which
     * this <tt>VirtualServer</tt> receives requests.
     *
     * @param webListeners the collection of <tt>WebListener</tt> instances from which
     * this <tt>VirtualServer</tt> receives requests.
     */
    public void setWebListeners(WebListener...  webListeners) {
        if (webListeners != null) {
            this.webListeners = Arrays.asList(webListeners);
        }
    }

    /**
     * Gets the collection of <tt>WebListener</tt> instances from which
     * this <tt>VirtualServer</tt> receives requests.
     *
     * @return the collection of <tt>WebListener</tt> instances from which
     * this <tt>VirtualServer</tt> receives requests.
     */
    public Collection<WebListener> getWebListeners() {
        return webListeners;
    }

    /**
     * Registers the given <tt>Context</tt> with this <tt>VirtualServer</tt>
     * at the given context root.
     *
     * <p>If this <tt>VirtualServer</tt> has already been started, the
     * given <tt>context</tt> will be started as well.
     */
    public void addContext(Context context, String contextRoot)
        throws ConfigException, GlassFishException {
        if (vs != null) {
            vs.addContext(context, contextRoot);
        } else {
            throw new GlassFishException("Virtual server "+id+" has not been added");
        }
    }

    /**
     * Stops the given <tt>context</tt> and removes it from this
     * <tt>VirtualServer</tt>.
     */
    public void removeContext(Context context)
            throws GlassFishException {
        if (vs != null) {
            vs.removeContext(context);
        } else {
            throw new GlassFishException("Virtual server "+id+" has not been added");
        }
    }

    /**
     * Finds the <tt>Context</tt> registered at the given context root.
     */
    public Context getContext(String contextRoot) {
        if (vs != null) {
            return vs.getContext(contextRoot);
        } else {
            return null;
        }
    }

    /**
     * Gets the collection of <tt>Context</tt> instances registered with
     * this <tt>VirtualServer</tt>.
     */
    public Collection<Context> getContexts() {
        if (vs != null) {
            return vs.getContexts();
        } else {
            return null;
        }
    }

    /**
     * Reconfigures this <tt>VirtualServer</tt> with the given
     * configuration.
     *
     * <p>In order for the given configuration to take effect, this
     * <tt>VirtualServer</tt> may be stopped and restarted.
     */
    public void setConfig(VirtualServerConfig config)
        throws ConfigException {
        this.config = config;
        if (vs != null) {
            vs.setConfig(config);
        }
    }

    /**
     * Gets the current configuration of this <tt>VirtualServer</tt>.
     */
    public VirtualServerConfig getConfig() {
        return config;
    }

    public void setVirtualServer(VirtualServer vs) {
        this.vs = vs;
    }

    public VirtualServer getVirtualServer() {
        return vs;
    }

}
