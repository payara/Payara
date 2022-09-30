
package fish.payara.nucleus.hazelcast.xsd;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of eviction complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="eviction">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="size" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" default="10000" />
 *       &lt;attribute name="max-size-policy" type="{http://www.hazelcast.com/schema/config}max-size-policy" default="ENTRY_COUNT" />
 *       &lt;attribute name="eviction-policy" type="{http://www.hazelcast.com/schema/config}eviction-policy" default="LRU" />
 *       &lt;attribute name="comparator-class-name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "eviction", namespace = "http://www.hazelcast.com/schema/config")
public class Eviction {

    @XmlAttribute(name = "size")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger size;
    @XmlAttribute(name = "max-size-policy")
    protected MaxSizePolicy maxSizePolicy;
    @XmlAttribute(name = "eviction-policy")
    protected EvictionPolicy evictionPolicy;
    @XmlAttribute(name = "comparator-class-name")
    protected String comparatorClassName;

    /**
     * Gets the value of property size.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSize() {
        if (size == null) {
            return new BigInteger("10000");
        } else {
            return size;
        }
    }

    /**
     * Sets the value of property size.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSize(BigInteger value) {
        this.size = value;
    }

    /**
     * Gets the value of property maxSizePolicy.
     * 
     * @return
     *     possible object is
     *     {@link MaxSizePolicy }
     *     
     */
    public MaxSizePolicy getMaxSizePolicy() {
        if (maxSizePolicy == null) {
            return MaxSizePolicy.ENTRY_COUNT;
        } else {
            return maxSizePolicy;
        }
    }

    /**
     * Sets the value of property maxSizePolicy.
     * 
     * @param value
     *     allowed object is
     *     {@link MaxSizePolicy }
     *     
     */
    public void setMaxSizePolicy(MaxSizePolicy value) {
        this.maxSizePolicy = value;
    }

    /**
     * Gets the value of property evictionPolicy.
     * 
     * @return
     *     possible object is
     *     {@link EvictionPolicy }
     *     
     */
    public EvictionPolicy getEvictionPolicy() {
        if (evictionPolicy == null) {
            return EvictionPolicy.LRU;
        } else {
            return evictionPolicy;
        }
    }

    /**
     * Sets the value of property evictionPolicy.
     * 
     * @param value
     *     allowed object is
     *     {@link EvictionPolicy }
     *     
     */
    public void setEvictionPolicy(EvictionPolicy value) {
        this.evictionPolicy = value;
    }

    /**
     * Gets the value of property comparatorClassName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComparatorClassName() {
        return comparatorClassName;
    }

    /**
     * Sets the value of property comparatorClassName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComparatorClassName(String value) {
        this.comparatorClassName = value;
    }

}
