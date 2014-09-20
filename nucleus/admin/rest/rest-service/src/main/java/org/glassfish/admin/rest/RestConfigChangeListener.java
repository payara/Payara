/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest;

import java.beans.PropertyChangeEvent;
import org.glassfish.admin.rest.adapter.Reloader;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ObservableBean;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * This test listen to a property change event only injecting the parent containing the property.
 *
 * @author Ludovic Champenois
 */
// TODO: Delete this once we're sure it's no longer needed
@Deprecated
public class RestConfigChangeListener implements ConfigListener {
//    private Reloader r;
    private ServerContext sc;

    public RestConfigChangeListener(ServiceLocator habitat, Reloader reload, ResourceConfig rc, ServerContext sc) {
//        this.r = reload;
        this.sc = sc;


        RestConfig target = ResourceUtil.getRestConfig(habitat);

        if (target != null) {
            ((ObservableBean) ConfigSupport.getImpl(target)).addListener(this);
        }

        /// ((ObservableBean) ConfigSupport.getImpl(target)).removeListener(this);
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader apiClassLoader = sc.getCommonClassLoader();
            Thread.currentThread().setContextClassLoader(apiClassLoader);

            // TODO - JERSEY2
//            rc.getContainerResponseFilters().clear();
//            rc.getContainerRequestFilters().clear();
//            rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.FALSE);

//            RestConfig restConf = ResourceUtil.getRestConfig(habitat);
//            if (restConf != null) {
//                if (restConf.getLogOutput().equalsIgnoreCase("true")) { //enable output logging
//                    rc.getContainerResponseFilters().add(LoggingFilter.class);
//                }
//                if (restConf.getLogInput().equalsIgnoreCase("true")) { //enable input logging
//                    rc.getContainerRequestFilters().add(LoggingFilter.class);
//                }
//                if (restConf.getWadlGeneration().equalsIgnoreCase("false")) { //disable WADL
//                    rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
//                }
//            }

//            r.reload();
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
        return null;
    }
}
