/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.v3.server.ApplicationLoaderService;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.kernel.KernelLoggerInfo;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;


/**
 * @author kedar
 * @author Ken Paulsen (ken.paulsen@sun.com)
 */
final class InstallerThread extends Thread {

    private final Domain domain;
    private final ServerEnvironmentImpl env;
    private final String contextRoot;
    private final AdminConsoleAdapter adapter;
    private final ServiceLocator habitat;
    private final Logger log = KernelLoggerInfo.getLogger();
    private final List<String> vss;


    /**
     * Constructor.
     */
    InstallerThread(AdminConsoleAdapter adapter, ServiceLocator habitat, Domain domain, ServerEnvironmentImpl env, String contextRoot, List<String> vss) {

        this.adapter = adapter;
        this.habitat = habitat;
        this.domain = domain;
        this.env = env;
        this.contextRoot = contextRoot;
        this.vss = vss;  //defensive copying is not required here
    }

    /**
     *
     */
    @Override
    public void run() {
        try {
            // The following are the basic steps which are required to get the
            // Admin Console web application running.  Each step ensures that
            // it has not already been completed and adjusts the state message
            // accordingly.
            install();
            load();

            // From within this Thread mark the installation process complete
            adapter.setInstalling(false);
        } catch (Exception ex) {
            adapter.setInstalling(false);
            adapter.setStateMsg(AdapterState.APPLICATION_NOT_INSTALLED);
            log.log(Level.INFO, KernelLoggerInfo.adminGuiInstallProblem, ex);
        }
    }

   
    /**
     * <p> Install the admingui.war file.</p>
     */
    private void install() throws Exception {
        if (domain.getSystemApplicationReferencedFrom(env.getInstanceName(), AdminConsoleAdapter.ADMIN_APP_NAME) != null) {
            // Application is already installed
            adapter.setStateMsg(AdapterState.APPLICATION_INSTALLED_BUT_NOT_LOADED);
            return;
        }

        // Set the adapter state
        adapter.setStateMsg(AdapterState.INSTALLING);
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Installing the Admin Console Application...");
        }

        //create the application entry in domain.xml
        ConfigCode code = new ConfigCode() {
            @Override
            public Object run(ConfigBeanProxy... proxies) throws PropertyVetoException, TransactionFailure {
                SystemApplications sa = (SystemApplications) proxies[0];
                Application app = sa.createChild(Application.class);
                sa.getModules().add(app);
                app.setName(AdminConsoleAdapter.ADMIN_APP_NAME);
                app.setEnabled(Boolean.TRUE.toString());
                app.setObjectType("system-admin"); //TODO
                app.setDirectoryDeployed("true");
                app.setContextRoot(contextRoot);
                try {
                    app.setLocation("${com.sun.aas.installRootURI}/lib/install/applications/" + AdminConsoleAdapter.ADMIN_APP_NAME);
                } catch (Exception me) {
                    // can't do anything
                    throw new RuntimeException(me);
                }
                Module singleModule = app.createChild(Module.class);
                app.getModule().add(singleModule);
                singleModule.setName(app.getName());
                Engine webe = singleModule.createChild(Engine.class);
                webe.setSniffer("web");
                Engine sece = singleModule.createChild(Engine.class);
                sece.setSniffer("security");
                singleModule.getEngines().add(webe);
                singleModule.getEngines().add(sece);
                Server s = (Server) proxies[1];
                List<ApplicationRef> arefs = s.getApplicationRef();
                ApplicationRef aref = s.createChild(ApplicationRef.class);
                aref.setRef(app.getName());
                aref.setEnabled(Boolean.TRUE.toString());
                aref.setVirtualServers(getVirtualServerList()); //TODO
                arefs.add(aref);
                return true;
            }
        };
        Server server = domain.getServerNamed(env.getInstanceName());
        ConfigSupport.apply(code, domain.getSystemApplications(), server);

        // Set the adapter state
        adapter.setStateMsg(AdapterState.APPLICATION_INSTALLED_BUT_NOT_LOADED);
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Admin Console Application Installed.");
        }
    }

    /**
     *
     */
    private String getVirtualServerList() {
        if (vss == null)
            return "";
        String s = Arrays.toString(vss.toArray(new String[vss.size()]));
        //standard JDK implemetation always returns this enclosed in [], remove them
        s = s.substring(1, s.length() - 1);
        return (s);
    }

    /**
     * <p> Load the Admin Console web application.</p>
     */
    private void load() {
        ApplicationRegistry appRegistry = habitat.<ApplicationRegistry>getService(ApplicationRegistry.class);
        ApplicationInfo appInfo = appRegistry.get(AdminConsoleAdapter.ADMIN_APP_NAME);
        if (appInfo != null && appInfo.isLoaded()) {
            // Application is already loaded
            adapter.setStateMsg(AdapterState.APPLICATION_LOADED);
            return;
        }

        // hook for Jerome
        Application config = adapter.getConfig();
        if (config == null) {
            throw new IllegalStateException("Admin Console application has no system app entry!");
        }
        // Set adapter state
        adapter.setStateMsg(AdapterState.APPLICATION_LOADING);

        // Load the Admin Console Application
        String sn = env.getInstanceName();
// FIXME: An exception may not be thrown... check for errors!
        ApplicationRef ref = domain.getApplicationRefInServer(sn, AdminConsoleAdapter.ADMIN_APP_NAME);
        habitat.<ApplicationLoaderService>getService(ApplicationLoaderService.class).processApplication(config, ref);

        // Set adapter state
        adapter.setStateMsg(AdapterState.APPLICATION_LOADED);
    }

}
