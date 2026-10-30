/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 */

package fish.payara.samples.otel.spanname;

import fish.payara.samples.otel.JaxrsApp;
import fish.payara.samples.otel.async.AsyncResource;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AbstractSpanNameTest {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected static WebArchive base() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(JaxrsApp.class)
                .addClass(AsyncResource.class)
                .addClass(Conf.class)
                .addClass(InMemoryExporter.class)
                .addClass(InMemoryExporter.Provider.class)
                .addClass(AbstractSpanNameTest.class)
                .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemoryExporter.Provider.class)
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml").resolve("org.assertj:assertj-core").withTransitivity().asFile());
    }

    protected static WebArchive configSource(WebArchive base, Class<? extends ConfigSource> csclass) {
        return base.addClass(csclass).addAsServiceProvider(ConfigSource.class, csclass);
    }

    @ArquillianResource
    protected URI baseUri;

    @Inject
    protected InMemoryExporter exporter;

    protected WebTarget target(String wrapType, String basePath, String path) {
        var target = ClientBuilder.newClient().target(baseUri).path("jaxrs").path(basePath).path(path);

        if (wrapType != null) {
            target = target.queryParam("propagation", wrapType);
        }
        return target;
    }

    protected abstract static class Conf implements ConfigSource {
        protected static final String SPAN_NAMING_KEY = "payara.telemetry.span-convention";
        private Map<String,String> configProps = Collections.synchronizedMap(new HashMap<>());
        protected Conf(Map<String, String> additionalProps) {
            // enable OTEL
            configProps.put("otel.sdk.disabled", "false");
            // enable our test exporter
            configProps.put("otel.traces.exporter", "in-memory");
            // schedule export every 10 milliseconds
            configProps.put("otel.bsp.schedule.delay", "10");
            // anything else (possible overrides)
            configProps.putAll(additionalProps);
        }

        @Override
        public Map<String, String> getProperties() {
            return configProps;
        }

        @Override
        public Set<String> getPropertyNames() {
            return configProps.keySet();
        }

        @Override
        public String getValue(String s) {
            return configProps.get(s);
        }

        @Override
        public String getName() {
            return "test-props";
        }
    }
}
