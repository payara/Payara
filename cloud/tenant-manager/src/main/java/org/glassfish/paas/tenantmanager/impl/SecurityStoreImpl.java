package org.glassfish.paas.tenantmanager.impl;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.jvnet.hk2.annotations.Service;

/**
 * Default implementation for file realm.
 * 
 * @author Andriy Zhdanov
 *
 */
@Service
public class SecurityStoreImpl implements SecurityStore {

    /**
     * Creates file-user in default realm.
     */
    @Override
    public void create(String name, char[] password) {

        CommandInvocation cmd = commandRunner.getCommandInvocation("create-file-user", actionReport);
        ParameterMap map = new ParameterMap();
        map.add("userpassword", password.toString());
        map.add("username", name);
        // TODO: map.add("authrealmname", "file"); if default-realm differs from 'file'? 
        cmd.parameters(map);
        cmd.execute();
        if (actionReport.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            Throwable cause = actionReport.getFailureCause();
            // minimize end error message
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        } else if (actionReport.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            logger.fine(actionReport.getMessage());
        }
    }

    @Inject
    private CommandRunner commandRunner;

    @Inject
    @Named("plain")
    private ActionReport actionReport;

    @Inject
    private Logger logger;
}
