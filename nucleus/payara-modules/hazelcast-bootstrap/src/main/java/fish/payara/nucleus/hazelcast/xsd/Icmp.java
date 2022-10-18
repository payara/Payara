
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigInteger;


/**
 * 
 *                 ICMP can be used in addition to the other detectors. It operates at layer 3 detects network
 *                 and hardware issues more quickly
 *             
 * 
 * <p>Java Class of icmp complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="icmp">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="timeout-milliseconds" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="ttl" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="parallel-mode" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="fail-fast-on-startup" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="max-attempts" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="interval-milliseconds" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "icmp", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Icmp {

    @XmlElement(name = "timeout-milliseconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1000")
    protected BigInteger timeoutMilliseconds;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "255")
    protected BigInteger ttl;
    @XmlElement(name = "parallel-mode", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean parallelMode;
    @XmlElement(name = "fail-fast-on-startup", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean failFastOnStartup;
    @XmlElement(name = "max-attempts", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "2")
    protected BigInteger maxAttempts;
    @XmlElement(name = "interval-milliseconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1000")
    protected BigInteger intervalMilliseconds;
    @XmlAttribute(name = "enabled")
    @XmlSchemaType(name = "anySimpleType")
    protected String enabled;

    /**
     * Gets the value of property timeoutMilliseconds.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getTimeoutMilliseconds() {
        return timeoutMilliseconds;
    }

    /**
     * Sets the value of property timeoutMilliseconds.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setTimeoutMilliseconds(BigInteger value) {
        this.timeoutMilliseconds = value;
    }

    /**
     * Gets the value of property ttl.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getTtl() {
        return ttl;
    }

    /**
     * Sets the value of property ttl.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setTtl(BigInteger value) {
        this.ttl = value;
    }

    /**
     * Gets the value of property parallelMode.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isParallelMode() {
        return parallelMode;
    }

    /**
     * Sets the value of property parallelMode.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setParallelMode(Boolean value) {
        this.parallelMode = value;
    }

    /**
     * Gets the value of property failFastOnStartup.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isFailFastOnStartup() {
        return failFastOnStartup;
    }

    /**
     * Sets the value of property failFastOnStartup.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFailFastOnStartup(Boolean value) {
        this.failFastOnStartup = value;
    }

    /**
     * Gets the value of property maxAttempts.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Sets the value of property maxAttempts.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMaxAttempts(BigInteger value) {
        this.maxAttempts = value;
    }

    /**
     * Gets the value of property intervalMilliseconds.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getIntervalMilliseconds() {
        return intervalMilliseconds;
    }

    /**
     * Sets the value of property intervalMilliseconds.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setIntervalMilliseconds(BigInteger value) {
        this.intervalMilliseconds = value;
    }

    /**
     * Gets the value of property enabled.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEnabled() {
        if (enabled == null) {
            return "false";
        } else {
            return enabled;
        }
    }

    /**
     * Sets the value of property enabled.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEnabled(String value) {
        this.enabled = value;
    }

}
