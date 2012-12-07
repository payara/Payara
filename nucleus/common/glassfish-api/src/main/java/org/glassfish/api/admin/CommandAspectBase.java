package org.glassfish.api.admin;

import java.lang.annotation.Annotation;

import org.glassfish.api.ActionReport;

/**
 * Empty implementation of CommandAspectImpl.
 * 
 * @author andriy.zhdanov
 */
public class CommandAspectBase<T extends Annotation> implements CommandAspectImpl<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(T ann, AdminCommand command, AdminCommandContext context,
            Job instance) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void done(T ann, AdminCommand command, Job instance) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdminCommand createWrapper(T ann, CommandModel model,
	    AdminCommand command, ActionReport report) {
	return command;
    }

}
