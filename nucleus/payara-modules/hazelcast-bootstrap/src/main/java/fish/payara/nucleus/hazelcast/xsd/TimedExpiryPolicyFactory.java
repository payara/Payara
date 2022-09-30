
package fish.payara.nucleus.hazelcast.xsd;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of timed-expiry-policy-factory complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="timed-expiry-policy-factory">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="expiry-policy-type" use="required" type="{http://www.hazelcast.com/schema/config}expiry-policy-type" />
 *       &lt;attribute name="duration-amount" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *       &lt;attribute name="time-unit" type="{http://www.hazelcast.com/schema/config}time-unit" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "timed-expiry-policy-factory", namespace = "http://www.hazelcast.com/schema/config")
public class TimedExpiryPolicyFactory {

    @XmlAttribute(name = "expiry-policy-type", required = true)
    protected ExpiryPolicyType expiryPolicyType;
    @XmlAttribute(name = "duration-amount")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger durationAmount;
    @XmlAttribute(name = "time-unit")
    protected TimeUnit timeUnit;

    /**
     * Gets the value of property expiryPolicyType.
     * 
     * @return
     *     possible object is
     *     {@link ExpiryPolicyType }
     *     
     */
    public ExpiryPolicyType getExpiryPolicyType() {
        return expiryPolicyType;
    }

    /**
     * Sets the value of property expiryPolicyType.
     * 
     * @param value
     *     allowed object is
     *     {@link ExpiryPolicyType }
     *     
     */
    public void setExpiryPolicyType(ExpiryPolicyType value) {
        this.expiryPolicyType = value;
    }

    /**
     * Gets the value of property durationAmount.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getDurationAmount() {
        return durationAmount;
    }

    /**
     * Sets the value of property durationAmount.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setDurationAmount(BigInteger value) {
        this.durationAmount = value;
    }

    /**
     * Gets the value of property timeUnit.
     * 
     * @return
     *     possible object is
     *     {@link TimeUnit }
     *     
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Sets the value of property timeUnit.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeUnit }
     *     
     */
    public void setTimeUnit(TimeUnit value) {
        this.timeUnit = value;
    }

}
