
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of keystore complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="keystore">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="path" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="current-key-alias" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="polling-interval" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "keystore", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Keystore {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String path;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "PKCS12")
    protected String type;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected String password;
    @XmlElement(name = "current-key-alias", namespace = "http://www.hazelcast.com/schema/config")
    protected String currentKeyAlias;
    @XmlElement(name = "polling-interval", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Integer pollingInterval;

    /**
     * Gets the value of property path.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the value of property path.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPath(String value) {
        this.path = value;
    }

    /**
     * Gets the value of property type.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of property type.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of property password.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of property password.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of property currentKeyAlias.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrentKeyAlias() {
        return currentKeyAlias;
    }

    /**
     * Sets the value of property currentKeyAlias.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrentKeyAlias(String value) {
        this.currentKeyAlias = value;
    }

    /**
     * Gets the value of property pollingInterval.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Sets the value of property pollingInterval.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPollingInterval(Integer value) {
        this.pollingInterval = value;
    }

}
