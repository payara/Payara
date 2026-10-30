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

import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.ATTRIBUTE;
import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.KEY;
import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.SPECIFIER;
import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.SUB_ATTRIBUTE;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlTransient;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricUnits;

@XmlAccessorType(XmlAccessType.FIELD)
public class MetricsMetadata implements Metadata {

    private static final Logger LOGGER = Logger.getLogger(MetricsMetadata.class.getName());

    @XmlElement(name = "mbean")
    private String mBean;

    @XmlElement(name = "service")
    private String service;

    @XmlElement
    private boolean dynamic = true;

    @XmlElement
    private String name;

    @XmlElement
    private String description;

    @XmlElement
    private String unit;

    @XmlElement
    private String type;

    @XmlTransient
    private Boolean valid;

    @XmlElementWrapper(name = "tags", nillable = true)
    @XmlElement(name = "tag")
    private List<XmlTag> tags;


    public MetricsMetadata() {
        tags = new ArrayList<>();
    }

    public MetricsMetadata(Metadata metadata) {
        this(null, metadata.getName(), metadata.description().orElse(null), null, metadata.unit().orElse(null));
    }

    public MetricsMetadata(String mBean, String name, String description, String type, String unit) {
        this();
        this.type = type;
        this.mBean = mBean;
        this.name = name;
        this.description = description;
        this.unit = unit;
    }

    public MetricsMetadata(String name, String description, String type, String unit) {
        this();
        this.name = name;
        this.description = description;
        this.type = type;
        this.unit = unit;
    }

    public String getMBean() {
        return mBean;
    }

    public void setMBean(String mBean) {
        this.mBean = mBean;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isValid() {
        if (valid == null) {
            valid = validateMetadata();
        }
        return valid;
    }

    List<XmlTag> getTags() {
        return tags;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUnit() {
        return unit == null ? MetricUnits.NONE : unit;
    }

    @Override
    public Optional<String> unit() {
        return Optional.ofNullable(unit);
    }

    @Override
    public String getDescription() {
        return description == null ? "" : description;
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private boolean validateMetadata() {
        boolean validationResult = true;
        MetricsMetadata metadata = this;

        if (isNull(metadata.getName())) {
            LOGGER.log(WARNING, "'name' property not defined in " + metadata.getMBean() + " mbean metadata", new Exception());
            validationResult = false;
        }
        if (isNull(metadata.getMBean()) && isNull(metadata.getService())) {
            LOGGER.log(WARNING, "'mbean' or 'service' property not defined in {0} metadata", metadata.getName());
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
                } else if (metadata.getMBean().contains(keyword)) {
                    boolean tagSpecifier = false;
                    for (XmlTag tag : tags) {
                        if (tag.getValue() != null && tag.getValue().contains(keyword)) {
                            tagSpecifier = true;
                            break;
                        }
                    }
                    if (!(metadata.getName().contains(keyword) || tagSpecifier)) {
                        LOGGER.log(
                                WARNING,
                                "{0} placeholder not found in 'name' {1} property or in tags",
                                new String[]{keyword, metadata.getName()}
                        );
                        validationResult = false;
                    }
                }
            }
        }
        return validationResult;
    }

    public void setTags(List<XmlTag> tags) {
        if (tags == null) {
            throw new IllegalArgumentException("tags must not be null");
        }
        this.tags = tags;
    }

    public void addTags(List<XmlTag> tags) {
        this.tags.addAll(tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, unit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Metadata)) {
            return false;
        }
        Metadata that = (Metadata) o;

        return Objects.equals(name, that.getName()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
                Objects.equals(getUnit(), that.getUnit());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MetricsMetadata.class.getSimpleName() + "[", "]")
                .add("mBean='" + mBean + "'")
                .add("dynamic=" + dynamic)
                .add("name='" + name + "'")
                .add("description='" + description + "'")
                .add("unit='" + unit + "'")
                .add("type='"+ type + "'")
                .add("valid=" + valid)
                .add("tags=" + tags)
                .toString();
    }
}
