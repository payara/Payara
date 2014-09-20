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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import javax.inject.Inject;
import java.beans.PropertyVetoException;

/**
 * Resolver for an un-named type. If the type instance is not found, it will
 * create an instance of it under the domain instance.
 *
 * @author Jerome Dochez
 */
@Service(name="type")
public class TypeResolver implements CrudResolver {

    @Inject
    private ServiceLocator habitat;

    @Inject
    Domain domain;

    final protected static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(TypeResolver.class);

    @Override
    public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, final Class<T> type) {
        T proxy = habitat.getService(type);
        if (proxy==null) {
            try {
                proxy = type.cast(ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    @Override
                    public Object run(Domain writeableDomain) throws PropertyVetoException, TransactionFailure {
                        ConfigBeanProxy child = writeableDomain.createChild(type);
                        Dom domDomain = Dom.unwrap(writeableDomain);
                        final String elementName;
                        try {
                            elementName = GenericCrudCommand.elementName(domDomain.document, Domain.class, type);
                        } catch (ClassNotFoundException e) {
                            throw new TransactionFailure(e.toString());
                        }
                        if (elementName==null) {
                            String msg = localStrings.getLocalString(TypeResolver.class,
                                    "TypeResolver.no_element_of_that_type",
                                    "The Domain configuration does not have a sub-element of the type {0}", type.getSimpleName());
                            throw new TransactionFailure(msg);
                        }
                        domDomain.setNodeElements(elementName, Dom.unwrap(child));

                        // add to the habitat
                        ServiceLocatorUtilities.addOneConstant(habitat, child, null, type);

                        return child;
                    }
                }, domain));
            } catch(TransactionFailure e) {
                throw new RuntimeException(e);
            }
            if (proxy==null) {
                String msg = localStrings.getLocalString(TypeResolver.class,
                        "TypeResolver.target_object_not_found",
                        "Cannot find a single component instance of type {0}", type.getSimpleName());
                throw new RuntimeException(msg);
            }
        }
        return proxy;

    }
}
