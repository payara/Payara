
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of vault complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="vault">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="address" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="secret-path" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="token" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ssl" type="{http://www.hazelcast.com/schema/config}factory-class-with-properties" minOccurs="0"/>
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
@XmlType(name = "vault", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Vault {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String address;
    @XmlElement(name = "secret-path", namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String secretPath;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String token;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected FactoryClassWithProperties ssl;
    @XmlElement(name = "polling-interval", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Integer pollingInterval;

    /**
     * Gets the value of property address.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the value of property address.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAddress(String value) {
        this.address = value;
    }

    /**
     * Gets the value of property secretPath.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecretPath() {
        return secretPath;
    }

    /**
     * Sets the value of property secretPath.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecretPath(String value) {
        this.secretPath = value;
    }

    /**
     * Gets the value of property token.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the value of property token.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToken(String value) {
        this.token = value;
    }

    /**
     * Gets the value of property ssl.
     * 
     * @return
     *     possible object is
     *     {@link FactoryClassWithProperties }
     *     
     */
    public FactoryClassWithProperties getSsl() {
        return ssl;
    }

    /**
     * Sets the value of property ssl.
     * 
     * @param value
     *     allowed object is
     *     {@link FactoryClassWithProperties }
     *     
     */
    public void setSsl(FactoryClassWithProperties value) {
        this.ssl = value;
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
