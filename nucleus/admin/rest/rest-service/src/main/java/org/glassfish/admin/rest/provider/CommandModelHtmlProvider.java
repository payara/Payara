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
import java.io.File;
import java.util.Collection;
import java.util.Properties;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandModel.ParamModel;

/** Marshals {@code CommandModel} into HTML representation.
 *
 * @author mmares
 */
@Provider
@Produces(MediaType.TEXT_HTML)
public class CommandModelHtmlProvider extends BaseProvider<CommandModel> {
    
    public CommandModelHtmlProvider() {
        super(CommandModel.class, MediaType.TEXT_HTML_TYPE);
    }

    @Override
    public String getContent(CommandModel proxy) {
        StringBuilder r = new StringBuilder(128);
        r.append("<html><body>");
        r.append("<h1>Command model: ").append(proxy.getCommandName()).append("</h1>\n");
        r.append("<b>Unknown-options-are-operands:</b> ").append(proxy.unknownOptionsAreOperands());
        r.append("<br/>\n");
        String str = proxy.getUsageText();
        if (StringUtils.ok(str)) {
            r.append("<i>").append(str).append("</i><br/>\n");
        }
        //Options
        Collection<ParamModel> parameters = proxy.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            r.append("<h2>Options:</h2>\n");
            for (CommandModel.ParamModel p : proxy.getParameters()) {
                Param par = p.getParam();
                r.append("<h3>").append(p.getName());
                if (par.primary()) {
                    r.append(" [PRIMARY]");
                }
                r.append("</h3>\n");
                r.append("<table border=\"1\">\n");
                r.append("  <tr><td><b>Type: </b></td><td>").append(typeToValue(p)).append("</td></tr>\n");
                str = p.getLocalizedDescription();
                if (StringUtils.ok(str)) {
                    r.append("  <tr><td><b>Description: </b></td><td>").append(StringUtils.escapeForHtml(str)).append("</td></tr>\n");
                }
                r.append("  <tr><td><b>Optional: </b></td><td>").append(par.optional()).append("</td></tr>\n");
                r.append("  <tr><td><b>Obsolete: </b></td><td>").append(par.obsolete()).append("</td></tr>\n");
                str = par.shortName();
                if (StringUtils.ok(str)) {
                    r.append("  <tr><td><b>Short name: </b></td><td>").append(str).append("</td></tr>\n");
                }
                str = par.defaultValue();
                if (StringUtils.ok(str)) {
                    r.append("  <tr><td><b>Default value: </b></td><td>").append(str).append("</td></tr>\n");
                }
                str = par.acceptableValues();
                if (StringUtils.ok(str)) {
                    r.append("  <tr><td><b>Acceptable values: </b></td><td>").append(str).append("</td></tr>\n");
                }
                str = par.alias();
                if (StringUtils.ok(str)) {
                    r.append("  <tr><td><b>Alias: </b></td><td>").append(str).append("</td></tr>\n");
                }
                if (par.primary() && par.multiple()) {
                    r.append("  <tr><td><b>Multiple: </b></td><td>true</td></tr>\n");
                }
                r.append("</table>\n");
            }
        }
        r.append("</body></html>");
        return r.toString();
    }
    
    public static String typeToValue(CommandModel.ParamModel p) {
        Class t = p.getType();
        if (t == null) {
            return "undefined";
        }
        StringBuilder result = new StringBuilder(50);
        result.append(t.getName());
        result.append(" [");
        result.append(simplifiedTypeOf(p));
        result.append(']');
        return result.toString();
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
