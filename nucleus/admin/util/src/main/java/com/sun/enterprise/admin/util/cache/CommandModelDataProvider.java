/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util.cache;

import com.sun.enterprise.admin.util.CachedCommandModel;
import com.sun.enterprise.admin.util.CommandModelData;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.jvnet.hk2.annotations.Service;

/** 
 * Works with {@link com.sun.enterprise.admin.util.CachedCommandModel} and
 * {@link com.sun.enterprise.admin.util.CommandModelData).
 *
 * @author mmares
 */
@Service
public class CommandModelDataProvider implements DataProvider {
    private static final String ALIAS_ELEMENT = "alias";
    private static final String CLASS_ELEMENT = "class";
    private static final String DEFAULT_VALUE_ELEMENT = "default-value";
    private static final String ETAG_ELEMENT = "e-tag";
    private static final String NAME_ELEMENT = "name";
    private static final String OBSOLETE_ELEMENT = "obsolete";
    private static final String OPTIONAL_ELEMENT = "optional";
    private static final String PARAMETERS_ELEMENT = "parameters";
    private static final String PARAMETER_ELEMENT = "parameter";
    private static final String SHORTNAME_ELEMENT = "short-name";
    private static final String UNKNOWN_ARE_OPERANDS_ELEMENT = "unknown-are-operands";
    
    private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    private XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    
    private static final String ROOT_ELEMENT = "command-model";
    
    @Override
    public boolean accept(Class clazz) {
        return clazz == CommandModel.class || 
                clazz == CachedCommandModel.class || 
                clazz == CommandModelData.class;
    }
    
    @Override
    public byte[] toByteArray(Object o) {
        if (o == null) {
            return new byte[0];
        }
        CommandModel cm = (CommandModel) o;
        //Manual implementation based on stax for good performance
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(baos);
        // @todo Java SE 7: Managed source
        XMLStreamWriter xml = null;
        try {
            xml = outputFactory.createXMLStreamWriter(bos, "UTF-8");
            xml.writeStartDocument();
            xml.writeStartElement(ROOT_ELEMENT);
            //ETag
            if (o instanceof CachedCommandModel) {
                CachedCommandModel ccm = (CachedCommandModel) o;
                xml.writeStartElement(ETAG_ELEMENT);
                xml.writeCharacters(ccm.getETag());
                xml.writeEndElement();
            }
            //command name
            String str = cm.getCommandName();
            if (str != null && !str.isEmpty()) {
                xml.writeStartElement(NAME_ELEMENT);
                xml.writeCharacters(str);
                xml.writeEndElement();
            }
            //unknown are operands
            if (cm.unknownOptionsAreOperands()) {
                xml.writeStartElement(UNKNOWN_ARE_OPERANDS_ELEMENT);
                xml.writeCharacters("true");
                xml.writeEndElement();
            }
            //Parameters
            xml.writeStartElement(PARAMETERS_ELEMENT);
            for (CommandModel.ParamModel paramModel : cm.getParameters()) {
                xml.writeStartElement(PARAMETER_ELEMENT);
                //parameter / name
                str = paramModel.getName();
                if (str != null && !str.isEmpty()) {
                    xml.writeStartElement(NAME_ELEMENT);
                    xml.writeCharacters(str);
                    xml.writeEndElement();
                }
                //parameter / class
                if (paramModel.getType() != null) {
                    xml.writeStartElement(CLASS_ELEMENT);
                    xml.writeCharacters(paramModel.getType().getName());
                    xml.writeEndElement();
                }
                Param param = paramModel.getParam();
                //parameter / shortName
                str = param.shortName();
                if (str != null && !str.isEmpty()) {
                    xml.writeStartElement(SHORTNAME_ELEMENT);
                    xml.writeCharacters(str);
                    xml.writeEndElement();
                }
                //parameter / alias
                str = param.alias();
                if (str != null && !str.isEmpty()) {
                    xml.writeStartElement(ALIAS_ELEMENT);
                    xml.writeCharacters(str);
                    xml.writeEndElement();
                }
                //parameter / optional
                if (param.optional()) {
                    xml.writeStartElement(OPTIONAL_ELEMENT);
                    xml.writeCharacters("true");
                    xml.writeEndElement();
                }
                //parameter / obsolete
                if (param.obsolete()) {
                    xml.writeStartElement(OBSOLETE_ELEMENT);
                    xml.writeCharacters("true");
                    xml.writeEndElement();
                }
                //parameter / defaultValue
                str = param.defaultValue();
                if (str != null && !str.isEmpty()) {
                    xml.writeStartElement(DEFAULT_VALUE_ELEMENT);
                    xml.writeCharacters(str);
                    xml.writeEndElement();
                }
                xml.writeEndElement(); //parameter
            }
            xml.writeEndElement();
            //root
            xml.writeEndElement();
            xml.writeEndDocument();
        } catch (XMLStreamException ex) {
            return new byte[0];
        } finally {
            try {xml.close();} catch (Exception ex) {}
            try {bos.close();} catch (Exception ex) {}
        }
        return baos.toByteArray();
    }

    @Override
    public Object toInstance(byte[] data, Class clazz) {
        if (data == null || data.length == 0) {
            return null;
        }
        boolean inCommandModel = false;
        boolean inParam = false;
        String eTag = null;
        String name = null;
        boolean unknownAreOperands = false;
        String pName = null;
        Class pCls = null;
        String pShortName = null;
        String pAlias = null;
        boolean pOptional = false;
        boolean pObsolete = false;
        String pDefaultValue = null;
        String currentElement = null;
        List<CommandModelData.ParamModelData> params = new ArrayList<CommandModelData.ParamModelData>();
        InputStream is = new ByteArrayInputStream(data);
        try {
            XMLStreamReader xml = inputFactory.createXMLStreamReader(is);
            while (xml.hasNext()) {
                xml.next();
                //Start element
                if (xml.getEventType() == XMLStreamReader.START_ELEMENT) {
                    currentElement = xml.getLocalName();
                    if (!inCommandModel) {
                        if (!ROOT_ELEMENT.equals(currentElement)) {
                            return null;
                        } else {
                            inCommandModel = true;
                        }
                    } else if (PARAMETER_ELEMENT.equals(currentElement)) {
                        //reset for new param
                        inParam = true;
                        pName = null;
                        pCls = null;
                        pShortName = null;
                        pAlias = null;
                        pOptional = false;
                        pObsolete = false;
                        pDefaultValue = null;
                    }
                } else
                //End element
                if (xml.getEventType() == XMLStreamReader.END_ELEMENT) {
                    if (PARAMETER_ELEMENT.equals(xml.getLocalName())) {
                        inParam = false;
                        params.add(new CommandModelData.ParamModelData(pName, 
                                pCls, pOptional, pDefaultValue, pShortName, 
                                pObsolete, pAlias));
                    }
                    currentElement = null;
                } else
                //Characters
                if (xml.getEventType() == XMLStreamReader.CHARACTERS) {
                    // @toto JavaSE 7: String switch case
                    if (inParam) {
                        if (NAME_ELEMENT.equals(currentElement)) {
                            pName = xml.getText();
                        } else if (CLASS_ELEMENT.equals(currentElement)) {
                            pCls = Class.forName(xml.getText());
                        } else if (SHORTNAME_ELEMENT.equals(currentElement)) {
                            pShortName = xml.getText();
                        } else if (ALIAS_ELEMENT.equals(currentElement)) {
                            pAlias = xml.getText();
                        } else if (DEFAULT_VALUE_ELEMENT.equals(currentElement)) {
                            pDefaultValue = xml.getText();
                        } else if (OPTIONAL_ELEMENT.equals(currentElement)) {
                            pOptional = Boolean.parseBoolean(xml.getText());
                        } else if (OBSOLETE_ELEMENT.equals(currentElement)) {
                            pObsolete = Boolean.parseBoolean(xml.getText());
                        }
                    } else {
                        if (NAME_ELEMENT.equals(currentElement)) {
                            name = xml.getText();
                        } else if (ETAG_ELEMENT.equals(currentElement)) {
                            eTag = xml.getText();
                        } else if (UNKNOWN_ARE_OPERANDS_ELEMENT.equals(currentElement)) {
                            unknownAreOperands = Boolean.parseBoolean(xml.getText());
                        }
                    }
                }
            }
            //Build result
            CachedCommandModel result = new CachedCommandModel(name, eTag);
            result.dashOk = unknownAreOperands;
            for (ParamModelData paramModel : params) {
                result.add(paramModel);
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
