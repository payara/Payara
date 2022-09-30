
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Configuration for hot restart (symmetric) encryption at rest. Encryption is based
 *                 on Java Cryptography Architecture.
 *             
 * 
 * <p>Java Class of encryption-at-rest complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="encryption-at-rest">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="algorithm" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="salt" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="key-size" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="secure-store" type="{http://www.hazelcast.com/schema/config}secure-store"/>
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
@XmlType(name = "encryption-at-rest", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class EncryptionAtRest {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String algorithm;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "thesalt")
    protected String salt;
    @XmlElement(name = "key-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Integer keySize;
    @XmlElement(name = "secure-store", namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected SecureStore secureStore;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property algorithm.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets the value of property algorithm.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAlgorithm(String value) {
        this.algorithm = value;
    }

    /**
     * Gets the value of property salt.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSalt() {
        return salt;
    }

    /**
     * Sets the value of property salt.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSalt(String value) {
        this.salt = value;
    }

    /**
     * Gets the value of property keySize.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getKeySize() {
        return keySize;
    }

    /**
     * Sets the value of property keySize.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setKeySize(Integer value) {
        this.keySize = value;
    }

    /**
     * Gets the value of property secureStore.
     * 
     * @return
     *     possible object is
     *     {@link SecureStore }
     *     
     */
    public SecureStore getSecureStore() {
        return secureStore;
    }

    /**
     * Sets the value of property secureStore.
     * 
     * @param value
     *     allowed object is
     *     {@link SecureStore }
     *     
     */
    public void setSecureStore(SecureStore value) {
        this.secureStore = value;
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
