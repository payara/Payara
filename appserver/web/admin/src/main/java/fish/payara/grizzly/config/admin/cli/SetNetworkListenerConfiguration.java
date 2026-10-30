/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.grizzly.config.admin.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.glassfish.web.admin.LogFacade;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 *
 * @author jonathan coustick
 * @since 4.1.2.182
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@Service(name = "set-network-listener-configuration")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set-network-listener-configuration")
@RestEndpoints({
    @RestEndpoint(configBean = NetworkListener.class,
            opType = RestEndpoint.OpType.POST,
            description = "Configures a network listener")
})
public class SetNetworkListenerConfiguration implements AdminCommand, EventListener {

    private static final Logger LOGGER = LogFacade.getLogger();
    private static final String ADMIN_LISTENER = "admin-listener";
    private static final ResourceBundle rb = LogFacade.getLogger().getResourceBundle();

    //Parameters
    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true)
    private Boolean dynamic;

    @Param(name = "address", optional = true)
    private String address;

    @Param(name = "listenerport", optional = false, alias = "Port")
    private Integer port;

    @Param(name = "listenerPortRange", alias = "listenerportrange", optional = true)
    private String portRange;
    @Param(name = "threadpool", optional = true, alias = "threadPool")
    private String threadPool;
    @Param(name = "protocol", optional = true)
    private String protocol;
    @Param(name = "name", primary = true)
    private String listenerName;
    @Param(name = "transport", optional = true)
    private String transport;
    @Param(name = "jkenabled", optional = true, alias = "jkEnabled")
    private Boolean jkEnabled;
    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    Target targetUtil;

    @Inject
    NetworkConfig confign;
    
    @Inject
    UnprocessedConfigListener ucl;
    
    @Inject
    Events events;
    
    @PostConstruct
    public void postConstuct(){
        events.register(this);
    }
    @Override
    public void event(EventListener.Event event) {
            if (event.is(EventTypes.SERVER_SHUTDOWN)) {
                shutdownChange();
            }
    }

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();

        Config newConfig = targetUtil.getConfig(target);
        if (newConfig != null) {
            config = newConfig;
        }

        NetworkListener listener = config.getNetworkConfig().getNetworkListener(listenerName);

        if (!validate(actionReport)) {
            return;
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {
                    @Override
                    public Object run(final NetworkListener listenerProxy) throws PropertyVetoException, TransactionFailure {
                        if (enabled != null){
                            listenerProxy.setEnabled(enabled.toString());
                        }
                        if (address != null){
                            listenerProxy.setAddress(address);
                        }
                        if (port != null && !ADMIN_LISTENER.equals(listenerName)){
                            listenerProxy.setPort(port.toString());
                        }
                        if (portRange != null){
                            listenerProxy.setPortRange(portRange);
                        }
                        if (protocol != null){
                            listenerProxy.setProtocol(protocol);
                        }
                        if (threadPool != null){
                            listenerProxy.setThreadPool(threadPool);
                        }
                        if (transport != null){
                            listenerProxy.setTransport(transport);
                        }
                        if (jkEnabled != null){
                            listenerProxy.setJkEnabled(jkEnabled.toString());
                        }
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return null;
                    }

                }, listener);
            
            String oldPort = listener.getPort();
            if (port != null && ADMIN_LISTENER.equals(listenerName)){
                
                UnprocessedChangeEvent unprocessed = new UnprocessedChangeEvent(
                        new PropertyChangeEvent(this, "port", oldPort, port), listener.getName() + " port changed from " + oldPort + " to " + port);
                LOGGER.log(Level.INFO, MessageFormat.format(rb.getString(LogFacade.ADMIN_PORT_CHANGED), listenerName, oldPort, port));
                actionReport.setMessage(MessageFormat.format(rb.getString(LogFacade.ADMIN_PORT_CHANGED), listenerName, oldPort, port.toString()));
                List<UnprocessedChangeEvents> unprocessedList = new ArrayList<>();
                unprocessedList.add(new UnprocessedChangeEvents(unprocessed));
                ucl.unprocessedTransactedEvents(unprocessedList);
            }
            
        } catch (TransactionFailure e) {
            LOGGER.log(Level.SEVERE, null, e);
            actionReport.setMessage(MessageFormat.format(rb.getString(LogFacade.CREATE_NETWORK_LISTENER_FAIL), listenerName)
                    + (e.getMessage() == null ? "No reason given" : e.getMessage()));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            actionReport.setFailureCause(e);
            return;
        }

        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

    private boolean validate(ActionReport report) {
        NetworkListener listener = config.getNetworkConfig().getNetworkListener(listenerName);
        if (listener == null) {
            //Re-use message which says that listener does not exists
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.DELETE_NETWORK_LISTENER_NOT_EXISTS), listenerName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }

        if (port < 1 || port > 65535) {
            report.setMessage(MessageFormat.format(rb.getString(LogFacade.PORT_OUT_OF_RANGE), port));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }

        if (false) {

        }
        return true;
    }

    private void shutdownChange() { 
        List<UnprocessedChangeEvent> processed = new ArrayList<>();
        for (UnprocessedChangeEvents unchangedEvents: ucl.getUnprocessedChangeEvents()){
            for (UnprocessedChangeEvent unprocessedEvent: unchangedEvents.getUnprocessed()){
                PropertyChangeEvent event = unprocessedEvent.getEvent();
                if (event.getSource().getClass().equals(this.getClass()) && event.getPropertyName().equals("port")){
                    SetNetworkListenerConfiguration oldConfig =  (SetNetworkListenerConfiguration) event.getSource();
                     NetworkListeners listeners = oldConfig.confign.getNetworkListeners();
                     for (NetworkListener listener: listeners.getNetworkListener()){
                         if (listener.getName().equals(oldConfig.listenerName)){
                             try {
                                 ConfigSupport.apply(new SingleConfigCode<NetworkListener>() {
                                     @Override
                                     public Object run(final NetworkListener listenerProxy) throws PropertyVetoException, TransactionFailure {
                                         listenerProxy.setPort((event.getNewValue().toString()));
                                         return null;
                                     }}, listener);
                             } catch (TransactionFailure ex) {
                                 Logger.getLogger(SetNetworkListenerConfiguration.class.getName()).log(Level.SEVERE, null, ex);
                             } finally {
                                 processed.add(unprocessedEvent);
                             }
                         }
                     }
                     
                }
            }
        }
    }

}
