package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.remote.RemoteCommand;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 24/2/12
 * Time: 11:38 AM
 * To change this template use File | Settings | File Templates.
 */
@org.jvnet.hk2.annotations.Service(name = "tail-service-log-file")
@Scoped(PerLookup.class)
public class TailServiceLogFile extends CLICommand {

    private String commandName = "_hidden-tail-service-log-file";

    @Param(name = "filepointer", optional = true, defaultValue = "0")
    private String filepointer;

    @Param(name = "servicename", optional = false)
    private String serviceName;

    @Param(name = "logtype", optional = false)
    private String logtype;

    @Override
    protected int executeCommand() throws CommandException {

        while (true) {
            RemoteCommand cmd = new RemoteCommand(commandName, programOpts, env);
            Map<String, String> attr = cmd.executeAndReturnAttributes(getParams());

            String fileData = attr.get("filedata_value");
            String filePointer = attr.get("filepointer_value");

            if (fileData != null && fileData.trim().length() > 0) {
                System.out.println(fileData);
            }
            this.filepointer = filePointer;
        }
    }

private String[] getParams() {
        List<String> ss = new ArrayList<String>();

        ss.add(commandName);
        ss.add("--filepointer");
        ss.add(filepointer);
        ss.add("--serviceName");
        ss.add(serviceName);
        ss.add("--logtype");
        ss.add(logtype);
        return ss.toArray(new String[ss.size()]);
    }
}
