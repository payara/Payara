/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;

import javax.validation.constraints.Pattern;
import java.beans.PropertyVetoException;

/**
 * Tag interface for all types of resource.
 * 
 * @author Jerome Dochez
 */
@Configured
public interface Resource extends ConfigBeanProxy {

    /**
     * Gets the value of the objectType property.
     * where object-type defines the type of the resource.
     * It can be:
     *  system-all - These are system resources for all instances and DAS
     *  system-all-req - These are system-all resources that are required to be
     *                   configured in the system (cannot be deleted). 
     *  system-admin - These are system resources only in DAS
     *  system-instance - These are system resources only in instances
     *                    (and not DAS)
     *  user - User resources (This is the default for all elements)
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="user")
    @Pattern(regexp="(system-all|system-all-req|system-admin|system-instance|user)")            
    String getObjectType();

    /**
     * Sets the value of the objectType property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException if the change is unacceptable to one
     * of the listeners.
     */
    void setObjectType(String value) throws PropertyVetoException;

    /**
     * Gets the value of deployment-order.
     * @return
     */
    @Attribute (defaultValue = "100",dataType = Integer.class)
    String getDeploymentOrder();

    /**
     * Sets the value of the deployment order.
     * @param value
     * @throws PropertyVetoException
     */
    void setDeploymentOrder(String value) throws PropertyVetoException;


    @DuckTyped
    String getIdentity();
    
    class Duck {
        public static String getIdentity(Resource resource){
            return null;
        }
        /*
         * True if this resource should be copied to any new instance or cluster.
         * Note: this isn't a DuckTyped method because it requires every subclass
         * to implement this method.
         */
        public static boolean copyToInstance(Resource resource) {
            String ot = resource.getObjectType();
            return "system-all".equals(ot) || 
                    "system-all-req".equals(ot) ||
                    "system-instance".equals(ot);
        }
    }
}
