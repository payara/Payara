/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api.admin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.Job;
import org.glassfish.api.admin.WrappedAdminCommand;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Utility class for command framework. Currently it just provides hooks for
 * command runner, to extend command functionality using aspects. It might be
 * extended in future with more listeners for command life cycle phases, and
 * additional utility methods. This class is in development and is subject
 * to change.
 * 
 * @author andriy.zhdanov
 * 
 */
public final class CommandSupport {

    /**
     * Get parameter value for a command.
     * 
     * @param command
     * @param name parameter name
     * 
     * @return parameter value or null in case of any problem.
     */
    public static String getParamValue(AdminCommand command, String name) {
        return getParamValue(command, name, String.class);
    }

    /**
     * Get parameter value for a command.
     * 
     * @param command
     * @param name parameter name
     * @param paramType expected return type
     * 
     * @return parameter value or null in case of any problem.
     */
    public static <T> T getParamValue(AdminCommand command, String name, Class<T> paramType) {
        AdminCommand unwrappedCommand = getUnwrappedCommand(command);
        Class<?> commandClass = unwrappedCommand.getClass(); 
        for (final Field field : commandClass.getDeclaredFields()) {
            Param param = field.getAnnotation(Param.class);
            if (param != null && name.equals(CommandModel.getParamName(param, field))) {
                if (!paramType.isAssignableFrom(field.getType())) {
                    break; // return null
                }
                try {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {

                        @Override
                        public Object run() {
                            field.setAccessible(true);
                            return null;
                        }
                    });
                    Object value = field.get(unwrappedCommand);
                    return paramType.cast(value);
                } catch (IllegalAccessException e) {
                        throw new RuntimeException("Unexpected error", e);
                }
            }
        }
        return null;
    }

    /**
     * Execute aspects when command is just completely initialized, i..e
     * injected with parameters.
     */
    public static void init(final ServiceLocator serviceLocator,
        final AdminCommand command,
            final AdminCommandContext context,
            final Job instance) {

        processAspects(serviceLocator, command, new Function() {
            @Override
            public AdminCommand apply(Annotation a,
                    CommandAspectImpl<Annotation> aspect,
                    AdminCommand command) {
                aspect.init(a, command, context, instance);
                return command;
            }
        });
    }

    /**
     * Execute aspects when command is finished successfully or not.
     */
    public static void done(final ServiceLocator serviceLocator,
        final AdminCommand command,
            final Job instance, boolean isNotify) {

        processAspects(serviceLocator, command, new Function() {
            @Override
            public AdminCommand apply(Annotation a,
                    CommandAspectImpl<Annotation> aspect,
                    AdminCommand command) {
                aspect.done(a, command, instance);
                return command;
            }
        });
        if (isNotify) {
            CommandAspectFacade commandAspectFacade = serviceLocator.getService(CommandAspectFacade.class);
            if (commandAspectFacade != null)
                commandAspectFacade.done(command, instance);
        }
    }


    public static void done(final ServiceLocator serviceLocator,
            final AdminCommand command,
                final Job instance) {
        done(serviceLocator, command, instance,false);
    }


    /**
     * Execute wrapping aspects, see {@link org.glassfish.api.AsyncImpl} for example.
     */
    public static AdminCommand createWrappers(final ServiceLocator serviceLocator,
        final CommandModel model,
        final AdminCommand command,
            final ActionReport report) {

        return processAspects(serviceLocator, command, new Function() {
            @Override
            public AdminCommand apply(Annotation a,
                CommandAspectImpl<Annotation> cai,
                AdminCommand command) {
                return cai.createWrapper(a, model, command, report);
            }
        });
    }

    private static AdminCommand processAspects(ServiceLocator serviceLocator,
        AdminCommand command, Function function) {

        Annotation annotations[] = getUnwrappedCommand(command).getClass().getAnnotations();
        // TODO: annotations from wrapper class
        for (Annotation a : annotations) {
            CommandAspect ca = a.annotationType().getAnnotation(CommandAspect.class);
            if (ca != null) {
                CommandAspectImpl<Annotation> cai =
                        serviceLocator.<CommandAspectImpl<Annotation>>getService(ca.value());
                command = function.apply(a, cai, command);
            }
        }

        return command;
    }

    // Get root of wrapped command.
    private static AdminCommand getUnwrappedCommand(AdminCommand wrappedCommand) {
        if (wrappedCommand instanceof WrappedAdminCommand) {
            return ((WrappedAdminCommand)wrappedCommand).getWrappedCommand();
        }
        return wrappedCommand;
    }

    private interface Function {
        public AdminCommand apply(Annotation ca, CommandAspectImpl<Annotation> cai, AdminCommand object);
    }

}
