/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portion Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.cli;

import java.util.*;
import org.jvnet.hk2.annotations.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.util.logging.Level;

/**
 * A local export command.
 *  
 * @author Bill Shannon
 */
@Service(name = "export")
@PerLookup
public class ExportCommand extends CLICommand {

    private static final LocalStringsImpl strings = new LocalStringsImpl(ExportCommand.class);

    @Param(name = "environment-variable", primary = true, optional = true, multiple = true)
    private List<String> vars;

    @Override
    public int executeCommand() throws CommandException, CommandValidationException {
        int ret = 0;    // by default, success

        // if no operands, print out everything
        if (vars == null || vars.isEmpty()) {
            for (Map.Entry<String, String> e : env.entrySet())
                logger.log(Level.INFO, "{0} = {1}", new Object[]{e.getKey(), quote(e.getValue())});
        } else {
            // otherwise, process each operand
            for (String arg : vars) {
                // separate into name and value
                String envname;
                String value;
                int eq = arg.indexOf('=');
                if (eq < 0) {   // no value
                    envname = arg;
                    value = null;
                } else {
                    envname = arg.substring(0, eq);
                    value = arg.substring(eq + 1);
                }

                // check that name is legitimate
                if (!envname.startsWith(Environment.getPrefix())) {
                    logger.info(strings.get("badEnvVarSet", envname, Environment.getPrefix()));
                    ret = -1;
                    continue;
                }

                // if no value, print it, otherwise set it
                if (value == null) {
                    String v = env.get(envname);
                    if (v != null)
                        logger.log(Level.INFO, "{0} = {1}", new Object[]{envname, v});
                } else
                    env.put(envname, value);
            }
        }
        return ret;
    }
}
