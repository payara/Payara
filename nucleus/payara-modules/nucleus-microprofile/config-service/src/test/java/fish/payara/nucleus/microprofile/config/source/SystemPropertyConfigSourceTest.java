/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.microprofile.config.source;

import java.util.Map;
import java.util.Properties;
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
public class SystemPropertyConfigSourceTest {
    
    public SystemPropertyConfigSourceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getProperties method, of class SystemPropertyConfigSource.
     */
    @Test
    public void testGetProperties() {
        System.out.println("getProperties");
        SystemPropertyConfigSource instance = new SystemPropertyConfigSource(true);
        Properties expResult = System.getProperties();
        Map<String, String> result = instance.getProperties();
        for (String stringPropertyName : expResult.stringPropertyNames()) {
            assertEquals(expResult.getProperty(stringPropertyName), instance.getValue(stringPropertyName));
        }
        
    }

    /**
     * Test of getOrdinal method, of class SystemPropertyConfigSource.
     */
    @Test
    public void testGetOrdinal() {
        System.out.println("getOrdinal");
        SystemPropertyConfigSource instance = new SystemPropertyConfigSource(true);
        int expResult = 400;
        int result = instance.getOrdinal();
        assertEquals(expResult, result);
    }

    /**
     * Test of getName method, of class SystemPropertyConfigSource.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        SystemPropertyConfigSource instance = new SystemPropertyConfigSource(true);
        String expResult = "SystemProperty";
        String result = instance.getName();
        assertEquals(expResult, result);
    }
    
    @Test
    public void testAddProperty() {
        SystemPropertyConfigSource instance = new SystemPropertyConfigSource(true);
        assertNull(instance.getValue("NoProperty"));
        System.setProperty("NoProperty", "test");
        assertEquals("test", instance.getValue("NoProperty"));
    }
    
}
