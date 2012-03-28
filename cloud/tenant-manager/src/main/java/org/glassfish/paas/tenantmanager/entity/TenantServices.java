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
package org.glassfish.paas.tenantmanager.entity;

import java.util.ArrayList;
import java.util.List;

import org.glassfish.paas.tenantmanager.api.TenantScoped;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

/**
 * Tenant services.
 * 
 * @author Andriy Zhdanov
 * 
 */
@Configured
@TenantScoped
public interface TenantServices extends ConfigBeanProxy {
    /**
     * All tenant services.
     * 
     * @return Services.
     */
    @Element("*")
    List<TenantService> getTenantServices();

    /**
     * DefaultService types for a tenant. DefaultService is mapping to default a
     * service would take if the deployer does not specify which one to use. For
     * example a tenant can configure that whenever database service is needed
     * by an app and the app has not specified any mapping to database, the
     * default is going to be pointing to a shared database running somewhere.
     * 
     * 
     * 
     * @return Services.
     */
    @DuckTyped
    List<DefaultService> getDefaultServices();

    /**
     * Services shared by various environments of a tenant. SharedServices These
     * are services that are shared by various environments of a tenant. It's 
     * not yet clear how these services will be deployed and would they always
     * be ExternalService or not.
     * 
     * @return Services.
     */
    @DuckTyped
    List<SharedService> getSharedServices();

    /**
     * Services provisioned externally. ExternalServices refers to services that
     * are not provisioned by BG. For example a physical database installation
     * that is shared enterprise wise if BG is use on premise.
     * 
     * @return Services.
     */
    @DuckTyped
    List<ExternalService> getExternalServices();

    class Duck {
        public static List<DefaultService> getDefaultServices(TenantServices tenantServices) {
            return getServices(tenantServices, DefaultService.class);
        }

        public static List<SharedService> getSharedServices(TenantServices tenantServices) {
            return getServices(tenantServices, SharedService.class);
        }

        public static List<ExternalService> getExternalServices(TenantServices tenantServices) {
            return getServices(tenantServices, ExternalService.class);
        }

        private static <T extends TenantService> List<T> getServices(TenantServices tenantServices, Class<T> type) {
            List<T> services = new ArrayList<T>();
            for (TenantService service : tenantServices.getTenantServices()) {
                if (type.isInstance(service)) {
                    services.add(type.cast(service));
                }
            }
            return services;
            
        }
    }
    
}
