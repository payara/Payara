/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.hazelcast.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import java.util.ArrayList;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;

/**
 *
 * @author jonathan
 */
@Service(name = "create-hazelcast-instance")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("create.hazelcast.instance")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "create-hazelcast-instance",
            description = "Create Hazelcast Instance")
})
public class CreateHazelcastInstance implements AdminCommand {

    //Create instance parameters
    @Param(name = "node", alias = "nodeagent")
    String node;
    
    @Param(name = "config", optional = true)
    @I18n("generic.config")
    String configRef;
    
    @Param(name = "lbenabled", optional = true)
    private Boolean lbEnabled;
    
    @Param(name = "checkports", optional = true, defaultValue = "true")
    private Boolean checkPorts;
    
    @Param(optional = true, defaultValue = "false")
    private Boolean terse;
    
    @Param(name = "portbase", optional = true)
    private String portBase;
    
    @Param(name = "systemproperties", optional = true, separator = ':')
    private String systemProperties;
    
    @Param(name = "instance_name", primary = true)
    private String instance;
    
    //Hazelcast Parameters
    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;
    
    @Param(name = "enabled", optional = true, defaultValue = "true")
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "true")
    private Boolean dynamic;

    @Param(name = "hazelcastConfigurationFile", shortName = "f", optional = true)
    private String configFile;

    @Param(name = "startPort", optional = true)
    private String startPort;

    @Param(name = "multicastGroup", shortName = "g", optional = true)
    private String multiCastGroup;

    @Param(name = "multicastPort", optional = true)
    private String multicastPort;
        
    @Param(name = "clusterName", optional = true)
    private String hzClusterName;

    @Param(name = "clusterPassword", optional = true)
    private String hzClusterPassword;    

    @Param(name = "jndiName", shortName = "j", optional = true)
    private String jndiName;
    
    @Param(name = "licenseKey", shortName = "lk", optional = true)
    private String licenseKey;
    
    @Param(name = "lite", optional = true, defaultValue = "false")
    private Boolean lite;

    @Param(name = "hostawareParitioning", optional = true, defaultValue = "false")
    private Boolean hostawarePartitioning;  
    
    //Persistence paramers
    @Param(name = "webPersistence", optional = true, defaultValue = "hazelcast", acceptableValues="memory, file, hazelcast, replicated")
    private String webPersistence;
    
    @Param(name = "ejbPersistence", optional = true, defaultValue = "hazelcast", acceptableValues="file, hazelcast, replicated")
    private String ejbPersistence;
    
    //Other variables
    @Inject
    protected Logger logger;
    
    @Inject
    ServiceLocator serviceLocator;
    
    @Override
    public void execute(AdminCommandContext context) {
        
        try {
            
            CommandRunner run = serviceLocator.getService(CommandRunner.class);
            CommandInvocation createinstance =  run.getCommandInvocation("create-instance", context.getActionReport(), context.getSubject());
            ParameterMap instanceArgs = prepareCreateInstanceArgs();
            createinstance.parameters(instanceArgs);
            createinstance.execute();

            CommandInvocation hazelcastConfig = run.getCommandInvocation("set-hazelcast-configuration", context.getActionReport(), context.getSubject());
            RemoteCLICommand setHazelcastConfig = new RemoteCLICommand();
            ParameterMap hazelcastArgs = prepareHazelcastArgs();
            hazelcastConfig.parameters(hazelcastArgs);
            hazelcastConfig.execute();
            
            CommandInvocation webPersistenceCommand = run.getCommandInvocation("set", context.getActionReport(), context.getSubject());
            ParameterMap webPArgs = new ParameterMap();
            webPArgs.add("DEFAULT", "configs.config." + instance + "-config.availability-service.web-container-availability.persistence-type=" + webPersistence);
            webPersistenceCommand.parameters(webPArgs);
            webPersistenceCommand.execute();
            
            CommandInvocation ejbPersist = run.getCommandInvocation("set", context.getActionReport(), context.getSubject());
            ParameterMap ejbPArgs = new ParameterMap();
            ejbPArgs.add("DEFAULT","configs.config." + instance + "-config.availability-service.ejb-container-availability.sfsb-ha-persistence-type=" + ejbPersistence);
            ejbPersist.parameters(ejbPArgs);
            ejbPersist.execute();
            
            
        } catch (Exception e){
            
            logger.log(Level.SEVERE,"Error executing operation" ,e);
            throw new UnsupportedOperationException(e);
        }
        
        
    }
    
    private ParameterMap prepareCreateInstanceArgs(){
        ParameterMap instArgs = new ParameterMap();
        ArrayList<String> instanceArgs = new ArrayList<String>();
        instArgs.add("node", node);
        if (lbEnabled != null){
            instArgs.add("lbenabled", lbEnabled.toString());
        }
        if (checkPorts != null){
            instArgs.add("checkPorts", checkPorts.toString());
        }
        if (terse != null){
            instArgs.add("terse", terse.toString());
        }
        if (portBase != null){
            instArgs.add("portbase", portBase);;
        }
        if (systemProperties != null){
            instArgs.add("systemProperties", systemProperties);
        }
        if (instance != null){
             instArgs.add("instance_name", instance);;
        }
        return instArgs;
    }
    
    
    private ParameterMap prepareHazelcastArgs() {
        ParameterMap hazelargs = new ParameterMap();
        ArrayList<String> hazelcastArgs = new ArrayList<String>();
        hazelcastArgs.add("set-hazelcast-configuration");
        hazelargs.add("target", instance);
        if (enabled != null) {
            hazelargs.add("enabled", enabled.toString());
        }
        if (dynamic != null) {
            hazelargs.add("dynamic", dynamic.toString());
        }
        if (configFile != null) {
            hazelargs.add("hazelcastconfigurationfile", configFile);
        }
        if (startPort != null) {
            hazelargs.add("startPort", startPort);
        }
        if (multiCastGroup != null) {
            hazelargs.add("multicastGroup", multiCastGroup);
        }
        if (multicastPort != null) {
            hazelargs.add("multicastPort", multicastPort);
        }
        if (hzClusterName != null) {
            hazelargs.add("clusterName", hzClusterName);
        }
        if (hzClusterPassword != null) {
            hazelargs.add("clusterPassword", hzClusterPassword);
        }
        if (jndiName != null) {
            hazelargs.add("jndiName", jndiName);
        }
        if (licenseKey != null) {
            hazelargs.add("licenseKey", licenseKey);;
        }
        if (lite != null) {
            hazelargs.add("lite", lite.toString());
        }
        if (hostawarePartitioning != null) {
            hazelargs.add("hostawareParitioning", hostawarePartitioning.toString());;
        }
        
        return hazelargs;
    }


    
}
