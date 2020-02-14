/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.monitoring.runtime;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.jvnet.hk2.config.Dom.unwrap;

import java.beans.PropertyChangeEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.InternalSystemAdministrator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.MonitoringService;

import fish.payara.monitoring.adapt.GroupData;
import fish.payara.monitoring.adapt.GroupDataRepository;
import fish.payara.monitoring.adapt.MonitoringConsole;
import fish.payara.monitoring.adapt.MonitoringConsoleFactory;
import fish.payara.monitoring.adapt.MonitoringConsoleRuntime;
import fish.payara.monitoring.adapt.MonitoringConsoleWatchConfig;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.monitoring.collect.MonitoringWatchSource;
import fish.payara.monitoring.configuration.MonitoringConsoleConfiguration;
import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.requesttracing.RequestTracingService;

/**
 * This implementation of the {@link MonitoringConsoleRuntime} connects the Payara independent parts of the monitoring
 * console with the Payara server.
 * 
 * @author Jan Bernitt
 * @since 5.201
 */
@Service
public class MonitoringConsoleRuntimeImpl
        implements ConfigListener, MonitoringConsoleRuntime, MonitoringConsoleWatchConfig, GroupDataRepository {

    private static final Logger LOGGER = Logger.getLogger("monitoring-console-core");

    private static final String SET_MONITORING_CONSOLE_CONFIGURATION_COMMAND = "set-monitoring-console-configuration";

    /**
     * The topic name used to share data of instances with the DAS.
     */
    private static final String MONITORING_DATA_TOPIC_NAME = "payara-monitoring-data";

    @Inject
    private PayaraExecutorService executor;
    @Inject
    private ServerEnvironment serverEnv;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;
    @Inject
    private Domain domain;
    @Inject
    private CommandRunner commandRunner;
    @Inject
    private InternalSystemAdministrator kernelIdentity;
    @Inject
    private HazelcastCore hazelcastCore;
    @Inject
    private RequestTracingService requestTracingService;
    @Inject
    private ServiceLocator serviceLocator;

    private ITopic<?> exchange;
    private MonitoringConsoleConfiguration config;
    private MonitoringConsole console;

    @PostConstruct
    public void init() {
        try {
            boolean isDas = serverEnv.isDas();
            config = domain.getExtensionByType(MonitoringConsoleConfiguration.class);
            String instanceName = "server"; // default
            if (hazelcastCore.isEnabled()) {
                HazelcastInstance hz = hazelcastCore.getInstance();
                instanceName = hz.getCluster().getLocalMember().getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
                exchange = hz.getTopic(MONITORING_DATA_TOPIC_NAME);
            }
            Supplier<List<MonitoringDataSource>> dataSources = () -> serviceLocator.getAllServices(MonitoringDataSource.class);
            Supplier<List<MonitoringWatchSource>> watchSources = () -> serviceLocator.getAllServices(MonitoringWatchSource.class);
            console = MonitoringConsoleFactory.getInstance().create(instanceName, isDas, this, dataSources, watchSources);
            setEnabled(parseBoolean(serverConfig.getMonitoringService().getMonitoringEnabled()));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to init monitoring console runtime", ex);
        }
    }

    @Override
    public final UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        for (PropertyChangeEvent e : events) {
            if (e.getSource() instanceof ConfigBeanProxy) {
                Class<?> source = unwrap((ConfigBeanProxy)e.getSource()).getImplementationClass();
                if (source == MonitoringService.class) {
                    String property = e.getPropertyName();
                    if ("monitoring-enabled".equals(property)) {
                        setEnabled(parseBoolean(e.getNewValue().toString()));
                    }
                }
            }
        }
        return null;
    }

    private void setEnabled(boolean enabled) {
        if (console != null) {
            console.setEnabled(enabled);
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> boolean send(Class<T> type, T snapshot) {
        if (exchange == null) {
            return false;
        }
        ((ITopic<T>) exchange).publish(snapshot);
        return true;
    }

    @Override
    public <T extends Serializable> boolean receive(Class<T> type, Consumer<T> receiver) {
        if (exchange == null) {
            return false;
        }
        exchange.addMessageListener(msg -> receiver.accept(type.cast(msg.getMessageObject())));
        return true;
    }

    @Override
    public Iterable<ClassLoader> contexts() {
        return asList(this.getClass().getClassLoader());
    }

    @Override
    public MonitoringConsoleWatchConfig getWatchConfig() {
        return this;
    }

    @Override
    public boolean isDisabled(String name) {
        return config.getDisabledWatchNames().contains(name);
    }

    @Override
    public void disable(String name) {
        runCommand(SET_MONITORING_CONSOLE_CONFIGURATION_COMMAND, "disable-watch", name);
    }

    @Override
    public void enable(String name) {
        runCommand(SET_MONITORING_CONSOLE_CONFIGURATION_COMMAND, "enable-watch", name);
    }

    @Override
    public void add(String name, String watchJson) {
        runCommand(SET_MONITORING_CONSOLE_CONFIGURATION_COMMAND, 
                "add-watch-name", name, 
                "add-watch-json", watchJson);
    }

    @Override
    public void remove(String name) {
        runCommand(SET_MONITORING_CONSOLE_CONFIGURATION_COMMAND, "remove-watch", name);
    }

    @Override
    public Iterable<String> list() {
        return config.getCustomWatchValues();
    }

    private void runCommand(String name, String... params) {
        try {
            ActionReport report = commandRunner.getActionReport("plain");
            CommandInvocation cmd = commandRunner.getCommandInvocation(name, report, kernelIdentity.getSubject());
            ParameterMap paramsMap = new ParameterMap();
            for (int i = 0; i < params.length; i += 2) {
                paramsMap.add(params[i], params[i + 1]);
            }
            cmd.parameters(paramsMap).execute();
        } catch (Exception ex) {
            LOGGER.log(java.util.logging.Level.WARNING, "Failed to run command: " + name, ex);
        }
    }

    @Override
    public GroupDataRepository getGroupData() {
        return this;
    }

    @Override
    public Collection<GroupData> selectAll(String source, String group) {
        if (!"requesttracing".equals(source)) {
            return emptyList();
        }
        List<GroupData> matches = new ArrayList<>();
        for (RequestTrace trace : requestTracingService.getRequestTraceStore().getTraces()) {
            if (RequestTracingService.metricGroupName(trace).equals(group)) {
                GroupData data = new GroupData();
                data
                    .addField("id", trace.getTraceId())
                    .addField("startTime", trace.getStartTime().toEpochMilli())
                    .addField("endTime", trace.getEndTime().toEpochMilli())
                    .addField("elapsedTime", trace.getElapsedTime());
                for (RequestTraceSpan span : trace.getTraceSpans()) {
                    GroupData tags = data.addChild(span.getId().toString())
                        .addField("id", span.getId())
                        .addField("operation", RequestTracingService.stripPackageName(span.getEventName()))
                        .addField("startTime", span.getTimeOccured())
                        .addField("endTime", span.getTraceEndTime().toEpochMilli())
                        .addField("duration", span.getSpanDuration())
                        .addChild("tags");
                    for (Entry<String, String> tag : span.getSpanTags().entrySet()) {
                        tags.addField(tag.getKey(), tag.getValue());
                    }
                }
                matches.add(data);
            }
        }
        return matches;
    }
}
