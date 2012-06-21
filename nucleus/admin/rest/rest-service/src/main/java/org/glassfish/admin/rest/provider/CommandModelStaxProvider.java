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

import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestService;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;

/** Marshals {@code CommandModel} into XML and JSON representation.
 *
 * @author mmares
 */
@Provider
@Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON, "application/x-javascript"})
public class CommandModelStaxProvider extends BaseProvider<CommandModel> {
    
    private static final XMLOutputFactory XML_FACTORY = XMLOutputFactory.newInstance();
    private static final MappedNamespaceConvention JSON_CONVENTION = new MappedNamespaceConvention();
    
    public final static Logger logger =
            LogDomains.getLogger(RestService.class, LogDomains.ADMIN_LOGGER);
    
    public CommandModelStaxProvider() {
        super(CommandModel.class, MediaType.APPLICATION_XML_TYPE, 
              MediaType.TEXT_XML_TYPE, MediaType.APPLICATION_JSON_TYPE);
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }
    
    private static XMLStreamWriter getXmlWriter(OutputStream os, boolean indent) throws XMLStreamException {
        XMLStreamWriter wr = XML_FACTORY.createXMLStreamWriter(os, Constants.ENCODING);
        if (indent) {
            wr = new IndentingXMLStreamWriter(wr);
        }
        return wr;
    }

    private static XMLStreamWriter getJsonWriter(OutputStream os) throws UnsupportedEncodingException {
        return new MappedXMLStreamWriter(JSON_CONVENTION, new OutputStreamWriter(os, Constants.ENCODING));
    }
    
    private static void writeContentToStream(CommandModel proxy, XMLStreamWriter wr) throws XMLStreamException {
        if (proxy == null) {
            return;
        }
        wr.writeStartDocument();
        wr.writeStartElement("command");
        wr.writeAttribute("name", proxy.getCommandName());
        if (proxy.unknownOptionsAreOperands()) {
            wr.writeAttribute("unknown-options-are-operands", "true");
        }
        String usage = proxy.getUsageText();
        if (StringUtils.ok(usage)) {
            wr.writeStartElement("usage");
            wr.writeCharacters(usage);
            wr.writeEndElement();
        }
        //Options
        for (CommandModel.ParamModel p : proxy.getParameters()) {
            Param par = p.getParam();
            wr.writeStartElement("option");
            wr.writeAttribute("name", p.getName());
            wr.writeAttribute("type", CommandModelHtmlProvider.simplifiedTypeOf(p));
            if (par.optional()) {
                wr.writeAttribute("optional", "true");
            }
            if (par.obsolete()) {
                wr.writeAttribute("obsolete", "true");
            }
            String str = par.shortName();
            if (StringUtils.ok(str)) {
                wr.writeAttribute("short", str);
            }
            str = par.defaultValue();
            if (StringUtils.ok(str)) {
                wr.writeAttribute("default", str);
            }
            str = par.acceptableValues();
            if (StringUtils.ok(str)) {
                wr.writeAttribute("acceptable-values", str);
            }
            str = par.alias();
            if (StringUtils.ok(str)) {
                wr.writeAttribute("alias", str);
            }
            str = p.getLocalizedDescription();
            if (StringUtils.ok(str)) {
                wr.writeCharacters(str);
            }
            wr.writeEndElement();
        }
        wr.writeEndElement(); //</command>
        wr.writeEndDocument();
    }

    @Override
    public String getContent(CommandModel proxy) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            writeContentToStream(proxy, getXmlWriter(baos, super.getFormattingIndentLevel() > -1));
            return baos.toString(Constants.ENCODING);
        } catch (XMLStreamException se) {
            //todo: JDK7 - Connect both catches
            logger.log(Level.SEVERE, "Cannot marshal CommandModel", se);
            return "";
        } catch (UnsupportedEncodingException uee) {
            logger.log(Level.SEVERE, "Cannot marshal CommandModel", uee);
            return "";
        }
    }
    
    /** Faster with direct stream writing
     */
    @Override
    public void writeTo(CommandModel proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        try {
            String writeBefore = null;
            String writeAfter = null;
            XMLStreamWriter writer;
            if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                String callBackJSONP = getCallBackJSONP();
                if (callBackJSONP != null) {
                    writeBefore = callBackJSONP + "(";
                    writeAfter = ")";
                }
                writer = getJsonWriter(entityStream);
            } else {
                writer = getXmlWriter(entityStream, super.getFormattingIndentLevel() > -1);
            }
            //Write it
            if (writeBefore != null) {
                entityStream.write(writeBefore.getBytes(Constants.ENCODING));
            }
            writeContentToStream(proxy, writer);
            if (writeAfter != null) {
                entityStream.write(writeAfter.getBytes(Constants.ENCODING));
            }
        } catch (XMLStreamException uee) {
            logger.log(Level.SEVERE, "Cannot marshal CommandModel", uee);
            throw new WebApplicationException(uee, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
}
