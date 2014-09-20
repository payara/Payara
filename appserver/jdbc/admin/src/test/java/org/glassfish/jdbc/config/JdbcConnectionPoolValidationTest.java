/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jdbc.config;

import org.glassfish.jdbc.config.JdbcConnectionPool;

import java.beans.PropertyVetoException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;


import javax.validation.ConstraintViolationException;

/**
 *
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 */
public class JdbcConnectionPoolValidationTest extends ConfigApiTest {

    private JdbcConnectionPool pool = null;
    private static final String NAME = "test"; //same as the one in JdbcConnectionPoolValidation.xml

    public JdbcConnectionPoolValidationTest() {
    }

    @Override
    public String getFileName() {
        return ("JdbcConnectionPoolValidation");
    }

    @Before
    public void setUp() {
        pool = super.getHabitat().getService(JdbcConnectionPool.class, NAME);
    }

    @After
    public void tearDown() {
        pool = null;
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test (expected= ConstraintViolationException.class)
    public void testBooleanDoesNotTakeInteger1() throws Throwable {
        try {
            ConfigSupport.apply(new SingleConfigCode<JdbcConnectionPool>() {
                public Object run(JdbcConnectionPool jdbcConnectionPool) throws PropertyVetoException, TransactionFailure {
                    jdbcConnectionPool.setConnectionLeakReclaim("123"); //this method should only take boolean;
                    return null;
                }
            }, pool);

        } catch(TransactionFailure e) {
            throw e.getCause().getCause();
        }
    }


    @Test
    public void testBooleanTakesTrueFalse() {
        try {
            pool.setSteadyPoolSize("true"); //this only takes a boolean
            pool.setSteadyPoolSize("false"); //this only takes a boolean
            pool.setSteadyPoolSize("TRUE"); //this only takes a boolean
            pool.setSteadyPoolSize("FALSE"); //this only takes a boolean
            pool.setSteadyPoolSize("FALSE"); //this only takes a boolean
        } catch(PropertyVetoException pv) {
            //ignore?
        }
    }
}
