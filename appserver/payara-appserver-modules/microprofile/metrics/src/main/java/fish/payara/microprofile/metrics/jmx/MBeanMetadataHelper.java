/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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

import static fish.payara.microprofile.metrics.MetricsHelper.convertToTags;
import fish.payara.microprofile.metrics.Tag;
import fish.payara.microprofile.metrics.cdi.MetricRegistryRepository;
import org.eclipse.microprofile.metrics.Metadata;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import static org.eclipse.microprofile.metrics.Metadata.GLOBAL_TAGS_VARIABLE;
import org.eclipse.microprofile.metrics.MetricRegistry;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;

public class MBeanMetadataHelper {

    private static final String PLACEHOLDER = "%s";

    private static final Logger LOGGER = Logger.getLogger(MBeanMetadataHelper.class.getName());

    private static MBeanMetadataHelper instance;

    private MBeanMetadataHelper() {
    }

    public static MBeanMetadataHelper getInstance() {
        if (instance == null) {
            synchronized (MBeanMetadataHelper.class) {
                if (instance == null) {
                    instance = new MBeanMetadataHelper();
                }
            }
        }
        return instance;
    }
    
        /**
     * metrics.xml contains the base & vendor metrics metadata.
     *
     * @param metadataConfig
     */
    public static void initMetadataConfig(MBeanMetadataConfig metadataConfig){
        List<Tag> globalTags = convertToTags(System.getenv(GLOBAL_TAGS_VARIABLE));
        
        registerMetadata(
                MetricRegistryRepository.BASE_METRIC_REGISTRY,
                metadataConfig.getBase(), 
                globalTags);
        registerMetadata(
                MetricRegistryRepository.VENDOR_METRIC_REGISTRY,
                metadataConfig.getVendor(), 
                globalTags);
    }
    
    private static void registerMetadata(MetricRegistry metricRegistry,
            List<MBeanMetadata> metadataList, List<Tag> globalTags) {
        MBeanMetadataHelper.getInstance().expandDynamicMetadata(metadataList);
        metadataList.forEach(beanMetadata -> {
            beanMetadata.processTags(globalTags);
            org.eclipse.microprofile.metrics.Metric type;
            if (beanMetadata.getTypeRaw() == GAUGE) {
                type = new MBeanGaugeImpl(beanMetadata.getMbean());
            } else if (beanMetadata.getTypeRaw() == COUNTER) {
                type = new MBeanCounterImpl(beanMetadata.getMbean());
            } else {
                throw new IllegalStateException("Not yet supported: " + beanMetadata);
            }
            metricRegistry.register(beanMetadata.getName(), type, beanMetadata);
        });
    }

    /**
     * Get mbean expression value from the javax.management.MBeanServer
     *
     * @param expression The mbean expression to fetch value
     * @return The value of the Mbean attribute
     */
    public Number getExpressionValue(String expression) {
        try {
            MBeanExpression mBean = new MBeanExpression(expression);
            Object attribute = mBean.getAttribute();
            if (attribute instanceof Number) {
                return (Number) attribute;
            } else if (attribute instanceof CompositeData) {
                CompositeData compositeData = (CompositeData) attribute;
                return (Number) compositeData.get(mBean.getSubAttributeName());
            } else {
                throw new IllegalArgumentException(expression);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Expand dynamic metadata by replacing a placeholder of <b>%s</b>
     * in the name and MBean name with the real Mbean key-value.
     *
     * @param metadataList list of MBean Metadata
     */
    public void expandDynamicMetadata(List<MBeanMetadata> metadataList) {
        List<MBeanMetadata> resolvedMetadataList = new ArrayList<>();
        List<Metadata> removedMetadataList = new ArrayList<>(metadataList.size());
        for (MBeanMetadata metadata : metadataList) {
            if (metadata.getName().contains(PLACEHOLDER)) {
                MBeanExpression mBean;
                try {
                    mBean = new MBeanExpression(metadata.getMbean().replace(PLACEHOLDER, "*"));
                    String keyHolder = mBean.findDynamicKey();
                    Set<ObjectName> objNames = mBean.queryNames(null);
                    for (ObjectName objName : objNames) {
                        String keyValue = objName.getKeyPropertyList().get(keyHolder);
                        MBeanMetadata resolvedMetadata = new MBeanMetadata(
                                metadata.getName().replace(PLACEHOLDER, keyValue),
                                metadata.getDisplayName().replace(PLACEHOLDER, keyValue),
                                metadata.getDescription().replace(PLACEHOLDER, keyValue),
                                metadata.getTypeRaw(),
                                metadata.getUnit());
                        resolvedMetadata.setMbean(objName.getCanonicalName() + "/" + mBean.getAttributeName());
                        resolvedMetadataList.add(resolvedMetadata);
                    }
                    removedMetadataList.add(metadata);
                } catch (IllegalArgumentException | MalformedObjectNameException ex) {
                    LOGGER.log(Level.SEVERE, ex, () -> metadata.getMbean() + " is not valid");
                    removedMetadataList.add(metadata);
                }
            }
        }
        metadataList.removeAll(removedMetadataList);
        metadataList.addAll(resolvedMetadataList);
    }

}
