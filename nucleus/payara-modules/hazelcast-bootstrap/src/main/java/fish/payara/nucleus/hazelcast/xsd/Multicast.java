
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of multicast complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="multicast">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="multicast-group" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="multicast-port" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" minOccurs="0"/>
 *         &lt;element name="multicast-timeout-seconds" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="multicast-time-to-live" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="trusted-interfaces" type="{http://www.hazelcast.com/schema/config}trusted-interfaces" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="loopbackModeEnabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "multicast", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Multicast {

    @XmlElement(name = "multicast-group", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "224.2.2.3")
    protected String multicastGroup;
    @XmlElement(name = "multicast-port", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "54327")
    @XmlSchemaType(name = "unsignedShort")
    protected Integer multicastPort;
    @XmlElement(name = "multicast-timeout-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "2")
    protected Integer multicastTimeoutSeconds;
    @XmlElement(name = "multicast-time-to-live", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "32")
    protected Integer multicastTimeToLive;
    @XmlElement(name = "trusted-interfaces", namespace = "http://www.hazelcast.com/schema/config")
    protected TrustedInterfaces trustedInterfaces;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;
    @XmlAttribute(name = "loopbackModeEnabled")
    protected Boolean loopbackModeEnabled;

    /**
     * Gets the value of property multicastGroup.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMulticastGroup() {
        return multicastGroup;
    }

    /**
     * Sets the value of property multicastGroup.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMulticastGroup(String value) {
        this.multicastGroup = value;
    }

    /**
     * Gets the value of property multicastPort.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMulticastPort() {
        return multicastPort;
    }

    /**
     * Sets the value of property multicastPort.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMulticastPort(Integer value) {
        this.multicastPort = value;
    }

    /**
     * Gets the value of property multicastTimeoutSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMulticastTimeoutSeconds() {
        return multicastTimeoutSeconds;
    }

    /**
     * Sets the value of property multicastTimeoutSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMulticastTimeoutSeconds(Integer value) {
        this.multicastTimeoutSeconds = value;
    }

    /**
     * Gets the value of property multicastTimeToLive.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMulticastTimeToLive() {
        return multicastTimeToLive;
    }

    /**
     * Sets the value of property multicastTimeToLive.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMulticastTimeToLive(Integer value) {
        this.multicastTimeToLive = value;
    }

    /**
     * Gets the value of property trustedInterfaces.
     * 
     * @return
     *     possible object is
     *     {@link TrustedInterfaces }
     *     
     */
    public TrustedInterfaces getTrustedInterfaces() {
        return trustedInterfaces;
    }

    /**
     * Sets the value of property trustedInterfaces.
     * 
     * @param value
     *     allowed object is
     *     {@link TrustedInterfaces }
     *     
     */
    public void setTrustedInterfaces(TrustedInterfaces value) {
        this.trustedInterfaces = value;
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

    /**
     * Gets the value of property loopbackModeEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isLoopbackModeEnabled() {
        if (loopbackModeEnabled == null) {
            return false;
        } else {
            return loopbackModeEnabled;
        }
    }

    /**
     * Sets the value of property loopbackModeEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLoopbackModeEnabled(Boolean value) {
        this.loopbackModeEnabled = value;
    }

}
