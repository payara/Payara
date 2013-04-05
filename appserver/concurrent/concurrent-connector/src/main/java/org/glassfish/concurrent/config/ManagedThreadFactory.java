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

package org.glassfish.concurrent.config;

import com.sun.enterprise.config.modularity.ConfigBeanInstaller;
import com.sun.enterprise.config.modularity.annotation.CustomConfiguration;
import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.customvalidators.ReferenceConstraint;
import org.glassfish.admin.cli.resources.ResourceConfigCreator;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.admin.cli.resources.UniqueResourceNameConstraint;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;
import org.glassfish.resourcebase.resources.ResourceTypeOrder;
import org.glassfish.resourcebase.resources.ResourceDeploymentOrder;

import javax.validation.Payload;
import javax.validation.constraints.Min;
import java.beans.PropertyVetoException;

/**
 * Concurrency managed thread factory resource definition
 */

@Configured
@ResourceConfigCreator(commandName="create-managed-thread-factory")
@RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-managed-thread-factory"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-managed-thread-factory")
})
@ResourceTypeOrder(deploymentOrder=ResourceDeploymentOrder.MANAGED_THREAD_FACTORY)
@ReferenceConstraint(skipDuringCreation=true, payload=ManagedThreadFactory.class)
@UniqueResourceNameConstraint(message="{resourcename.isnot.unique}", payload=ManagedThreadFactory.class)
@CustomConfiguration(baseConfigurationFileName = "managed-thread-factory-conf.xml")
public interface ManagedThreadFactory extends ConfigBeanProxy, Resource,
        BindableResource, ConcurrencyResource, Payload  {

    /**
     * Gets the value of the threadPriority property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue=""+Thread.NORM_PRIORITY, dataType=Integer.class)
    @Min(value=0)
    String getThreadPriority();

    /**
     * Sets the value of the threadPriority property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setThreadPriority(String value) throws PropertyVetoException;
    
    @DuckTyped
    String getIdentity();

    class Duck {
        public static String getIdentity(ManagedThreadFactory resource){
            return resource.getJndiName();
        }
    }

    @Service
    public class ManagedThreadFactoryConfigActivator extends ConfigBeanInstaller {

    }
}
