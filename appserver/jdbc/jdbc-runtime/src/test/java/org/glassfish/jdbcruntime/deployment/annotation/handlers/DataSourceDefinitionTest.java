/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.jdbcruntime.deployment.annotation.handlers;

import com.sun.enterprise.deployment.DataSourceDefinitionDescriptor;
import java.lang.annotation.Annotation;
import javax.annotation.sql.DataSourceDefinition;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for DataSourceDefinition processing in DataSourceDefinitionHandler
 * @author jonathan coustick
 */
public class DataSourceDefinitionTest {
 
    /**
     * Test to ensure that if the URL has been set, it will override the default serverName of localhost
     * and cause that to be set to null.
     */
    @Test
    public void testServerAndURL() {
        DataSourceDefinitionHandler handler = new DataSourceDefinitionHandler();
        
        //Check url overrides serverName and sets it to null
        DataSourceDefinition definition = new DataSourceDefinitionImpl() {
            @Override
            public String url() {
                return "http://database:5432/demo";
            }

            @Override
            public String serverName() {
                return "localhost";
            }
            
        };
        DataSourceDefinitionDescriptor descriptor = handler.createDescriptor(definition);
        Assert.assertNull(descriptor.getServerName());
        
        
        //Check if url is not set then serverName is left as-is
        definition = new DataSourceDefinitionImpl() {
            @Override
            public String url() {
                return null;
            }

            @Override
            public String serverName() {
                return "localhost";
            }
        };
        descriptor = handler.createDescriptor(definition);
        Assert.assertEquals("localhost", descriptor.getServerName());
    }
    
    abstract class DataSourceDefinitionImpl implements DataSourceDefinition {
        @Override
            public String name() {
                return null;
            }

            @Override
            public String className() {
                return null;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String user() {
                return null;
            }

            @Override
            public String password() {
                return null;
            }

            @Override
            public String databaseName() {
                return null;
            }

            @Override
            public int portNumber() {
                return -1;
            }

            @Override
            public int isolationLevel() {
                return -1;
            }

            @Override
            public boolean transactional() {
                return false;
            }

            @Override
            public int initialPoolSize() {
                return -1;
            }

            @Override
            public int maxPoolSize() {
                return -1;
            }

            @Override
            public int minPoolSize() {
                return -1;
            }

            @Override
            public int maxIdleTime() {
                return -1;
            }

            @Override
            public int maxStatements() {
                return -1;
            }

            @Override
            public String[] properties() {
                return null;
            }

            @Override
            public int loginTimeout() {
                return -1;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return DataSourceDefinition.class;
            }
    }
            
            
}
