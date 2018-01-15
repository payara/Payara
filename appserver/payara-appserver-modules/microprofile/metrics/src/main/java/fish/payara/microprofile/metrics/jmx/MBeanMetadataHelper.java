/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ObjectName;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;

public class MBeanMetadataHelper {

    private static final String SPECIFIER = "%s";

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

    public static void registerMetadata(MetricRegistry metricRegistry,
            List<MBeanMetadata> metadataList, Map<String, String> globalTags) {

        if (!metricRegistry.getMetadata().isEmpty()) {
            metricRegistry.removeMatching(MetricFilter.ALL);
        }

        MBeanMetadataHelper.getInstance().resolveDynamicMetadata(metadataList);
        metadataList.forEach(beanMetadata -> {
            beanMetadata.getTags().putAll(globalTags);
            org.eclipse.microprofile.metrics.Metric type;
            MBeanExpression mBeanExpression = new MBeanExpression(beanMetadata.getMBean());
             if (beanMetadata.getTypeRaw() == COUNTER) {
                 type = new MBeanCounterImpl(mBeanExpression);
            } else if (beanMetadata.getTypeRaw() == GAUGE) {
                type = (Gauge) mBeanExpression::getNumberValue;
            }  else {
                throw new IllegalStateException("Unsupported type : " + beanMetadata);
            }
            metricRegistry.register(beanMetadata.getName(), type, beanMetadata);
        });
    }

    /**
     * Resolve dynamic metadata by replacing specifier <b>%s</b> with the mbean value.
     *
     * @param metadataList list of MBean Metadata
     */
    public void resolveDynamicMetadata(List<MBeanMetadata> metadataList) {
        List<MBeanMetadata> resolvedMetadataList = new ArrayList<>();
        List<Metadata> removedMetadataList = new ArrayList<>(metadataList.size());
        for (MBeanMetadata metadata : metadataList) {
            if (metadata.getName().contains(SPECIFIER)) {
                MBeanExpression mBeanExpression;
                try {
                    mBeanExpression = new MBeanExpression(metadata.getMBean().replace(SPECIFIER, "*"));
                    String dynamicKey = mBeanExpression.findDynamicKey();
                    for (ObjectName objName : mBeanExpression.queryNames(null)) {
                        String dynamicValue = objName.getKeyPropertyList().get(dynamicKey);
                        resolvedMetadataList.add(new MBeanMetadata(
                                objName.getCanonicalName() + "/" + mBeanExpression.getAttributeName(),
                                metadata.getName().replace(SPECIFIER, dynamicValue),
                                metadata.getDisplayName().replace(SPECIFIER, dynamicValue),
                                metadata.getDescription().replace(SPECIFIER, dynamicValue),
                                metadata.getTypeRaw(),
                                metadata.getUnit())
                        );
                    }
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.SEVERE, ex, () -> metadata.getMBean() + " is invalid");
                } finally {
                    removedMetadataList.add(metadata);
                }
            }
        }
        metadataList.removeAll(removedMetadataList);
        metadataList.addAll(resolvedMetadataList);
    }

}
