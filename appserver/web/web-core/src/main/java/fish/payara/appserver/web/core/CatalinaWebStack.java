/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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
 *
 */

package fish.payara.appserver.web.core;

import java.io.File;
import java.util.OptionalInt;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.JmxEnabled;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.grizzly.http.server.HttpHandler;

/**
 * Entry point to Catalina.
 *
 * Sets up top level services of Catalina stack, leaves managing Host and lower objects to client.
 */
public class CatalinaWebStack {

    private final Tomcat tomcat=new Tomcat();
    private Server server;
    private Configuration configuration;

    private Service service;

    private Engine engine;
    private boolean hasConnectors;


    private CatalinaWebStack(Configuration configuration) {
        this.configuration = configuration;
    }

    void init() {
        // e. g. relative redirects are not expected by servlet tck
        System.setProperty("org.apache.catalina.STRICT_SERVLET_COMPLIANCE", "true");
        tomcat.setBaseDir(configuration.getCatalinaHome().getAbsolutePath());

        server = tomcat.getServer();
        service = tomcat.getService();
        service.setName("Payara");
        engine = tomcat.getEngine();
        engine.setRealm(null);
        engine.setParentClassLoader(configuration.parentClassLoader());
        engine.setDefaultHost(configuration.defaultDomainName());
        engine.setName(configuration.defaultDomainName());
        if (service instanceof JmxEnabled) {
            ((JmxEnabled) service).setDomain(configuration.defaultDomainName());
        }
    }

    public void addDefaultHost(Host host) {
        engine.setDefaultHost(host.getName());
        engine.addChild(host);
    }

    public void addHost(Host host) {
        engine.addChild(host);
    }

    public Container[] getHosts() {
        return engine.findChildren();
    }

    public Host getHost(String name) {
        return (Host) engine.findChild(name);
    }

    public void start() throws LifecycleException {
        if (!hasConnectors) {
            service.addConnector(new GrizzlyConnector());
        }
        tomcat.init();
        tomcat.start();
    }

    public void stop() throws LifecycleException {
        server.stop();
    }


    public HttpHandler httpHandler() {
        if (service.getState() != LifecycleState.STARTED) {
            throw new IllegalStateException("Server is not yet started");
        }

        return ((GrizzlyConnector)service.findConnectors()[0]).asHttpHandler();
    }

    public static CatalinaWebStack create(Configuration conf) {
        var stack = new CatalinaWebStack(conf);
        stack.init();
        return stack;
    }

    /**
     * All data needed to configure Catalina's Server, Service and Engine
     */
    public interface Configuration {

        File getCatalinaHome();

        default ClassLoader parentClassLoader() {
            return Configuration.class.getClassLoader();
        }

        default String defaultDomainName() {
            return "localhost";
        }
    }

    public Engine getEngine() {
        return tomcat.getEngine();
    }

    public synchronized HttpHandler createConnector(String address, int port, String defaultHost, boolean isSecure, String name, String instanceName, OptionalInt redirectPort) {
        GrizzlyConnector connector = new GrizzlyConnector(name);
        connector.setSecure(isSecure);
        connector.setPort(port);
        redirectPort.ifPresent(connector::setRedirectPort);
        // there's nothing to do with the rest. They were used for MapperListener, which we don't have anymore.
        service.addConnector(connector);
        hasConnectors = true;
        return connector.asHttpHandler();
    }

    public void setDefaultRedirectPort(int defaultRedirectPort) {
        for (Connector connector : service.findConnectors()) {
            if (connector.getRedirectPort() == -1) {
                connector.setRedirectPort(defaultRedirectPort);
            }
        }
    }

    public synchronized void removeConnector(String name) {
        for (Connector connector : service.findConnectors()) {
            if (connector instanceof GrizzlyConnector &&  ((GrizzlyConnector) connector).getName().equals(name)) {
                service.removeConnector(connector);
                hasConnectors = service.findConnectors().length > 0;
                return;
            }
        }
    }

    public void setJvmRoute(String jvmRoute) {
        engine.setJvmRoute(jvmRoute);
    }
}
