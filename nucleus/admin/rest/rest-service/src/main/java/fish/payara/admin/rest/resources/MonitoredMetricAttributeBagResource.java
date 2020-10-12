/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.admin.rest.resources;

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.config.support.TranslatedConfigView;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.TransactionFailure;

public class MonitoredMetricAttributeBagResource extends AbstractAttributeBagResource {

    public static final LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(MonitoredMetricAttributeBagResource.class);

    @Override
    public String getDescriptionName() {
        return "monitored-metric";
    }

    @Override
    public String getPropertiesName() {
        return "monitoredMetrics";
    }

    @Override
    public String getnodeElementName() {
        return "monitored-metrics";
    }

    @Override
    public List<Map<String, String>> getAllAttributes() {
        List<Map<String, String>> attributes = new ArrayList<>();

        for (Dom child : entity) {
            Map<String, String> entry = new HashMap<>();

            entry.put("metricName", child.attribute("metric-name"));
            String description = child.attribute("description");
            if (description != null) {
                entry.put("description", description);
            }
            attributes.add(entry);
        }
        return attributes;
    }

    @Override
    public void excuteSetCommand(List<Map<String, String>> attributesToAdd, List<Map<String, String>> attributesToDelete) throws TransactionFailure {
        try {
            // Add all required metrics
            for (Map<String, String> attribute : attributesToAdd) {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("add-metric", String.format("metricName=%s description=%s", attribute.get("metricName"), attribute.get("description")));
                parameters.put("service", "mp-metrics");
                RestActionReporter reporter = ResourceUtil.runCommand("set-healthcheck-service-configuration", parameters, getSubject());
                if (reporter.isFailure()) {
                    throw new TransactionFailure(reporter.getMessage());
                }
            }
            // Delete all unrequired metrics
            for (Map<String, String> attribute : attributesToDelete) {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("delete-metric", String.format("metricName=%s", attribute.get("metricName")));
                parameters.put("service", "mp-metrics");

                RestActionReporter reporter = ResourceUtil.runCommand("set-healthcheck-service-configuration", parameters, getSubject());
                if (reporter.isFailure()) {
                    throw new TransactionFailure(reporter.getMessage());
                }
            }
        } finally {
            TranslatedConfigView.doSubstitution.set(true);
        }
    }

    @Override
    public boolean attributesAreEqual(Map<String, String> attribute1, Map<String, String> attribute2) {
        return attribute1.get("metricName").equals(attribute2.get("metricName"));
    }
}
