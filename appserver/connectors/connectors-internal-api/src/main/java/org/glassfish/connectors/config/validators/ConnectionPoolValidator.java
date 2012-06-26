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

package org.glassfish.connectors.config.validators;

import org.glassfish.connectors.config.ConnectorConnectionPool;
import com.sun.enterprise.config.serverbeans.ResourcePool;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.glassfish.config.support.Constants;

/**
 * Implementation for Connection Pool validation.
 * Following validations are done :
 * - Validation of datasource/driver classnames when resource type is not null 
 * - Max pool size to be always higher than steady pool size
 * - Check if statement wrapping is on when certain features are enabled.
 * 
 * @author Shalini M
 */
public class ConnectionPoolValidator
    implements ConstraintValidator<ConnectionPoolConstraint, ResourcePool> {
    
    protected ConnectionPoolErrorMessages poolFaults;
    
    public void initialize(final ConnectionPoolConstraint constraint) {
        this.poolFaults = constraint.value();
    }

    @Override
    public boolean isValid(final ResourcePool pool,
        final ConstraintValidatorContext constraintValidatorContext) {

        if (poolFaults == ConnectionPoolErrorMessages.MAX_STEADY_INVALID) {
            if (pool instanceof ConnectorConnectionPool) {
                ConnectorConnectionPool connPool = (ConnectorConnectionPool) pool;
                String maxPoolSize = connPool.getMaxPoolSize();
                String steadyPoolSize = connPool.getSteadyPoolSize();
                if(steadyPoolSize == null) {
                    steadyPoolSize = Constants.DEFAULT_STEADY_POOL_SIZE;
                }
                if (maxPoolSize == null) {
                    maxPoolSize = Constants.DEFAULT_MAX_POOL_SIZE;
                }
                if (Integer.parseInt(maxPoolSize) <
                        (Integer.parseInt(steadyPoolSize))) {
                    //max pool size fault
                    return false;
                }                
            }
        }
        return true;
    }
}





