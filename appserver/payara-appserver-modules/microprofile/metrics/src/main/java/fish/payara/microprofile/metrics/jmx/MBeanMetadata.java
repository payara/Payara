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

import fish.payara.microprofile.metrics.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

@XmlAccessorType(XmlAccessType.FIELD)
public class MBeanMetadata extends Metadata {

    private String mbean;
    
    public MBeanMetadata() {
        super(null, MetricType.INVALID);
    }
    
    public MBeanMetadata(String name, String displayName, String description, MetricType typeRaw, String unit) {
        super(name, displayName, description, typeRaw, unit);
    }

    public String getMbean() {
        return mbean;
    }

    public void setMbean(String mbean) {
        this.mbean = mbean;
    }

    public void setLabels(List<Tag> in) {
        for (Tag tag : in) {
            addTag(tag.toKVString());
        }
    }

    public List<Tag> getLabels() {
        List<Tag> out = new ArrayList<>(getTags().size());
        for (Map.Entry<String, String> entity : getTags().entrySet()) {
            Tag t = new Tag(entity.getKey(), entity.getValue());
            out.add(t);
        }
        return out;
    }

    public void processTags(List<Tag> globalTags) {
        globalTags.forEach(tag -> getTags().put(tag.getKey(), tag.getValue()));
        getLabels().forEach(tag -> getTags().put(tag.getKey(), tag.getValue()));
    }
}
