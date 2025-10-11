/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.microprofile.metrics.jmx;

import fish.payara.microprofile.metrics.healthcheck.HealthCheckCounter;
import fish.payara.microprofile.metrics.healthcheck.HealthCheckGauge;
import fish.payara.microprofile.metrics.healthcheck.ServiceExpression;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.nucleus.healthcheck.HealthCheckStatsProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

@Service
public class MetricsMetadataHelper {

    public static final String SPECIFIER = "%s"; // microprofile-metrics specification defined specifier
    public static final String KEY = "${key}";
    public static final String ATTRIBUTE = "${attribute}";
    public static final String SUB_ATTRIBUTE = "${subattribute}";
    public static final String ATTRIBUTE_SEPARATOR = "/";
    public static final String SUB_ATTRIBUTE_SEPARATOR = "#";
    public static final String INSTANCE = "${instance}";

    public static final String GAUGE_METRIC_MBEAN_NAME = "gauge";

    public static final String COUNTER_METRIC_MBEAN_NAME = "counter";

    private static final Logger LOGGER = Logger.getLogger(MetricsMetadataHelper.class.getName());

    @Inject
    private ServerEnvironment serverEnv;

    @Inject
    private ServiceLocator habitat;

    /**
     * Registers metrics as MBeans
     *
     * @param metricRegistry Registry to add metrics to
     * @param metadataList List of all {@link MetricsMetadata} representing a
     * {@link Metric}
     * @param globalTags
     * @param isRetry true if this is not initial registration, this is used to
     * register lazy-loaded MBeans
     * @return the list of unresolved MBean Metadata
     */
    public List<MetricsMetadata> registerMetadata(MetricRegistry metricRegistry,
            List<MetricsMetadata> metadataList, boolean isRetry) {

        if (!metricRegistry.getNames().isEmpty() && !isRetry) {
            metricRegistry.removeMatching(MetricFilter.ALL);
        }

        List<MetricsMetadata> unresolvedMetadataList = resolveDynamicMetadata(metadataList);
        for (MetricsMetadata beanMetadata : metadataList) {
            List<Tag> tags = new ArrayList<>();
            for (XmlTag tag : beanMetadata.getTags()) {
                tags.add(new Tag(tag.getName(), tag.getValue()));
            }
            try {
                if (metricRegistry.getNames().contains(beanMetadata.getName()) &&
                        metricRegistry.getMetricIDs().contains(new MetricID(beanMetadata.getName(), tags.toArray(new Tag[tags.size()])))) {
                    continue;
                }
                Metric type;
                if (beanMetadata.getMBean() != null) {
                    MBeanExpression mBeanExpression = new MBeanExpression(beanMetadata.getMBean());

                    switch (beanMetadata.getType()) {
                        case COUNTER_METRIC_MBEAN_NAME:
                            type = new MBeanCounterImpl(mBeanExpression);
                            break;
                        case GAUGE_METRIC_MBEAN_NAME:
                            type = new MBeanGuageImpl(mBeanExpression);
                            break;
                        default:
                            throw new IllegalStateException("Unsupported type : " + beanMetadata);
                    }
                    if (metricRegistry instanceof MetricRegistryImpl) {
                        ((MetricRegistryImpl) metricRegistry).register(beanMetadata,
                                getInterfaceType(beanMetadata.getType()), type, tags.toArray(new Tag[tags.size()]));
                    }

                } else {
                    ServiceExpression expression = new ServiceExpression(beanMetadata.getService());
                    HealthCheckStatsProvider healthCheck = habitat.getService(HealthCheckStatsProvider.class, expression.getServiceId());
                    if (healthCheck != null) {
                        switch (beanMetadata.getType()) {
                            case COUNTER_METRIC_MBEAN_NAME:
                                type = new HealthCheckCounter(healthCheck, expression);
                                metricRegistry.counter(beanMetadata, tags.toArray(new Tag[tags.size()]));
                                break;
                            case GAUGE_METRIC_MBEAN_NAME:
                                type = new HealthCheckGauge(healthCheck, expression);
                                break;
                            default:
                                throw new IllegalStateException("Unsupported type : " + beanMetadata);
                        }
                        if (metricRegistry instanceof MetricRegistryImpl) {
                            ((MetricRegistryImpl) metricRegistry).register(beanMetadata,
                                    getInterfaceType(beanMetadata.getType()), type, tags.toArray(new Tag[tags.size()]));
                        }
                    } else {
                        throw new IllegalStateException("Health-Check service not found : " + beanMetadata.getService());
                    }
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.log(WARNING, ex.getMessage(), ex);
            }
        }
        return unresolvedMetadataList;
    }
    
    public String getInterfaceType(String type) {
        if(type.equals(COUNTER_METRIC_MBEAN_NAME)) {
            return Counter.class.getTypeName();
        }
        
        if(type.equals(GAUGE_METRIC_MBEAN_NAME)) {
            return Gauge.class.getTypeName();
        }
        return null;
    }

    /**
     * Resolve dynamic metadata by replacing specifier <b>%s</b> with the mbean value.
     *
     * @param metadataList list of MBean Metadata
     * @return the list of unresolved MBean Metadata
     */
    public List<MetricsMetadata> resolveDynamicMetadata(List<MetricsMetadata> metadataList) {
        List<MetricsMetadata> unresolvedMetadataList = new ArrayList<>();
        List<MetricsMetadata> resolvedMetadataList = new ArrayList<>();
        List<Metadata> removedMetadataList = new ArrayList<>(metadataList.size());
        for (MetricsMetadata metadata : metadataList) {
            if (!metadata.isValid()) {
                removedMetadataList.add(metadata);
                continue;
            }
            if (metadata.getMBean() != null
                    && (metadata.getMBean().contains(SPECIFIER)
                    || metadata.getMBean().contains(KEY)
                    || metadata.getMBean().contains(ATTRIBUTE)
                    || metadata.getMBean().contains(SUB_ATTRIBUTE)
                    || metadata.getMBean().contains(INSTANCE))) {
                try {
                    String instanceName = serverEnv.getInstanceName();
                    // set (optional) instance the query expression
                    String queryExpression = metadata.getMBean()
                            .replace(INSTANCE, instanceName);

                    if (metadata.getMBean().contains(SPECIFIER)
                            || metadata.getMBean().contains(KEY)) {
                        MBeanExpression mBeanExpression = new MBeanExpression(
                                queryExpression
                                        .replace(SPECIFIER, "*")
                                        .replace(KEY, "*")
                        );
                        String dynamicKey = mBeanExpression.findDynamicKey();
                        Set<ObjectName> mBeanObjects = mBeanExpression.queryNames(null);
                        if (mBeanObjects.isEmpty()) {
                            unresolvedMetadataList.add(metadata);
                            LOGGER.log(INFO, "{0} does not correspond to any MBeans", metadata.getMBean());
                        } else if (metadata.isDynamic()) {
                            unresolvedMetadataList.add(metadata);
                        }
                        for (ObjectName objName : mBeanObjects) {
                            String dynamicValue = objName.getKeyPropertyList().get(dynamicKey);
                            resolvedMetadataList.addAll(
                                    loadAttribute(objName, mBeanExpression, metadata, dynamicValue, instanceName)
                            );
                        }
                    } else {
                        MBeanExpression mBeanExpression = new MBeanExpression(queryExpression);
                        ObjectName objName = mBeanExpression.getObjectName();
                        if (objName == null) {
                            unresolvedMetadataList.add(metadata);
                            LOGGER.log(INFO, "{0} does not correspond to any MBeans", metadata.getMBean());
                        } else if (metadata.isDynamic()) {
                            unresolvedMetadataList.add(metadata);
                        }
                        resolvedMetadataList.addAll(
                                loadAttribute(objName, mBeanExpression, metadata, null, instanceName)
                        );
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(SEVERE, ex, () -> metadata.getMBean() + " is invalid");
                } finally {
                    removedMetadataList.add(metadata);
                }
            } else if (metadata.getService() != null
                    && (metadata.getService().contains(SPECIFIER)
                    || metadata.getService().contains(ATTRIBUTE)
                    || metadata.getService().contains(SUB_ATTRIBUTE)
                    || metadata.getService().contains(INSTANCE))) {
                ServiceExpression expression = new ServiceExpression(
                        metadata.getService().replace(SPECIFIER, "*")
                );
                if (expression.getAttributeName() != null
                        && (expression.getAttributeName().equals("*") || expression.getAttributeName().equals(ATTRIBUTE))) {
                    removedMetadataList.add(metadata);
                    resolvedMetadataList.addAll(
                            loadAttribute(expression, metadata)
                    );
                } else if (expression.getSubAttributeName() != null
                        && (expression.getSubAttributeName().equals("*") || expression.getSubAttributeName().equals(SUB_ATTRIBUTE))) {
                    removedMetadataList.add(metadata);
                    resolvedMetadataList.addAll(
                            loadSubAttribute(expression, metadata, expression.getAttributeName())
                    );
                }
            }
        }

        metadataList.removeAll(removedMetadataList);
        metadataList.addAll(resolvedMetadataList);
        return unresolvedMetadataList;
    }
    
    private List<MetricsMetadata> loadAttribute(
            ServiceExpression expression,
            MetricsMetadata metadata) {

        List<MetricsMetadata> metadataList = new ArrayList<>();
        String instanceName = serverEnv.getInstanceName();
        HealthCheckStatsProvider healthCheck = habitat.getService(HealthCheckStatsProvider.class, expression.getServiceId());
        if (healthCheck != null) {
            for (String attribute : healthCheck.getAttributes()) {
                if (expression.getSubAttributeName() != null
                        && (expression.getSubAttributeName().equals("*") || expression.getSubAttributeName().equals(SUB_ATTRIBUTE))) {
                    metadataList.addAll(
                            loadSubAttribute(expression, metadata, attribute)
                    );
                } else {
                    metadataList.add(createMetadata(metadata, expression.getServiceId(), null, attribute, expression.getSubAttributeName(), instanceName));
                }
            }
        } else {
            LOGGER.log(WARNING, "Health-Check service not found : {0}", expression.getServiceId());
        }
        return metadataList;
    }

    private List<MetricsMetadata> loadSubAttribute(
            ServiceExpression expression,
            MetricsMetadata metadata,
            String attribute) {

        List<MetricsMetadata> metadataList = new ArrayList<>();
        String instanceName = serverEnv.getInstanceName();
        HealthCheckStatsProvider healthCheck = habitat.getService(HealthCheckStatsProvider.class, expression.getServiceId());
        if (healthCheck != null) {
            for (String subAttribute : healthCheck.getSubAttributes()) {
                metadataList.add(createMetadata(metadata, expression.getServiceId(), null, attribute, subAttribute, instanceName));
            }
        } else {
            LOGGER.log(WARNING, "Health-Check service not found : {0}", expression.getServiceId());
        }
        return metadataList;
    }

    private static List<MetricsMetadata> loadAttribute(
            ObjectName objName,
            MBeanExpression mBeanExpression,
            MetricsMetadata metadata,
            String key,
            String instanceName) {

        List<MetricsMetadata> metadataList = new ArrayList<>();
        String attributeName;

        if (ATTRIBUTE.equals(mBeanExpression.getAttributeName())) {
            List<MBeanAttributeInfo> attributes = mBeanExpression.queryAttributes(objName);
            for (MBeanAttributeInfo attribute : attributes) {
                attributeName = attribute.getName();
                metadataList.addAll(
                        loadSubAttribute(
                                objName,
                                mBeanExpression,
                                metadata,
                                key,
                                attributeName,
                                instanceName,
                                true
                        )
                );
            }
        } else {
            attributeName = mBeanExpression.getAttributeName();
            metadataList.addAll(
                    loadSubAttribute(
                            objName,
                            mBeanExpression,
                            metadata,
                            key,
                            attributeName,
                            instanceName,
                            false
                    )
            );
        }
        return metadataList;
    }

    private static List<MetricsMetadata> loadSubAttribute(
            ObjectName objName,
            MBeanExpression mBeanExpression,
            MetricsMetadata metadata,
            String key,
            String attribute,
            String instanceName,
            boolean isDynamicAttribute) {
        List<MetricsMetadata> metadataList = new ArrayList<>();
        String exp = objName.getCanonicalName();
        String subAttribute = mBeanExpression.getSubAttributeName();
        if (subAttribute != null) {
            if (SUB_ATTRIBUTE.equals(subAttribute)) {
                Object obj = mBeanExpression.querySubAttributes(objName, attribute);
                if (obj instanceof CompositeDataSupport) {
                    CompositeDataSupport compositeData = (CompositeDataSupport) obj;
                    MetadataBuilder newMetadataBuilder = Metadata.builder(metadata);
                    for (String subAttrResolvedName : compositeData.getCompositeType().keySet()) {
                        subAttribute = subAttrResolvedName;
                        if ("description".equals(subAttribute)
                                && compositeData.get(subAttribute) instanceof String
                                && metadata.description().isPresent()) {
                            newMetadataBuilder = newMetadataBuilder.withDescription((String) compositeData.get(subAttribute));
                        } else if ("name".equals(subAttribute)
                                && compositeData.get(subAttribute) instanceof String) {
                            newMetadataBuilder = newMetadataBuilder.withName((String) compositeData.get(subAttribute));
                        } else if ("unit".equals(subAttribute)
                                && compositeData.get(subAttribute) instanceof String
                                && MetricUnits.NONE.equals(metadata.unit().orElse("none"))) {
                            newMetadataBuilder = newMetadataBuilder.withUnit((String) compositeData.get(subAttribute));
                        }
                    }
                    MetricsMetadata newMbeanMetadata = new MetricsMetadata(newMetadataBuilder.build());
                    newMbeanMetadata.addTags(metadata.getTags());
                    metadataList.add(createMetadata(newMbeanMetadata, exp, key, attribute, subAttribute, instanceName));
                }
            } else if (isDynamicAttribute) {
                Object obj = mBeanExpression.querySubAttributes(objName, attribute);
                if (obj instanceof CompositeDataSupport) {
                    CompositeDataSupport compositeData = (CompositeDataSupport) obj;
                    if (compositeData.containsKey(subAttribute) && compositeData.get(subAttribute) instanceof Number) {
                        metadataList.add(createMetadata(metadata, exp, key, attribute, subAttribute, instanceName));
                    }
                }
            } else {
                metadataList.add(createMetadata(metadata, exp, key, attribute, subAttribute, instanceName));
            }
        } else {
            metadataList.add(createMetadata(metadata, exp, key, attribute, subAttribute, instanceName));
        }
        return metadataList;
    }

    private static MetricsMetadata createMetadata(
            MetricsMetadata metadata,
            String exp,
            String key,
            String attribute,
            String subAttribute,
            String instanceName) {
        StringBuilder builder = new StringBuilder();
        builder.append(exp);
        builder.append(ATTRIBUTE_SEPARATOR);
        builder.append(attribute);
        if (subAttribute != null) {
            builder.append(SUB_ATTRIBUTE_SEPARATOR);
            builder.append(subAttribute);
        }
        MetricsMetadata newMetaData =  new MetricsMetadata(
                formatMetadata(metadata.getName(), key, attribute, subAttribute, instanceName),
                formatMetadata(metadata.getName(), key, attribute, subAttribute, instanceName),
                formatMetadata(metadata.description().isPresent() ? metadata.getDescription() : metadata.getName(),
                        key, attribute, subAttribute, instanceName), metadata.getType(),
                metadata.unit().orElse(null));
        if(metadata.getMBean() != null) {
            newMetaData.setMBean(builder.toString());
        } else {
            newMetaData.setMBean(null);
            newMetaData.setService(builder.toString());
        }
        for (XmlTag oldTag: metadata.getTags()) {
            XmlTag newTag = new XmlTag();
            newTag.setName(formatMetadata(oldTag.getName(), key, attribute, subAttribute, instanceName));
            newTag.setValue(formatMetadata(oldTag.getValue(), key, attribute, subAttribute, instanceName));
            newMetaData.getTags().add(newTag);
        }
        return newMetaData;
    }

    private static String formatMetadata(
            String metadata,
            String dynamicValue,
            String attributeName,
            String subAttributeName,
            String instanceName) {
        if (dynamicValue != null && metadata.contains(SPECIFIER)) {
            metadata = metadata.replace(SPECIFIER, dynamicValue);
        }
        if (dynamicValue != null && metadata.contains(KEY)) {
            metadata = metadata.replace(KEY, dynamicValue);
        }
        if (attributeName != null && metadata.contains(ATTRIBUTE)) {
            metadata = metadata.replace(ATTRIBUTE, attributeName);
        }
        if (subAttributeName != null && metadata.contains(SUB_ATTRIBUTE)) {
            metadata = metadata.replace(SUB_ATTRIBUTE, subAttributeName);
        }
        if (metadata.contains(INSTANCE)) {
            metadata = metadata.replace(INSTANCE, instanceName);
        }
        return metadata;
    }
    




}
