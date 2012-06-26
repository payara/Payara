/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.hk2.component.InjectionResolver;
import com.sun.enterprise.config.serverbeans.CopyConfig;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.common.util.admin.GenericCommandModel;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.lang.reflect.Proxy;

/**
 * Implementation of the generic delete command
 *
 * @author Jerome Dochez
 */
@Scoped(PerLookup.class)
public class GenericDeleteCommand extends GenericCrudCommand implements AdminCommand {

    @Inject
    CommandRunner runner;

    @AccessRequired.To("delete")
    private ConfigBeanProxy tgt;
    
    private ConfigBean child;

    CommandModel model;
    Delete delete = null;    
    
    @Override
    public CommandModel getModel() {
        return model;
    }
       
    @Override
    public void postConstruct() {

        super.postConstruct();
        delete = targetMethod.getAnnotation(Delete.class);
        resolverType = delete.resolver();
        try {
            // we pass false for "useAnnotations" as the @Param declarations on
	    // the target type are not used for the Delete method parameters.
            model = new GenericCommandModel(targetType, false, delete.cluster(), delete.i18n(),
                    new LocalStringManagerImpl(targetType),
                    habitat.getComponent(DomDocument.class), commandName,
                    delete.resolver(), delete.decorator());
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
            logger.severe(msg);
            throw new ComponentException(msg, e);

        }
        
    }
    
    @Override
    void prepareInjection(final AdminCommandContext ctx) {
        super.prepareInjection(ctx);
        tgt = resolver.resolve(ctx, targetType);

        if (tgt != null) {
            child = (ConfigBean) ConfigBean.unwrap(tgt);
        }
    }

    @Override
    public void execute(final AdminCommandContext context) {

        final ActionReport result = context.getActionReport();
        
        //check if cluster software is installed else fail , see issue 12023
        //This will be revisited with issue 12900
        final CopyConfig command = (CopyConfig) runner
                .getCommand("copy-config", context.getActionReport(), context.getLogger());
        if (command == null ) {
            String msg = localStrings.getLocalString("cannot.execute.command",
                    "Cluster software is not installed");
            result.failure(logger,msg) ;
            return;
        }
        
        if (tgt==null) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericDeleteCommand.target_object_not_found",
                    "The CrudResolver {0} could not find the configuration object of type {1} where instances of {2} should be removed",
                    resolver.getClass().toString(), parentType, targetType);
            result.failure(logger, msg);
            return;
        }
        
        try {
            ConfigBeanProxy parentProxy = child.parent().createProxy();
            ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
                @Override
                public Object run(ConfigBeanProxy parentProxy) throws PropertyVetoException, TransactionFailure {
                    ConfigSupport._deleteChild(child.parent(), (WriteableView) Proxy.getInvocationHandler(parentProxy), child);

                    DeletionDecorator<ConfigBeanProxy, ConfigBeanProxy> decorator = habitat.getComponent(delete.decorator());
                    if (decorator==null) {
                        String msg = localStrings.getLocalString(GenericCrudCommand.class,
                                "GenericCreateCommand.deletion_decorator_not_found",
                                "The DeletionDecorator {0} could not be found in the habitat,is it annotated with @Service ?",
                                delete.decorator().toString());
                        result.failure(logger, msg);
                        throw new TransactionFailure(msg);
                    } else {
                        // inject the decorator with any parameters from the initial CLI invocation
                        manager.inject(decorator, paramResolver);

                        // invoke the decorator
                        decorator.decorate(context, parentProxy, tgt);

                    }
                    return null;
                }
            }, parentProxy);


        } catch(TransactionFailure e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericDeleteCommand.transaction_exception",
                    "Exception while deleting the configuration {0} :{1}",
                    child.typeName(), e.getMessage());
            result.failure(logger, msg);
        }

    }
}
