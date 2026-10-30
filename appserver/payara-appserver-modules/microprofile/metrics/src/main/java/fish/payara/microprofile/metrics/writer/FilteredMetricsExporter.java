/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.microprofile.metrics.writer;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Tag;

public class FilteredMetricsExporter extends OpenMetricsExporter {

    private final Collection<String> metricNames;

    public FilteredMetricsExporter(Writer out, Collection<String> metricNames) {
        super(out);
        this.metricNames = metricNames;
    }

    protected FilteredMetricsExporter(String scope, PrintWriter out, Set<String> typeWrittenByGlobalName,
                                      Set<String> helpWrittenByGlobalName, Collection<String> metricNames) {
        super(scope, out, typeWrittenByGlobalName, helpWrittenByGlobalName);
        this.metricNames = metricNames;
    }

    @Override
    public MetricExporter in(String scope, boolean asNode) {
        return new FilteredMetricsExporter(scope, out, typeWrittenByGlobalName, helpWrittenByGlobalName, metricNames);
    }

    @Override
    protected void appendTYPE(String globalName, OpenMetricsType type) {
        // Do nothing
    }

    @Override
    protected void appendHELP(String globalName, Metadata metadata) {
        // Do nothing
    }
    
    @Override
    protected void appendValue(String globalName, Tag[] tags, Number value) {
        String key = globalName + tagsToString(tags);
        if (metricNames.contains(key)) {
            out.append(key)
               .append('=')
               .append(roundValue(value))
               .append(',');
        }
    }

}
