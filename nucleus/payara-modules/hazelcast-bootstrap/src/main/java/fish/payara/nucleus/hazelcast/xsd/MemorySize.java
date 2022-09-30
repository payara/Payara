
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of memory-size complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="memory-size">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}int" default="128" />
 *       &lt;attribute name="unit" type="{http://www.hazelcast.com/schema/config}memory-unit" default="MEGABYTES" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "memory-size", namespace = "http://www.hazelcast.com/schema/config")
public class MemorySize {

    @XmlAttribute(name = "value")
    protected Integer value;
    @XmlAttribute(name = "unit")
    protected MemoryUnit unit;

    /**
     * Gets the value of property value.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getValue() {
        if (value == null) {
            return  128;
        } else {
            return value;
        }
    }

    /**
     * Sets the value of property value.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setValue(Integer value) {
        this.value = value;
    }

    /**
     * Gets the value of property unit.
     * 
     * @return
     *     possible object is
     *     {@link MemoryUnit }
     *     
     */
    public MemoryUnit getUnit() {
        if (unit == null) {
            return MemoryUnit.MEGABYTES;
        } else {
            return unit;
        }
    }

    /**
     * Sets the value of property unit.
     * 
     * @param value
     *     allowed object is
     *     {@link MemoryUnit }
     *     
     */
    public void setUnit(MemoryUnit value) {
        this.unit = value;
    }

}
