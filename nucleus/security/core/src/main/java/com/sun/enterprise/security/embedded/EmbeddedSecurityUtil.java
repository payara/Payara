/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.embedded;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.EmbeddedSecurity;
import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.server.ServerEnvironmentImpl;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;
import org.jvnet.hk2.config.types.Property;

/**
 * Utility file to copy the security related config files
 * from the passed non-embedded instanceDir to the embedded
 * server instance's config.
 * This is a service that is protected. This implements
 * the Contract EmbeddedSecurity
 * 
 * @author Nithya Subramanian
 */

@Service
@Singleton
public class EmbeddedSecurityUtil implements EmbeddedSecurity {

    private static final Logger _logger = SecurityLoggerInfo.getLogger();
    
    public void copyConfigFiles(ServiceLocator habitat, File fromInstanceDir, File domainXml) {
        //For security reasons, permit only an embedded server instance to carry out the copy operations
        ServerEnvironment se = habitat.getService(ServerEnvironment.class);
        if (!isEmbedded(se)) {
            return;
        }

        if ((fromInstanceDir == null) || (domainXml == null)) {
            throw new IllegalArgumentException("Null inputs");
        }

        File toInstanceDir = habitat.<ServerEnvironmentImpl>getService(ServerEnvironmentImpl.class).getInstanceRoot();

        List<String> fileNames = new ArrayList<String>();

        //Handling the exception here, since it is causing CTS failure - CR 6981191

        try {

            //Add FileRealm keyfiles to the list
            fileNames.addAll(new EmbeddedSecurityUtil().new DomainXmlSecurityParser(domainXml).getAbsolutePathKeyFileNames(fromInstanceDir));

            //Add keystore and truststore files

            // For the embedded server case, will the system properties be set in case of multiple embedded instances?
            //Not sure - so obtain the other files from the usual locations instead of from the System properties

            String keyStoreFileName = fromInstanceDir + File.separator + "config" + File.separator + "keystore.jks";
            String trustStoreFileName = fromInstanceDir + File.separator + "config" + File.separator + "cacerts.jks";

            fileNames.add(keyStoreFileName);
            fileNames.add(trustStoreFileName);

            //Add login.conf and security policy

            String loginConf = fromInstanceDir + File.separator + "config" + File.separator + "login.conf";
            String secPolicy = fromInstanceDir + File.separator + "config" + File.separator + "server.policy";

            fileNames.add(loginConf);
            fileNames.add(secPolicy);

            File toConfigDir = new File(toInstanceDir, "config");
            if (!toConfigDir.exists()) {
                if(!toConfigDir.mkdir()) {
                    throw new IOException();
                }
            }

            //Copy files into new directory
            for (String fileName : fileNames) {
                FileUtils.copyFile(new File(fileName), new File(toConfigDir, parseFileName(fileName)));
            }
        }catch(IOException e) {
            _logger.log(Level.WARNING, SecurityLoggerInfo.ioError, e);
        }catch(XMLStreamException e) {
            _logger.log(Level.WARNING, SecurityLoggerInfo.xmlStreamingError, e);
        }



    }

    public String parseFileName(String fullFilePath) {
        if (fullFilePath == null) {
            return null;
        }
        File file = new File(fullFilePath);
        return file.getName();
       
    }

    public boolean isEmbedded(ServerEnvironment se) {
        if (se.getRuntimeType() == RuntimeType.EMBEDDED) {
            return true;
        }
        return false;
    }

    public List<String> getKeyFileNames(SecurityService securityService) {
        List<String> keyFileNames = new ArrayList<String>();

        List<AuthRealm> authRealms = securityService.getAuthRealm();
        for (AuthRealm authRealm : authRealms) {
            String className = authRealm.getClassname();
            if ("com.sun.enterprise.security.auth.realm.file.FileRealm".equals(className)) {
                List<Property> props = authRealm.getProperty();
                for (Property prop : props) {
                    if ("file".equals(prop.getName())) {
                        keyFileNames.add(prop.getValue());
                    }
                }
            }
        }

        return keyFileNames;
    }

    //Inner class to parse the domainXml to obtain the keyfile names
    class DomainXmlSecurityParser {

        XMLStreamReader xmlReader;
        XMLInputFactory xif =
                (XMLInputFactory.class.getClassLoader() == null)
                ? XMLInputFactory.newInstance()
                : XMLInputFactory.newInstance(XMLInputFactory.class.getName(),
                XMLInputFactory.class.getClassLoader());


        private static final String AUTH_REALM = "auth-realm";
        private static final String CONFIG = "config";
        private static final String CLASSNAME = "classname";
        private static final String FILE_REALM_CLASS = "com.sun.enterprise.security.auth.realm.file.FileRealm";
        private static final String PROPERTY = "property";
        private static final String NAME = "name";
        private static final String VALUE = "value";
        private static final String FILE = "file";
        private static final String INSTANCE_DIR_PLACEHOLDER = "${com.sun.aas.instanceRoot}";

        DomainXmlSecurityParser(File domainXml) throws XMLStreamException, FileNotFoundException {
            xmlReader = xif.createXMLStreamReader(new FileReader(domainXml));

        }

        private String replaceInstanceDir(String fromInstanceDir, String keyFileName) {
            return StringUtils.replace(keyFileName, INSTANCE_DIR_PLACEHOLDER, fromInstanceDir);

        }
        //Obtain the keyfile names for the server-config (the first appearing config in domain.xml
        List<String> getAbsolutePathKeyFileNames(File fromInstanceDir) throws XMLStreamException {
            List<String> keyFileNames = new ArrayList<String>();
            while (skipToStartButNotPast(AUTH_REALM, CONFIG)) {
                String realmClass = xmlReader.getAttributeValue(null, CLASSNAME);
                if (realmClass.equals(FILE_REALM_CLASS)) {
                    while (skipToStartButNotPast(PROPERTY, AUTH_REALM)) {
                        if (FILE.equals(xmlReader.getAttributeValue(null, NAME))) {
                            String keyFileName = xmlReader.getAttributeValue(null, VALUE);
                            //Replace the Placeholder in the keyfile names
                            keyFileNames.add(replaceInstanceDir(fromInstanceDir.getAbsolutePath(),keyFileName ));

                        }
                    }
                }
            }
            return keyFileNames;
        }

        private boolean skipToStartButNotPast(String startName, String stopName) throws XMLStreamException {
            if (!StringUtils.ok(startName) || !StringUtils.ok(stopName)) {
                throw new IllegalArgumentException();
            }

            while (xmlReader.hasNext()) {
                xmlReader.next();
                // getLocalName() will throw an exception in many states.  Be careful!!
                if (xmlReader.isStartElement() && startName.equals(xmlReader.getLocalName())) {
                    return true;
                }
                if (xmlReader.isEndElement() && stopName.equals(xmlReader.getLocalName())) {
                    return false;
                }
            }
            return false;
        }
    }
}
