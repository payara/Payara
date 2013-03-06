/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.jsf;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.web.WebModule;
import com.sun.faces.spi.FacesConfigResourceProvider;
import com.sun.logging.LogDomains;
import java.net.URI;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.cdi.CDILoggerInfo;
import org.glassfish.weld.WeldApplicationContainer;
import org.glassfish.weld.WeldDeployer;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * This provider returns the Web Beans faces-config.xml to the JSF runtime.
 * It will only return the configuraion file for Web Beans deployments.
 */  
public class WeldFacesConfigProvider implements FacesConfigResourceProvider {

    private static final String HABITAT_ATTRIBUTE =
            "org.glassfish.servlet.habitat";
    private InvocationManager invokeMgr;

    private Logger logger = Logger.getLogger(WeldFacesConfigProvider.class.getName());

    private static final String SERVICES_FACES_CONFIG = "META-INF/services/faces-config.xml";

    public Collection<URI> getResources(ServletContext context) {

        ServiceLocator defaultServices = (ServiceLocator)context.getAttribute(
                HABITAT_ATTRIBUTE);
        invokeMgr = defaultServices.getService(InvocationManager.class);
        ComponentInvocation inv = invokeMgr.getCurrentInvocation();
        WebModule webModule = (WebModule)inv.getContainer();
        WebBundleDescriptor wdesc = webModule.getWebBundleDescriptor();

        List<URI> list = new ArrayList<URI>(1);

        if (!wdesc.hasExtensionProperty(WeldDeployer.WELD_EXTENSION)) {
            return list;
        }

        // Don't use Util.getCurrentLoader().  This config resource should
        // be available from the same classloader that loaded this instance.
        // Doing so allows us to be more OSGi friendly.
        ClassLoader loader = this.getClass().getClassLoader();
        URL resource = loader.getResource(SERVICES_FACES_CONFIG);
        if (resource != null) {
            try {
                list.add(resource.toURI());
            } catch (URISyntaxException ex) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE,
                               CDILoggerInfo.SEVERE_ERROR_CREATING_URI_FOR_FACES_CONFIG_XML,
                               new Object [] {resource.toExternalForm(), ex});
                }
            }
        }
        return list;
    }

}
