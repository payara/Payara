/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.util;

import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.tools.verifier.util.LogDomains;

import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author  dochez
 * @version
 */
public class XMLValidationHandler extends DefaultHandler {

    protected static Hashtable<String, String> mapping = null;
    private Logger logger = LogDomains.getLogger(LogDomains.AVK_VERIFIER_LOGGER);
    private boolean throwsException;

    /** Creates new DTDEntityResolver */
    public XMLValidationHandler(boolean thowsException) {
        this.throwsException = thowsException;
        Init();
    }

    public XMLValidationHandler() {
        this.throwsException=true;
        Init();
    }


    private static void Init() {
        if (mapping==null) {
            mapping = new Hashtable<String, String>();
            mapping.put(com.sun.enterprise.deployment.node.ApplicationNode.PUBLIC_DTD_ID ,  "application_1_3.dtd" );
            mapping.put(com.sun.enterprise.deployment.node.ApplicationNode.PUBLIC_DTD_ID_12 , "application_1_2.dtd");
            mapping.put(org.glassfish.ejb.deployment.node.EjbBundleNode.PUBLIC_DTD_ID, "ejb-jar_2_0.dtd" ) ;
            mapping.put(org.glassfish.ejb.deployment.node.EjbBundleNode.PUBLIC_DTD_ID_12, "ejb-jar_1_1.dtd" );
            mapping.put(com.sun.enterprise.deployment.node.appclient.AppClientNode.PUBLIC_DTD_ID, "application-client_1_3.dtd" );
            mapping.put(com.sun.enterprise.deployment.node.appclient.AppClientNode.PUBLIC_DTD_ID_12, "application-client_1_2.dtd" );

            mapping.put(org.glassfish.web.deployment.node.WebBundleNode.PUBLIC_DTD_ID, "web-app_2_3.dtd" );
            mapping.put(org.glassfish.web.deployment.node.WebBundleNode.PUBLIC_DTD_ID_12,   "web-app_2_2.dtd");

            //connector1.5
            mapping.put(com.sun.enterprise.deployment.node.connector.ConnectorNode.PUBLIC_DTD_ID, "connector_1_5.dtd" );
            mapping.put(com.sun.enterprise.deployment.node.connector.ConnectorNode.PUBLIC_DTD_ID_10,   "connector_1_0.dtd");

            //SunOne 8.0 Specific Stuff
            mapping.put(DTDRegistry.SUN_APPLICATION_130_DTD_PUBLIC_ID,"sun-application_1_3-0.dtd");
            mapping.put(DTDRegistry.SUN_APPLICATION_140_DTD_PUBLIC_ID,"sun-application_1_4-0.dtd");
            mapping.put(DTDRegistry.SUN_APPLICATION_140beta_DTD_PUBLIC_ID,"sun-application_1_4-0.dtd");
            mapping.put(DTDRegistry.SUN_APPLICATION_500_DTD_PUBLIC_ID,"sun-application_5_0-0.dtd");

            mapping.put(DTDRegistry.SUN_APPCLIENT_130_DTD_PUBLIC_ID,"sun-application-client_1_3-0.dtd");
            mapping.put(DTDRegistry.SUN_APPCLIENT_140_DTD_PUBLIC_ID,"sun-application-client_1_4-0.dtd");
            mapping.put(DTDRegistry.SUN_APPCLIENT_140beta_DTD_PUBLIC_ID,"sun-application-client_1_4-0.dtd");
            mapping.put(DTDRegistry.SUN_APPCLIENT_141_DTD_PUBLIC_ID,"sun-application-client_1_4-1.dtd");
            mapping.put(DTDRegistry.SUN_APPCLIENT_500_DTD_PUBLIC_ID,"sun-application-client_5_0-0.dtd");

            mapping.put(DTDRegistry.SUN_WEBAPP_230_DTD_PUBLIC_ID,"sun-web-app_2_3-0.dtd");
            mapping.put(DTDRegistry.SUN_WEBAPP_231_DTD_PUBLIC_ID,"sun-web-app_2_3-1.dtd");
            mapping.put(DTDRegistry.SUN_WEBAPP_240_DTD_PUBLIC_ID,"sun-web-app_2_4-0.dtd");
            mapping.put(DTDRegistry.SUN_WEBAPP_240beta_DTD_PUBLIC_ID,"sun-web-app_2_4-0.dtd");
            mapping.put(DTDRegistry.SUN_WEBAPP_241_DTD_PUBLIC_ID,"sun-web-app_2_4-1.dtd");
            mapping.put(DTDRegistry.SUN_WEBAPP_250_DTD_PUBLIC_ID,"sun-web-app_2_5-0.dtd");

            mapping.put(DTDRegistry.SUN_EJBJAR_200_DTD_PUBLIC_ID,"sun-ejb-jar_2_0-0.dtd");
            mapping.put(DTDRegistry.SUN_EJBJAR_201_DTD_PUBLIC_ID,"sun-ejb-jar_2_0-1.dtd");
            mapping.put(DTDRegistry.SUN_EJBJAR_210_DTD_PUBLIC_ID,"sun-ejb-jar_2_1-0.dtd");
            mapping.put(DTDRegistry.SUN_EJBJAR_210beta_DTD_PUBLIC_ID,"sun-ejb-jar_2_1-0.dtd");
            mapping.put(DTDRegistry.SUN_EJBJAR_211_DTD_PUBLIC_ID,"sun-ejb-jar_2_1-1.dtd");
            mapping.put(DTDRegistry.SUN_EJBJAR_300_DTD_PUBLIC_ID,"sun-ejb-jar_3_0-0.dtd");

            mapping.put(DTDRegistry.SUN_CONNECTOR_100_DTD_PUBLIC_ID,"sun-connector_1_0-0.dtd");
            mapping.put(DTDRegistry.SUN_CLIENTCONTAINER_700_DTD_PUBLIC_ID,"sun-application-client-container_1_0.dtd");

            mapping.put(DTDRegistry.SUN_CMP_MAPPING_700_DTD_PUBLIC_ID,"sun-cmp-mapping_1_0.dtd");
            mapping.put(DTDRegistry.SUN_CMP_MAPPING_800_DTD_PUBLIC_ID,"sun-cmp-mapping_1_1.dtd");
            mapping.put(DTDRegistry.SUN_CMP_MAPPING_810_DTD_PUBLIC_ID,"sun-cmp-mapping_1_2.dtd");

            mapping.put(DTDRegistry.TAGLIB_12_DTD_PUBLIC_ID,"web-jsptaglibrary_1_2.dtd");
            mapping.put(DTDRegistry.TAGLIB_11_DTD_PUBLIC_ID,"web-jsptaglibrary_1_1.dtd");
        }
    }

    public InputSource resolveEntity(String publicID, String systemID) throws SAXException {
        try{
            if (publicID==null) {
                // unspecified schema
                if (systemID==null || systemID.lastIndexOf('/')==systemID.length()) {
                    return null;
                }

                String fileName = getSchemaURLFor(systemID.substring(systemID.lastIndexOf('/')+1));
                // if this is not a request for a schema located in our repository,
                // let's hope that the hint provided by schemaLocation is correct
                if (fileName==null) {
                    fileName = systemID;
                }
                return new InputSource(fileName);
            }

            if (mapping.containsKey(publicID)) {
                return new InputSource(new FileInputStream(new File(getAbsoluteFilenameForDTD((String) mapping.get(publicID)))));
            }
        } catch(java.io.IOException ioe) {
            throw new SAXException(ioe);
        }
        return null;
    }

    public void error(SAXParseException spe) throws SAXParseException {
        logger.log(Level.FINE,"XML Error line : " + spe.getLineNumber() + " " + spe.getLocalizedMessage());        
        if (throwsException)
            throw spe;
    }

    public void fatalError(SAXParseException spe) throws SAXParseException {
        logger.log(Level.FINE,"XML Error line : " + spe.getLineNumber() + " " + spe.getLocalizedMessage());
        throw spe;
    }

    protected String getAbsoluteFilenameForDTD(String dtdFilename)
    {
        //String j2ee13 = DirLocation.getDTDDirRoot();
        String j2ee13 = DTDRegistry.DTD_LOCATION;
    	File f = new File(j2ee13 +File.separator+ dtdFilename);
	    return f.getAbsolutePath();
    }

    protected String getSchemaURLFor(String schemaSystemID) throws IOException {
        File f = getSchemaFileFor(schemaSystemID);
        if (f!=null) {
            return f.toURL().toString();
        } else {
            return null;
        }
    }

    protected File getSchemaFileFor(String schemaSystemID) throws IOException {

	    String schemaLoc = DTDRegistry.SCHEMA_LOCATION.replace('/', File.separatorChar);
        File f = new File(schemaLoc +File.separatorChar+ schemaSystemID);
        if (!f.exists()) {
            return null;
        }
        return f;
    }
}
