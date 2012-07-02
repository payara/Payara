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
import java.util.List;
import java.util.Properties;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;

/** Transfers ActionReport like DTO (with all attributes and unsiplified 
 * complexity). The goal is to provide real object transfer mechanism.
 *
 * @author mmares
 */
@Provider
@Produces({"actionreport/json", "actionreport/xml"})
public class ActionReportDtoStaxProvider extends AbstractStaxProvider<ActionReporter> {
    
    public ActionReportDtoStaxProvider() {
        super(ActionReporter.class, new MediaType("actionreport", "json"), new MediaType("actionreport", "xml"));
    }

    @Override
    protected void writeContentToStream(ActionReporter ar, XMLStreamWriter wr) throws XMLStreamException {
        if (ar == null) {
            return;
        }
        wr.writeStartDocument();
        writeActionReport(ar, wr);
        wr.writeEndDocument();
    }
    
    private void writeActionReport(ActionReporter ar, XMLStreamWriter wr) throws XMLStreamException {
        wr.writeStartElement("action-report");
        writeString("exit-code", ar.getActionExitCode().name(), wr);
        writeString("description", ar.getActionDescription(), wr);
        writeString("failure-cause", (ar.getFailureCause() == null ? null : ar.getFailureCause().getLocalizedMessage()), wr);
        writeProperties("extra-property", ar.getExtraProperties(), wr);
        writeMessagePart(ar.getTopMessagePart(), wr);
        List<ActionReporter> subReports = ar.getSubActionsReport();
        if (subReports != null) {
            for (ActionReporter subReport : subReports) {
                writeActionReport(subReport, wr);
            }
        }
        wr.writeEndElement(); //action-report
    }
    
    private void writeMessagePart(ActionReport.MessagePart mp, XMLStreamWriter wr) throws XMLStreamException {
        if (mp == null) {
            return;
        }
        wr.writeStartElement("message");
        writeString("body", mp.getMessage(), wr);
        writeProperties("property", mp.getProps(), wr);
        writeString("children-type", mp.getChildrenType(), wr);
        List<MessagePart> children = mp.getChildren();
        if (children != null) {
            for (MessagePart child : children) {
                writeMessagePart(child, wr);
            }
        }
        wr.writeEndElement(); //message
    }
    
    private void writeProperties(String name, Properties props, XMLStreamWriter wr) throws XMLStreamException {
        if (props == null) {
            return;
        }
        for (String key : props.stringPropertyNames()) {
            wr.writeStartElement(name);
            writeString("name", key, wr);
            writeString("value", props.getProperty(key), wr);
//            wr.writeAttribute("name", key);
//            wr.writeAttribute("value", props.getProperty(key));
            wr.writeEndElement();
        }
    }
    
    private void writeString(String name, String str, XMLStreamWriter wr) throws XMLStreamException {
        if (str != null && !str.isEmpty()) {
            wr.writeStartElement(name);
            wr.writeCharacters(str);
            wr.writeEndElement();
        }
    }
    
}
