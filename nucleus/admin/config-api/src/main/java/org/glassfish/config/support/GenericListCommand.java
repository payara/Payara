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
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.common.util.admin.GenericCommandModel;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;

import java.util.List;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.AccessRequired.AccessCheck;

/**
 * Generic list command implementation.
 *
 * @author Jerome Dochez
 */
@Scoped(PerLookup.class)
public class GenericListCommand  extends GenericCrudCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    CommandModel model;
    Listing listing;
    
//    @AccessRequired.To("read")
    private ConfigBeanProxy parentBean;

    @Override
    public void postConstruct() {

        super.postConstruct();

        listing = targetMethod.getAnnotation(Listing.class);
        resolverType = listing.resolver();
        try {
	    // we pass false for "useAnnotations" as the @Param declarations on
	    // the target type are not used for the List method parameters.
            model = new GenericCommandModel(targetType, false, null, listing.i18n(),
                    new LocalStringManagerImpl(targetType),
                    habitat.getComponent(DomDocument.class), commandName, listing.resolver(), null);
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

    public Collection<? extends AccessCheck> getAccessChecks() {
        final Collection<AccessCheck> checks = new ArrayList<AccessCheck>();
        checks.add(new AccessCheck(AccessRequired.Util.resourceNameFromConfigBeanProxy(parentBean), "read"));
        return checks;
    }
    
    
    @Override
    void prepareInjection(final AdminCommandContext ctx) {
        super.prepareInjection(ctx);

        parentBean = resolver.resolve(ctx, parentType);
        
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
            List<ConfigBeanProxy> children = (List<ConfigBeanProxy>) targetMethod.invoke(parentBean);
            for (ConfigBeanProxy child : children) {
                Dom childDom = Dom.unwrap(child);
                String key = childDom.getKey();
                if (key==null) {
                    String msg = localStrings.getLocalString(GenericCrudCommand.class,
                            "GenericListCommand.element_has_no_key",
                            "The element {0} has not key attribute",
                            targetType);
                    result.failure(logger, msg);
                    return;

                }
                context.getActionReport().addSubActionsReport().setMessage(key);
            }
        } catch (Exception e) {
            String msg = localStrings.getLocalString(GenericCrudCommand.class,
                    "GenericCrudCommand.method_invocation_exception",
                    "Exception while invoking {0} method : {1}",
                    targetMethod.toString(), e.toString());
            result.failure(logger, msg, e);
        }
    }

    @Override
     public CommandModel getModel() {
        return model;
    }
}
