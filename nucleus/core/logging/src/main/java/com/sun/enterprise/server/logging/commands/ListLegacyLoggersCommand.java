package com.sun.enterprise.server.logging.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import java.util.Enumeration;
import java.util.logging.LogManager;

@Service(name = "list-legacy-loggers")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(RuntimeType.DAS) // or ALL if you want instances too
public class ListLegacyLoggersCommand implements AdminCommand {

    @Inject
    private ActionReport report;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        Enumeration<String> names =
                LogManager.getLogManager().getLoggerNames();

        int count = 0;
        StringBuilder output = new StringBuilder();

        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name != null && name.startsWith("javax.")) {
                count++;
                output.append("LEGACY LOGGER: ").append(name).append("\n");
            }
        }

        output.append("Total legacy loggers: ").append(count);

        report.setMessage(output.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}