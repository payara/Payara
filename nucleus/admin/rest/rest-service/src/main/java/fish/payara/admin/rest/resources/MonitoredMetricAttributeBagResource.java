/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
