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

package org.glassfish.loadbalancer.admin.cli;

import java.util.logging.Logger;
import java.util.Properties;
import java.util.regex.Pattern;

import java.beans.PropertyVetoException;
import javax.security.auth.Subject;


import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.api.ActionReport;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;

import org.glassfish.loadbalancer.config.LoadBalancers;
import org.glassfish.loadbalancer.config.LoadBalancer;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Domain;

import org.glassfish.api.admin.*;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import static org.glassfish.config.support.Constants.*;

import org.glassfish.api.admin.CommandRunner.CommandInvocation;

import javax.inject.Inject;

/**
  * This method supports the create-http-lb CLI command. It creates a lb-config, cluster-ref, health-checker by using
  * the given parameters.
  * @param loadbalancername the name for the load-balancer element that will be created
  * @param target cluster-ref or server-ref parameter of lb-config
  * @param options Map of option name and option value. The valid options are
  *          responsetimeout response-timeout-in-seconds attribute of lb-config
  *          httpsrouting https-routing parameter of lb-config
  *          reloadinterval reload-poll-interval-in-seconds parameter of lb-config
  *          monitor monitoring-enabled parameter of lb-config
  *          routecookie route-cookie-enabled parameter of lb-config
  *          lb-policy load balancing policy to be used for the cluster target
  *          lb-policy-module specifies the path to the shared library implementing the user-defined policy
  *          healthcheckerurl url attribute of health-checker
  *          healthcheckerinterval interval-in-seconds parameter of health-checker
  *          healthcheckertimeout timeout-in-seconds parameter of health-checker
  * @author Yamini K B
  */
@Service(name = "create-http-lb")
@PerLookup
@I18n("create.http.lb")
@TargetType(value={CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@org.glassfish.api.admin.ExecuteOn({RuntimeType.DAS})
public final class CreateHTTPLoadBalancerCommand extends LBCommandsBase
        implements AdminCommand {

    @Param (optional=false)
    String devicehost;

    @Param (optional=false)
    String deviceport;

    @Param (optional=true)
    String target;

    @Param (optional=true)
    String sslproxyhost;

    @Param (optional=true)
    String sslproxyport;

    @Param (optional=true)
    String lbpolicy;

    @Param (optional=true)
    String lbpolicymodule;

    @Param (optional=true, defaultValue="/")
    String healthcheckerurl;

    @Param (optional=true, defaultValue="30")
    String healthcheckerinterval;

    @Param (optional=true, defaultValue="10")
    String healthcheckertimeout;

    @Param (optional=true)
    String lbenableallinstances;

    @Param (optional=true)
    String lbenableallapplications;

    @Param (optional=true)
    String lbweight;

    @Param (optional=true, defaultValue="60")
    String responsetimeout;

    @Param (optional=true, defaultValue="false")
    Boolean httpsrouting;

    @Param (optional=true, defaultValue="60")
    String reloadinterval;

    @Param (optional=true, defaultValue="false")
    Boolean monitor;

    @Param (optional=true, defaultValue="true")
    Boolean routecookie;

    @Param (obsolete=true, optional=true)
    Boolean autoapplyenabled;

    @Param(optional=true, name="property", separator=':')
    Properties properties;

    @Param (primary=true)
    String load_balancer_name;

    @Inject
    Target tgt;

    @Inject
    Logger logger;

    @Inject
    CommandRunner runner;

    @Inject
    Applications applications;

    private ActionReport report;

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreateHTTPLoadBalancerCommand.class);

    
    @Override
    public void execute(AdminCommandContext context) {
        //final Logger logger = context.getLogger();

        report = context.getActionReport();

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);        

        if (load_balancer_name == null) {
            String msg = localStrings.getLocalString("NullLBName", "Load balancer name cannot be null");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if(!Pattern.matches(NAME_REGEX, load_balancer_name)){
            String msg = localStrings.getLocalString("loadbalancer.invalid.name", "Invalid load-balancer name");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        LoadBalancers loadBalancers = domain.getExtensionByType(LoadBalancers.class);
        if (loadBalancers != null && loadBalancers.getLoadBalancer(load_balancer_name) != null) {
            String msg = localStrings.getLocalString("LBExists", "Load balancer {0} already exists", load_balancer_name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if (target != null && !tgt.isValid(target)) {
            String msg = localStrings.getLocalString("InvalidTarget", "Invalid target", target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        boolean isCluster = tgt.isCluster(target);
        String lbConfigName = load_balancer_name + "_LB_CONFIG";        
        
        if(!isCluster){
            if((lbpolicy!=null) || (lbpolicymodule!=null)){
                String msg = localStrings.getLocalString("NotCluster",
                        "{0} not a cluster", target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
        }

        try {
            createLBConfig(lbConfigName, context.getSubject());

            if (target != null) {
                final CreateHTTPLBRefCommand command = (CreateHTTPLBRefCommand)runner
                        .getCommand("create-http-lb-ref", report, context.getLogger());
                command.target = target;
                //command.lbname = load_balancer_name;
                command.config = lbConfigName;
                command.lbpolicy = lbpolicy;
                command.lbpolicymodule = lbpolicymodule;
                command.healthcheckerurl = healthcheckerurl;
                command.healthcheckerinterval = healthcheckerinterval;
                command.healthcheckertimeout = healthcheckertimeout;
                command.lbenableallinstances = lbenableallinstances;
                command.lbenableallapplications = lbenableallapplications;
                command.lbweight = lbweight;
                command.execute(context);
                checkCommandStatus(context);
            }
        } catch (CommandException e) {
            String msg = e.getLocalizedMessage();
            logger.warning(msg);
//                    report.setActionExitCode(ExitCode.FAILURE);
//                    report.setMessage(msg);
//                    return;
        }
        addLoadBalancer(lbConfigName);

        if (isCluster && lbweight != null) {
            try {
                final ConfigureLBWeightCommand command = (ConfigureLBWeightCommand)runner
                        .getCommand("configure-lb-weight", report, context.getLogger());
                command.weights=lbweight;
                command.cluster=target;
                command.execute(context);
                checkCommandStatus(context);
            } catch (CommandException e) {
                String msg = e.getLocalizedMessage();
                logger.warning(msg);
    //                    report.setActionExitCode(ExitCode.FAILURE);
    //                    report.setMessage(msg);
    //                    return;
            }
        }
    }

    private void createLBConfig(String config, final Subject subject) throws CommandException {
        CommandInvocation ci = runner.getCommandInvocation("create-http-lb-config", report, subject);
        ParameterMap map = new ParameterMap();
        //map.add("target", target);
        map.add("responsetimeout", responsetimeout);
        map.add("httpsrouting", httpsrouting==null ? null : httpsrouting.toString());
        map.add("reloadinterval", reloadinterval);
        map.add("monitor", monitor==null ? null : monitor.toString());
        map.add("routecookie", routecookie==null ? null : routecookie.toString());
        map.add("name", config);
        ci.parameters(map);
        ci.execute();
    }

   private void addLoadBalancer(final String lbConfigName) {
       LoadBalancers loadBalancers = domain.getExtensionByType(LoadBalancers.class);
       //create load-balancers parent element if it does not exist
       if (loadBalancers == null) {
           Transaction transaction = new Transaction();
           try {
               ConfigBeanProxy domainProxy = transaction.enroll(domain);
               loadBalancers = domainProxy.createChild(LoadBalancers.class);
               ((Domain) domainProxy).getExtensions().add(loadBalancers);
               transaction.commit();
           } catch (TransactionFailure ex) {
               transaction.rollback();
               String msg = localStrings.getLocalString("FailedToUpdateLB",
                       "Failed to update load-balancers");
               report.setActionExitCode(ActionReport.ExitCode.FAILURE);
               report.setMessage(msg);
               return;
           } catch (RetryableException ex) {
               transaction.rollback();
               String msg = localStrings.getLocalString("FailedToUpdateLB",
                       "Failed to update load-balancers");
               report.setActionExitCode(ActionReport.ExitCode.FAILURE);
               report.setMessage(msg);
               return;
           }
       }

        try {
            ConfigSupport.apply(new SingleConfigCode<LoadBalancers>() {
                    @Override
                    public Object run(LoadBalancers param) throws PropertyVetoException, TransactionFailure {
                        LoadBalancer lb = param.createChild(LoadBalancer.class);
                        lb.setDeviceHost(devicehost);
                        lb.setDevicePort(deviceport);
                        lb.setLbConfigName(lbConfigName);
                        lb.setName(load_balancer_name);

                        // add properties
                        if (properties != null) {
                            for (Object propname: properties.keySet()) {
                                Property newprop = lb.createChild(Property.class);
                                newprop.setName((String) propname);
                                newprop.setValue(properties.getProperty((String) propname));
                                lb.getProperty().add(newprop);
                            }
                        }

                        if (sslproxyhost != null) {
                            Property newprop = lb.createChild(Property.class);
                            newprop.setName("ssl-proxy-host");
                            newprop.setValue(sslproxyhost);
                            lb.getProperty().add(newprop);
                        }

                        if (sslproxyport != null) {
                            Property newprop = lb.createChild(Property.class);
                            newprop.setName("ssl-proxy-port");
                            newprop.setValue(sslproxyport);
                            lb.getProperty().add(newprop);
                        }
                        param.getLoadBalancer().add(lb);
                        return Boolean.TRUE;
                    }
            }, loadBalancers);
        } catch (TransactionFailure ex) {
            String msg = localStrings.getLocalString("FailedToUpdateLB",
                    "Failed to update load-balancers");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
    }
}
