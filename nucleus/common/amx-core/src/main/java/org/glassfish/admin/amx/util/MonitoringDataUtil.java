package org.glassfish.admin.amx.util;

import java.util.Map.Entry;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import org.glassfish.admin.amx.core.AMXProxy;

import fish.payara.monitoring.collect.MonitoringDataCollector;

/**
 * Utility to collect monitoring data from monitoring beans.
 *
 * @author Jan Bernitt
 */
public class MonitoringDataUtil {

    public static void collectBean(MonitoringDataCollector collector, AMXProxy bean) {
        for (Entry<String, Object> attrEntry : bean.attributesMap().entrySet()) {
            String metric = attrEntry.getKey();
            Object value = attrEntry.getValue();
            if (value instanceof CompositeData) {
                CompositeData data = (CompositeData) value;
                CompositeType type = data.getCompositeType();
                if ("CountStatistic".equals(type.getTypeName())) {
                    Long count = (Long) data.get("count");
                    String unit = (String) data.get("unit");
                    String name = (String) data.get("name");
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    metric = name.endsWith("Count") ? name
                            : name + Character.toUpperCase(unit.charAt(0)) + unit.substring(1);
                    collector.collect(metric, count);
                }
            } else if (value instanceof Number) {
                collector.collect(metric, (Number) value);
            } else if (value instanceof Boolean) {
                collector.collect(metric, (Boolean) value);
            }
        }
    }
}
