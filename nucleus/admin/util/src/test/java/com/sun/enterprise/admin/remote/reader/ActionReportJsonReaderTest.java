/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.enterprise.admin.remote.reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.glassfish.api.ActionReport;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mmares
 */
public class ActionReportJsonReaderTest {
    
    private ActionReportJsonReader reader = new ActionReportJsonReader();
    
    public ActionReportJsonReaderTest() {
    }
    
    private ActionReport unmarshall(String json) throws IOException {
        return reader.readFrom(null, null, null, null, null, new ByteArrayInputStream(json.getBytes("UTF-8")));
    }

    @Test
    public void testReadFrom() throws Exception {
        ActionReport ar = unmarshall("{\"message\":\"First message in First report\\nFirst Message in Second Report\",\"command\":\"Some description\",\"exit_code\":\"SUCCESS\",\"properties\":"
                + "{\"AR1-MSG1-PROP2\":\"1.1.2.\",\"AR1-MSG1-PROP1\":\"1.1.1.\"},\"extraProperties\":{\"EP1-PROP1\":\"1.1\",\"EP1-PROP2\":\"1.2\"},\"children\":"
                + "[{\"message\":\"Second message in First report\",\"properties\":{\"AR1-MSG2-PROP2\":\"1.2.2.\",\"AR1-MSG2-PROP1\":\"1.2.1.\"},\"children\":"
                + "[{\"message\":\"Third message in First report\",\"properties\":{\"AR1-MSG3-PROP2\":\"1.3.2.\",\"AR1-MSG3-PROP1\":\"1.3.1.\"}}]},"
                + "{\"message\":\"Fourth message in First report\",\"properties\":{\"AR1-MSG4-PROP2\":\"1.4.2.\",\"AR1-MSG4-PROP1\":\"1.4.1.\"}}],"
                + "\"subReports\":[{\"message\":\"First Message in Second Report\",\"command\":\"Description 2\",\"exit_code\":\"WARNING\",\"children\":"
                + "[{\"message\":\"Second Message in Second Report\",\"properties\":{\"AR2-MSG2-PROP1\":\"2.2.1.\"}}]}],\"top_message\":\"First message in First report\"}");
        assertNotNull(ar);
    }
}
