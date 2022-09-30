
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of crdt-replication complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="crdt-replication">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="replication-period-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="max-concurrent-replication-targets" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "crdt-replication", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class CrdtReplication {

    @XmlElement(name = "replication-period-millis", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long replicationPeriodMillis;
    @XmlElement(name = "max-concurrent-replication-targets", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxConcurrentReplicationTargets;

    /**
     * Gets the value of property replicationPeriodMillis.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getReplicationPeriodMillis() {
        return replicationPeriodMillis;
    }

    /**
     * Sets the value of property replicationPeriodMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setReplicationPeriodMillis(Long value) {
        this.replicationPeriodMillis = value;
    }

    /**
     * Gets the value of property maxConcurrentReplicationTargets.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMaxConcurrentReplicationTargets() {
        return maxConcurrentReplicationTargets;
    }

    /**
     * Sets the value of property maxConcurrentReplicationTargets.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxConcurrentReplicationTargets(Long value) {
        this.maxConcurrentReplicationTargets = value;
    }

}
