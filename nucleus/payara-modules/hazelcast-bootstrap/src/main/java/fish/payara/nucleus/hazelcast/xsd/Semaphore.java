
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of semaphore complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="semaphore">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="jdk-compatible" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="initial-permits" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "semaphore", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Semaphore {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected String name;
    @XmlElement(name = "jdk-compatible", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean jdkCompatible;
    @XmlElement(name = "initial-permits", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long initialPermits;

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
     * Gets the value of property jdkCompatible.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isJdkCompatible() {
        return jdkCompatible;
    }

    /**
     * Sets the value of property jdkCompatible.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setJdkCompatible(Boolean value) {
        this.jdkCompatible = value;
    }

    /**
     * Gets the value of property initialPermits.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getInitialPermits() {
        return initialPermits;
    }

    /**
     * Sets the value of property initialPermits.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setInitialPermits(Long value) {
        this.initialPermits = value;
    }

}
