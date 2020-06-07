/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.admin;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;

import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.customvalidators.JavaClassName;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.universal.collections.ManifestUtils;
import com.sun.enterprise.admin.report.PropsFileActionReporter;
import org.glassfish.api.admin.AccessRequired;

/**
 * Dumps a sorted list of all registered Contract's in the Habitat
 *
 * <p>
 * Useful for debugging and developing new Contract's
 * @author Byron Nevins
 * @param <i>
 */
@Service(name = "_get-habitat-info")
@PerLookup
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="_get-habitat-info", 
        description="_get-habitat-info")
})
@GetHabitatInfo.Constraint
@AccessRequired(resource="domain", action="dump")
public class GetHabitatInfo implements AdminCommand {
    @Inject
    ServiceLocator serviceLocator;
    
    @Inject
    ModulesRegistry modulesRegistry;
    
    @JavaClassName
    @Param(primary = true, optional = true)
    String contract;
    
    @Pattern(regexp="true|false")
    @Param(optional = true)
    String started = "false";

    @Override
    public void execute(AdminCommandContext context) {
        StringBuilder sb = new StringBuilder();
        if (contract == null) {
            dumpContracts(sb);
            dumpModules(sb);
            dumpTypes(sb);
        }
        else {
            dumpInhabitantsImplementingContractPattern(contract, sb);
        }


        String msg = sb.toString();
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ExitCode.SUCCESS);

        if (report instanceof PropsFileActionReporter) {
            msg = ManifestUtils.encode(msg);
        }
        report.setMessage(msg);
    }

    private void dumpContracts(StringBuilder sb) {
        // Probably not very efficient but it is not a factor for this rarely-used
        // user-called command...

        sb.append("\n*********** Sorted List of all Registered Contracts in the Habitat **************\n");
        List<ActiveDescriptor<?>> allDescriptors = serviceLocator.getDescriptors(BuilderHelper.allFilter());
        
        SortedSet<String> allContracts = new TreeSet<String>();
        for (ActiveDescriptor<?> aDescriptor : allDescriptors) {
            allContracts.addAll(aDescriptor.getAdvertisedContracts());
        }

        // now the contracts are sorted...

        Iterator<String> it = allContracts.iterator();
        for (int i = 1; it.hasNext(); i++) {
            sb.append("Contract-" + i + ": " + it.next() + "\n");
        }
    }

    private void dumpInhabitantsImplementingContractPattern(String pattern, StringBuilder sb) {
        sb.append("\n*********** List of all services for contract named like " + contract + " **************\n");
        List<ActiveDescriptor<?>> allDescriptors = serviceLocator.getDescriptors(BuilderHelper.allFilter());
        HashSet<String> allContracts = new HashSet<String>();
        for (ActiveDescriptor<?> aDescriptor : allDescriptors) {
            allContracts.addAll(aDescriptor.getAdvertisedContracts());
        }
        
        Iterator<String> it = allContracts.iterator();
        while (it.hasNext()) {
            String cn = it.next();
            if (cn.toLowerCase(Locale.ENGLISH).indexOf(pattern.toLowerCase(Locale.ENGLISH)) < 0)
                continue;
            sb.append("\n-----------------------------\n");
            for ( ActiveDescriptor<?> descriptor : serviceLocator.getDescriptors(BuilderHelper.createContractFilter(cn))) {
                sb.append("Inhabitant-Metadata: " + descriptor.getMetadata());
                sb.append("\n");
                boolean isStarted = Boolean.parseBoolean(started);
                if (isStarted) {
                	ServiceHandle<?> handle = serviceLocator.getServiceHandle(descriptor);
                    
                    sb.append((handle.isActive() ? " started" : " not started"));
                }
            }
        }
    }

    private void dumpTypes(StringBuilder sb) {
        sb.append("\n\n*********** Sorted List of all Types in the Habitat **************\n\n");
        List<ActiveDescriptor<?>> allDescriptors = serviceLocator.getDescriptors(BuilderHelper.allFilter());
        HashSet<String> allTypes = new HashSet<String>();
        for (ActiveDescriptor<?> aDescriptor : allDescriptors) {
            allTypes.add(aDescriptor.getImplementation());
        }
        
        Iterator<String> it = allTypes.iterator();

        if (it == null)  //PP (paranoid programmer)
            return;

        SortedSet<String> types = new TreeSet<String>();

        while (it.hasNext()) {
            types.add(it.next());
        }

        // now the types are sorted...

        it = types.iterator();

        for (int i = 1; it.hasNext(); i++) {
            sb.append("Type-" + i + ": " + it.next() + "\n");
        }
    }

    private void dumpModules(StringBuilder sb) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        modulesRegistry.dumpState(new PrintStream(baos));
        sb.append("\n\n*********** List of all Registered Modules **************\n\n");
        sb.append(baos.toString());
    }
    /*
     * NOTE: this valdation is here just to test the AdminCommand validation 
     * implementation.
     */
    @Retention(RUNTIME)
    @Target({TYPE})
    @javax.validation.Constraint(validatedBy = GetHabitatInfo.Validator.class)
    public static @interface Constraint {
        String message() default "The contract argument is test but started is true.";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {}; 
    }
    
    public static class Validator 
        implements ConstraintValidator<GetHabitatInfo.Constraint, GetHabitatInfo>, Payload {

        @Override
        public void initialize(final GetHabitatInfo.Constraint constraint) { }

        @Override
        public boolean isValid(final GetHabitatInfo bean,
            final ConstraintValidatorContext constraintValidatorContext) {
            if (bean.contract.equals("test") && bean.started.equals("true"))
                return false;
            return true;
        }
    }

}
