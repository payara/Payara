
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of metrics-management-center complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="metrics-management-center">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="retention-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
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
@XmlType(name = "metrics-management-center", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class MetricsManagementCenter {

    @XmlElement(name = "retention-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "5")
    @XmlSchemaType(name = "unsignedInt")
    protected Long retentionSeconds;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property retentionSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getRetentionSeconds() {
        return retentionSeconds;
    }

    /**
     * Sets the value of property retentionSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setRetentionSeconds(Long value) {
        this.retentionSeconds = value;
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
