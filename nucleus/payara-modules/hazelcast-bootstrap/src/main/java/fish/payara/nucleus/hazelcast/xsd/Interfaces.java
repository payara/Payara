
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 *                 You can specify which network interfaces that Hazelcast should use. Servers mostly have more
 *                 than one network interface, so you may want to list the valid IPs. Range characters
 *                 ('\*' and '-') can be used for simplicity. For instance, 10.3.10.\* refers to IPs between
 *                 10.3.10.0 and 10.3.10.255. Interface 10.3.10.4-18 refers to IPs between 10.3.10.4 and
 *                 10.3.10.18 (4 and 18 included). If network interface configuration is enabled (it is disabled
 *                 by default) and if Hazelcast cannot find an matching interface, then it will print a message
 *                 on the console and will not start on that node.
 *             
 * 
 * <p>Java Class of interfaces complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="interfaces">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="interface" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "interfaces", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "_interface"
})
public class Interfaces {

    @XmlElement(name = "interface", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "127.0.0.1")
    protected List<String> _interface;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of the interface property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the interface property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInterface().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getInterface() {
        if (_interface == null) {
            _interface = new ArrayList<String>();
        }
        return this._interface;
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
