
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of index complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="index">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="attributes" type="{http://www.hazelcast.com/schema/config}index-attributes"/>
 *         &lt;element name="bitmap-index-options" type="{http://www.hazelcast.com/schema/config}bitmap-index-options" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="type" type="{http://www.hazelcast.com/schema/config}index-type" default="SORTED" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "index", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Index {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected IndexAttributes attributes;
    @XmlElement(name = "bitmap-index-options", namespace = "http://www.hazelcast.com/schema/config")
    protected BitmapIndexOptions bitmapIndexOptions;
    @XmlAttribute(name = "name")
    @XmlSchemaType(name = "anySimpleType")
    protected String name;
    @XmlAttribute(name = "type")
    protected IndexType type;

    /**
     * Gets the value of property attributes.
     * 
     * @return
     *     possible object is
     *     {@link IndexAttributes }
     *     
     */
    public IndexAttributes getAttributes() {
        return attributes;
    }

    /**
     * Sets the value of property attributes.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexAttributes }
     *     
     */
    public void setAttributes(IndexAttributes value) {
        this.attributes = value;
    }

    /**
     * Gets the value of property bitmapIndexOptions.
     * 
     * @return
     *     possible object is
     *     {@link BitmapIndexOptions }
     *     
     */
    public BitmapIndexOptions getBitmapIndexOptions() {
        return bitmapIndexOptions;
    }

    /**
     * Sets the value of property bitmapIndexOptions.
     * 
     * @param value
     *     allowed object is
     *     {@link BitmapIndexOptions }
     *     
     */
    public void setBitmapIndexOptions(BitmapIndexOptions value) {
        this.bitmapIndexOptions = value;
    }

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
     * Gets the value of property type.
     * 
     * @return
     *     possible object is
     *     {@link IndexType }
     *     
     */
    public IndexType getType() {
        if (type == null) {
            return IndexType.SORTED;
        } else {
            return type;
        }
    }

    /**
     * Sets the value of property type.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexType }
     *     
     */
    public void setType(IndexType value) {
        this.type = value;
    }

}
