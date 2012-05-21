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

import java.util.List;

import org.glassfish.paas.tenantmanager.api.TenantScoped;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Tenant environments place holder.
 * 
 * @author Andriy Zhdanov
 *
 */
@Configured
@TenantScoped
public interface Environments extends ConfigBeanProxy {

    @Element("*")
    List<TenantEnvironment> getEnvironments();

    @Attribute(defaultValue="1")
    String getNextId();
    void setNextId(String id);
    

    /**
     * Get particular environments by type.
     * 
     * @return Service. May be <code>null</code>.
     */
    @DuckTyped
    <T extends TenantEnvironment> T getEnvironmentsByType(Class<T> type);

    @DuckTyped
    Long allocateId();

    class Duck extends Tenant.Extensible {
        public static <T extends TenantEnvironment> List<T> getEnvironmentsByType(Environments environments, Class<T> type) {
            return getExtensionsByType(environments.getEnvironments(), type);
        }

        public static Long allocateId(Environments environments) throws TransactionFailure {
            return (Long) ConfigSupport.apply(new SingleConfigCode<Environments>() {
                @Override
                public Object run(Environments environments) throws TransactionFailure {
                    String idString = environments.getNextId();
                    Long id = Long.valueOf(idString); // can throw exception
                    environments.setNextId("" + (id + 1));
                    return id;
                }
            }, environments);

        }
    }

}
