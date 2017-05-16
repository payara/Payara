/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.adapter;

import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.v3.admin.AdminAdapter;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.jvnet.hk2.config.types.Property;
import org.glassfish.server.ServerEnvironmentImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.kernel.KernelLoggerInfo;

/** Makes various decisions about the admin adapters.
 *
 * @author &#2325;&#2375;&#2342;&#2366;&#2352; (km@dev.java.net)
 * @since GlassFish V3 (March 2008)
 */
public final class AdminEndpointDecider {

    private String asadminContextRoot;
    private String guiContextRoot;
    private List<String> asadminHosts; //list of virtual servers for asadmin
    private List<String> guiHosts;     //list of virtual servers for admin GUI
    
    private int port;  // both asadmin and admin GUI are on same port
    private InetAddress address;
    private int maxThreadPoolSize = 5;
    private Config cfg;
    private Logger log = KernelLoggerInfo.getLogger();
    
    public static final int ADMIN_PORT           = 4848;
    
    public AdminEndpointDecider(Config cfg) {
        if (cfg == null || log == null)
            throw new IllegalArgumentException("config or logger can't be null");
        this.cfg = cfg;
        setValues();
    }
    
    public int getListenPort() {
        return port;
    }

    public InetAddress getListenAddress() {
        return address;
    }

    public int getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public List<String> getAsadminHosts() {
        return asadminHosts;
    }
    
    public List<String> getGuiHosts() {
        return guiHosts;
    }
    
    public String getAsadminContextRoot() {
        return asadminContextRoot;
    }
    
    public String getGuiContextRoot() {
        return guiContextRoot;
    }
    
    private void setValues() {
        asadminContextRoot = AdminAdapter.PREFIX_URI;  //can't change
        //asadminHosts       = Collections.emptyList();  //asadmin is handled completely by the adapter, no VS needed
        NetworkListener nl = cfg.getAdminListener();
        ThreadPool tp = nl.findThreadPool();
        if (tp != null) {
            try {
                maxThreadPoolSize = Integer.parseInt(tp.getMaxThreadPoolSize());
            } catch (NumberFormatException ne) {
            }
        }
        String dvs     = nl.findHttpProtocol().getHttp().getDefaultVirtualServer();
        guiHosts       = Collections.unmodifiableList(Arrays.asList(dvs));
        asadminHosts   = guiHosts;  //same for now
        try {
            address = InetAddress.getByName(nl.getAddress());
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
        if (ServerTags.ADMIN_LISTENER_ID.equals(nl.getName())) {
            guiContextRoot = "";  //at the root context for separate admin-listener
            try {
                port = Integer.parseInt(nl.getPort());
            } catch(NumberFormatException ne) {
                port = ADMIN_PORT;
            }
        }
        else {
            try {
                port = Integer.parseInt(nl.getPort());
            } catch(NumberFormatException ne) {
                port = 8080;   // this is the last resort
            }
            //get the context root from admin-service
            AdminService as = cfg.getAdminService();
            if (as == null)
                guiContextRoot = ServerEnvironmentImpl.DEFAULT_ADMIN_CONSOLE_CONTEXT_ROOT;
            else
                setGuiContextRootFromAdminService(as);
        }
    }
    
    private void setGuiContextRootFromAdminService(AdminService as) {
        for (Property p : as.getProperty()) {
            setGuiContextRoot(p);
        }
    }
    private void setGuiContextRoot(Property prop) {
	if (prop == null) {
	    guiContextRoot = ServerEnvironmentImpl.DEFAULT_ADMIN_CONSOLE_CONTEXT_ROOT;
	    return;
	}
	if (ServerTags.ADMIN_CONSOLE_CONTEXT_ROOT.equals(prop.getName())) {
	    if (prop.getValue() != null && prop.getValue().startsWith("/")) {
		guiContextRoot = prop.getValue();
                log.log(Level.INFO, KernelLoggerInfo.contextRoot, guiContextRoot);
	    } else {
		log.log(Level.INFO, KernelLoggerInfo.invalidContextRoot, ServerEnvironmentImpl.DEFAULT_ADMIN_CONSOLE_CONTEXT_ROOT);
		guiContextRoot = ServerEnvironmentImpl.DEFAULT_ADMIN_CONSOLE_CONTEXT_ROOT;
	    }
	}
    }    
}
