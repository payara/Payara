/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.universal.collections.ManifestUtils;
import java.lang.reflect.Field;
import java.util.*;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * Create data structures that describe the command.
 *
 * @author Jerome Dochez
 * 
 */
@Service(name="_list-descriptors")
@PerLookup
@I18n("list.commands")
@AccessRequired(resource="domain", action="dump")
public class ListCommandDescriptorsCommand implements AdminCommand {
    @Inject
    ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {
        setAdminCommands();
        sort();
        
        for (AdminCommand cmd : adminCmds) {
            cliCmds.add(reflect(cmd));
        }
        ActionReport report = context.getActionReport();
        StringBuilder sb = new StringBuilder();
        sb.append("ALL COMMANDS: ").append(EOL);
        for(CLICommand cli : cliCmds) {
            sb.append(cli.toString()).append(EOL);
        }
        report.setMessage(sb.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
    
    private CLICommand reflect(AdminCommand cmd) {
        CLICommand cliCmd = new CLICommand(cmd);

        for (Field f : cmd.getClass().getDeclaredFields()) {
            final Param param = f.getAnnotation(Param.class);
            if (param==null)
                continue;
            
            Option option = new Option(param, f);
            cliCmd.options.add(option);
        }
        return cliCmd;
    }
 
    private void setAdminCommands() {
        adminCmds = new ArrayList<AdminCommand>();
        for (AdminCommand command : habitat.<AdminCommand>getAllServices(AdminCommand.class)) {
            adminCmds.add(command);
        }
    }

    private void sort() {
        Collections.sort(adminCmds, new Comparator<AdminCommand>() {
            @Override
            public int compare(AdminCommand c1, AdminCommand c2) {
                Service service1 = c1.getClass().getAnnotation(Service.class);
                Service service2 = c2.getClass().getAnnotation(Service.class);
                
                String name1 = (service1 != null) ? service1.name() : "";
                String name2 = (service2 != null) ? service2.name() : "";
                
                return name1.compareTo(name2);
            }
        }
        );
    }
    
    private static boolean ok(String s) {
        return s != null && s.length() > 0 && !s.equals("null");
    }
    
    private List<AdminCommand> adminCmds;
    private List<CLICommand> cliCmds = new LinkedList<CLICommand>();
    private final static String EOL = ManifestUtils.EOL_TOKEN;
    
    private static class CLICommand {
        CLICommand(AdminCommand adminCommand) {
            this.adminCommand = adminCommand;
            Service service = adminCommand.getClass().getAnnotation(Service.class);
            name = (service != null) ? service.name() : "";
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CLI Command: name=").append(name);
            sb.append(" class=").append(adminCommand.getClass().getName()).append(EOL);
            
            for(Option opt : options) {
                sb.append(opt.toString()).append(EOL);
            }
            return sb.toString();
        }

        AdminCommand adminCommand;
        String name;
        List<Option> options = new LinkedList<Option>();
    }
    private static class Option {
        Option(Param p, Field f) {
            final Class<?> ftype = f.getType();
            name = p.name();
            
            if(!ok(name)) {
                name = f.getName();
            }

            required = !p.optional();
            operand = p.primary();
            defaultValue = p.defaultValue();
            type = ftype;
        }
        @Override
        public String toString() {
            String s = "   Option:" + 
                    " name=" + name + 
                    " required=" + required +
                    " operand=" + operand +
                    " defaultValue=" + defaultValue +
                    " type=" + type.getName();
            return s;
        }

        private boolean required;
        private boolean operand;
        private String name;
        private String defaultValue;
        private Class<?> type;
    }
}
