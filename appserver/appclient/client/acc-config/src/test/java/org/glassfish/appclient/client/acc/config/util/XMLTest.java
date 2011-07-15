/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.appclient.client.acc.config.util;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.EntityResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.glassfish.appclient.client.acc.config.ClientContainer;
import org.glassfish.appclient.client.acc.config.TargetServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tjquinn
 */
public class XMLTest {

    private static final String[] SAMPLE_XML_PATH = {"/sun-acc.xml", "/glassfish-acc.xml"};
    
    private static final String FIRST_HOST = "glassfish.dev.java.net";
    private static final int FIRST_PORT = 3701;
    private static final String SECOND_HOST = "other.dev.java.net";
    private static final int SECOND_PORT = 4701;

    private static final String FIRST_PROP_NAME = "firstProp";
    private static final String FIRST_PROP_VALUE = "firstValue";

    private static final String SECOND_PROP_NAME = "secondProp";
    private static final String SECOND_PROP_VALUE = "secondValue";

    public XMLTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testProps() throws Exception {
        System.out.println("testProps");
        for (String sampleXMLPath : SAMPLE_XML_PATH) {
            System.out.println("  Testing with " + sampleXMLPath);
            ClientContainer cc = readConfig(sampleXMLPath);
            Properties props = XML.toProperties(cc.getProperty());
            assertEquals("property value mismatch for first property from " + sampleXMLPath, 
                    FIRST_PROP_VALUE, props.getProperty(FIRST_PROP_NAME));
            assertEquals("property value mismatch for second property from " + sampleXMLPath, 
                    SECOND_PROP_VALUE, props.getProperty(SECOND_PROP_NAME));
        }
    }

    @Test
    public void testReadSampleXML() throws Exception {
        System.out.println("testReadSampleXML");
        for (String sampleXMLPath : SAMPLE_XML_PATH) {
            System.out.println("  Testing with " + sampleXMLPath);
            ClientContainer cc = readConfig(sampleXMLPath);
            List<TargetServer> servers = cc.getTargetServer();


            assertTrue("target servers did not read correctly from " + sampleXMLPath,
                    servers.get(0).getAddress().equals(FIRST_HOST) &&
                    servers.get(0).getPort().equals(FIRST_PORT) &&
                    servers.get(1).getAddress().equals(SECOND_HOST) &&
                    servers.get(1).getPort() == SECOND_PORT
                );
        }

    }

    private static ClientContainer readConfig(final String configPath) 
            throws JAXBException, FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        ClientContainer result = null;
        InputStream is = XMLTest.class.getResourceAsStream(configPath);
        try {
            if (is == null) {
                fail("cannot locate test file " + configPath);
            }
            JAXBContext jc = JAXBContext.newInstance(ClientContainer.class );

            Unmarshaller u = jc.createUnmarshaller();

            final SAXSource src = setUpToUseLocalDTDs(is);

            result = (ClientContainer) u.unmarshal(src);

            return result;
        } finally {
            is.close();
        }
    }

    private static SAXSource setUpToUseLocalDTDs(final InputStream is)
            throws ParserConfigurationException, SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        SAXParser saxParser = parserFactory.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        InputSource inSrc = new InputSource(is);
        xmlReader.setEntityResolver(new LocalEntityResolver());
        SAXSource saxSource = new SAXSource(xmlReader,inSrc);
        return saxSource;
    }

    /**
     * Resolves entity references against local files if possible.
     * <p>
     * This is here primarily to allow us to find the local copy of the
     * glassfish- or sun-application-client DTD without going out over the network.
     */
    private static class LocalEntityResolver implements EntityResolver {

        private static enum ACC_INFO {
            SUN_ACC(
                "-//Sun Microsystems Inc.//DTD Application Server 8.0 Application Client Container//EN",
                "dtds/sun-application-client-container_1_2.dtd"),
            GLASSFISH_ACC(
                "-//GlassFish.org//DTD GlassFish Application Server 3.1 Application Client Container//EN",
                "dtds/glassfish-application-client-container_1_3.dtd");
            
            private static final String SYSTEM_ID_PREFIX = "http://glassfish.org/";
            
            private final String publicID;
            private final String systemIDSuffix;
            private final URI uri;
            
            ACC_INFO(final String publicID, final String systemIDSuffix) {
                this.publicID = publicID;
                this.systemIDSuffix = systemIDSuffix;
                uri = URI.create(LOCAL_PATH_PREFIX + systemIDSuffix);
            }
        }
        
        private static final String SUN_ACC_PUBLIC_ID =
                "-//Sun Microsystems Inc.//DTD Application Server 8.0 Application Client Container//EN";
        private static final String GLASSFISH_ACC_PUBLIC_ID =
                "-//GlassFish.org//DTD GlassFish Application Server 3.1 Application Client Container//EN";
        private static final String SYSTEM_ID_PREFIX =
                "http://glassfish.org/";
        private static final URI SUN_ACC_SYSTEM_ID_URI = 
                URI.create(SYSTEM_ID_PREFIX + "dtds/sun-application-client-container_1_2.dtd");
        private static final URI GLASSFISH_ACC_SYSTEM_ID_URI = 
                URI.create(SYSTEM_ID_PREFIX + "dtds/glassfish-application-client-container_1_3.dtd");
        
        private static final String LOCAL_PATH_PREFIX = "/glassfish/lib/";

        private static final Map<String,String> publicIdToLocalPathMap =
                initPublicIdToLocalPathMap();

        private static Map<String,String> initPublicIdToLocalPathMap() {
            final Map<String,String> result = new HashMap<String,String>();
            for (ACC_INFO accInfo : ACC_INFO.values()) {
                result.put(accInfo.publicID, accInfo.uri.toASCIIString());
            }
            return result;
        }

        /*
         * The deployment/dtds module should be in the dependencies, so the
         * entries in that JAR should be accessible on the class path.
         */
        @Override
        public InputSource resolveEntity(String publicId, String systemId){
            InputSource result = null;
            final String localPath = publicIdToLocalPathMap.get(publicId);
            if (localPath == null) {
                return null;
            }

//            showClassPath(Thread.currentThread().getContextClassLoader(), "context");
//            showClassPath(getClass().getClassLoader(), "class");
//            showClassPath(ClassLoader.getSystemClassLoader(), "system");

            /*
             * The next line works because the pom for this project extracted
             * the DTD from the deployment/dtds dtds.zip file and placed it into
             * a temporary directory which we used for generating the JAXB classes.
             * The pom also adds that same temporary directory to the test-time
             * class path, which allows this class to find that locally-extracted
             * copy of the DTD.
             */
            final InputStream is = getClass().getResourceAsStream(localPath);
            if (is == null) {
                System.err.println("Found map entry for public Id but could not find the local path " + localPath);
                return null;
            }
            result = new InputSource(is);
            return result;
        }

//        private void showClassPath(ClassLoader cl, final String title) {
//            System.err.println("URLs of loaders for the " + title + " class loader");
//            while (cl != null) {
//                if (cl instanceof URLClassLoader) {
//                    System.err.println("  " + cl.toString());
//                    URLClassLoader urlCL = URLClassLoader.class.cast(cl);
//                    System.err.println();
//                    for (URL url : urlCL.getURLs()) {
//                        System.err.println("    " + url.toExternalForm());
//                    }
//                }
//                cl = cl.getParent();
//            }
//            System.err.println();
//        }

    }
}
