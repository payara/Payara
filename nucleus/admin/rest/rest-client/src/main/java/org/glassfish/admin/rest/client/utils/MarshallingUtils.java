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

package org.glassfish.admin.rest.client.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.api.logging.LogHelper;

/**
 *
 * @author jasonlee
 */
public class MarshallingUtils {
    public static List<Map<String, String>> getPropertiesFromJson(String json) {
        List<Map<String, String>> properties = null;
        json = json.trim();
        if (json.startsWith("{")) {
            properties = new ArrayList<Map<String, String>>();
            properties.add(processJsonMap(json));
        } else if (json.startsWith("[")) {
            try {
                properties = processJsonArray(new JSONArray(json));
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            throw new RuntimeException("The JSON string must start with { or ["); // i18n
        }

        return properties;
    }

    public static List<Map<String, String>> getPropertiesFromXml(String xml) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        InputStream input = null;
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            input = new ByteArrayInputStream(xml.trim().getBytes("UTF-8"));
            XMLStreamReader parser = inputFactory.createXMLStreamReader(input);
            while (parser.hasNext()) {
                int event = parser.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        if ("list".equals(parser.getLocalName())) {
                            list = processXmlList(parser);
                        }
                        break;
                    }
                    default: {
                        // no-op
                    }
                }
            }
        } catch (UnsupportedEncodingException ex) {
            LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_ENCODING_ERROR, ex, "UTF-8");
            throw new RuntimeException(ex);
        } catch (XMLStreamException ex) {
            LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_XML_STREAM_ERROR, ex);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_IO_ERROR, ex);
            }
        }
        return list;
    }

    public static String getXmlForProperties(final Map<String, String> properties) {
        return getXmlForProperties(new ArrayList<Map<String, String>>() {{ add(properties); }} );
    }

    public static String getXmlForProperties(List<Map<String, String>> properties) {
        try {
            String xml = null;
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            StringWriter sw = new StringWriter();
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(sw);
            writer.writeStartDocument("UTF-8","1.0");
            writer.writeStartElement("list");
            for (Map<String, String> property : properties) {
                writer.writeStartElement("map");
                for (Map.Entry<String, String> entry : property.entrySet()) {
                writer.writeStartElement("entry");
                    writer.writeAttribute("key", entry.getKey());
                    writer.writeAttribute("value", entry.getValue());
                writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            return sw.toString();
        } catch (XMLStreamException ex) {
            LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_XML_STREAM_ERROR, ex);
            throw new RuntimeException(ex);
        }
    }

    public static String getJsonForProperties(final Map<String, String> properties) {
        return getJsonForProperties(new ArrayList<Map<String, String>>() {{ add(properties); }} );
    }

    public static String getJsonForProperties(List<Map<String, String>> properties) {
        JSONArray list = new JSONArray();

        for (Map<String, String> property : properties) {
            list.put(property);
        }

        return list.toString();
    }

    public static Map buildMapFromDocument(String text) {
        Map map = null;
        if ((text == null) || text.isEmpty()) {
            return new HashMap();
        }

        text = text.trim();

        if (text.startsWith("{")) {
            map = processJsonMap(text);
        } else if (text.startsWith("<")) {
            InputStream input = null;
            try {
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
                input = new ByteArrayInputStream(text.trim().getBytes("UTF-8"));
                XMLStreamReader parser = inputFactory.createXMLStreamReader(input);
                while (parser.hasNext()) {
                    int event = parser.next();
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            if ("map".equals(parser.getLocalName())) {
                                map = processXmlMap(parser);
                            }
                            break;
                        }
                        default: {
                            // No-op
                        }
                    }
                }
            } catch (UnsupportedEncodingException ex) {
                LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_ENCODING_ERROR, ex, "UTF-8");
                throw new RuntimeException(ex);
            } catch (XMLStreamException ex) {
                LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_XML_STREAM_ERROR, ex);
                throw new RuntimeException(ex);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ex) {
                    LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_IO_ERROR, ex);
                }
            }
        } else {
            System.out.println(text);
            throw new RuntimeException ("An unknown document type was provided:  " + text); //.substring(0, 10));
        }

        return map;
    }

    /**************************************************************************/
    private static Map processJsonMap(String json) {
        Map map;
        try {
            map = processJsonObject(new JSONObject(json));
        } catch (JSONException e) {
            map = new HashMap();
        }
        return map;
    }

    private static Map processJsonObject(JSONObject jo) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Iterator i = jo.keys();
            while (i.hasNext()) {
                String key = (String)i.next();
                Object value = jo.get(key);
                if (value instanceof JSONArray) {
                    map.put(key, processJsonArray((JSONArray)value));
                } else if (value instanceof JSONObject) {
                    map.put(key, processJsonObject((JSONObject)value));
                } else if (value.getClass().getSimpleName().equalsIgnoreCase("null")) {
                    // The Map may not store null values, but we shouldn't rely on
                    // that behavior, just to be safe
                    map.put(key, null);
                } else {
                    map.put(key, value);
                }
            }
        } catch (JSONException e) {
            LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_JSON_ERROR, e);
        }

        return map;
    }

    private static List processJsonArray(JSONArray ja) {
        List results = new ArrayList();

        try {
            for (int i = 0; i < ja.length(); i++) {
                Object entry = ja.get(i);
                if (entry instanceof JSONArray) {
                    results.add(processJsonArray((JSONArray)entry));
                } else if (entry instanceof JSONObject) {
                    results.add(processJsonObject((JSONObject)entry));
                } else {
                    results.add(entry);
                }
            }
        } catch (JSONException e) {
            LogHelper.log(RestClientLogging.logger, Level.SEVERE, RestClientLogging.REST_CLIENT_JSON_ERROR, e);
        }

        return results;
    }

    private static Map processXmlMap(XMLStreamReader parser) throws XMLStreamException {
        boolean endOfMap = false;
        Map<String, Object> entry = new HashMap<String, Object>();
        String key = null;
        String element = null;
        while (!endOfMap) {
            int event = parser.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    if ("entry".equals(parser.getLocalName())) {
                        key = parser.getAttributeValue(null, "key");
                        String value = parser.getAttributeValue(null, "value");
                        if (value != null) {
                            entry.put(key, value);
                            key = null;
                        }
                    } else if ("map".equals(parser.getLocalName())) {
                        Map value = processXmlMap(parser);
                        entry.put(key, value);
                    } else if ("list".equals(parser.getLocalName())) {
                        List value = processXmlList(parser);
                        entry.put(key, value);
                    } else {
                        element = parser.getLocalName();
                    }
                    break;
                }
                case XMLStreamConstants.END_ELEMENT: {
                    if ("map".equals(parser.getLocalName())) {
                        endOfMap = true;
                    }
                    element = null;
                    break;
                }
                default: {
                    String text=parser.getText();
                    if (element != null) {
                        if ("number".equals(element)) {
                            if (text.contains(".")) {
                                entry.put(key, Double.parseDouble(text));
                            } else {
                                entry.put(key, Long.parseLong(text));
                            }
                        } else if ("string".equals(element)) {
                            entry.put(key, text);
                        }

                        element = null;
                    }
                }
            }
        }
        return entry;
    }

    private static List processXmlList(XMLStreamReader parser) throws XMLStreamException {
        List list = new ArrayList();
        boolean endOfList = false;
        String element = null;
        while (!endOfList) {
            int event = parser.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    if ("map".equals(parser.getLocalName())) {
                        list.add(processXmlMap(parser));
                    } else {
                        element = parser.getLocalName();
                    }
                    break;
                }
                case XMLStreamConstants.END_ELEMENT: {
                    if ("list".equals(parser.getLocalName())) {
                        endOfList = true;
                    }
                    element = null;
                    break;
                }
                default: {
                    String text = parser.getText();
                    if (element != null) {
                        if ("number".equals(element)) {
                            if (text.contains(".")) {
                                list.add(Double.parseDouble(text));
                            } else {
                                list.add(Long.parseLong(text));
                            }
                        } else if ("string".equals(element)) {
                            list.add(text);
                        }

                        element = null;
                    }
                }
            }
        }
        return list;
    }
}
