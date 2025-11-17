/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2019-2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package org.glassfish.main.jdbc.config.validators;

import com.sun.enterprise.config.serverbeans.ResourcePool;
import com.sun.enterprise.util.ExceptionUtil;

import java.sql.Driver;
import java.util.function.Consumer;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.sun.enterprise.util.OS;
import jakarta.validation.Payload;

import org.glassfish.api.jdbc.SQLTraceListener;
import org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.glassfish.jdbc.config.validators.JdbcConnectionPoolConstraint;
import org.glassfish.jdbc.config.validators.JdbcConnectionPoolValidator;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages.CUSTOM_VALIDATION_CLASS_NAME_MANDATORY;
import static org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages.POOL_SIZE_MAX;
import static org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages.POOL_SIZE_STEADY;
import static org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages.RES_TYPE_MANDATORY;
import static org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages.STMT_WRAPPING_DISABLED;
import static org.glassfish.connectors.config.validators.ConnectionPoolErrorMessages.TABLE_NAME_MANDATORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Note: context parameter is not used in validator, that's why it is always null in this test.
 *
 * @author David Matejcek
 */
public class JdbcConnectionPoolValidatorTest {

    private JdbcConnectionPoolValidator validator;


    @Before
    public void createValidator() {
        this.validator = new JdbcConnectionPoolValidator();
    }


    @Test
    public void nulls() {
        // any of message enums
        this.validator.initialize(createAnnotation(POOL_SIZE_MAX));
        assertTrue("isValid(null, null)", this.validator.isValid(null, null));
    }


    @Test
    public void uninitialized() {
        final JdbcConnectionPool pool = createMock();
        assertTrue("everything OK", this.validator.isValid(pool, null));
    }


    @Test
    public void nonJdbcConnectionPool() {
        final ResourcePool pool = createNiceMock(ResourcePool.class);
        assertTrue("everything OK", this.validator.isValid(pool, null));
    }


    @Test
    public void poolSizeSteady() {
        this.validator.initialize(createAnnotation(POOL_SIZE_STEADY));
        final JdbcConnectionPool pool = createMock();
        assertTrue("everything OK", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn(null));
        assertTrue("steady pool size null", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn("-1"));
        assertFalse("steady pool size negative", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn("${env.steadypoolsizeproperty}"));
        assertTrue("undefined variable in steady pool size", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn("${ENV=steadypoolsizeproperty}"));
        assertTrue("undefined variable in steady pool size", this.validator.isValid(pool, null));

        if (OS.isWindows()) {
            updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn("${ENV=USERNAME}"));
            assertFalse("env.USERNAME variable in steady pool size is not a number", this.validator.isValid(pool, null));
        } else {
            updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn("${ENV=USER}"));
            assertFalse("env.USER variable in steady pool size is not a number", this.validator.isValid(pool, null));
        }

        updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn("${MPCONFIG=USER}"));
        try {
            this.validator.isValid(pool, null);
            fail("Expected exception, because we don't have any microprofile impl here.");
        } catch (final Exception e) {
            assertEquals("root cause.message", "No ConfigProviderResolver implementation found!",
                ExceptionUtil.getRootCause(e).getMessage());
        }

        updateMock(pool, p -> {
            expect(p.getSteadyPoolSize()).andStubReturn("8");
            expect(p.getMaxPoolSize()).andStubReturn("5");
        });
        assertTrue("this should not check the max pool size", this.validator.isValid(pool, null));
    }


    @Test
    public void poolSizeMax() {
        this.validator.initialize(createAnnotation(POOL_SIZE_MAX));
        final JdbcConnectionPool pool = createMock();
        assertTrue("everything OK", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn(null));
        assertTrue("steady pool size null", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getSteadyPoolSize()).andStubReturn("-1"));
        assertTrue("negative steady pool size is not thing checked by max checker", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getSteadyPoolSize()).andStubReturn("8");
            expect(p.getMaxPoolSize()).andStubReturn("5");
        });
        assertFalse("steady pool size is higher than max pool size", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getSteadyPoolSize()).andStubReturn("0");
            expect(p.getMaxPoolSize()).andStubReturn("0");
        });
        assertFalse("max pool size is 0", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getMaxPoolSize()).andStubReturn("${ENV=maxpoolsizeproperty}"));
        assertTrue("undefined variable in max pool size", this.validator.isValid(pool, null));
    }


    @Test
    public void wrappers() {
        this.validator.initialize(createAnnotation(STMT_WRAPPING_DISABLED));
        final JdbcConnectionPool pool = createMock();
        assertTrue("everything OK", this.validator.isValid(pool, null));

        updateMock(pool, p -> expect(p.getWrapJdbcObjects()).andStubReturn("false"));
        assertTrue("wrapping disabled, all related settings ok", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getWrapJdbcObjects()).andStubReturn("false");
            expect(p.getSqlTraceListeners()).andStubReturn(null);
            expect(p.getStatementCacheSize()).andStubReturn("200");
        });
        assertFalse("wrapping disabled, but statement cache size set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getWrapJdbcObjects()).andStubReturn(null);
            expect(p.getStatementCacheSize()).andStubReturn("13");
            expect(p.getStatementLeakTimeoutInSeconds()).andStubReturn("30");
        });
        assertFalse("wrapping disabled, but statement leak timeout set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getWrapJdbcObjects()).andStubReturn(null);
            expect(p.getStatementLeakTimeoutInSeconds()).andStubReturn(null);
            expect(p.getStatementLeakReclaim()).andStubReturn("true");
        });
        assertFalse("wrapping disabled, but statement leak reclaim set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getWrapJdbcObjects()).andStubReturn(null);
            expect(p.getStatementLeakTimeoutInSeconds()).andStubReturn(null);
            expect(p.getStatementLeakReclaim()).andStubReturn("false");
        });
        assertTrue("wrapping disabled, no conflicts", this.validator.isValid(pool, null));
    }


    @Test
    public void tableNameConnectionValidation() {
        this.validator.initialize(createAnnotation(TABLE_NAME_MANDATORY));
        final JdbcConnectionPool pool = createMock();
        assertTrue("everything OK", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getIsConnectionValidationRequired()).andStubReturn("true");
            expect(p.getConnectionValidationMethod()).andStubReturn("table");
            expect(p.getValidationTableName()).andStubReturn("MY-TABLE");
        });
        assertTrue("validation by table enabled, everything is set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getIsConnectionValidationRequired()).andStubReturn("true");
            // is this alright?
            expect(p.getConnectionValidationMethod()).andStubReturn("somethingelse");
            expect(p.getValidationTableName()).andStubReturn(null);
        });
        assertTrue("validation by table is not set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getIsConnectionValidationRequired()).andStubReturn("true");
            expect(p.getConnectionValidationMethod()).andStubReturn("table");
            expect(p.getValidationTableName()).andStubReturn(null);
        });
        assertFalse("validation by table set, but no table name", this.validator.isValid(pool, null));
    }


    @Test
    public void customConnectionValidation() {
        this.validator.initialize(createAnnotation(CUSTOM_VALIDATION_CLASS_NAME_MANDATORY));
        final JdbcConnectionPool pool = createMock();
        assertTrue("everything OK", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getIsConnectionValidationRequired()).andStubReturn("true");
            expect(p.getConnectionValidationMethod()).andStubReturn("custom-validation");
            expect(p.getValidationClassname()).andStubReturn(String.class.getName());
        });
        assertTrue("validation by class enabled, everything is set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getIsConnectionValidationRequired()).andStubReturn("true");
            // is this alright?
            expect(p.getConnectionValidationMethod()).andStubReturn("somethingelse");
            expect(p.getValidationClassname()).andStubReturn(null);
        });
        assertTrue("validation by class is not set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(p.getIsConnectionValidationRequired()).andStubReturn("true");
            expect(p.getConnectionValidationMethod()).andStubReturn("custom-validation");
            expect(p.getValidationClassname()).andStubReturn(null);
        });
        assertFalse("validation by class set, but no class name", this.validator.isValid(pool, null));
    }


    @Test
    public void resourceType() {
        this.validator.initialize(createAnnotation(RES_TYPE_MANDATORY));
        final JdbcConnectionPool pool = createMock();
        assertTrue("everything OK", this.validator.isValid(pool, null));

        final Class<?>[] apiTypes = new Class[] {DataSource.class, XADataSource.class, ConnectionPoolDataSource.class};
        for (Class<?> api : apiTypes) {
            updateMock(pool, p -> {
                expect(pool.getResType()).andStubReturn(api.getName());
                expect(pool.getDatasourceClassname()).andStubReturn(null);
                expect(pool.getDriverClassname()).andStubReturn("ewdwedw");
            });
            assertFalse("Resource type " + api + ", but missing impl", this.validator.isValid(pool, null));

            updateMock(pool, p -> {
                expect(pool.getResType()).andStubReturn(api.getName());
                expect(pool.getDatasourceClassname()).andStubReturn(DataSource.class.getName());
                expect(pool.getDriverClassname()).andStubReturn(null);
            });
            assertTrue("Resource type " + api + "and Data source type set", this.validator.isValid(pool, null));
        }

        updateMock(pool, p -> {
            expect(pool.getResType()).andStubReturn(null);
            expect(pool.getDatasourceClassname()).andStubReturn(null);
            expect(pool.getDriverClassname()).andStubReturn(Driver.class.getName());
        });
        assertTrue("Resource type is null, driver is set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(pool.getResType()).andStubReturn(null);
            expect(pool.getDatasourceClassname()).andStubReturn(DataSource.class.getName());
            expect(pool.getDriverClassname()).andStubReturn(null);
        });
        assertTrue("Resource type is null, ds is set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(pool.getResType()).andStubReturn(null);
            expect(pool.getDatasourceClassname()).andStubReturn(DataSource.class.getName());
            expect(pool.getDriverClassname()).andStubReturn(Driver.class.getName());
        });
        assertFalse("Resource type is null, ds and driver are set", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(pool.getResType()).andStubReturn(null);
            expect(pool.getDatasourceClassname()).andStubReturn(null);
            expect(pool.getDriverClassname()).andStubReturn(null);
        });
        assertFalse("Resource type, ds and driver are null", this.validator.isValid(pool, null));

        // questionable behavior of the old code
        updateMock(pool, p -> {
            expect(pool.getResType()).andStubReturn(String.class.getName());
            expect(pool.getDatasourceClassname()).andStubReturn(null);
            expect(pool.getDriverClassname()).andStubReturn(null);
        });
        assertTrue("Resource type is unknown", this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(pool.getResType()).andStubReturn(Driver.class.getName());
            expect(pool.getDatasourceClassname()).andStubReturn(null);
            expect(pool.getDriverClassname()).andStubReturn(null);
        });
        assertFalse("Resource type is " + pool.getResType() + ", but driver class is null",
            this.validator.isValid(pool, null));

        updateMock(pool, p -> {
            expect(pool.getResType()).andStubReturn(Driver.class.getName());
            expect(pool.getDatasourceClassname()).andStubReturn(null);
            expect(pool.getDriverClassname()).andStubReturn(Driver.class.getName());
        });
        assertTrue("Resource type is " + pool.getResType() + ", and driver class is set",
            this.validator.isValid(pool, null));
    }


    private JdbcConnectionPool createMock() {
        final JdbcConnectionPool pool = createNiceMock(JdbcConnectionPool.class);
        expect(pool.getName()).andStubReturn("MyPoolName");

        expect(pool.getWrapJdbcObjects()).andStubReturn("true");
        // fictive wrapper, validator does not check if it can work
        expect(pool.getSqlTraceListeners()).andStubReturn(SQLTraceListener.class.getName());
        expect(pool.getStatementCacheSize()).andStubReturn("10");
        expect(pool.getStatementLeakTimeoutInSeconds()).andStubReturn("30");
        expect(pool.getStatementLeakReclaim()).andStubReturn("true");

        expect(pool.getSteadyPoolSize()).andStubReturn("0");
        expect(pool.getMaxPoolSize()).andStubReturn("16");

        expect(pool.getIsConnectionValidationRequired()).andStubReturn("false");
        expect(pool.getConnectionValidationMethod()).andStubReturn(null);
        expect(pool.getValidationTableName()).andStubReturn(null);
        expect(pool.getValidationClassname()).andStubReturn(null);

        expect(pool.getResType()).andStubReturn(DataSource.class.getName());
        // fictive, validator does not check if it is a really usable impl
        expect(pool.getDatasourceClassname()).andStubReturn(DataSource.class.getName());
        expect(pool.getDriverClassname()).andStubReturn(null);

        replay(pool);
        return pool;
    }


    private void updateMock(final JdbcConnectionPool pool, final MockUpdater action) {
        // EasyMock 4.1: resets always to created state before first replay, but
        // forgets updated expectations!
        reset(pool);
        action.accept(pool);
        replay(pool);
    }


    private static JdbcConnectionPoolConstraint createAnnotation(final ConnectionPoolErrorMessages message) {
        return new JdbcConnectionPoolConstraint() {

            @Override
            public Class<JdbcConnectionPoolConstraint> annotationType() {
                return JdbcConnectionPoolConstraint.class;
            }


            @Override
            public ConnectionPoolErrorMessages value() {
                return message;
            }


            @Override
            public Class<? extends Payload>[] payload() {
                // not needed
                return null;
            }


            @Override
            public String message() {
                return "this is a mock with no message";
            }


            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }
        };
    }

    private interface MockUpdater extends Consumer<JdbcConnectionPool> {
    }
}
