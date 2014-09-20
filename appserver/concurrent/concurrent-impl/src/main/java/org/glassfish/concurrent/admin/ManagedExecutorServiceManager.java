/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.concurrent.admin;

import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.I18n;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfiguredBy;
import org.jvnet.hk2.config.TransactionFailure;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.glassfish.concurrent.config.ManagedExecutorServiceBase;
import org.glassfish.concurrent.config.ManagedExecutorService;

import static org.glassfish.resources.admin.cli.ResourceConstants.*;

import java.util.HashMap;
import java.util.Properties;
import java.beans.PropertyVetoException;

/**
 *
 * The managed executor service manager allows you to create and delete 
 * the managed-executor-service config element
 */
@Service (name=ServerTags.MANAGED_EXECUTOR_SERVICE)
@I18n("managed.executor.service.manager")
@ConfiguredBy(Resources.class)
public class ManagedExecutorServiceManager extends ManagedExecutorServiceBaseManager {

    private String maximumPoolSize = ""+Integer.MAX_VALUE;
    private String taskQueueCapacity = ""+Integer.MAX_VALUE;

    @Override
    protected void setAttributes(HashMap attributes, String target) {
        super.setAttributes(attributes, target);
        maximumPoolSize = (String) attributes.get(MAXIMUM_POOL_SIZE);
        taskQueueCapacity = (String) attributes.get(TASK_QUEUE_CAPACITY);
    }

    @Override
    protected ResourceStatus isValid(Resources resources, boolean validateResourceRef, String target){
        if (Integer.parseInt(corePoolSize) == 0 &&
            Integer.parseInt(maximumPoolSize) == 0) {
            String msg = localStrings.getLocalString("coresize.maxsize.both.zero", "Options corepoolsize and maximumpoolsize cannot both have value 0.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        if (Integer.parseInt(corePoolSize) >
            Integer.parseInt(maximumPoolSize)) {
            String msg = localStrings.getLocalString("coresize.biggerthan.maxsize", "Option corepoolsize cannot have a bigger value than option maximumpoolsize.");
            return new ResourceStatus(ResourceStatus.FAILURE, msg);
        }

        return super.isValid(resources, validateResourceRef, target); 
    }

    protected ManagedExecutorServiceBase createConfigBean(Resources param, Properties properties) throws PropertyVetoException, TransactionFailure {
        ManagedExecutorService managedExecutorService = param.createChild(ManagedExecutorService.class);
        setAttributesOnConfigBean(managedExecutorService, properties); 
        managedExecutorService.setMaximumPoolSize(maximumPoolSize);
        managedExecutorService.setTaskQueueCapacity(taskQueueCapacity);
        return managedExecutorService;
    }

    public String getResourceType () {
        return ServerTags.MANAGED_EXECUTOR_SERVICE;
    }
}
