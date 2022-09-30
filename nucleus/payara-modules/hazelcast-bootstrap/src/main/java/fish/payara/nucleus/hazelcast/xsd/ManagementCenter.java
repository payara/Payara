
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of management-center complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="management-center">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="trusted-interfaces" type="{http://www.hazelcast.com/schema/config}trusted-interfaces" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="scripting-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "management-center", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class ManagementCenter {

    @XmlElement(name = "trusted-interfaces", namespace = "http://www.hazelcast.com/schema/config")
    protected TrustedInterfaces trustedInterfaces;
    @XmlAttribute(name = "scripting-enabled")
    protected Boolean scriptingEnabled;

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
     * Gets the value of property scriptingEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isScriptingEnabled() {
        return scriptingEnabled;
    }

    /**
     * Sets the value of property scriptingEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setScriptingEnabled(Boolean value) {
        this.scriptingEnabled = value;
    }

}
