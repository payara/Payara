package org.glassfish.api.admin;

import java.lang.annotation.Annotation;

import org.glassfish.api.ActionReport;

/**
 * Empty implementation of CommandAspectImpl.
 * 
 * @author andriy.zhdanov
 */
public class CommandAspectBase implements CommandAspectImpl {

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(AdminCommand command, AdminCommandContext context,
            Job instance) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void done(AdminCommand command, Job instance) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdminCommand createWrapper(Annotation ann, CommandModel model,
	    AdminCommand command, ActionReport report) {
	return command;
    }

}
