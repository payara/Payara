/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.util.OS;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.common.util.admin.CommandModelImpl;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author jonathan
 */
@Service(name = "generate-bash-autocomplete")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@AccessRequired(resource = "domain", action = "read")
public class GenerateBashAutoCompletionCommand implements AdminCommand {

    @Param(optional = true, primary = true, name = "file")
    String filePath;

    @Param(optional = true, defaultValue = "false")
    Boolean force;

    @Inject
    ServiceLocator habitat;

    @Inject
    ServerContext serverContext;

    private File file;

    // constants for writing bash script
    private static final String VARNAME = "__asadmin_commands=\"";
    private static final String BASH_FUNCTION = "_asadmin()\n"
            + "{\n"
            + "\n"
            + "    local cur prev opts\n"
            + "    COMREPLY=()\n"
            + "    cur=\"${COMP_WORDS[COMP_CWORD]}\"\n"
            + "    #prev=\"${COMP_WORDS[COMP_CWORD-1]}\"\n"
            + "\n"
            + "    COMPREPLY=( $(compgen -W \"${__asadmin_commands}\" -- ${cur}))\n"
            + "\n"
            + "}\n";
    private static final String COMPLETE_CALL = "complete -F _asadmin asadmin";
    private static final String ADD_PATH = "PATH=$PATH:";
    
    private static final String DEFAULT_FILE = File.separator + "bin" + File.separator + "bash_autocomplete";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        if (!validate()) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Unable to write to file at " + filePath + ", see server.log for details");
            return;
        }

        List<String> commandNames = new ArrayList<>();
        List<ServiceHandle<AdminCommand>> allCommandHandles = habitat.getAllServiceHandles(AdminCommand.class, new Annotation[0]);
        for (ServiceHandle<AdminCommand> commandHandler : allCommandHandles) {
            AdminCommand trueCommand = commandHandler.getService();
            CommandModel model = new CommandModelImpl(trueCommand.getClass());
            if (model.getCommandName() == null || model.getCommandName().startsWith("_")) {
                continue;
            }
            commandNames.add(model.getCommandName());
            for (String param : model.getParametersNames()) {
                //    System.out.print("\t" + param);
            }
        }
        if (writeCommands(commandNames)){
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage("Written bash autocomplete file to " + filePath);
        } else {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Unable to write to file at " + filePath + ", see server.log for details");
        }
        
    }

    private boolean validate() {

        try {
            if (filePath == null) {
                filePath = serverContext.getInstallRoot().getCanonicalPath() + DEFAULT_FILE;
            }
            file = new File(filePath);
            if ((file.exists() && file.isFile() && force) || file.createNewFile()) {
                return true;
            }
        } catch (IOException ex) {
            Logger.getLogger(GenerateBashAutoCompletionCommand.class.getName()).log(Level.WARNING, "Unable to create file at {0}:{1}", new Object[]{filePath, ex.getMessage()});
        }
        return false;
    }
    
    

    /**
     * 
     * @param commands
     * @return true if written to file successfully, false otherwise
     */
    private boolean writeCommands(List<String> commands) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            //Write the opening to the list of commands
            writer.write(VARNAME);
            //Write all the commands to the list
            for (String command: commands){
                writer.write(command);
                writer.newLine();
            }
            writer.write("\"");
            writer.newLine();
            //Write function to generate the commands
            writer.write(BASH_FUNCTION);
            writer.newLine();
            //write function to tell bash what function the autocompletion is for
            writer.write(COMPLETE_CALL);
            writer.newLine();
            //Add directory of payara5/glassfish/bin to the path
            writer.write(ADD_PATH);
            writer.write(serverContext.getInstallRoot().getPath() + File.separator + "bin");
            //flush the buffer
            writer.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(GenerateBashAutoCompletionCommand.class.getName()).log(Level.WARNING, "Unable to write to file at " + filePath, ex);
        }
        return false;

    }

}
