
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java Class of map-attribute complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="map-attribute">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="extractor-class-name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "map-attribute", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "value"
})
public class MapAttribute {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "extractor-class-name")
    protected String extractorClassName;

    /**
     * Gets the value of property value.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of property value.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of property extractorClassName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExtractorClassName() {
        return extractorClassName;
    }

    /**
     * Sets the value of property extractorClassName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExtractorClassName(String value) {
        this.extractorClassName = value;
    }

}
