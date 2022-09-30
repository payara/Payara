
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Enterprise only. Hazelcast allows you to encrypt the entire socket level communication among
 *                 all Hazelcast members. Encryption is based on Java Cryptography Architecture. In symmetric
 *                 encryption, each node uses the same key, so the key is shared.
 *             
 * 
 * <p>Java Class of symmetric-encryption complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="symmetric-encryption">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="algorithm" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="salt" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="iteration-count" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
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
@XmlType(name = "symmetric-encryption", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class SymmetricEncryption {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String algorithm;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "thesalt")
    protected String salt;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "thepassword")
    protected String password;
    @XmlElement(name = "iteration-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "19")
    protected Integer iterationCount;
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
     * Gets the value of property iterationCount.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getIterationCount() {
        return iterationCount;
    }

    /**
     * Sets the value of property iterationCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setIterationCount(Integer value) {
        this.iterationCount = value;
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
