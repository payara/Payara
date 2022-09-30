
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Configuration for an event journal. The event journal keeps events related
 *                 to a specific partition and data structure. For instance, it could keep
 *                 map or cache add, update, remove, merge events along with the key, old value,
 *                 new value and so on.
 *                 This configuration is not tied to a specific data structure and can be reused.
 *             
 * 
 * <p>Java Class of event-journal complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="event-journal">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="capacity" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="time-to-live-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "event-journal", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class EventJournal {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long capacity;
    @XmlElement(name = "time-to-live-seconds", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long timeToLiveSeconds;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property capacity.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getCapacity() {
        return capacity;
    }

    /**
     * Sets the value of property capacity.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCapacity(Long value) {
        this.capacity = value;
    }

    /**
     * Gets the value of property timeToLiveSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    /**
     * Sets the value of property timeToLiveSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setTimeToLiveSeconds(Long value) {
        this.timeToLiveSeconds = value;
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
            return false;
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
