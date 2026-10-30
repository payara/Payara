/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.microprofile.metrics.impl;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsCustomPercentiles {
    
    private Double[] percentiles;

    protected String metricName;
    
    private boolean isDisabled;

    public MetricsCustomPercentiles(String name, Double[] percentiles) {
        this.metricName = name;
        this.percentiles = percentiles;
    }
    
    public MetricsCustomPercentiles(String name, boolean isDisabled) {
        this.metricName = name;
        this.isDisabled = isDisabled;
    }

    public Double[] getPercentiles() {
        return percentiles;
    }

    public void setPercentiles(Double[] percentiles) {
        this.percentiles = percentiles;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    public String getMetricName() {
        return metricName;
    }

    public static MetricsCustomPercentiles matches(Collection<MetricsCustomPercentiles> configurations, String metricName) {
        for (MetricsCustomPercentiles propertyConfig : configurations) {
            int idxWildcard = propertyConfig.getMetricName().indexOf("*");
            if (idxWildcard > -1 && metricName.contains(propertyConfig.getMetricName().substring(0, idxWildcard))) {
                return propertyConfig;
            }
            Pattern p = Pattern.compile(metricName.trim());
            Matcher m = p.matcher(propertyConfig.getMetricName().trim());
            if (m.matches()) {
                return propertyConfig;
            }
        }
        return null;
    }
}
