/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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

import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.nonNull;
import java.util.Set;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.jvnet.hk2.annotations.Service;

@Service
public class MBeanMetadataHelper {

    public static final String SPECIFIER = "%s"; // microprofile-metrics specification defined specifier
    public static final String KEY = "${key}";
    public static final String ATTRIBUTE = "${attribute}";
    public static final String SUB_ATTRIBUTE = "${subattribute}";
    public static final String ATTRIBUTE_SEPARATOR = "/";
    public static final String SUB_ATTRIBUTE_SEPARATOR = "#";

    private static final Logger LOGGER = Logger.getLogger(MBeanMetadataHelper.class.getName());

    /**
     * Registers metrics as MBeans
     *
     * @param metricRegistry Registry to add metrics to
     * @param metadataList List of all {@link MBeanMetadata} representing a
     * {@link Metric}
     * @param globalTags
     * @param isRetry true if this is not initial registration, this is used to
     * register lazy-loaded MBeans
     * @return the list of unresolved MBean Metadata
     */
    public List<MBeanMetadata> registerMetadata(MetricRegistry metricRegistry,
            List<MBeanMetadata> metadataList, boolean isRetry) {

        if (!metricRegistry.getMetadata().isEmpty() && !isRetry) {
            metricRegistry.removeMatching(MetricFilter.ALL);
        }

        List<MBeanMetadata> unresolvedMetadataList = resolveDynamicMetadata(metadataList);
        for (MBeanMetadata beanMetadata : metadataList) {
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
                MBeanExpression mBeanExpression = new MBeanExpression(beanMetadata.getMBean());
                switch (beanMetadata.getTypeRaw()) {
                    case COUNTER:
                        type = new MBeanCounterImpl(mBeanExpression);
                        break;
                    case GAUGE:
                        type = new MBeanGuageImpl(mBeanExpression);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported type : " + beanMetadata);
                }
                metricRegistry.register(beanMetadata, type, tags.toArray(new Tag[tags.size()]));
            } catch (IllegalArgumentException ex) {
                LOGGER.log(WARNING, ex.getMessage(), ex);
            }
        }
        return unresolvedMetadataList;
    }

    /**
     * Resolve dynamic metadata by replacing specifier <b>%s</b> with the mbean value.
     *
     * @param metadataList list of MBean Metadata
     * @return the list of unresolved MBean Metadata
     */
    public List<MBeanMetadata> resolveDynamicMetadata(List<MBeanMetadata> metadataList) {
        List<MBeanMetadata> unresolvedMetadataList = new ArrayList<>();
        List<MBeanMetadata> resolvedMetadataList = new ArrayList<>();
        List<Metadata> removedMetadataList = new ArrayList<>(metadataList.size());
        for (MBeanMetadata metadata : metadataList) {
            if (!metadata.isValid()) {
                removedMetadataList.add(metadata);
                continue;
            }
            if (metadata.getMBean().contains(SPECIFIER)
                    || metadata.getMBean().contains(KEY)
                    || metadata.getMBean().contains(ATTRIBUTE)
                    || metadata.getMBean().contains(SUB_ATTRIBUTE)) {
                try {
                    if (metadata.getMBean().contains(SPECIFIER)
                            || metadata.getMBean().contains(KEY)) {
                        MBeanExpression mBeanExpression = new MBeanExpression(
                                metadata.getMBean()
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
                                    loadAttribute(objName, mBeanExpression, metadata, dynamicValue)
                            );
                        }
                    } else {
                        MBeanExpression mBeanExpression = new MBeanExpression(metadata.getMBean());
                        ObjectName objName = mBeanExpression.getObjectName();
                        if (objName == null) {
                            unresolvedMetadataList.add(metadata);
                            LOGGER.log(INFO, "{0} does not correspond to any MBeans", metadata.getMBean());
                        } else if (metadata.isDynamic()) {
                            unresolvedMetadataList.add(metadata);
                        }
                        resolvedMetadataList.addAll(
                                loadAttribute(objName, mBeanExpression, metadata, null)
                        );
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(SEVERE, ex, () -> metadata.getMBean() + " is invalid");
                } finally {
                    removedMetadataList.add(metadata);
                }
            }
        }

        metadataList.removeAll(removedMetadataList);
        metadataList.addAll(resolvedMetadataList);
        return unresolvedMetadataList;
    }

    private List<MBeanMetadata> loadAttribute(
            ObjectName objName,
            MBeanExpression mBeanExpression,
            MBeanMetadata metadata,
            String key) {

        List<MBeanMetadata> metadataList = new ArrayList<>();
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
                            false
                    )
            );
        }
        return metadataList;
    }

    private List<MBeanMetadata> loadSubAttribute(
            ObjectName objName,
            MBeanExpression mBeanExpression,
            MBeanMetadata metadata,
            String key,
            String attribute,
            boolean isDynamicAttribute) {
        List<MBeanMetadata> metadataList = new ArrayList<>();
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
                                && metadata.getDescription().isPresent()) {
                            newMetadataBuilder = newMetadataBuilder.withDescription((String) compositeData.get(subAttribute));
                        } else if ("name".equals(subAttribute)
                                && compositeData.get(subAttribute) instanceof String
                                && metadata.getDisplayName() == null) {
                            newMetadataBuilder = newMetadataBuilder.withDisplayName((String) compositeData.get(subAttribute));
                        } else if ("unit".equals(subAttribute)
                                && compositeData.get(subAttribute) instanceof String
                                && MetricUnits.NONE.equals(metadata.getUnit().orElse("none"))) {
                            newMetadataBuilder = newMetadataBuilder.withUnit((String) compositeData.get(subAttribute));
                        }
                    }
                    MBeanMetadata newMbeanMetadata = new MBeanMetadata(newMetadataBuilder.build());
                    newMbeanMetadata.addTags(metadata.getTags());
                    metadataList.add(createMetadata(newMbeanMetadata, exp, key, attribute, subAttribute));
                }
            } else if (isDynamicAttribute) {
                Object obj = mBeanExpression.querySubAttributes(objName, attribute);
                if (obj instanceof CompositeDataSupport) {
                    CompositeDataSupport compositeData = (CompositeDataSupport) obj;
                    if (compositeData.containsKey(subAttribute) && compositeData.get(subAttribute) instanceof Number) {
                        metadataList.add(createMetadata(metadata, exp, key, attribute, subAttribute));
                    }
                }
            } else {
                metadataList.add(createMetadata(metadata, exp, key, attribute, subAttribute));
            }
        } else {
            metadataList.add(createMetadata(metadata, exp, key, attribute, subAttribute));
        }
        return metadataList;
    }

    private MBeanMetadata createMetadata(
            MBeanMetadata metadata,
            String exp,
            String key,
            String attribute,
            String subAttribute) {
        StringBuilder builder = new StringBuilder();
        builder.append(exp);
        builder.append(ATTRIBUTE_SEPARATOR);
        builder.append(attribute);
        if (subAttribute != null) {
            builder.append(SUB_ATTRIBUTE_SEPARATOR);
            builder.append(subAttribute);
        }
        MBeanMetadata newMetaData =  new MBeanMetadata(builder.toString(),
                formatMetadata(
                        metadata.getName(),
                        key,
                        attribute,
                        subAttribute
                ),
                formatMetadata(
                        nonNull(metadata.getDisplayName()) ? metadata.getDisplayName() : metadata.getName(),
                        key,
                        attribute,
                        subAttribute
                ),
                formatMetadata(
                        metadata.getDescription().isPresent() ? metadata.getDescription().get() : metadata.getName(),
                        key,
                        attribute,
                        subAttribute
                ),
                metadata.getTypeRaw(),
                metadata.getUnit().orElse(null)
        );
        for (XmlTag oldTag: metadata.getTags()) {
            XmlTag newTag = new XmlTag();
            newTag.setName(formatMetadata(oldTag.getName(), key, attribute, subAttribute));
            newTag.setValue(formatMetadata(oldTag.getValue(), key, attribute, subAttribute));
            newMetaData.getTags().add(newTag);
        }
        return newMetaData;
    }

    private String formatMetadata(
            String metadata,
            String dynamicValue,
            String attributeName,
            String subAttributeName) {
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
        return metadata;
    }

}
