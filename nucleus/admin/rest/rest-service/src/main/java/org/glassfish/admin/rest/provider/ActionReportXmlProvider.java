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

package org.glassfish.admin.rest.provider;

import com.sun.enterprise.v3.common.ActionReporter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.admin.rest.utils.xml.XmlArray;
import org.glassfish.admin.rest.utils.xml.XmlMap;
import org.glassfish.admin.rest.utils.xml.XmlObject;
import org.glassfish.api.ActionReport.MessagePart;

/**
 * @author Ludovic Champenois
 * @author mmares
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
public class ActionReportXmlProvider extends BaseProvider<ActionReporter> {
    
    public ActionReportXmlProvider() {
        super(ActionReporter.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Override
    public String getContent(ActionReporter ar) {
        XmlObject result = processReport(ar);
        return result.toString(getFormattingIndentLevel());
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }

    protected XmlObject processReport(ActionReporter ar) {
        XmlMap result = new XmlMap("map");
        result.put("message", (ar instanceof RestActionReporter) ? ((RestActionReporter)ar).getCombinedMessage() : ar.getMessage());
        result.put("command", ar.getActionDescription());
        result.put("exit_code", ar.getActionExitCode().toString());

        Properties properties = ar.getTopMessagePart().getProps();
        if ((properties != null) && (!properties.isEmpty())) {
            result.put("properties", new XmlMap("properties", properties));
        }

        Properties extraProperties = ar.getExtraProperties();
        if ((extraProperties != null) && (!extraProperties.isEmpty())) {
            result.put("extraProperties", getExtraProperties(result, extraProperties));
        }

        List<MessagePart> children = ar.getTopMessagePart().getChildren();
        if ((children != null) && (!children.isEmpty())) {
            result.put("children", processChildren(children));
        }

        List<ActionReporter> subReports = ar.getSubActionsReport();
       if ((subReports != null) && (!subReports.isEmpty())) {
            result.put("subReports", processSubReports(subReports));
        }

        return result;
    }

    protected XmlArray processChildren(List<MessagePart> parts) {
        XmlArray array = new XmlArray("children");

        for (MessagePart part : parts) {
            XmlMap object = new XmlMap("part");
            object.put("message", part.getMessage());
            object.put("properties", new XmlMap("properties", part.getProps()));
            List<MessagePart> children = part.getChildren();
            if (children.size() > 0) {
                object.put("children", processChildren(part.getChildren()));
            }
            array.put(object);
        }

        return array;
    }

    protected XmlArray processSubReports(List<ActionReporter> subReports) {
        XmlArray array = new XmlArray("subReports");

        for (ActionReporter subReport : subReports) {
            array.put(processReport(subReport));
        }

        return array;
    }

    protected XmlMap getExtraProperties(XmlObject object, Properties props) {
        XmlMap extraProperties = new XmlMap("extraProperties");
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = entry.getKey().toString();
            Object value = getXmlObject(entry.getValue());
            if (value != null) {
                extraProperties.put(key, value);
            }
        }

        return extraProperties;
    }

    protected Object getXmlObject(Object object) {
        Object result = null;
        if (object == null) {
            result = "";
        } else if (object instanceof Collection) {
            result = getXml((Collection)object);
        } else if (object instanceof Map) {
            result = getXml((Map)object);
        } else if (object instanceof Number) {
            result = new XmlObject("number", (Number)object);
        } else if (object instanceof String) {
            result = object;
        } else {
            result = new XmlObject(object.getClass().getSimpleName(), object);
        }

        return result;
    }

    protected XmlArray getXml(Collection c) {
        XmlArray result = new XmlArray("list");
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Object item = i.next();
            Object obj = getXmlObject(item);
            if (!(obj instanceof XmlObject)) {
                obj = new XmlObject(obj.getClass().getSimpleName(), obj);
            }
            result.put((XmlObject)obj);
        }

        return result;
    }

    protected XmlMap getXml(Map map) {
        XmlMap result = new XmlMap("map");

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            result.put(entry.getKey().toString(), getXmlObject(entry.getValue()));
        }

        return result;
    }

}
