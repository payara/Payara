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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.jdbc.config.validators;

import com.sun.enterprise.config.serverbeans.ResourcePool;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.config.support.Constants;
import org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages;
import org.glassfish.jdbc.config.JdbcConnectionPool;

/**
 * Implementation for Connection Pool validation.
 *
 * Following validations are done:
 * <ul>
 * <li>Validation of datasource/driver classnames when resource type is not null
 * <li>Max pool size to be always higher than steady pool size
 * <li>Check if statement wrapping is on when certain features are enabled.
 * </lu>
 *
 * @author Shalini M
 */
public class JdbcConnectionPoolValidator implements ConstraintValidator<JdbcConnectionPoolConstraint, ResourcePool> {

    protected ConnectionPoolErrorMessages poolFaults;


    @Override
    public void initialize(final JdbcConnectionPoolConstraint constraint) {
        this.poolFaults = constraint.value();
    }


    @Override
    public boolean isValid(final ResourcePool pool, final ConstraintValidatorContext context) {
        if (!JdbcConnectionPool.class.isInstance(pool)) {
            return true;
        }
        // original code allowed this. Why?
        if (this.poolFaults == null) {
            return true;
        }
        final JdbcConnectionPool jdbcPool = (JdbcConnectionPool) pool;
        switch (poolFaults) {
            case POOL_SIZE_STEADY:
                return validateSteadyPoolSize(jdbcPool);
            case POOL_SIZE_MAX:
                return validateMaxPoolSize(jdbcPool);
            case STMT_WRAPPING_DISABLED:
                return validateWrapping(jdbcPool);
            case TABLE_NAME_MANDATORY:
                return validateTableConnectionValidation(jdbcPool);
            case CUSTOM_VALIDATION_CLASS_NAME_MANDATORY:
                return validateClassConnectionValidation(jdbcPool);
            case RES_TYPE_MANDATORY:
                return validateResourceType(jdbcPool);
            default:
                return true;
        }
    }


    private boolean validateSteadyPoolSize(final JdbcConnectionPool jdbcPool) {
        final Integer steadyPoolSize = getSteadyPoolSize(jdbcPool);
        return steadyPoolSize != null && steadyPoolSize >= 0;
    }


    private boolean validateMaxPoolSize(final JdbcConnectionPool jdbcPool) {
        final Integer steadyPoolSize = getSteadyPoolSize(jdbcPool);
        final String propertyValue = jdbcPool.getMaxPoolSize();
        final String value = propertyValue == null ? Constants.DEFAULT_MAX_POOL_SIZE : propertyValue;
        final Integer maxPoolSize = toInteger(resolve(value));
        if (maxPoolSize == null || maxPoolSize < 1) {
            return false;
        }
        if (steadyPoolSize != null && steadyPoolSize > maxPoolSize) {
            return false;
        }
        return true;
    }


    private boolean validateWrapping(final JdbcConnectionPool jdbcPool) {
        final String stmtCacheSize = jdbcPool.getStatementCacheSize();
        final String stmtLeakTimeout = jdbcPool.getStatementLeakTimeoutInSeconds();
        final boolean wrappingEnabled = isTrue(jdbcPool.getWrapJdbcObjects());
        if (!wrappingEnabled && isPositiveInt(stmtCacheSize)) {
            return false;
        }
        if (!wrappingEnabled && isPositiveInt(stmtLeakTimeout)) {
            return false;
        }
        if (!wrappingEnabled && isTrue(jdbcPool.getStatementLeakReclaim())) {
            return false;
        }
        return true;
    }


    private boolean validateTableConnectionValidation(final JdbcConnectionPool jdbcPool) {
        if (isTrue(jdbcPool.getIsConnectionValidationRequired())) {
            if ("table".equals(jdbcPool.getConnectionValidationMethod())) {
                if (isEmpty((jdbcPool.getValidationTableName()))) {
                    return false;
                }
            }
        }
        return true;
    }


    private boolean validateClassConnectionValidation(final JdbcConnectionPool jdbcPool) {
        if (isTrue(jdbcPool.getIsConnectionValidationRequired())) {
            if ("custom-validation".equals(jdbcPool.getConnectionValidationMethod())) {
                if (isEmpty(jdbcPool.getValidationClassname())) {
                    return false;
                }
            }
        }
        return true;
    }


    private boolean validateResourceType(final JdbcConnectionPool jdbcPool) {
        final String resType = jdbcPool.getResType();
        final String dsClassName = jdbcPool.getDatasourceClassname();
        final String driverClassName = jdbcPool.getDriverClassname();
        if (resType == null) {
            // One of each datasource/driver classnames must be provided.
            if (isEmpty(dsClassName) && isEmpty(driverClassName)) {
                return false;
            }
            // Check if both are provided and if so, return false
            if (dsClassName != null && driverClassName != null) {
                return false;
            }
        } else if (resType.equals("javax.sql.DataSource") //
            || resType.equals("javax.sql.ConnectionPoolDataSource") //
            || resType.equals("javax.sql.XADataSource")) {
            if (isEmpty(dsClassName)) {
                return false;
            }
        } else if (resType.equals("java.sql.Driver")) {
            if (isEmpty(driverClassName)) {
                return false;
            }
        }
        return true;
    }


    private Integer getSteadyPoolSize(final JdbcConnectionPool jdbcPool) {
        final String property = jdbcPool.getSteadyPoolSize();
        return toInteger(resolve(property == null ? Constants.DEFAULT_STEADY_POOL_SIZE : property));
    }


    private String resolve(final String propertyValue) {
        if (propertyValue.startsWith("$")) {
            return getParsedVariable(propertyValue.substring(2, propertyValue.length() - 1));
        }
        return propertyValue;
    }


    private String getParsedVariable(final String variableToRetrieve) {

        final String[] variableReference = variableToRetrieve.split("=");
        if (variableReference.length == 1) {
            // We got a system variable as no split occured
            return System.getProperty(variableReference[0]);
        }

        final String variableToFind = variableReference[1];
        switch (variableReference[0]) {
            case "ENV":
                // Check environment variables for requested value
                String varValue = System.getenv(variableToFind);
                if (!isEmpty(varValue)) {
                    return varValue;
                }
                break;
            case "MPCONFIG":
                // Check microprofile config for requested value
                final Config config = ConfigProvider.getConfig();
                varValue = config.getValue(variableToFind, String.class);
                if (!isEmpty(varValue)) {
                    return varValue;
                }
                break;
        }

        // If this point is reached, the variable value could not be found
        return null;
    }


    private boolean isEmpty(final String value) {
        return value == null || value.trim().isEmpty();
    }


    private boolean isPositiveInt(final String value) {
        try {
            return !isEmpty(value) && Integer.parseInt(value) > 0;
        } catch (final NumberFormatException e) {
            return false;
        }
    }


    private boolean isTrue(final String value) {
        return Boolean.parseBoolean(value);
    }


    private Integer toInteger(final String resolvedValue) {
        if (isEmpty(resolvedValue)) {
            return null;
        }
        try {
            return Integer.valueOf(resolvedValue);
        } catch (final NumberFormatException nfe) {
            return null;
        }
    }
}
