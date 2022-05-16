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
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.Servlet;
import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.junit.rules.ExternalResource;


public class GrizzlyTestHarness extends ExternalResource {

    protected Grizzly grizzly;

    protected Catalina catalina;

    private GrizzlyConnector connector;

    public void start() {
        setLoggerLevels();
        grizzly = new Grizzly();
        catalina = new GrizzlyTestHarness.Catalina();
        connector = new GrizzlyConnector();
        connector.setXpoweredBy(true);
        try {
            catalina.start(connector);
        } catch (LifecycleException e) {
            throw new IllegalStateException(e);
        }
        grizzly.config.addHttpHandler(connector.asHttpHandler());
    }

    static void setLoggerLevels() {
        Logger.getLogger("org.glassfish.grizzly").setLevel(Level.INFO);
        Logger.getLogger("org.apache.catalina").setLevel(Level.ALL);
        Logger.getLogger("org.apache.catalina.util.LifecycleBase").setLevel(Level.INFO);
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            handler.setLevel(Level.ALL);
        }
        ;
    }

    public void stop() {
        try {
            if (catalina != null) {
                catalina.stop();
            }
            if (grizzly != null) {
                grizzly.server.shutdown().get();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        stop();
    }

    public interface ContextBuilder {
        StandardContext getContext();
        default ContextBuilder addServlet(Servlet instance, String mapping) {
            Catalina.addServlet(getContext(), instance, mapping);
            return this;
        }

        default ContextBuilder addServlet(String servletName, Class<? extends Servlet> servletClass, String mapping) {
            Catalina.addServlet(getContext(), servletName, servletClass, mapping);
            return this;
        }
    }

    public StandardContext addContext(String contextName, Consumer<ContextBuilder> builder) {
        var ctx = catalina.addContext(contextName);
        builder.accept(() -> ctx);
        return ctx;
    }

    protected static class Grizzly {
        HttpServer server;

        public int port;

        public ServerConfiguration config;

        Grizzly() {
            NetworkListener listener = new NetworkListener("grizzly", "localhost", new PortRange(8080, 9080), true);
            listener.getTransport().getWorkerThreadPoolConfig()
                    .setThreadFactory(
                            (new ThreadFactoryBuilder())
                                    .setNameFormat("grizzly-http-server-%d")
                                    .setUncaughtExceptionHandler((t, e) -> e.printStackTrace())
                                    .build());
            server = new HttpServer();
            server.addListener(listener);
            config = server.getServerConfiguration();
            config.setPassTraceRequest(true);
            config.setDefaultQueryEncoding(Charsets.UTF8_CHARSET);
            try {
                server.start();
                port = listener.getPort();
            } catch (IOException e) {
                server.shutdownNow();
            }
        }
    }

    protected static class Catalina {

        private final StandardService service;

        private final StandardEngine engine;

        private final StandardHost host;

        private final StandardServer server;

        Catalina() {
            server = new StandardServer();
            server.setCatalinaHome(new File("."));

            service = new StandardService();
            server.addService(service);
            engine = new StandardEngine();
            service.setContainer(engine);
            host = new StandardHost();
            host.setName("localhost");
            engine.setDefaultHost("localhost");
            engine.addChild(host);
        }

        void start(Connector c) throws LifecycleException {
            service.addConnector(c);
            server.start();
        }

        void stop() throws LifecycleException {
            server.stop();
        }

        protected StandardContext addContext(String name) {
            var ctx = new StandardContext();
            ctx.setName(name);
            ctx.setPath("ROOT".equals(name) ? "" : ("/" + name));
            // Needed for embedded usecase without tomcat deployer
            ctx.addLifecycleListener(new Tomcat.FixContextListener());
            host.addChild(ctx);
            return ctx;
        }

        protected static StandardWrapper addServlet(StandardContext ctx, Servlet instance, String mapping) {
            var wrapper = new StandardWrapper();
            wrapper.setServlet(instance);
            wrapper.setName(instance.getClass().getName());
            ctx.addChild(wrapper);
            wrapper.addMapping(mapping);
            return wrapper;
        }

        protected static StandardWrapper addServlet(StandardContext ctx, String servletName, Class<? extends Servlet> servletClass, String mapping) {
            var wrapper = new StandardWrapper();
            wrapper.setServletClass(servletClass.getName());
            wrapper.setName(servletName);
            ctx.addChild(wrapper);
            wrapper.addMapping(mapping);
            return wrapper;
        }

        public void removeServletByMapping(StandardContext ctx, String path) {
            for (Container child : ctx.findChildren()) {
                var wrapper = (StandardWrapper) child;
                for (String mapping : wrapper.findMappings()) {
                    if (mapping.equals(path)) {
                        ctx.removeChild(child);
                        break;
                    }
                }
            }
        }
    }
}
