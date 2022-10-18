
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of advanced-network complex type.
 * 
 * <pre>
 * &lt;complexType name="advanced-network">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="join" type="{http://www.hazelcast.com/schema/config}join" minOccurs="0"/>
 *         &lt;element name="failure-detector" type="{http://www.hazelcast.com/schema/config}failure-detector" minOccurs="0"/>
 *         &lt;element name="member-address-provider" type="{http://www.hazelcast.com/schema/config}member-address-provider" minOccurs="0"/>
 *         &lt;element name="wan-endpoint-config" type="{http://www.hazelcast.com/schema/config}endpoint-config" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="wan-server-socket-endpoint-config" type="{http://www.hazelcast.com/schema/config}server-socket-endpoint-config" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="member-server-socket-endpoint-config" type="{http://www.hazelcast.com/schema/config}server-socket-endpoint-config"/>
 *         &lt;element name="client-server-socket-endpoint-config" type="{http://www.hazelcast.com/schema/config}server-socket-endpoint-config" minOccurs="0"/>
 *         &lt;element name="rest-server-socket-endpoint-config" type="{http://www.hazelcast.com/schema/config}rest-server-socket-endpoint-config" minOccurs="0"/>
 *         &lt;element name="memcache-server-socket-endpoint-config" type="{http://www.hazelcast.com/schema/config}server-socket-endpoint-config" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "advanced-network", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "joinOrFailureDetectorOrMemberAddressProvider"
})
public class AdvancedNetwork {

    @XmlElementRefs({
        @XmlElementRef(name = "failure-detector", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "wan-endpoint-config", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "join", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "member-address-provider", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "wan-server-socket-endpoint-config", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "member-server-socket-endpoint-config", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "rest-server-socket-endpoint-config", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "client-server-socket-endpoint-config", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "memcache-server-socket-endpoint-config", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> joinOrFailureDetectorOrMemberAddressProvider;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of the joinOrFailureDetectorOrMemberAddressProvider property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the joinOrFailureDetectorOrMemberAddressProvider property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getJoinOrFailureDetectorOrMemberAddressProvider().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link FailureDetector }{@code >}
     * {@link JAXBElement }{@code <}{@link EndpointConfig }{@code >}
     * {@link JAXBElement }{@code <}{@link Join }{@code >}
     * {@link JAXBElement }{@code <}{@link MemberAddressProvider }{@code >}
     * {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}
     * {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}
     * {@link JAXBElement }{@code <}{@link RestServerSocketEndpointConfig }{@code >}
     * {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}
     * {@link JAXBElement }{@code <}{@link ServerSocketEndpointConfig }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getJoinOrFailureDetectorOrMemberAddressProvider() {
        if (joinOrFailureDetectorOrMemberAddressProvider == null) {
            joinOrFailureDetectorOrMemberAddressProvider = new ArrayList<JAXBElement<?>>();
        }
        return this.joinOrFailureDetectorOrMemberAddressProvider;
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
