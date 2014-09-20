/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;


import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Cluster;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;

import javax.inject.Inject;
import java.util.Collection;


/**
 * Resolver based on a supplied target parameter (with a possible default
 * value).
 *
 * @author Jerome Dochez
 */
@Service
public class TargetBasedResolver implements CrudResolver {

    @Param(defaultValue = "server", optional=true)
    String target="server";

    @Inject
    ServiceLocator habitat;
    
    @Override
    public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type) {
        try {
            ConfigBeanProxy proxy = getTarget(Config.class, type);
            if (proxy==null) {
                proxy=getTarget(Cluster.class, type);
            }
            if (proxy==null) {
                proxy=getTarget(Server.class, type);
            }
            return type.cast(proxy);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    private <T extends ConfigBeanProxy> T getTarget(Class<? extends ConfigBeanProxy> targetType, Class<T> type) throws ClassNotFoundException {

        // when using the target based parameter, we look first for a configuration of that name,
        // then we look for a cluster of that name and finally we look for a subelement of the right type

        final String name = getName();

        ConfigBeanProxy config = habitat.getService(targetType, target);
        if (config!=null) {
            try {
                return type.cast(config);
            } catch (ClassCastException e) {
                // ok we need to do more work to find which object is really requested.
            }
            Dom parentDom = Dom.unwrap(config);

            String elementName = GenericCrudCommand.elementName(parentDom.document, targetType, type);
            if (elementName==null) {
                return null;
            }
            ConfigModel.Property property = parentDom.model.getElement(elementName);
            if (property.isCollection()) {
                Collection<Dom> collection = parentDom.nodeElements(elementName);
                if (collection==null) {
                    return null;
                }

                for (Dom child : collection) {
                    if (name.equals(child.attribute("ref"))) {
                        return type.cast(child.<ConfigBeanProxy>createProxy());
                    }
                }
            }
        }
        return null;
    }

    public String getName() {
        return "";
    }

}
