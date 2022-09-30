
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of executor-service complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="executor-service">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="pool-size" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" minOccurs="0"/>
 *         &lt;element name="queue-capacity" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="split-brain-protection-ref" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" default="default" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "executor-service", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class ExecutorService {

    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean statisticsEnabled;
    @XmlElement(name = "pool-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "8")
    @XmlSchemaType(name = "unsignedShort")
    protected Integer poolSize;
    @XmlElement(name = "queue-capacity", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long queueCapacity;
    @XmlElement(name = "split-brain-protection-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected Object splitBrainProtectionRef;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of property statisticsEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    /**
     * Sets the value of property statisticsEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setStatisticsEnabled(Boolean value) {
        this.statisticsEnabled = value;
    }

    /**
     * Gets the value of property poolSize.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPoolSize() {
        return poolSize;
    }

    /**
     * Sets the value of property poolSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPoolSize(Integer value) {
        this.poolSize = value;
    }

    /**
     * Gets the value of property queueCapacity.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * Sets the value of property queueCapacity.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setQueueCapacity(Long value) {
        this.queueCapacity = value;
    }

    /**
     * Gets the value of property splitBrainProtectionRef.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getSplitBrainProtectionRef() {
        return splitBrainProtectionRef;
    }

    /**
     * Sets the value of property splitBrainProtectionRef.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setSplitBrainProtectionRef(Object value) {
        this.splitBrainProtectionRef = value;
    }

    /**
     * Gets the value of property name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        if (name == null) {
            return "default";
        } else {
            return name;
        }
    }

    /**
     * Sets the value of property name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

}
