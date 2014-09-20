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

package com.sun.enterprise.admin.servermgmt.template;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.InputSource;

import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.xml.templateinfo.TemplateInfo;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

public class TemplateInfoHolder {

    private static final LocalStringsImpl _strings = new LocalStringsImpl(TemplateInfoHolder.class);
    //Path where schema resides. 
    private final static String TEMPLATE_INFO_SCHEMA_PATH = "xsd/schema/template-info.xsd";
    private TemplateInfo _templateInfo;
    private String _location;

    public TemplateInfoHolder(InputStream inputSteam, String location)
            throws DomainException {
        try {
            _templateInfo = parse(inputSteam);
        } catch (Exception e) {
            throw new DomainException(_strings.get("failedToParse", TEMPLATE_INFO_SCHEMA_PATH));
        }
        _location = location;
    }

    public TemplateInfo getTemplateInfo() {
        return _templateInfo;
    }

    public String getLocation() {
        return _location;
    }

    /**
     * Parse the configuration stream against the template-info schema.
     *
     * @param configStream InputStream of template-info.xml file.
     * @return Parsed Object.
     * @throws Exception If any error occurs in parsing.
     */
    @SuppressWarnings("rawtypes")
    private TemplateInfo parse(InputStream configStream)
            throws Exception {
        if (configStream == null) {
            throw new DomainException("Invalid stream");
        }
        try {
            URL schemaUrl = getClass().getClassLoader().getResource(TEMPLATE_INFO_SCHEMA_PATH);
            JAXBContext context = JAXBContext.newInstance(TemplateInfo.class.getPackage().getName());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaUrl);
            unmarshaller.setSchema(schema);
            InputSource is = new InputSource(configStream);
            SAXSource source = new SAXSource(is);
            Object obj = unmarshaller.unmarshal(source);
            return obj instanceof JAXBElement ? (TemplateInfo)(((JAXBElement) obj).getValue()) : (TemplateInfo) obj;
        }
        finally {
            try {
                configStream.close();
                configStream = null;
            } catch(IOException e)
            { /** Ignore */ }
        }
    }
}
