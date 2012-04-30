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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomcat.util.digester;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Create a <code>SAXParser</code> based on the underlying Xerces version.
 * Currently, Xerces 2.3 and up doesn't implement schema validation the same way
 * 2.1 was. In other to support schema validation in a portable way between 
 * parser, some features/properties need to be set.
 *
 * @since 1.6
 */

public class XercesParser{

    /**
     * The Log to which all SAX event related logging calls will be made.
     */
    protected static final Logger log =
        Logger.getLogger("org.apache.tomcat.util.digester.Digester.sax");


    /**
     * The JAXP 1.2 property required to set up the schema location.
     */
    private static final String JAXP_SCHEMA_SOURCE =
        "http://java.sun.com/xml/jaxp/properties/schemaSource";


    /**
     * The JAXP 1.2 property to set up the schemaLanguage used.
     */
    protected static final String JAXP_SCHEMA_LANGUAGE =
        "http://java.sun.com/xml/jaxp/properties/schemaLanguage";


    /**
     * Xerces dynamic validation property
     */
    protected static final String XERCES_DYNAMIC = 
        "http://apache.org/xml/features/validation/dynamic";


    /**
     * Xerces schema validation property
     */
    protected static final String XERCES_SCHEMA =
        "http://apache.org/xml/features/validation/schema";


    /**
     * A <code>float</code> representing the underlying Xerces version
     */
    static float version;


    /**
     * The current Xerces version.
     */
    static String versionNumber = null;


    /**
     * Return the current Xerces version.
     * @return the current Xerces version.
     */
    private static String getXercesVersion() {
        // If for some reason we can't get the version, set it to 1.0.
        String versionNumber = "1.0";
        try{
            // Use reflection to avoid a build dependency with Xerces.
            Class<?> versionClass = 
                            Class.forName("org.apache.xerces.impl.Version");
            // Will return Xerces-J 2.x.0
            Method method = 
                versionClass.getMethod("getVersion", (Class[]) null); 
            String version = (String)method.invoke(null, (Object[]) null);
            versionNumber = version.substring( "Xerces-J".length() , 
                                               version.lastIndexOf(".") ); 
        } catch (Exception ex){
            // Do nothing.
        }
        return versionNumber;
    }


    /**
     * Create a <code>SAXParser</code> based on the underlying
     * <code>Xerces</code> version.
     * @param properties parser specific properties/features
     * @return an XML Schema/DTD enabled <code>SAXParser</code>
     */
    public static SAXParser newSAXParser(Properties properties) 
            throws ParserConfigurationException, 
                   SAXException,
                   SAXNotSupportedException {

        SAXParserFactory factory =  
                        (SAXParserFactory)properties.get("SAXParserFactory");

        if (versionNumber == null){
            versionNumber = getXercesVersion();
            version = new Float( versionNumber ).floatValue();
        }

        // Note: 2.2 is completely broken (with XML Schema). 
        if (version > 2.1) {

            configureXerces(factory);
            return factory.newSAXParser();
        } else {
            SAXParser parser = factory.newSAXParser();
            configureOldXerces(parser,properties);
            return parser;
        }
    }


    /**
     * Configure schema validation as recommended by the JAXP 1.2 spec.
     * The <code>properties</code> object may contains information about
     * the schema local and language. 
     * @param properties parser optional info
     */
    private static void configureOldXerces(SAXParser parser, 
                                           Properties properties) 
            throws ParserConfigurationException, 
                   SAXNotSupportedException {

        String schemaLocation = (String)properties.get("schemaLocation");
        String schemaLanguage = (String)properties.get("schemaLanguage");

        try{
            if (schemaLocation != null) {
                parser.setProperty(JAXP_SCHEMA_LANGUAGE, schemaLanguage);
                parser.setProperty(JAXP_SCHEMA_SOURCE, schemaLocation);
            }
        } catch (SAXNotRecognizedException e){
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, parser.getClass().getName() + ": " 
                                            + e.getMessage() + " not supported."); 
            }
        }

    }


    /**
     * Configure schema validation as recommended by the Xerces spec. 
     * Both DTD and Schema validation will be enabled simultaneously.
     * @param factory SAXParserFactory to be configured
     */
    private static void configureXerces(SAXParserFactory factory)
            throws ParserConfigurationException, 
                   SAXNotRecognizedException, 
                   SAXNotSupportedException {

        factory.setFeature(XERCES_DYNAMIC, true);
        factory.setFeature(XERCES_SCHEMA, true);

    }
}
