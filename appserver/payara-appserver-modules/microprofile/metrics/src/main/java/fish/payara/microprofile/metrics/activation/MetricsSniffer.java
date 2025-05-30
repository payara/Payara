/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
package fish.payara.microprofile.metrics.activation;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.JandexIndexer.Index;
import org.jvnet.hk2.annotations.Service;

import fish.payara.microprofile.connector.MicroProfileSniffer;

@Service
@PerLookup
public class MetricsSniffer extends MicroProfileSniffer {

    private static final Logger LOGGER = Logger.getLogger(MetricsSniffer.class.getName());

    @Inject
    private ServerEnvironment serverEnv;
    @Inject
    JandexIndexer jandexIndexer;

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Annotation>[] getAnnotationTypes() {
        return new Class[]{
                // Search for Metrics annotations
                org.eclipse.microprofile.metrics.annotation.Counted.class,
                org.eclipse.microprofile.metrics.annotation.Gauge.class,
                org.eclipse.microprofile.metrics.annotation.Metric.class,
                org.eclipse.microprofile.metrics.annotation.Timed.class,
                org.eclipse.microprofile.metrics.annotation.RegistryType.class,
                org.eclipse.microprofile.metrics.annotation.RegistryScope.class,
                // All JAX-RS applications are valid applications for Metrics
                jakarta.ws.rs.Path.class
        };
    }

    @Override
    public boolean handles(DeploymentContext context) {
        Index index = jandexIndexer.getRootIndex(context);
        if (index != null) {
            if (!index.getIndex().getAnnotations(MetricRegistry.class).isEmpty()) {
                return true;
            }
            if (!index.getIndex().getAnnotations(Metric.class).isEmpty()) {
                return true;
            }
        }

        return super.handles(context);
    }

    @Override
    public boolean handles(ReadableArchive archive) {
        // Check for metrics.xml files
        try {
            if (archive.exists("metrics.xml")) {
                return true;
            }
            File metricsResource = new File(serverEnv.getConfigDirPath(), "metrics.xml");
            if (metricsResource.exists()) {
                return true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading archive", e);
            return false;
        }

        return super.handles(archive);
    }

    @Override
    protected Class<?> getContainersClass() {
        return MetricsContainer.class;
    }

    @Override
    public String getModuleType() {
        return "metrics";
    }
    
}
