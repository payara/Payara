
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of security-object complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="security-object">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="properties" type="{http://www.hazelcast.com/schema/config}properties" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="class-name" use="required" type="{http://www.hazelcast.com/schema/config}non-space-string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "security-object", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "properties"
})
public class SecurityObject {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Properties properties;
    @XmlAttribute(name = "class-name", required = true)
    protected String className;

    /**
     * Gets the value of property properties.
     * 
     * @return
     *     possible object is
     *     {@link Properties }
     *     
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the value of property properties.
     * 
     * @param value
     *     allowed object is
     *     {@link Properties }
     *     
     */
    public void setProperties(Properties value) {
        this.properties = value;
    }

    /**
     * Gets the value of property className.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the value of property className.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClassName(String value) {
        this.className = value;
    }

}
