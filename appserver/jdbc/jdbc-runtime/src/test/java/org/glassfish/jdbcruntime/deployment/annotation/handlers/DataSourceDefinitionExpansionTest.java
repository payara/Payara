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
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.sql.DataSourceDefinition;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for variable expansion in DataSourceDefinition processing in DataSourceDefinitionHandler
 * @author jonathan coustick
 */
public class DataSourceDefinitionExpansionTest {
    /**
     * Test to ensure that if the URL has been set, it will override the default serverName of localhost
     * and cause that to be set to null.
     */
    @Test
    public void testServerAndURL() throws Exception {
        String url = "url";
        String serverName = "server name";
        String className = "class name";
        String name = "name";
        String description = "description";
        String user = "user";
        String password = "password";
        String databaseName = "database name";
        String property1 = "property 1";
        String property2 = "property 2";

        Map<String,String> env = new HashMap<>();
        env.put("DB_URL", url);
        env.put("DB_SERVER_NAME", serverName);
        env.put("DB_CLASS_NAME", className);
        env.put("DB_NAME", name);
        env.put("DB_DESCRIPTION", description);
        env.put("DB_USER", user);
        env.put("DB_PASSWORD", password);
        env.put("DB_DATABASE_NAME", databaseName);
        env.put("DB_PROPERTY1", property1);
        env.put("DB_PROPERTY2", property2);
        EnvironmentUtil.setEnv(env);
        DataSourceDefinitionHandler handler = new DataSourceDefinitionHandler();
        
        //Check url overrides serverName and sets it to null
        DataSourceDefinition definition = new DataSourceDefinitionImpl() {
            @Override
            public String url() {
                return "${ENV=DB_URL}";
            }

            @Override
            public String serverName() {
                return "${ENV=DB_SERVER_NAME}";
            }

            @Override
            public String className() {
                return "${ENV=DB_DRIVER}";
            }

            @Override
            public String name() {
                return "${ENV=DB_NAME}";
            }

            @Override
            public String description() {
                return "${ENV=DB_DESCRIPTION}";
            }

            @Override
            public String user() {
                return "${ENV=DB_USER}";
            }

            @Override
            public String password() {
                return "${ENV=DB_PASSWORD}";
            }

            @Override
            public String databaseName() {
                return "${ENV=DB_DATABASE}";
            }

            @Override
            public String[] properties() {
                return new String[] {"property1=${DB_PROPERTY1}","property1=${DB_PROPERTY2}"};
            }
        };
        DataSourceDefinitionDescriptor descriptor = handler.createDescriptor(definition);
        Assert.assertEquals(url,descriptor.getUrl());
        Assert.assertNull(descriptor.getServerName()); // because url is set
        Assert.assertEquals(className,descriptor.getClassName());
        Assert.assertEquals(name,descriptor.getName());
        Assert.assertEquals(description,descriptor.getDescription());
        Assert.assertEquals(user,descriptor.getUser());
        Assert.assertEquals(password,descriptor.getPassword());
        Assert.assertEquals(databaseName,descriptor.getDatabaseName());
        Assert.assertEquals(property1,descriptor.getProperty("DB_PROPERTY1"));
        Assert.assertEquals(property2,descriptor.getProperty("DB_PROPERTY2"));

        definition = new DataSourceDefinitionImpl() {
            @Override
            public String url() {
                return null;
            }
            @Override
            public String serverName() {
                return "${ENV=DB_SERVER_NAME}";
            }
        };

        descriptor = handler.createDescriptor(definition);
        Assert.assertEquals(serverName,descriptor.getServerName());

    }
}
