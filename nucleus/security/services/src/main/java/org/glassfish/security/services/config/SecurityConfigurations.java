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
package org.glassfish.security.services.config;

import com.sun.enterprise.config.serverbeans.DomainExtension;
import com.sun.enterprise.config.modularity.annotation.HasNoDefaultConfiguration;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The top level security configuration which holds the list of configured security services.
 */
@Configured
@HasNoDefaultConfiguration
public interface SecurityConfigurations extends ConfigBeanProxy, DomainExtension {
    /**
     * Gets the list of configured security services.
     */
    @Element("*")
    List<SecurityConfiguration> getSecurityServices();

    /**
     * Gets the list of configured security services by security service type.
     */
    @DuckTyped
    <T extends SecurityConfiguration> List<T> getSecurityServicesByType(Class<T> type);

    /**
     * Gets the default configured security service by security service type.
     */
    @DuckTyped
    <T extends SecurityConfiguration> T getDefaultSecurityServiceByType(Class<T> type);

    /**
     * Gets a named security service configuration by specific security type.
     */
    @DuckTyped
    <T extends SecurityConfiguration> T getSecurityServiceByName(String name, Class<T> type);

    /**
     * Gets a named security service configuration.
     */
    @DuckTyped
    SecurityConfiguration getSecurityServiceByName(String name);

    class Duck {
        /**
         * Gets the list of configured security services by security service type.
         */
        public static <T extends SecurityConfiguration> List<T> getSecurityServicesByType(SecurityConfigurations services, Class<T> type) {
            List<T> typedServices = new ArrayList<T>();
            for (SecurityConfiguration securityServiceConfiguration : services.getSecurityServices()) {
                try {
                    if (type.isAssignableFrom(securityServiceConfiguration.getClass())) {
                        typedServices.add(type.cast(securityServiceConfiguration));
                    }
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
			return Collections.unmodifiableList(typedServices);
        }

        /**
         * Gets the default configured security service by security service type.
         */
    	public static <T extends SecurityConfiguration> T getDefaultSecurityServiceByType(SecurityConfigurations services, Class<T> type) {
            for (SecurityConfiguration securityServiceConfiguration : services.getSecurityServices()) {
                try {
                    if (securityServiceConfiguration.getDefault()) {
                        return type.cast(securityServiceConfiguration);
                    }
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            return null;
        }

        /**
         * Gets a named security service configuration by specific security type.
         */
    	public static <T extends SecurityConfiguration> T getSecurityServiceByName(SecurityConfigurations services, String name, Class<T> type) {
            for (SecurityConfiguration securityServiceConfiguration : services.getSecurityServices()) {
                try {
                    if (securityServiceConfiguration.getName().equals(name)) {
                        return type.cast(securityServiceConfiguration);
                    }
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            return null;
        }

        /**
         * Gets a named security service configuration.
         */
    	public static SecurityConfiguration getSecurityServiceByName(SecurityConfigurations services, String name) {
            for (SecurityConfiguration securityServiceConfiguration : services.getSecurityServices()) {
                if (securityServiceConfiguration.getName().equals(name)) {
                    return securityServiceConfiguration;
                }
            }
            return null;
        }
    }
}
