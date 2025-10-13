/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.admin.servermgmt.stringsubs.StringSubstitutionException;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.StringsubsDefinition;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * This class parses the string substitution XML.
 */
public class StringSubstitutionParser {

    private static final Logger LOGGER = SLogger.getLogger(); 
            
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(StringSubstitutionParser.class);
    /** Path where schema resides i.e Parent directory for schema.  */
    private static final String DEFAULT_SCHEMA = "xsd/schema/stringsubs.xsd";

    /**
     * Parse the configuration stream against the string-subs schema.
     *
     * @param configStream InputStream of stringsubs.xml file.
     * @return Parsed Object.
     * @throws StringSubstitutionException If any error occurs in parsing.
     */
    @SuppressWarnings("rawtypes")
    public static StringsubsDefinition parse(InputStream configStream)
            throws StringSubstitutionException {
        // If schema information is missing
        if(configStream == null) {
            throw new StringSubstitutionException(STRINGS.get("invalidStream"));
        }
        try {
            URL schemaUrl = StringSubstitutionParser.class.getClassLoader().getResource(DEFAULT_SCHEMA);
            JAXBContext context = JAXBContext.newInstance(StringsubsDefinition.class.getPackage().getName());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaUrl);
            unmarshaller.setSchema(schema);
            InputSource is = new InputSource(configStream);
            SAXSource source = new SAXSource(is);
            Object obj = unmarshaller.unmarshal(source);
            return obj instanceof JAXBElement ? (StringsubsDefinition) ((JAXBElement) obj).getValue() : (StringsubsDefinition) obj;
        } catch(SAXException | JAXBException se) {
            throw new StringSubstitutionException(STRINGS.get("failedToParse", DEFAULT_SCHEMA), se);      
        } finally {
            try {
                configStream.close();
                configStream = null;
            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(Level.FINER, STRINGS.get("errorInClosingStream"));
                }
            }
        }
    }
}
