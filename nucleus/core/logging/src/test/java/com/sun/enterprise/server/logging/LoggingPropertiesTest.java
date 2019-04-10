/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.server.logging;

import com.sun.common.util.logging.LoggingConfigImpl;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.glassfish.api.admin.FileMonitoring;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 * @author Susan Rai
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggingPropertiesTest {

    private final Properties props = new Properties();
    private static final String LOG_STANDARD_STREAMS_PROPERTY = "com.sun.enterprise.server.logging.GFFileHandler.logStandardStreams";
    private Map<String, String> properties;

    @Rule
    public TemporaryFolder tempLoggingfolder = new TemporaryFolder();
    private File loggingFile;

    @Mock
    private FileMonitoring fileMonitoring;
    private LoggingConfigImpl loggingConfigImpl;

    @Before
    public void initialise() throws IOException, NoSuchFieldException {
        loggingFile = tempLoggingfolder.newFile("logging.properties");
        loggingConfigImpl = new LoggingConfigImpl(loggingFile.getParentFile(), loggingFile.getParentFile());
        FieldSetter.setField(loggingConfigImpl, loggingConfigImpl.getClass().getDeclaredField("fileMonitoring"), fileMonitoring);
    }

    @Test
    public void setLoggingPropertiesTest() throws IOException {
        properties = new HashMap<>();
        properties.put(LOG_STANDARD_STREAMS_PROPERTY, "false");
        loggingConfigImpl.setLoggingProperties(properties);

        Map<String, String> newProperties = new HashMap<>();
        properties.put("hello", "world");
        loggingConfigImpl.setLoggingProperties(newProperties);
        loadProperties();

        assertEquals("Logging property doesn't match", props.get(LOG_STANDARD_STREAMS_PROPERTY), "false");
    }

    @Test
    public void deletePropertiesTest() throws IOException {
        properties = new HashMap<>();
        properties.put("test", "properties");
        loggingConfigImpl.setLoggingProperties(properties);
        loggingConfigImpl.deleteLoggingProperties(properties);
        loadProperties();

        assertNull("Property couldn't be deleted", props.get("test"));
    }

    @Test
    public void getLoggingPropertiesTest() throws IOException {
        loadProperties();
        props.remove(LOG_STANDARD_STREAMS_PROPERTY);
        closePropFile();
        // Should get the default value of logging properties
        properties = loggingConfigImpl.getLoggingProperties();

        assertEquals("Default logging properties were not set", properties.get(LOG_STANDARD_STREAMS_PROPERTY), "true");
    }

    @Test
    public void setLoggingProperty() throws IOException {
        loggingConfigImpl.setLoggingProperty("test.set.property", "true");
        loadProperties();

        assertNotNull("Logging property failed to set", props.get("test.set.property"));
    }

    private void loadProperties() throws FileNotFoundException, IOException {
        props.clear();
        try (InputStream fis = new BufferedInputStream(new FileInputStream(loggingFile))) {
            props.load(fis);
        }
    }

    private void closePropFile() throws FileNotFoundException, IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(loggingFile))) {
            props.store(os, "GlassFish logging.properties list");
            os.flush();
        }
    }
}
