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

package org.glassfish.elasticity.config.serverbeans;

import com.sun.enterprise.config.serverbeans.*;
import java.util.List;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;
import java.beans.PropertyVetoException;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/17/11
 */
@Configured
@Singleton
public interface ElasticServices extends DomainExtension {
      /**
     * Return the list of currently configured elastic service. Services can
     * be added or removed by using the returned {@link java.util.List}
     * instance
     *
     * @return the list of configured {@link ElasticService}
     */
    @Element ("elasticservice")
    @Create(value = "_create-elastic-service", decorator=ElasticService.Decorator.class, resolver = ESResolver.class, i18n = @I18n("org.glassfish.elasticity.config.create-elastic-service"))
    public List<ElasticService> getElasticService();

    /**
     * Return the elastic service with the given name, or null if no such elastic service exists.
     *
     * @param   name    the name of the elastic service
     * @return          the Elastic Service object, or null if no such elastic service
     */
    @DuckTyped
    public ElasticService getElasticService(String name);

    class Duck {
        public static ElasticService getElasticService(ElasticServices instance, String name) {
            for (ElasticService service : instance.getElasticService()) {
                if (service.getName().equals(name)) {
                    return service;
                }
            }
            return null;
        }

    }

    @Service
    public class ESResolver implements CrudResolver {
        @Inject
        Domain domain;

        @Inject(optional = true)
        ElasticServices elasticServices = null;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type)  {
            if (elasticServices!=null) return (T) elasticServices;
            try {
                elasticServices = (ElasticServices) ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    @Override
                    public Object run(Domain wDomain) throws PropertyVetoException, TransactionFailure {
                        ElasticServices es = wDomain.createChild(ElasticServices.class);
                        wDomain.getExtensions().add(es);
                        return es;
                    }
                }, domain);
            } catch (TransactionFailure t)  {
                throw new RuntimeException(t);
            }
            return (T) elasticServices;
        }
    }

}
