
package fish.payara.nucleus.hazelcast.xsd;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of fenced-lock complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="fenced-lock">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="lock-acquire-limit" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fenced-lock", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class FencedLock {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String name;
    @XmlElement(name = "lock-acquire-limit", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger lockAcquireLimit;

    /**
     * Gets the value of property name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of property name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of property lockAcquireLimit.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getLockAcquireLimit() {
        return lockAcquireLimit;
    }

    /**
     * Sets the value of property lockAcquireLimit.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setLockAcquireLimit(BigInteger value) {
        this.lockAcquireLimit = value;
    }

}
