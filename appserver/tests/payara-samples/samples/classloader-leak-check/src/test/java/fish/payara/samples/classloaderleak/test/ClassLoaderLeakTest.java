/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.samples.classloaderleak.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Cuba Stanley
 */
public class ClassLoaderLeakTest {
    
    public String baseURL = "https://localhost:8080/api/";

    @Test
    public void testClassLoaderInstanceCount() throws MalformedURLException, IOException {
        
        int initialInstanceCount = 0;
        int postInstanceCount = 1; // Assume initial failure base state
        
        // Get the initial value for instance count
        URL connectionURL = new URL(baseURL + "instance-count/previous");
        HttpURLConnection connection = (HttpURLConnection)connectionURL.openConnection();
        try {
            initialInstanceCount = readResponseValue(connection);
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        // Get the value after running all tests
        connectionURL = new URL(baseURL + "instance-count/new");
        connection = (HttpURLConnection)connectionURL.openConnection();
        try {
            postInstanceCount = readResponseValue(connection);
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        assertTrue(postInstanceCount <= initialInstanceCount);
        
    }
    
    public int readResponseValue(HttpURLConnection connection) throws ProtocolException, IOException {
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(2500);
        BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = responseReader.readLine()) != null) {
            content.append(inputLine);
        }
        responseReader.close();
        
        return Integer.parseInt(content.toString());
    }
}
