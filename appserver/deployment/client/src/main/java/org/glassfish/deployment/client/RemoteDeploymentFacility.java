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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.deployment.client;

import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.remote.RemoteCommand;
import org.glassfish.api.admin.CommandException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Implements DeploymentFacility, currently using the RemoteCommand to work with the
 * admin back-end.
 * <p>
 * Because RemoteCommand uses the http interface with the admin back-end it
 * is connectionless.  Clients of RemoteDeploymentFacility must still invoke
 * {@link #connect} before attempting to use the DF.
 * 
 * @author tjquinn
 */
public class RemoteDeploymentFacility extends AbstractDeploymentFacility {
    
    @Override
    protected boolean doConnect() {
        return true;
    }

    @Override
    public boolean doDisconnect() {
        return true;
    }
    
    @Override
    protected DFCommandRunner getDFCommandRunner(
            String commandName, 
            Map<String,Object> commandOptions,
            String[] operands) throws CommandException {
        return new RemoteCommandRunner(commandName, commandOptions, operands);
    }

    private class RemoteCommandRunner implements DFCommandRunner {

        private final String commandName;
        private final Map<String,Object> commandOptions;
        private final String[] operands;

        private RemoteCommandRunner(
                String commandName,
                Map<String,Object> commandOptions,
                String[] operands) {
            this.commandOptions = commandOptions;
            this.commandName = commandName;
            this.operands = operands;
        }

        public DFDeploymentStatus run() throws CommandException {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                String[] commandArgs = prepareRemoteCommandArguments(
                    commandName, 
                    commandOptions, 
                    operands
                    );
                Environment env = new Environment();
                ProgramOptions po = prepareRemoteCommandProgramOptions(env);
                RemoteCommand rc =
                    new RemoteCommand(commandName, po, env, "jsr-88/xml", baos);
                rc.executeAndReturnOutput(commandArgs);
                DFDeploymentStatus status = CommandXMLResultParser.parse(new ByteArrayInputStream(baos.toByteArray()));
                return status;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Assembles an argument list suitable for use by RemoteCommand from the
     * command, options, and operand.
     * @param commandName the command to execute
     * @param options Map, with each key an option name and each value (optionally) the corresponding option value
     * @param operands the operands to the command
     * @return argument list for RemoteCommand
     */
    protected String[] prepareRemoteCommandArguments(
            String commandName,
            Map<String,Object> options,
            String[] operands) {

        ArrayList<String> result = new ArrayList<String>();
        result.add(commandName);
        if (options == null) {
            options = Collections.EMPTY_MAP;
        }
        for (Map.Entry<String,Object> entry : options.entrySet()) {
            result.add("--" + entry.getKey() + "=" + convertValue(entry.getValue()));
        }

        if (operands != null) {
            for (String o : operands) {
                result.add(o);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    protected ProgramOptions prepareRemoteCommandProgramOptions(
            Environment env) throws CommandException {
        /*
         * Add the authentication information from the
         * caller-provided connection identifier.
         */
        ServerConnectionIdentifier targetDAS = getTargetDAS();
        ProgramOptions po = new ProgramOptions(env);
        po.setHost(targetDAS.getHostName());
        po.setPort(targetDAS.getHostPort());
        po.setUser(targetDAS.getUserName());
        po.setSecure(targetDAS.isSecure());
        po.setPassword(getTargetDAS().getPassword(), ProgramOptions.PasswordLocation.LOCAL_PASSWORD);
        po.setOptionsSet(true);
        return po;
    }

    private Object convertValue(Object value) {
        if (value instanceof Properties) {
            StringBuilder sb = new StringBuilder();
            Properties p = (Properties) value;
            for (Map.Entry<Object,Object> entry : p.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(":");
                }
                sb.append((String) entry.getKey() + "=" + (String) entry.getValue());
            }
            return sb.toString();
        } else {
            return value;
        }
    }
}
