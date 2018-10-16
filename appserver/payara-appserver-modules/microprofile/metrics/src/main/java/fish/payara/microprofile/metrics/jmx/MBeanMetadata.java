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

import static fish.payara.microprofile.metrics.jmx.MBeanMetadataHelper.ATTRIBUTE;
import static fish.payara.microprofile.metrics.jmx.MBeanMetadataHelper.KEY;
import static fish.payara.microprofile.metrics.jmx.MBeanMetadataHelper.SPECIFIER;
import static fish.payara.microprofile.metrics.jmx.MBeanMetadataHelper.SUB_ATTRIBUTE;
import static java.util.Arrays.asList;
import java.util.HashSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

@XmlAccessorType(XmlAccessType.FIELD)
public class MBeanMetadata extends Metadata {

    private static final Logger LOGGER = Logger.getLogger(MBeanMetadata.class.getName());

    @XmlElement(name = "mbean")
    private String mBean;

    @XmlElement
    private boolean dynamic = true;

    @XmlTransient
    private Boolean valid;

    private static final Set<String> SUPPORTED_UNITS
            = new HashSet<>(asList(
                    MetricUnits.NONE,
                    MetricUnits.BITS,
                    MetricUnits.KILOBITS,
                    MetricUnits.MEGABITS,
                    MetricUnits.GIGABITS,
                    MetricUnits.KIBIBITS,
                    MetricUnits.MEBIBITS,
                    MetricUnits.GIBIBITS,
                    MetricUnits.BYTES,
                    MetricUnits.KILOBYTES,
                    MetricUnits.MEGABYTES,
                    MetricUnits.GIGABYTES,
                    MetricUnits.NANOSECONDS,
                    MetricUnits.MICROSECONDS,
                    MetricUnits.MILLISECONDS,
                    MetricUnits.SECONDS,
                    MetricUnits.MINUTES,
                    MetricUnits.HOURS,
                    MetricUnits.DAYS,
                    MetricUnits.PERCENT,
                    MetricUnits.PER_SECOND
            ));

    public MBeanMetadata() {
        super(null, MetricType.INVALID);
    }

    public MBeanMetadata(String mBean, String name, String displayName, String description, MetricType typeRaw, String unit) {
        super(name, displayName, description, typeRaw, unit);
        this.mBean = mBean;
    }

    public String getMBean() {
        return mBean;
    }

    public void setMBean(String mBean) {
        this.mBean = mBean;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public String getUnit() {
        if (isNull(super.getUnit())) {
            setUnit(MetricUnits.NONE);
        }
        return super.getUnit();
    }

    public void setUnit(String unit) {
        if (SUPPORTED_UNITS.contains(unit)) {
            super.setUnit(unit);
        }
    }

    public boolean isValid() {
        if (valid == null) {
            valid = validateMetadata();
        }
        return valid;
    }

    private boolean validateMetadata() {
        boolean validationResult = true;
        MBeanMetadata metadata = this;

        if (isNull(metadata.getName())) {
            LOGGER.log(WARNING, "'name' property not defined in {0} mbean metadata", metadata.getMBean());
            validationResult = false;
        }
        if (isNull(metadata.getMBean())) {
            LOGGER.log(WARNING, "'mbean' property not defined in {0} metadata", metadata.getName());
            validationResult = false;
        }
        if (isNull(metadata.getType())) {
            LOGGER.log(WARNING, "'type' property not defined in {0} metadata", metadata.getName());
            validationResult = false;
        }
        if (nonNull(metadata.getName()) && nonNull(metadata.getMBean())) {
            for (String keyword : new String[]{SPECIFIER, KEY, ATTRIBUTE, SUB_ATTRIBUTE}) {
                if (metadata.getName().contains(keyword) && !metadata.getMBean().contains(keyword)) {
                    LOGGER.log(
                            WARNING,
                            "{0} placeholder not found in 'mbean' {1} property",
                            new String[]{keyword, metadata.getMBean()}
                    );
                    validationResult = false;
                } else if (metadata.getMBean().contains(keyword) && !metadata.getName().contains(keyword)) {
                    LOGGER.log(
                            WARNING,
                            "{0} placeholder not found in 'name' {1} property",
                            new String[]{keyword, metadata.getName()}
                    );
                    validationResult = false;
                }
            }
        }
        return validationResult;
    }

}
