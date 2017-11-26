/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.microprofile.config.source;

import java.util.Map;
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
public class EnvironmentConfigSourceTest {
    
    public EnvironmentConfigSourceTest() {
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
     * Test of getProperties method, of class EnvironmentConfigSource.
     */
    @Test
    public void testGetProperties() {
        System.out.println("getProperties");
        EnvironmentConfigSource instance = new EnvironmentConfigSource();
        Map<String, String> result = instance.getProperties();
        Map<String,String> environment = System.getenv();
        for (String string : environment.keySet()) {
            assertEquals(environment.get(string), instance.getValue(string));
        }

    }

    /**
     * Test of getOrdinal method, of class EnvironmentConfigSource.
     */
    @Test
    public void testGetOrdinal() {
        System.out.println("getOrdinal");
        EnvironmentConfigSource instance = new EnvironmentConfigSource();
        int expResult = 300;
        int result = instance.getOrdinal();
        assertEquals(expResult, result);
    }

    /**
     * Test of getName method, of class EnvironmentConfigSource.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        EnvironmentConfigSource instance = new EnvironmentConfigSource();
        String expResult = "Environment";
        String result = instance.getName();
        assertEquals(expResult, result);
    }
    
}
