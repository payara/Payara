/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        Thread.sleep(100);
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
