/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "importOrConfigReplacersOrClusterName"
})
@XmlRootElement(name = "hazelcast", namespace = "http://www.hazelcast.com/schema/config")
public class Hazelcast {

    @XmlElementRefs({
        @XmlElementRef(name = "multimap", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "reliable-topic", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "network", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "hot-restart-persistence", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "native-memory", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "sql", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "listeners", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "user-code-deployment", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "set", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "flake-id-generator", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "instance-name", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "member-attributes", namespace = "http://www.hazelcast.com/schema/config", type = MemberAttributes.class, required = false),
        @XmlElementRef(name = "topic", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "cluster-name", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "lite-member", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "crdt-replication", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "management-center", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "partition-group", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "durable-executor-service", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "split-brain-protection", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "security", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "cp-subsystem", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "instance-tracking", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "license-key", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "wan-replication", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "config-replacers", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "replicatedmap", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "ringbuffer", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "cardinality-estimator", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "properties", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "serialization", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "queue", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "cache", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "import", namespace = "http://www.hazelcast.com/schema/config", type = Import.class, required = false),
        @XmlElementRef(name = "pn-counter", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "list", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "auditlog", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "executor-service", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "advanced-network", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "map", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "scheduled-executor-service", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "metrics", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    protected java.util.List<Object> importOrConfigReplacersOrClusterName;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Gets the value of the importOrConfigReplacersOrClusterName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the importOrConfigReplacersOrClusterName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getImportOrConfigReplacersOrClusterName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Multimap }{@code >}
     * {@link JAXBElement }{@code <}{@link ReliableTopic }{@code >}
     * {@link JAXBElement }{@code <}{@link Network }{@code >}
     * {@link JAXBElement }{@code <}{@link HotRestartPersistence }{@code >}
     * {@link JAXBElement }{@code <}{@link NativeMemory }{@code >}
     * {@link JAXBElement }{@code <}{@link Sql }{@code >}
     * {@link JAXBElement }{@code <}{@link Listeners }{@code >}
     * {@link JAXBElement }{@code <}{@link UserCodeDeployment }{@code >}
     * {@link JAXBElement }{@code <}{@link Set }{@code >}
     * {@link JAXBElement }{@code <}{@link FlakeIdGenerator }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link MemberAttributes }
     * {@link JAXBElement }{@code <}{@link Topic }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link LiteMember }{@code >}
     * {@link JAXBElement }{@code <}{@link CrdtReplication }{@code >}
     * {@link JAXBElement }{@code <}{@link ManagementCenter }{@code >}
     * {@link JAXBElement }{@code <}{@link PartitionGroup }{@code >}
     * {@link JAXBElement }{@code <}{@link DurableExecutorService }{@code >}
     * {@link JAXBElement }{@code <}{@link SplitBrainProtection }{@code >}
     * {@link JAXBElement }{@code <}{@link Security }{@code >}
     * {@link JAXBElement }{@code <}{@link CpSubsystem }{@code >}
     * {@link JAXBElement }{@code <}{@link InstanceTracking }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link WanReplication }{@code >}
     * {@link JAXBElement }{@code <}{@link ConfigReplacers }{@code >}
     * {@link JAXBElement }{@code <}{@link Replicatedmap }{@code >}
     * {@link JAXBElement }{@code <}{@link Ringbuffer }{@code >}
     * {@link JAXBElement }{@code <}{@link CardinalityEstimator }{@code >}
     * {@link JAXBElement }{@code <}{@link Properties }{@code >}
     * {@link JAXBElement }{@code <}{@link Serialization }{@code >}
     * {@link JAXBElement }{@code <}{@link Queue }{@code >}
     * {@link JAXBElement }{@code <}{@link Cache }{@code >}
     * {@link Import }
     * {@link JAXBElement }{@code <}{@link PnCounter }{@code >}
     * {@link JAXBElement }{@code <}{@link List }{@code >}
     * {@link JAXBElement }{@code <}{@link FactoryClassWithProperties }{@code >}
     * {@link JAXBElement }{@code <}{@link ExecutorService }{@code >}
     * {@link JAXBElement }{@code <}{@link AdvancedNetwork }{@code >}
     * {@link JAXBElement }{@code <}{@link Map }{@code >}
     * {@link JAXBElement }{@code <}{@link ScheduledExecutorService }{@code >}
     * {@link JAXBElement }{@code <}{@link Metrics }{@code >}
     * 
     * 
     */
    public java.util.List<Object> getImportOrConfigReplacersOrClusterName() {
        if (importOrConfigReplacersOrClusterName == null) {
            importOrConfigReplacersOrClusterName = new ArrayList<Object>();
        }
        return this.importOrConfigReplacersOrClusterName;
    }

    /**
     * Gets the value of property id.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        if (id == null) {
            return "default";
        } else {
            return id;
        }
    }

    /**
     * Sets the value of property id.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
