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

package org.glassfish.concurrent.runtime.deployer;

import com.sun.logging.LogDomains;
import org.glassfish.concurrent.runtime.ConcurrentRuntime;
import org.glassfish.enterprise.concurrent.*;
import org.glassfish.resourcebase.resources.api.ResourceInfo;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ConcurrentObjectFactory implements ObjectFactory {

    private static Logger _logger = LogDomains.getLogger(ConcurrentObjectFactory.class, LogDomains.JNDI_LOGGER);

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference) obj;
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"ConcurrentNamingObjectFactory: " + ref +
                    " Name:" + name);
        }
        BaseConfig config = (BaseConfig) ref.get(0).getContent();
        ResourceInfo resourceInfo = (ResourceInfo) ref.get(1).getContent();

        Object instance = null;
        switch(config.getType()) {
            case CONTEXT_SERVICE:
                instance = getContextService((ContextServiceConfig)config, resourceInfo);
                break;
            case MANAGED_EXECUTOR_SERVICE:
                instance = getManagedExecutorService((ManagedExecutorServiceConfig)config, resourceInfo);
                break;
            case MANAGED_SCHEDULED_EXECUTOR_SERVICE:
                instance = getManagedScheduledExecutorService((ManagedScheduledExecutorServiceConfig)config, resourceInfo);
                break;
            case MANAGED_THREAD_FACTORY:
                instance = getManagedThreadFactory((ManagedThreadFactoryConfig)config, resourceInfo);
                break;
            default:
                break;
        }
        return instance;
    }

    private ContextServiceImpl getContextService(ContextServiceConfig config, ResourceInfo resourceInfo) {

        ContextServiceImpl contextService = getRuntime().getContextService(resourceInfo, config);
        return contextService;
    }

    private ManagedThreadFactoryImpl getManagedThreadFactory(ManagedThreadFactoryConfig config, ResourceInfo resourceInfo) {
        ManagedThreadFactoryImpl managedThreadFactory = getRuntime().getManagedThreadFactory(resourceInfo, config);
        return managedThreadFactory;
    }

    private ManagedExecutorServiceAdapter getManagedExecutorService(ManagedExecutorServiceConfig config, ResourceInfo resourceInfo) {
        ManagedExecutorServiceImpl mes = getRuntime().getManagedExecutorService(resourceInfo, config);
        return mes.getAdapter();
    }

    private ManagedScheduledExecutorServiceAdapter getManagedScheduledExecutorService(ManagedScheduledExecutorServiceConfig config, ResourceInfo resourceInfo) {
        ManagedScheduledExecutorServiceImpl mes = getRuntime().getManagedScheduledExecutorService(resourceInfo, config);
        return mes.getAdapter();
    }

    private ConcurrentRuntime getRuntime() {
        return ConcurrentRuntime.getRuntime();
    }
}
