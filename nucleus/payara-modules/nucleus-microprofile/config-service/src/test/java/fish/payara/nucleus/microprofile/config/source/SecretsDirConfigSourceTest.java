/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author steve
 */
public class SecretsDirConfigSourceTest {
    
    public SecretsDirConfigSourceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws IOException {
        testDirectory = Files.createTempDirectory("microprofile-config-test");
        // create a couple of test files
        Path file1 = Paths.get(testDirectory.toString(), "property1");
        Path file2 = Paths.get(testDirectory.toString(), "property2");
        file1 = Files.createFile(file1);
        Files.write(file1, "value1".getBytes());
        file2 = Files.createFile(file2);
        Files.write(file2, "value2".getBytes());
    }
    
    @After
    public void tearDown() throws IOException {
        for (File file : testDirectory.toFile().listFiles()) {
            file.delete();
        }
        Files.delete(testDirectory);
    }

    Path testDirectory;
    
    /**
     * Test of getProperties method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetProperties() {
        SecretsDirConfigSource instance = new SecretsDirConfigSource(testDirectory);
        Map<String, String> result = instance.getProperties();
        assertEquals(2, result.size());

    }

    /**
     * Test of getPropertyNames method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetPropertyNames() {
        SecretsDirConfigSource instance = new SecretsDirConfigSource(testDirectory);
        Set<String> result = instance.getPropertyNames();
        assertEquals(2, result.size());
        assertTrue(result.contains("property1"));
        assertTrue(result.contains("property2"));
    }

    /**
     * Test of getValue method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetValue() {
        String property = "";
        SecretsDirConfigSource instance = new SecretsDirConfigSource(testDirectory);
        String expResult = "value1";
        String result = instance.getValue("property1");
        assertEquals(expResult, result);
        expResult = "value2";
        result = instance.getValue("property2");
        assertEquals(expResult, result);
    }

    /**
     * Test of getName method, of class SecretsDirConfigSource.
     */
    @Test
    public void testGetName() {
        SecretsDirConfigSource instance = new SecretsDirConfigSource(testDirectory);
        String expResult = "Secrets Directory";
        String result = instance.getName();
        assertEquals(expResult, result);
    }
    
    /**
     * Test the changed Property
     * @throws java.io.IOException
     */
    @Test
    public void testChangeProperty() throws IOException, InterruptedException {
        SecretsDirConfigSource instance = new SecretsDirConfigSource(testDirectory);
        String value = instance.getValue("property1");
        assertEquals("value1", value);
        // change the file
        Path file1 = Paths.get(testDirectory.toString(), "property1");
        Thread.sleep(1000);
        System.out.println("Test measured last modified time before write " + Files.getLastModifiedTime(file1));
        Files.write(file1, "value-changed".getBytes());
        System.out.println("Test measured last modified time after write" + Files.getLastModifiedTime(file1));
        value = instance.getValue("property1");
        assertEquals("value-changed", value);
        // clean up
        Files.write(file1, "value1".getBytes());
    }
    
    /**
     * Tests getting a new property as the file has now appeared
     */
    @Test
    public void testNewFile() throws IOException {
        SecretsDirConfigSource instance = new SecretsDirConfigSource(testDirectory);
        String value = instance.getValue("property-new");
        assertNull(value);
        // change the file
        Path file1 = Paths.get(testDirectory.toString(), "property-new");
        Files.write(file1, "newValue".getBytes());
        value = instance.getValue("property-new");
        assertEquals("newValue", value);
        // clean up
        Files.delete(file1);
    }
    
    @Test
    public void testBadDirectoryNoBlowUp() {
        SecretsDirConfigSource instance = new SecretsDirConfigSource(Paths.get(testDirectory.toString(), "FOOBLE"));
        assertNull(instance.getValue("BILLY"));       
    }
}
