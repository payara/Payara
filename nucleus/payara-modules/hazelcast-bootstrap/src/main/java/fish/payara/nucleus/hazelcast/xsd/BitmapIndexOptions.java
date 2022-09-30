
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of bitmap-index-options complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="bitmap-index-options">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="unique-key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="unique-key-transformation" type="{http://www.hazelcast.com/schema/config}bitmap-index-unique-key-transformation" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "bitmap-index-options", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class BitmapIndexOptions {

    @XmlElement(name = "unique-key", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "__key")
    protected String uniqueKey;
    @XmlElement(name = "unique-key-transformation", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "OBJECT")
    @XmlSchemaType(name = "string")
    protected BitmapIndexUniqueKeyTransformation uniqueKeyTransformation;

    /**
     * Gets the value of property uniqueKey.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUniqueKey() {
        return uniqueKey;
    }

    /**
     * Sets the value of property uniqueKey.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUniqueKey(String value) {
        this.uniqueKey = value;
    }

    /**
     * Gets the value of property uniqueKeyTransformation.
     * 
     * @return
     *     possible object is
     *     {@link BitmapIndexUniqueKeyTransformation }
     *     
     */
    public BitmapIndexUniqueKeyTransformation getUniqueKeyTransformation() {
        return uniqueKeyTransformation;
    }

    /**
     * Sets the value of property uniqueKeyTransformation.
     * 
     * @param value
     *     allowed object is
     *     {@link BitmapIndexUniqueKeyTransformation }
     *     
     */
    public void setUniqueKeyTransformation(BitmapIndexUniqueKeyTransformation value) {
        this.uniqueKeyTransformation = value;
    }

}
