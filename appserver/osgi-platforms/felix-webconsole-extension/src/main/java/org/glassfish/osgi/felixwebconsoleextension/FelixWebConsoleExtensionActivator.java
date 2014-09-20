/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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


package org.glassfish.osgi.felixwebconsoleextension;

import org.apache.felix.webconsole.BrandingPlugin;
import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This activator servers following purposes:
 * a) Registers a BrandingPlugin service to customize the look and feel.
 * See http://felix.apache.org/site/branding-the-web-console.html for more details.
 * b) Registers configuration object to select the right HttpService.
 * c) Registers a SecurityProvider to integrate with GlassFish security service.
 *
 * @author sanjeeb.sahoo@oracle.com
 * @author tangyong@cn.fujitsu.com
 */
public class FelixWebConsoleExtensionActivator implements BundleActivator {
	
    private Logger logger = Logger.getLogger(getClass().getPackage().getName());
    private BundleContext context;
    private static final String WEBCONSOLE_PID = "org.apache.felix.webconsole.internal.servlet.OsgiManager";
    private static final String PROP_HTTP_SERVICE_SELECTOR = "http.service.filter";
    private static final String PROP_REALM = "realm";
    private static final String REALM="GlassFish Server";
    private static final String HTTP_SERVICE_SELECTOR = "VirtualServer=server"; // We bind to default virtual host
    private ServiceTracker tracker;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        registerBrandingPlugin();
        configureConsole();
        registerWebConsoleSecurityProvider(); // GLASSFISH-12975
    }

    private void registerWebConsoleSecurityProvider() {   	   	 
    	 final GlassFishSecurityProvider secprovider = new GlassFishSecurityProvider();
    	 secprovider.setBundleContext(context);
         ServiceRegistration reg = context.registerService(WebConsoleSecurityProvider.class.getName(), secprovider, null);
         logger.logp(Level.INFO, "FelixWebConsoleExtensionActivator", "start", "Registered {0}", new Object[]{secprovider});
	}

	private void configureConsole() {
        tracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                try {
                    ConfigurationAdmin ca = ConfigurationAdmin.class.cast(context.getService(reference));
                    org.osgi.service.cm.Configuration config = null;
                    config = ca.getConfiguration(WEBCONSOLE_PID, null);
                    Dictionary old = config.getProperties();
                    Dictionary newProps = new Hashtable();
                    newProps.put(PROP_HTTP_SERVICE_SELECTOR, HTTP_SERVICE_SELECTOR);
                    newProps.put(PROP_REALM, REALM);
                    if (old != null) {
                        old.remove( Constants.SERVICE_PID );
                    }

                    if( !newProps.equals( old ) )
                    {
                        if (config.getBundleLocation() != null)
                        {
                            config.setBundleLocation(null);
                        }
                        config.update(newProps);
                    }
                } catch (IOException e) {
                    logger.logp(Level.INFO, "FelixWebConsoleExtensionActivator", "addingService",
                            "Failed to update webconsole configuration", e);
                }
                return null;
            }
        };
        tracker.open();
    }

    private void registerBrandingPlugin() {
        final GlassFishBrandingPlugin service = new GlassFishBrandingPlugin();
        ServiceRegistration reg = context.registerService(BrandingPlugin.class.getName(), service, null);
        logger.logp(Level.INFO, "FelixWebConsoleExtensionActivator", "start", "Registered {0}", new Object[]{service});
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (tracker != null) {
            tracker.close();
        }
    }
}
