
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of metrics complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="metrics">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="management-center" type="{http://www.hazelcast.com/schema/config}metrics-management-center" minOccurs="0"/>
 *         &lt;element name="jmx" type="{http://www.hazelcast.com/schema/config}metrics-jmx" minOccurs="0"/>
 *         &lt;element name="collection-frequency-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "metrics", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Metrics {

    @XmlElement(name = "management-center", namespace = "http://www.hazelcast.com/schema/config")
    protected MetricsManagementCenter managementCenter;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected MetricsJmx jmx;
    @XmlElement(name = "collection-frequency-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "5")
    @XmlSchemaType(name = "unsignedInt")
    protected Long collectionFrequencySeconds;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property managementCenter.
     * 
     * @return
     *     possible object is
     *     {@link MetricsManagementCenter }
     *     
     */
    public MetricsManagementCenter getManagementCenter() {
        return managementCenter;
    }

    /**
     * Sets the value of property managementCenter.
     * 
     * @param value
     *     allowed object is
     *     {@link MetricsManagementCenter }
     *     
     */
    public void setManagementCenter(MetricsManagementCenter value) {
        this.managementCenter = value;
    }

    /**
     * Gets the value of property jmx.
     * 
     * @return
     *     possible object is
     *     {@link MetricsJmx }
     *     
     */
    public MetricsJmx getJmx() {
        return jmx;
    }

    /**
     * Sets the value of property jmx.
     * 
     * @param value
     *     allowed object is
     *     {@link MetricsJmx }
     *     
     */
    public void setJmx(MetricsJmx value) {
        this.jmx = value;
    }

    /**
     * Gets the value of property collectionFrequencySeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getCollectionFrequencySeconds() {
        return collectionFrequencySeconds;
    }

    /**
     * Sets the value of property collectionFrequencySeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCollectionFrequencySeconds(Long value) {
        this.collectionFrequencySeconds = value;
    }

    /**
     * Gets the value of property enabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEnabled() {
        if (enabled == null) {
            return true;
        } else {
            return enabled;
        }
    }

    /**
     * Sets the value of property enabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

}
