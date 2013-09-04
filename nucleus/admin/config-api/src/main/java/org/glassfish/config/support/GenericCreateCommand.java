/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.AnnotationUtil;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.ExceptionUtil;

import java.util.logging.Level;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.config.Named;
import org.glassfish.common.util.admin.GenericCommandModel;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;

import java.util.List;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;

import javax.security.auth.Subject;

import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;

/**
 * Generic create command implementation.
 *
 * This command can create POJO configuration objects from an asadmin command
 * invocation parameters.
 *
 * So far, such POJO must be ConfigBeanProxy subclasses and be annotated with the
 * {@link org.glassfish.api.Param} annotation to property function. 
 *
 * @author Jerome Dochez
 */
@PerLookup
public class GenericCreateCommand extends GenericCrudCommand implements AdminCommand, 
    AdminCommandSecurity.AccessCheckProvider {

    GenericCommandModel model;
    Create create;
    
    private ConfigBeanProxy parentBean;
    
    @Override
    public void postConstruct() {

        super.postConstruct();

        create = getAnnotation(targetMethod, Create.class);
        resolverType = create.resolver();
        try {
            model = new GenericCommandModel(targetType, true, create.cluster(), create.i18n(),
                    new LocalStringManagerImpl(targetType),
                    habitat.<DomDocument>getService(DomDocument.class),
                    commandName, AnnotationUtil.presentTransitive(ManagedJob.class, create.decorator()), create.resolver(), create.decorator());
            if (logger.isLoggable(level)) {
                for (String paramName : model.getParametersNames()) {
                    CommandModel.ParamModel param = model.getModelFor(paramName);
                    logger.log(Level.FINE, "I take {0} parameters", param.getName());
                }
            }
        } catch(Exception e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCreateCommand.command_model_exception",
                    "Exception while creating the command model for the generic command {0} : {1}",
                    commandName, e.getMessage());
            logger.log(Level.SEVERE, ConfigApiLoggerInfo.GENERIC_CREATE_CMD_FAILED, commandName);
            throw new RuntimeException(msg, e);

        }
        
    }

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final Collection<AccessCheck> checks = new ArrayList<AccessCheck>();
        checks.add(new AccessCheck(parentBean, (Class<? extends ConfigBeanProxy>) targetType, "create"));
        return checks;
    }
    
    
    @Override
    void prepareInjection(final AdminCommandContext ctx) {
        super.prepareInjection(ctx);
        parentBean = resolver.resolve(ctx, parentType);
    }

    @Override
    public boolean preAuthorization(final AdminCommandContext adminCommandContext) {
        if ( ! super.preAuthorization(adminCommandContext)) {
            return false;
        }
        prepareInjection(adminCommandContext);
        return true;
    }

    
    @Override
    public void execute(final AdminCommandContext context) {

        final ActionReport result = context.getActionReport();
        if (parentBean==null) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCreateCommand.target_object_not_found",
                    "The CrudResolver {0} could not find the configuration object of type {1} where instances of {2} should be added",
                    resolver.getClass().toString(), parentType, targetType);
            result.failure(logger, msg);
            return;
        }
        
        try {
            ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy> () {
                @Override
                public Object run(ConfigBeanProxy writableParent) throws PropertyVetoException, TransactionFailure {


                    ConfigBeanProxy childBean = writableParent.createChild(targetType);
                    manager.inject(childBean, targetType, getInjectionResolver());

                    String name = null;
                    if (Named.class.isAssignableFrom(targetType)) {
                        name = ((Named) childBean).getName();

                    }

                    // check that such instance does not exist yet...
                    if (name!=null) {
                        Object cbp = habitat.getService(targetType, name);
                        if (cbp!=null) {
                            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                    "GenericCreateCommand.already_existing_instance",
                                    "A {0} instance with a \"{1}\" name already exist in the configuration",
                                    targetType.getSimpleName(), name);
                            result.failure(logger, msg);
                            throw new TransactionFailure(msg);
                        }
                    }

                    try {
                        if (targetMethod.getParameterTypes().length==0) {
                            // return type must be a list to which we add our child.
                            Object result = targetMethod.invoke(writableParent);
                            if (result instanceof List) {                                
                                List<ConfigBeanProxy> children = List.class.cast(result);
                                children.add(childBean);
                            }
                        } else {
                            targetMethod.invoke(writableParent, childBean);
                        }
                    } catch (Exception e) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCrudCommand.method_invocation_exception",
                                "Exception while invoking {0} method : {1}",
                                targetMethod.toString(), e.toString());
                        result.failure(logger, msg, e);
                        throw new TransactionFailure(msg,e);
                    }

                    
                    CreationDecorator<ConfigBeanProxy> decorator = null;
                    if (create != null) {
                        decorator = habitat.getService(create.decorator());
                    }
                    if (decorator==null) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCreateCommand.decorator_not_found",
                                "The CreationDecorator {0} could not be found in the habitat, is it annotated with @Service ?",
                                create == null ? "null" : create.decorator().toString());
                        result.failure(logger, msg);
                        throw new TransactionFailure(msg);
                    } else {
                        // inject the decorator with any parameters from the initial CLI invocation
                        manager.inject(decorator, paramResolver);

                        // invoke the decorator
                        decorator.decorate(context, childBean);
                    }

                    return childBean;
                }
            }, parentBean);
        } catch(TransactionFailure e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCreateCommand.transaction_exception",
                    "Exception while adding the new configuration : {0} ",
                    getRootCauseMessage(e));
            result.failure(logger, msg);
        }
    }

    @Override
    public CommandModel getModel() {
        return model;
    }

    /**
     * Return the message from the root cause of the exception. If the root
     * cause has no message, then return the passed exception's message.
     */
    private String getRootCauseMessage(Exception e) {
        String msg = ExceptionUtil.getRootCause(e).getMessage();
        if (msg != null && msg.length() > 0) {
            return msg;
        } else {
            return e.getMessage();
        }
    }
    
    @Override
    public Class getDecoratorClass() {
        if (create != null) {
            return create.decorator();
        } else {
            return null;
        }
    }

}
