/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
import jakarta.annotation.sql.DataSourceDefinition;
import org.glassfish.internal.deployment.ClassHierarchy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for DataSourceDefinition processing in DataSourceDefinitionHandler
 * @author jonathan coustick
 */
public class DataSourceDefinitionTest {

    @DataSourceDefinition(name = "${ENV=DB_NAME}", className = "${ENV=DB_CLASS_NAME}", description = "${ENV=DB_DESCRIPTION}",
            url = "${ENV=DB_URL}", serverName = "${ENV=DB_SERVER_NAME}", databaseName = "${ENV=DB_DATABASE_NAME}",
            user = "${ENV=DB_USER}", password = "${ENV=DB_PASSWORD}",
            properties = {"property1=${ENV=DB_PROPERTY1}", "property2=${ENV=DB_PROPERTY2}"})
    private static class EnvironmentVariables {
    }

    @DataSourceDefinition(name = "java:app/jdbc/demo", className = "org.h2.jdbcx.JdbcDataSource",
            url = "http://database:5432/demo", serverName = "localhost")
    private static class UrlAndServerName {
    }

    @DataSourceDefinition(name = "java:app/jdbc/demo", className = "org.h2.jdbcx.JdbcDataSource", serverName = "localhost")
    private static class ServerNameOnly {
    }

    private static IndexView index;

    @BeforeClass
    public static void indexDefinitions() throws IOException {
        index = Index.of(DataSourceDefinition.class, EnvironmentVariables.class, UrlAndServerName.class, ServerNameOnly.class);
    }

    private static AnnotationInstance definition(Class<?> annotatedClass) {
        return ClassHierarchy.annotation(index, index.getClassByName(annotatedClass), DataSourceDefinition.class);
    }

    @Test
    public void test_environment_variable_expansion_works() throws Exception {
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

        DataSourceDefinitionDescriptor descriptor = handler.createDescriptor(index, definition(EnvironmentVariables.class));

        Assert.assertEquals(url,descriptor.getUrl());
        Assert.assertNull(descriptor.getServerName()); // because url is set
        Assert.assertEquals(className,descriptor.getClassName());
        Assert.assertEquals(name,descriptor.getName());
        Assert.assertEquals(description,descriptor.getDescription());
        Assert.assertEquals(user,descriptor.getUser());
        Assert.assertEquals(password,descriptor.getPassword());
        Assert.assertEquals(databaseName,descriptor.getDatabaseName());
        Assert.assertEquals(property1,descriptor.getProperty("property1"));
        Assert.assertEquals(property2,descriptor.getProperty("property2"));
    }

    /**
     * Test to ensure that if the URL has been set, it will override the default serverName of localhost
     * and cause that to be set to null.
     */
    @Test
    public void testServerAndURL() {
        DataSourceDefinitionHandler handler = new DataSourceDefinitionHandler();

        //Check url overrides serverName and sets it to null
        DataSourceDefinitionDescriptor descriptor = handler.createDescriptor(index, definition(UrlAndServerName.class));

        Assert.assertNull(descriptor.getServerName());

        //Check if url is not set then serverName is left as-is
        descriptor = handler.createDescriptor(index, definition(ServerNameOnly.class));

        Assert.assertEquals("localhost", descriptor.getServerName());
    }
}
