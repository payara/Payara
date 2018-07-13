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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ObjectName;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static org.eclipse.microprofile.metrics.MetricType.GAUGE;
import org.jvnet.hk2.annotations.Service;

@Service
public class MBeanMetadataHelper {

    private static final String SPECIFIER = "%s";

    private static final Logger LOGGER = Logger.getLogger(MBeanMetadataHelper.class.getName());
    
    private List<MBeanMetadata> unresolvedMetadataList;

    /**
     * Registers metrics as MBeans
     * @param metricRegistry Registry to add metrics to
     * @param metadataList List of all {@link MBeanMetadata} representing a {@link Metric}
     * @param globalTags 
     * @param isRetry true if this is not initial registration, this is used to register 
     * lazy-loaded MBeans
     */
    public void registerMetadata(MetricRegistry metricRegistry,
            List<MBeanMetadata> metadataList, Map<String, String> globalTags, boolean isRetry) {

        if (!metricRegistry.getMetadata().isEmpty()) {
            metricRegistry.removeMatching(MetricFilter.ALL);
        }

        resolveDynamicMetadata(metadataList);
        for (MBeanMetadata beanMetadata: metadataList){
          try {
            if (metricRegistry.getNames().contains(beanMetadata.getName()) && isRetry){
                //
                continue;
            }
            beanMetadata.getTags().putAll(globalTags);
            Metric type;
            MBeanExpression mBeanExpression = new MBeanExpression(beanMetadata.getMBean());
            switch (beanMetadata.getTypeRaw()) {
                case COUNTER:
                    type = new MBeanCounterImpl(mBeanExpression);
                    break;
                case GAUGE:
                    type = (Gauge<Number>) mBeanExpression::getNumberValue;
                    break;
                default:
                    throw new IllegalStateException("Unsupported type : " + beanMetadata);
            }
            metricRegistry.register(beanMetadata, type);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, e.getMessage());
            }
        }
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
            if (!validateMetadata(metadata)) {
                removedMetadataList.add(metadata);
                continue;
            }
            if (metadata.getName().contains(SPECIFIER)) {
                MBeanExpression mBeanExpression;
                try {
                    mBeanExpression = new MBeanExpression(metadata.getMBean().replace(SPECIFIER, "*"));
                    String dynamicKey = mBeanExpression.findDynamicKey();
                    Set<ObjectName> mBeanObjects = mBeanExpression.queryNames(null);
                    if (mBeanObjects.isEmpty()){
                        LOGGER.log(Level.INFO, "{0} does not correspond to any MBeans", metadata.getMBean());
                    }
                    for (ObjectName objName : mBeanObjects) {
                        String dynamicValue = objName.getKeyPropertyList().get(dynamicKey);

                        StringBuilder builder = new StringBuilder();
                        builder.append(objName.getCanonicalName());
                        builder.append("/");
                        builder.append(mBeanExpression.getAttributeName());
                        String subAttrName = mBeanExpression.getSubAttributeName();
                        if (subAttrName != null) {
                            builder.append("#");
                            builder.append(subAttrName);
                        }

                        resolvedMetadataList.add(
                                new MBeanMetadata(
                                        builder.toString(),
                                        metadata.getName().replace(SPECIFIER, dynamicValue),
                                        metadata.getDisplayName().replace(SPECIFIER, dynamicValue),
                                        metadata.getDescription().replace(SPECIFIER, dynamicValue),
                                        metadata.getTypeRaw(),
                                        metadata.getUnit()
                                )
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
    
    private boolean validateMetadata(MBeanMetadata metadata) {
        boolean valid = true;
        
        if (metadata.getName() == null) {
            LOGGER.log(Level.WARNING, "'name' property not defined in {0} mbean metadata", metadata.getMBean());
            valid = false;
        }
        if (metadata.getMBean() == null) {
            LOGGER.log(Level.WARNING, "'mbean' property not defined in {0} metadata", metadata.getName());
            valid = false;
        }
        if (metadata.getDisplayName() == null) {
            LOGGER.log(Level.WARNING, "'displayName' property not defined in {0} metadata", metadata.getName());
            valid = false;
        }
        if (metadata.getDescription() == null) {
            LOGGER.log(Level.WARNING, "'description' property not defined in {0} metadata", metadata.getName());
            valid = false;
        }
        if (metadata.getType() == null) {
            LOGGER.log(Level.WARNING, "'type' property not defined in {0} metadata", metadata.getName());
            valid = false;
        }
        if (metadata.getUnit() == null) {
            LOGGER.log(Level.WARNING, "'unit' property not defined for {0} metadata", metadata.getName());
            valid = false;
        }
        if (metadata.getName() != null && metadata.getMBean() != null) {
            if (metadata.getName().contains(SPECIFIER) && !metadata.getMBean().contains(SPECIFIER)) {
                LOGGER.log(Level.WARNING, "'%s' placeholder not found in 'mbean' {0} property", metadata.getMBean());
                valid = false;
            } else if (metadata.getMBean().contains(SPECIFIER) && !metadata.getName().contains(SPECIFIER)) {
                LOGGER.log(Level.WARNING, "'%s' placeholder not found in 'name' {0} property", metadata.getName());
                valid = false;
            }
        }

        return valid;
    }

}
