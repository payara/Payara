/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2019] [Payara Foundation and/or its affiliates]
package org.glassfish.jdbc.config.validators;

import com.sun.enterprise.config.serverbeans.ResourcePool;
import java.util.Properties;
import org.glassfish.config.support.Constants;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Implementation for Connection Pool validation.
 * Following validations are done :
 * - Validation of datasource/driver classnames when resource type is not null 
 * - Max pool size to be always higher than steady pool size
 * - Check if statement wrapping is on when certain features are enabled.
 * 
 * @author Shalini M
 */
public class JdbcConnectionPoolValidator
    implements ConstraintValidator<JdbcConnectionPoolConstraint, ResourcePool> {
    
    protected ConnectionPoolErrorMessages poolFaults;
    
    public void initialize(final JdbcConnectionPoolConstraint constraint) {
        this.poolFaults = constraint.value();
    }

    public String getParsedVariable(String variableToRetrieve) {

        String[] variableReference = variableToRetrieve.split("=");

        if (variableReference.length == 1) {
            //We got a system variable as no split occured
            return System.getProperty(variableReference[0]);
        }

        String variableToFind = variableReference[1];

        switch (variableReference[0]) {
            case "ENV":
                //Check environment variables for requested value
                String varValue = System.getenv(variableToFind);
                if (varValue != null && !varValue.isEmpty()) {
                    return varValue;
                }
                break;
            case "MPCONFIG":
                //Check microprofile config for requested value
                Config config = ConfigProvider.getConfig();
                varValue = config.getValue(variableToFind, String.class);
                if (varValue != null && !varValue.isEmpty()) {
                    return varValue;
                }
                break;
        }
        
        //If this point is reached, the variable value could not be found
        return null; 
    }
    
    @Override
    public boolean isValid(final ResourcePool pool,
        final ConstraintValidatorContext constraintValidatorContext) {

        if(poolFaults == ConnectionPoolErrorMessages.MAX_STEADY_INVALID) {
            if(pool instanceof JdbcConnectionPool) {
                
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                String maxPoolSize = jdbcPool.getMaxPoolSize();
                String steadyPoolSize = jdbcPool.getSteadyPoolSize();
                
                int maxPoolSizeValue = 0;
                int steadyPoolSizeValue = 0;
                
                if (steadyPoolSize == null) {
                    steadyPoolSize = Constants.DEFAULT_STEADY_POOL_SIZE;
                } else if(steadyPoolSize.startsWith("$")) {
                    steadyPoolSize = getParsedVariable(steadyPoolSize.substring(2, steadyPoolSize.length() - 1));
                    if(steadyPoolSize == null) return false;
                }
                
                if (maxPoolSize == null) {
                    maxPoolSize = Constants.DEFAULT_MAX_POOL_SIZE;
                } else if(maxPoolSize.startsWith("$")) {
                    maxPoolSize = getParsedVariable(maxPoolSize.substring(2, maxPoolSize.length() - 1));
                    if(maxPoolSize == null) return false;
                }
                
                try {
                    maxPoolSizeValue = Integer.parseInt(maxPoolSize);
                    steadyPoolSizeValue = Integer.parseInt(steadyPoolSize);
                } catch(NumberFormatException nfe) {
                    System.out.println("Exception occured whilst parsing value to int: \n - " + nfe.getMessage());
                    return false;
                }
                
                if (maxPoolSizeValue < steadyPoolSizeValue 
                        || steadyPoolSizeValue < 0
                        || maxPoolSizeValue <= 0) {
                    //Value(s) are invalid so return error
                    return false;
                }
            }
        }
        
        if(poolFaults == ConnectionPoolErrorMessages.STMT_WRAPPING_DISABLED) {
            if(pool instanceof JdbcConnectionPool) {
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                String stmtCacheSize = jdbcPool.getStatementCacheSize();
                String stmtLeakTimeout = jdbcPool.getStatementLeakTimeoutInSeconds();
                
                // PAYARA-661 allow empty "" sql trace listeners
                if (jdbcPool.getSqlTraceListeners() != null && !jdbcPool.getSqlTraceListeners().isEmpty()) {
                    if (!Boolean.valueOf(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
                if (stmtCacheSize != null && Integer.parseInt(stmtCacheSize) != 0) {
                    if (!Boolean.valueOf(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
                if (stmtLeakTimeout != null && Integer.parseInt(stmtLeakTimeout) != 0) {
                    if (!Boolean.parseBoolean(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
                if (Boolean.valueOf(jdbcPool.getStatementLeakReclaim())) {
                    if (!Boolean.valueOf(jdbcPool.getWrapJdbcObjects())) {
                        return false;
                    }
                }
            }
        }

        if(poolFaults == ConnectionPoolErrorMessages.TABLE_NAME_MANDATORY){
            if(pool instanceof JdbcConnectionPool){
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                if (Boolean.valueOf(jdbcPool.getIsConnectionValidationRequired())) {
                    if ("table".equals(jdbcPool.getConnectionValidationMethod())) {
                        if(jdbcPool.getValidationTableName() == null || jdbcPool.getValidationTableName().equals("")){
                            return false;
                        }
                    }
                }
            }
        }

        if(poolFaults == ConnectionPoolErrorMessages.CUSTOM_VALIDATION_CLASS_NAME_MANDATORY){
            if(pool instanceof JdbcConnectionPool){
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                if (Boolean.valueOf(jdbcPool.getIsConnectionValidationRequired())) {
                    if ("custom-validation".equals(jdbcPool.getConnectionValidationMethod())) {
                        if(jdbcPool.getValidationClassname() == null || jdbcPool.getValidationClassname().equals("")){
                            return false;
                        }
                    }
                }
            }
        }

        if (poolFaults == ConnectionPoolErrorMessages.RES_TYPE_MANDATORY) {
            if (pool instanceof JdbcConnectionPool) {
                JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
                String resType = jdbcPool.getResType();
                String dsClassName = jdbcPool.getDatasourceClassname();
                String driverClassName = jdbcPool.getDriverClassname();
                if (resType == null) {
                    //One of each datasource/driver classnames must be provided.
                    if ((dsClassName == null || dsClassName.equals("")) &&
                            (driverClassName == null || driverClassName.equals(""))) {
                        return false;
                    } else {
                        //Check if both are provided and if so, return false
                        if (dsClassName != null && driverClassName != null) {
                            return false;
                        }
                    }
                } else if (resType.equals("javax.sql.DataSource") ||
                        resType.equals("javax.sql.ConnectionPoolDataSource") ||
                        resType.equals("javax.sql.XADataSource")) {
                    //Then datasourceclassname cannot be empty
                    if (dsClassName == null || dsClassName.equals("")) {
                        return false;
                    }
                } else if (resType.equals("java.sql.Driver")) {
                    //Then driver classname cannot be empty
                    if (driverClassName == null || driverClassName.equals("")) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}





