package fish.payara.embedded;
 
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EmbeddedDeploymentTest {
 
    @Test
    public void testDeployCluster() throws IOException {
        String filename = "testEmbeddedDeploymentLog.xml";
        String currentPath = new java.io.File(".").getCanonicalPath();
        try {
            GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
            GlassFishProperties glassfishProperties = new GlassFishProperties();
            glassfishProperties.setPort("http-listener", 9080);
            glassfishProperties.setPort("https-listener", 9845);
            GlassFish glassfish = runtime.newGlassFish(glassfishProperties);

            FileHandler handler = new FileHandler(filename, false);

            // Set the log levels. For example, set 'deployment' and 'server' log levels to INFO
            Logger logger = Logger.getLogger("");
            logger.getHandlers()[0].setLevel(Level.INFO);
            logger.removeHandler(logger.getHandlers()[0]);
            logger.addHandler(handler);


            glassfish.start();
            glassfish.getDeployer().deploy(new File("./src/test/resources/clusterjsp.war"));

            // Close the logger
            logger.removeHandler(handler);
            handler.close();
            glassfish.stop();
            glassfish.dispose();

            boolean result = parseLog(currentPath + "/" + filename);
            assert (result);

            // clean up the log file
            new File(filename).delete();

        } catch (GlassFishException ex) {
            ex.printStackTrace();
        }
    }

    public boolean parseLog(String path) throws IOException {
        boolean testSuccess = false;
        try {
            //creating a constructor of file class and parsing an XML file
            File file = new File(path);
            //an instance of factory that gives a document builder
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            //an instance of builder to parse the specified xml file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("record");
            // nodeList is not iterable, so we are using a for loop
            for (int itr = 0; itr < nodeList.getLength(); itr++) {
                Node node = nodeList.item(itr);
                Element eElement = (Element) node;
                String logLevel = eElement.getElementsByTagName("level").item(0).getTextContent();
                String logMessage = eElement.getElementsByTagName("message").item(0).getTextContent();

                if (!logLevel.equals("INFO")) {
                    System.out.println(logLevel + " - " + logMessage);
                }
                if (logMessage.contains("clusterjsp was successfully deployed")) {
                    System.out.println("RESULT = clusterjsp was successfully deployed");
                    testSuccess = true;
                }
            }
        } catch (IOException | ParserConfigurationException | DOMException | SAXException e) {
            e.printStackTrace();
        }
        return testSuccess;
    }
 
}
