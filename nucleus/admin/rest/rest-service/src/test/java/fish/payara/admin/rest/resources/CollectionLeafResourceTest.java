/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.admin.rest.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.admin.rest.resources.CollectionLeafResource;
import org.jvnet.hk2.config.TransactionFailure;

import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author asroth
 */
public class CollectionLeafResourceTest {
    
    public CollectionLeafResourceTest() {
    }

    @Test
    public void testProcessData() throws TransactionFailure{      
        Map<String, String> data1 = new HashMap<>();
        data1.put("-Dproduct.name=XXX", "");
     
        Map<String, String> data2 = new HashMap<>();
        data2.put("-Dproduct.name", "");
        
        Map<String, String> data3 = new HashMap<>();
        data3.put("-Dproduct.name", "XXX");
        
        Map<String, String> data4 = new HashMap<>();
        data4.put("-client", "");
        
        class Wrapper extends CollectionLeafResource{
           public Map<String, String> getProcessedData(Map<String, String> data){
               return processData(data, true);
           }
        };
        
        Wrapper wrapper = new Wrapper();
        
        assertEquals("-Dproduct.name=XXX", wrapper.getProcessedData(data1).get("id"));
        assertEquals("-Dproduct.name=", wrapper.getProcessedData(data2).get("id"));
        assertEquals("-Dproduct.name=XXX", wrapper.getProcessedData(data3).get("id"));
        assertEquals("-client=", wrapper.getProcessedData(data4).get("id"));
    }   
}
