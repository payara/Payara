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
import java.lang.reflect.Type;
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
@Produces(MediaType.TEXT_PLAIN)
public class CommandModelTxtProvider extends BaseProvider<CommandModel> {
    
    public CommandModelTxtProvider() {
        super(CommandModel.class, MediaType.TEXT_PLAIN_TYPE);
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }

    @Override
    public String getContent(CommandModel proxy) {
        StringBuilder r = new StringBuilder(128);
        r.append("       *** Command model: ").append(proxy.getCommandName()).append(" ***\n\n");
        r.append("  - Unknown-options-are-operands: ").append(proxy.unknownOptionsAreOperands());
        r.append('\n');
        String str = proxy.getUsageText();
        if (StringUtils.ok(str)) {
            r.append("\n\"").append(str).append("\"\n\n");
        }
        //Options
        Collection<ParamModel> parameters = proxy.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            r.append("\nOptions:\n--------\n");
            for (CommandModel.ParamModel p : proxy.getParameters()) {
                Param par = p.getParam();
                r.append("\n* ").append(p.getName());
                if (par.primary()) {
                    r.append(" [PRIMARY]");
                }
                r.append('\n');
                r.append("    - Type: ").append(CommandModelHtmlProvider.typeToValue(p)).append('\n');
                str = p.getLocalizedDescription();
                if (StringUtils.ok(str)) {
                    r.append("    - Description: ").append(StringUtils.escapeForHtml(str)).append('\n');
                }
                r.append("    - Optional: ").append(par.optional()).append('\n');
                r.append("    - Obsolete: ").append(par.obsolete()).append('\n');
                str = par.shortName();
                if (StringUtils.ok(str)) {
                    r.append("    - Short name: ").append(str).append('\n');
                }
                str = par.defaultValue();
                if (StringUtils.ok(str)) {
                    r.append("    - Default value: ").append(str).append('\n');
                }
                str = par.acceptableValues();
                if (StringUtils.ok(str)) {
                    r.append("    - Acceptable values: ").append(str).append('\n');
                }
                str = par.alias();
                if (StringUtils.ok(str)) {
                    r.append("    - Alias: ").append(str).append('\n');
                }
                if (par.primary() && par.multiple()) {
                    r.append("    - Multiple: true\n");
                }
            }
        }
        return r.toString();
    }
    
}
