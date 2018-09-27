package fish.payara.admin.rest.streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class StreamsTest {
    
    private ByteArrayOutputStream os;
    private StreamWriter jsonStreamWriter;
    private StreamWriter xmlStreamWriter;
    
    @BeforeMethod
    public void initialise() throws XMLStreamException {
        os = new ByteArrayOutputStream();
        jsonStreamWriter = new JsonStreamWriter(os);
        xmlStreamWriter = new XmlStreamWriter(os);
    }
    
    @Test
    public void jsonStreamTest() throws Exception {
        jsonStreamWriter.writeStartDocument();
        jsonStreamWriter.writeStartObject("object");
        jsonStreamWriter.writeAttribute("test", "value");
        jsonStreamWriter.writeAttribute("test2", "value");
        jsonStreamWriter.writeStartArray("array");
        jsonStreamWriter.writeStartObject("anything");
        jsonStreamWriter.writeAttribute("single", "index");
        jsonStreamWriter.writeEndObject();
        jsonStreamWriter.writeEndArray();
        jsonStreamWriter.writeEndObject();
        jsonStreamWriter.writeEndDocument();
        jsonStreamWriter.close();
        
        assertEquals(new String(os.toByteArray()), "{\"object\":{\"test\":\"value\",\"test2\":\"value\",\"array\":[{\"single\":\"index\"}]}}");
    }
    
    @Test
    public void xmlStreamTest() throws Exception {
        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartObject("object");
        xmlStreamWriter.writeAttribute("test", "value");
        xmlStreamWriter.writeAttribute("test2", "value");
        xmlStreamWriter.writeStartArray("array");
        xmlStreamWriter.writeStartObject("anything");
        xmlStreamWriter.writeAttribute("single", "index");
        xmlStreamWriter.writeEndObject();
        xmlStreamWriter.writeEndArray();
        xmlStreamWriter.writeEndObject();
        xmlStreamWriter.writeEndDocument();
        xmlStreamWriter.close();
        
        assertEquals(new String(os.toByteArray()), "<?xml version='1.0' encoding='UTF-8'?><object test=\"value\" test2=\"value\"><anything single=\"index\"/></object>");
    }
    
    @AfterMethod
    public void destroy() throws IOException {
        os.close();
    }
    
}
