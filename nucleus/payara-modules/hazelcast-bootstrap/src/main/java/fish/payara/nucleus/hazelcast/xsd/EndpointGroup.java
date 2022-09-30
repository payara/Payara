
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of endpoint-group complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="endpoint-group">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{http://www.hazelcast.com/schema/config}endpoint-group-name" />
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "endpoint-group", namespace = "http://www.hazelcast.com/schema/config")
public class EndpointGroup {

    @XmlAttribute(name = "name", required = true)
    protected EndpointGroupName name;
    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;

    /**
     * Gets the value of property name.
     * 
     * @return
     *     possible object is
     *     {@link EndpointGroupName }
     *     
     */
    public EndpointGroupName getName() {
        return name;
    }

    /**
     * Sets the value of property name.
     * 
     * @param value
     *     allowed object is
     *     {@link EndpointGroupName }
     *     
     */
    public void setName(EndpointGroupName value) {
        this.name = value;
    }

    /**
     * Gets the value of property enabled.
     * 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of property enabled.
     * 
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

}
