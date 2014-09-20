/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.io.File;
import java.util.Properties;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;

/** Marshals {@code CommandModel} into XML and JSON representation.
 *
 * @author mmares
 */
@Provider
@Produces({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.APPLICATION_JSON, "application/x-javascript"})
public class CommandModelStaxProvider extends AbstractStaxProvider<CommandModel> {
    public CommandModelStaxProvider() {
        super(CommandModel.class, MediaType.APPLICATION_XML_TYPE, 
              MediaType.TEXT_XML_TYPE, MediaType.APPLICATION_JSON_TYPE);
    }
    
    @Override
    protected void writeContentToStream(CommandModel proxy, XMLStreamWriter wr) throws XMLStreamException {
        if (proxy == null) {
            return;
        }
        wr.writeStartDocument();
        wr.writeStartElement("command");
        wr.writeAttribute("name", proxy.getCommandName());
        if (proxy.unknownOptionsAreOperands()) {
            wr.writeAttribute("unknown-options-are-operands", "true");
        }
        if (proxy.isManagedJob()) {
            wr.writeAttribute("managed-job", "true");
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
            wr.writeAttribute("type", simplifiedTypeOf(p));
            if (par.primary()) {
                wr.writeAttribute("primary", "true");
            }
            if (par.multiple()) {
                wr.writeAttribute("multiple", "true");
            }
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
                wr.writeAttribute("description", str);
            }
            str = p.getLocalizedPrompt();
            if (StringUtils.ok(str)) {
                wr.writeAttribute("prompt", str);
            }
            str = p.getLocalizedPromptAgain();
            if (StringUtils.ok(str)) {
                wr.writeAttribute("prompt-again", str);
            }
            wr.writeEndElement();
        }
        wr.writeEndElement(); //</command>
        wr.writeEndDocument();
    }
    
    public static String simplifiedTypeOf(CommandModel.ParamModel p) {
        Class t = p.getType();
        if (t == Boolean.class || t == boolean.class) {
            return "BOOLEAN";
        } else if (t == File.class || t == File[].class) {
            return "FILE";
        } else if (t == Properties.class) { // XXX - allow subclass?
            return "PROPERTIES";
        } else if (p.getParam().password()) {
            return "PASSWORD";
        } else {
            return "STRING";
        }
    }

}
